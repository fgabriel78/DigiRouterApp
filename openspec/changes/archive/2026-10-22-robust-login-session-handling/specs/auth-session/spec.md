# Spec Delta

## MODIFIED Requirements

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

#### Scenario: Transparent recovery of transient transport reset during authentication
- **GIVEN** an authentication attempt in progress
- **WHEN** the initial connection or handshake encounters a transient socket reset, unexpected end of stream, or transport exception
- **THEN** the system automatically retries the authentication sequence once after a short backoff before surfacing an error to the user.

### Requirement: Single-Session Concurrency Arbitration
The system SHALL inform the user of the router's single-administrator session limitation and handle concurrent session conflicts.

#### Scenario: Display concurrency limitation hint
- **GIVEN** the login screen
- **WHEN** the view renders
- **THEN** an informational note provides neutral guidance that the router architecture permits only one concurrent administration session, without asserting that another session is currently active.

#### Scenario: Conflict with active web session
- **GIVEN** an active web browser administration session open on a computer
- **WHEN** the user logs in from the mobile app
- **THEN** the router accepts the new login and automatically terminates the previous web session.
