# Spec Delta

## MODIFIED Requirements

### Requirement: Declarative Hierarchy and Schema Specification
The system SHALL model router configuration screens declaratively using `Page`, `Section`, `Field`, and typed metadata structures.

#### Scenario: Structure pages into sections and fields
- **GIVEN** a configuration domain definition
- **WHEN** a `Page` is declared in `Catalog`
- **THEN** it defines an identifier, title, functional `Group`, optional description, whether it requires admin privileges, and an ordered list of `Section` elements.

#### Scenario: Support typed field definitions
- **GIVEN** a section field declaration
- **WHEN** a `Field` is defined
- **THEN** it specifies an attribute key, label, help text, optional conditional visibility predicate (`visibleIf`), and a polymorphic `FieldType` (Switch, Text, Secret, Number, Choice, Ipv4, Mac, TimeOfDay, Info).

#### Scenario: Register custom VPN server page in network group
- **GIVEN** the catalog page registry
- **WHEN** `Catalog.pages` is initialized
- **THEN** it contains a `Page` with id `"vpn"`, functional group `Group.NETWORK`, `custom = true`, and `adminOnly = true`.
