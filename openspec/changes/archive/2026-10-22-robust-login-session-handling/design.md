# Design

## Context

See `proposal.md` for motivation. Currently, `RouterViewModel.login()` executes a sequential chain (`c.busy()`, `c.login()`, `c.refreshToken()`). When another administration session is active or the device's local network transport is in cold-start, the router's embedded web server drops or resets the active TCP connection as it purges the prior session. Because `RouterViewModel` treats any immediate `IOException` as a fatal failure, the user is presented with a red connection error ("No se puede contactar con el router") alongside an unconditional, misleading warning stating that another session is open. Tapping the login button a second time succeeds because the previous session was already invalidated on the router by the first attempt.

## Goals / Non-Goals

**Goals:**
- Eliminate the need for users to tap "Iniciar sesión" a second time by introducing an automated, transparent single-retry on transient network/socket resets during login.
- Prevent non-critical pre-login checks (`c.busy()`) from failing the authentication flow.
- Clarify UI guidance by rewording `login_busy_warning` into a neutral informational notice about the router's single-admin architecture.

**Non-Goals:**
- Multi-user concurrency: The router firmware is inherently single-session; the app cannot maintain concurrent active sessions on the router.
- Modifying the underlying protocol cryptographic handshake in `:protocol`.

## Decisions

### Decision 1: Transparent Single Auto-Retry for Transient Transport Errors

In `RouterViewModel.login()`, when an authentication attempt fails due to a recoverable transport failure, execute a single automatic retry after a brief pause (~500ms).

```
                      LOGIN HANDSHAKE FLOW
+---------------------------------------------------------------+
| User taps [Iniciar sesión]                                    |
+---------------------------------------------------------------+
                               |
                               v
               +-------------------------------+
               | Attempt 1: c.login(password)  |
               +-------------------------------+
                               |
            +------------------+------------------+
            | Success                             | Error (e)
            v                                     v
+-----------------------+              +-----------------------+
| Navigate to Dashboard |              | Is recoverable        |
+-----------------------+              | transport failure?    |
                                       +-----------------------+
                                           |               |
                                    Yes    |               | No (e.g. wrong pwd)
                                           v               v
                             +-------------------+  +-------------------+
                             | Delay 500ms       |  | Surface error     |
                             | Attempt 2: login  |  | to UI             |
                             +-------------------+  +-------------------+
                                       |
                            +----------+----------+
                            | Success             | Error
                            v                     v
                 +-----------------------+  +-------------------+
                 | Navigate to Dashboard |  | Surface error     |
                 +-----------------------+  +-------------------+
```

- **Recoverable errors**: `IOException` (including socket closed, connection reset, broken pipe, unexpected end of stream), and `RouterClient.isSessionOrStreamFailure(e)`.
- **Non-recoverable errors**: `RouterException` with invalid credentials (e.g., error 71233), `SecurityException` / `UnknownServiceException` (cleartext policy restrictions).
- **Alternative considered**: Requiring manual user retry. *Rejected* because it creates friction and confusion when the second tap consistently succeeds.

### Decision 2: Decouple `c.busy()` from Login Execution

Currently, `val busy = c.busy()` runs before `c.login()`. If the router is momentarily unresponsive to `/cgi/getBusy`, the entire login fails even if credentials and `/cgi/login` would have succeeded.

- Wrap `runCatching { c.busy() }` so that pre-login busy status querying is best-effort.
- If it fails, default `otherSessionActive` to `false` and proceed directly to `c.login()`.
- **Alternative considered**: Completely remove `c.busy()`. *Rejected* because reading the lock status provides useful telemetry/state if ever rendered post-login, but it should not gate authentication.

### Decision 3: Neutral Session Limitation Wording

Replace the assertive text in resource files:
- Spanish (`values-es/strings.xml`):
  - *From*: `"Hay otra sesión de administración abierta en el router. Si continúas, se cerrará."`
  - *To*: `"Nota: El router solo admite una sesión de administración activa. Si hay otra abierta, se cerrará al entrar."`
- English (`values/strings.xml`):
  - *From*: `"Another administration session is open on the router. Continuing will close it."`
  - *To*: `"Note: The router permits only one active administration session. Signing in will close any existing session."`

## Risks / Trade-offs

- **[Risk]** A truly unreachable router (e.g. phone completely disconnected from Wi-Fi) takes ~500ms longer to show the error due to the single retry.
  → *Mitigation*: 1 retry with 500ms delay adds minimal perceived latency while dramatically improving reliability for on-network users.
- **[Risk]** Exhausted retries still produce `LoginError.Unreachable`.
  → *Mitigation*: If the router is genuinely unreachable after 2 attempts, `LoginError.Unreachable` is the accurate diagnosis.
