# Design

## Context

RouterApp connects to DIGI's TP-Link XGB430v Pro router using encrypted TR-181/GDPR RPC calls under the unprivileged `user` account. While administrative functions (firmware, PON provisioning, routing tables) are locked behind operator credentials, extensive security-relevant configurations are directly queryable and mutable via `RouterClient`:
- DMZ Host (`DEV2_DMZ_HOST_CFG`)
- Wi-Fi radios and SSIDs (`DEV2_ADT_WIFI_COMMON`)
- UPnP configuration and active mappings (`DEV2_UPNP_CFG`, `DEV2_UPNP_PORTMAPPING`)
- Virtual servers / NAT port forwarding (`DEV2_PORTMAPPING`)
- Denial of Service flood filters (`DEV2_DDOS_CFG`)
- USB storage servers (`DEV2_FTP_SERVER`, `DEV2_SMB_SERVICE`)

See `proposal.md` for motivation and capability definitions.

## Goals / Non-Goals

**Goals:**
- Implement a pure Kotlin `SecurityAuditEngine` in `:protocol` that evaluates router security state across 8+ dimensions and calculates a normalized 0–100 health score.
- Eliminate false positives on UPnP port mappings by distinguishing benign high-port gaming/voice traffic from high-risk services.
- Provide automated 1-tap batch remediation for non-disruptive vulnerabilities.
- Deliver an expressive Material 3 UI with animated circular score progress, status filters, and an urgent dashboard alert banner for critical risks.
- Ensure 100% JVM unit test coverage for scoring heuristics, UPnP evaluation, and remediation planning.

**Non-Goals:**
- Deep packet inspection (DPI) or custom iptables scripting (unsupported by router under `user` account).
- Automated silent changes to Wi-Fi passphrases without user-specified values.
- Continuous background polling daemon (scans run on-demand or upon dashboard refresh).

## Decisions

### 1. Architectural Division: Engine in `:protocol`, Presentation in `:app`

```
+-----------------------------------------------------------------------+
|                              :protocol                                |
|                                                                       |
|   RouterClient -----> SecurityAuditEngine -----> SecurityReport      |
|                             |                         |               |
|                             +--> RiskyPortCatalog     +--> Findings   |
|                             |                         +--> Score      |
|                             +--> BatchRemediationPlan                 |
+-----------------------------------------------------------------------+
                                   |
                                   v
+-----------------------------------------------------------------------+
|                                :app                                   |
|                                                                       |
|   SecurityHealthViewModel                                             |
|        |                                                              |
|        +---> DashboardScreen (CriticalAlertBanner + SummaryCard)      |
|        |                                                              |
|        +---> SecurityHealthScreen (ScoreGauge, FilterChips, FixDialog)|
+-----------------------------------------------------------------------+
```

- **Rationale:** Keeps audit algorithms, port risk rules, and remediation command generation independent of the Android SDK. Enables exhaustive JVM unit testing with mock router responses.
- **Alternatives Considered:** Embedding evaluation logic inside `SecurityHealthViewModel`. Rejected because it hampers headless testing and violates project architecture constraints.

### 2. Scoring Model and Severity Deductions

The evaluation begins with a base score of 100 points. Penalties are deducted based on finding severity:

```
+-------------------+-------------+---------+------------------------------------+
| Check             | Severity    | Penalty | Target OID                         |
+-------------------+-------------+---------+------------------------------------+
| DMZ Disabled      | Critical    | -25 pts | DEV2_DMZ_HOST_CFG                  |
| Wi-Fi Encryption  | Critical    | -25 pts | DEV2_ADT_WIFI_COMMON               |
| FTP Internet/Anon | Critical    | -20 pts | DEV2_FTP_SERVER                    |
| Guest LAN Access  | High        | -15 pts | DEV2_ADT_WIFI_COMMON               |
| Risky UPnP Ports  | High        | -15 pts | DEV2_UPNP_PORTMAPPING              |
| Dangerous NAT     | High        | -15 pts | DEV2_PORTMAPPING                   |
| WPS Disabled      | Medium      | -10 pts | DEV2_ADT_WIFI_COMMON               |
| Anonymous SMB     | Medium      | -10 pts | DEV2_SMB_SERVICE                   |
| DoS Protection    | Medium      | -10 pts | DEV2_DDOS_CFG                      |
| Wi-Fi Passphrase  | Medium      | -10 pts | DEV2_ADT_WIFI_COMMON               |
+-------------------+-------------+---------+------------------------------------+
```

