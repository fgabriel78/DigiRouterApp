# Design

## Context

The TP-Link XGB430v Pro (DIGI) router implements Intelligent QoS (`main_iqosSettings.htm`) across three TR-181 data model objects:
1. `DEV2_IQOS` (Singleton): Global QoS engine toggle (`enable`), upstream/downstream bandwidth caps (`upTotalBW`, `upUnit`, `downTotalBW`, `downUnit`), priority queue percentages (`high`, `middle`, `low`), and companion service fields (`linkType`, `iptv*`, `voIP*`, `guest*`).
2. `DEV2_IQOS_RULE` (Multi-instance list): Priority queue rules where `class_` is `4` (High), `3` (Middle), or `2` (Low), and `type` is `11` (Device by `macAddress` and `deviceName`), `21`/`22`/`23` (Custom TCP/UDP/All port rules), or `30` (System app).
3. `DEV2_GAME_QOS_CLIENT` (Multi-instance list): Hardware Game Port client status (`hostName`, `physAddress`, `IPAddress`, `active`, `IsAgentDevice`, `gamePortClient`).

See `proposal.md` for user motivation and `specs/qos-management/spec.md` for behavioral requirements.

## Goals / Non-Goals

**Goals:**
- Provide a pure-Kotlin, framework-independent `QosEngine` in `:protocol` with a testable `QosRouterRpc` interface.
- Preserve all companion attributes on `DEV2_IQOS` (`iptvEnable`, `iptvClass`, `iptvBW`, `iptvUnit`, `voIPEnable`, `voIPClass`, `guestEnable`, `guestClass`, `linkType`) when updating bandwidth caps and queue percentages.
- Enforce exact firmware slider coupling rules (`5%` step, `High >= 15%`, `Middle >= 10%`, `Low >= 5%`, `High > Middle > Low`, `High + Middle + Low <= 100%`) in both domain validation and UI slider state.
- Join `DEV2_IQOS_RULE` and `DEV2_GAME_QOS_CLIENT` with `ConnectedDevices.load(client)` and `DeviceMetadataStore` so mobile users can assign devices to `High`, `Middle`, or `Low` priority with 1-tap controls instead of desktop drag-and-drop.

**Non-Goals:**
- Creating or editing custom TCP/UDP port rules (`type = 21/22/23`) or system app rules (`type = 30`). Existing non-device rules are displayed read-only with an option to delete.
- Editing IPTV or VoIP QoS sub-settings from the QoS screen (their existing values on `DEV2_IQOS` are preserved).
- Upgrading the router's QoS signature database (`main_iqosDatabase.htm` / `/cgi/dbup`).

## Decisions

### 1. Dedicated Custom Page & Engine vs. Declarative `Catalog` Form
- **Decision**: Implement a dedicated `QosEngine` in `:protocol` (`es.routerapp.protocol.qos`) and a custom Compose screen `QosScreen` + `QosViewModel` in `:app`, registered in `Catalog.kt` as `Page(id = "qos", title = "Intelligent QoS", group = Group.NETWORK, custom = true, adminOnly = true)`.
- **Rationale**:
  - `DEV2_IQOS` requires coupled 3-slider constraints (`high`, `middle`, `low`) and companion-attribute preservation that exceed static `FieldType` declarations.
  - `DEV2_IQOS_RULE` device assignment requires joining `ConnectedDevices` (`DEV2_WIFI_APDEV_ASSOCDEV` + `DEV2_WIFI_APDEV_ETHASSOCDEV`), `DEV2_GAME_QOS_CLIENT`, and `DeviceMetadataStore`, plus executing `delete` + `add` sequences when changing an existing rule's priority tier.
- **Alternatives Considered**:
  - *Generic `Catalog` sections*: Cannot express coupled 3-slider limits, cross-object union with `ConnectedDevices`, or the firmware's `delete` + `add` rule mutation pattern.

### 2. Resolving Devices via `ConnectedDevices` Instead of `DEV2_HOST_ENTRY`
- **Decision**: Use `ConnectedDevices.load(client)` merged with `DEV2_IQOS_RULE` and `DEV2_GAME_QOS_CLIENT` (matched by normalized uppercase colon-separated MAC address), enriched with `DeviceMetadataStore` aliases and categories.
- **Rationale**: As documented in `ConnectedDevices.kt`, `gl DEV2_HOST_ENTRY` is rejected by this firmware with error `71011`. `ConnectedDevices.load(client)` reliably retrieves both Wi-Fi and Ethernet clients via `ACT_UPDATE_MAPINFO`, `DEV2_WIFI_APDEV_ASSOCDEV`, and `DEV2_WIFI_APDEV_ETHASSOCDEV`.
- **Alternatives Considered**:
  - *Querying `DEV2_HOST_ENTRY` like `main_iqosSettings.htm`*: Fails on the target router firmware and lacks user-defined aliases/categories from `DeviceMetadataStore`.

