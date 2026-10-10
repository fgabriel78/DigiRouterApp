# Tasks

## 1. Protocol: Backhaul Health Evaluation & Diagnostics

- [x] 1.1 Implement `BackhaulHealth` enum and `evaluateBackhaulHealth(node: MeshNode)` helper function in `:protocol` (`protocol/src/main/kotlin/es/routerapp/protocol/pages/ConnectedDevices.kt`), categorizing links into `WIRED_OPTIMAL`, `WIFI_EXCELLENT`, `WIFI_GOOD`, `WIFI_WEAK`, and `NONE`.
- [x] 1.2 Add unit tests in `protocol/src/test/kotlin/es/routerapp/protocol/BackhaulHealthTest.kt` verifying Ethernet backhaul, high-speed Wi-Fi, low-signal/low-rate Wi-Fi, and controller/unconnected nodes via `./gradlew :protocol:test`.

## 2. Strings & Localization

- [x] 2.1 Add bilingual string resources for the EasyMesh Topology Explorer in `app/src/main/res/values/strings.xml` and `app/src/main/res/values-es/strings.xml` (screen title, gateway label, backhaul status labels, relocation advisory, standalone gateway notice, client counts, and sheet headers).

## 3. UI: TopologyScreen & Node Client BottomSheet

- [x] 3.1 Implement `TopologyScreen.kt` in `:app` with Material 3 Expressive tokens, rendering the vertical hierarchy tree: Internet Gateway header, Primary Router controller card, visual backhaul connectors with health badges, and satellite agent cards.
- [x] 3.2 Implement `NodeClientsBottomSheet` composable in `TopologyScreen.kt` using `ModalBottomSheet` with `rememberBottomSheetState(initialValue = SheetValue.Hidden)`, displaying client devices associated with the tapped node with category icons, friendly aliases, and triggering `DeviceDetailSheet` on client click.
- [x] 3.3 Implement single-node fallback view in `TopologyScreen.kt` displaying the primary router as the central hub when no satellite repeaters are active.

## 4. Navigation & Screen Integration

- [x] 4.1 Add navigation route for `pageId == "topology"` in `MainActivity.kt`, rendering `TopologyScreen(engine, onBack = { pageId = null }, recovery = vm)`.
- [x] 4.2 Add entry points to `TopologyScreen` in `DashboardScreen` (interactive action card in the EasyMesh section) and `DevicesScreen` (action icon in TopAppBar) in `app/src/main/kotlin/es/routerapp/app/StatusScreens.kt`.
- [x] 4.3 Verify full compilation and execution of `./gradlew testDebugUnitTest` ensuring all tests pass with zero warnings.
