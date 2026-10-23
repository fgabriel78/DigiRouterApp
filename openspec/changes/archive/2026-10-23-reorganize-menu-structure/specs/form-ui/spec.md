# form-ui Specification

## MODIFIED Requirements

### Requirement: Expressive Home Screen Navigation and Grouped Layout
The system SHALL present an expressive home dashboard featuring a glanceable router status hero card, equal-height quick access bento tiles, and grouped container cards for advanced configuration categories using Material 3 Expressive design tokens.

#### Scenario: Glanceable hero router status card
- **GIVEN** an active authenticated session
- **WHEN** the home screen renders
- **THEN** it renders a hero status card at the top displaying the router model, active connection state indicator, router IP address, and tapping the card navigates directly to the fiber diagnostics or system summary screen.

#### Scenario: Equal-height quick access bento grid
- **GIVEN** the primary status and overview navigation items (`Summary`, `Connected devices`, `Network topology`, `Speed test`)
- **WHEN** the quick access section renders
- **THEN** it renders these four items in a 2x2 bento grid where each card has equal fixed height, an expressive vibrant shape badge, title, and tap navigation without vertical text wrapping discrepancies.

#### Scenario: Grouped container cards for configuration categories
- **GIVEN** the configuration and diagnostic categories (`Wi-Fi`, `Local network & storage`, `Ports & remote access`, `Security & family`, `Diagnostics`, `System`)
- **WHEN** rendering configuration categories
- **THEN** each category renders as a cohesive Material 3 container card containing its items (including `Main Wi-Fi` at the top of the `Wi-Fi` group) as horizontal rows with leading shape badges, descriptive subtitles, and trailing navigation chevrons, avoiding single-item category cards.
