# fiber-diagnostics Specification

## Purpose
Provides GPON/XGS-PON optical power evaluation, optical traffic light health classification, and automated layered end-to-end network connectivity diagnostics.

## Requirements

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

### Requirement: Dedicated Asistente DIGI Catalog Group and Navigation
The system SHALL provide an explicit "Asistente DIGI" category in the navigation catalog exposing specialized network diagnostic tools without intermediate hub screens.

#### Scenario: Display available diagnostic tools in Asistente DIGI catalog group
- **GIVEN** an active router management session
- **WHEN** the user views the application catalog on the Home screen
- **THEN** the system displays an "Asistente DIGI" category containing distinct menu items for "Diagnóstico de fibra" (`fiber-diag`) and "Diagnóstico dual" (`dual-diag`).

#### Scenario: Direct navigation to diagnostic tools
- **GIVEN** the Home catalog is displayed
- **WHEN** the user taps either "Diagnóstico de fibra" or "Diagnóstico dual"
- **THEN** the application transitions directly to the selected tool screen (`fiber-diag` or `dual-diag`), and back navigation returns directly to the Home screen.

#### Scenario: Home screen Hero Card opens Fiber Diagnostic
- **GIVEN** the Home screen is displayed
- **WHEN** the user taps the HeroStatusCard action banner
- **THEN** the application opens the Optical Fiber Diagnostic (`fiber-diag`) screen directly.

### Requirement: Dual Diagnostic Measurement (Router Hardware vs Mobile Client)
The system SHALL coordinate simultaneous latency measurements from both the router's internal hardware and the mobile application client towards a standardized public Internet target (Cloudflare `1.1.1.1`).

#### Scenario: Router hardware ping execution via DEV2_DIAG_IPPING
- **GIVEN** an active diagnostic run
- **WHEN** the router diagnostic stage triggers
- **THEN** the system issues a TR-181 ping request to `DEV2_DIAG_IPPING` targeting `1.1.1.1` with 4 repetitions, polls for completion, and extracts average latency (ms), minimum/maximum latency, and packet loss count.

#### Scenario: Mobile client Wi-Fi LAN round-trip probe
- **GIVEN** an active diagnostic run
- **WHEN** the local wireless stage triggers
- **THEN** the mobile application client measures round-trip socket latency directly against the local router gateway IP (`192.168.1.1`), computing local Wi-Fi latency (ms) and jitter.

#### Scenario: Mobile client end-to-end Internet probe
- **GIVEN** an active diagnostic run
- **WHEN** the client Internet stage triggers
- **THEN** the mobile application client measures direct round-trip latency against Cloudflare `1.1.1.1`, capturing total user-perceived network latency.

#### Scenario: Graceful fallback when router OID ping is unexposed
- **GIVEN** router firmware that rejects or times out on `DEV2_DIAG_IPPING`
- **WHEN** the dual diagnostic runs
- **THEN** the system falls back to calculating fiber WAN latency by subtracting local Wi-Fi round-trip time from the mobile client's end-to-end Internet latency, without crashing or aborting the test.

### Requirement: Comparative Latency Analysis and Bottleneck Isolation Verdict
The system SHALL analyze the discrepancy between local Wi-Fi latency and router WAN fiber latency to emit an unambiguous root-cause diagnosis.

#### Scenario: Local Wi-Fi bottleneck isolation
- **GIVEN** router hardware WAN latency is low (<= 20 ms) with 0% packet loss, but mobile Wi-Fi latency is elevated (> 25 ms or high jitter)
- **WHEN** the verdict engine evaluates the metrics
- **THEN** the system classifies the outcome as `WIFI_BOTTLENECK`, displaying: "El problema de velocidad está en tu señal Wi-Fi dentro de casa, no en la fibra de DIGI" and recommending switching to 5 GHz or reducing distance to the router.

#### Scenario: Carrier fiber network bottleneck isolation
- **GIVEN** mobile Wi-Fi latency to the router is optimal (<= 10 ms), but router WAN latency to Cloudflare is elevated (> 50 ms) or experiencing packet loss
- **WHEN** the verdict engine evaluates the metrics
- **THEN** the system classifies the outcome as `FIBER_BOTTLENECK`, displaying: "El problema está en la red exterior de fibra, tu conexión Wi-Fi es óptima" and explaining possible external carrier congestion.

#### Scenario: All optimal connection verification
- **GIVEN** mobile Wi-Fi latency is optimal (<= 12 ms) and router WAN latency is optimal (<= 25 ms) with 0% packet loss
- **WHEN** the verdict engine evaluates the metrics
- **THEN** the system classifies the outcome as `ALL_OPTIMAL`, displaying: "Tanto la fibra de DIGI como tu red Wi-Fi funcionan a pleno rendimiento".

#### Scenario: Dual degradation detection
- **GIVEN** both mobile Wi-Fi latency and router WAN latency exceed nominal thresholds
- **WHEN** the verdict engine evaluates the metrics
- **THEN** the system classifies the outcome as `BOTH_DEGRADED`, indicating performance loss across both wireless and fiber links.

