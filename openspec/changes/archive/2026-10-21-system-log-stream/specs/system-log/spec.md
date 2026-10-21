# Spec Delta

## Purpose

Enables power users and administrators to capture, parse, stream, filter, and manage router system log events in real time, as well as inspect and modify the router's internal syslog daemon severity threshold.

## ADDED Requirements

### Requirement: System Log Extraction and Parsing
The system SHALL retrieve raw event logs from the router via authenticated HTTP communication and parse them into structured log entities comprising timestamp, severity code, source module, and payload message.

#### Scenario: Successfully parse standard log entries
- **WHEN** the router returns a status code `0` followed by valid formatted log lines matching the standard router format
- **THEN** the system decodes each line into a structured entry with timestamp, severity level (Emergency through Debug), module identifier, and body text, presented in reverse chronological order.

#### Scenario: Handle empty or header-only log response
- **WHEN** the router log buffer is empty or contains only the success status line
- **THEN** the system produces an empty list of entries without throwing an error and displays an empty state.

#### Scenario: Ignore malformed or corrupted log lines
- **WHEN** a log line does not conform to the expected timestamp, severity, and module regular expression
- **THEN** the system safely ignores the malformed line and continues parsing remaining entries.

### Requirement: Mirror Buffer Polling and Lifecycle
The system SHALL poll the router event buffer at a 3-second interval while the log stream interface is active, mirroring the router's active window without retaining expired entries, and SHALL support pausing and resuming polling.

#### Scenario: Active polling cadence
- **WHEN** the system log screen is displayed and live streaming is enabled
- **THEN** the system polls the router every 3 seconds and updates the displayed log entries to match the router's current buffer.

#### Scenario: Pause live streaming
- **WHEN** the user taps the pause action
- **THEN** recurring background network requests are halted immediately, maintaining the last fetched list on screen with a paused status indicator.

#### Scenario: Resume live streaming
- **WHEN** the user resumes streaming from a paused state
- **THEN** recurring polling re-activates immediately, refreshing the screen with current buffer contents.

### Requirement: Client-Side Log Filtering and Search
The system SHALL provide real-time client-side filtering across severity levels, dynamic source modules, and freeform text search.

#### Scenario: Filter by severity categories
- **WHEN** the user selects a severity filter chip (such as Errors, Warnings, Info, or All)
- **THEN** the log list displays only entries matching the selected severity constraints.

#### Scenario: Filter by discovered module
- **WHEN** the user selects a specific module from the dynamically populated module selector
- **THEN** the log list displays only entries originating from that module.

#### Scenario: Text search query
- **WHEN** the user enters a search query string
- **THEN** the log list immediately filters to entries containing the query within the message, module, or timestamp.

### Requirement: Router Log Sensitivity Configuration
The system SHALL inspect and update the router's internal local logging severity threshold via router configuration parameters.

#### Scenario: Inspect current log sensitivity
- **WHEN** the log settings interface is opened
- **THEN** the system retrieves `DEV2_SYSLOG_CFG` and presents the active local severity level.

#### Scenario: Update router log sensitivity
- **WHEN** the user selects a new local severity level and saves
- **THEN** the system submits the updated `localSeverity` attribute to `DEV2_SYSLOG_CFG` and notifies the user upon success.

### Requirement: Buffer Clearing and Sharing Actions
The system SHALL provide administrative actions to clear the router's log buffer upon explicit confirmation and export the current filtered log entries.

#### Scenario: Clear router log buffer with confirmation
- **WHEN** the user triggers the clear buffer action and confirms the warning dialog
- **THEN** the system invokes the router log clearing endpoint and updates the displayed list to an empty state.

#### Scenario: Export log entries
- **WHEN** the user invokes the share action
- **THEN** the system compiles the currently filtered log entries into formatted text and launches the system share sheet.
