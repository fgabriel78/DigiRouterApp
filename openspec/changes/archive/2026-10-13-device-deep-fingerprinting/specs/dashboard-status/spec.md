# Spec Delta

## ADDED Requirements

### Requirement: Deep Local Network Fingerprinting
The system SHALL perform non-blocking local network discovery using mDNS, SSDP, and targeted HTTP endpoints when the user views connected devices or network topology to definitively identify hardware models, verified manufacturers, and device categories.

#### Scenario: Automatic non-blocking scan on screen entry
- **GIVEN** the user enters the Connected Devices screen or Topology Explorer screen
- **WHEN** the list of connected clients is loaded
- **THEN** the system automatically launches a non-blocking background discovery job that holds a `WifiManager.MulticastLock` for at most 3 seconds to capture mDNS and SSDP announcements without stalling UI rendering.

#### Scenario: Resolving UPnP and SSDP advertised devices
- **GIVEN** an active host responding to SSDP `M-SEARCH` queries or advertising UPnP services
- **WHEN** SSDP headers or associated device descriptor XMLs indicate device identity (such as PlayStation 5, LG webOS TV, or Sonos speaker)
- **THEN** the system extracts the exact model name and manufacturer, categorizes the device (e.g., `CONSOLE`, `TV`), and marks the vendor source as `DEEP_FINGERPRINT`.

#### Scenario: Resolving mDNS and Bonjour service announcements
- **GIVEN** an active host on the local subnet advertising mDNS services (such as `_googlecast._tcp`, `_airplay._tcp`, `_hue._tcp`, `_printer._tcp`)
- **WHEN** service PTR/SRV or TXT records containing model attributes (such as Chromecast model, Apple model identifier, or Philips Hue bridge) are received
- **THEN** the system resolves the exact hardware model name, verified manufacturer, and matching device category.

#### Scenario: Targeted HTTP fallback probes for unresolved hosts
- **GIVEN** a connected host on the router that remains unresolved after the multicast window and has no user alias
- **WHEN** the fallback phase executes
- **THEN** the system executes bounded concurrent HTTP probes (maximum 4 parallel coroutines, timeout <= 1200ms) to candidate local ports (e.g., 80, 8008, 8080) to inspect endpoints such as `/description.xml` or `/setup/eureka_info`.

#### Scenario: Precedence and local cache persistence
- **GIVEN** a connected device with multiple identification attributes
- **WHEN** the display title, manufacturer, and category are resolved
- **THEN** the system enforces precedence:
  1. User custom alias and custom category override all other sources.
  2. Deep fingerprint exact model and verified category take precedence over hardware OUI and hostname heuristics.
  3. Hardware IEEE OUI manufacturer applies when deep fingerprinting is absent.
  4. Hostname heuristics apply when OUI is unavailable or MAC is private.
- **AND** the verified fingerprint metadata is cached locally keyed by MAC address to eliminate repeated network queries in future sessions.

#### Scenario: Verified fingerprint visual indicator
- **GIVEN** a connected device resolved via deep fingerprinting
- **WHEN** the device card is rendered in the devices list or inside the device details modal bottom sheet
- **THEN** the UI displays the exact model name as the title (unless overridden by an alias) and shows a distinct badge indicating verified network fingerprint identification.
