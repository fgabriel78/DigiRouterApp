# Spec Delta

## MODIFIED Requirements

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
