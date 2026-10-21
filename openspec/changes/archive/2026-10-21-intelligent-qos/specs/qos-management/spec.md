# qos-management Specification

## Purpose

Enables network administrators to configure Intelligent QoS bandwidth caps, allocate High, Middle, and Low priority queue percentages, prioritize individual client devices for latency protection during bandwidth saturation, and inspect hardware Game Port clients.

## ADDED Requirements

### Requirement: Admin-Restricted QoS Navigation
The system SHALL expose Intelligent QoS within the Local Network (`NETWORK`) group exclusively to users authenticated with administrative credentials.

#### Scenario: Admin user navigation
- **WHEN** user logs in with the username "admin"
- **THEN** Intelligent QoS is visible and navigable under the Local network category

#### Scenario: Non-admin user access restriction
- **WHEN** user logs in with standard non-admin credentials
- **THEN** Intelligent QoS is excluded from the visible catalog and inaccessible

### Requirement: Global QoS Toggle and Bandwidth Cap Management
The system SHALL read and update the global QoS engine state (`DEV2_IQOS`), including the master enable toggle, upstream total bandwidth cap and unit (`Mbps` or `Kbps`), and downstream total bandwidth cap and unit (`Mbps` or `Kbps`), while preserving existing companion attributes on save.

#### Scenario: Loading global QoS configuration
- **WHEN** administrator opens the Intelligent QoS screen
- **THEN** the system queries `DEV2_IQOS` and displays the current enable state, upstream bandwidth value and unit (`1` = Mbps, `2` = Kbps), downstream bandwidth value and unit, and priority queue percentages

#### Scenario: Validating and saving bandwidth caps
- **WHEN** administrator enables or disables QoS and saves positive integer upstream and downstream bandwidth caps within the supported range (`1..10000`)
- **THEN** the system writes the updated attributes to `DEV2_IQOS` while preserving existing companion attributes (`linkType`, `iptv*`, `voIP*`, `guest*`)

#### Scenario: Rejecting out-of-range bandwidth values
- **WHEN** administrator enters a blank, non-numeric, zero, negative, or out-of-range (`> 10000`) bandwidth cap
- **THEN** the system rejects the save operation and displays a validation error message without dispatching a router write

### Requirement: Coupled Priority Queue Allocation Sliders
The system SHALL enforce coupled 5%-step constraints across High, Middle, and Low priority queue percentage allocations such that `High >= 15%`, `Middle >= 10%`, `Low >= 5%`, `High > Middle > Low`, and `High + Middle + Low <= 100%`.

#### Scenario: Adjusting priority queue sliders within valid bounds
- **WHEN** administrator adjusts the High, Middle, or Low priority percentage slider in 5% increments
- **THEN** the system dynamically constrains the remaining sliders so that `Middle <= min(High - 5, 100 - High - Low)`, `Low <= min(Middle - 5, 100 - High - Middle)`, and `High <= 100 - Middle - Low`, displaying both percentage and effective bandwidth per tier

#### Scenario: Rejecting invalid queue percentage combinations
- **WHEN** a QoS configuration save is attempted with queue percentages that violate step, minimum, ordering (`High > Middle > Low`), or total sum (`<= 100%`) constraints
- **THEN** the system fails validation and prevents writing invalid queue allocations to `DEV2_IQOS`

### Requirement: Live Device Discovery Join and Priority Tier Assignment
The system SHALL join `DEV2_IQOS_RULE` (`type = 11`) with discovered LAN/Wi-Fi clients and local device metadata to display each device's priority tier and allow administrators to assign devices to High (`4`), Middle (`3`), or Low (`2`) priority.

#### Scenario: Displaying default unassigned devices in Middle priority
- **WHEN** a connected LAN or Wi-Fi device has no matching `DEV2_IQOS_RULE` entry and is not an active Game Port client
- **THEN** the system displays the device with its resolved alias, category icon, IP, and MAC address as assigned to Middle Priority (default)

#### Scenario: Promoting a default device to High or Low priority
- **WHEN** administrator assigns an unassigned (default Middle) device to High (`4`) or Low (`2`) priority
- **THEN** the system adds a new `DEV2_IQOS_RULE` instance with `type = 11`, the device's MAC address, hostname, and target `class_`

#### Scenario: Reassigning a device between High and Low priority
- **WHEN** administrator changes a device that already has a `DEV2_IQOS_RULE` entry to a different non-default priority tier
- **THEN** the system deletes the existing `DEV2_IQOS_RULE` instance by `stack` and adds a new `DEV2_IQOS_RULE` instance with the updated `class_`

#### Scenario: Returning a prioritized device to default Middle priority
- **WHEN** administrator sets a prioritized device back to Middle priority or deletes its rule
- **THEN** the system deletes the corresponding `DEV2_IQOS_RULE` instance by `stack` so the device returns to default Middle priority

### Requirement: Hardware Game Port Client Detection
The system SHALL query `DEV2_GAME_QOS_CLIENT` with graceful degradation and display active hardware Game Port clients (`gamePortClient = 1`, `active = 1`, `isAgentDevice != 1`) as pinned to High Priority.

#### Scenario: Displaying active Game Port client
- **WHEN** `DEV2_GAME_QOS_CLIENT` returns an active non-agent client connected via the hardware Game Port
- **THEN** the system displays the device pinned in High Priority with a Game Port indicator and prevents demoting its priority tier in software

#### Scenario: Graceful degradation when Game QoS OID is unsupported
- **WHEN** querying `DEV2_GAME_QOS_CLIENT` fails or returns an error on firmware without Game Console support
- **THEN** the system degrades gracefully to an empty Game Port client list without failing the QoS screen load

### Requirement: Router Configuration NVRAM Persistence
The system SHALL commit QoS configuration and rule changes to router non-volatile memory (`ACT_SAVE_CFG`) so prioritization settings survive router reboots.

#### Scenario: Committing changes on QoS save or rule mutation
- **WHEN** administrator saves `DEV2_IQOS` settings or adds, updates, or deletes a `DEV2_IQOS_RULE` entry
- **THEN** the system executes `ACT_SAVE_CFG` on the router
