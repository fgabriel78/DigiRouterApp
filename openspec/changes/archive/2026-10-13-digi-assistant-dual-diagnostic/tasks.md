# Tasks

## 1. Protocol Models & Dual Diagnostic Engine

- [x] 1.1 Implement dual diagnostic domain models (`DualDiagnosticModels.kt`) in `es.routerapp.protocol.dualdiag` (`DualDiagnosticState`, `DualDiagnosticVerdict`, `HopLatencyMetric`) and verify compilation with `./gradlew :protocol:compileKotlin`.
- [x] 1.2 Implement `DualDiagnosticEngine.kt` in `:protocol` coordinating router hardware ping (`DEV2_DIAG_IPPING`), mobile Wi-Fi gateway probe, and mobile Cloudflare probe with decision verdict evaluation, verified by unit tests in `./gradlew :protocol:test`.

## 2. Localization & Catalog Navigation

- [x] 2.1 Add bilingual strings for the Dual Diagnostic (titles, descriptions, verdicts, advice) in `app/src/main/res/values/strings.xml` and `app/src/main/res/values-es/strings.xml`.
- [x] 2.2 Define `Group.ASSISTANT` in `Spec.kt`, register `fiber-diag` and `dual-diag` in `Group.ASSISTANT` within `Catalog.kt`, remove `digi-assistant-hub`, and verify with `./gradlew :protocol:test`.

## 3. UI Screens, ViewModels & Navigation

- [x] 3.1 Implement `DualDiagnosticViewModel.kt` in `:app` managing dual diagnostic execution lifecycle and state emissions, verified with JVM unit tests in `./gradlew testDebugUnitTest`.
- [x] 3.2 Remove `DigiAssistantHubScreen.kt` and its icon mapping in `Components.kt`.
- [x] 3.3 Implement `DualDiagnosticScreen.kt` featuring live comparative metrics (Wi-Fi vs Fibra), verdict banner, and manual retry action, verified with `./gradlew :app:assembleDebug`.
- [x] 3.4 Update `MainActivity.kt` navigation routing to wire `HeroStatusCard` directly to `fiber-diag` and ensure direct back navigation to Home, verifying with `./gradlew :app:assembleDebug`.

## 4. Integration Verification & Spec Coherence

- [x] 4.1 Execute full multi-module test suite via `./gradlew testDebugUnitTest :protocol:test` to verify zero regressions.
- [x] 4.2 Validate OpenSpec change coherence and schema compliance via `openspec validate digi-assistant-dual-diagnostic`.
