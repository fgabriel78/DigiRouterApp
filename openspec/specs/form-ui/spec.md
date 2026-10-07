# form-ui Specification

## Purpose
Renders declarative catalog pages and sections into native Android Jetpack Compose interfaces using Material 3 Expressive components, orchestrating client-side validation, confirmation dialogs, modal CRUD forms, and user feedback.

## Requirements

### Requirement: Dynamic Form Section Rendering
The system SHALL dynamically render sections of declarative pages into Material 3 cards with distinct visual containers.

#### Scenario: Render single-instance and multi-instance sections
- **GIVEN** a declarative `Page` containing one or more `Section` definitions
- **WHEN** `GenericPageScreen` renders
- **THEN** it renders single-instance sections as cohesive cards and multi-instance sections as lists of cards with dynamic item titles, section notes, and optional add buttons.

#### Scenario: Render read-only and show-all sections
- **GIVEN** a section marked `readOnly = true` or `showAll = true`
- **WHEN** the section renders
- **THEN** all fields are rendered in uneditable or display-only representations without save actions.

### Requirement: Polymorphic Field Component Binding
The system SHALL map abstract `FieldType` definitions to dedicated Compose interactive controls.

#### Scenario: Map field types to UI components
- **GIVEN** a field rendered within a section card
- **WHEN** the UI resolves its component
- **THEN** `FieldType.Switch` renders a `Switch`, `FieldType.Text` renders an `OutlinedTextField`, `FieldType.Secret` renders a password field with visibility toggle, `FieldType.Number` renders a numeric text field with unit label, `FieldType.Choice` renders an exposed dropdown menu or segmented button row, and `FieldType.TimeOfDay` invokes an Android `TimePickerDialog`.

#### Scenario: Evaluate conditional field visibility
- **GIVEN** a field with a `visibleIf` predicate
- **WHEN** dependent field values change
- **THEN** the field is shown or hidden reactively based on the evaluation of the current in-memory values map.

### Requirement: Client-Side Input Validation Presentation
The system SHALL evaluate field constraints on input change and block persistence if any field contains invalid data.

#### Scenario: Display inline validation errors
- **GIVEN** an active input field being edited by the user
- **WHEN** the value violates constraints (e.g., length, numeric range, IP format, or MAC format)
- **THEN** the field displays supporting error text and is highlighted with error colors, and the section save button is disabled until all errors are resolved.

### Requirement: Destructive and Sensitive Operation Confirmation
The system SHALL prompt the user with an explicit confirmation dialog before executing sensitive modifications or danger actions.

#### Scenario: Confirm save with warning message
- **GIVEN** a section with a configured `saveWarning`
- **WHEN** the user taps the save button after making modifications
- **THEN** an `AlertDialog` presents the warning description, requiring explicit user confirmation before committing changes to the router.

#### Scenario: Confirm dangerous page action
- **GIVEN** a `PageAction` marked with `danger = true` (such as router reboot)
- **WHEN** the user taps the action button
- **THEN** an `AlertDialog` appears requiring confirmation before executing the action.

### Requirement: Dynamic Item Creation and Deletion Dialogs
The system SHALL render modal dialogs for adding new list items and require confirmation when deleting items.

#### Scenario: Add item dialog flow
- **GIVEN** a section with an `AddSpec`
- **WHEN** the user taps the floating or inline Add button
- **THEN** a modal dialog prompts for the designated add fields, validates user inputs, and upon submission invokes `PageEngine.add()` before refreshing the section.

#### Scenario: Delete item confirmation flow
- **GIVEN** a deletable section instance
- **WHEN** the user taps the delete icon
- **THEN** an `AlertDialog` prompts for confirmation before invoking `PageEngine.delete()`.

### Requirement: User Feedback and Error Toast Presentation
The system SHALL notify the user of operational success and present translated router errors.

#### Scenario: Success snackbar notification
- **GIVEN** a successful save, add, delete, or action execution
- **WHEN** the operation finishes
- **THEN** the screen displays a Snackbar indicating that changes were applied successfully.

#### Scenario: Translate router error codes
- **GIVEN** an operation that fails with a `RouterException`
- **WHEN** handling the error
- **THEN** the system extracts the router error code and formats a localized error message (e.g., "Router error 71233") in a Snackbar.

### Requirement: Expressive Home Screen Navigation and Grouped Layout
The system SHALL present an expressive home dashboard featuring a glanceable router status hero card, equal-height quick access bento tiles, and grouped container cards for advanced configuration categories using Material 3 Expressive design tokens.

#### Scenario: Glanceable hero router status card
- **GIVEN** an active authenticated session
- **WHEN** the home screen renders
- **THEN** it renders a hero status card at the top displaying the router model, active connection state indicator, router IP address, and tapping the card navigates directly to the full system summary screen.

#### Scenario: Equal-height quick access bento grid
- **GIVEN** the primary navigation items (Summary, Connected Devices, Speed Test, Main Wi-Fi)
- **WHEN** the quick access section renders
- **THEN** it renders these four items in a 2x2 bento grid where each card has equal fixed height, an expressive vibrant shape badge, title, and tap navigation without vertical text wrapping discrepancies.

#### Scenario: Grouped container cards for configuration categories
- **GIVEN** the remaining configuration categories (Wi-Fi settings, local network, NAT & forwarding, security, storage, system)
- **WHEN** rendering configuration categories
- **THEN** each category renders as a cohesive Material 3 container card containing its items as horizontal rows with leading shape badges and trailing navigation chevrons, avoiding orphaned single-column cards.

