# Design: DIGI Fiber Diagnostic Assistant

## Context

The router firmware exposes GPON/XGS-PON optical statistics via TR-181 OIDs (`DEV2_OPTC_GPON_CFG`, `DEV2_GPON_INTF_STATS`) and WAN parameters via `DEV2_ADT_WAN`. The codebase separates pure router protocol logic in `:protocol` from Android Compose presentation in `:app`.

The application previously gathered partial GPON stats on the dashboard but neither parsed them into structured domain models nor presented optical link diagnostics. Non-technical users experiencing connection instability need an automated, layered diagnostic tool to differentiate physical fiber faults from network or DNS issues.

## Goals / Non-Goals

**Goals:**
- Provide a pure Kotlin parsing and diagnostic execution engine in `:protocol` with zero Android dependencies.
- Normalize optical reception power (Rx dBm), laser transmission power (Tx dBm), module temperature, and voltage across diverse router firmware representations.
- Classify optical link health into 4 visual traffic light tiers (Optimal Green, Marginal Yellow, Critical Red, Saturation Amber) plus External ONT detection.
- Execute an automated 5-step diagnostic sequence upon screen entry with live step-by-step progress and root-cause failure isolation.
- Design a Material 3 Expressive UI in `:app` with clean typography, dynamic gauge, and manual retry.

**Non-Goals:**
- Automated support ticket dispatch or external carrier API integration (explicitly deferred by user).
- Automated laser recalibration or physical fiber repair (read-only diagnostic).
- Continuous background polling of optical power when the screen is closed (diagnostics run strictly on-demand while viewing the screen).

## Decisions

### 1. Architectural Separation: `:protocol` Engine vs `:app` Presentation

```
+-----------------------------------------------------------------------+
|                              :protocol                                |
|                                                                       |
|   RouterClient ──> OpticalTelemetryParser ──> OpticalTelemetry       |
|          │                                            │               |
|          └──> FiberDiagnosticEngine ──────────────────┴──────────┐    |
|               (5-step sequential runner, Flow<DiagnosticState>)  │    |
+──────────────────────────────────────────────────────────────────┼────+
                                                                   │
+──────────────────────────────────────────────────────────────────┼────+
|                                :app                              │    |
|                                                                  v    |
|   FiberDiagnosticViewModel ───────────────────────────> StateFlow     |
|          │                                                            |
|          v                                                            |
|   FiberDiagnosticScreen (Jetpack Compose, Material 3 Expressive)      |
|   * Traffic Light Gauge & Rx Metric                                   |
|   * Sequential Step Cards (Pending -> Running -> Done / Failed)       |
|   * Manual Re-run Action                                              |
+-----------------------------------------------------------------------+
```

- **Rationale**: Isolating data extraction, string/integer normalization, and diagnostic logic in `:protocol` keeps all tests runnable on JVM without Robolectric or Android emulator, maintaining fast CI and clean architectural boundaries.
- **Alternatives Considered**: Executing diagnostic ping and DNS checks directly inside the ViewModel in `:app`. Rejected to keep core diagnostics reusable and fully unit-testable.

### 2. Telemetry Normalization, Hybrid Querying & Robust Number Parsing

Routers may output optical power in varied formats and table structures:
1. Multi-instance table OIDs (`DEV2_OPTC_GPON_CFG`, `DEV2_GPON_INTF_STATS`, `DEV2_OPTC_INTF`):
   - Router firmware often rejects single-object `"go"` (`client.get()`) operations on list tables.
   - `ClientTelemetrySource` employs hybrid resolution: querying `"gl"` (`client.getList()`) first, falling back to `"go"` (`client.get()`) if empty or rejected, merging candidate optical sources.
2. Formatted decimal string: `"-21.45 dBm"` or `"-21.45"`.
3. Scaled integer: `-2145` (hundredths of dBm) or `-21450` (thousandths of dBm).
4. Standard TR-181 property names: `opticalSignalLevel`, `transmitOpticalLevel`, `rxOpticalPower`, `txOpticalPower`, `rxPwr`.
5. Empty / unexposed fields on active connections:
   - When optical dBm metrics are omitted by firmware or operating over Ethernet WAN, but the WAN connection is `Connected` with an active IP lease or GPON bytes flow, Step 1 degrades gracefully to an informational warning rather than failing and aborting the remaining 4 diagnostic steps.

`OpticalTelemetryParser` converts inputs into standardized `Float` values:
- Values between `-100.0` and `10.0` are treated directly as dBm.
- Values with absolute magnitudes `> 100` are normalized by dividing by 100.0 (or 1000.0).
- If no optical values are found or values indicate `0` with Ethernet WAN active, status is mapped to `OpticalHealthStatus.EXTERNAL_ONT`.

### 3. Traffic Light Thresholds & Null/Zero dBm Handling