### 3. Priority Tier State Transitions (`DEV2_IQOS_RULE`)
- **Decision**: Model device priority transitions around the router's native rule semantics:
  - Unassigned devices default to `QosPriority.MIDDLE` (`class_ = 3`) with no `DEV2_IQOS_RULE` instance.
  - Assigning an unassigned device to `HIGH` (`4`) or `LOW` (`2`) invokes `rpc.add("DEV2_IQOS_RULE", mapOf("deviceName" to name, "macAddress" to mac, "class_" to priority.code.toString(), "type" to "11"))`.
  - Moving a device that already has a `DEV2_IQOS_RULE` (with `stack`) to a different priority either:
    - Deletes `stack` if the target is `MIDDLE` (returning it to default unassigned Middle priority), or
    - Deletes `stack` and adds a new `DEV2_IQOS_RULE` with the new `class_` if switching between `HIGH` and `LOW` (or if an explicit `MIDDLE` rule existed and is being moved to `HIGH`/`LOW`).
  - Every mutation is followed by `rpc.operate("ACT_SAVE_CFG")`.
- **Rationale**: Mirrors `main_iqosSettings.htm` lines 862–915 directly, avoiding unsupported in-place `so` (`set`) mutations on `DEV2_IQOS_RULE`.

### 4. Preserving `DEV2_IQOS` Companion Attributes and Handling `downTotalBW`
- **Decision**: Before writing `DEV2_IQOS`, `QosEngine.saveConfig` fetches the current `DEV2_IQOS` object (or uses the raw attributes snapshot), overlays the updated `enable`, `upTotalBW`, `upUnit`, `downTotalBW`, `downUnit`, `high`, `middle`, and `low` fields, ensures `linkType` defaults to `"0"` if absent, and preserves `iptv*`, `voIP*`, and `guest*` attributes. If the router returns `downTotalBW = 0` or `upTotalBW = 0` initially, the UI presents an editable field initialized from the stored value (or sensible default such as `1000` Mbps when first enabling QoS) and validates `1..10000` before saving.
- **Rationale**: Prevents clobbering IPTV, VoIP, or Guest Wi-Fi QoS settings configured elsewhere on the router.

---

## Architecture & Diagrams

### User Flow & Component Architecture

```text
+-------------------------------------------------------------------------+
|                              USER FLOW                                  |
+-------------------------------------------------------------------------+
|                                                                         |
|  [HomeScreen (Admin)]                                                   |
|         |                                                               |
|         v  (Tap "Intelligent QoS" in Local network group)               |
|  [QosScreen] <---> [QosViewModel] <---> [DeviceMetadataStore]           |
|                          |                                              |
|                          v  (Dispatchers.IO + RecoveryRunner)           |
|         +----------------+----------------+                             |
|         |                                 |                             |
|         v                                 v                             |
|  [QosEngine (:protocol)]         [ConnectedDevices (:protocol)]         |
|   * get/set DEV2_IQOS             * op ACT_UPDATE_MAPINFO               |
|   * getList/add/del               * getList DEV2_WIFI_APDEV_ASSOCDEV    |
|     DEV2_IQOS_RULE                * getList DEV2_WIFI_APDEV_ETHASSOCDEV |
|   * getList DEV2_GAME_QOS_CLIENT                                        |
|   * op ACT_SAVE_CFG                                                     |
+-------------------------------------------------------------------------+
```

### Data Transformation & Device Merge Pipeline

```text
+-----------------------+   +-----------------------+   +-----------------------+
|   ConnectedDevices    |   |    DEV2_IQOS_RULE     |   | DEV2_GAME_QOS_CLIENT  |
|  (Active LAN / Wi-Fi) |   | (type=11 Device Rules |   | (gamePortClient=="1"  |
|                       |   |  & type=21..30 Rules) |   |  & active=="1")       |
+-----------+-----------+   +-----------+-----------+   +-----------+-----------+
            |                           |                           |
            +-------------+-------------+-------------+-------------+
                          |                           |
                          v                           v
            +---------------------------------------------------+
            |        MAC Normalization (AA:BB:CC:DD:EE:FF)      |
            |        + DeviceMetadataStore Alias/Category       |
            +-------------------------+-------------------------+
                                      |
                                      v
            +---------------------------------------------------+
            |              Merged QosDeviceItem List            |
            |  1. Game Port Clients -> HIGH (locked/pinned)     |
            |  2. Rule-bound MACs   -> HIGH / MIDDLE / LOW      |
            |     (includes offline saved rule devices)         |
            |  3. Unassigned Online -> MIDDLE (default, no rule)|
            +---------------------------------------------------+
```

