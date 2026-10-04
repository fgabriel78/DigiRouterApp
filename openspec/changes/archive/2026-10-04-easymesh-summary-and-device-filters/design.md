# Design

## Context

The application interacts with the router firmware via TR-181/DEV2 data models over HTTP. For topology and host discovery, the router firmware exposes:
- `ACT_UPDATE_MAPINFO`: Triggers a topology refresh in the firmware.
- `DEV2_WIFI_APDEV`: Enumerates all mesh nodes (the controller and agent satellites) with attributes such as role (`X_TP_IsController`), activity (`X_TP_Active`), MAC address (`MACAddress`), user-assigned hostname (`X_TP_HostName`), model name (`X_TP_ModelName`), IP address (`X_TP_IPAddress`), backhaul medium (`backhaulLinkType`), signal strength (`backhaulSignalStrength`), and link speed (`X_TP_LinkRate`).
- `DEV2_WIFI_APDEV_ASSOCDEV`: Wi-Fi clients, where `X_TP_ApDeviceMac` corresponds to the AP node MAC address.
- `DEV2_WIFI_APDEV_ETHASSOCDEV`: Wired clients, where `APDeviceMACAddress` corresponds to the AP node MAC address.

Currently:
- `StatusScreens.kt` (`SummaryScreen`) queries only the controller's `DEV2_DEV_INFO` and total client count.
- `ConnectedDevices.kt` queries `DEV2_WIFI_APDEV_ASSOCDEV` and `DEV2_WIFI_APDEV_ETHASSOCDEV` without reading `DEV2_WIFI_APDEV` or propagating the node MAC address.
- `DevicesScreen` only filters by connection type (`ALL`, `WIFI`, `CABLE`, `OFFLINE`).

## Goals / Non-Goals

**Goals:**
- Provide a structured model `MeshNode` representing EasyMesh nodes with role, hardware info, backhaul health, and connected device counts.
- Enhance `ConnectedDevices` to associate each client with its parent node's MAC address and resolved name.
- Enhance `SummaryScreen` to render an EasyMesh nodes overview when an EasyMesh mesh is configured with active agent nodes.
- Enhance `DevicesScreen` to provide filtering by connected node in addition to connection medium, and display the node name on each device card.
- Maintain full backward compatibility for setups without EasyMesh or where EasyMesh is unconfigured.

**Non-Goals:**
- Allowing remote reboot, firmware update, or factory reset of individual agent nodes from the summary screen (managed under system/advanced settings).
- Supporting manual steering or forced reassociation of clients between nodes.

## Decisions

### Decision 1: Node Representation Data Model & Protocol Layer
- Introduce `MeshNode` in `es.routerapp.protocol.pages`:
  ```kotlin
  data class MeshNode(
      val mac: String,
      val name: String,
      val model: String,
      val ip: String,
      val isController: Boolean,
      val active: Boolean,
      val backhaulType: String?, // "Wi-Fi", "Ethernet", null for controller
      val backhaulSignal: Int?,
      val linkRate: Long?,
      val uptime: Long?,
      val connectedClientsCount: Int = 0,
  )
  ```
- Enrich `ConnectedDevice`:
  ```kotlin
  data class ConnectedDevice(
      val name: String,
      val ip: String,
      val mac: String,
      val wifi: Boolean,
      val level: Int?,
      val rate: Long?,
      val active: Boolean,
      val nodeMac: String? = null,
      val nodeName: String? = null,
  )
  ```
  Default `null` parameters ensure existing instantiations and unit test signatures continue to compile and work seamlessly.
- Update `ConnectedDevices.load` to also query `DEV2_WIFI_APDEV` (tolerating failure) and pass the node list into `parse(...)`. Provide a helper `loadNodes(client)` or return a composite `ConnectedDevicesData(devices, nodes)` to supply both screens efficiently.

*Alternatives considered:*
- Querying `DEV2_WIFI_APDEV` separately in each UI screen: rejected because resolving `nodeName` on devices requires joining client association MACs with node instances, which belongs in the protocol/data mapping layer.

### Decision 2: Multi-Node Display in Summary Screen (`SummaryScreen`)
- In `DashboardData`, add `nodes: List<MeshNode> = emptyList()`.
- Detection of EasyMesh network: `val hasMesh = nodes.count { it.active } > 1 || nodes.any { !it.isController && it.active }`.
- When `hasMesh` is true:
  - Add an EasyMesh section header and a responsive grid/column of node cards.
  - Controller card: Model, hostname, role badge ("Router Principal"), IP address, uptime, and connected client count badge.
  - Agent cards: Model, hostname, role badge ("Satélite Mesh"), IP address, backhaul badge (Cable vs Wi-Fi with signal/rate), and connected client count badge.
- When `hasMesh` is false:
  - Preserve the existing single-router top card and stats without empty or redundant mesh sections.

*Alternatives considered:*
- Replacing the top router card entirely: rejected because GPON/WAN connectivity applies to the whole network and is managed at the primary router; the multi-node section complements the WAN overview cleanly.

### Decision 3: Node Filtering in Devices Screen (`DevicesScreen`)
- Retain the top medium filter chips (`ALL`, `WIFI`, `CABLE`, `OFFLINE`).
- Below the medium filter chips, when multiple nodes exist (`nodes.size > 1`), display a horizontally scrollable chip group:
  - "Todos los nodos" (`selectedNodeMac == null`)
  - Node chips labeled with `node.name` (e.g., "Router Principal", "Salón", "Dormitorio") showing the count of active clients connected to that node.
- Filtering logic combines both criteria:
  `list = filteredByMedium.filter { selectedNodeMac == null || it.nodeMac.equals(selectedNodeMac, ignoreCase = true) }`
- In each device card:
  - Render an `AssistChip` or subtitle badge showing the node name (e.g. `d.nodeName`) so the user immediately knows which AP serves that device.

*Alternatives considered:*
- Replacing medium filters with node filters: rejected because users frequently want to filter "Wired devices on Satellite 1" or "Wi-Fi devices on Main Router". A two-level or combined filter allows both dimensions.

## Risks / Trade-offs

- **[Risk]** Satellite nodes query might fail or time out on older firmware versions without EasyMesh support.
  - **Mitigation**: Wrap the `DEV2_WIFI_APDEV` query in `runCatching` with fallback to empty list. `ConnectedDevices` continues to work normally with `nodeMac = null`.
- **[Risk]** Client instances might report a node MAC with different letter casing or formatting than `DEV2_WIFI_APDEV`.
  - **Mitigation**: Normalize all MAC comparisons using `uppercase()` and trimmed strings.
- **[Risk]** Disconnected/offline devices do not have an active AP association MAC in the firmware.
  - **Mitigation**: When `nodeMac` is null or empty on an offline device, display no node badge or display "—", and include offline devices when "Todos los nodos" is selected.
