# Proposal

## Why

Operating the DIGI Wi-Fi 7 router with `admin` credentials unlocks critical diagnostic tools previously hidden from the operator `user` account, chief among them the live system event log (`/cgi/log` and `DEV2_SYSLOG_CFG`). Currently, diagnosing network drops, Wi-Fi station disassociations, GPON OMCI link events, or DHCP assignment failures requires opening a desktop browser or guessing blind. Bringing a native, streaming, and filterable system log viewer directly into the Android app empowers network administrators and power users to troubleshoot router issues in real time without desktop access.

## What Changes

- Add a high-performance system log parser in `:protocol` that decodes router log output into structured `LogEntry` objects with timestamp, severity code, source module, and payload message.
- Add client methods in `:protocol` for querying the current log buffer (`GET /cgi/log`), clearing the router's log buffer (`GET /cgi/log?clr`), reading syslog sensitivity (`DEV2_SYSLOG_CFG`), and updating the local logging severity threshold.
- Add `SystemLogViewModel` in `:app` managing a 3-second polling lifecycle, mirroring the router's active window, handling pause/resume controls, and executing client-side filtering by severity, module, and query.
- Create `SystemLogScreen` in `:app` with Material 3 Expressive styling, featuring a live status indicator, instant search, severity chips, module filters, single-tap copy, share sheet export, buffer clear confirmation, and a sensitivity configuration modal.
- Register `system-log` in `Catalog.kt` as an `adminOnly` page in `Group.SYSTEM`, routing from `MainActivity` when logged in with the `admin` role.
- Provide full bilingual localization for English (`values/strings.xml`) and Spanish (`values-es/strings.xml`).

## Capabilities

### New Capabilities
- `system-log`: Captures, parses, streams, filters, and manages router system log events and syslog daemon severity configuration.

### Modified Capabilities
None.

## Impact

- `:protocol`:
  - New models and parser in `es.routerapp.protocol.log` (`LogSeverity`, `LogEntry`, `SyslogConfig`, `LogParser`).
  - New API methods on `RouterClient` for fetching logs, clearing logs, and reading/writing `DEV2_SYSLOG_CFG`.
  - Added unit tests verifying regex parsing across multiple severities and modules, buffer edge cases, and syslog config serialization.
- `:app`:
  - New screen `SystemLogScreen.kt` and ViewModel `SystemLogViewModel.kt` in `es.digirouter.app`.
  - Updated `MainActivity.kt` navigation to route `system-log` page ID.
  - New string resources in `res/values/strings.xml` and `res/values-es/strings.xml`.
  - Unit tests in `SystemLogViewModelTest.kt` verifying polling cadence, filter combinations, and sensitivity mutation.
- APIs & Dependencies: Uses existing OkHttp and coroutine infrastructure; no new third-party dependencies required.
