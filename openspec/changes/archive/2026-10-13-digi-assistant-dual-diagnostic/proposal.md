# Proposal

## Why

Users experiencing connection slowdowns, packet loss, or latency spikes frequently cannot identify whether the root cause is their in-home wireless environment (Wi-Fi range, channel interference, physical obstacles) or DIGI's fiber infrastructure.

While the application provides an integrated optical GPON diagnostic, users lack an organized "Asistente DIGI" section in the main catalog and a comparative test that benchmarks the router's hardware connection against the mobile device's connection to isolate wireless vs fiber bottlenecks.

## What Changes

- **Dedicated "Asistente DIGI" Catalog Group (`Group.ASSISTANT`)**: Group diagnostic tools under an explicit "Asistente DIGI" category instead of generic status groupings, providing direct top-level access to:
  1. **Optical Fiber Diagnostic (`fiber-diag`)**: Existing 5-layer optical power and registration diagnostic.
  2. **Dual Diagnostic (`dual-diag`)**: Comparative latency and bottleneck analysis between router hardware and mobile Wi-Fi.
- **Direct Navigation Architecture**: Eliminate unnecessary intermediate hub screens (`digi-assistant-hub`), allowing users to launch either diagnostic tool directly from the catalog and return smoothly to Home.
- **Router Hardware Ping Execution (`DEV2_DIAG_IPPING`)**: Leverage the router's TR-181 diagnostic tool (`diagnostic.htm` / `DEV2_DIAG_IPPING`) to execute ICMP ping tests directly from router hardware towards a high-availability Internet reference (Cloudflare `1.1.1.1`).
- **Mobile Client Ping & Local Wi-Fi Probe**: Measure mobile client round-trip latency towards the local router gateway (`192.168.1.1`) and towards Cloudflare (`1.1.1.1`), computing local Wi-Fi latency and jitter.
- **Automated Bottleneck Isolation & Verdict Engine**: Compare router WAN latency against mobile LAN latency to emit an unambiguous diagnosis (`WIFI_BOTTLENECK`, `FIBER_BOTTLENECK`, `ALL_OPTIMAL`, or `BOTH_DEGRADED`) with actionable user recommendations.
- **Home Navigation Wiring**: Retain direct navigation from `HeroStatusCard` to Optical Fiber Diagnostic (`fiber-diag`).

## Capabilities

### New Capabilities

*(None)*

### Modified Capabilities

- `fiber-diagnostics`: Add requirements for the dedicated "Asistente DIGI" catalog group, direct diagnostic navigation, router hardware ping execution via `DEV2_DIAG_IPPING`, mobile local/remote network probing, and comparative bottleneck verdict isolation.

## Impact

- `:protocol`:
  - Add `Group.ASSISTANT` with English source title "DIGI Assistant" and Spanish translation "Asistente DIGI".
  - Add domain models for dual diagnostics (`DualDiagnosticMetric`, `DualDiagnosticVerdict`, `DualDiagnosticState`).
  - Add `RouterPingClient` / `DualDiagnosticEngine` executing `DEV2_DIAG_IPPING` on the router and local/remote socket probes.
  - Unit tests covering verdict evaluation and simulation of router diagnostic responses.
- `:app`:
  - New `DualDiagnosticScreen.kt` and `DualDiagnosticViewModel.kt` featuring live comparison metrics and dynamic verdict styling.
  - Navigation updates in `MainActivity.kt`, `Catalog.kt`, and `Components.kt`.
  - Bilingual string resources in `values/strings.xml` and `values-es/strings.xml`.
