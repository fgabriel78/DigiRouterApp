# Tasks

## 1. String Resources and Data Model

- [x] 1.1 Add string resources for Wi-Fi network categories (`wifi_type_primary`, `wifi_type_guest`, `wifi_type_additional`, `wifi_all_disabled`) in `res/values/strings.xml` and `res/values-es/strings.xml`. Verify compilation with `./gradlew assembleDebug`.
- [x] 1.2 Define `WifiNetworkCategory` and `ConsolidatedWifiNetwork` data structures in `StatusScreens.kt` (or dedicated status model). Verify model integrity.

## 2. Consolidation Logic and Unit Tests

- [x] 2.1 Implement `consolidateWifiNetworks` pure helper function to aggregate primary, guest, and multi-SSID networks from `DEV2_ADT_WIFI_COMMON`, deduplicating identical SSIDs across frequency bands and formatting active bands.
- [x] 2.2 Write unit tests in `app/src/test/kotlin` covering single-band, dual-band consolidated SSIDs, guest network extraction, additional multi-SSID extraction, primary-disabled state, and all-disabled state. Verify tests pass with `./gradlew testDebugUnitTest`.

## 3. UI Dashboard Rendering

- [x] 3.1 Update `DashboardScreen` in `StatusScreens.kt` to replace the raw per-band primary Wi-Fi grid items with consolidated network cards featuring distinct iconography (`Icons.Filled.Wifi`, `Icons.Filled.Group`, `Icons.Filled.Layers`) and formatted subtitles.
- [x] 3.2 Implement disabled primary Wi-Fi indicator card and global "Wi-Fi desactivado" fallback card when all networks are offline.
- [x] 3.3 Verify the complete application build and test suite pass cleanly with `./gradlew assembleDebug testDebugUnitTest`.
