# vpn-servers Specification

## Purpose
Enables hosting and managing home VPN servers (OpenVPN, IPSec, and PPTP) on the router, with cryptographic key management, live connected client monitoring, Carrier-Grade NAT (CGNAT) and DDNS readiness detection, and one-tap `.ovpn` mobile client profile export with endpoint host override.

## Requirements

### Requirement: OpenVPN Server Configuration Management
The system SHALL allow authenticated admin users to read and update OpenVPN server parameters via `DEV2_OPENVPN`.

#### Scenario: Read OpenVPN server configuration
- **WHEN** an admin navigates to the OpenVPN settings
- **THEN** the system retrieves and displays the enable toggle, protocol (UDP/TCP), port number, tunnel subnet, netmask, and client access mode (LAN only vs Internet and LAN).

#### Scenario: Update OpenVPN server configuration
- **WHEN** the user modifies OpenVPN parameters with valid port (1-65535) and IPv4 subnet/mask and saves
- **THEN** the system transmits the modified attributes to `DEV2_OPENVPN` and confirms successful persistence.

### Requirement: OpenVPN Cryptographic Key Regeneration
The system SHALL support regenerating OpenVPN certificates and cryptographic keys on the router via `ACT_OP_OVPN_REGEN_KEYS`.

#### Scenario: Confirm and trigger key regeneration
- **WHEN** the user requests key regeneration and confirms the revocation warning dialog
- **THEN** the system executes `ACT_OP_OVPN_REGEN_KEYS` with an indeterminate progress state until the router completes key generation.

### Requirement: OpenVPN Client Profile Download and Endpoint Customization
The system SHALL retrieve the OpenVPN client profile from the router, permit endpoint host override, and export the file to the Android platform.

#### Scenario: Download profile and detect endpoint
- **WHEN** the user initiates `.ovpn` profile export
- **THEN** the system fetches `/cgi/openvpn?cliOvpnDown` and parses the default `remote` directive from the profile text.

#### Scenario: Override remote host before sharing
- **WHEN** the user provides an alternate hostname or domain override in the export dialog
- **THEN** the system replaces the `remote <host>` line in the `.ovpn` content with the user-specified host before exporting.

#### Scenario: Share profile via Android Share Sheet
- **WHEN** the user confirms profile export
- **THEN** the system writes the `.ovpn` file to a secure cache directory and dispatches an `ACTION_SEND` Intent with a `FileProvider` URI targeting VPN client apps or storage.

### Requirement: IPSec and PPTP Server Configuration
The system SHALL allow admin users to inspect and configure IPSec tunnels and legacy PPTP server settings.

#### Scenario: Configure IPSec server settings
- **WHEN** the user navigates to the IPSec tab and updates tunnel settings
- **THEN** the system writes updated attributes to `DEV2_IPSEC` and `DEV2_IPSEC_CFG`.

#### Scenario: Configure PPTP server settings
- **WHEN** the user enables PPTP or updates the client IP pool and authentication settings
- **THEN** the system persists changes to `DEV2_PPTP`.

### Requirement: Live VPN Client Session Monitoring
The system SHALL query and display active remote VPN connections with virtual IP assignments and session durations.

#### Scenario: Display active connected clients
- **WHEN** the user opens the Connected Clients tab
- **THEN** the system retrieves the active client table and displays each connected client's identifier, assigned virtual IP, remote endpoint, and connected duration.

#### Scenario: Empty client state
- **WHEN** no remote clients are connected
- **THEN** the system displays an empty state indicator with a message indicating zero active tunnels.

### Requirement: Network Environment and CGNAT Readiness Detection
The system SHALL evaluate the router's WAN IP address and DDNS configuration status to provide an incoming VPN connectivity assessment.

#### Scenario: Detect CGNAT without active DDNS
- **WHEN** the WAN IP falls within the CGNAT range (`100.64.0.0/10`) and DDNS is not active
- **THEN** the system displays a warning banner informing the user that external incoming connections require public IP or DDNS, providing a direct link to the DDNS configuration page.

#### Scenario: Detect active DDNS or public IP
- **WHEN** the router has an active DDNS domain or a public WAN IPv4 address
- **THEN** the system displays a confirmation banner indicating incoming VPN connectivity is ready.
