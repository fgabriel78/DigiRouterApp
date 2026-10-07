package es.routerapp.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.NetworkWifi1Bar
import androidx.compose.material.icons.filled.NetworkWifi2Bar
import androidx.compose.material.icons.filled.NetworkWifi3Bar
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SignalWifi4Bar
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import es.routerapp.protocol.pages.ConnectedDevice
import es.routerapp.protocol.pages.ConnectedDevices
import es.routerapp.protocol.pages.ConnectedDevicesData
import es.routerapp.protocol.pages.Group
import es.routerapp.protocol.pages.MeshNode
import es.routerapp.protocol.pages.PageEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private class DashboardData(
    val info: Map<String, String>,
    val wan: Map<String, String>?,
    val gpon: Map<String, String>,
    val hosts: Int,
    val wifi: List<Map<String, String>>,
    val meshNodes: List<MeshNode> = emptyList(),
)

private fun fmtUptime(seconds: Long): String {
    val d = seconds / 86400
    val h = seconds % 86400 / 3600
    val m = seconds % 3600 / 60
    return if (d > 0) "${d} d ${h} h" else "${h} h ${m} min"
}

private fun fmtBytes(b: Long): String = when {
    b >= 1L shl 30 -> "%.1f GB".format(b / (1L shl 30).toDouble())
    b >= 1L shl 20 -> "%.0f MB".format(b / (1L shl 20).toDouble())
    else -> "$b B"
}

@Composable
private fun bandName(b: String?): String = when {
    b == null -> ""
    b.startsWith("2") -> stringResource(R.string.band_2_4)
    b.startsWith("5") -> stringResource(R.string.band_5)
    b.startsWith("6") -> stringResource(R.string.band_6)
    else -> b
}

