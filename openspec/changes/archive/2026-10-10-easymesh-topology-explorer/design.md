# Design

## Context

See `proposal.md` for motivation.

The `:protocol` module already retrieves EasyMesh nodes (`MeshNode`) and connected clients (`ConnectedDevice`) via `ConnectedDevices.loadData(client)` (querying `ACT_UPDATE_MAPINFO`, `DEV2_WIFI_APDEV`, `DEV2_WIFI_APDEV_ASSOCDEV`, and `DEV2_WIFI_APDEV_ETHASSOCDEV`). Each client contains `nodeMac` and `nodeName`, and each node contains `backhaulType`, `backhaulSignal`, `linkRate`, `isController`, and `connectedClientsCount`. In `:app`, `DeviceMetadataStore` holds custom friendly aliases and category icons.

## Goals / Non-Goals

**Goals:**
- Provide a dedicated, full-screen interactive topology visualization (`TopologyScreen`) showing the network hierarchy from Gateway down to satellites.
- Distinguish backhaul connection mediums (Ethernet vs Wi-Fi) with signal strength, link speeds, and placement quality diagnostics.
- Provide per-node client inspection via `ModalBottomSheet` on node click, deep-linking into the existing `DeviceDetailSheet`.
- Gracefully handle single-node setups where no satellite repeaters are active.
- Ensure 100% JVM unit test coverage for protocol evaluation and maintain strict bilingual localization.

**Non-Goals:**
- Mesh node configuration management (e.g. changing satellite SSIDs or triggering mesh pairing/WPS from the topology map).
- Multi-hop tree resolution beyond what `DEV2_WIFI_APDEV` provides (current firmware reports star links to controller).

## Architecture & Data Flow

```
+-------------------------------------------------------------+
|                       ROUTER FIRMWARE                       |
|   DEV2_WIFI_APDEV        DEV2_WIFI_APDEV_ASSOCDEV / ETH     |
+------------------------------+------------------------------+
                               |
                               v
+-------------------------------------------------------------+
|                         :protocol                           |
|  ConnectedDevices.loadData() -> ConnectedDevicesData        |
|  BackhaulEvaluator.evaluate(MeshNode) -> BackhaulHealth     |
|  (Optimal, Good, Fair, Weak/Relocate, WiredGigabit)         |
+------------------------------+------------------------------+
                               |
                               v
+-------------------------------------------------------------+
|                           :app                              |
|                                                             |
|   DashboardScreen                      DevicesScreen        |
|   (Bento EasyMesh Card)                (TopAppBar Action)   |
|            \                                 /              |
|             \                               /               |
|              v                             v                |
|               TopologyScreen (pageId = "topology")          |
|                                                             |
|               +-----------------------------+               |
|               |  Internet / Gateway Header  |               |
|               +--------------+--------------+               |
|                              v                              |
|               +-----------------------------+               |
|               | Primary Router (Controller) |               |
|               +--------------+--------------+               |
|                              |                              |
|                    BackhaulLinkConnectors                   |
|                              |                              |
|                              v                              |
|               +-----------------------------+               |
|               |   Satellite Agent Cards     |               |
|               +--------------+--------------+               |
|                              | (Node Click)                 |
|                              v                              |
|               +-----------------------------+               |
|               |   NodeClientsBottomSheet    |               |
|               +--------------+--------------+               |
|                              | (Client Click)               |
|                              v                              |
|               +-----------------------------+               |
|               |      DeviceDetailSheet      |               |
|               +-----------------------------+               |
+-------------------------------------------------------------+
```

## Decisions

### 1. Dedicated Full-Screen (`TopologyScreen`) vs Tab in DevicesScreen
- **Decision**: Create an independent, full-screen composable `TopologyScreen` in `:app`.
- **Rationale**: A dedicated screen provides sufficient canvas area for vertical tree connectors, backhaul diagnostic badges, and breathing room for Bento-style cards without cluttering the existing device filter tabs.
- **Alternatives Considered**: Tab toggle in `DevicesScreen` was rejected during exploration because it cramped both views and hindered intuitive navigation from the Dashboard.

### 2. Inspection via ModalBottomSheet vs Inline Accordion
- **Decision**: Tapping a node in the topology opens a `ModalBottomSheet` displaying only the devices connected to that specific node.
- **Rationale**: Keeps the network tree clean and glanceable regardless of client count (10–30+ devices). Allows quick comparison between repeaters without layout jumps.
- **Alternatives Considered**: Expanding device items directly into the tree canvas caused severe vertical bloat and pushed downstream satellites off-screen.

### 3. Backhaul Health Diagnosis Model in `:protocol`
- **Decision**: Introduce a pure Kotlin enum/class `BackhaulHealth` and evaluator in `:protocol`:
  - `WIRED_OPTIMAL`: Ethernet backhaul (typically 1 Gbps / full link speed).
  - `WIFI_EXCELLENT`: Wi-Fi signal level >= 4 or rate >= 600 Mbps.
  - `WIFI_GOOD`: Wi-Fi signal level == 3 or rate in 200..599 Mbps.
  - `WIFI_WEAK`: Wi-Fi signal level < 3 or rate < 200 Mbps (triggers "Relocate closer" advisory).
- **Rationale**: Business logic remains unit-testable on JVM without Android UI dependencies, adhering to the project's architecture rules.
- **Alternatives Considered**: Calculating health directly in Compose composable was rejected to preserve separation of concerns and ensure unit test coverage.

### 4. Single-Node Gateway Fallback
- **Decision**: When `meshNodes` has only 1 node or all satellites are inactive, `TopologyScreen` displays the primary router in the center with an informative badge ("Standalone Router · All devices connect directly") and allows tapping it to see all connected clients.
- **Rationale**: Avoids showing a blank or disabled error screen for users who do not own mesh satellites, turning the feature into a useful gateway hub view.

## Edge Cases & Mitigations

- **[Dark Theme Contrast]**: Connectors and diagnostic chips use semantic Material 3 Expressive tokens (`surfaceContainerHigh`, `surfaceContainerHighest`, and `outlineVariant`) to maintain clear line contrast in both light and dark modes.
- **[Partial API Failure]**: If `DEV2_WIFI_APDEV` fails or returns partial data, `loadData` gracefully degrades and uses known clients, displaying available nodes without crashing the screen.
- **[Offline Satellites]**: Inactive satellites (`active == false`) are rendered with muted grey containers and a "Disconnected" badge rather than hidden completely.
- **[Long Hostnames]**: Node and device labels use `TextOverflow.Ellipsis` and `maxLines = 1` to prevent layout deformation.

## Risks & Trade-offs

- **[Risk: Stale Map Information]** → The router firmware updates client associations periodically upon `ACT_UPDATE_MAPINFO`.
  *Mitigation*: Include a refresh icon button in `TopologyScreen`'s TopAppBar triggering `loadData` on demand.
- **[Risk: Randomized / Missing Client Names]** → Devices without hostnames might display raw IPs or MACs.
  *Mitigation*: Resolve against `DeviceMetadataStore` (aliases) and OUI vendor heuristics already built into the app.
