# Proposal

## Why

Modifying critical router settings (such as Wi-Fi credentials, LAN subnets, DHCP ranges, or port forwards) carries operational risk: an accidental mistake can lock users out of their network or disrupt connected home devices. The router firmware (`backNRestore.htm`) supports downloading and restoring configuration snapshots, but this functionality is not yet available in the mobile application.

Adding local backup and restore enables users to manually snapshot their working router configuration directly into the phone's storage before applying sensitive changes, and restore any of the last 5 saved snapshots whenever necessary.

## What Changes

- Add a dedicated **Copia y restauración** ("Backup & Restore") screen within the **Sistema** group in the app.
- Provide a manual "Crear copia de seguridad ahora" trigger that downloads the router configuration binary (`.bin`) from the router and stores it in the app's local private storage.
- Automatically format backup names using localized date and timestamp (e.g., "Copia 15/10/2026 23:15").
- Enforce strict FIFO retention keeping at most the **5 most recent copies**, automatically deleting the oldest when a 6th is generated.
- Allow restoring any of the 5 stored local backups back to the router, protected by a high-risk confirmation dialog explaining connection interruption and rebooting.
- Implement the restore upload (`POST /cgi/confup`), restore trigger (`/cgi/bnr`), and automated reboot monitoring with countdown and reconnect detection.
- Allow sharing/exporting and manually deleting individual backup entries from the local list.
- Explicitly exclude external file pickers: restore operations operate exclusively on the managed local history.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `system-management`: Adds requirements for manual router configuration backup downloads, 5-slot local FIFO retention and metadata tracking, and router configuration restore with reboot lifecycle orchestration.

## Impact

- `:protocol`:
  - `RouterClient`: Add binary download support (`downloadBackup(): ByteArray`) resolving dynamic endpoint `/cgi/{modelName}V{modelVersion}...bin` or `/cgi/conf.bin`, and multipart upload with restore trigger (`restoreBackup(data: ByteArray)`).
  - `Catalog.kt`: Register `backup` page under `Group.SYSTEM`.
  - `SpanishTexts.kt`: Add localized UI strings for backup, restore, retention, and reboot warnings.
- `:app`:
  - `BackupStore`: Local persistence manager for `.bin` files in `context.filesDir/backups/`, JSON metadata indexing (timestamp, size, device firmware), and FIFO pruning to 5 items.
  - `BackupRestoreViewModel`: State management for download, restore, sharing, deletion, and reboot status polling.
  - `BackupRestoreScreen`: Compose screen with Material 3 Expressive layout (action hero card, 5-item backup list, high-risk confirmation dialog, reboot countdown progress).
  - `MainActivity.kt`: Navigation route for `backup` screen.
  - Bilingual string resources in `values/strings.xml` and `values-es/strings.xml`.
