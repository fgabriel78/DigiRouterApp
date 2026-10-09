# Design

## Context

Connected devices are currently discovered through `ConnectedDevices.loadData(client)` and held in `ConnectedDevice` instances. In `DevicesScreen` ([StatusScreens.kt](file:///c:/Users/fgabr/Dev/RouterApp/app/src/main/kotlin/es/routerapp/app/StatusScreens.kt)), each device is rendered as a static card displaying the raw hostname reported by the router, IP address, Wi-Fi signal or Ethernet icon, link rate, and associated EasyMesh node.

Because the router's `user` account cannot write client inventory records to the router, custom aliases, category icons, and notes must be managed client-side. Furthermore, client MAC addresses currently carry no manufacturer information.

## Goals / Non-Goals

**Goals:**
- Provide zero-latency, 100% private offline IEEE OUI vendor resolution for top consumer tech manufacturers.
- Identify IEEE 802 randomized/locally administered MAC addresses and present them clearly as "Private MAC address".
- Provide a persistent local store (`DeviceMetadataStore`) mapping MAC addresses to custom aliases and device categories.
- Update `DevicesScreen` to prioritize user-configured aliases, display vendor badges, and render category-specific icons.
- Add an interactive Material 3 `ModalBottomSheet` for inspecting hardware/connection specs and editing alias/category.

**Non-Goals:**
- External web API lookups for MAC addresses (avoiding privacy leaks, latency, and offline failures).
- Modifying router DHCP leases or firmware records.
- Bandwidth management or throttling (QoS is restricted to the admin role).

## Decisions

### 1. Offline OUI Vendor Resolution (`OuiLookup`)
- **Structure**: Curated dataset of the top ~1,000+ consumer device manufacturers (Apple, Samsung, Google, Sony, Xiaomi, LG, Nintendo, Microsoft, Amazon, Espressif, Philips/Signify, Tuya, Intel, Realtek, TP-Link, Synology, Raspberry Pi, Sonos, etc.).
- **Randomized MAC Detection**:
  ```kotlin
  val isLocallyAdministered = (firstByte and 0x02) != 0
  ```
  If true, the lookup classifies the address as `Private`, allowing the UI to display a distinctive badge and helpful explanation instead of an "Unknown Vendor" label.
- **Alternatives Considered**:
  - *Full IEEE dump (~35k entries)*: Adds 3+ MB of asset bloat for marginal benefit (mostly enterprise industrial hardware).
  - *Online HTTP APIs*: Leaks home network MAC addresses to third parties and fails without internet connectivity.

### 2. Local Storage Architecture (`DeviceMetadataStore`)
- **Storage**: Backed by Android `SharedPreferences` storing serialized JSON via `kotlinx.serialization`.
- **Model**:
  ```kotlin
  enum class DeviceCategory {
      PHONE, TABLET, COMPUTER, TV, CONSOLE, IOT, PRINTER, ROUTER, OTHER
  }

  @Serializable
  data class DeviceCustomMetadata(
      val alias: String? = null,
      val category: DeviceCategory? = null,
      val notes: String? = null,
  )
  ```
- **Rationale**: Lightweight, zero boilerplate, no database migration overhead for small key-value sets (typically 20–60 devices).
- **Alternatives Considered**:
  - *Room Database*: Overkill for a simple key-value mapping of device MACs.

### 3. Visual Hierarchy & Card Typography
- **Option B Implementation**:
  - **With Custom Alias**:
    - Primary title: Custom alias (e.g., "Living Room TV").
    - Title badge: Vendor tag chip (e.g., "Sony") or Private MAC chip.
    - Subtitle: IP address and connected EasyMesh AP.
  - **Without Alias**:
    - Primary title: Router-reported hostname (or vendor name / IP if hostname is blank).
    - Title badge: Vendor tag or Private MAC chip.
    - Subtitle: IP address and connected EasyMesh AP.
  - **Iconography**: When a category is chosen, use the category icon (`Icons.Filled.Smartphone`, `Tv`, `Computer`, `SportsEsports`, etc.); otherwise fall back to signal level or Ethernet cable icon.

### 4. Interactive Bottom Sheet (`DeviceDetailSheet`)
- Tapping a device card triggers a Material 3 `ModalBottomSheet`:
  - **Category Picker**: Scrollable row of category chips with icons.
  - **Alias Editor**: `OutlinedTextField` with clear button.
  - **Connection & Hardware Metrics**:
    - Original router hostname
    - Hardware manufacturer (with OUI hex)
    - IP address and MAC address
    - Connection type (Wi-Fi band, signal strength in bars and dBm if available, link speed)
    - Connected EasyMesh node (e.g., "Router Principal" or satellite node name)
  - **Actions**: "Reset to Default" button and "Save" button.

## Risks / Trade-offs

- **[MAC Randomization on iOS/Android]** → Modern phones change MAC addresses when "Private Wi-Fi Address" is toggled or reset per SSID.
  *Mitigation*: Educate users in the detail sheet that private MAC addresses are device-specific; advise keeping private MACs stable per home network for persistent aliases.
- **[Device Roaming / Dynamic IP Reassignment]** → Devices may receive different IP addresses over time via DHCP.
  *Mitigation*: All metadata is strictly keyed by normalized MAC address (`xx:xx:xx:xx:xx:xx` uppercase), ensuring aliases remain consistent regardless of IP changes.
- **[UI Density on Small Screens]** → Long aliases and vendor badges could cause card text wrapping.
  *Mitigation*: Apply `TextOverflow.Ellipsis`, `maxLines = 1`, and flexible badge sizing consistent with existing EasyMesh node card patterns.
