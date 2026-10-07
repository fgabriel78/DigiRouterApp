# Proposal

## Why

When the mobile app has been open in the background for an extended period, the TP-Link router's session inactivity timer terminates the connection and invalidates the session. When the user resumes the app, stale connection pooling produces raw technical errors ("unexpected end of stream"), trapping the user on dead-end error screens without a seamless way to recover or re-authenticate.

## What Changes

- Add connection pool hygiene: evict stale or dead idle connections from OkHttp's connection pool when the app returns from the background to the foreground.
- Add transparent session re-authentication: when a router request encounters a dropped connection, socket reset, or expired session error, automatically attempt a silent re-login using active in-memory or remembered credentials and retry the failed request.
- Add graceful session expiration fallback: if silent re-authentication fails or credentials are not available, automatically transition the user to the login screen with a friendly localized notification ("Your session has ended due to inactivity. Please sign in again").
- Enhance error categorization: map low-level stream/transport termination errors to clean session expiration diagnostics instead of raw socket exception messages.

## Capabilities

### New Capabilities
*(None)*

### Modified Capabilities
- `auth-session`: Update session lifecycle requirements to include automatic transparent session recovery upon connection drop/expiry and graceful fallback to the login screen with localized session expiration notices.

## Impact

- **Affected code**:
  - `protocol`: `RouterClient` (connection pool eviction helper / retry interceptor / session error detection).
  - `app`: `RouterViewModel` (silent re-login orchestration, session recovery lifecycle, session expired state), `PageScreen` & `StatusScreens` (recovery trigger integration), `MainActivity` & `LoginScreen` (session expired banner presentation), and localized strings.
- **Dependencies & APIs**: No new third-party dependencies.
- **Breaking changes**: None.
