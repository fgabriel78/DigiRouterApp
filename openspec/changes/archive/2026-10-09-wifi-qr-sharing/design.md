# Design

## Context

See `proposal.md` for background and user goals.

Currently, the app retrieves all wireless profiles via the `DEV2_ADT_WIFI_COMMON` OID, including SSID, security mode, pre-shared key (`PSK`), broadcast visibility (`SSIDAdvertise`), and enabled status for 2.4 GHz, 5 GHz, 6 GHz, and MLO. On the dashboard, `WifiConsolidation.kt` consolidates bands by identical SSID into `ConsolidatedWifiNetwork` objects. In settings, `PageScreen.kt` renders individual Wi-Fi section cards (`primary`, `guest`, `mssid1`, `mssid2`, `mlo`).

This design introduces a standalone QR formatting/generation utility, extends network models with credential metadata, and introduces a shared `WifiQrBottomSheet` triggered from both entry points.

## Goals / Non-Goals

**Goals:**
- Generate compliant Wi-Fi barcode URIs (`WIFI:S:<SSID>;T:<AUTH>;P:<PASSWORD>;H:<HIDDEN>;;`) with proper string escaping.
- Render high-contrast QR code bitmaps offline with no required camera/hardware permissions.
- Provide a unified `WifiQrBottomSheet` with QR display, network details, masked passphrase toggle, clipboard copying, and native Android `ACTION_SEND` share.
- Integrate seamless QR launch triggers into Dashboard network cards and Settings Wi-Fi cards.

**Non-Goals:**
- QR code scanning / camera integration (this feature is generation and display only).
- Generating QR codes for disabled networks.
- External cloud sharing, link shorteners, or remote sync services.
- Router backend protocol changes (reads existing client OID fields).

## Decisions

### 1. QR Code Generation via ZXing Core
- **Choice**: Add `com.google.zxing:core:3.5.3` to version catalog and app dependencies.
- **Rationale**: ZXing Core is lightweight (~500 KB), mature, offline, and generates a raw `BitMatrix` without requiring Android camera libraries or bloated UI dependencies.
- **Alternatives considered**:
  - `qrcode-kotlin`: Pure Kotlin, but less widely battle-tested across Android versions.
  - Google Play Services ML Kit: Geared toward barcode scanning, not offline generation.

### 2. Wi-Fi URI Barcode Format and Escaping
- **Choice**: Standard `WIFI:S:<SSID>;T:<AUTH>;P:<PASSWORD>;H:<HIDDEN>;;`.
  - Auth mapping: `WPA2-Personal`, `WPA3-Personal`, `WPA2-WPA3-Personal` -> `T:WPA;` (universally recognized by both Android and iOS scanners). `None` / `OWE` -> `T:nopass;` (omitting `P:`). `WEP` -> `T:WEP;`.
  - Special character escaping: `;`, `:`, `\`, `,`, `"` in SSID and Password will be escaped with `\`.
  - Hidden flag: `H:true;` if SSID broadcast is disabled (`SSIDAdvertise == "0"`).

### 3. Extend `ConsolidatedWifiNetwork` Model
- **Choice**: Add `psk: String`, `securityMode: String`, `hidden: Boolean` to `ConsolidatedWifiNetwork`.
- **Rationale**: Avoids extra asynchronous queries or OID fetching on the dashboard. When the dashboard aggregates `DEV2_ADT_WIFI_COMMON`, all attributes are already present in the dictionary.
- **Wi-Fi 7 MLO**: When `mloEnable == "1"`, consolidate the unified MLO network with `mloSSID` and `mloPSK`.

### 4. Shared `WifiQrBottomSheet` Component
- **Choice**: Create `WifiQrBottomSheet` in `app/src/main/kotlin/es/routerapp/app/WifiQrBottomSheet.kt`.
- **UI Structure**:
  - Material 3 `ModalBottomSheet`.
  - QR Code card: Rendered on a pure white `Surface` with padding to ensure high optical contrast even when the app is in Dark Mode.
  - Network title (SSID), category badge (Primary, Guest, Additional, MLO), and active frequency band chips.
  - Passphrase row: Obscured with dots by default, with an eye toggle to show/hide plaintext and a copy button.
  - Actions: Outlined "Copy password" button and Filled "Share..." button.

### 5. Native System Sharing
- **Choice**: Use standard Android Intent:
  ```kotlin
  val intent = Intent(Intent.ACTION_SEND).apply {
      type = "text/plain"
      putExtra(Intent.EXTRA_SUBJECT, "Wi-Fi: $ssid")
      putExtra(Intent.EXTRA_TEXT, "Wi-Fi: $ssid\nPassword: $psk")
  }
  context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_wifi)))
  ```
- **Rationale**: Works natively across all installed apps (WhatsApp, Telegram, QuickShare, SMS, Email, Notes) with zero extra permissions.

### 6. Dual Entry Points
- **Dashboard**: Trailing QR icon button (`Icons.Filled.QrCode2`) on each enabled Wi-Fi card in `StatusScreens.kt`. Tapping opens the bottom sheet with `inspectingWifi`.
- **Settings**: Trailing or header QR button on editable Wi-Fi sections (`primary`, `guest`, `mssid1`, `mssid2`, `mlo`) in `PageScreen.kt`.

## Risks / Trade-offs

- **[Risk] Low QR contrast in Dark Theme** -> *Mitigation*: The QR code image is always rendered in black-on-white inside a white rounded surface with 16.dp quiet zone padding.
- **[Risk] Special characters in credentials** -> *Mitigation*: Dedicated unit-tested escaping helper `escapeWifiString()` for standard Wi-Fi URI format.
- **[Risk] Disparate band credentials for split SSIDs** -> *Mitigation*: Networks with different SSIDs are already represented as distinct cards by `consolidateWifiNetworks`, each producing its own accurate QR code.
