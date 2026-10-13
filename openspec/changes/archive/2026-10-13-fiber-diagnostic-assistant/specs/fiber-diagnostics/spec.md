# Spec Delta

## Purpose
Provides GPON/XGS-PON optical power evaluation, optical traffic light health classification, and automated layered end-to-end network connectivity diagnostics.

## ADDED Requirements

### Requirement: Optical Power Telemetry and Traffic Light Categorization
The system SHALL extract optical transceiver telemetry from the router (`DEV2_OPTC_GPON_CFG` and `DEV2_GPON_INTF_STATS`), normalize reception power values (Rx dBm), and categorize optical link health into distinct traffic light status levels.

#### Scenario: Optimal optical power level (Green)
- **GIVEN** active optical telemetry reporting received power between `-15.0 dBm` and `-25.0 dBm` inclusive
- **WHEN** the optical health is evaluated
- **THEN** the system classifies the link as `OPTIMAL` (Green), indicating optimal attenuation margin and healthy fiber connection.

#### Scenario: Marginal optical attenuation warning (Yellow)
- **GIVEN** active optical telemetry reporting received power between `-25.1 dBm` and `-27.9 dBm` inclusive
- **WHEN** the optical health is evaluated
- **THEN** the system classifies the link as `MARGINAL` (Yellow), indicating elevated insertion loss or dirty optical connector with preventive advisory.

#### Scenario: Critical optical attenuation danger (Red)
- **GIVEN** active optical telemetry reporting received power less than `-28.0 dBm`
- **WHEN** the optical health is evaluated
- **THEN** the system classifies the link as `CRITICAL` (Red), warning of severe fiber bend, physical damage, or imminent service disconnection.

#### Scenario: Optical saturation warning
- **GIVEN** active optical telemetry reporting received power strictly between `-8.0 dBm` and `0.0 dBm`
- **WHEN** the optical health is evaluated
- **THEN** the system classifies the link as `SATURATED` (Amber/Warning), alerting to abnormal optical receiver saturation.

#### Scenario: Zero or uninitialized optical power handling
- **GIVEN** router telemetry reporting optical reception power as `0.0 dBm`, `0`, or uninitialized non-negative value
- **WHEN** the optical health is evaluated
- **THEN** the system does not misclassify the metric as optical saturation, instead treating it as unexposed telemetry (`UNKNOWN`), allowing non-blocking degradation if WAN connectivity is active.

#### Scenario: External ONT detection via Ethernet WAN
- **GIVEN** a router configuration operating via external ONT connected over an Ethernet WAN port with no internal GPON transceiver telemetry
- **WHEN** the optical health is evaluated
- **THEN** the system identifies the connection as `EXTERNAL_ONT` (Informational), bypassing internal optical thresholds without raising a false fiber alarm.

#### Scenario: Heterogeneous telemetry string, integer normalization, and TR-181 key aliases
- **GIVEN** raw router telemetry outputs provided as decimal strings (e.g. `"-21.4 dBm"`), scaled integers in hundredths of dBm (e.g. `-2140`), or standard TR-181 property names (`opticalSignalLevel`, `rxOpticalPower`, `transmitOpticalLevel`)
- **WHEN** the optical telemetry parser processes the input
- **THEN** the parser normalizes the value into a standard floating-point dBm metric, extracting complementary transceiver metrics including laser transmission power (Tx dBm), module temperature (°C), and voltage (V).

#### Scenario: Unexposed optical telemetry graceful degradation when WAN is active
- **GIVEN** an active fiber connection where the router firmware does not expose optical transceiver dBm metrics (or returns empty optical OIDs) but the WAN session is `Connected` or GPON traffic is flowing
- **WHEN** the optical health is evaluated
- **THEN** the system classifies Step 1 as an informational warning without aborting the diagnostic pipeline, permitting subsequent validation of OMCI, WAN lease, gateway reachability, and DNS.

