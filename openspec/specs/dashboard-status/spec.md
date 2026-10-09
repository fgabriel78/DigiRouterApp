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
The system SHALL display the state and details of all enabled wireless networks (including primary, guest, and additional networks) and consolidate matching SSIDs across frequency bands.

#### Scenario: Render Wi-Fi band status cards
- **GIVEN** common Wi-Fi instances from `DEV2_ADT_WIFI_COMMON`
- **WHEN** the dashboard renders
- **THEN** it aggregates all enabled networks (`primaryEnable`, `guestEnable`, `mssid1Enable`, `mssid2Enable`), consolidating matching SSIDs of the same network type across multiple frequency bands (e.g., 2.4 GHz, 5 GHz, 6 GHz) into a single status card showing all active frequency bands.

#### Scenario: Display distinct iconography by network type
- **GIVEN** active wireless networks of different categories (primary, guest, or additional multi-SSID)
- **WHEN** the status cards render
- **THEN** each card displays distinct iconography and a descriptive type label distinguishing primary Wi-Fi, guest networks, and secondary networks.

#### Scenario: Display primary Wi-Fi disabled status
- **GIVEN** the primary Wi-Fi network is disabled across all or individual bands
- **WHEN** the dashboard renders
- **THEN** it renders a status card explicitly indicating that the primary Wi-Fi is deactivated.

#### Scenario: Render fallback when all wireless networks are disabled
- **GIVEN** no wireless network profiles are enabled on any band
- **WHEN** the dashboard renders
- **THEN** it renders a single status card indicating that Wi-Fi is globally disabled.

#### Scenario: Launch Wi-Fi QR credential sharing from active network card
- **GIVEN** an active Wi-Fi status card rendered on the dashboard
- **WHEN** the user interacts with the QR action on the card
- **THEN** the system launches the Wi-Fi QR sharing sheet initialized with the network's SSID, passphrase, security mode, and active bands.

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

### Requirement: Hardware Manufacturer Identification and Private MAC Detection
The system SHALL resolve client MAC addresses against an offline database of IEEE Organizationally Unique Identifier (OUI) prefixes to determine hardware manufacturers, and SHALL identify IEEE 802 locally administered addresses as private MACs.

#### Scenario: Identify known hardware vendor from MAC OUI
- **GIVEN** a connected host with a globally unique MAC address matching a known IEEE OUI prefix
- **WHEN** the host is rendered in the devices list or device detail sheet
- **THEN** the system displays the hardware manufacturer name (e.g. Apple, Samsung, Sony) alongside the device details.

#### Scenario: Identify private or randomized MAC address
- **GIVEN** a connected host whose MAC address has the IEEE locally administered bit set (second least-significant bit of the first byte)
- **WHEN** the host is rendered in the devices list or device detail sheet
- **THEN** the system identifies the address as a private MAC address instead of attempting a vendor match.

### Requirement: Custom Device Aliases and Categorization
The system SHALL allow users to assign custom friendly aliases and category icons to connected devices, persisting the metadata locally across application sessions keyed by client MAC address.

#### Scenario: Render device card with custom alias and category
- **GIVEN** a connected device with an assigned alias and category in local storage
- **WHEN** the device card renders in the devices screen
- **THEN** the custom alias is displayed as the primary title, the selected category icon is displayed, and a vendor badge is shown alongside the title.

#### Scenario: Fallback display when no alias is configured
- **GIVEN** a connected device without custom metadata in local storage
- **WHEN** the device card renders in the devices screen
- **THEN** the router-reported hostname is displayed as the primary title, accompanied by a vendor badge or private MAC indicator, and the default connection icon is used.

#### Scenario: Inspect and edit device metadata in detail sheet
- **GIVEN** a user selecting a device card in the devices screen
- **WHEN** the device detail bottom sheet is opened
- **THEN** the sheet displays detailed connection metrics (IP, MAC, vendor, EasyMesh AP node, link speed, signal level) and allows the user to edit the alias and choose from predefined categories (Phone, Tablet, Computer, TV, Console, Smart Home/IoT, Printer, Network/AP, Other).

