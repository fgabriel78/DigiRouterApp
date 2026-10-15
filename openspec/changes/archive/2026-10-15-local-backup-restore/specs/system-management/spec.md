# Spec Delta

## ADDED Requirements

### Requirement: Manual Router Configuration Backup Download
The system SHALL provide a manual action to download the current router configuration binary snapshot to local phone storage.

#### Scenario: Successful configuration download and local persistence
- **GIVEN** an authenticated router session
- **WHEN** the user triggers "Crear copia de seguridad"
- **THEN** the client queries device information (`DEV2_DEV_INFO`) to resolve the router backup endpoint (`/cgi/{modelName}V{modelVersion}...bin` or `/cgi/conf.bin`), fetches the binary configuration payload, and writes it to app-private storage.

### Requirement: Local Backup History and FIFO Retention
The system SHALL maintain an indexed history of up to 5 local configuration backups with automatic FIFO pruning and export/deletion controls.

#### Scenario: Automatic naming and metadata tracking
- **GIVEN** a freshly downloaded configuration file
- **WHEN** the backup is saved to the local library
- **THEN** the system generates an automatic display name with local date and time ("Copia dd/MM/yyyy HH:mm"), records metadata (file size, timestamp, device model and firmware build), and persists the metadata index.

#### Scenario: Strict 5-slot FIFO pruning
- **GIVEN** a backup library already containing 5 saved backups
- **WHEN** the user creates a new 6th backup
- **THEN** the system automatically deletes the oldest backup file and its metadata entry from storage, maintaining at most 5 entries.

#### Scenario: Manual entry management
- **GIVEN** one or more backups in the local list
- **WHEN** the user selects "Compartir" or "Eliminar" on an entry
- **THEN** the system dispatches an Android share intent with the `.bin` file or prompts for deletion confirmation and removes the file.

### Requirement: Router Configuration Restore and Reboot Lifecycle
The system SHALL restore a selected local backup to the router with high-risk confirmation and monitored reboot recovery.

#### Scenario: High-risk confirmation dialog
- **GIVEN** a saved backup in the local list
- **WHEN** the user taps "Restaurar"
- **THEN** the system displays a confirmation dialog warning that the router will restore settings and restart, interrupting network connectivity for 1-2 minutes.

#### Scenario: Multipart upload and reboot orchestration
- **GIVEN** user confirmation on the restore dialog
- **WHEN** the restore sequence begins
- **THEN** the client sends a multipart POST to `/cgi/confup` containing the `.bin` payload under parameter `filename`, invokes `/cgi/bnr` to trigger application, and displays a reboot countdown progress overlay with automated reconnect detection.
