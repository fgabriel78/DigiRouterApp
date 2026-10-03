# network-lan-dhcp Specification

## Purpose
Configures the router's local area network addressing parameters, the integrated DHCPv4 server pool, static IP-to-MAC hardware reservations, and third-party Dynamic DNS provider accounts.

## Requirements

### Requirement: Local IP and Subnet Configuration
The system SHALL configure the router's local gateway IP address, subnet mask, and IGMP snooping behavior.

#### Scenario: Update router LAN IP and subnet
- **GIVEN** the local network section (`DEV2_ADT_LAN`)
- **WHEN** the user modifies `IPAddress` or `IPSubnetMask`
- **THEN** the system validates IPv4 formatting and displays a warning that changing the IP address may interrupt network access until client DHCP leases renew.

#### Scenario: Toggle IGMP snooping
- **GIVEN** the local network section
- **WHEN** the user toggles `IGMPSnoopEnabled`
- **THEN** the system updates the multicast snooping state on the local bridge.

### Requirement: DHCPv4 Server Pool Management
The system SHALL configure the DHCP server address pool, lease times, and network provisioning attributes.

#### Scenario: Configure DHCP server pool boundaries
- **GIVEN** DHCPv4 server enabled (`DHCPv4Enable == "1"`)
- **WHEN** the user modifies pool parameters
- **THEN** the system allows setting `DHCPv4MinIPAddress`, `DHCPv4MaxIPAddress`, lease time between 120 and 864000 seconds, custom gateway (`DHCPv4IPRouters`), domain name, and DNS server addresses.

#### Scenario: Disable DHCP server
- **GIVEN** an active DHCP server
- **WHEN** the user toggles `DHCPv4Enable` to disabled (`"0"`)
- **THEN** pool-specific fields are conditionally hidden from the view and disabled on the router.

### Requirement: Static DHCP Address Reservations CRUD
The system SHALL support creating, listing, and deleting permanent IP-to-MAC address bindings within the DHCP server pool.

#### Scenario: Create new static IP reservation
- **GIVEN** the DHCP reservations section (`DEV2_DHCPV4_POOL_STATICADDR`)
- **WHEN** the user submits an `AddSpec` with client MAC (`chaddr`) and reserved IP (`yiaddr`)
- **THEN** the engine validates MAC and IPv4 formats, resolves the parent pool (`X_TP_MainPool == "1"` on `DEV2_DHCPV4_SERVER_POOL`), and submits an `ao` operation with `enable = "1"`.

#### Scenario: Delete static IP reservation
- **GIVEN** an existing DHCP reservation instance
- **WHEN** the user triggers deletion
- **THEN** the system issues a `do` operation targeting the instance's stack identifier.

### Requirement: Dynamic DNS Account Integration
The system SHALL configure DynDNS and No-IP dynamic DNS provider credentials using an atomic login/logout transaction hook.

#### Scenario: Connect to dynamic DNS service
- **GIVEN** a dynamic DNS section (`DEV2_DYN_DNS_CFG` or `DEV2_NOIP_DNS_CFG`)
- **WHEN** the user toggles `enable` to `"1"` with username, password, and domain
- **THEN** the `ddnsHook` transforms the payload into an atomic login write (`enable="1"`, `login="1"`, `userName`, `password`, `userDomain`), displaying a warning to disconnect conflicting providers first.

#### Scenario: Disconnect dynamic DNS service
- **GIVEN** an active dynamic DNS service connection
- **WHEN** the user toggles `enable` to off
- **THEN** the `ddnsHook` clears credentials and transmits `login="0"` to log out from the provider.
