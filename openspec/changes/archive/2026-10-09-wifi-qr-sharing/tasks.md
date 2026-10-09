# Tasks

## 1. Dependencies and Data Model

- [x] 1.1 Add ZXing Core (`com.google.zxing:core`) to `gradle/libs.versions.toml` and `app/build.gradle.kts`, verifying Gradle sync/compilation succeeds.
- [x] 1.2 Extend `ConsolidatedWifiNetwork` in `WifiConsolidation.kt` with `psk`, `securityMode`, and `hidden` properties, updating `consolidateWifiNetworks()` to populate them across primary, guest, additional, and MLO networks.
- [x] 1.3 Update existing unit tests in `WifiConsolidationTest.kt` and add new assertions verifying credentials and visibility flags are properly captured during consolidation.

## 2. QR Code Formatting and Generation Utility

- [x] 2.1 Create `WifiQr.kt` implementing standard Wi-Fi barcode URI generation (`WIFI:S:...;T:...;P:...;H:...;;`) with special-character escaping and auth-mode mapping (`WPA`, `nopass`, `WEP`).
- [x] 2.2 Add offline QR bitmap rendering in `WifiQr.kt` using ZXing `QRCodeWriter` generating an Android `Bitmap` or Compose `ImageBitmap`.
- [x] 2.3 Create `WifiQrTest.kt` covering URI formatting, special character escaping (`\`, `;`, `:`, `,`), open networks (`nopass`), hidden SSIDs, and verify all test cases pass.

## 3. UI Component: Wi-Fi QR Bottom Sheet

- [x] 3.1 Add string resources for Wi-Fi QR sharing (copy password, share Wi-Fi, title, QR content descriptions) in `strings.xml`.
- [x] 3.2 Implement `WifiQrBottomSheet.kt` displaying the high-contrast QR code, network badge, frequency band chips, masked password row with reveal toggle, clipboard copy button, and Android system `ACTION_SEND` share launcher.

## 4. Screen Integrations

- [x] 4.1 Integrate QR launcher into `DashboardScreen` in `StatusScreens.kt`, adding a trailing QR icon button on active network cards that displays `WifiQrBottomSheet`.
- [x] 4.2 Integrate QR launcher into `PageScreen.kt`, providing a QR action button on active Wi-Fi section cards (`primary`, `guest`, `mssid1`, `mssid2`, `mlo`) that opens `WifiQrBottomSheet`.

## 5. Verification and Integration Validation

- [x] 5.1 Execute `./gradlew testDebugUnitTest` and verify all tests pass without regressions.
