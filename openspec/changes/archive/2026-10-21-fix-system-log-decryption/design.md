# Design

## Context
See `proposal.md` for background and motivation.

In the router's embedded firmware (`INCLUDE_LOGIN_GDPR_ENCRYPT`), the router maintains `checkUrlTable` in its client-side JavaScript (`js_gdprProxy.js` / `js_proxy.js`):
```javascript
var checkUrlTable = [
    {"url":"/cgi_gdpr", "flag":1},
    {"url":"/cgi/log", "flag":1},
    {"url":"/cgi/route", "flag":1},
    {"url":"/frame/menu.cgi", "flag":1}
];
```
When GDPR encryption is active on the router, responses to `GET /cgi/log` are AES-128-CBC encrypted (base64 encoded). The clearing endpoint (`/cgi/log?clr`) is explicitly exempted from response encryption in the router's web client (`s.url.match("clr") == null`).

In `:protocol`, `RouterClient.rawGet(path, decrypt = true)` uses `crypto.aesDecryptBase64(text)` wrapped in `runCatching`, falling back safely to plaintext if decryption fails or crypto is unavailable. Setting `decrypt = true` for `getSystemLogs()` provides complete compatibility with real encrypted router responses while preserving backwards compatibility with unencrypted mock tests or older firmware.

## Goals / Non-Goals

**Goals:**
- Enable transparent AES decryption for `/cgi/log` in `RouterClient.getSystemLogs()`.
- Strengthen `LogParser` regular expressions to accept variations in module names (including hyphens and numbers) and flexible whitespace.
- Add test coverage verifying that encrypted responses are correctly decrypted by `RouterClient` and parsed into `LogEntry` entities.

**Non-Goals:**
- Changing clearing behavior (`clearSystemLogs()` remains unencrypted as per router specification).
- Modifying UI components in `:app` (`SystemLogScreen` and `SystemLogViewModel` already handle data when received).

## Architecture & Data Flow

```
+-------------------------------------------------------------+
|               Router Firmware (/cgi/log)                    |
|    - Buffer of syslog events (oldest -> newest)             |
|    - AES-128-CBC encrypted base64 payload                   |
+-------------------------------------------------------------+
                              |
                     GET /cgi/log with TokenID
                              v
+-------------------------------------------------------------+
|                      :protocol Module                       |
|                                                             |
|   +-----------------------------------------------------+   |
|   |                    RouterClient                     |   |
|   | - rawGet("/cgi/log", decrypt = true)                |   |
|   | - AES-128-CBC decryption via GdprCrypto             |   |
|   | - Fallback to raw text if unencrypted / crypto null |   |
|   +--------------------------+--------------------------+   |
|                              | Decrypted Plaintext          |
|                              v                              |
|   +-----------------------------------------------------+   |
|   |                      LogParser                      |   |
|   | - Validates return code '0'                         |   |
|   | - Regex: ^([\d-]+\s+[\d:]+)\s+\[(\d)\]\s+([\w.-]+): |   |
|   | - Emits List<LogEntry> (reversed: newest first)     |   |
|   +-----------------------------------------------------+   |
+-------------------------------------------------------------+
                              |
                   List<LogEntry> via Flow
                              v
+-------------------------------------------------------------+
|                        :app Module                          |
|   SystemLogViewModel -> StateFlow -> SystemLogScreen (M3)   |
|   (Filter chips, live ticker, search query, module list)    |
+-------------------------------------------------------------+
```

## Decisions

### 1. Enable `decrypt = true` in `RouterClient.getSystemLogs()`
- **Decision**: Update `val raw = rawGet("/cgi/log", decrypt = true)` in `getSystemLogs()`.
- **Rationale**: The router's firmware includes `/cgi/log` in `checkUrlTable`. In authenticated sessions with `admin` or `user`, the router transmits the buffer as an AES-encrypted Base64 string. `rawGet` already contains the decryption logic via `crypto.aesDecryptBase64(text)`.
- **Alternative considered**: Decrypting manually inside `getSystemLogs()`. Rejected because `rawGet` already centralizes the `decrypt: Boolean` contract with `runCatching` fallback across the codebase (e.g. `/frame/menu.cgi`).

### 2. Retain `decrypt = false` for `clearSystemLogs()`
- **Decision**: Keep `clearSystemLogs()` calling `/cgi/log?clr` with `decrypt = false`.
- **Rationale**: The router web interface (`js_proxy.js`) explicitly checks `s.url.match("clr") == null` before attempting decryption. The `/cgi/log?clr` response is a 2-byte plaintext `0\r\n` without AES framing.

### 3. Broaden `LogParser` Regex for Module Names
- **Decision**: Change module matching pattern from `\s+(\w+):` to `\s+([\w.-]+):` and spacing from `\s` to `\s+`.
- **Rationale**: While standard modules like `MESH`, `DHCPD`, and `WIFI` use word characters, daemons such as `TR-069`, `MINI-UPNPD`, or `802.11` may contain hyphens or dots. Using `[\w.-]+` prevents valid log entries from being discarded silently.

## Edge Cases & Mitigations

- **[Unencrypted or Mock Environments]** → If tests or older firmware return unencrypted plaintext, `runCatching` inside `rawGet` catches the `IllegalArgumentException` / `BadPaddingException` and falls back directly to the original plaintext.
- **[Empty or Blank Buffer]** → If the log buffer is cleared or has only the status code `0`, `LogParser` continues to return `emptyList()` cleanly without errors.
- **[Corrupted Ciphertext or Padding Error]** → If AES decryption fails due to corrupted packet data, `rawGet` falls back to the raw string; `LogParser` then checks `lines[0].toIntOrNull() == 0`, failing safely to `emptyList()` without crashing the ViewModel ticker loop.

## Risks / Trade-offs

- **[Performance Overhead]** → AES decryption of a ~2-4 KB text payload takes < 1 ms on JVM / Android and runs on `Dispatchers.IO`, presenting no risk of UI jank or excessive battery usage on the 3-second polling cadence.
