# Tasks

## 1. Protocol Models, Parser and Client Methods

- [x] 1.1 Create `LogSeverity`, `LogEntry`, and `SyslogConfig` data models in `protocol/src/main/kotlin/es/routerapp/protocol/log/LogModels.kt` and verify compilability with `./gradlew :protocol:compileKotlin`.
- [x] 1.2 Implement `LogParser` in `protocol/src/main/kotlin/es/routerapp/protocol/log/LogParser.kt` to decode router `/cgi/log` text into structured entities, handling status codes, regex matching, reverse ordering, and malformed line tolerance.
- [x] 1.3 Create unit tests in `protocol/src/test/kotlin/es/routerapp/protocol/log/LogParserTest.kt` validating standard entry parsing, empty responses, malformed lines, and severity mappings, verified via `./gradlew :protocol:test --tests "es.routerapp.protocol.log.LogParserTest"`.
- [x] 1.4 Add `getSystemLogs()`, `clearSystemLogs()`, `getSyslogConfig()`, and `setLocalSeverity()` to `RouterClient.kt`.
- [x] 1.5 Create unit tests in `protocol/src/test/kotlin/es/routerapp/protocol/RouterClientLogTest.kt` validating client log retrieval, clear invocation, and `DEV2_SYSLOG_CFG` serialization, verified via `./gradlew :protocol:test --tests "es.routerapp.protocol.RouterClientLogTest"`.

## 2. ViewModel and State Management

- [x] 2.1 Implement `SystemLogViewModel.kt` in `app/src/main/kotlin/es/digirouter/app/SystemLogViewModel.kt` featuring a 3-second polling ticker, buffer mirroring, dynamic module extraction, client-side filtering (severity, module, query), and sensitivity configuration.
- [x] 2.2 Create unit tests in `app/src/test/kotlin/es/digirouter/app/SystemLogViewModelTest.kt` validating 3-second cadence, pause/resume transitions, combined filtering, and sensitivity updates, verified via `./gradlew :app:testDebugUnitTest --tests "es.digirouter.app.SystemLogViewModelTest"`.

## 3. UI, Localization and Navigation

- [x] 3.1 Declare localized string resources for log viewer titles, filter chips, live states, clear dialogs, and sensitivity settings in `app/src/main/res/values/strings.xml` and `app/src/main/res/values-es/strings.xml`.
- [x] 3.2 Build `SystemLogScreen.kt` in `app/src/main/kotlin/es/digirouter/app/SystemLogScreen.kt` using Material 3 Expressive with live pulsing badge, search bar, severity chips, module selector, monospace log list, sensitivity bottom sheet, and share intent launcher.
- [x] 3.3 Register `system-log` page with `adminOnly = true` in `protocol/src/main/kotlin/es/routerapp/protocol/pages/Catalog.kt` and map route in `app/src/main/kotlin/es/digirouter/app/MainActivity.kt`.
- [x] 3.4 Add Spanish catalog translations for `System Log` in `protocol/src/main/kotlin/es/routerapp/protocol/i18n/SpanishTexts.kt` and verify via `./gradlew :protocol:test --tests "es.routerapp.protocol.CatalogI18nTest"`.

## 4. Verification and Integration

- [x] 4.1 Run full project unit test suite via `./gradlew testDebugUnitTest` and `./gradlew :protocol:test` to verify zero test failures or regressions.
- [x] 4.2 Validate OpenSpec change compliance via `openspec validate system-log-stream`.
