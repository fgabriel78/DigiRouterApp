# Spec Delta

## ADDED Requirements

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
