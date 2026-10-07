# Proposal

## Why

Currently, the Summary ("Resumen") dashboard screen only displays primary Wi-Fi SSIDs per frequency band and ignores the guest network ("Red de invitados") and additional multi-SSID profiles. This creates an inconsistent and incomplete overview for users who expect to see all active wireless networks broadcast by the router without having to navigate deep into configuration menus. Furthermore, identical SSIDs broadcasting across multiple frequency bands (e.g. 2.4 GHz and 5 GHz) are currently shown as duplicate separate cards instead of a unified consolidated entry.

## What Changes

- **Consolidated wireless network overview**: Display all enabled wireless networks on the router, including the primary Wi-Fi, guest network, and additional networks (Multi-SSID 1 and 2).
- **Deduplication across bands**: Consolidate networks of the same type that share the same SSID across multiple frequency bands (2.4 GHz, 5 GHz, 6 GHz) into a single card listing all active bands (e.g., "2,4 GHz · 5 GHz").
- **Visual distinction by network type**: Use distinct icons and descriptive category labels for each network type:
  - Wi-Fi principal: Wi-Fi icon (`Icons.Filled.Wifi` / `Icons.Filled.WifiOff`)
  - Red de invitados: Group/People icon (`Icons.Filled.Group`)
  - Redes adicionales: Multi-layer icon (`Icons.Filled.Layers`)
- **Primary Wi-Fi status visibility**: If the primary Wi-Fi is disabled on all or any band, display a card explicitly indicating that the primary network is deactivated (`Wi-Fi principal · Desactivada`), ensuring users always know the status of their main network.
- **Global Wi-Fi disabled fallback**: If no wireless networks are active across the entire router, display a single informational card indicating that Wi-Fi is disabled.
- **Localization**: Add and adapt Spanish and English strings for network type indicators and consolidated band formatting.

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `dashboard-status`: Modifies the "Wi-Fi Band Status Overview" requirement to aggregate and consolidate all enabled wireless network profiles (primary, guest, multi-SSID) with band deduplication and distinct type iconography.

## Impact

- **UI / Presentation Layer**: [StatusScreens.kt](file:///c:/Users/fgabr/Dev/RouterApp/app/src/main/kotlin/es/routerapp/app/StatusScreens.kt) (`DashboardScreen` and supporting helpers) to parse and aggregate `DEV2_ADT_WIFI_COMMON` instances into consolidated Wi-Fi entries.
- **Localization Resources**: [values/strings.xml](file:///c:/Users/fgabr/Dev/RouterApp/app/src/main/res/values/strings.xml) and [values-es/strings.xml](file:///c:/Users/fgabr/Dev/RouterApp/app/src/main/res/values-es/strings.xml) to supply badges/labels for guest and additional networks.
- **API / Protocol Layer**: No protocol changes or new OID queries are required; `DEV2_ADT_WIFI_COMMON` is already fetched during dashboard loading.
