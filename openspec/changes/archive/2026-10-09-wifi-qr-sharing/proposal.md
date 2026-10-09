# Proposal

## Why

Sharing Wi-Fi credentials with guests or setting up new devices currently requires navigating deep into settings and manually transcribing complex passphrases. Introducing standard Wi-Fi QR code generation with native Android sharing on both the Dashboard and Wi-Fi configuration screens enables instant, error-free network onboarding and encourages the use of isolated guest networks.

## What Changes

- Add QR code generation conforming to the standard `WIFI:S:<SSID>;T:<AUTH>;P:<PASSWORD>;H:<HIDDEN>;;` format.
- Create a reusable `WifiQrBottomSheet` displaying high-contrast QR code, network details, masked passphrase with visibility toggle, copy-to-clipboard, and system share button.
- Integrate Android system share (`Intent.ACTION_SEND`) to share network credentials via Android's native share sheet.
- Enhance Dashboard Wi-Fi cards with a direct QR action button to open the QR sheet for any active network.
- Enhance Wi-Fi configuration forms (Main Wi-Fi, Guest, Multi-SSID, and Wi-Fi 7 MLO) with a QR code action button for immediate credential sharing and preview.
- Support all wireless network profiles: Primary (2.4 GHz, 5 GHz, 6 GHz), Guest, Multi-SSID 1 & 2, and Wi-Fi 7 MLO.
- Add `zxing-core` dependency for lightweight, offline QR matrix generation.

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `wifi-management`: Add requirement for Wi-Fi credential sharing via standard QR code, native Android sharing, and password visibility controls across all wireless profiles.
- `dashboard-status`: Update Wi-Fi Band Status Overview requirement to support launching the Wi-Fi QR modal directly from active network cards.

## Impact

- **Dependencies**: Add `com.google.zxing:core` to `libs.versions.toml` and `app/build.gradle.kts`.
- **UI Components**: New `WifiQrBottomSheet` composable and `WifiQr` formatter/generator helper.
- **Screens**:
  - `app/src/main/kotlin/es/routerapp/app/StatusScreens.kt` (Dashboard Wi-Fi card actions)
  - `app/src/main/kotlin/es/routerapp/app/PageScreen.kt` (Wi-Fi settings card actions)
  - `app/src/main/kotlin/es/routerapp/app/WifiConsolidation.kt` (include credentials in consolidated networks)
- **APIs / Data**: No changes to router backend protocol; leverages existing `DEV2_ADT_WIFI_COMMON` attributes already fetched by the app.
