# Tasks

## 1. Strings and UI Guidance

- [x] 1.1 Update `login_busy_warning` in `app/src/main/res/values/strings.xml` and `app/src/main/res/values-es/strings.xml` to provide neutral, non-assertive concurrency guidance, and verify strings compile cleanly.
- [x] 1.2 Review `LoginScreen` in `app/src/main/kotlin/es/digirouter/app/MainActivity.kt` to ensure the disclaimer is rendered with appropriate styling and layout alignment.

## 2. Login Resilience and Auto-Retry

- [x] 2.1 Decouple `c.busy()` in `RouterViewModel.login()` using `runCatching` and implement a transparent single auto-retry (~500ms backoff) for recoverable transport exceptions (`IOException`, socket resets, stream EOF) before surfacing errors to `state.error`.
- [x] 2.2 Add unit tests in `app/src/test/kotlin/es/digirouter/app/RouterViewModelTest.kt` verifying transparent single-retry success on transient transport exceptions and confirming that non-recoverable errors (wrong password, cleartext policy) do not retry.

## 3. Verification

- [x] 3.1 Run `./gradlew testDebugUnitTest` and verify all tests pass across `:protocol` and `:app`.
- [x] 3.2 Run `openspec validate robust-login-session-handling --strict` and verify that all change artifacts adhere strictly to OpenSpec standards.
