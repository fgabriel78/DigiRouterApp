# Design

## Context

See `proposal.md` for background and motivation. The router firmware (`main_backNRestore.htm`) implements backup downloads by querying device model metadata from `DEV2_DEV_INFO` and generating a dynamic binary URL `/cgi/{modelName}V{modelVersion}{buildDate}{buildTime}n.bin` (falling back to `/cgi/conf.bin`). Restoring requires a multipart HTTP POST to `/cgi/confup` with part name `filename`, immediately followed by a script call to `/cgi/bnr` (`$.cgi("/cgi/bnr")`), which triggers an unauthenticated system reboot of 60–120 seconds while applying settings.

In the mobile app, `:protocol` communicates with the router via OkHttp, handling OID requests and session management. `:app` uses Jetpack Compose with Material 3 Expressive.

## Goals / Non-Goals

**Goals:**
- Provide a clean, Material 3 Expressive UI screen in `Group.SYSTEM` for manual configuration backup and restoration.
- Provide binary download and multipart restore methods in `:protocol` that conform to router firmware semantics.
- Store up to 5 backups locally in the app's internal private storage with strict FIFO pruning.
- Provide high-risk confirmation, an animated reboot countdown, and automated reconnect polling upon restore.
- Support sharing/exporting and manual deletion of backup files.

**Non-Goals:**
- Automatic background backups prior to individual edits on other screens (user explicitly required 100% manual triggering).
- Picking arbitrary `.bin` files from external storage / Storage Access Framework (SAF file picker explicitly excluded; restores operate strictly on local history).
- Editing or inspecting the proprietary binary payload of the `.bin` configuration file.

## Architecture & Flows

```text
+--------------------------------------------------------------------------+
|                               :app Module                                |
|                                                                          |
|  [ MainActivity / Navigation ] ---> [ BackupRestoreScreen (Compose) ]    |
|                                                     |                    |
|                                        (State / Actions)                 |
|                                                     v                    |
|                                        [ BackupRestoreViewModel ]        |
|                                              |            |              |
|                             (Save / List / Prune)    (Download / Upload) |
|                                              |            |              |
|                                              v            v              |
|                                       +-------------+  +---------------+ |
|                                       | BackupStore |  | RouterClient  | |
|                                       +-------------+  +---------------+ |
|                                              |                 |         |
|                          (context.filesDir)  v                 |         |
|                       [ backups/ + index.json ]                |         |
+----------------------------------------------------------------|---------+
                                                                 |
                                              HTTP Binary Stream |
                                                                 v
                                                     +---------------------+
                                                     | Router (HTTP Engine)|
                                                     |  GET  /cgi/*.bin    |
                                                     |  POST /cgi/confup   |
                                                     |  POST /cgi/bnr      |
                                                     +---------------------+
```

### State Machine & Reboot Lifecycle

```text
[ IDLE ] --( Tap "Crear copia" )--> [ DOWNLOADING ]
                                           |
                                  (Success: save & prune)
                                           v
                                       [ IDLE ] (5-slot list updated)

[ IDLE ] --( Tap "Restaurar" )----> [ CONFIRMATION_DIALOG ]
                                           |
                                  (User confirms)
                                           v
                                  [ UPLOADING_PAYLOAD ] (/cgi/confup)
                                           |
                                      (Trigger /cgi/bnr)
                                           v
                                  [ REBOOT_COUNTDOWN ] (90s timer)
                                           |
                                  (Poll http://router:80/ every 3s)
                                           v
                              +------------+------------+
                              |                         |
                       (Router Responds)         (Timeout reached)
                              v                         v
                       [ RECONNECTED ]           [ REBOOT_TIMEOUT ]
                              v                         v
                       (Navigate to Login)       (Prompt retry/check Wi-Fi)
```

## Decisions

### 1. Endpoint Resolution and Binary Transfer in `:protocol`
- **Choice**: Extend `RouterClient` with `downloadBackup(): ByteArray` and `restoreBackup(bytes: ByteArray)`.
- **Rationale**: Firmware `main_backNRestore.htm` conditionally resolves:
  `action = "/cgi/" + modelName.replace(" ", "") + "V" + modelVersion.split(".")[0] + buildDate + buildTime + "n"`
  and appends `.bin` (or `_backupcfg.bin` if role != admin). If metadata is unavailable, it falls back to `/cgi/conf.bin`. Encapsulating this resolution and raw OkHttp byte streaming in `:protocol` preserves modularity.
- **Alternatives considered**:
  - *Delegating HTTP transfer directly to Android DownloadManager*: Rejected because the router endpoint requires authenticated session cookies and custom headers (`TokenID`, `User-Agent`, `Referer`) that DownloadManager does not reliably maintain.

### 2. Local Storage and 5-Slot FIFO Retention in `BackupStore`
- **Choice**: Store files in `context.filesDir.resolve("backups")` with a lightweight metadata index file `backups_index.json`.
- **Rationale**: App-private storage requires zero runtime storage permissions. When a new backup is saved, `BackupStore` reads the list; if `count >= 5`, the oldest entries by timestamp are deleted from disk and the index, guaranteeing that storage consumption never grows unbounded.
- **Alternatives considered**:
  - *Room Database*: Overkill for a strictly bounded 5-item list.
  - *Naming files with all metadata*: Fragile across locales and file systems; separate JSON index keeps timestamps, build tags, and file sizes clean.

### 3. Automatic Date/Time Naming
- **Choice**: Automatic display format based on local device date/time (e.g. `Copia 15/10/2026 23:15`), backed by ISO-8601 timestamps in the file name `backup_yyyyMMdd_HHmmss.bin`.
- **Rationale**: Meets user requirement of zero-friction one-tap backups without keyboard interruption.

### 4. Restore Safety and Reboot Lifecycle
- **Choice**: Dedicated full-screen or modal reboot progress overlay with a 90-second animated circular timer and periodic connection ping (`/cgi/getBusy` or base ping).
- **Rationale**: When restoring, the router abruptly terminates network connections. Without an explicit countdown, users might assume the app crashed or power-cycle the router prematurely while flash is being rewritten.

## Risks / Trade-offs

- **[Risk] Router session dropped during upload**: Network disconnect before `/cgi/bnr` finishes.
  → *Mitigation*: Verify HTTP 200 response on `/cgi/confup` before dispatching `/cgi/bnr`.
- **[Risk] Wi-Fi password or IP change in restored configuration**: If restored backup had different Wi-Fi credentials or IP subnet (e.g., `192.168.0.1` vs `192.168.1.1`), mobile phone disconnects from Wi-Fi.
  → *Mitigation*: Prominently highlight this warning in the confirmation dialog. When reconnect polling times out, guide user with instructions to reconnect to Wi-Fi.
- **[Risk] Large binary payload in memory**:
  → *Mitigation*: Router configuration binaries for TP-Link XGB430v are ~100–300 KB, easily fitting in heap memory without streaming complexity.

## Edge Cases

- **Offline State / Session Timeout**: If session expired before tapping "Crear copia", `RouterClient` throws `RouterException` session expired, triggering `RecoveryRunner` to re-authenticate seamlessly.
- **Dark Theme Contrast**: Bento cards, icon badges (`ShapeBadge`), and countdown indicators use Material 3 semantic tokens (`surfaceContainerHigh`, `errorContainer`, `onSurface`) for high contrast in dark mode.
