package es.routerapp.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.NetworkWifi1Bar
import androidx.compose.material.icons.filled.NetworkWifi2Bar
import androidx.compose.material.icons.filled.NetworkWifi3Bar
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.SignalWifi4Bar
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import es.routerapp.protocol.pages.PageEngine
import es.routerapp.protocol.pages.BackhaulHealth
import es.routerapp.protocol.pages.ConnectedDevice
import es.routerapp.protocol.pages.ConnectedDevices
import es.routerapp.protocol.pages.ConnectedDevicesData
import es.routerapp.protocol.pages.Group
import es.routerapp.protocol.pages.MeshNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private fun signalIconForLevel(level: Int?): ImageVector = when {
    level == null -> Icons.Filled.Wifi
    level >= 4 -> Icons.Filled.SignalWifi4Bar
    level == 3 -> Icons.Filled.NetworkWifi3Bar
    level == 2 -> Icons.Filled.NetworkWifi2Bar
    else -> Icons.Filled.NetworkWifi1Bar
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopologyScreen(
    engine: PageEngine,
    onBack: () -> Unit,
    recovery: RecoveryRunner = RecoveryRunner.NoOp,
    metadataStore: DeviceMetadataStore? = null,
) {
    val context = LocalContext.current
    val store = remember(context, metadataStore) {
        metadataStore ?: SharedPrefsDeviceMetadataStore(context)
    }
    var metadataMap by remember(store) { mutableStateOf(store.getAll()) }
    var reloadKey by remember { mutableIntStateOf(0) }
    var inspectingNode by remember { mutableStateOf<MeshNode?>(null) }
    var inspectingDevice by remember { mutableStateOf<ConnectedDevice?>(null) }

    val snack = remember { SnackbarHostState() }
    val data by produceState<Result<ConnectedDevicesData>?>(null, reloadKey) {
        value = null
        value = runCatching {
            recovery.run {
                withContext(Dispatchers.IO) {
                    ConnectedDevices.loadData(engine.client)
                }
            }
        }
    }

    val palette = groupPalette(Group.WIFI)

    ScreenScaffold(
        title = stringResource(R.string.screen_topology),
        onBack = onBack,
        snack = snack,
        palette = palette,
        icon = Icons.Filled.Router,
        actions = {
            IconButton(onClick = { reloadKey++ }) {
                Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.refresh))
            }
        },
    ) {
        val res = data
        when {
            res == null -> LoadingState()
            res.isFailure -> ErrorState(errorText(res.exceptionOrNull()!!)) { reloadKey++ }
            else -> {
                val connData = res.getOrThrow()
                val devices = connData.devices
                val nodes = connData.nodes

                val controller = nodes.firstOrNull { it.isController } ?: MeshNode(
                    mac = "",
                    name = stringResource(R.string.mesh_role_controller),
                    model = "Router",
                    ip = "192.168.1.1",
                    isController = true,
                    active = true,
                    backhaulType = null,
                    backhaulSignal = null,
                    linkRate = null,
                    uptime = null,
                    connectedClientsCount = devices.count { it.active && (it.nodeMac == null || it.nodeMac == "") },
                )
                val satellites = nodes.filter { !it.isController }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // 1. Internet Gateway Node
                    item {
                        GatewayCard()
                        TreeConnector()
                    }

                    // 2. Primary Router (Controller) Card
                    item {
                        val controllerDevicesCount = if (controller.mac.isNotEmpty()) {
                            devices.count { it.active && it.nodeMac.equals(controller.mac, ignoreCase = true) }
                        } else {
                            devices.count { it.active && (it.nodeMac == null || satellites.none { s -> s.mac.equals(it.nodeMac, true) }) }
                        }

                        NodeCard(
                            node = controller.copy(connectedClientsCount = controllerDevicesCount),
                            isController = true,
                            onClick = {
                                inspectingNode = controller.copy(connectedClientsCount = controllerDevicesCount)
                            },
                        )
                    }

                    // 3. Satellites or Fallback Notice
                    if (satellites.isNotEmpty()) {
                        satellites.forEach { sat ->
                            val satCount = devices.count { it.active && it.nodeMac.equals(sat.mac, ignoreCase = true) }
                            val updatedSat = sat.copy(connectedClientsCount = satCount)
                            item {
                                TreeConnector()
                                BackhaulLinkBadge(sat)
                                TreeConnector()
                                NodeCard(
                                    node = updatedSat,
                                    isController = false,
                                    onClick = { inspectingNode = updatedSat },
                                )
                            }
                        }
                    } else {
                        item {
                            Spacer(Modifier.height(24.dp))
                            SingleNodeFallbackCard(
                                onInspectDevices = {
                                    val count = devices.count { it.active }
                                    inspectingNode = controller.copy(connectedClientsCount = count)
                                },
                            )
                        }
                    }

                    item {
                        Spacer(Modifier.height(32.dp))
                    }
                }

                // Modal BottomSheet for inspecting node clients
                val currentNode = inspectingNode
                if (currentNode != null) {
                    val nodeClients = devices.filter { dev ->
                        if (currentNode.isController) {
                            dev.nodeMac.isNullOrEmpty() || dev.nodeMac.equals(currentNode.mac, ignoreCase = true) ||
                                satellites.none { s -> s.mac.equals(dev.nodeMac, ignoreCase = true) }
                        } else {
                            dev.nodeMac.equals(currentNode.mac, ignoreCase = true)
                        }
                    }

                    NodeClientsBottomSheet(
                        node = currentNode,
                        clients = nodeClients,
                        metadataMap = metadataMap,
                        onDismiss = { inspectingNode = null },
                        onClientClick = { dev -> inspectingDevice = dev },
                    )
                }

                // Modal BottomSheet for inspecting single device details
                val currentDevice = inspectingDevice
                if (currentDevice != null) {
                    val meta = metadataMap[currentDevice.mac.trim().uppercase()]
                    DeviceDetailSheet(
                        device = currentDevice,
                        initialMetadata = meta,
                        onDismiss = { inspectingDevice = null },
                        onSave = { updatedMeta ->
                            store.save(currentDevice.mac, updatedMeta)
                            metadataMap = store.getAll()
                        },
                        onReset = {
                            store.reset(currentDevice.mac)
                            metadataMap = store.getAll()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun GatewayCard() {
    val c = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = c.surfaceContainerHigh),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ShapeBadge(
                icon = Icons.Filled.Public,
                container = c.primaryContainer,
                content = c.onPrimaryContainer,
                size = 52.dp,
                shapeIndex = 2,
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.topology_gateway),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.topology_gpon_fiber),
                    style = MaterialTheme.typography.bodySmall,
                    color = c.onSurfaceVariant,
                )
            }
            Surface(
                shape = CircleShape,
                color = c.secondaryContainer,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(c.primary),
                    )
                    Text(
                        text = stringResource(R.string.devices_active),
                        style = MaterialTheme.typography.labelSmall,
                        color = c.onSecondaryContainer,
                    )
                }
            }
        }
    }
}

