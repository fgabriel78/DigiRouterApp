# Proposal

## Why

In networks using EasyMesh (such as TP-Link / Digi mesh setups), satellite nodes (agents) extend coverage throughout the home or office, and client devices connect dynamically across these different access points. Currently, the Summary ("Resumen") screen only displays hardware and connection information for the main router (controller), giving no visibility into mesh satellite nodes, their backhaul link health, or their individual load. Furthermore, the Devices ("Dispositivos") screen only allows filtering by connection medium (Wi-Fi vs Ethernet) without showing which mesh node each device is connected to, making it difficult for users to identify network distribution and diagnose coverage or roaming issues.

## What Changes

- **EasyMesh Multi-Node Summary**:
  - Detect when an EasyMesh mesh network is active (querying `DEV2_WIFI_APDEV` and/or `DEV2_X_TP_EASYMESH`).
  - When mesh nodes are present, display an EasyMesh nodes overview section in the Summary screen showing all active nodes (Controller / main router and Agent satellites).
  - For each node, display its hostname/location, model name, role (Principal vs Agente / Satélite), IP/MAC address, backhaul connection link (Ethernet or Wi-Fi with signal strength / link rate for agents), online status, and number of connected client devices.
  - Retain the existing single-router presentation when EasyMesh is not configured or no agent nodes are active.

- **Mesh Node Association in Connected Devices**:
  - Enhance device discovery (`ConnectedDevices.load` / `parse`) to map client association fields (`X_TP_ApDeviceMac` for wireless clients and `APDeviceMACAddress` for wired clients) to the respective node in `DEV2_WIFI_APDEV`.
  - Record the connected node's MAC and display name on each `ConnectedDevice`.
  - In the Devices list item, show the name of the node to which the device is currently attached.

- **Node Filtering in Devices Screen**:
  - Add node-based filter options in the Devices screen (e.g. filter chips / selector for "Todos los nodos" or individual node names), alongside the existing connection type filters (All Active, Wi-Fi, Ethernet Cable, Disconnected).
  - Enable users to filter devices by specific node or combine with connection type filtering.

## Capabilities

### New Capabilities
*(None)*

### Modified Capabilities
- `dashboard-status`: Update the capability to aggregate and display EasyMesh node metrics across all mesh access points in the summary dashboard, associate connected devices with their parent node, and provide node-level filtering in the connected devices screen.

## Impact

- **Protocol Layer**:
  - Update `ConnectedDevices.kt` and `ConnectedDevice` data class to include node association details (`nodeMac`, `nodeName`).
  - Query `DEV2_WIFI_APDEV` to resolve node MACs to human-readable names and controller/agent roles.
  - Provide a mesh node data model (`MeshNode` or similar) to expose node health and client distribution.
- **UI / App Layer (`StatusScreens.kt`)**:
  - Summary screen (`SummaryScreen`): Add mesh nodes card/section when multiple nodes exist.
  - Devices screen (`DevicesScreen`): Add node filter selector/chips and show connected node badge/label in device cards.
- **Resource / Localization (`strings.xml`)**:
  - Add localized strings (English and Spanish) for node roles, backhaul metrics, node filters, and mesh status.
- **Existing Tests**:
  - Update unit tests in `ConnectedDevicesTest.kt` to cover node mapping and backward compatibility when node data is absent.
