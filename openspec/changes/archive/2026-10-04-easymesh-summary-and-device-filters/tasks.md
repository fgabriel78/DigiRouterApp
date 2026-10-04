# Tasks

## 1. Protocol Models and Node Association

- [x] 1.1 Define `MeshNode` data class and extend `ConnectedDevice` with optional `nodeMac: String? = null` and `nodeName: String? = null` in `protocol/src/main/kotlin/es/routerapp/protocol/pages/ConnectedDevices.kt`, verifying existing test suite compiles.
- [x] 1.2 Implement `MeshNodes` parser and update `ConnectedDevices.load` to query `DEV2_WIFI_APDEV` and map wireless (`X_TP_ApDeviceMac`) and wired (`APDeviceMACAddress`) client associations to their parent node MAC and hostname.
- [x] 1.3 Add unit tests in `protocol/src/test/kotlin/es/routerapp/protocol/ConnectedDevicesTest.kt` verifying node extraction, backhaul detection, and client-to-node mapping, verifying tests pass with `./gradlew :protocol:test`.

## 2. Localization Resources

- [x] 2.1 Add string resources for EasyMesh multi-node summary and node filtering in `app/src/main/res/values/strings.xml` and `app/src/main/res/values-es/strings.xml` (e.g., node roles, backhaul indicators, all nodes filter label, connected node badge), verifying XML validity.

## 3. EasyMesh Nodes Overview in Summary Screen

- [x] 3.1 Update `DashboardData` in `app/src/main/kotlin/es/routerapp/app/StatusScreens.kt` to include `meshNodes: List<MeshNode>` loaded from the router client.
- [x] 3.2 Implement the multi-node UI section in `SummaryScreen` (`StatusScreens.kt`) displaying each active node's role (Controller vs Satellite), model, IP, backhaul link details (Ethernet vs Wi-Fi with rate/signal), and connected client counts when mesh nodes are detected.

## 4. Node-Based Filtering in Devices Screen

- [x] 4.1 Update `DevicesScreen` in `app/src/main/kotlin/es/routerapp/app/StatusScreens.kt` to load nodes alongside devices and maintain selected node filter state.
- [x] 4.2 Add the horizontal node filter selector in `DevicesScreen` allowing users to filter by "Todos los nodos" or an individual mesh node, combined with existing connection medium filters (All, Wi-Fi, Cable, Offline).
- [x] 4.3 Add a connected node badge to each device item card in `DevicesScreen` showing which AP node the device is connected to.
- [x] 4.4 Run full verification with `./gradlew test` and `./gradlew assembleDebug` to confirm all unit tests and builds succeed without warnings or errors.
