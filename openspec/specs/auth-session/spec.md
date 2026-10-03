# auth-session Specification

## Purpose
Manages router connection endpoints, single-session concurrency arbitration, user authentication state, and ephemeral in-memory credential lifecycles for the mobile application.

## Requirements

### Requirement: Router Address and Endpoint Configuration
The system SHALL permit specifying the router's network address and configure the underlying API client accordingly.

#### Scenario: Default router IP address
- **GIVEN** a freshly launched app with no saved state
- **WHEN** the login screen is displayed
- **THEN** the router address field defaults to `192.168.1.1` and the username defaults to `user`.

#### Scenario: Custom router address input
- **GIVEN** a user with a non-standard LAN IP configuration
- **WHEN** the user inputs a custom IP or hostname (e.g., `192.168.0.1` or `http://router.local`)
- **THEN** the system sanitizes the trailing slashes and configures `RouterClient` with the normalized URL.

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
The system SHALL never persist the administrative password to non-volatile local storage.

#### Scenario: Credential storage policy
- **GIVEN** a logged-in session
- **WHEN** the app runs or is terminated
- **THEN** the password is held exclusively in memory during the active session and is never written to `SharedPreferences`, local databases, or files.

#### Scenario: Explicit session logout
- **GIVEN** an active logged-in session
- **WHEN** the user taps the logout action in the top app bar
- **THEN** the system executes `/cgi/logout` on the router, resets `UiState`, clears the in-memory engine, and returns to the login screen.

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
