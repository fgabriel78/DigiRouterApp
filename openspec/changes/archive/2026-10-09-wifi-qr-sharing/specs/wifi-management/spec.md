# Spec Delta

## ADDED Requirements

### Requirement: Wi-Fi Credential Sharing via QR Code
The system SHALL generate standard Wi-Fi QR codes for all configured wireless profiles (primary, guest, multi-SSID, and MLO), provide masked passphrase inspection with reveal controls, support direct clipboard copying, and integrate native system credential sharing.

#### Scenario: Generate standard QR code payload for protected network
- **GIVEN** a configured wireless network with an SSID and security mode requiring a passphrase (e.g. WPA2-Personal, WPA3-Personal)
- **WHEN** the user requests the QR code for that network
- **THEN** the system generates a standard Wi-Fi barcode URI string in the format `WIFI:S:<SSID>;T:WPA;P:<PASSWORD>;H:<HIDDEN>;;` with special characters escaped and renders a high-contrast QR code image.

#### Scenario: Generate standard QR code payload for open network
- **GIVEN** a configured wireless network with security mode `None` or `OWE`
- **WHEN** the user requests the QR code for that network
- **THEN** the system generates a standard Wi-Fi barcode URI string with authentication type `nopass` and no passphrase parameter (`WIFI:S:<SSID>;T:nopass;H:<HIDDEN>;;`).

#### Scenario: Launch native system share sheet
- **GIVEN** the Wi-Fi QR sharing modal is open for a wireless network
- **WHEN** the user selects the system share action
- **THEN** the system dispatches an Android `ACTION_SEND` intent populated with the network SSID and passphrase.

#### Scenario: Mask and reveal passphrase in sharing modal
- **GIVEN** the Wi-Fi QR sharing modal is displayed
- **WHEN** the user views the network credentials
- **THEN** the passphrase is masked by default and can be unmasked using a visibility toggle or copied directly to the clipboard.

#### Scenario: Wi-Fi configuration form QR launch
- **GIVEN** an editable Wi-Fi configuration card in the settings screens (Primary, Guest, Multi-SSID, or MLO)
- **WHEN** the network is enabled and has a valid SSID
- **THEN** a QR trigger action is available to preview and share the credentials.
