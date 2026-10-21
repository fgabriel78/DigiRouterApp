# Spec Delta

## MODIFIED Requirements

### Requirement: System Log Extraction and Parsing
The system SHALL retrieve raw event logs from the router via authenticated HTTP communication and parse them into structured log entities comprising timestamp, severity code, source module, and payload message.

#### Scenario: Successfully parse standard log entries
- **WHEN** the router returns a status code `0` followed by valid formatted log lines matching the standard router format
- **THEN** the system decodes each line into a structured entry with timestamp, severity level (Emergency through Debug), module identifier, and body text, presented in reverse chronological order.

#### Scenario: Handle encrypted router log responses
- **WHEN** the router returns an AES-encrypted Base64 response over `/cgi/log` under GDPR session encryption
- **THEN** the system decrypts the ciphertext prior to parsing and decodes the structured log entries without failure.

#### Scenario: Deduplicate identical log entry identifiers
- **WHEN** multiple log entries share identical timestamps, modules, and messages within the router buffer
- **THEN** the system generates unique, deterministic identifiers for each entry to guarantee collision-free rendering in list views.

#### Scenario: Handle empty or header-only log response
- **WHEN** the router log buffer is empty or contains only the success status line
- **THEN** the system produces an empty list of entries without throwing an error and displays an empty state.

#### Scenario: Ignore malformed or corrupted log lines
- **WHEN** a log line does not conform to the expected timestamp, severity, and module regular expression
- **THEN** the system safely ignores the malformed line and continues parsing remaining entries.
