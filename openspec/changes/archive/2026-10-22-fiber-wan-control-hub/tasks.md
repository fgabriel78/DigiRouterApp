# Tasks

## 1. Data Models and Utilities (`:protocol`)

- [x] 1.1 Implement `GponHexUtil.kt` providing bidirectional conversion between 12-char ASCII+HEX (`HWTC12345678`) and 16-char HEX (`4857544331323334`), plus standardized SFP ONT export snippet generation; verify with unit tests in `GponHexUtilTest` via `./gradlew :protocol:test`.
- [x] 1.2 Define `GponAuthInfo`, `OltMode`, and `WanRenewProgress` data models in `OpticalModels.kt`, mapping `DEV2_GPON_AUTH` attributes and renewal progress states; verify model creation and mapping tests via `./gradlew :protocol:test`.

## 2. Engine Extensions & Lifecycle Actions (`:protocol`)

- [x] 2.1 Extend `FiberDiagnosticEngine.kt` to read `DEV2_GPON_AUTH` on stack `'1,0,0,0,0,0'` during Step 2 (OMCI Registration) and attach `GponAuthInfo` to `DiagnosticSnapshot`; verify with mock engine tests via `./gradlew :protocol:test`.
- [x] 2.2 Implement `restartOmci()` in `FiberDiagnosticEngine.kt` dispatching `operate("ACT_OMCI_RESTART")` and polling `DEV2_GPON_INTF_OMCI_STATS` until state returns to `O5 Associated`; verify with unit tests in `FiberDiagnosticEngineTest` via `./gradlew :protocol:test`.
- [x] 2.3 Implement `renewWanSession()` in `FiberDiagnosticEngine.kt` executing `ACT_PPP_DISCONN` / `ACT_PPP_CONN` for PPPoE and `ACT_DHCP_RELEASE` / `ACT_DHCP_RENEW` for DHCP, polling `DEV2_ADT_WAN` for IP renewal confirmation; verify with unit tests in `FiberDiagnosticEngineTest` via `./gradlew :protocol:test`.

## 3. ViewModel & UI Integration (`:app`)

- [x] 3.1 Declare bilingual string resources in English (`values/strings.xml`) and Spanish (`values-es/strings.xml`) covering ONT registration parameters, masked password toggle, SFP export, OMCI restart confirmation, and WAN renewal status toasts.
- [x] 3.2 Extend `FiberDiagnosticViewModel.kt` to expose `renewWanSession()` and `restartOmci()` flows with observable state transitions; verify with unit tests in `FiberDiagnosticViewModelTest` via `./gradlew :app:testDebugUnitTest`.
- [x] 3.3 Update `FiberDiagnosticScreen.kt` to render the expandable ONT Registration card in Step 2 featuring dual ASCII/HEX SN, masked PLOAM password, SFP export button, and OMCI restart trigger.
- [x] 3.4 Update `FiberDiagnosticScreen.kt` to render the "Renew Public IP / Reconnect" button in Step 3 featuring in-flight progress indication, WAN details, and result comparison.

## 4. System Verification & Clean Build

- [x] 4.1 Run `./gradlew testDebugUnitTest` and verify that all protocol and app unit tests pass cleanly with zero regressions.