### Requirement: Layered End-to-End Connectivity Diagnostic Test
The system SHALL execute a multi-layer diagnostic sequence evaluating the complete connection stack from physical optical layer to global internet reachability.

#### Scenario: Automatic sequential execution upon screen entry
- **GIVEN** the user navigates to the Fiber Diagnostic Assistant screen
- **WHEN** the screen is composed
- **THEN** the system automatically initiates the layered diagnostic sequence without requiring manual trigger.

#### Scenario: Step 1 Physical optical link validation with hybrid OID querying
- **GIVEN** the diagnostic sequence starts
- **WHEN** Step 1 executes
- **THEN** the system queries optical transceiver telemetry using hybrid `getList` and `get` fallbacks across `DEV2_OPTC_GPON_CFG`, `DEV2_GPON_INTF_STATS`, and `DEV2_OPTC_INTF`, validates Rx and Tx power margins, and reports physical layer status without blocking subsequent stages if connection is active.

#### Scenario: Step 2 OMCI and OLT registration verification
- **GIVEN** physical link verification completes
- **WHEN** Step 2 executes
- **THEN** the system verifies OMCI link status and confirms successful ONT registration with the central optical line terminal (OLT).

#### Scenario: Step 3 WAN session and IP address lease verification
- **GIVEN** Step 2 succeeds
- **WHEN** Step 3 executes
- **THEN** the system inspects active WAN connection status (`DEV2_ADT_WAN`) to verify that the IPoE/PPPoE session is established and public IPv4/IPv6 leases are assigned.

#### Scenario: Step 4 DIGI default gateway reachability and latency check
- **GIVEN** Step 3 succeeds
- **WHEN** Step 4 executes
- **THEN** the system tests network reachability to the assigned DIGI default gateway IP, verifying packet round-trip time and latency.

#### Scenario: Step 5 DNS resolution and global internet verification
- **GIVEN** Step 4 succeeds
- **WHEN** Step 5 executes
- **THEN** the system executes DNS host resolution queries against configured name servers and verifies HTTP/socket reachability to global internet destinations.

#### Scenario: Diagnostic step failure isolation
- **GIVEN** an error or timeout encountered at any stage of the diagnostic sequence
- **WHEN** the failed step is identified
- **THEN** the sequence flags the specific failing layer, terminates subsequent dependent tests, and provides an actionable diagnostic explanation pinpointing the root cause.

#### Scenario: Three-tier overall diagnostic outcome summary
- **GIVEN** all diagnostic steps complete without blocking failures
- **WHEN** the overall summary is evaluated
- **THEN** the system classifies the overall outcome as `WARNING` (Amber/Notice) if any individual step yielded a warning (e.g. unexposed optical power, marginal signal, or saturation) providing a nuanced message ("Operational with physical link warnings"), and classifies as `SUCCESS` (Green) only when all steps pass cleanly without warnings.

### Requirement: Interactive Diagnostic Screen and Lifecycle Controls
The system SHALL present a dedicated diagnostic screen featuring an optical traffic light gauge, step-by-step progress cards, and manual re-run controls.

#### Scenario: Dedicated navigation entry and hero banner integration
- **GIVEN** an authenticated application session
- **WHEN** the user interacts with the Home screen hero banner action or selects the Fiber Diagnostic item from the navigation catalog
- **THEN** the application transitions directly to the Fiber Diagnostic Assistant screen.

#### Scenario: Real-time progress feedback during test execution
- **GIVEN** a diagnostic run in progress
- **WHEN** individual test steps transition between pending, running, success, or failed states
- **THEN** the UI displays animated progress indicators on active steps and checks off completed stages in real time.

#### Scenario: Manual re-run of diagnostic test
- **GIVEN** a completed or failed diagnostic run
- **WHEN** the user activates the re-run / refresh button
- **THEN** the system resets the step indicators and re-executes the complete diagnostic sequence from Step 1.
