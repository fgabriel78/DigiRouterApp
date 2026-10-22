# Design

## Context

The DIGI Wi-Fi 7 router exposes GPON/XGS-PON optical diagnostics, OMCI synchronization status, and active WAN connection parameters via TR-181 data model objects (`DEV2_OPTC_GPON_CFG`, `DEV2_GPON_INTF_STATS`, `DEV2_GPON_INTF_OMCI_STATS`, `DEV2_ADT_WAN`) and authentication objects (`DEV2_GPON_AUTH`). Currently, `FiberDiagnosticEngine` in `:protocol` evaluates connection health sequentially across five steps (Physical Optical, OMCI, WAN, Gateway Ping, DNS Reachability), while `FiberDiagnosticScreen` in `:app` renders the outcome using Material 3 Expressive cards.

To empower power users and fiber enthusiasts without risking operational instability, this design introduces on-demand WAN session reconnection (forcing IP reallocation) and ONT identity inspection (with exportable SN and PLOAM credentials) directly into the diagnostic interface.

## Goals / Non-Goals

**Goals:**
- Implement automated session recycling for PPPoE (`ACT_PPP_DISCONN` $\rightarrow$ `ACT_PPP_CONN`) and DHCP (`ACT_DHCP_RELEASE` $\rightarrow$ `ACT_DHCP_RENEW`) via `FiberDiagnosticEngine`.
- Read and parse `DEV2_GPON_AUTH` parameters (GPON SN in ASCII and HEX, masked PLOAM password, OLT mode).
- Provide SFP ONT configuration export snippets for third-party GPON/XGS-PON sticks (CarlitoxxPro, OpenWrt, pfSense).
- Support non-destructive OMCI state machine restart via `ACT_OMCI_RESTART`.
- Maintain contextual, expandable inline card UI within `FiberDiagnosticScreen` adhering to Material 3 Expressive tokens and bilingual localization.

**Non-Goals:**
- Modifying or writing ONT credentials back to `DEV2_GPON_AUTH` (strictly read-only and export-only to prevent bricking optical sync).
- Provisioning secondary WAN interfaces or creating static routes.
- Full hardware reboots (the purpose of these actions is specifically to avoid hardware restarts).

## Decisions

### 1. Unified Inline Step Expansion vs Dedicated Tools Tab
- **Decision:** Embed ONT registration inspection and OMCI restart within Step 2 (OMCI Registration), and WAN IP renewal within Step 3 (WAN Session) as expandable detail views.
- **Rationale:** Preserves the cohesive 5-step diagnostic hierarchy. Users immediately see the remedy or technical telemetry in the exact phase where an anomaly occurs rather than switching tabs.
- **Alternatives Considered:**
  - *Dedicated "Fiber Tools" Tab:* Separates read-only diagnostics from active tools, but introduces fragmented navigation and duplicates status indicators.

### 2. Read-and-Export Only Safety Policy for `DEV2_GPON_AUTH`
- **Decision:** Restrict `DEV2_GPON_AUTH` handling in the mobile client to read operations and clipboard export.
- **Rationale:** Writing an invalid GPON SN, invalid PLOAM password, or wrong OLT mode immediately severs the physical OLT registration, causing total service loss that can only be fixed by manual router reset or ISP dispatch.
- **Alternatives Considered:**
  - *Guarded In-App Editing with Confirmation Dialog:* While technically supported by `set(DEV2_GPON_AUTH)`, accidental entry is too hazardous for a mobile companion app.

### 3. Bidirectional GPON Serial Number Representation
- **Decision:** Implement `GponHexUtil` in `:protocol` to convert between 12-character ASCII+HEX (4 ASCII vendor prefix chars + 8 hex digits, e.g. `HWTC12345678`) and 16-character hexadecimal (e.g. `4857544331323334`).
- **Rationale:** Fiber enthusiasts and SFP stick firmware often require pure HEX (OpenWrt/Linux `omcid`), while ISP technicians and outer ONT labels use ASCII+HEX. Providing both with individual copy buttons satisfies both use cases seamlessly.

```
+-------------------------------------------------------------------------+
|                  GPON SERIAL NUMBER CONVERSION LOGIC                    |
+-------------------------------------------------------------------------+

  ASCII+HEX Format (12 characters):
  [ H ][ W ][ T ][ C ] [ 1 ][ 2 ][ 3 ][ 4 ][ 5 ][ 6 ][ 7 ][ 8 ]
    |    |    |    |     |    |    |    |    |    |    |    |
    v    v    v    v     |    |    |    |    |    |    |    |  (Direct
   0x48 0x57 0x54 0x43   |    |    |    |    |    |    |    |   HEX copy)
    |    |    |    |     v    v    v    v    v    v    v    v
  [ 4  8  5  7  5  4  4  3 ] [ 1  2  3  4  5  6  7  8 ]
  Pure HEX Format (16 hexadecimal characters)
```

### 4. Asynchronous WAN Reconnect Flow & Polling State Machine
- **Decision:** Expose a Flow-based state machine in `FiberDiagnosticEngine` that emits progress states during renewal:
  - `IDLE` $\rightarrow$ `DISCONNECTING` $\rightarrow$ `RECONNECTING` $\rightarrow$ `VERIFYING` $\rightarrow$ `SUCCESS` (or `ERROR`).
- **Rationale:** Dropping a session causes a temporary loss of WAN IP. Polling `DEV2_ADT_WAN` with a 10-second timeout guarantees the app waits for connection restoration before emitting the verified new IP.

```
+-------------------------------------------------------------------------+
|                   WAN IP RENEWAL STATE TRANSITION                       |
+-------------------------------------------------------------------------+

  [ Idle: Current IP ]
          |
   (Tap "Renew IP")
          v
  [ DISCONNECTING ]  --> Dispatch ACT_PPP_DISCONN or ACT_DHCP_RELEASE
          |
   (Delay 1500ms)
          v
  [ RECONNECTING ]   --> Dispatch ACT_PPP_CONN or ACT_DHCP_RENEW
          |
   (Poll DEV2_ADT_WAN every 1000ms)
          |
          +-------------------------+
          |                         |
   (Status == "Connected")   (Timeout > 10s)
          |                         |
          v                         v
  [ SUCCESS: New IP ]        [ ERROR: Timeout ]
```

## Risks / Trade-offs

- **[Temporary WAN Packet Loss]** $\rightarrow$ While WAN is disconnecting and reconnecting (approx 3-6s), all active router internet traffic is paused.
  - *Mitigation:* Clear UI warning before initiation explaining that active downloads or video streams will briefly pause.
- **[Stale IP Reporting]** $\rightarrow$ Firmware may momentarily report the old IP address immediately after reconnecting.
  - *Mitigation:* Require `connStatusV4 == "Connected"` and verify that the session has transitioned before capturing the new IP.
- **[Dark Theme Contrast & Credential Masking]** $\rightarrow$ Plaintext credentials on dark backgrounds can suffer glare or unintended exposure.
  - *Mitigation:* PLOAM passwords are fully masked (`••••••••`) by default with an eye toggle, and configuration snippets are copied directly to the system clipboard with an ephemeral toast.

## Migration Plan

- Extends existing `:protocol` engine classes (`FiberDiagnosticEngine.kt`) and models (`OpticalModels.kt`).
- Extends existing Compose screens (`FiberDiagnosticScreen.kt`) and ViewModels (`FiberDiagnosticViewModel.kt`).
- Backward compatible: If `DEV2_GPON_AUTH` returns empty (e.g. on pure DSL or Ethernet WAN models), the ONT section gracefully collapses.