```
       0 dBm                      -8 dBm                -15 dBm               -25 dBm         -28 dBm              -40 dBm
  <----+-----------------------------+---------------------+---------------------+---------------+--------------------+---->
       |          ⚠️ SATURATED       |     🟢 OPTIMAL      |      🟢 OPTIMAL     |  🟡 MARGINAL  |     🔴 CRITICAL    |
       |  (-8.0 dBm < Rx < 0.0 dBm)  | (-8.0 to -15.0 dBm) | (-15.0 to -25.0 dBm)|(-25.1 to -27.9)| (Rx < -28.0 dBm)   |
```

- **0.0 dBm / Non-negative dummy metric**: Real GPON optical links never receive exactly `0.000 dBm` (1.0 mW). Chipset firmware outputs `0` or `0.0` when optical power is unmeasured or uncalibrated. The parser maps `rxDbm == 0.0f` to `UNKNOWN` (unexposed) rather than `SATURATED`.
- **True Saturation**: Defined strictly for high negative optical power where `-8.0 dBm < rxDbm < 0.0 dBm`.
- **Rationale**: ITU-T G.984 GPON Class B+ receiver sensitivity minimum is `-27 dBm` to `-28 dBm`. Class C+ is `-30 dBm`. XGS-PON N1/N2 is `-28 dBm`. Reception weaker than `-28 dBm` introduces packet loss and CRC errors. The `-25` to `-27.9 dBm` window serves as a preventive threshold (warning to check patch cord before total loss).
- **Alternatives Considered**: A binary OK/Fail check. Rejected because optical attenuation degradation is gradual and preventive notice avoids emergency disconnects.

### 4. 5-Layer Diagnostic Sequence Flow

```
+---------------+
| Screen Opened |
+-------┬-------+
        │
        v
+─────────────────────────────────+     FAIL
| Step 1: Optical Link & Power    | ───────────> [ Halt: Physical Fiber Fault ]
+───────┬─────────────────────────+
        │ PASS / External ONT
        v
+─────────────────────────────────+     FAIL
| Step 2: OMCI & OLT Registration | ───────────> [ Halt: OLT Registration Error ]
+───────┬─────────────────────────+
        │ PASS
        v
+─────────────────────────────────+     FAIL
| Step 3: WAN Session & IP Lease  | ───────────> [ Halt: WAN Authentication Error ]
+───────┬─────────────────────────+
        │ PASS
        v
+─────────────────────────────────+     FAIL
| Step 4: Default Gateway Ping    | ───────────> [ Halt: Local DIGI Gateway Drop ]
+───────┬─────────────────────────+
        │ PASS
        v
+─────────────────────────────────+     FAIL
| Step 5: DNS & Global Internet   | ───────────> [ Halt: DNS / External Outage ]
+───────┬─────────────────────────+
        │ PASS
        v
+─────────────────────────────────+
| Complete: All Layers Healthy 🟢 |
+─────────────────────────────────+
```

### 5. UI Integration & Navigation

- **Primary Entry**: The Home screen hero card (`HeroStatusCard`) currently displays *"Pulsa para ver diagnóstico completo"*. Tapping this hero card navigates directly to `fiber-diag` (`FiberDiagnosticScreen`).
- **Catalog Entry**: `fiber-diag` registered in `Catalog.pages` under `Group.STATUS`, allowing navigation from the drawer/grid.
- **Immediate Execution**: Screen initializes a `LaunchedEffect(runKey)` that begins execution on entry without requiring user tap.

### 6. Three-Tier Overall Diagnostic Outcome (Success / Warning / Failed)

Previously, the diagnostic outcome was strictly binary (`isSuccess` true/false). If Step 1 had a `WARNING` (e.g. unexposed power or saturation), the bottom summary card displayed a misleading green "All connectivity layers operational" banner.

The summary architecture evolves to 3 distinct tiers:
- **`SUCCESS` (Green)**: 0 failed steps, 0 warning steps. Displays `fiber_diag_all_passed` ("All connectivity layers operational").
- **`WARNING` (Amber)**: 0 failed steps, 1+ warning steps. Displays `fiber_diag_passed_with_warnings` ("Connection operational with physical link warnings") with an explanatory warning banner and amber warning icon.
- **`FAILED` (Red)**: 1+ failed steps. Displays `fiber_diag_has_issues` ("Connection problems detected") pointing to the isolated failure step.

## Risks / Trade-offs

- **[Risk] Router returns unexpected OID key names for optical metrics** → *Mitigation*: Inspect multiple known field names (`rxPower`, `rxOptPower`, `rx_power`, `opticalRxPower`) in priority order with case-insensitive fallbacks.
- **[Risk] External ONT setups raise false alarm** → *Mitigation*: Check whether active WAN interface is Ethernet-based and suppress optical failure when transceiver is absent.
- **[Risk] Gateway ping blocks on restrictive firewall** → *Mitigation*: Use lightweight TCP/HTTP probe to port 53/80 of gateway if ICMP ping is restricted by ISP.
- **[Risk] Dark theme contrast on colored status banners** → *Mitigation*: Use semantic Material 3 Expressive container tokens (`primaryContainer`, `errorContainer`, `tertiaryContainer`) with matching on-container text colors.
