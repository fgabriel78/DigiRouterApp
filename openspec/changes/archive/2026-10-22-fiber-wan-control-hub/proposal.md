# Proposal

## Why

While DIGI FTTH routers expose GPON/XGS-PON optical diagnostics and WAN session statistics, users frequently experience stalled sessions or need to rotate dynamic public IP addresses (e.g., escaping restrictive CGNAT pools or refreshing expired leases). Currently, their only recourse is a disruptive, 120-second hardware reboot that severs the local Wi-Fi and LAN network. Furthermore, fiber enthusiasts and power users wishing to inspect their ONT registration parameters (GPON Serial Number in ASCII/HEX, PLOAM password, OLT mode) or back them up for third-party SFP ONT sticks have no mobile interface to access `DEV2_GPON_AUTH` or trigger non-destructive OMCI restarts (`ACT_OMCI_RESTART`).

Integrating on-demand WAN session recycling and ONT registration inspection directly into the existing Asistente DIGI / Fiber Diagnostics screen transforms it from a passive read-only monitor into an active, layered diagnostic and control hub.

## What Changes

- **On-Demand WAN Reconnect & Public IP Renewal**:
  - Implement automated session drop and re-establishment for active WAN connections via TR-181 action primitives (`ACT_PPP_DISCONN` / `ACT_PPP_CONN` for PPPoE and `ACT_DHCP_RELEASE` / `ACT_DHCP_RENEW` for DHCP).
  - Add an interactive "Renew Public IP / Reconnect" control to the WAN diagnostic card in `FiberDiagnosticScreen` with animated progress and new IP verification.
- **GPON ONT Registration & Telemetry Inspection**:
  - Read `DEV2_GPON_AUTH` parameters (GPON Serial Number, PLOAM password, OLT mode) on stack `1,0,0,0,0,0`.
  - Provide dual representation for GPON SN: standard ASCII+HEX (e.g., `HWTC12345678`) and raw 16-character hexadecimal (e.g., `4857544331323334`) with bidirectional conversion utilities.
  - Implement masked PLOAM password display with reveal/copy actions.
  - Provide a one-tap "Export ONT Config" snippet formatted for third-party SFP ONT sticks and OpenWrt/pfSense routers.
  - Enforce a strict **Read & Export Only** safety policy for credentials (no mutations written to `DEV2_GPON_AUTH` to prevent accidental service loss).
- **Targeted OMCI Lifecycle Restart**:
  - Provide an on-demand "Restart OMCI" action (`ACT_OMCI_RESTART`) within the OMCI Registration step card to re-trigger the O1..O5 handshake without a hardware restart.

## Capabilities

### New Capabilities
*(None; builds upon the existing fiber diagnostics and network assistant domain)*

### Modified Capabilities
- `fiber-diagnostics`: Add requirements for GPON ONT registration inspection, dual-format SN conversion, SFP configuration export, on-demand OMCI restart, and automated WAN session recycling / IP renewal.

## Impact

- **`:protocol`**:
  - Extend `FiberDiagnosticEngine.kt` and `OpticalModels.kt` with `GponAuthInfo`, `WanSessionControl`, and action runners for `ACT_PPP_*`, `ACT_DHCP_*`, and `ACT_OMCI_RESTART`.
  - Add `GponHexUtil.kt` for vendor prefix ASCII $\longleftrightarrow$ HEX conversion and export formatting.
  - Add unit tests verifying parsing, SN conversion, and action execution sequences.
- **`:app`**:
  - Update `FiberDiagnosticScreen.kt` to expand Step 2 (OMCI Registration) with the ONT registration card and OMCI restart button.
  - Expand Step 3 (WAN Session) with the "Renew Public IP / Reconnect" button and in-flight state tracking.
  - Add bilingual strings in English (`values/strings.xml`) and Spanish (`values-es/strings.xml`).
