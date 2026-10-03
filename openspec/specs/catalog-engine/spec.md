# catalog-engine Specification

## Purpose
Provides the declarative schema definition model, input validation rules, cross-object joins, differential persistence engine, and internationalization mechanisms that translate TR-181 data model objects into UI screens.

## Requirements

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

### Requirement: Field Value Validation and Constraints
The system SHALL enforce type constraints and range boundaries on field values before dispatching writes to the router.

#### Scenario: Validate text and secret length
- **GIVEN** a field of type `FieldType.Text` or `FieldType.Secret` with minimum and maximum lengths
- **WHEN** a user enters a string value
- **THEN** the validator returns null if the length is within bounds, or a localized error message if the text is shorter than minimum or longer than maximum.

#### Scenario: Validate number bounds and units
- **GIVEN** a field of type `FieldType.Number` with a minimum and maximum boundary
- **WHEN** a user enters a numeric value
- **THEN** the validator verifies the value parses to an integer between minimum and maximum, rejecting non-numeric input or out-of-range numbers.

#### Scenario: Validate IPv4 address format
- **GIVEN** a field of type `FieldType.Ipv4`
- **WHEN** a user enters an IP address
- **THEN** the validator verifies standard dot-decimal IPv4 octet format (0-255 each), rejecting invalid strings.

#### Scenario: Validate MAC address format
- **GIVEN** a field of type `FieldType.Mac`
- **WHEN** a user enters a hardware address
- **THEN** the validator verifies the format matches 6 pairs of hexadecimal digits separated by colons (`AA:BB:CC:DD:EE:FF`).

### Requirement: Dynamic Join and Cross-Object Merging
The system SHALL merge attributes from secondary TR-181 list objects into primary section instances based on matching key values.

#### Scenario: Join client host details with wireless association metrics
- **GIVEN** a section with a `Join` declaration specifying a secondary OID, local key, remote key, and attribute prefix
- **WHEN** `PageEngine.load(section)` is executed
- **THEN** the engine loads the secondary object list, finds instances where the remote key matches the local key, and merges remote attributes with the designated prefix into the primary instance.

### Requirement: Differential Save Optimization
The system SHALL compute differences between original router values and user-edited values, transmitting only modified attributes.

#### Scenario: Save only modified attributes
- **GIVEN** an original instance with values and an edited map of values
- **WHEN** `PageEngine.save(section, original, edited)` is invoked
- **THEN** the engine compares only editable non-Info fields, constructs a change map containing only differing values, and executes a `so` write operation only if changes are present.

#### Scenario: Apply changes to all instances
- **GIVEN** a section marked with `applyToAll = true` (such as MLO settings across bands)
- **WHEN** save is executed
- **THEN** the engine applies the computed attribute changes across all loaded instances of that section.

### Requirement: Lifecycle Hooks and Post-Save Triggers
The system SHALL support pre-save transformation hooks, post-save action triggers, and dynamic parameter resolution.

#### Scenario: Execute save hook for side effects
- **GIVEN** a section with a `saveHook` (such as enforcing AES encryption when switching security modes, or formatting DDNS account commands)
- **WHEN** changes are detected during save
- **THEN** the engine invokes `saveHook(original, edited, changed)` allowing the section logic to inject, rewrite, or validate required companion attributes.

#### Scenario: Execute post-save actions
- **GIVEN** a section with configured `afterSave` action identifiers (such as `ACT_WIFI_RELOAD_MLO`)
- **WHEN** the `so` write succeeds
- **THEN** the engine executes `client.operate(op)` for each action identifier in the list.

#### Scenario: Resolve dynamic parameters
- **GIVEN** an `AddSpec` with a dynamic placeholder value such as `@activeWan`
- **WHEN** a new instance is created
- **THEN** the engine queries active WAN interfaces (`DEV2_ADT_WAN`) to resolve the connected interface name before submitting the add operation.

### Requirement: Internationalization and Localization Dictionary
The system SHALL display catalog texts in the user's preferred language with an automated fallback mechanism.

#### Scenario: Translate catalog text to active locale
- **GIVEN** an English source string from the catalog
- **WHEN** `tr(text, args)` is called
- **THEN** the system returns the Spanish translation if available, formatted with positional arguments `{0}`, `{1}`, or returns the original English text if no translation exists.