@Composable
private fun TreeConnector() {
    val c = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .width(3.dp)
            .height(20.dp)
            .background(c.outlineVariant),
    )
}

@Composable
private fun BackhaulLinkBadge(node: MeshNode) {
    val c = MaterialTheme.colorScheme
    val isWired = node.backhaulType.equals("Ethernet", ignoreCase = true)
    val health = node.backhaulHealth

    val (containerColor, contentColor, icon) = when {
        isWired -> Triple(c.tertiaryContainer, c.onTertiaryContainer, Icons.Filled.Lan)
        health == BackhaulHealth.WIFI_EXCELLENT -> Triple(c.secondaryContainer, c.onSecondaryContainer, Icons.Filled.Wifi)
        health == BackhaulHealth.WIFI_GOOD -> Triple(c.surfaceContainerHighest, c.onSurfaceVariant, Icons.Filled.Wifi)
        else -> Triple(c.errorContainer, c.onErrorContainer, Icons.Filled.WarningAmber)
    }

    val statusText = when {
        isWired -> stringResource(R.string.topology_backhaul_optimal)
        health == BackhaulHealth.WIFI_EXCELLENT -> stringResource(R.string.topology_backhaul_excellent)
        health == BackhaulHealth.WIFI_GOOD -> stringResource(R.string.topology_backhaul_good)
        else -> stringResource(R.string.topology_backhaul_weak)
    }

    val speedRate = node.linkRate?.let { "$it Mbit/s" }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = containerColor,
            contentColor = contentColor,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                val mediumLabel = if (isWired) {
                    stringResource(R.string.mesh_backhaul_wired)
                } else {
                    stringResource(R.string.mesh_backhaul_wifi)
                }
                val label = buildString {
                    append(mediumLabel)
                    if (speedRate != null) append(" · $speedRate")
                    append(" · $statusText")
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (health == BackhaulHealth.WIFI_WEAK) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = c.errorContainer),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        Icons.Filled.WarningAmber,
                        contentDescription = null,
                        tint = c.error,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = stringResource(R.string.topology_backhaul_relocate_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = c.onErrorContainer,
                    )
                }
            }
        }
    }
}

