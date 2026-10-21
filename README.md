# Router DIGI — manage your Wi-Fi 7 router from Android

A **native Android app** (Kotlin + Jetpack Compose, Material 3 Expressive) to manage the **DIGI Wi-Fi 7 router in Spain** (TP-Link **XGB430v Pro**, `ESDIGI` variant, XGS-PON) without opening its web configuration page at `http://192.168.1.1/`.

The app talks directly to the router, reproducing the encrypted protocol used by its web interface, and presents the options as mobile screens: more visual and less text-heavy than the original web UI.

> [!WARNING]
> This is an **unofficial** project. It is not affiliated with or endorsed by DIGI or TP-Link. It was developed by reverse engineering the web interface and has only been tested with the model and firmware listed below. Changing a router's configuration can leave you without a connection. **Use it at your own risk.**

## What it can do

The app supports both the standard **`user`** account and the advanced **`admin`** account, dynamically adapting available screens based on credentials:

| Group | Screens | Mode |
|---|---|---|
| **Status** | Summary (Internet status, public IP, uptime, traffic, Wi-Fi networks) · Connected devices (filters: active, Wi-Fi, cable, disconnected; EasyMesh topology explorer with backhaul link rates & RSSI; device aliases & vendor OUI fingerprinting) · Speed test (Wi-Fi download, upload, ping, and jitter benchmark) · Wi-Fi QR Code sharing | User & Admin |
| **Assistant** | Fiber Diagnostics (optical Rx/Tx power, temperature, voltage, layered connectivity tests) · Dual Diagnostic (router hardware ping vs mobile Wi-Fi ping comparison to isolate local interference) | User & Admin |
| **Wi-Fi** | Main network (SSID, password, security) and Wi-Fi 7 MLO · Radio settings (mode, channel width, channel, power) · Advanced Wi-Fi (beamforming, MU-MIMO, OFDMA, TWT…, band steering) · WPS · Guest network · Additional networks (multi-SSID) | User & Admin |
| **Local network** | LAN and DHCP server · DHCP reservations · Dynamic DNS (DynDNS, No-IP) · **Intelligent QoS** (bandwidth caps, coupled 5% queue sliders, device prioritization, Game Port detection) | User / **Admin Only** (QoS) |
| **Ports and NAT** | Virtual servers (port forwarding) · Port triggering · DMZ · UPnP · ALG | User & Admin |
| **Security** | Security Health Score (posture audit & 1-tap quick fixes) · **Parental Controls** (child profiles, screen time quotas, bedtimes, category & URL filtering, web insights) · DoS protection · IP-MAC binding (ARP) | User / **Admin Only** (Parental) |
| **USB and storage** | Samba, FTP and DLNA | User & Admin |
| **System** | Date and time · LEDs (with night schedule) · Scheduled and immediate reboot · Local backup & restore (5-slot encrypted FIFO snapshots) · **System Log** (live encrypted event stream, severity filters, syslog sensitivity) · Traffic statistics | User / **Admin Only** (Log) |

Changes are sent the same way the router's web UI does, with only the modified fields. Sensitive operations (changing the SSID, the router IP, DMZ, FTP access from the Internet, rebooting…) ask for confirmation.

### Administrator Features (`admin` mode)

When logging in with the router's `admin` credentials, the app unlocks dedicated advanced management screens:

1. **Intelligent QoS & Bandwidth Prioritization (`DEV2_IQOS`)**:
   - Master QoS engine toggle with configurable upstream and downstream bandwidth caps (`Mbps` / `Kbps`).
   - Coupled priority queue allocation sliders (High, Middle, Low) enforcing 5% step constraints (`High >= 15%`, `Middle >= 10%`, `Low >= 5%`, `High > Middle > Low`, total `<= 100%`).
   - Live client device prioritization: joins active LAN/Wi-Fi devices and custom aliases to assign individual devices to High (`4`), Middle (`3`, default), or Low (`2`) priority.
   - Hardware Game Port client detection (`DEV2_GAME_QOS_CLIENT`) with pinned High Priority and graceful degradation on unsupported firmware.
