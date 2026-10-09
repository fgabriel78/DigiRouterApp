# Proposal

## Why

With modern mobile and desktop operating systems (iOS, Android, Windows) enabling Wi-Fi MAC address randomization by default, client hardware MAC addresses are locally administered and carry no IEEE Organizationally Unique Identifier (OUI). Consequently, the existing offline OUI vendor lookup labels most phones, tablets, and laptops simply as "MAC privada" without vendor context, or leaves the vendor blank when a physical MAC's OUI is not in the database.

However, many of these devices disclose their model or identity through DHCP client hostnames (e.g., `iPhone-de-Pepe`, `Galaxy-S23`, `Pixel-8`, `DESKTOP-49K`, `iPad-Air`, `Redmi-Note-12`). Introducing intelligent hostname heuristics to deduce manufacturer and device category bridges this gap, providing immediate recognition for devices with randomized or unlisted MACs.

## What Changes

- **Hostname Heuristics Resolver (`DeviceHostnameHeuristics`)**:
  - Implement pattern matching and prefix/token rules on device hostnames to infer the hardware manufacturer (e.g., Apple, Samsung, Google, Xiaomi, Amazon, Nintendo, Sony, Microsoft) and default `DeviceCategory` (PHONE, TABLET, COMPUTER, TV, CONSOLE, IOT, PRINTER).
- **Hierarchical Device Identification Engine**:
  - Prioritize identification sources:
    1. User custom alias and custom category (always takes top priority).
    2. Verified physical OUI hardware lookup.
    3. Heuristic manufacturer and category deduced from hostname.
    4. Private MAC indication ("MAC privada").
    5. Unknown/fallback.
- **Enhanced UI Presentation**:
  - In `StatusScreens` device cards, display the heuristically inferred vendor badge and apply the inferred category icon by default when no custom alias/category has been defined by the user.
  - In `DeviceDetailSheet`, clearly indicate whether the vendor was identified via physical hardware OUI or inferred via hostname, explain what "MAC privada" signifies, and pre-select the suggested category in the editor.

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `dashboard-status`: Enrich connected host identification and categorization with hostname-based pattern heuristics for inferring manufacturers and device types when MAC addresses are randomized or unregistered.

## Impact

- **App Module**:
  - New `DeviceHostnameHeuristics` utility and unit tests.
  - Integration into `MacVendorInfo` / `OuiLookup` or dedicated device resolution logic.
  - Updates to `StatusScreens.kt` (device card badge & default category icon) and `DeviceDetailSheet.kt` (informative vendor origin and private MAC explanation).
  - String resources for heuristic indicators and explanatory copy.
- **Protocol Module**:
  - No changes; utilizes existing `ConnectedDevice.name`.
- **Dependencies**:
  - No new external dependencies.
