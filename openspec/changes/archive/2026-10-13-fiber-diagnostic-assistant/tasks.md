# Tasks

## 1. Pure Kotlin Protocol Models & Telemetry Parser

- [x] 1.1 Implement optical telemetry domain models (`OpticalTelemetry`, `OpticalHealthStatus`, `DiagnosticStep`, `DiagnosticResult`) in `es.routerapp.protocol.optical` and verify compilation with `./gradlew :protocol:compileKotlin`.
- [x] 1.2 Implement `OpticalTelemetryParser.kt` normalizing varied router representations (strings with dBm suffixes, scaled integers in hundredths of dBm, empty/missing fields) into standard metrics, and verify with comprehensive unit tests in `./gradlew :protocol:test`.

## 2. Multi-Layer Diagnostic Execution Engine

- [x] 2.1 Implement `FiberDiagnosticEngine.kt` in `:protocol` executing the 5-step diagnostic sequence (Optical power -> OMCI registration -> WAN lease -> Gateway ping -> DNS resolution) with step failure isolation and `Flow` progress updates, and verify with unit tests in `./gradlew :protocol:test`.

## 3. UI, StateFlow ViewModel & Navigation

- [x] 3.1 Implement `FiberDiagnosticViewModel.kt` in `:app` managing diagnostic lifecycle, automatic execution on entry, and retry logic, and verify state mappings with JVM unit tests in `./gradlew testDebugUnitTest`.
- [x] 3.2 Add bilingual strings for optical power levels, traffic light descriptions, diagnostic steps, and error explanations in `app/src/main/res/values/strings.xml` and `app/src/main/res/values-es/strings.xml`.
- [x] 3.3 Implement `FiberDiagnosticScreen.kt` using Material 3 Expressive tokens, traffic light gauge visualization, animated progress indicators per step, and manual retry action, and verify UI compilation with `./gradlew :app:assembleDebug`.
- [x] 3.4 Register `fiber-diag` page in `Catalog.kt` and wire `HeroStatusCard` in `MainActivity.kt` to launch the Fiber Assistant, verifying navigation and build with `./gradlew :app:assembleDebug`.

## 4. Integration Verification & Spec Coherence

- [x] 4.1 Execute full test suite across both modules via `./gradlew testDebugUnitTest :protocol:test` to verify zero regressions.
- [x] 4.2 Validate OpenSpec change coherence and schema compliance via `openspec validate fiber-diagnostic-assistant`.

## 5. Hybrid OID Querying & Non-Blocking Graceful Degradation

- [x] 5.1 Expand `OpticalTelemetryParser.kt` key dictionary to support TR-181 standard properties (`opticalSignalLevel`, `transmitOpticalLevel`, `rxOpticalPower`, `txOpticalPower`, `rxPwr`) and verify with unit tests in `./gradlew :protocol:test`.
- [x] 5.2 Implement hybrid `getList`/`get` resolution in `ClientTelemetrySource` across `DEV2_OPTC_GPON_CFG`, `DEV2_GPON_INTF_STATS`, and `DEV2_OPTC_INTF`, and introduce non-blocking degradation in `FiberDiagnosticEngine.kt` when WAN is connected, verifying with unit tests in `./gradlew :protocol:test`.
- [x] 5.3 Verify multi-module unit tests and build debug APK with `./gradlew testDebugUnitTest :app:assembleDebug`.

## 6. Null/Zero Metric Disambiguation & Three-Tier Summary

- [x] 6.1 Update `OpticalTelemetryParser.kt` to treat `0.0 dBm` (and non-negative dummy metrics) as unexposed telemetry (`UNKNOWN`), reserving `SATURATED` strictly for `-8.0 dBm < rxDbm < 0.0 dBm`, and add unit test coverage in `./gradlew :protocol:test`.
- [x] 6.2 Update `FiberDiagnosticEngine.kt` to compute three-tier overall diagnostic outcome (`SUCCESS`, `WARNING`, `FAILED`) with tailored summary messages when non-blocking steps have warnings, and verify with unit tests in `./gradlew :protocol:test`.
- [x] 6.3 Add bilingual strings for the warning summary state in `values/strings.xml` and `values-es/strings.xml`.
- [x] 6.4 Update `FiberDiagnosticScreen.kt` `OverallSummaryCard` to display amber warning styling, warning icon, and tailored strings when the outcome is `WARNING`.
- [x] 6.5 Execute multi-module test suite and assemble fresh debug APK with `./gradlew testDebugUnitTest :app:assembleDebug`.

