# Proposal

## Why

Users frequently encounter a confusing "No se puede contactar con el router" error on their initial login attempt, requiring an immediate second tap on "Iniciar sesión" which then succeeds. This is exacerbated by a persistent, misleading warning ("Hay otra sesión de administración abierta en el router. Si continúas, se cerrará.") rendered unconditionally on the login screen. The router's embedded web server resets TCP sockets when terminating concurrent sessions or during cold transport warm-up, and the app prematurely fails rather than performing an automated, transparent single-retry.

## What Changes

- **Neutral Session Concurrency Guidance**: Replace the confusing, assertively phrased `login_busy_warning` ("Hay otra sesión...") with a neutral informational disclaimer in both Spanish and English explaining that the router architecture permits only one concurrent administrator session.
- **Login Handshake Auto-Retry**: Implement a single, transparent automatic retry (with a ~500ms backoff) in `RouterViewModel` when a transient network exception, TCP connection reset, EOF, or socket closure occurs during initial credential authentication.
- **Resilient Pre-Login Probe**: Ensure optional pre-login checks (`c.busy()`) do not abort or fail the primary authentication flow if `/cgi/getBusy` encounters transient errors or socket contention.

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `auth-session`: Updates the single-session concurrency requirement to specify neutral, non-assertive concurrency guidance, and enhances the user authentication flow with transparent single-attempt recovery for transient socket resets during credential authentication.

## Impact

- `:app`:
  - `MainActivity.kt`: Update the login screen disclaimer display and styling.
  - `RouterViewModel.kt`: Add retry logic for transient transport failures during `login()`, and decouple `c.busy()` failure from blocking authentication.
  - `strings.xml` and `values-es/strings.xml`: Update `login_busy_warning` string resources with clear, neutral phrasing.
  - `RouterViewModelTest.kt`: Unit tests verifying automatic single-retry on transient `IOException`/socket resets and proper error propagation when retries are exhausted.
- `:protocol`: No breaking API changes; `RouterClient.login()` and `RouterClient.isSessionOrStreamFailure()` continue to serve as the core protocol primitives.
