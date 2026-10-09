# Spec Delta

## Purpose
Evaluates router configuration security posture, assigns a composite health score (0-100), detects risky UPnP mappings, and executes automated multi-setting remediations.

## ADDED Requirements

### Requirement: Security Posture Evaluation and Scoring
The system SHALL evaluate router configurations across multiple security vectors (DMZ, Wi-Fi security, WPS, guest network isolation, DoS flood defenses, USB file sharing exposure, and port mappings) and compute an aggregate health score between 0 and 100.

#### Scenario: All security checks pass
- **GIVEN** a router configuration where DMZ is disabled, Wi-Fi uses WPA2/WPA3, WPS is disabled, guest networks are isolated from LAN, DoS defense is active, and USB file sharing is secured
- **WHEN** the security audit engine evaluates the configuration
- **THEN** the system SHALL return a score of 100 with zero critical or warning findings.

#### Scenario: Critical vulnerability detected
- **GIVEN** a router configuration with DMZ enabled or an unencrypted open Wi-Fi network
- **WHEN** the security audit engine evaluates the configuration
- **THEN** the system SHALL deduct points according to vulnerability severity and mark the finding as Critical.

### Requirement: Intelligent UPnP Risk Filtering
The system SHALL evaluate active UPnP port mappings against a catalog of dangerous services and only penalize mappings targeting high-risk ports.

#### Scenario: Safe gaming UPnP port mappings
- **GIVEN** UPnP enabled with active port mappings for high dynamic ports (e.g. UDP 3074, UDP 9308 for gaming or voice)
- **WHEN** UPnP security is evaluated
- **THEN** the system SHALL NOT deduct score points and SHALL categorize the check as passed.

#### Scenario: Dangerous UPnP port mapping
- **GIVEN** UPnP enabled with an active mapping exposing a high-risk administrative or plaintext service (e.g., port 21, 22, 23, 445, 3389, or unencrypted database)
- **WHEN** UPnP security is evaluated
- **THEN** the system SHALL flag the mapping as a High/Critical finding and deduct score points.

### Requirement: 1-Tap Batch Remediation ("Resolver todo con 1 toque")
The system SHALL generate and execute a consolidated batch remediation plan for all automated, non-disruptive security findings upon user confirmation.

#### Scenario: Preview batch remediation plan
- **GIVEN** a security report containing remediable findings (e.g., active DMZ, enabled WPS, unisolated guest network, disabled DoS protection)
- **WHEN** the user initiates batch remediation
- **THEN** the system SHALL present a confirmation dialog detailing all configuration changes to be applied.

#### Scenario: Execute batch remediation
- **GIVEN** the confirmed batch remediation plan
- **WHEN** the user confirms execution
- **THEN** the system SHALL send configuration updates to the router sequentially via RouterClient and re-evaluate the security health score.

### Requirement: Security Health Presentation and Filtering
The system SHALL present the security posture in a dedicated interface with a circular score gauge, status filtering, and individual remediation actions.

#### Scenario: Filter security findings
- **GIVEN** a security report with a mix of passed, warning, and critical findings
- **WHEN** the user selects a filter chip (All, At Risk, Passed)
- **THEN** the list displays only the findings matching the selected status filter.
