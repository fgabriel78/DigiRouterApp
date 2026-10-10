# Proposal

## Why

In mesh networks, users frequently experience connection drops or speed degradation without knowing if a satellite repeater is connected via Wi-Fi or Ethernet, whether the backhaul signal is too weak due to poor placement, or which specific repeater their phones, laptops, and smart TVs are actually associated with. Today, the app provides a basic static summary card list and horizontal device filter chips, but lacks an interactive visual topology map to understand, diagnose, and inspect the mesh hierarchy and client distribution.

Providing a dedicated **EasyMesh Topology Explorer** screen bridges this gap by presenting a clear graphical tree from the Internet gateway and primary router down to satellite repeaters, showcasing backhaul link health with actionable recommendations, and allowing users to inspect devices connected to each node in a modal bottom sheet.

## What Changes

- **Dedicated Topology Explorer Screen (`TopologyScreen`)**: Full-screen interactive hierarchy showing the internet gateway, primary router (controller), backhaul connections, and satellite repeaters.
- **Backhaul Medium & Signal Diagnostics**: Clear visual distinction between wired Ethernet (speed/stability) and Wi-Fi backhaul (signal strength in dBm/level, negotiated link speed in Mbps, and placement quality assessment indicating whether a satellite should be moved closer to the router).
- **Per-Node Client Inspection via Modal Bottom Sheet**: Tapping any node (repeater or main router) opens a `ModalBottomSheet` listing all devices associated with that node (displaying category icon, friendly alias, IP address, and connection link rate/signal). Tapping a device opens the existing `DeviceDetailSheet`.
- **Single-Node Network Fallback**: When no mesh satellites are detected (standard single-router setup), the topology gracefully presents the central router node with an informational status explaining that all devices connect directly to the primary router, preserving access to the client inspection sheet.
- **Multiple Entry Points**: Accessible from an action card/button in the Dashboard's EasyMesh section and from an action button in the top app bar of `DevicesScreen`.
- **Bilingual Localization**: All user-visible strings localized in English (`values/strings.xml`) and Spanish (`values-es/strings.xml`).

## Capabilities

### Modified Capabilities
- `dashboard-status`: Extends the EasyMesh topology and metrics requirements to specify the standalone interactive topology explorer screen, backhaul health diagnostics (Ethernet vs Wi-Fi), and modal client inspection per node.

## Impact

- **`:protocol`**: Add backhaul health assessment helper utilities (evaluating signal level and link rate into structured health states: Optimal, Good, Weak/Relocate) and retain clean separation without Android dependencies.
- **`:app`**:
  - Add `TopologyScreen.kt` featuring Material 3 Expressive tokens, tree connectors, node cards, and `ModalBottomSheet` client list.
  - Wire navigation in `MainActivity.kt` (`pageId == "topology"`).
  - Add entry points in `DashboardScreen` and `DevicesScreen` in `StatusScreens.kt`.
  - Add bilingual string resources in `strings.xml` and `values-es/strings.xml`.
