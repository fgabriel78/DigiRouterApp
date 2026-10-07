# Spec Delta

## MODIFIED Requirements

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
