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
The system SHALL discover local network hosts across wireless and wired interfaces and determine their online activity status, link speeds, and connection medium.

#### Scenario: Refresh network topology and query connected hosts
- **GIVEN** an authenticated session
- **WHEN** the devices screen loads or the summary screen requests active client count
- **THEN** the system triggers `ACT_UPDATE_MAPINFO` and queries `DEV2_WIFI_APDEV_ASSOCDEV` (wireless clients) and `DEV2_WIFI_APDEV_ETHASSOCDEV` (wired clients), tolerating partial failure if one query succeeds.

#### Scenario: Parse and deduplicate client instances
- **GIVEN** the raw wireless and wired client instances
- **WHEN** instances are aggregated into connected devices
- **THEN** entries are deduplicated by MAC address (active instances take precedence) and client names resolve to `X_TP_HostName`, falling back to IP address, and lastly MAC address.

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
The system SHALL allow filtering clients by physical medium and online status, displaying active clients first.

#### Scenario: Filter clients by connection type
- **GIVEN** the connected devices list
- **WHEN** the user selects a filter chip (All Active, Wi-Fi, Ethernet Cable, Disconnected)
- **THEN** the list updates dynamically to show only devices matching the selected category.

#### Scenario: Sort devices by status and name
- **GIVEN** a combined list of active and offline devices
- **WHEN** devices are displayed
- **THEN** active devices are sorted ahead of disconnected devices, ordered alphabetically by hostname.
