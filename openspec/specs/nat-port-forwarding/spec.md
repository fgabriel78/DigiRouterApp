# nat-port-forwarding Specification

## Purpose
Manages Network Address Translation (NAT) rules, inbound virtual server port forwarding, application port triggering, DMZ host exposure, UPnP discovery/mapping status, and Application Layer Gateways (ALG).

## Requirements

### Requirement: Virtual Servers Port Forwarding CRUD
The system SHALL manage static inbound port forwarding rules mapped to internal local hosts.

#### Scenario: Add new virtual server rule
- **GIVEN** the virtual servers section (`DEV2_PORTMAPPING`)
- **WHEN** the user creates a rule with description, external port (1-65535), internal port (1-65535), internal client IPv4, and protocol (TCP, UDP, or TCP and UDP)
- **THEN** the engine resolves the `@activeWan` connection dynamically, applies default attributes (`enable="1"`, `X_TP_AddrType="0"`), and submits an `ao` operation.

#### Scenario: Delete virtual server rule
- **GIVEN** an existing virtual server port mapping
- **WHEN** the user deletes the rule
- **THEN** the system issues a `do` operation targeting the rule instance stack.

### Requirement: Port Triggering Configuration CRUD
The system SHALL manage dynamic port triggering rules for applications requiring bidirectional port opening upon outgoing traffic.

#### Scenario: Create port triggering rule
- **GIVEN** the port triggering section (`DEV2_PORTTRIGGERING`)
- **WHEN** the user adds a rule specifying application name, trigger port, trigger protocol, open port or port range (e.g., `5000-5010`), and open protocol
- **THEN** the system binds the rule to `@activeWan` and submits an `ao` operation.

#### Scenario: Delete port triggering rule
- **GIVEN** an existing port triggering entry
- **WHEN** the user deletes the entry
- **THEN** the system executes a `do` operation to remove the rule from the router.

### Requirement: DMZ Host Configuration
The system SHALL configure an isolated DMZ host IP to expose a single local machine directly to external traffic.

#### Scenario: Enable DMZ host
- **GIVEN** the DMZ section (`DEV2_DMZ_HOST_CFG`)
- **WHEN** the user toggles `enable` and inputs a valid internal IPv4 address
- **THEN** the system displays a security warning that the host will be exposed without port filtering, and saves the configuration.

### Requirement: Universal Plug and Play (UPnP) Management
The system SHALL enable or disable automatic UPnP port redirection and display active dynamic port mappings.

#### Scenario: Toggle UPnP service
- **GIVEN** the UPnP section (`DEV2_UPNP_CFG`)
- **WHEN** the user toggles `enable`
- **THEN** the system updates the UPnP daemon state.

#### Scenario: Inspect active UPnP mappings
- **GIVEN** active port mappings requested by local devices (`DEV2_UPNP_PORTMAPPING`)
- **WHEN** the UPnP page renders
- **THEN** the system displays the dynamic mapping table in read-only mode showing all returned attributes.

### Requirement: Application Layer Gateway (ALG) Toggles
The system SHALL configure protocol-specific ALG pass-through filters.

#### Scenario: Toggle ALG pass-through flags
- **GIVEN** the ALG configuration section (`DEV2_ALG_CFG`)
- **WHEN** the user configures protocol switches
- **THEN** the system updates individual switches for PPTP, L2TP, IPSec, FTP, TFTP, H.323, RTSP, and SIP.
