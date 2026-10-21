# Tasks

## 1. Protocol Data Models & Engine (:protocol)

- [x] 1.1 Define data structures in `es.routerapp.protocol.parental` (`ParentalProfile`, `ProfileDevice`, `TimeLimitConfig`, `FilterCategories`, `WebInsights`, `BlockedHistoryEntry`, `VisitedDomain`) and verify bitmask conversions and serialization via unit tests in `./gradlew :protocol:test --tests "es.routerapp.protocol.parental.*"`.
- [x] 1.2 Implement `ParentalControlEngine` in `:protocol` for profile listing, adding, editing, deleting, pause toggling, and flash commit (`ACT_SAVE_CFG`), and verify mock wire calls in `ParentalControlEngineTest`.
- [x] 1.3 Implement Web Insights telemetry and blocked access log fetching in `ParentalControlEngine` with composite stack date resolution (`DEV2_PC_VISITRANK`, `DEV2_PC_VISITRANK_ENTRY`, `DEV2_PC_BLOCKHISTORY_ENTRY`, `DEV2_PC_UNBLOCKEDLIST`), and verify query parsing via unit tests in `ParentalControlInsightsTest`.

## 2. Catalog Registration & Navigation Routing

- [x] 2.1 Register `Page(id = "parental-control", title = "Parental Controls", group = Group.SECURITY, adminOnly = true, custom = true)` in `Catalog.kt`, and verify role-based filtering via `CatalogTest` (accessible to `admin`, hidden for `user`).
- [x] 2.2 Register navigation destination for `"parental-control"` in `MainActivity.kt` pointing to `ParentalControlScreen`, verifying routing succeeds for admin sessions.

## 3. ViewModel & Live Device Picker Integration (:app)

- [x] 3.1 Implement `ParentalControlViewModel` managing UI states (`Loading`, `Success`, `Error`), profile pause toggling, profile persistence, and insights caching, and verify state emission in unit tests.
- [x] 3.2 Build `LiveDevicePicker` composable consuming `ConnectedDevices` data to display active network clients with hostnames, vendor icons, and selection checkboxes.

## 4. Profile Management & Editor UI (:app)

- [x] 4.1 Implement `ParentalControlScreen` featuring Material 3 Expressive Bento cards with child profile avatar, assigned device count, quick pause/resume switch, and screen time progress bar.
- [x] 4.2 Implement `ProfileEditorBottomSheet` providing multi-section editing: profile name & age preset, live device picker, weekday vs weekend bedtime/quota sliders, and category filter toggles + custom domain blacklist manager.
- [x] 4.3 Add bilingual string definitions in `app/src/main/res/values/strings.xml` and `app/src/main/res/values-es/strings.xml` covering all profile attributes, schedules, categories, and error messages.

## 5. Web Insights & Blocked History UI (:app)

- [x] 5.1 Implement `WebInsightsSheet` displaying date selector (Today, Yesterday, 2 days ago), screen time usage bar, and top visited domains ranking.
- [x] 5.2 Implement blocked access attempts list with 1-tap unblock action calling `ParentalControlEngine.unblockUrl` and dynamically updating item status.

## 6. End-to-End Verification & Validation

- [x] 6.1 Run full build and unit test suite via `./gradlew testDebugUnitTest` and verify zero compilation errors or test failures.
- [x] 6.2 Validate change schema compliance via `openspec validate parental-controls --strict` and confirm all requirements and scenarios are satisfied.
