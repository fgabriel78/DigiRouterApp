# Design: DIGI Assistant & Dual Diagnostic (Router vs Mobile)

## Context

See `proposal.md` for motivation. The application currently features a standalone optical fiber diagnostic screen (`FiberDiagnosticScreen`) directly accessed via `fiber-diag` and the Home screen hero banner. Users need an organized "Asistente DIGI" category in the primary catalog offering both optical fiber and dual diagnostic tools directly, avoiding intermediate hub screens while isolating in-home Wi-Fi latency from DIGI's fiber infrastructure.

The router firmware exposes hardware ping diagnostics on `diagnostic.htm` mapped via TR-181 object `DEV2_DIAG_IPPING` (callable by standard `user` role).

## Goals / Non-Goals

**Goals:**
- Provide a dedicated catalog group `Group.ASSISTANT` ("Asistente DIGI") hosting both Optical Fiber Diagnostics and Dual Diagnostics with direct 1-tap access.
- Wire `HeroStatusCard` to launch Optical Fiber Diagnostics (`fiber-diag`) directly.
- Implement `DualDiagnosticEngine` in `:protocol` that coordinates:
  1. Router hardware ICMP ping to Cloudflare (`1.1.1.1`) via `DEV2_DIAG_IPPING`.
  2. Mobile client TCP/socket ping to local router gateway (`192.168.1.1`).
  3. Mobile client TCP/socket ping to Cloudflare (`1.1.1.1`).
- Compute clear comparative verdicts (`WIFI_BOTTLENECK`, `FIBER_BOTTLENECK`, `ALL_OPTIMAL`, `BOTH_DEGRADED`) with actionable user advice.
- Maintain pure JVM unit testability in `:protocol` and ViewModel testability in `:app`.

**Non-Goals:**
- Intermediate hub screen (direct navigation from the catalog group is cleaner and less friction).
- Full traceroute hop-by-hop visualization (ping provides instant ms response and packet loss without 30-second traceroute delays).
- Automated router Wi-Fi channel switching or transmit power modifications (read-only advisory diagnostic).
- Continuous background polling when screens are inactive.

## Decisions

### 1. Dedicated "Asistente DIGI" Catalog Group Navigation

```
                       +----------------------------------+
                       |           Home Screen            |
                       |  - Hero Status Card              |
                       |  - Bento Grid (Resumen, etc.)    |
                       |  - Grupo: "Asistente DIGI"       |
                       +-------+--------------------+-----+
                               |                    |
        Hero Card Tap /        |                    |  Catalog Tap
        Catalog Tap (Fibra)    |                    |  (Dual Diag)
                               v                    v
                  +-----------------------+     +-----------------------+
                  | FiberDiagnosticScreen |     | DualDiagnosticScreen  |
                  | (Potencia optica GPON)|     | (Router vs Movil ping)|
                  +-----------+-----------+     +-----------+-----------+
                              |                             |
                              +-------- Back Navigation ----+
                                             |
                                             v
                                  [ Home Screen ]
```

- **Rationale**: Organizing diagnostics under `Group.ASSISTANT` provides clear visual grouping on the main screen without adding intermediate clicks or separate hub pages.
- **Alternatives Considered**: An intermediate hub screen (`DigiAssistantHubScreen`). Rejected due to redundant click-through friction; users prefer direct navigation to the exact tool from the catalog.

### 2. Router Hardware Ping Execution & Polling

```
  App Client                        Router Client                    TR-181 Router
      |                                   |                                 |
      |--- runDualDiagnostics() --------->|                                 |
      |                                   |--- set("DEV2_DIAG_IPPING", ---->|
      |                                   |        diagnosticsState="Req")  |
      |                                   |                                 | (Router executes
      |                                   |                                 |  ping -c 4 1.1.1.1)
      |                                   |--- get("DEV2_DIAG_IPPING") ---->|
      |                                   |<-- diagnosticsState="Complete" -|
      |                                   |    avg=7, min=6, max=9, loss=0  |
      |<-- emit(RouterPingResult) --------|                                 |
```

- The engine sets `diagnosticsState = "Requested"`, `host = "1.1.1.1"`, `numberOfRepetitions = "4"` on `DEV2_DIAG_IPPING`.
- Polls every 800ms (up to 6 attempts). Once `diagnosticsState == "Complete"`, extracts average response time and failure counts.
- **Graceful Fallback**: If `DEV2_DIAG_IPPING` fails or is blocked on specific firmware, calculates approximate router WAN latency by subtracting local LAN ping from mobile Internet ping (`R_wan ≈ M_net - M_lan`).

### 3. Verdict Decision Engine & Thresholds

```
                      +---------------------------------+
                      |     Dual Diagnostic Engine      |
                      +----------------+----------------+
                                       |
                    +------------------+------------------+
                    |                                     |
           WiFi RTT > 25 ms                      WiFi RTT <= 10 ms
          or Jitter > 15 ms                     Router WAN RTT > 45 ms
          Router WAN <= 20 ms                   or Packet Loss > 0%
                    |                                     |
                    v                                     v
          [ WIFI_BOTTLENECK ]                   [ FIBER_BOTTLENECK ]
         "Problema en tu Wi-Fi,                 "Problema en la fibra,
          no en la fibra DIGI"                   tu Wi-Fi es optimo"
```

- **`WIFI_BOTTLENECK`**: Router WAN is fast (`<= 20 ms`) with 0% loss, but Mobile Wi-Fi is slow (`> 25 ms` or high jitter). Action: Advise 5 GHz band, closer proximity.
- **`FIBER_BOTTLENECK`**: Mobile Wi-Fi is fast (`<= 10 ms`), but Router WAN is slow (`> 45 ms`) or dropping packets. Action: Advise external carrier congestion.
- **`ALL_OPTIMAL`**: Both hops under nominal thresholds (`<= 12 ms` Wi-Fi, `<= 25 ms` WAN, 0% loss).
- **`BOTH_DEGRADED`**: Both wireless and fiber links elevated.

## Risks / Trade-offs

- **[Risk] Router firmware takes > 5 seconds to complete ping** -> *Mitigation*: Run mobile local probe and mobile Internet probe concurrently with router ping to minimize overall user wait time to under 4 seconds total.
- **[Risk] Firewall on local router IP restricts socket probes** -> *Mitigation*: Probe port 80 (router web admin) which is guaranteed to be open and listening for the active app session.
- **[Risk] High-contrast accessibility on comparison cards** -> *Mitigation*: Material 3 Expressive semantic color pairs (`surfaceContainerHigh`, `primaryContainer`, `errorContainer`, `tertiaryContainer`).
