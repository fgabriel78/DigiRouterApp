# Spec Delta

## ADDED Requirements

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
