# Proposal: DIGI Fiber Diagnostic Assistant

## Why

Optical attenuation anomalies (excessive cable bending, dirty SC-APC connectors, and degraded CTO splitters) are the leading cause of intermittent packet loss, latency spikes, and unexpected disconnects for DIGI FTTH customers. While DIGI routers expose optical telemetry and diagnostic OIDs (`DEV2_OPTC_GPON_CFG`, `DEV2_GPON_INTF_STATS`, `DEV2_ADT_WAN`), non-technical users have no accessible way to know whether an issue is caused by physical fiber degradation, ONT unregistration, WAN IP loss, gateway reachability, or DNS resolution failure.

Introducing the "Asistente DIGI" empowers users with an immediate, self-service diagnostic tool featuring a clear Optical Power Traffic Light (Green/Yellow/Red) and a layered end-to-end connectivity test executed automatically upon entering the screen.

## What Changes

- **Pure Kotlin Optical & Telemetry Engine (`:protocol`)**:
  - Implement parsing and robust normalization for optical transceiver telemetry from `DEV2_OPTC_GPON_CFG` and `DEV2_GPON_INTF_STATS`.
  - Handle varied router output formats (raw floating strings, scaled hundredths of dBm, or missing values).
  - Classify optical power into standard FTTH health brackets:
    - 🟢 **Optimal / Green** (`-15.0 dBm` to `-25.0 dBm`): Clean optical link with optimal attenuation margin.
    - 🟡 **Marginal / Yellow** (`-25.1 dBm` to `-27.9 dBm`): Elevated loss or dirty connector; preventive warning.
    - 🔴 **Critical / Red** (`< -28.0 dBm`): Severe attenuation; imminent disconnect warning.
    - ⚠️ **Saturation** (`> -8.0 dBm`): Excess optical power warning.
    - ⚪ **External ONT Detection**: Graceful fallback when the router uses an external ONT via Ethernet WAN.
  - Implement layered end-to-end diagnostic runner executing:
    1. Optical link health (Rx/Tx power, temperature, voltage).
    2. OMCI registration status at the OLT.
    3. WAN connectivity and public IPv4/IPv6 lease.
    4. DIGI default gateway reachability and ping latency.
    5. DNS resolution and global internet verification.

- **Material 3 Expressive UI & Navigation (`:app`)**:
  - Add `FiberDiagnosticScreen.kt` featuring an interactive optical traffic light gauge, live step-by-step test execution cards with animated progress indicators, and actionable diagnostic summaries.
  - Trigger immediate, non-blocking test execution upon screen entry with manual retry capability.
  - Register `fiber-diag` page in `Catalog.kt` under `Group.STATUS` and connect the Home screen hero banner (*"Pulsa para ver diagnóstico completo"*) directly to the Fiber Assistant.
  - Full bilingual localization in English (`values/strings.xml`) and Spanish (`values-es/strings.xml`).

## Capabilities

### New Capabilities
- `fiber-diagnostics`: Defines system requirements for GPON/XGS-PON optical power evaluation, optical traffic light categorization, and sequential multi-layer end-to-end network connectivity diagnostics with automatic screen-entry execution.

### Modified Capabilities
<!-- None: dashboard-status and other existing specs remain untouched as fiber-diagnostics is a self-contained capability. -->

## Impact

- **`:protocol`**:
  - New models: `OpticalTelemetry`, `OpticalHealthStatus`, `DiagnosticStep`, `DiagnosticResult`.
  - New parser: `OpticalTelemetryParser.kt`.
  - New engine: `FiberDiagnosticEngine.kt` coordinating OID queries and socket/ping verification.
  - Pure unit tests with mock responses in JVM without Android dependencies.
- **`:app`**:
  - New screen: `FiberDiagnosticScreen.kt`.
  - New ViewModel / StateFlow: `FiberDiagnosticViewModel.kt`.
  - Navigation updates in `MainActivity.kt` and `Catalog.kt`.
  - Bilingual string resources added for all diagnostic states, thresholds, and technical descriptions.
