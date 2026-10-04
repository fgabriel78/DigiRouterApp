# Spec Delta

## ADDED Requirements

### Requirement: EasyMesh Multi-Node Topology and Metrics Overview
The system SHALL aggregate and display operational status, backhaul link health, and client distribution across all EasyMesh nodes when an EasyMesh mesh network is configured.

#### Scenario: Display EasyMesh nodes overview when mesh nodes are active
- **GIVEN** an active EasyMesh network with one controller and one or more agent nodes queried from `DEV2_WIFI_APDEV`
- **WHEN** the summary dashboard renders
- **THEN** it renders an EasyMesh network section showing each node's configured hostname/location, model name, role (Controller/Router vs Agent/Satellite), IP address, backhaul link medium (Ethernet or Wi-Fi with signal strength and negotiated link rate), online status, and count of currently associated client devices.

#### Scenario: Fallback to standalone router display when EasyMesh is unconfigured
- **GIVEN** `DEV2_WIFI_APDEV` returns no active agent nodes or EasyMesh is disabled
- **WHEN** the summary dashboard renders
- **THEN** it displays the standalone primary router hardware card and standard summary stats without the multi-node mesh section.

## MODIFIED Requirements

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
