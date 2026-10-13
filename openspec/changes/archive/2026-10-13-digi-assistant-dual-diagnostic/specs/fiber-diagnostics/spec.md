# Spec Delta

## ADDED Requirements

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
