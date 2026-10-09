# Spec Delta

## ADDED Requirements

### Requirement: Critical Security Alert Banner
The dashboard SHALL display a high-priority alert banner at the top of the screen whenever one or more critical security vulnerabilities are detected on the router.

#### Scenario: Display critical security alert
- **GIVEN** the dashboard data loader discovers at least one Critical security finding (e.g., active DMZ or open Wi-Fi)
- **WHEN** the dashboard screen renders
- **THEN** a high-priority alert card is displayed above standard metrics, offering a direct 1-tap fix action and a link to the full security audit.

#### Scenario: Hide alert when no critical vulnerabilities exist
- **GIVEN** all critical security checks pass
- **WHEN** the dashboard screen renders
- **THEN** the critical security alert card is hidden.

### Requirement: Dashboard Security Health Entry Card
The dashboard SHALL present a summary card indicating the current security health score and vulnerability count, providing direct navigation to the security audit screen.

#### Scenario: Render security summary card
- **GIVEN** a loaded security report
- **WHEN** the dashboard bento grid renders
- **THEN** a card displaying the numerical score (0-100), color-coded health status, and findings summary is rendered.
