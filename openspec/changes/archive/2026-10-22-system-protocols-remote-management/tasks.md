# Tasks

## 1. Protocol Engine & Data Models

- [x] 1.1 Create `RemoteManagementModels.kt` in `:protocol` with data structures for `SshConfig`, `TelnetConfig`, `HttpConfig`, `SuperHttpConfig`, and `IspManagementAudit`, verifying with `./gradlew :protocol:assemble`
- [x] 1.2 Implement `RemoteManagementEngine.kt` in `:protocol` handling `DEV2_SSH_CFG`, `DEV2_TELNET_CFG`, `DEV2_HTTP_CFG`, `DEV2_SUPER_HTTP_CFG`, `DEV2_MANAGEMENT_SERVER`, and raw GET `/cgi/https?downCACert`, verifying with unit tests in `RemoteManagementEngineTest.kt` via `./gradlew :protocol:test`

## 2. ViewModel & Platform Integrations

- [x] 2.1 Implement `RemoteManagementViewModel.kt` in `:app` managing UI state flow, port validation, recovery runner integration, and KeyChain intent triggers, verifying with unit tests via `./gradlew :app:testDebugUnitTest`
- [x] 2.2 Add bilingual string resources for remote management labels, port descriptions, risk warnings, and certificate actions in `app/src/main/res/values/strings.xml` and `app/src/main/res/values-es/strings.xml`, verifying with `./gradlew :app:assembleDebug`

## 3. Jetpack Compose UI & Navigation Integration

- [x] 3.1 Build `RemoteManagementScreen.kt` in `:app` with Material 3 Expressive tokens, segmented cards for Daemons, Web & SSL, and ISP Audit, contextual WAN risk banners, and CA Install action, verifying with `./gradlew :app:assembleDebug`
- [x] 3.2 Register `remote-mgmt` in `Catalog.kt` under `Group.SYSTEM` with `adminOnly = true` and wire navigation in `MainActivity.kt`, verifying with unit tests in `./gradlew :app:testDebugUnitTest`

## 4. Full Verification & OpenSpec Validation

- [x] 4.1 Execute full test suite via `./gradlew testDebugUnitTest` ensuring zero regressions across all modules
- [x] 4.2 Run `openspec validate --change "system-protocols-remote-management"` to verify all schema artifacts, requirements, and scenarios pass specification checks
