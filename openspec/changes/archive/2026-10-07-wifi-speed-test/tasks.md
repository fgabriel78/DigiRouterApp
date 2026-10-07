# Tasks

## 1. Core Speed Test Engine (`protocol` module)

- [x] 1.1 Implement data models (`SpeedTestPhase`, `SpeedTestProgress`, `SpeedTestResult`, `SpeedTestState`) in `es.routerapp.protocol.speedtest` and verify unit test compilation passes with `./gradlew.bat :protocol:compileKotlin`.
- [x] 1.2 Implement `SpeedTestEngine` with OkHttp streaming against Cloudflare speed test endpoints (Ping/Jitter probe, Download throughput stream, Upload throughput post, cancellation handling) and verify with unit tests via `./gradlew.bat :protocol:test`.

## 2. Catalog and Localization Integration

- [x] 2.1 Register the `speedtest` custom page under `Group.STATUS` in `Catalog.kt` and add English to Spanish translations in `SpanishTexts.kt`.
- [x] 2.2 Run `./gradlew.bat :protocol:test --tests es.routerapp.protocol.CatalogI18nTest` and verify all catalog strings pass translation coverage.

## 3. Speed Test User Interface (`app` module)

- [x] 3.1 Implement speed test state management in ViewModel with reactive StateFlow, test start/stop controls, and previous results persistence.
- [x] 3.2 Implement `SpeedTestScreen.kt` with Material 3 Expressive UI components, real-time animated speedometer/gauge, phase indicators, and metrics summary card.
- [x] 3.3 Wire `speedtest` page navigation in `MainActivity.kt` (`AppNavigation`) and verify navigation back stack handling.

## 4. End-to-End Build and Verification

- [x] 4.1 Run full project compilation and unit tests with `./gradlew.bat test compileDebugSources` to verify complete build integrity.
