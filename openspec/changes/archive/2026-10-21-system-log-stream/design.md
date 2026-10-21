# Design

## Context

See `proposal.md` for user motivation and business rationale.

The TP-Link XGB430v Pro running ESDIGI firmware stores event logs in a circular RAM buffer exposed via `/cgi/log`. Unlike modern web applications with WebSocket or SSE streams, the router uses raw HTTP endpoints. Administration settings for the logging daemon live in TR-181 OID `DEV2_SYSLOG_CFG`.

The project is structured into two modules:
- `:protocol`: A pure-JVM module with `RouterClient` wrapping OkHttp and crypto operations.
- `:app`: An Android Jetpack Compose application module using Material 3 Expressive, ViewModels, and Coroutines.

## Goals / Non-Goals

**Goals:**
- Provide a responsive, real-time log monitor in `:app` that polls the router on a 3-second cadence and mirrors the router's active buffer.
- Implement robust parsing in `:protocol` that isolates log model contracts from Android dependencies.
- Enable direct control over the router's internal logging sensitivity (`DEV2_SYSLOG_CFG.localSeverity`).
- Support immediate client-side filtering by severity, dynamic module, and search text without re-querying the router.
- Integrate seamlessly into the existing `Catalog` and `MainActivity` navigation stack under the `admin` role.

**Non-Goals:**
- Continuous background logging or Android push notifications when the app is closed.
- Persistent database storage of router logs across app sessions (the app explicitly mirrors the active router buffer).
- Remote syslog server hosting on the phone (avoiding open UDP port 514 listeners and battery drain).

## Architecture & Data Flow

```
+-------------------------------------------------------------------------+
|                  Router: In-Memory Event Ring Buffer                    |
|                        (/var/log circular buffer)                       |
+-------------------------------------------------------------------------+
                                    |
          GET /cgi/log (Every 3s)   |   GET /cgi/log?clr (On Clear)
          POST /cgi_gdpr?9 (Syslog) |
                                    v
+-------------------------------------------------------------------------+
|                          :protocol Module                               |
|                                                                         |
|   +---------------------+        +----------------------------------+   |
|   |    RouterClient     | =====> |            LogParser             |   |
|   | - getSystemLogs()   |        | - Validates return code '0'      |   |
|   | - clearSystemLogs() |        | - Regex: (\S+\s\S+) \[(\d)\] ... |   |
|   | - getSyslogConfig() |        | - Produces List<LogEntry>        |   |
|   | - setLocalSeverity()|        | - Reverses (newest at top)       |   |
|   +---------------------+        +----------------------------------+   |
+-------------------------------------------------------------------------+
                                    |
                                    v
+-------------------------------------------------------------------------+
|                            :app Module                                  |
|                                                                         |
|   +-----------------------------------------------------------------+   |
|   |                      SystemLogViewModel                         |   |
|   | - Coroutine ticker loop (delay: 3000ms, paused via StateFlow)   |   |
|   | - Mirrors current router window without local accumulation      |   |
|   | - Dynamic module discovery: distinctBy { it.module }.sorted()   |   |
|   | - Filter pipeline: logs -> filter(severity) -> filter(module)   |   |
|   |                    -> filter(query) -> filteredLogs             |   |
|   +---------------------------------+-------------------------------+   |
|                                     |                                   |
|                                     v StateFlow<SystemLogUiState>       |
|   +-----------------------------------------------------------------+   |
|   |                     SystemLogScreen (Compose)                   |   |
|   | - Pulsing green Live indicator / Paused badge                   |   |
|   | - Filter chips: [All] [Errors] [Warnings] [Info] [Debug]        |   |
|   | - Dynamic module dropdown / chips                               |   |
|   | - LazyColumn with color-coded severity badges & monospace text  |   |
|   | - BottomSheet for DEV2_SYSLOG_CFG sensitivity tuning            |   |
|   | - Confirmation dialog for buffer clearing                       |   |
|   +-----------------------------------------------------------------+   |
+-------------------------------------------------------------------------+
```

