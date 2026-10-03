# storage-services Specification

## Purpose
Manages network file sharing protocols, anonymous access rules, WAN exposure settings, and DLNA media streaming for USB mass storage devices attached to the router.

## Requirements

### Requirement: Samba Network File Sharing
The system SHALL configure the router's embedded Samba (SMB) server for local network folder sharing.

#### Scenario: Configure SMB server parameters
- **GIVEN** the Samba section (`DEV2_SMB_SERVICE`)
- **WHEN** the user enables the service
- **THEN** the system permits configuring the NetBIOS server name (1-15 characters), toggling `shareAll` to expose all storage partitions, and toggling `anonymous` access permissions.

### Requirement: FTP File Server Management
The system SHALL configure the embedded FTP server, custom listening port, and remote WAN access permissions.

#### Scenario: Configure FTP server parameters
- **GIVEN** the FTP section (`DEV2_FTP_SERVER`)
- **WHEN** the user configures the server
- **THEN** the system permits setting server name (1-15 chars), custom TCP port (1-65535, defaulting to 21), anonymous access toggle, and partition sharing.

#### Scenario: Toggle FTP access from Internet
- **GIVEN** the FTP server section
- **WHEN** the user modifies `accessFromInternet`
- **THEN** the system displays a clear security warning that allowing FTP from the Internet exposes private files externally, before persisting the setting.

### Requirement: DLNA Media Streaming Server
The system SHALL configure the router's DLNA media server for network audio, video, and image streaming.

#### Scenario: Enable DLNA media server
- **GIVEN** the DLNA section (`DEV2_DLNA_MEDIA_SERVER`)
- **WHEN** the user modifies `serverState`
- **THEN** the system permits updating the broadcast media server display name (1-32 characters) and toggling whole-disk media sharing.
