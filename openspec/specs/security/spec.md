# security Specification

## Purpose
Configures router firewall hardening mechanisms, Denial of Service (DoS) flood filtering rules, LAN ping response policies, and static IP-to-MAC ARP bindings.

## Requirements

### Requirement: Denial of Service (DoS) Attack Mitigation
The system SHALL configure attack mitigation thresholds and packet flood filtering on the router's firewall.

#### Scenario: Enable DoS defense filters
- **GIVEN** the DoS protection section (`DEV2_DDOS_CFG`)
- **WHEN** the user enables master protection (`enable = "1"`)
- **THEN** the system reveals and allows toggling ICMP flood filtering (`enableIcmpFilter`), UDP flood filtering (`enableUdpFilter`), and SYN flood filtering (`enableSynFilter`).

#### Scenario: Ignore ping requests from LAN
- **GIVEN** the DoS protection section
- **WHEN** the user modifies `forbidLanPing`
- **THEN** the system saves the setting to drop ICMP echo requests originating from local network devices.

### Requirement: IP-to-MAC Address Binding (ARP)
The system SHALL control enforcement of ARP table bindings to prevent ARP spoofing and unauthorized IP reassignment.

#### Scenario: Toggle ARP binding enforcement
- **GIVEN** the ARP binding section (`DEV2_ARP_BIND`)
- **WHEN** the user modifies the `enable` switch
- **THEN** the system writes the attribute to activate or deactivate kernel-level IP-to-MAC binding enforcement.
