# catalog-engine Specification

## MODIFIED Requirements

### Requirement: Declarative Hierarchy and Schema Specification
The system SHALL model router configuration screens declaratively using `Page`, `Section`, `Field`, and typed metadata structures.

#### Scenario: Structure pages into sections and fields
- **GIVEN** a configuration domain definition
- **WHEN** a `Page` is declared in `Catalog`
- **THEN** it defines an identifier, title, functional `Group`, descriptive subtitle (`description`), whether it requires admin privileges, and an ordered list of `Section` elements.

#### Scenario: Support typed field definitions
- **GIVEN** a section field declaration
- **WHEN** a `Field` is defined
- **THEN** it specifies an attribute key, label, help text, optional conditional visibility predicate (`visibleIf`), and a polymorphic `FieldType` (Switch, Text, Secret, Number, Choice, Ipv4, Mac, TimeOfDay, Info).

#### Scenario: Register custom VPN server page in network group
- **GIVEN** the catalog page registry
- **WHEN** `Catalog.pages` is initialized
- **THEN** it contains a `Page` with id `"vpn"`, functional group `Group.NAT`, `custom = true`, and `adminOnly = true`.

#### Scenario: Register network topology page in status group
- **GIVEN** the catalog page registry
- **WHEN** `Catalog.pages` is initialized
- **THEN** it contains a `Page` with id `"topology"`, functional group `Group.STATUS`, and `custom = true`.

#### Scenario: Consolidate related micro-sections into multi-section pages
- **GIVEN** related router configuration sections
- **WHEN** `Catalog.pages` is initialized
- **THEN** `"guest"` includes guest and multi-SSID sections, `"wifi-adv"` includes advanced options, band steering, and WPS sections, `"dhcp-static"` includes DHCP reservations and ARP binding sections, `"upnp"` includes UPnP, active mappings, and port triggering sections, `"dmz"` includes DMZ host and ALG sections, and `"time"` includes system time, router LEDs, and traffic statistics sections.

#### Scenario: Resolve legacy page identifiers via alias lookup
- **GIVEN** a lookup call to `Catalog.byId(id)` with a merged legacy identifier (`"mssid"`, `"wps"`, `"arp"`, `"port-trigger"`, `"alg"`, `"led"`, or `"stats"`)
- **WHEN** the catalog resolves the page
- **THEN** it returns the consolidated parent `Page` (`"guest"`, `"wifi-adv"`, `"dhcp-static"`, `"upnp"`, `"dmz"`, or `"time"` respectively).