@Composable
fun DashboardScreen(
    engine: PageEngine,
    onBack: () -> Unit,
    recovery: RecoveryRunner = RecoveryRunner.NoOp,
) {
    var reloadKey by remember { mutableIntStateOf(0) }
    val snack = remember { SnackbarHostState() }
    val data by produceState<Result<DashboardData>?>(null, reloadKey) {
        value = null
        value = runCatching {
            recovery.run {
                withContext(Dispatchers.IO) {
                    val c = engine.client
                    fun one(oid: String): Map<String, String> = PageEngine.toInstances(c.get(oid)).firstOrNull()?.values.orEmpty()
                    // Optional data: a failure here must not hide the rest of the dashboard.
                    fun first(oid: String): Map<String, String> =
                        runCatching { PageEngine.toInstances(c.getList(oid)).firstOrNull()?.values.orEmpty() }.getOrDefault(emptyMap())
                    val connData = runCatching { ConnectedDevices.loadData(c) }.getOrNull()
                    val wans = PageEngine.toInstances(c.getList("DEV2_ADT_WAN")).map { it.values }
                    val active = engine.activeWanName()
                    DashboardData(
                        info = one("DEV2_DEV_INFO"),
                        wan = wans.firstOrNull { it["name"] == active },
                        gpon = first("DEV2_GPON_INTF_STATS"),
                        hosts = connData?.devices?.count { it.active } ?: 0,
                        wifi = PageEngine.toInstances(c.getList("DEV2_ADT_WIFI_COMMON")).map { it.values },
                        meshNodes = connData?.nodes.orEmpty(),
                    )
                }
            }
        }
    }
    val palette = groupPalette(Group.STATUS)
    ScreenScaffold(stringResource(R.string.screen_summary), onBack, snack, palette, pageIcon("dashboard"), actions = {
        IconButton(onClick = { reloadKey++ }) { Icon(Icons.Filled.Refresh, stringResource(R.string.refresh)) }
    }) {
        val res = data
        when {
            res == null -> LoadingState()
            res.isFailure -> ErrorState(errorText(res.exceptionOrNull()!!)) { reloadKey++ }
            else -> {
                val d = res.getOrThrow()
                val online = d.wan?.get("connStatusV4") == "Connected"
                val c = MaterialTheme.colorScheme
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item(span = { GridItemSpan(2) }) {
                        Card(
                            Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(32.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (online) c.primaryContainer else c.errorContainer,
                                contentColor = if (online) c.onPrimaryContainer else c.onErrorContainer,
                            ),
                        ) {
                            Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                ShapeBadge(
                                    if (online) Icons.Filled.Router else Icons.Filled.CloudOff,
                                    if (online) c.primary else c.error, if (online) c.onPrimary else c.onError, 72.dp, 0,
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(d.info["modelName"].orEmpty().ifEmpty { stringResource(R.string.router_default) }, style = MaterialTheme.typography.titleLarge)
                                    AssistChip(
                                        onClick = {},
                                        label = { Text(stringResource(if (online) R.string.dash_online else R.string.dash_offline)) },
                                        leadingIcon = {
                                            Icon(if (online) Icons.Filled.CheckCircle else Icons.Filled.CloudOff, null, Modifier.padding(2.dp))
                                        },
                                    )
                                }
                            }
                        }
                    }
                    item { StatTile(Icons.Filled.Devices, d.hosts.toString(), stringResource(R.string.dash_devices), groupPalette(Group.WIFI), shapeIndex = 1) }
                    item {
                        StatTile(Icons.Filled.Schedule, d.info["upTime"]?.toLongOrNull()?.let(::fmtUptime) ?: "—", stringResource(R.string.dash_uptime),
                            groupPalette(Group.NETWORK), shapeIndex = 2)
                    }
                    item {
                        StatTile(Icons.Filled.ArrowDownward, d.gpon["bytesReceived"]?.toLongOrNull()?.let(::fmtBytes) ?: "—", stringResource(R.string.dash_downloaded),
                            groupPalette(Group.NAT), shapeIndex = 3)
                    }
                    item {
                        StatTile(Icons.Filled.ArrowUpward, d.gpon["bytesSent"]?.toLongOrNull()?.let(::fmtBytes) ?: "—", stringResource(R.string.dash_sent),
                            groupPalette(Group.SYSTEM), shapeIndex = 4)
                    }
                    d.wan?.let { w ->
                        item(span = { GridItemSpan(2) }) {
                            Card(
                                Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(28.dp),
                                colors = CardDefaults.cardColors(containerColor = c.surfaceContainerHigh),
                            ) {
                                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    ShapeBadge(Icons.Filled.Public, c.secondaryContainer, c.onSecondaryContainer, 52.dp, 5)
                                    Column(Modifier.weight(1f)) {
                                        Text(w["connIPv4Address"].orEmpty().ifEmpty { "—" }, style = MaterialTheme.typography.titleMedium)
                                        Text(stringResource(R.string.dash_public_ip, w["connType"].orEmpty()), style = MaterialTheme.typography.bodySmall,
                                            color = c.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                    val hasMesh = d.meshNodes.count { it.active } > 1 || d.meshNodes.any { !it.isController && it.active }
                    if (hasMesh) {
                        item(span = { GridItemSpan(2) }) {
                            Text(
                                stringResource(R.string.mesh_network_title),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                            )
                        }
                        gridItems(d.meshNodes.filter { it.active }, span = { GridItemSpan(2) }) { node ->
                            Card(
                                Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(28.dp),
                                colors = CardDefaults.cardColors(containerColor = c.surfaceContainerHigh),
                            ) {
                                Row(
                                    Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                ) {
                                    ShapeBadge(
                                        if (node.isController) Icons.Filled.Router else (if (node.backhaulType == "Ethernet") Icons.Filled.Lan else Icons.Filled.Wifi),
                                        if (node.isController) c.primaryContainer else c.secondaryContainer,
                                        if (node.isController) c.onPrimaryContainer else c.onSecondaryContainer,
                                        52.dp,
                                        if (node.isController) 0 else 4,
                                    )
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        ) {
                                            Text(
                                                node.name,
                                                style = MaterialTheme.typography.titleSmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false),
                                            )
                                            if (node.isController) {
                                                Icon(
                                                    Icons.Filled.Star,
                                                    contentDescription = stringResource(R.string.mesh_role_controller),
                                                    tint = c.primary,
                                                    modifier = Modifier.size(16.dp),
                                                )
                                            }
                                        }
                                        val details = buildList {
                                            if (node.model.isNotEmpty()) add(node.model)
                                            if (node.ip.isNotEmpty()) add(node.ip)
                                            if (!node.isController && node.backhaulType != null) {
                                                val bhText = if (node.backhaulType == "Ethernet") {
                                                    stringResource(R.string.mesh_backhaul_wired)
                                                } else {
                                                    val speedPart = if (node.linkRate != null) " ${node.linkRate} Mbit/s" else ""
                                                    "${stringResource(R.string.mesh_backhaul_wifi)}$speedPart"
                                                }
                                                add(bhText)
                                            }
                                        }.joinToString(" · ")
                                        if (details.isNotEmpty()) {
                                            Text(
                                                details,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = c.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                    }
                                    Text(
                                        stringResource(R.string.mesh_devices_count, node.connectedClientsCount),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = c.primary,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }
                    gridItems(d.wifi, span = { GridItemSpan(2) }) { w ->
                        val on = w["primaryEnable"] == "1"
                        Card(
                            Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(28.dp),
                            colors = CardDefaults.cardColors(containerColor = c.surfaceContainerHigh),
                        ) {
                            Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                ShapeBadge(
                                    if (on) Icons.Filled.Wifi else Icons.Filled.WifiOff,
                                    if (on) c.tertiaryContainer else c.surfaceContainerHighest,
                                    if (on) c.onTertiaryContainer else c.onSurfaceVariant, 52.dp, 2,
                                )
                                Column(Modifier.weight(1f)) {
                                    Text(if (on) w["primarySSID"].orEmpty() else stringResource(R.string.wifi_disabled), style = MaterialTheme.typography.titleMedium, maxLines = 1)
                                    Text(bandName(w["band"]), style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private enum class DeviceFilter { ALL, WIFI, CABLE, OFFLINE }

private fun signalIcon(level: Int?): ImageVector = when {
    level == null -> Icons.Filled.Wifi
    level >= 4 -> Icons.Filled.SignalWifi4Bar
    level == 3 -> Icons.Filled.NetworkWifi3Bar
    level == 2 -> Icons.Filled.NetworkWifi2Bar
    else -> Icons.Filled.NetworkWifi1Bar
}

@Composable
fun DevicesScreen(
    engine: PageEngine,
    onBack: () -> Unit,
    recovery: RecoveryRunner = RecoveryRunner.NoOp,
) {
    var reloadKey by remember { mutableIntStateOf(0) }
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
    var filter by rememberSaveable { mutableStateOf(DeviceFilter.ALL) }
    var selectedNodeMac by rememberSaveable { mutableStateOf<String?>(null) }
    val palette = groupPalette(Group.STATUS)
    ScreenScaffold(stringResource(R.string.screen_devices), onBack, snack, palette, pageIcon("devices"), actions = {
        IconButton(onClick = { reloadKey++ }) { Icon(Icons.Filled.Refresh, stringResource(R.string.refresh)) }
    }) {
        val res = data
        when {
            res == null -> LoadingState()
            res.isFailure -> ErrorState(errorText(res.exceptionOrNull()!!)) { reloadKey++ }
            else -> {
                val connData = res.getOrThrow()
                val everyone = connData.devices
                val nodes = connData.nodes
                val active = everyone.filter { it.active }
                val offline = everyone.filter { !it.active }
                val filteredByMedium = when (filter) {
                    DeviceFilter.ALL -> active
                    DeviceFilter.WIFI -> active.filter { it.wifi }
                    DeviceFilter.CABLE -> active.filter { !it.wifi }
                    DeviceFilter.OFFLINE -> offline
                }
                val list = if (selectedNodeMac == null) {
                    filteredByMedium
                } else {
                    filteredByMedium.filter { it.nodeMac.equals(selectedNodeMac, ignoreCase = true) }
                }
                val c = MaterialTheme.colorScheme
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatTile(Icons.Filled.Devices, active.size.toString(), stringResource(R.string.devices_active), groupPalette(Group.WIFI), Modifier.weight(1f), 1,
                                selected = filter == DeviceFilter.ALL) { filter = DeviceFilter.ALL }
                            StatTile(Icons.Filled.Wifi, active.count { it.wifi }.toString(), stringResource(R.string.devices_wifi), groupPalette(Group.NETWORK), Modifier.weight(1f), 2,
                                selected = filter == DeviceFilter.WIFI) { filter = DeviceFilter.WIFI }
                            StatTile(Icons.Filled.Lan, active.count { !it.wifi }.toString(), stringResource(R.string.devices_cable), groupPalette(Group.NAT), Modifier.weight(1f), 3,
                                selected = filter == DeviceFilter.CABLE) { filter = DeviceFilter.CABLE }
                        }
                    }
                    item {
                        StatTile(Icons.Filled.LinkOff, offline.size.toString(), stringResource(R.string.devices_disconnected), groupPalette(Group.SYSTEM), Modifier.fillMaxWidth(), 4,
                            selected = filter == DeviceFilter.OFFLINE) { filter = DeviceFilter.OFFLINE }
                    }
                    if (nodes.size > 1) {
                        item {
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                item {
                                    FilterChip(
                                        selected = selectedNodeMac == null,
                                        onClick = { selectedNodeMac = null },
                                        label = { Text(stringResource(R.string.mesh_filter_all_nodes)) },
                                        leadingIcon = if (selectedNodeMac == null) {
                                            { Icon(Icons.Filled.Check, null, Modifier.size(16.dp)) }
                                        } else null,
                                    )
                                }
                                items(nodes) { n ->
                                    val isSelected = selectedNodeMac.equals(n.mac, ignoreCase = true)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            selectedNodeMac = if (isSelected) null else n.mac
                                        },
                                        label = { Text("${n.name} (${n.connectedClientsCount})") },
                                        leadingIcon = {
                                            Icon(
                                                if (isSelected) Icons.Filled.Check else (if (n.isController) Icons.Filled.Router else Icons.Filled.Wifi),
                                                null,
                                                Modifier.size(16.dp),
                                            )
                                        },
                                    )
                                }
                            }
                        }
                    }
                    if (list.isEmpty()) item { EmptyHint(stringResource(R.string.devices_none)) }
                    items(list) { d ->
                        Card(
                            Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(28.dp),
                            colors = CardDefaults.cardColors(containerColor = if (d.active) c.surfaceContainerHigh else c.surfaceContainerLow),
                        ) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                ShapeBadge(
                                    if (d.wifi) signalIcon(d.level) else Icons.Filled.Computer,
                                    if (d.active) c.tertiaryContainer else c.surfaceContainerHighest,
                                    if (d.active) c.onTertiaryContainer else c.onSurfaceVariant,
                                    48.dp, if (d.wifi) 0 else 3,
                                )
                                Column(Modifier.weight(1f)) {
                                    Text(d.name, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                                    Text(d.ip.ifEmpty { d.mac }, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
                                    val nodeName = d.nodeName
                                    if (!nodeName.isNullOrEmpty()) {
                                        Text(
                                            stringResource(R.string.mesh_connected_to, nodeName),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = c.primary,
                                        )
                                    }
                                }
                                if (d.rate != null) Text("${d.rate} Mbit/s", style = MaterialTheme.typography.labelMedium, color = c.primary)
                                if (!d.active) Icon(Icons.Filled.WifiOff, stringResource(R.string.devices_disconnected_cd), tint = c.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}
