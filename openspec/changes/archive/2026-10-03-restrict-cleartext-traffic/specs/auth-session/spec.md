# Spec Delta

## MODIFIED Requirements

### Requirement: Router Address and Endpoint Configuration
The system SHALL permit specifying the router's network address, restrict cleartext HTTP transport exclusively to local network ranges, and configure the underlying API client accordingly.

#### Scenario: Default router IP address
- **GIVEN** a freshly launched app with no saved state
- **WHEN** the login screen is displayed
- **THEN** the router address field defaults to `192.168.1.1` and the username defaults to `user`.

#### Scenario: Custom router address input
- **GIVEN** a user with a non-standard LAN IP configuration
- **WHEN** the user inputs a custom IP or hostname (e.g., `192.168.0.1` or `http://router.local`)
- **THEN** the system sanitizes the trailing slashes and configures `RouterClient` with the normalized URL.

#### Scenario: Cleartext permitted for local network destinations
- **GIVEN** a router address residing in private IPv4 ranges (RFC 1918 `192.168.0.0/16`, `10.0.0.0/8`, `172.16.0.0/12`), loopback (`127.0.0.1`, `localhost`), link-local (`169.254.0.0/16`), or local domain names (`*.local`, `*.lan`, `*.home.arpa`)
- **WHEN** the app establishes cleartext HTTP communication with the router
- **THEN** cleartext traffic is permitted by the application and network security policy.

#### Scenario: Cleartext blocked for external or public destinations
- **GIVEN** a configured address pointing to a public IP or remote domain outside local network ranges
- **WHEN** the app attempts to initiate cleartext HTTP communication
- **THEN** the system rejects or blocks the cleartext connection attempt and prevents cleartext transmission.

### Requirement: Login Error Categorization
The system SHALL categorize login and connectivity failures and present clear localized diagnostic messages.

#### Scenario: Wrong credentials entered
- **GIVEN** incorrect password input resulting in router error code 71233
- **WHEN** the login process fails
- **THEN** the system identifies the failure as `LoginError.WrongPassword` and displays a localized invalid credentials warning.

#### Scenario: Router unreachable or network timeout
- **GIVEN** a disconnected Wi-Fi link or invalid IP address causing an `IOException`
- **WHEN** the login process fails
- **THEN** the system identifies the failure as `LoginError.Unreachable` and displays a localized connection timeout message.

#### Scenario: Cleartext policy or non-local address error
- **GIVEN** a connection failure caused by a non-local router address or platform network security policy block
- **WHEN** the login process fails
- **THEN** the system catches the failure and informs the user that cleartext communication is restricted to local network ranges.
