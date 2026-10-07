# Spec Delta

## ADDED Requirements

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

## MODIFIED Requirements

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