2. **Parental Controls & Screen Time Insights (`DEV2_PARENTAL_CONTROL`)**:
   - Family profile management with member client device assignment.
   - Screen time limits & bedtime schedules: distinct weekday vs weekend allowances and instant Internet pause.
   - Content filtering: category toggles (Adult, Gambling, Social, Video/Streaming) and custom domain whitelists / blacklists.
   - Web activity insights: top visited domains and DNS query analytics per profile.
3. **Live System Log Stream & Syslog Sensitivity (`DEV2_PAGE_ACCESS_PERMISSION`)**:
   - Real-time encrypted event stream with live polling and search/filter across log messages and process tags.
   - Severity level filtering (Emergency, Alert, Critical, Error, Warning, Notice, Info, Debug).
   - Syslog daemon sensitivity level configuration.
   - One-tap export to clipboard or Android system share sheet.

### What is not included

- Advanced WAN uplink & physical media switching (Ethernet WAN vs GPON/XGS-PON).
- Static routing tables and RIP routing protocols (`route.htm`, `rip.htm`).
- Built-in VPN server configurations (OpenVPN, PPTP, IPSec servers).
- Firmware image flashing (`softup.htm`) and TR-069 remote ACS management.
- Advanced VoIP PBX telephony and USB 3G cellular fallback.
- Factory reset (`ACT_FACTORY_RESET` is intentionally omitted to prevent accidental loss of operator provisioning).
- Languages other than English and Spanish (see [Languages](#languages) to add one).

## Languages

The app is available in **English** and **Spanish**, and follows the language of the phone. On Android 13+ you can also pick a language for the app only in *Settings → System → Languages → App languages*. Code comments and documentation are in English; the router feature inventory is provided in both languages.

Where the texts live:

- **App chrome** (buttons, dashboard, devices, login, error messages): standard Android resources, `app/src/main/res/values/strings.xml` (English, default) and `values-es/strings.xml` (Spanish).
- **Screen catalog** (page, section and field names, help texts, option labels): the catalog in the pure-JVM `protocol` module is written in English, and translated when drawn with `tr()` using the dictionaries in [`protocol/.../i18n`](protocol/src/main/kotlin/es/routerapp/protocol/i18n) (`SpanishTexts.kt`). A missing translation falls back to English, and `CatalogI18nTest` fails if a catalog text has no Spanish entry.

To **add a language** (for example French, `fr`):

1. Create `app/src/main/res/values-fr/strings.xml` with the same names as `values/strings.xml`.
2. Add `<locale android:name="fr" />` to `app/src/main/res/xml/locales_config.xml`.
3. Create `FrenchTexts.kt` next to `SpanishTexts.kt` (English text → French text, same `{0}`, `{1}` placeholders) and register it in `I18n.kt`.
4. Run `./gradlew :protocol:test`.

## Test status

| Area | Status |
|---|---|
| Encryption and login (vectors generated with the router's own JavaScript) | Automated tests |
| Reading every screen | Verified against a real router |
| Writing (guest SSID, adding/removing rules, LED, NTP, DHCP, etc.) | Verified against a real router, with the original values restored afterwards |
| UI on a phone | Checked manually on a device |
| Dynamic DNS, MLO, reboot, changing the router IP, security changes of the main Wi-Fi | **Untested** |

Tested device: TP-Link XGB430v Pro (ESDIGI), firmware `0.2.0 3.2.1 v60c3.0 Build 250808`. Other models or firmware versions may not work.

## Requirements

- **JDK 17 or later** (tested with JDK 25).
- **Android SDK** with the `android-37` platform (Android Studio installs it automatically).
- A phone running **Android 8.0 (API 26) or later**, connected to the router's Wi-Fi or network.

You do not need to install Gradle: the project ships with the wrapper (Gradle 9.6.0).

## Build

1. Clone the repository:

   ```bash
   git clone https://github.com/fgabriel78/DigiRouterApp.git
   cd DigiRouterApp
   ```

2. Tell Gradle where the Android SDK is by creating `local.properties` in the project root (Android Studio generates it on its own; the file is in `.gitignore`):

   ```properties
   sdk.dir=C\:\\Users\\YOUR_USER\\AppData\\Local\\Android\\Sdk
   ```

   On Linux/macOS, for example `sdk.dir=/home/user/Android/Sdk`.

3. Build the debug APK:

   ```bash
   # Windows
   .\gradlew.bat :app:assembleDebug
   # Linux / macOS
   ./gradlew :app:assembleDebug
   ```

   The result is written to `app/build/outputs/apk/debug/app-debug.apk`.

You can also open the project in **Android Studio** and run the `app` configuration.

### Install on a phone

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Or copy the APK to the phone and open it (Android will ask you to allow installing from that source). To use `adb`, enable USB debugging on the phone first.

### Release build

The `release` build has minification (R8) enabled and is signed with **your own key**, which is never stored in the repository (`*.jks`, `*.keystore` and `keystore.properties` are in `.gitignore`).

1. Create a key (once) and keep a backup of it: without it you cannot publish updates.

   ```bash
   keytool -genkeypair -v -keystore my-release.jks -alias my-alias -keyalg RSA -keysize 2048 -validity 10000
   ```

2. Create `keystore.properties` in the project root (paths are relative to it):

   ```properties
   storeFile=my-release.jks
   storePassword=...
   keyAlias=my-alias
   keyPassword=...
   ```

   Alternatively, define the environment variables `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS` and `RELEASE_KEY_PASSWORD`. Without either, the release APK is built **unsigned** and cannot be installed.

3. Build it:

   ```bash
   ./gradlew :app:assembleRelease    # app/build/outputs/apk/release/app-release.apk
   ```

Test the release APK on a device before publishing it: minification can break code that relies on reflection.

### Automated tests

```bash
./gradlew test
```

This runs unit tests across both modules (`:protocol:test` for cryptographic vectors and `:app:test` for local address validation and error handling). There is also a test against a real router that only runs if you set the `ROUTER_LIVE=1` environment variable (it only makes unauthenticated queries).

## Usage

1. Connect the phone to the router's network.
2. Open the app and enter the router address (`192.168.1.1` by default), the username (`user` for standard access or `admin` for advanced management), and the password.
3. Optionally check **Remember password** to securely save credentials encrypted in Android KeyStore (`EncryptedSharedPreferences`).
4. Navigate using the expressive bento tiles and category containers on the home screen.

Notes:

- The router **only allows one administration session at a time**. If you have the router's web page open in a browser, close it first; logging in from the app will end any other session.
- With **Remember password** enabled, credentials are encrypted locally with hardware-backed keys. If disabled, credentials remain strictly ephemeral in memory.
- The app supports **automatic session recovery** to restore your authenticated session seamlessly after backgrounding or transient network interruptions.
- It works **only from the local network**. It does not expose or modify the operator's remote access.

## Architecture

The project has two Gradle modules:

```
DigiRouterApp
├── protocol/   Kotlin/JVM library, no Android dependencies
│   ├── GdprCrypto.kt     RSA-512 + AES-128-CBC + MD5: request encryption and signing
│   ├── RouterClient.kt   Login, session token and go/gl/so/ao/do/op operations
│   ├── pages/            Declarative screen catalog (Catalog.kt),
│   │                     field specification (Spec.kt) and engine (PageEngine.kt)
│   ├── qos/              Intelligent QoS data models and engine (QosEngine.kt)
│   ├── parental/         Parental control rules, schedules and analytics (ParentalControlEngine.kt)
│   ├── sysdiagnostics/   Optical fiber and dual ping diagnostic engine
│   └── discovery/        Command-line tools (see tools/README.md)
├── app/        Android app (Jetpack Compose, Material 3 Expressive)
│   └── ...               Login, dashboard, devices, speed test, fiber & dual diagnostics,
│                         security health score, QoS, parental controls, system log, backup,
│                         and a generic form renderer
├── docs/       Router feature inventory (router_features.md, router_features.es.md)
└── tools/      Documentation for the discovery and testing tools
```

### How it works

The router's web UI uses a proprietary TP-Link API (a TR-181-style data model). Every request travels encrypted:

1. `POST /cgi/getGDPRParm` returns the RSA public key (512 bits) and a sequence number.
2. Data is encrypted with AES-128-CBC using a random key; at login that key is sent encrypted with RSA and signed together with `MD5(user + password)`.
3. Subsequent operations (`go`/`gl` read, `so` modify, `ao` add, `do` delete, `op` run actions) are sent to `/cgi_gdpr` with the session token.

The Kotlin implementation is verified with vectors generated by running the router's own JavaScript (`encrypt.js`, `tpEncrypt.js`).

### Data-driven screens

Almost every screen is described in [`Catalog.kt`](protocol/src/main/kotlin/es/routerapp/protocol/pages/Catalog.kt) as sections of fields (switch, text, password, number, choice, IP, MAC, time…). The engine (`PageEngine`) reads and saves them, and the app draws them all with a single renderer. To **add a screen** you just need to:

1. Declare its `Page` and `Section` in `Catalog.kt` using the attribute names of the matching `DEV2_*` object (you can inspect them with the `probe` task).
2. Add it to the `pages` list and, optionally, an icon in `pageIcon()` (`Components.kt`).
3. Check it with `:protocol:pagesCheck` before using it.

## Command-line tools

[`tools/README.md`](tools/README.md) documents the Gradle tasks to explore the router and test the catalog (`pagesCheck`, `probe`, `safeTests`…). Some of them **write** to the router: read it before running them.

## Security and privacy

- The app uses plain HTTP because that is what the router offers on the local network; the request contents are encrypted with the router's own protocol. In `network_security_config.xml`, cleartext traffic is disabled globally and restricted to local network ranges and standard router gateways (RFC 1918 private subnets, loopback, link-local, and local domains like `.local` and `.lan`), with application-level validation on the configured address.
- **Never push** the contents of `tools/discovery-out/` or files containing your password to a public repository. Both are in `.gitignore`.
- If you find a security issue, open an [issue](https://github.com/fgabriel78/DigiRouterApp/issues) without including personal data or credentials.

## Main dependencies

Kotlin 2.4, Android Gradle Plugin 9.4, Jetpack Compose (BOM 2026.09), **Material 3 `1.5.0-beta02`**, OkHttp 5.5, kotlinx.serialization and kotlinx.coroutines.

> [!NOTE]
> Material 3 is tracked via its 1.5.0 beta release line (`1.5.0-beta02`) because the *Expressive* design system components (morphing shapes, bento containers, expressive top bars, connected sliders) are actively developed in this track. The version is pinned in [`gradle/libs.versions.toml`](gradle/libs.versions.toml).

## Contributing

Contributions are welcome, especially:

- Testing with other TP-Link or DIGI router models and firmware versions.
- Translations into other languages (see [Languages](#languages)).
- Feature improvements and optimizations for diagnostics or device discovery.

Open an [issue](https://github.com/fgabriel78/DigiRouterApp/issues) to discuss the change before sending a large pull request, and check with `:protocol:test` and `:protocol:pagesCheck` that everything still works.

## License

Distributed under the [MIT License](LICENSE).

## Legal notice

"DIGI" and "TP-Link" are trademarks of their respective owners, used here only to identify the compatible device.
