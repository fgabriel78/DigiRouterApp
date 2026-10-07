# Spec Delta

## ADDED Requirements

### Requirement: Stable Credential Input Field Presentation and Keyboard Insets
The system SHALL render the credential input form with responsive keyboard (IME) insets, compact adaptive headers, and uniform monospace masking metrics, preventing keyboard overlap, cursor jitter, and scroll fighting during credential entry.

#### Scenario: Stable monospace character masking
- **GIVEN** the login screen
- **WHEN** the user types characters into the password field while obscured
- **THEN** the field renders masked glyphs with uniform monospace advance width and spacing, preventing horizontal cursor oscillation or text scroll jumping between transient keystrokes and bullet glyphs.

#### Scenario: Keyboard insets and viewport resizing
- **GIVEN** the login screen on any device or soft keyboard configuration
- **WHEN** the user focuses an input field and the soft keyboard is displayed
- **THEN** the layout applies `adjustResize` and IME insets padding so the scrollable viewport resizes to the area above the keyboard, smoothly bringing the focused input field completely into view without keyboard overlap.

#### Scenario: Keyboard-responsive header compaction
- **GIVEN** the login screen
- **WHEN** the soft keyboard is visible
- **THEN** the router icon badge compacts to 48.dp, conserving vertical screen height so the credential fields remain visible and accessible above the keyboard.

#### Scenario: Natural form spacing without artificial blank gaps
- **GIVEN** the login screen
- **WHEN** viewing the credential form without errors
- **THEN** the form renders with natural, cohesive spacing between the password input and the "Remember password" checkbox without empty static placeholder containers.

#### Scenario: Minimalist authentication interface
- **GIVEN** the administrative login screen
- **WHEN** the user enters a password
- **THEN** the interface refrains from rendering password strength meters, scoring widgets, or progress indicators within or beneath the credential form.
