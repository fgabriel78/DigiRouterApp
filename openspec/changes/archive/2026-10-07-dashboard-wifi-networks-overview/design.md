# Design

## Context

The Summary ("Resumen") screen is rendered by `DashboardScreen` in [StatusScreens.kt](file:///c:/Users/fgabr/Dev/RouterApp/app/src/main/kotlin/es/routerapp/app/StatusScreens.kt). During data loading, it queries `DEV2_ADT_WIFI_COMMON` which returns one instance map per physical frequency band (e.g., 2.4 GHz, 5 GHz, 6 GHz).

Each band instance currently holds parameters for:
- Primary Wi-Fi: `primaryEnable`, `primarySSID`
- Guest network: `guestEnable`, `guestSSID`
- Multi-SSID 1: `mssid1Enable`, `mssid1SSID`
- Multi-SSID 2: `mssid2Enable`, `mssid2SSID`

Currently, `DashboardScreen` only iterates over `d.wifi` reading `primaryEnable` and `primarySSID`. If a router has both 2.4 GHz and 5 GHz radios broadcasting the same SSID (common in dual-band and Smart Connect environments), two identical cards are displayed without indication of guest or additional networks.

## Goals / Non-Goals

**Goals:**
- Aggregate all enabled wireless profiles (primary, guest, and multi-SSID) from the already-fetched `DEV2_ADT_WIFI_COMMON` payload.
- Consolidate networks of the same type that share an identical SSID across multiple frequency bands into a single card with combined band badges.
- Assign distinct visual iconography and category styling per network type.
- Retain visibility of primary Wi-Fi status even when disabled (`Wi-Fi principal · Desactivada`).
- Provide a clean empty/fallback state when no wireless networks are broadcasting.

**Non-Goals:**
- Toggling or modifying Wi-Fi parameters directly from the dashboard (editing remains in the dedicated catalog pages: `wifiMain`, `guest`, `multiSsid`).
- Displaying pre-shared keys, passwords, or QR codes on dashboard cards.

## Decisions

### 1. Data Model for Consolidated Networks
Introduce an internal data class and enum within `StatusScreens.kt` (or a status helper):

```kotlin
private enum class WifiNetworkCategory {
    PRIMARY,
    GUEST,
    ADDITIONAL
}

private data class ConsolidatedWifiNetwork(
    val category: WifiNetworkCategory,
    val ssid: String,
    val bands: List<String>,
    val enabled: Boolean = true,
    val additionalIndex: Int? = null,
)
```

### 2. Aggregation and Deduplication Logic
Create a pure helper function `consolidateWifiNetworks(rawBands: List<Map<String, String>>): List<ConsolidatedWifiNetwork>`:
1. **Primary networks**:
   - Filter entries where `primaryEnable == "1"` and `primarySSID` is non-empty.
   - Group by `primarySSID` and accumulate unique sorted bands.
   - If no bands have primary Wi-Fi enabled, add a single disabled entry: `ConsolidatedWifiNetwork(PRIMARY, ssid = "", bands = emptyList(), enabled = false)`.
2. **Guest networks**:
   - Filter entries where `guestEnable == "1"` and `guestSSID` is non-empty.
   - Group by `guestSSID` and accumulate unique sorted bands.
3. **Multi-SSID networks**:
   - Inspect `mssid1` and `mssid2`.
   - Filter entries where `mssid{n}Enable == "1"` and `mssid{n}SSID` is non-empty.
   - Group by `(n, mssid{n}SSID)` and accumulate unique sorted bands.
4. **Ordering**:
   - Primary network(s) first.
   - Guest network(s) second.
   - Additional network(s) third.

### 3. Visual Representation and Layout
- Maintain the 2-column grid span (`span = { GridItemSpan(2) }`) for consistency with other cards.
- **Card Iconography**:
  - Primary network (active): `Icons.Filled.Wifi` with tertiary container colors.
  - Primary network (disabled): `Icons.Filled.WifiOff` with surface container highest colors.
  - Guest network: `Icons.Filled.Group` with secondary container colors.
  - Additional networks: `Icons.Filled.Layers` with primary container or custom shape badge.
- **Subtitle & Band Formatting**:
  - Combine category name and bands: e.g., `"${typeLabel} · ${bands.joinToString(" · ")}"`.
  - Reuse the existing `bandName()` function to format band names ("2,4 GHz", "5 GHz", "6 GHz").

### 4. String Resources
Add strings in `res/values/strings.xml` and `res/values-es/strings.xml`:
- `wifi_type_primary`: "Wi-Fi principal" / "Main Wi-Fi"
- `wifi_type_guest`: "Red de invitados" / "Guest network"
- `wifi_type_additional`: "Red adicional %1$d" / "Additional network %1$d"
- `wifi_all_disabled`: "Wi-Fi desactivado" / "Wi-Fi disabled"

## Risks / Trade-offs

- **[Band order consistency]** → Mitigation: Sort bands consistently using radio frequency hierarchy (2.4 GHz before 5 GHz before 6 GHz).
- **[Long SSID or label wrapping]** → Mitigation: Apply `TextOverflow.Ellipsis` and `maxLines = 1` for both title and subtitle row, following the pattern established in the EasyMesh node cards redesign.