## Decisions

### 1. Polling Cadence vs. Remote UDP Syslog Server
- **Decision**: Use a 3-second HTTP polling loop (`GET /cgi/log` with `TokenID`) rather than turning the phone into a remote syslog UDP listener.
- **Rationale**: The router's embedded web server handles authenticated GET calls on `/cgi/log` with negligible CPU cost. Configuring the router's `DEV2_SYSLOG_CFG.serverIP` to the phone's LAN IP would introduce network fragility (DHCP IP shifts, battery saver killing socket listeners, Android Wi-Fi sleep policies).
- **Alternative considered**: Remote syslog UDP server on Android. Rejected due to complexity, firewall requirements, and battery impact.

### 2. Mirror Window vs. Local Session Accumulation
- **Decision**: Strictly mirror the router's active buffer window. When new entries push older ones off the router's memory, the app reflects the router's current state.
- **Rationale**: User explicit requirement during exploration. This keeps memory usage constant and guarantees consistency between what the router diagnostics report and what the app displays.
- **Alternative considered**: Appending to an unbounded in-memory session list. Rejected to prevent unbounded memory growth and stale state desynchronization.

### 3. Log Parsing and Error Isolation in `:protocol`
- **Decision**: Build `LogParser` and data models (`LogEntry`, `LogSeverity`, `SyslogConfig`) inside `:protocol` as pure Kotlin classes.
- **Rationale**: Preserves architectural modularity and enables 100% JVM unit test coverage with synthetic router responses without Android mocks or emulators.

### 4. Admin Role Gate in Navigation
- **Decision**: Mark the `system-log` page with `adminOnly = true` in `Catalog.kt`, and route it in `MainActivity.kt` to `SystemLogScreen`.
- **Rationale**: `userRole == 2` accounts will never see the screen in the navigation drawer or category groups, preventing 403 / permission rejection errors from the router.

## State Machine & Polling Transitions

```
+------------------+         User Enters Screen
|     Stopped      | -------------------------------------> [Active / Polling]
+------------------+                                              |     ^
         ^                                           Tap Pause    |     | Tap Resume
         |                                                        v     |
  User Leaves Screen                                       [ Paused State ]
         |                                                        |
         +--------------------------------------------------------+
```

## Edge Cases & Mitigations

- **Session Expiry or Transport Disruption**:
  - *Risk*: Router drops session after timeout or redirects cleartext HTTP.
  - *Mitigation*: The polling coroutine catches exceptions via `RecoveryRunner` and `RouterClient.isSessionOrStreamFailure()`. If a session fails, it pauses polling and triggers the app-wide session recovery / re-auth flow without crashing.
- **Malformed Log Lines**:
  - *Risk*: Third-party daemon writes unformatted ASCII or debug prints directly into the syslog ring buffer.
  - *Mitigation*: Regex matches are non-fatal. Lines failing the regex are omitted from the structured list while valid entries remain intact.
- **High Sensitivity Buffer Churn**:
  - *Risk*: Setting sensitivity to `Debug` generates rapid bursts of messages that scroll past quickly.
  - *Mitigation*: User can tap the Live button to freeze the view at any time for thorough inspection.
- **Dark Theme Accessibility**:
  - *Risk*: Low contrast on log severity badges in dark mode.
  - *Mitigation*: Use Material 3 Expressive semantic container colors (`errorContainer`, `tertiaryContainer`, `secondaryContainer`) with their respective `on*Container` content colors to guarantee WCAG AAA contrast in both light and dark themes.

## Risks / Trade-offs

- **[Router Concurrency Lock]** → If another browser session logs in as admin, the router will revoke the app's `TokenID`. The app's existing `isSessionOrStreamFailure` handler will detect this and navigate back to login.
- **[Network Bandwidth on Wi-Fi]** → Polling `/cgi/log` every 3 seconds consumes ~2-4 KB per request. Mitigated by terminating the coroutine job in `onCleared` / when navigating away from `SystemLogScreen`.
