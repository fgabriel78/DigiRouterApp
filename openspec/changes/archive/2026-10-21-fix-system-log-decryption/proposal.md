# Proposal

## Why
When the system log monitoring feature was introduced in v1.13.0, retrieving event logs in real-world environments resulted in an empty log view ("Sin eventos de registro"). While logs are plainly visible in the router's web console, the app receives nothing because the router's firmware (`INCLUDE_LOGIN_GDPR_ENCRYPT`) mandates AES-CBC encryption for `/cgi/log` (listed in the router's internal `checkUrlTable`). Because `RouterClient.getSystemLogs()` fetched `/cgi/log` with `decrypt = false`, the raw Base64-encoded ciphertext failed return-code validation (`toIntOrNull() == null`), causing `LogParser.parse` to prematurely return an empty list on every polling cycle.

Fixing this decryption requirement restores real-time log monitoring for network administrators and power users diagnosing router events.

## What Changes
- Update `RouterClient.getSystemLogs()` in `:protocol` to retrieve `/cgi/log` with `decrypt = true`, automatically handling router AES-CBC decryption while safely falling back to plaintext.
- Enhance `LogParser` in `:protocol` regex matching to robustly support module names containing hyphens, underscores, or numbers (`[\w.-]+`) and flexible whitespace.
- Deduplicate log entry identifiers in `LogParser` and safeguard Compose list keys in `SystemLogScreen` to prevent collision crashes on identical log lines.
- Update unit tests in `RouterClientLogTest` to verify that encrypted `/cgi/log` responses are correctly decrypted and parsed using active session crypto, alongside existing plaintext test vectors.

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `system-log`: Clarifies and ensures that system log extraction transparently handles AES-encrypted response payloads produced by GDPR-encrypted router sessions.

## Impact
- **Architecture**:
  - `:protocol`: Modifies `RouterClient.kt` (`getSystemLogs`) to enable decryption; hardens `LogParser.kt` regex parsing; updates `RouterClientLogTest.kt` with encrypted payload tests.
  - `:app`: No UI or ViewModel API changes required; the existing `SystemLogScreen` and `SystemLogViewModel` will now populate live log entries as intended.
- **Dependencies & APIs**: No external dependencies added. Reuses existing `GdprCrypto.aesDecryptBase64` already present in `:protocol`.