@Composable
private fun NodeCard(
    node: MeshNode,
    isController: Boolean,
    onClick: () -> Unit,
) {
    val c = MaterialTheme.colorScheme
    val border = if (isController) BorderStroke(2.dp, c.primary) else null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = c.surfaceContainerHigh),
        border = border,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ShapeBadge(
                icon = if (isController) Icons.Filled.Router else (if (node.backhaulType == "Ethernet") Icons.Filled.Lan else Icons.Filled.Wifi),
                container = if (isController) c.primaryContainer else c.secondaryContainer,
                content = if (isController) c.onPrimaryContainer else c.onSecondaryContainer,
                size = 52.dp,
                shapeIndex = if (isController) 0 else 4,
            )

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = node.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (isController) {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = stringResource(R.string.mesh_role_controller),
                            tint = c.primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                val details = buildList {
                    if (node.model.isNotEmpty()) add(node.model)
                    if (node.ip.isNotEmpty()) add(node.ip)
                }.joinToString(" · ")

                if (details.isNotEmpty()) {
                    Text(
                        text = details,
                        style = MaterialTheme.typography.bodySmall,
                        color = c.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Surface(
                shape = CircleShape,
                color = c.surfaceContainerHighest,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        Icons.Filled.Devices,
                        contentDescription = null,
                        tint = c.primary,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "${node.connectedClientsCount}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = c.primary,
                    )
                    Icon(
                        Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = c.onSurfaceVariant,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SingleNodeFallbackCard(onInspectDevices: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = c.surfaceContainerHigh),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ShapeBadge(
                icon = Icons.Filled.CheckCircle,
                container = c.secondaryContainer,
                content = c.onSecondaryContainer,
                size = 48.dp,
                shapeIndex = 1,
            )
            Text(
                text = stringResource(R.string.topology_single_node_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.topology_single_node_desc),
                style = MaterialTheme.typography.bodySmall,
                color = c.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            Button(
                onClick = onInspectDevices,
                shape = RoundedCornerShape(20.dp),
            ) {
                Icon(Icons.Filled.Devices, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.topology_node_clients_sheet_title))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NodeClientsBottomSheet(
    node: MeshNode,
    clients: List<ConnectedDevice>,
    metadataMap: Map<String, DeviceCustomMetadata>,
    onDismiss: () -> Unit,
    onClientClick: (ConnectedDevice) -> Unit,
    sheetState: SheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden),
) {
    val c = MaterialTheme.colorScheme

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = c.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ShapeBadge(
                    icon = if (node.isController) Icons.Filled.Router else Icons.Filled.Lan,
                    container = if (node.isController) c.primaryContainer else c.secondaryContainer,
                    content = if (node.isController) c.onPrimaryContainer else c.onSecondaryContainer,
                    size = 48.dp,
                    shapeIndex = if (node.isController) 0 else 4,
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        text = node.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${node.ip} · ${stringResource(R.string.mesh_devices_count, clients.size)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.onSurfaceVariant,
                    )
                }
            }

            if (clients.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp, horizontal = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.topology_no_clients),
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(clients, key = { it.mac }) { device ->
                        val meta = metadataMap[device.mac.trim().uppercase()]
                        val resolved = DeviceResolver.resolve(
                            mac = device.mac,
                            hostname = device.name,
                            ip = device.ip,
                            customMeta = meta,
                        )

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onClientClick(device) },
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = c.surfaceContainerHigh),
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                ShapeBadge(
                                    icon = (resolved.category ?: DeviceCategory.OTHER).icon(),
                                    container = c.surfaceContainerHighest,
                                    content = c.onSurfaceVariant,
                                    size = 44.dp,
                                    shapeIndex = 3,
                                )

                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = resolved.displayTitle,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    val sub = buildList {
                                        if (device.ip.isNotEmpty()) add(device.ip)
                                        if (resolved.vendor != null) add(resolved.vendor)
                                    }.joinToString(" · ")
                                    Text(
                                        text = sub,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = c.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }

                                Column(
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    Icon(
                                        imageVector = if (device.wifi) signalIconForLevel(device.level) else Icons.Filled.Lan,
                                        contentDescription = null,
                                        tint = if (device.active) c.primary else c.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    val rate = device.rate
                                    if (rate != null && rate > 0) {
                                        Text(
                                            text = "${rate}M",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = c.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
