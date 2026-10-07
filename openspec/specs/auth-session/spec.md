# auth-session Specification

## Purpose
Manages router connection endpoints, single-session concurrency arbitration, user authentication state, and ephemeral in-memory credential lifecycles for the mobile application.

## Requirements

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

### Requirement: User Authentication Flow
The system SHALL authenticate the user against the router asynchronously without blocking the user interface.

#### Scenario: Perform login submission
- **GIVEN** the login screen with address, username, and password entered
- **WHEN** the user taps the login button
- **THEN** the UI displays an indeterminate progress indicator, disables further input, and dispatches the login call on an IO background thread.

#### Scenario: Session transition upon successful login
- **GIVEN** a successful authentication response from the router
- **WHEN** the login call returns without errors
- **THEN** the system instantiates `PageEngine`, transitions the app state to `loggedIn = true`, and navigates to the home navigation grid with a smooth scale-fade animation.

### Requirement: Single-Session Concurrency Arbitration
The system SHALL inform the user of the router's single-administrator session limitation and handle concurrent session conflicts.

#### Scenario: Display concurrency limitation hint
- **GIVEN** the login screen
- **WHEN** the view renders
- **THEN** an informational badge reminds the user that the router only permits one concurrent administration session, advising them to close any active browser sessions.

#### Scenario: Conflict with active web session
- **GIVEN** an active web browser administration session open on a computer
- **WHEN** the user logs in from the mobile app
- **THEN** the router accepts the new login and automatically terminates the previous web session.

### Requirement: Ephemeral Credential Lifecycle
The system SHALL hold administrative credentials in memory by default, and SHALL only persist the password to secure non-volatile local storage when the user explicitly enables the remember password option.

#### Scenario: Credential storage policy
- **GIVEN** the login screen with "Remember password" unchecked
- **WHEN** the user logs in and the app runs or terminates
- **THEN** the password is held exclusively in memory during the active session and is never persisted to non-volatile local storage.

#### Scenario: Persist password when remember option is checked
- **GIVEN** the login screen with password entered and "Remember password" checked
- **WHEN** the user taps the login button and authentication succeeds
- **THEN** the system securely persists the password to hardware-backed encrypted storage.

#### Scenario: Auto-populate remembered password on screen display
- **GIVEN** a previously remembered password in secure storage
- **WHEN** the login screen is displayed
- **THEN** the password field is pre-populated with the saved password and the "Remember password" checkbox is checked.

#### Scenario: Uncheck remember password clears stored credential
- **GIVEN** a saved password in secure storage
- **WHEN** the user unchecks the "Remember password" option on the login screen
- **THEN** the system immediately purges the stored password from secure non-volatile storage.

#### Scenario: Update remembered password on credential change
- **GIVEN** a saved password and the "Remember password" option checked
- **WHEN** the user inputs a modified password and logs in successfully
- **THEN** the system updates the stored credential in encrypted storage with the new password.

#### Scenario: Explicit session logout
- **GIVEN** an active logged-in session
- **WHEN** the user taps the logout action in the top app bar
- **THEN** the system executes `/cgi/logout` on the router, resets `UiState`, clears the in-memory engine, and returns to the login screen preserving the remembered password if the option remained checked.

### Requirement: Automatic Session Recovery and Inactivity Handling
The system SHALL detect severed transport streams and expired router sessions, transparently attempt re-authentication using available session credentials, and gracefully transition to the login screen when recovery is not possible.

#### Scenario: Transparent re-authentication on connection loss or session timeout
- **GIVEN** an active application session and a router request failing due to an unexpected end of stream, closed socket, or expired session
- **WHEN** in-memory session credentials or remembered credentials are available
- **THEN** the system evicts stale connections, automatically executes a background re-login against the router, and re-executes the failed operation without presenting an error screen.

#### Scenario: Graceful redirect to login screen on unrecoverable session expiry
- **GIVEN** a severed connection or expired session where transparent re-authentication fails or credentials are unavailable
- **WHEN** the system exhausts recovery attempts
- **THEN** the system terminates the in-memory session state, transitions navigation back to the login screen, pre-fills the remembered password if available, and presents a localized session expiration notice.

#### Scenario: Evict idle connections upon application foregrounding
- **GIVEN** the application returning to the foreground from a background state
- **WHEN** the lifecycle transitions to active/resumed
- **THEN** the underlying HTTP client connection pool evicts all idle connections to prevent reusing sockets dropped by the router.

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

#### Scenario: Session expiration error identification
- **GIVEN** a session timeout or closed transport stream resulting in session termination
- **WHEN** the user is returned to the login screen
- **THEN** the system identifies the state as `LoginError.SessionExpired` and displays a friendly notice that the session has ended due to inactivity.
