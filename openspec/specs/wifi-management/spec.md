# wifi-management Specification

## Purpose
Manages wireless network configuration across 2.4 GHz, 5 GHz, and 6 GHz bands, including Wi-Fi 7 Multi-Link Operation (MLO), radio tuning, advanced 802.11 features, WPS pairing, guest networks, and secondary multi-SSID profiles.

## Requirements

### Requirement: Primary Wi-Fi Configuration
The system SHALL configure primary wireless networks per band with customizable SSIDs, security modes, and passphrases.

#### Scenario: Edit primary SSID and passphrase
- **GIVEN** primary Wi-Fi section for 2.4 GHz, 5 GHz, or 6 GHz (`DEV2_ADT_WIFI_COMMON`)
- **WHEN** the user updates the SSID (1-32 chars) or pre-shared key (8-63 chars)
- **THEN** the system validates length constraints and saves the attributes, displaying a warning that modifying the connected network may cause disconnection.

#### Scenario: Enforce AES encryption when enabling security
- **GIVEN** a network currently configured with security mode `None`
- **WHEN** the user changes the security mode to `WPA2-Personal` or `WPA3-Personal`
- **THEN** the `saveHook` automatically injects `primaryWPAWPA2EncryptionMode = "AES"` into the write payload.

### Requirement: Wi-Fi 7 Multi-Link Operation (MLO) Control
The system SHALL configure Wi-Fi 7 MLO to aggregate frequency bands into a unified high-throughput network and trigger radio reloads.

#### Scenario: Enable MLO network across bands
- **GIVEN** the MLO section with `applyToAll = true`
- **WHEN** the user enables MLO and sets the unified SSID and security credentials
- **THEN** the system distributes the configuration across all band instances, saves changes, and executes `ACT_WIFI_RELOAD_MLO` post-save.

### Requirement: Physical Radio Settings and Channel Tuning
The system SHALL tune physical radio parameters including 802.11 standards, channel bandwidth, automatic channel selection, manual channels, and transmit power.

#### Scenario: Select standard and channel width by band
- **GIVEN** the radio configuration section
- **WHEN** inspecting options for 2.4 GHz vs 5 GHz/6 GHz
- **THEN** 2.4 GHz offers bgn/bgnax/bgnaxbe with bandwidth up to 40MHz, while 5 GHz offers anac/anacax/anacaxbe with bandwidth up to 160MHz.

#### Scenario: Toggle manual channel assignment
- **GIVEN** automatic channel selection (`autoChannel`) enabled
- **WHEN** the user toggles `autoChannel` to off (`"0"`)
- **THEN** the manual channel selector becomes visible, populated with valid regional channels (1-13 for 2.4 GHz; 36-140 for 5 GHz).

#### Scenario: Configure transmit power level
- **GIVEN** the radio settings section
- **WHEN** the user adjusts transmit power
- **THEN** the choices map to "25" (Low), "50" (Medium), or "100" (High).

### Requirement: Advanced Wi-Fi Features and Band Steering
The system SHALL provide configuration controls for advanced 802.11 protocols, power saving mechanisms, timing intervals, and band steering.

#### Scenario: Toggle advanced radio optimizations
- **GIVEN** the advanced Wi-Fi options section
- **WHEN** the user configures features
- **THEN** the system allows toggling Tx Beamforming (`primaryTxBFEnable`), MU-MIMO (`primaryMUMIMOEnable`), OFDMA (`primaryOFDMAEnable`), Target Wake Time (`primaryTWTEnable`), BSS Color, WMM, and Client Isolation.

#### Scenario: Configure band steering to 5 GHz
- **GIVEN** the band steering section (`DEV2_WIFI_BANDSTEERING`)
- **WHEN** the user toggles `enable`
- **THEN** the router activates steering logic to transition capable dual-band clients to the 5 GHz band.

### Requirement: Wi-Fi Protected Setup (WPS) Control
The system SHALL allow enabling or disabling WPS and inspecting the current pairing state.

#### Scenario: Toggle WPS availability
- **GIVEN** the WPS configuration section (`DEV2_ADT_WIFI_COMMON`)
- **WHEN** the user modifies `WPSEnable`
- **THEN** the system writes the new state and displays the read-only `WPSState`.

### Requirement: Guest Network Management
The system SHALL configure an isolated guest network with independent SSIDs, security rules, and local network access boundaries.

#### Scenario: Configure guest network isolation
- **GIVEN** the guest network section
- **WHEN** the user enables the guest network
- **THEN** the user can set `guestSSID`, select security mode, and independently configure `guestIsolationEnable` (isolate guests from each other) and `guestLANAccessEnable` (isolate guests from local LAN).

### Requirement: Multi-SSID Additional Networks
The system SHALL support secondary and tertiary wireless networks (`mssid1`, `mssid2`) per frequency band.

#### Scenario: Enable secondary multi-SSID network
- **GIVEN** the multi-SSID section
- **WHEN** the user configures additional network 1 or 2
- **THEN** the system writes `mssid{n}Enable`, `mssid{n}SSID`, security mode, passphrase, and client isolation attributes with automatic AES hook activation.
