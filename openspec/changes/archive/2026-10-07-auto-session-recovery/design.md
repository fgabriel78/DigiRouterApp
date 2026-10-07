# Design

## Context

See `proposal.md` for problem background. The router's embedded web server drops idle TCP sockets and invalidates authentication sessions after a period of inactivity (`SESSION_TIMEOUT`). When an Android app resumes from the background, OkHttp attempts to reuse dead sockets from its `ConnectionPool`, yielding `unexpected end of stream` exceptions. When a request reaches the router after the session expires, the router returns cleartext redirects or errors, leaving the user trapped on an error screen with a broken retry button.

## Goals / Non-Goals

**Goals:**
- Eliminate raw technical transport exceptions (`unexpected end of stream`) from the UI.
- Evict stale idle connections when the application returns to the foreground.
- Transparently re-authenticate against the router and retry failed requests when credentials are available in memory or secure storage.
- Coordinate concurrent re-authentication attempts with a Mutex to prevent request stampedes against the router.
- Gracefully redirect to `LoginScreen` with a clear, localized message (`LoginError.SessionExpired`) if silent recovery fails or credentials are unavailable.

**Non-Goals:**
- Modifying router firmware timeout settings.
- Storing passwords to disk when the user unchecked "Remember password" (in-memory credentials survive only while the app process is alive).

## Decisions

### 1. Connection Pool Hygiene & Client Configuration
- **Decision**: Configure `RouterClient` with an explicit `ConnectionPool` having a short keep-alive duration (15 seconds, matching embedded router timeouts) and provide `evictIdleConnections()`.
- **Rationale**: Routers typically close keep-alive sockets after 10–15 seconds of inactivity. Evicting idle connections on `Activity.onResume` and keeping pool timeouts short prevents OkHttp from attempting to reuse closed TCP sockets.

### 2. Transparent Session Recovery in `RouterViewModel`
- **Decision**: Store the active password in memory inside `RouterViewModel` during the active session (regardless of whether "Remember password" is saved to disk). Introduce `executeWithRecovery<T>` wrapped with a `Mutex`:
  ```kotlin
  suspend fun <T> executeWithRecovery(block: suspend () -> T): T
  ```
  - If `block()` throws an exception matching session termination or socket stream closure:
    1. Evict idle connections.
    2. Acquire recovery lock.
    3. Execute `client.login(password, username)` to establish a fresh session.
    4. Re-execute `block()`.
  - If re-login fails or recovery is exhausted:
    1. Invalidate session (`loggedIn = false`).
    2. Set `error = LoginError.SessionExpired`.
- **Alternatives considered**:
  - *OkHttp Authenticator / Interceptor*: OkHttp `Authenticator` only handles HTTP 401 status codes. The TP-Link router returns 200 with cleartext HTML, non-zero JSON error codes, or terminates the connection at the TCP level (`unexpected end of stream`), which requires protocol-aware recovery rather than standard HTTP 401 handling.
  - *Direct manual user prompt on every timeout*: Interrupts the user every time they switch apps; transparent recovery provides a seamless mobile experience.

### 3. Session Expiration Diagnostic & Fallback UI
- **Decision**: Add `LoginError.SessionExpired` to `LoginError` enum.
- In `MainActivity.kt` (`LoginScreen`):
  Display a friendly warning row when `state.error == LoginError.SessionExpired`:
  - English: *"Your session has ended due to inactivity. Please sign in again."*
  - Spanish: *"Tu sesión ha finalizado por inactividad. Inicia sesión de nuevo."*
- If the password was remembered or previously entered, it remains pre-filled, enabling the user to restore their session with a single tap.

## Risks / Trade-offs

- **[Risk] Multiple parallel requests (e.g. Dashboard queries) attempting simultaneous re-login**
  → *Mitigation*: Protect session recovery with a Coroutine `Mutex` so the first failing request performs the re-login while subsequent requests wait and reuse the newly established session.
- **[Risk] Infinite recovery loops on repeated failures**
  → *Mitigation*: Limit recovery to a single re-login attempt per user action. If the retried call fails again, treat it as unrecoverable and trigger logout immediately.
- **[Risk] False positives treating normal network dropouts as session expiration**
  → *Mitigation*: Differentiate true connection unreachable errors (host not found, Wi-Fi disconnected) from session/stream termination.