#### Scenario: Reset custom device metadata
- **GIVEN** a device with existing custom metadata in local storage
- **WHEN** the user selects the reset or clear action in the device detail sheet
- **THEN** the custom alias and category are removed from local storage and the device card immediately reverts to default router-provided title and icon.

### Requirement: Intelligent Hostname-Based Vendor and Category Heuristics
The system SHALL analyze client hostnames using pattern-matching heuristics to infer hardware manufacturers and default device categories when physical OUI resolution is unavailable or the MAC address is randomized.

#### Scenario: Infer mobile device vendor and category from hostname
- **GIVEN** a connected host whose hostname matches mobile model patterns (such as `iPhone`, `iPad`, `Galaxy`, `Pixel`, `Redmi`, `POCO`)
- **WHEN** the host is rendered in the devices list or device detail sheet without a user-assigned alias or category
- **THEN** the system infers the corresponding manufacturer (e.g. Apple, Samsung, Google, Xiaomi) and device category (`PHONE` or `TABLET`), displaying the inferred vendor badge and category icon.

#### Scenario: Infer computer category from desktop or laptop hostname
- **GIVEN** a connected host whose hostname matches computer naming conventions (such as `DESKTOP-*`, `LAPTOP-*`, `MacBook*`, `iMac*`, `*-PC`)
- **WHEN** the host is rendered without a user-assigned category
- **THEN** the system applies the `COMPUTER` category icon by default.

#### Scenario: Infer entertainment and smart home device types
- **GIVEN** a connected host whose hostname matches consoles, smart TVs, or smart home devices (such as `Switch`, `PlayStation`, `Xbox`, `Apple-TV`, `Fire-TV`, `Chromecast`, `Echo`, `Nest`)
- **WHEN** the host is rendered in the devices list or device detail sheet
- **THEN** the system applies the corresponding category (`CONSOLE`, `TV`, or `IOT`) and resolves the respective vendor if identifiable.

#### Scenario: Precedence of identification sources
- **GIVEN** a connected device with multiple possible identification attributes
- **WHEN** the device title, vendor badge, and category icon are resolved
- **THEN** the system enforces precedence:
  1. User-configured custom alias and category always take highest priority.
  2. Physical IEEE OUI hardware manufacturer takes priority over heuristic manufacturer when available.
  3. Hostname-inferred vendor and category apply when physical OUI is missing or when the MAC address is private.
  4. If no vendor or heuristic match exists and the MAC is locally administered, the "Private MAC" badge is displayed.

#### Scenario: Informative private MAC explanation in device detail
- **GIVEN** a connected device identified as having a private MAC address
- **WHEN** the device detail bottom sheet is displayed
- **THEN** the sheet indicates that the device is using a randomized private address, explains that hardware OUI lookup is not possible for privacy reasons, and shows any heuristically inferred vendor alongside the explanation.

### Requirement: Critical Security Alert Banner
The dashboard SHALL display a high-priority alert banner at the top of the screen whenever one or more critical security vulnerabilities are detected on the router.

#### Scenario: Display critical security alert
- **GIVEN** the dashboard data loader discovers at least one Critical security finding (e.g., active DMZ or open Wi-Fi)
- **WHEN** the dashboard screen renders
- **THEN** a high-priority alert card is displayed above standard metrics, offering a direct 1-tap fix action and a link to the full security audit.

#### Scenario: Hide alert when no critical vulnerabilities exist
- **GIVEN** all critical security checks pass
- **WHEN** the dashboard screen renders
- **THEN** the critical security alert card is hidden.

### Requirement: Dashboard Security Health Entry Card
The dashboard SHALL present a summary card indicating the current security health score and vulnerability count, providing direct navigation to the security audit screen.

#### Scenario: Render security summary card
- **GIVEN** a loaded security report
- **WHEN** the dashboard bento grid renders
- **THEN** a card displaying the numerical score (0-100), color-coded health status, and findings summary is rendered.