### Device Priority State Transitions

```text
                  +-----------------------------------+
                  |     UNASSIGNED (Default Middle)   |
                  |       (No DEV2_IQOS_RULE)         |
                  +-----------------+-----------------+
                                    |
                   Tap "High" (4)   |   Tap "Low" (2)
                   add(DEV2_IQOS_RULE, type=11, class_=4|2)
                                    |
            +-----------------------+-----------------------+
            v                                               v
+-----------------------+  Tap "Low" (del + add)  +-----------------------+
|     HIGH PRIORITY     | ----------------------> |     LOW PRIORITY      |
|  (DEV2_IQOS_RULE c=4) | <---------------------- |  (DEV2_IQOS_RULE c=2) |
+-----------+-----------+  Tap "High" (del + add) +-----------+-----------+
            |                                               |
            | Tap "Middle" / Delete                         | Tap "Middle" / Delete
            | del(DEV2_IQOS_RULE, stack)                    | del(DEV2_IQOS_RULE, stack)
            +-----------------------+-----------------------+
                                    |
                                    v
                  +-----------------------------------+
                  |     UNASSIGNED (Default Middle)   |
                  +-----------------------------------+
```

---

## Edge Cases & Error Handling

1. **Unsupported or Empty `DEV2_GAME_QOS_CLIENT`**:
   - On firmware builds where `INCLUDE_GAMING_CONSOLE = 0`, querying `DEV2_GAME_QOS_CLIENT` may return an empty list or a router error code. `QosEngine` wraps `getList("DEV2_GAME_QOS_CLIENT")` in `runCatching { ... }.getOrDefault(emptyList())` so the screen loads normally.
2. **Offline Devices with Existing QoS Rules**:
   - A device previously assigned to High or Low priority in `DEV2_IQOS_RULE` may currently be offline (absent from `ConnectedDevices`). The merge logic retains all `DEV2_IQOS_RULE` (`type = 11`) entries so offline prioritized devices still appear with an "Offline" indicator and can be demoted/removed.
3. **MAC Address Format Differences**:
   - The router occasionally formats MACs with hyphens (`AA-BB-CC-DD-EE-FF`) or lowercase hex. All MAC comparisons normalize to uppercase colon-separated format (`AA:BB:CC:DD:EE:FF`), while preserving the router's expected colon format when writing `DEV2_IQOS_RULE.macAddress`.
4. **Coupled Slider Clamping**:
   - When the user drags one slider (e.g. `High`), `Middle` and `Low` are clamped in 5% steps so `High >= 15`, `Middle >= 10`, `Low >= 5`, `Middle <= min(High - 5, 100 - High - Low)`, and `Low <= min(Middle - 5, 100 - High - Middle)`. A helper function `QosConfig.adjustQueuePercentages(...)` in `:protocol` guarantees deterministic clamping and is covered by unit tests.
5. **Dark Theme & Expressive Contrast**:
   - Priority badges (`High`, `Middle`, `Low`, and `Game Port`) use semantic Material 3 Expressive container tokens (`primaryContainer`, `secondaryContainer`, `tertiaryContainer`, `surfaceContainerHigh`) to maintain legibility in both Light and Dark themes.
6. **Session Recovery & Partial Failures**:
   - All network calls in `QosViewModel` run inside `withContext(ioDispatcher) { recoveryRunner.run { ... } }` so expired sessions automatically re-authenticate, and `ConnectedDevices` failure does not block loading `DEV2_IQOS` and `DEV2_IQOS_RULE`.

## Risks / Trade-offs

- **[Risk] Firmware rejects `downTotalBW = 0` when saving `DEV2_IQOS` for the first time** → **Mitigation**: When `upTotalBW` or `downTotalBW` is `0` on the router (unconfigured state), pre-fill the UI editor with a valid default (`1000` Mbps) and require a positive integer (`1..10000`) before calling `saveConfig`.
- **[Risk] Delete + Add sequence for changing priority between High and Low could partially fail if connection drops mid-flight** → **Mitigation**: Execute inside `recoveryRunner.run` and reload the full rule list after mutation so UI state always reflects actual router state.
