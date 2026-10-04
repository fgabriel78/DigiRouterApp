# dashboard-status Specification

## Purpose
Provides visual operational dashboards, hardware information, optical GPON link status, active WAN metrics, and connected device inspection with real-time filtering and wireless signal diagnostics.

## Requirements

### Requirement: System and Connection Overview
The system SHALL aggregate device information, GPON optical metrics, and active WAN connection status into an operational dashboard summary.

#### Scenario: Display device model and uptime
- **GIVEN** a query to `DEV2_DEV_INFO`
- **WHEN** the dashboard loads
- **THEN** the screen renders the router model name, hardware version, firmware version, and formatted uptime (days, hours, minutes).

#### Scenario: Display active WAN and public IP
- **GIVEN** a query to `DEV2_ADT_WAN` matching the active connection name from `activeWanName()`
- **WHEN** the WAN data is processed
- **THEN** the dashboard indicates connection status (Connected vs Disconnected), external IP address, default gateway, and DNS servers.

#### Scenario: Display GPON optical link statistics
- **GIVEN** optical statistics available from `DEV2_GPON_INTF_STATS`
- **WHEN** the dashboard renders
- **THEN** the dashboard formats and displays optical rx/tx power and byte counters, tolerating missing stats if the interface is Ethernet WAN.

### Requirement: Wi-Fi Band Status Overview
The system SHALL display the state and primary SSID of each active wireless radio band.

#### Scenario: Render Wi-Fi band status cards
- **GIVEN** common Wi-Fi instances from `DEV2_ADT_WIFI_COMMON`
- **WHEN** the dashboard renders
- **THEN** it generates status cards for 2.4 GHz, 5 GHz, and 6 GHz showing the configured primary SSID, whether the radio is enabled or disabled, and band-specific badges.

### Requirement: Connected Host Discovery and Categorization
The system SHALL discover local network hosts across wireless and wired interfaces and determine their online activity status, link speeds, connection medium, and associated EasyMesh node.

#### Scenario: Refresh network topology and query connected hosts
- **GIVEN** an authenticated session
- **WHEN** the devices screen loads or the summary screen requests active client count
- **THEN** the system triggers `ACT_UPDATE_MAPINFO` and queries `DEV2_WIFI_APDEV_ASSOCDEV` (wireless clients), `DEV2_WIFI_APDEV_ETHASSOCDEV` (wired clients), and `DEV2_WIFI_APDEV` (mesh access points), tolerating partial failure if one query succeeds.

#### Scenario: Parse and deduplicate client instances
- **GIVEN** the raw wireless and wired client instances
- **WHEN** instances are aggregated into connected devices
- **THEN** entries are deduplicated by MAC address (active instances take precedence) and client names resolve to `X_TP_HostName`, falling back to IP address, and lastly MAC address.

#### Scenario: Associate client with connected mesh node
- **GIVEN** client instances containing `X_TP_ApDeviceMac` (wireless) or `APDeviceMACAddress` (wired) and known AP devices from `DEV2_WIFI_APDEV`
- **WHEN** connected devices are parsed
- **THEN** each client identifies the MAC address and resolved hostname of the mesh node to which it is attached.

### Requirement: Client Link Metrics
The system SHALL display physical signal strength for wireless clients and negotiated link speed for wired and wireless clients.

#### Scenario: Wireless signal strength and data rates
- **GIVEN** a wireless device from `DEV2_WIFI_APDEV_ASSOCDEV`
- **WHEN** rendering device cards
- **THEN** wireless clients display a multi-bar signal icon based on `X_TP_SignalStrengthLevel` (1 to 4 bars) and downlink bitrate in Mbit/s derived from `lastDataDownlinkRate`.

#### Scenario: Wired link speed
- **GIVEN** a wired device from `DEV2_WIFI_APDEV_ETHASSOCDEV`
- **WHEN** rendering device cards
- **THEN** wired clients display the negotiated link rate in Mbit/s derived from `linkSpeed`.

### Requirement: Device Filtering and Sorting
The system SHALL allow filtering clients by physical medium, connected EasyMesh node, and online status, displaying active clients first.

#### Scenario: Filter clients by connection type
- **GIVEN** the connected devices list
- **WHEN** the user selects a filter chip (All Active, Wi-Fi, Ethernet Cable, Disconnected)
- **THEN** the list updates dynamically to show only devices matching the selected category.

#### Scenario: Filter clients by connected EasyMesh node
- **GIVEN** an active EasyMesh network with multiple nodes and the connected devices list
- **WHEN** the user selects a node filter (e.g., all nodes or a specific node)
- **THEN** the list displays only clients associated with the selected mesh node, combinable with connection medium filters.

#### Scenario: Sort devices by status and name
- **GIVEN** a combined list of active and offline devices
- **WHEN** devices are displayed
- **THEN** active devices are sorted ahead of disconnected devices, ordered alphabetically by hostname.

#### Scenario: Render connected node indicator on device cards
- **GIVEN** devices associated with a known mesh node
- **WHEN** rendering device cards
- **THEN** each device card displays the name or badge of the connected mesh node alongside link metrics and connection type.

### Requirement: EasyMesh Multi-Node Topology and Metrics Overview
The system SHALL aggregate and display operational status, backhaul link health, and client distribution across all EasyMesh nodes in a uniform, single-line card layout when an EasyMesh mesh network is configured.

#### Scenario: Display EasyMesh nodes overview when mesh nodes are active
- **GIVEN** an active EasyMesh network with one controller and one or more agent nodes queried from `DEV2_WIFI_APDEV`
- **WHEN** the summary dashboard renders
- **THEN** it renders an EasyMesh network section with uniform card dimensions for each node, where the primary router is indicated via an icon rather than text badges, and text fields (name, model, IP, and backhaul link info) are truncated with ellipsis on single lines to maintain equal card heights.

#### Scenario: Fallback to standalone router display when EasyMesh is unconfigured
- **GIVEN** `DEV2_WIFI_APDEV` returns no active agent nodes or EasyMesh is disabled
- **WHEN** the summary dashboard renders
- **THEN** it displays the standalone primary router hardware card and standard summary stats without the multi-node mesh section.
