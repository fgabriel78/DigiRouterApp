# Design

## Context

Connected devices are represented by `ConnectedDevice` (in `protocol/src/main/kotlin/es/routerapp/protocol/pages/ConnectedDevices.kt`), which exposes `name` (DHCP client hostname reported by the router), `ip`, `mac`, connection metrics, and mesh association. 

In `app/src/main/kotlin/es/routerapp/app/OuiLookup.kt`, `resolve(mac)` identifies physical IEEE OUI vendors or flags the MAC as locally administered (`isPrivate = true`). When `isPrivate` is true, the vendor is set to null, causing `StatusScreens.kt` and `DeviceDetailSheet.kt` to display only a static, non-interactive "MAC privada" chip.

See `proposal.md` for motivation and background.

## Goals / Non-Goals

**Goals:**
- Implement an offline, zero-overhead regex/token pattern matcher (`DeviceHostnameHeuristics`) that inspects `ConnectedDevice.name` to infer manufacturer and `DeviceCategory`.
- Formulate a clear priority pipeline: User Custom Alias/Category > Hardware OUI Vendor > Hostname Heuristic > Private MAC Flag > Fallback.
- Reflect inferred categories in device card icons when no user category is explicitly set.
- Differentiate hardware OUI vendor badges from hostname-inferred badges in the UI.
- Enhance `DeviceDetailSheet` with educational copy explaining randomized private MACs and highlighting inferred attributes.

**Non-Goals:**
- Active network probing (mDNS, SSDP, NetBIOS, UPnP) which requires multicast socket permissions and increases battery consumption.
- Persisting inferred values to `DeviceMetadataStore` automatically (user store retains only explicit user overrides).

## Decisions

### 1. Hostname Pattern Matching Engine (`DeviceHostnameHeuristics`)
- **Structure**:
  ```kotlin
  data class HeuristicDeviceInfo(
      val vendor: String? = null,
      val category: DeviceCategory? = null,
  )

  object DeviceHostnameHeuristics {
      fun infer(hostname: String?): HeuristicDeviceInfo
  }
  ```
- **Rules**:
  - **Apple**:
    - `iPhone` -> Vendor: Apple, Category: `PHONE`
    - `iPad` -> Vendor: Apple, Category: `TABLET`
    - `MacBook`, `iMac`, `Mac-Mini`, `Mac-Pro` -> Vendor: Apple, Category: `COMPUTER`
    - `Apple-Watch` -> Vendor: Apple, Category: `IOT`
    - `Apple-TV` -> Vendor: Apple, Category: `TV`
  - **Samsung**:
    - `Galaxy[-_ ]?S`, `Galaxy[-_ ]?A`, `Galaxy[-_ ]?Z`, `Galaxy[-_ ]?Note`, `SM-[A-Z][0-9]{3}` -> Vendor: Samsung, Category: `PHONE`
    - `Galaxy[-_ ]?Tab` -> Vendor: Samsung, Category: `TABLET`
  - **Google**:
    - `Pixel` -> Vendor: Google, Category: `PHONE`
    - `Nest`, `Google-Home` -> Vendor: Google, Category: `IOT`
    - `Chromecast` -> Vendor: Google, Category: `TV`
  - **Xiaomi**:
    - `Redmi`, `POCO`, `Xiaomi`, `Mi[-_ ]` -> Vendor: Xiaomi, Category: `PHONE`
  - **Amazon**:
    - `Echo` -> Vendor: Amazon, Category: `IOT`
    - `Kindle` -> Vendor: Amazon, Category: `TABLET`
    - `Fire[-_]?TV`, `Fire[-_]?Stick` -> Vendor: Amazon, Category: `TV`
  - **Gaming Consoles**:
    - `Switch`, `Nintendo` -> Vendor: Nintendo, Category: `CONSOLE`
    - `PlayStation`, `PS4`, `PS5` -> Vendor: Sony, Category: `CONSOLE`
    - `Xbox` -> Vendor: Microsoft, Category: `CONSOLE`
  - **Computers**:
    - `DESKTOP-`, `LAPTOP-`, `.*-PC`, `PC-.*` -> Category: `COMPUTER`
  - **Generic Android**:
    - `android-[a-f0-9]+` -> Category: `PHONE` (Vendor: null)
- **Alternatives Considered**:
  - *Full machine-learning / Bayesian model*: Overkill, high memory footprint, unnecessary for typical home router hostnames.
  - *Exact prefix lookup*: Too brittle to handle user-customized hostnames like `iPhone-de-Carlos` or `Lauras-MacBook-Pro`.

### 2. Device Identity Resolution Pipeline
- Integrate resolution into a helper or extend `MacVendorInfo`:
  ```kotlin
  enum class VendorSource {
      HARDWARE_OUI,
      HOSTNAME_HEURISTIC,
      NONE,
  }

  data class ResolvedDeviceInfo(
      val displayTitle: String,
      val vendor: String?,
      val vendorSource: VendorSource,
      val isPrivateMac: Boolean,
      val defaultCategory: DeviceCategory?,
  )
  ```
- **Precedence**:
  1. Title: `customMeta.alias` -> `device.name` -> `resolvedVendor` -> `device.ip` -> `device.mac`.
  2. Category Icon: `customMeta.category` -> `heuristic.category` -> (Wi-Fi signal or Ethernet default).
  3. Vendor Badge: If physical OUI exists, display verified vendor badge. If private or unlisted, display heuristic vendor badge if found. If no vendor and private MAC, display "MAC privada" chip.

### 3. UI & UX Refinements
- **Device Cards (`StatusScreens.kt`)**:
  - Inferred vendor badges display the manufacturer with subtle visual styling (e.g., secondary chip container).
  - Default category icons are immediately populated based on inferred category, making device types identifiable at a glance.
- **Detail Sheet (`DeviceDetailSheet.kt`)**:
  - If `isPrivate`: Display an educational helper banner explaining that the device uses randomized Wi-Fi MAC for privacy.
  - If vendor is inferred: Display `Apple (detectado por nombre)` or an informational caption.
  - Pre-populate category picker in the editor if no custom category is saved, allowing 1-click confirmation.

## Risks / Trade-offs

- **[Heuristic False Positives]** → E.g., a computer host named `iPhone-Test-Rig`.
  *Mitigation*: User-defined custom aliases and categories always override heuristics. Inferred source is clearly labeled in details.
- **[Opaque or Empty Hostnames]** → E.g., clients with no DHCP hostname or privacy-configured random strings.
  *Mitigation*: Gracefully falls back to "MAC privada" or IP address without guessing false manufacturers.
- **[Localization]** → Explanatory copy must be localized in both English and Spanish (`strings.xml` and `values-es/strings.xml`).
