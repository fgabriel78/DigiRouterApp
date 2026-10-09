# Tasks

## 1. Security Engine & Port Risk Catalog (`:protocol`)

- [x] 1.1 Implement `RiskyPortCatalog` defining dangerous ports (21, 22, 23, 25, 53, 80, 137-139, 445, 1433, 3306, 3389, 5432, 5900, 6379, 27017) and verifying classification of gaming/WebRTC ports as safe via `./gradlew :protocol:test`.
- [x] 1.2 Implement `SecurityAuditEngine`, `SecurityCheck`, `SecurityReport`, `SecurityFinding`, and `RemediationAction` in `es.routerapp.protocol.security`.
- [x] 1.3 Implement comprehensive unit tests in `SecurityAuditEngineTest` verifying baseline 100-point score, severity deductions (DMZ, open Wi-Fi, WPS, guest LAN access, DoS, UPnP, FTP/SMB), score clamping, and batch remediation plan generation via `./gradlew :protocol:test`.

## 2. Localization & ViewModel Orchestration (`:app`)

- [x] 2.1 Add bilingual string resources for all security checks, severities, risk explanations, and remediation actions in `app/src/main/res/values/strings.xml` and `app/src/main/res/values-es/strings.xml`.
- [x] 2.2 Implement `SecurityHealthViewModel` to manage audit state, filter selections (`All`, `At Risk`, `Passed`), individual action triggers, and batch remediation workflows.
- [x] 2.3 Implement unit tests for `SecurityHealthViewModel` state transformations and remediation execution via `./gradlew :app:testDebugUnitTest`.

## 3. Dedicated Security Health UI (`:app`)

- [x] 3.1 Implement `SecurityHealthScreen` in Jetpack Compose featuring an animated circular score gauge, semantic health badges, and status filter chips.
- [x] 3.2 Implement expandable security finding cards displaying severity badges, plain-language risk explanations, and 1-tap individual fix actions.
- [x] 3.3 Implement the "Resolver todo con 1 toque" confirmation BottomSheet dialog previewing all automated actions with sequential execution feedback.

## 4. Dashboard Integration & End-to-End Verification (`:app`)

- [x] 4.1 Implement `SecurityAlertBanner` in `DashboardScreen` (`StatusScreens.kt`) to render high-priority warnings when critical vulnerabilities exist, providing an immediate fix action.
- [x] 4.2 Implement `SecuritySummaryCard` in `DashboardScreen`'s bento grid displaying the current score, health status, and navigation to the audit screen.
- [x] 4.3 Register navigation routes for `SecurityHealthScreen` in `MainActivity.kt` and verify clean compilation and passing unit tests across both modules via `./gradlew testDebugUnitTest`.
