# remote-management Specification

## Purpose

Enables administrative control over router terminal daemons, web management ports, SSL certificate installation, and read-only inspection of operator TR-069 ACS remote management configurations.

## Requirements

### Requirement: SSH Daemon Control
The system SHALL display and update router SSH daemon parameters, including local enable status, local listening port, remote WAN enable status, remote listening port, and idle session lifetime.

#### Scenario: Save SSH daemon parameters
- **WHEN** an administrator modifies the SSH local port or enables the local SSH daemon and saves
- **THEN** the system SHALL send the updated configuration to the router via `DEV2_SSH_CFG` and confirm success

#### Scenario: Warn on unconstrained remote SSH exposure
- **WHEN** an administrator enables remote WAN SSH without specifying restricted CIDR host subnets
- **THEN** the system SHALL display a high-risk security warning banner highlighting WAN brute-force exposure risks

### Requirement: Telnet Daemon Control
The system SHALL display and update router Telnet daemon parameters, including local enable status, local listening port, remote WAN enable status, and remote listening port.

#### Scenario: Save Telnet daemon parameters
- **WHEN** an administrator toggles the local Telnet switch or updates the Telnet port and saves
- **THEN** the system SHALL persist the changes to the router via `DEV2_TELNET_CFG` and update the active screen state

### Requirement: Web Management Ports and SSL Parameters
The system SHALL allow configuring local and remote listening ports for unencrypted HTTP and encrypted HTTPS web administration and setting the web session inactivity timeout.

#### Scenario: Update web management ports
- **WHEN** an administrator updates the local HTTPS port from 443 to a custom port within 1024-65535 and saves
- **THEN** the system SHALL persist the updated port mapping via `DEV2_HTTP_CFG`

#### Scenario: Display certificate status and serial number
- **WHEN** the remote management overview loads
- **THEN** the system SHALL display whether a local SSL certificate exists along with its serial number

### Requirement: CA Certificate Retrieval and Native Installation
The system SHALL download the router's CA root certificate binary and launch Android's native certificate installer.

#### Scenario: Trigger native Android KeyChain installer
- **WHEN** an administrator taps the Install CA Certificate action
- **THEN** the system SHALL fetch the certificate binary from `/cgi/https?downCACert` and launch the Android KeyChain install intent with the certificate bytes

### Requirement: Operator TR-069 Remote Management Audit
The system SHALL display a read-only audit of ISP remote management parameters, including CWMP ACS URL, bound WAN interface, periodic inform interval, connection request port, and operator-whitelisted management subnets.

#### Scenario: Inspect active TR-069 ACS configuration
- **WHEN** an administrator opens the remote management screen
- **THEN** the system SHALL display the operator ACS URL, the bound GPON interface (`pon.2`), inform interval, and operator remote subnets without exposing modification controls

#### Scenario: Display TR-369 support status
- **WHEN** inspecting the remote management audit section
- **THEN** the system SHALL show an inactive status badge indicating TR-369 is not enabled in the current router firmware build

### Requirement: Administrative Role Restriction
The system SHALL restrict visibility and navigation to the remote management screen exclusively to authenticated administrators.

#### Scenario: Hide from standard user role
- **WHEN** a user logs in with the standard `user` role
- **THEN** the remote management item SHALL NOT appear in the navigation catalog

#### Scenario: Allow admin access
- **WHEN** a user logs in with the `admin` role
- **THEN** the remote management item SHALL appear under System settings and navigate to the dedicated screen
