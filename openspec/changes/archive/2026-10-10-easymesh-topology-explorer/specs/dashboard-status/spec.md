# Spec Delta

## ADDED Requirements

### Requirement: Interactive EasyMesh Topology Explorer
The system SHALL provide a dedicated full-screen interactive network topology explorer screen displaying the gateway, primary router controller, backhaul connections, and satellite repeaters with their client counts.

#### Scenario: Render multi-node topology when satellites are active
- **GIVEN** an active network with one controller and one or more satellite nodes loaded from `ConnectedDevicesData`
- **WHEN** the user navigates to the topology explorer screen from the Dashboard or Devices screen
- **THEN** the screen renders a vertical hierarchy tree displaying the Internet gateway at the top, the primary router controller, visual connectors indicating backhaul medium, and satellite agent cards with their active client counts.

#### Scenario: Single-node gateway fallback when no satellites exist
- **GIVEN** a network with only the primary router and no active satellite nodes
- **WHEN** the user navigates to the topology explorer screen
- **THEN** the screen displays the primary router as the central hub alongside an informational notice indicating all devices connect directly to the router, and permits opening the client devices list for the router.

### Requirement: Backhaul Medium and Health Diagnostics
The system SHALL evaluate backhaul link metrics between satellite nodes and the controller to differentiate wired Ethernet from wireless Wi-Fi connections and diagnose link health with actionable recommendations.

#### Scenario: Ethernet wired backhaul
- **GIVEN** a satellite node reporting an `"Ethernet"` backhaul link type
- **WHEN** rendering the backhaul connector in the topology map
- **THEN** the connector displays wired LAN iconography, negotiated link speed, and indicates optimal high-stability backhaul status.

#### Scenario: Wi-Fi backhaul with optimal signal
- **GIVEN** a satellite node reporting a `"Wi-Fi"` backhaul link type with strong signal (level >= 3 or link rate >= 400 Mbps)
- **WHEN** rendering the backhaul connector in the topology map
- **THEN** the connector displays wireless iconography, signal strength, negotiated speed, and indicates good backhaul quality.

#### Scenario: Wi-Fi backhaul with weak signal advisory
- **GIVEN** a satellite node reporting a `"Wi-Fi"` backhaul link type with degraded signal (level < 3 or link rate < 150 Mbps)
- **WHEN** rendering the backhaul connector in the topology map
- **THEN** the connector displays an amber advisory badge warning of degraded backhaul and recommending moving the repeater closer to the primary router.

### Requirement: Per-Node Modal Device Inspection
The system SHALL allow users to tap any node in the topology explorer to view all client devices associated with that node inside a modal bottom sheet.

#### Scenario: Display client devices in modal bottom sheet
- **GIVEN** a user viewing the topology explorer
- **WHEN** the user taps the primary router or any satellite node
- **THEN** a `ModalBottomSheet` opens displaying all active client devices connected to that specific node, featuring each client's category icon, friendly alias or hostname, IP address, and connection link rate or Wi-Fi signal level.

#### Scenario: Deep navigation to device detail sheet
- **GIVEN** a user inspecting client devices in the per-node bottom sheet
- **WHEN** the user taps an individual client device card
- **THEN** the system opens the `DeviceDetailSheet` allowing the user to view hardware metrics, edit aliases, or update categories.