- Score is clamped to the range `[0, 100]`.
- Health status bands:
  - **90–100**: `Excellent` (Green / `primaryContainer`)
  - **70–89**: `Warning` (Amber / `tertiaryContainer`)
  - **< 70**: `Critical` (Red / `errorContainer`)

### 3. Intelligent UPnP & NAT Port Risk Filtering

```
                     Incoming UPnP Port Mapping
                                 |
                                 v
                     Is External Port in Blacklist?
                      (21, 22, 23, 25, 53, 80, 137-139,
                       445, 1433, 3306, 3389, 5432, 5900)
                               /            \
                             YES             NO
                             /                \
                 [Flag High-Risk Finding]   [Categorize as Safe /
                  Penalize -15 pts           Benign Gaming (0 pts)]
```

- **Rationale:** UPnP is required by gaming consoles (Xbox Live UDP 3074, PSN UDP 9308) and streaming apps. Blanket penalties create frustrating false alarms.
- **Alternatives Considered:** Penalizing any enabled UPnP state. Rejected due to negative user feedback on console networks.

### 4. 1-Tap Batch Remediation ("Resolver todo con 1 toque")

```
+----------------------------------------------------------------------+
|                     Batch Remediation Flow                           |
+----------------------------------------------------------------------+
|                                                                      |
|  1. User clicks "Resolver todo con 1 toque"                          |
|  2. SecurityAuditEngine filters remediable findings                  |
|  3. UI displays Confirmation BottomSheet listing safe actions:       |
|     - Disable DMZ (enable = "0")                                     |
|     - Disable WPS (WPSEnable = "0")                                  |
|     - Isolate Guest Wi-Fi (guestLANAccessEnable = "0")               |
|     - Enable DoS Defense (enable = "1", filters = "1")               |
|     - Disable WAN/Anonymous FTP (accessFromInternet = "0")          |
|  4. User confirms -> Sequential router RPC writes via RouterClient  |
|  5. Re-scan executes automatically -> Gauge animates to new score    |
+----------------------------------------------------------------------+
```

- Findings requiring creative user decisions (e.g. selecting a new Wi-Fi password) are omitted from the batch list and display a direct navigation button to the respective configuration screen.

### 5. UI Integration and Material 3 Expressive Components

- **Dashboard Alert Banner:** Displayed at the top of `DashboardScreen` when `criticalFindings.isNotEmpty()`. Rendered in `errorContainer` with a clear warning and a 1-tap resolution button.
- **Dashboard Bento Card:** Rendered in `DashboardScreen` alongside Internet and Device cards. Displays score, status badge, and navigation click handler.
- **Dedicated `SecurityHealthScreen`:**
  - Header: Animated circular progress indicator (`DrawScope.drawArc`) with smooth float interpolation.
  - Controls: Filter chips (`All`, `At Risk`, `Passed`).
  - Cards: Expandable cards with risk explanation and action buttons.
  - Floating/Bottom Action: "Resolver todo con 1 toque" button enabled when remediable issues exist.

## Risks / Trade-offs

- **[Risk] Router connection drops during multi-step batch remediation**  
  *Mitigation:* Execute updates sequentially with per-action error handling. If a write fails, abort gracefully, surface the error via Snackbar, and trigger a fresh scan to display the true partial state.
- **[Risk] Missing or empty OID instances (e.g. no USB drive plugged in)**  
  *Mitigation:* Engine treats missing or empty storage OIDs as "N/A / Safe" rather than throwing an exception.
- **[Risk] Dark Theme contrast on score gauge and severity badges**  
  *Mitigation:* Use standard semantic tokens (`colorScheme.errorContainer`, `onSurface`, `primary`) rather than hardcoded colors.

## Open Questions

None. All scope parameters, scoring weights, UPnP risk definitions, and remediation flows were finalized during exploration.
