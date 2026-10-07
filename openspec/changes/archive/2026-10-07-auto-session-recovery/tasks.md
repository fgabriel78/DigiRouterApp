# Tasks

## 1. Transport and Protocol Recovery Support (`:protocol` module)

- [x] 1.1 Configure `RouterClient` with a customized `ConnectionPool` having a 15-second keep-alive and expose `evictIdleConnections()`, verified with `./gradlew.bat :protocol:compileKotlin`.
- [x] 1.2 Implement session and stream failure detection helper (`isSessionOrStreamFailure`) identifying socket stream terminations, cleartext redirect replies, and session timeout codes, verified with unit tests in `./gradlew.bat :protocol:test`.

## 2. ViewModel Session Recovery and Lifecycle (`:app` module)

- [x] 2.1 Implement in-memory active credential tracking and `executeWithRecovery<T>` protected by a `Mutex` in `RouterViewModel` to orchestrate transparent re-login and request retry.
- [x] 2.2 Add `LoginError.SessionExpired` to `LoginError` enum and update `mapLoginError` to categorize session timeouts, verified with unit tests in `RouterViewModelTest`.
- [x] 2.3 Wire `MainActivity` lifecycle to invoke `client.evictIdleConnections()` when the activity resumes (`ON_RESUME`).

## 3. UI Integration and Localization

- [x] 3.1 Add `login_session_expired` string resources to `values/strings.xml` and `values-es/strings.xml`.
- [x] 3.2 Update `LoginScreen` in `MainActivity.kt` to display a friendly warning banner when returning with `LoginError.SessionExpired`.
- [x] 3.3 Wrap data fetching and mutations in `DashboardScreen`, `DevicesScreen`, and `GenericPageScreen` with session recovery to ensure transparent retries.

## 4. End-to-End Verification

- [x] 4.1 Run full project compilation and unit tests with `./gradlew.bat test compileDebugSources` to verify complete build and test integrity.
