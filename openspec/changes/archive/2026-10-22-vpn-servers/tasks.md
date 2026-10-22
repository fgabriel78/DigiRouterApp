# Tasks

## 1. Protocol Models & OpenVPN Engine (:protocol)

- [x] 1.1 Create `VpnModels.kt` in `:protocol` defining data models for OpenVPN, IPSec, PPTP configurations, and active client sessions; verify with serialization unit tests via `./gradlew :protocol:testDebugUnitTest`.
- [x] 1.2 Implement `OpenVpnProfileTransformer.kt` in `:protocol` for parsing and overriding the `remote <host> <port>` directive in `.ovpn` configuration text; verify regex handling and host replacement in `OpenVpnProfileTransformerTest`.
- [x] 1.3 Extend `RouterClient` with `.ovpn` download (`/cgi/openvpn?cliOvpnDown`) and key regeneration triggers (`ACT_OP_OVPN_REGEN_KEYS`), and implement `VpnEngine.kt` to coordinate TR-181 operations; verify with mock client tests in `VpnEngineTest`.
- [x] 1.4 Register the `"vpn"` page in `Catalog.kt` under `Group.NETWORK` with `adminOnly = true` and `custom = true`, and add dictionary entries to `SpanishTexts.kt`; verify catalog lookup and admin filtering via `CatalogTest`.

## 2. Storage & ViewModel State Management (:app)

- [x] 2.1 Update `res/xml/file_paths.xml` to include cache and vpn storage paths for `FileProvider`, and implement profile caching helper; verify file creation and `content://` URI generation in `VpnProfileShareTest`.
- [x] 2.2 Implement CGNAT (`100.64.0.0/10`) and DDNS readiness detection evaluator; verify detection across CGNAT, private, and public WAN IP ranges in `VpnReadinessTest`.
- [x] 2.3 Implement `VpnViewModel.kt` orchestrating configuration fetching, key regeneration progress lifecycle, host override, and error recovery; verify state transitions in `VpnViewModelTest`.

## 3. Jetpack Compose UI & Navigation (:app)

- [x] 3.1 Define bilingual string resources in `values/strings.xml` and `values-es/strings.xml` for all VPN controls, status badges, banners, and modals; verify clean compilation via `./gradlew :app:assembleDebug`.
- [x] 3.2 Build `VpnScreen.kt` using Material 3 Expressive tokens, segmented buttons (OpenVPN, Connected Clients, IPSec & PPTP), and CGNAT/DDNS status banner; verify UI component rendering with Compose previews.
- [x] 3.3 Implement the `.ovpn` Export Modal Bottom Sheet with live host override input, Android Share Sheet launcher (`Intent.ACTION_SEND`), and SAF file saver (`CreateDocument`); verify sheet state and export trigger actions.
- [x] 3.4 Wire `"vpn"` page routing into `MainActivity.kt` navigation handler; verify navigation to and back from the VPN screen.

## 4. System Verification

- [x] 4.1 Execute full test suite via `./gradlew testDebugUnitTest` and verify zero regressions across all modules.
- [x] 4.2 Validate OpenSpec change compliance via `openspec validate --change vpn-servers` and verify all checks pass.
