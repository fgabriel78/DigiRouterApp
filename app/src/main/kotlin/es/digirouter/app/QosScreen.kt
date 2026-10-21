package es.digirouter.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.routerapp.protocol.qos.BandwidthUnit
import es.routerapp.protocol.qos.QosConfig
import es.routerapp.protocol.qos.QosPriority
import es.routerapp.protocol.qos.QosRule
import es.routerapp.protocol.qos.QosRuleType
import es.routerapp.protocol.qos.QosValidationError
import es.routerapp.protocol.qos.bandwidthShare
import kotlin.math.roundToInt

private val CardShape = RoundedCornerShape(28.dp)

@Composable
fun QosScreen(viewModel: QosViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    var ruleToDelete by remember { mutableStateOf<QosRule?>(null) }

    // Errors after the first load are transient (snackbar); a failed first load shows a full error state instead.
    LaunchedEffect(state.error, state.config) {
        val error = state.error ?: return@LaunchedEffect
        if (state.config == null) return@LaunchedEffect
        snackbar.showSnackbar(context.getString(error.messageRes()))
        viewModel.clearError()
    }
    LaunchedEffect(state.saved) {
        if (state.saved) {
            viewModel.consumeSaved()
            snackbar.showSnackbar(context.getString(R.string.qos_saved))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.qos_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
                },
                actions = {
                    IconButton(onClick = viewModel::loadData, enabled = !state.isLoading && !state.isSaving) {
                        Icon(Icons.Filled.Refresh, stringResource(R.string.refresh))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            val draft = state.draft
            when {
                state.isLoading && draft == null -> LoadingState()
                draft == null -> ErrorState(stringResource((state.error ?: QosError.LOAD_FAILED).messageRes()), viewModel::loadData)
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item(key = "engine") { EngineCard(draft, viewModel::setEnabled) }
                    item(key = "bandwidth") { BandwidthCard(draft, viewModel) }
                    item(key = "queues") { QueuesCard(state, draft, viewModel) }
                    item(key = "devices_header") {
                        SectionHeader(stringResource(R.string.qos_devices_title), stringResource(R.string.qos_devices_subtitle))
                    }
                    if (state.devices.isEmpty()) {
                        item(key = "devices_empty") { EmptyHint(stringResource(R.string.qos_devices_empty)) }
                    }
                    items(state.devices, key = { "dev_${it.mac}" }) { device ->
                        DevicePriorityCard(
                            device = device,
                            pending = device.mac in state.pendingMacs,
                            onPriority = { viewModel.setDevicePriority(device, it) },
                        )
                    }
                    if (state.otherRules.isNotEmpty()) {
                        item(key = "rules_header") {
                            SectionHeader(stringResource(R.string.qos_other_rules_title), stringResource(R.string.qos_other_rules_subtitle))
                        }
                        items(state.otherRules, key = { "rule_${it.stack}" }) { rule ->
                            OtherRuleCard(rule, pending = rule.stack in state.pendingRuleStacks, onDelete = { ruleToDelete = rule })
                        }
                    }
                }
            }
        }
    }

    ruleToDelete?.let { rule ->
        AlertDialog(
            onDismissRequest = { ruleToDelete = null },
            icon = { Icon(Icons.Filled.Delete, null) },
            title = { Text(stringResource(R.string.qos_rule_delete)) },
            text = { Text(ruleDescription(rule)) },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteRule(rule); ruleToDelete = null }) { Text(stringResource(R.string.qos_rule_delete)) }
            },
            dismissButton = { TextButton(onClick = { ruleToDelete = null }) { Text(stringResource(android.R.string.cancel)) } },
        )
    }
}

@Composable
private fun EngineCard(draft: QosDraft, onEnabled: (Boolean) -> Unit) {
    val c = MaterialTheme.colorScheme
    Card(
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = if (draft.enabled) c.primaryContainer else c.surfaceContainerHigh,
            contentColor = if (draft.enabled) c.onPrimaryContainer else c.onSurface,
        ),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                val content = if (draft.enabled) c.onPrimaryContainer else c.onSurfaceVariant
                ShapeBadge(Icons.Filled.Speed, content.copy(alpha = 0.14f), content, 52.dp, 2)
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.qos_engine_title), style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(R.string.qos_engine_subtitle), style = MaterialTheme.typography.bodyMedium)
                }
                Switch(checked = draft.enabled, onCheckedChange = onEnabled)
            }
            AnimatedVisibility(!draft.enabled) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Info, null, Modifier.size(18.dp), tint = c.onSurfaceVariant)
                    Text(stringResource(R.string.qos_disabled_hint), style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun BandwidthCard(draft: QosDraft, vm: QosViewModel) {
    Card(shape = CardShape, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CardTitle(Icons.Filled.Tune, stringResource(R.string.qos_bandwidth_title))
            BandwidthRow(Icons.Filled.Download, stringResource(R.string.qos_download), draft.downText, draft.downUnit, vm::setDownBandwidth, vm::setDownUnit)
            BandwidthRow(Icons.Filled.Upload, stringResource(R.string.qos_upload), draft.upText, draft.upUnit, vm::setUpBandwidth, vm::setUpUnit)
            Text(stringResource(R.string.qos_bandwidth_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun BandwidthRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    text: String,
    unit: BandwidthUnit,
    onText: (String) -> Unit,
    onUnit: (BandwidthUnit) -> Unit,
) {
    val value = text.toIntOrNull()
    val invalid = value == null || value !in QosConfig.MIN_BANDWIDTH..QosConfig.MAX_BANDWIDTH
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = text,
            onValueChange = onText,
            label = { Text(label) },
            leadingIcon = { Icon(icon, null) },
            singleLine = true,
            isError = invalid,
            supportingText = if (invalid) {
                { Text(stringResource(R.string.qos_bandwidth_error, QosConfig.MIN_BANDWIDTH, QosConfig.MAX_BANDWIDTH)) }
            } else null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.weight(1f),
        )
        SingleChoiceSegmentedButtonRow(Modifier.width(148.dp).padding(top = 8.dp)) {
            BandwidthUnit.entries.forEachIndexed { i, u ->
                SegmentedButton(
                    selected = u == unit,
                    onClick = { onUnit(u) },
                    shape = SegmentedButtonDefaults.itemShape(i, BandwidthUnit.entries.size),
                    icon = {},
                ) { Text(u.label, maxLines = 1) }
            }
        }
    }
}

@Composable
private fun QueuesCard(state: QosUiState, draft: QosDraft, vm: QosViewModel) {
    val c = MaterialTheme.colorScheme
    val q = draft.queues
    Card(shape = CardShape, colors = CardDefaults.cardColors(containerColor = c.surfaceContainerHigh)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { CardTitle(Icons.Filled.Speed, stringResource(R.string.qos_queues_title)) }
                Text(stringResource(R.string.qos_queues_total, q.total), style = MaterialTheme.typography.labelLarge, color = c.onSurfaceVariant)
            }
            QueueSlider(QosPriority.HIGH, q.high, draft, vm::setHigh)
            QueueSlider(QosPriority.MIDDLE, q.middle, draft, vm::setMiddle)
            QueueSlider(QosPriority.LOW, q.low, draft, vm::setLow)
            Text(stringResource(R.string.qos_queues_hint), style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
            if (QosValidationError.QUEUE_ALLOCATION_INVALID in state.validationErrors) {
                Text(stringResource(R.string.qos_queue_invalid), style = MaterialTheme.typography.bodySmall, color = c.error)
            }
            AnimatedVisibility(state.isDirty) {
                Text(stringResource(R.string.qos_unsaved), style = MaterialTheme.typography.labelMedium, color = c.primary)
            }
            Button(
                onClick = vm::saveConfig,
                enabled = state.isDirty && !state.isSaving,
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.isSaving) LoadingIndicator(Modifier.size(24.dp))
                else Icon(Icons.Filled.Save, null, Modifier.size(ButtonDefaults.IconSize))
                Text(stringResource(R.string.qos_save), Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun QueueSlider(priority: QosPriority, percent: Int, draft: QosDraft, onChange: (Int) -> Unit) {
    val palette = priorityPalette(priority)
    val down = formatRate(bandwidthShare(draft.downBandwidth ?: 0, percent), draft.downUnit)
    val up = formatRate(bandwidthShare(draft.upBandwidth ?: 0, percent), draft.upUnit)
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PriorityChip(priority, palette)
            Text("$percent%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                stringResource(R.string.qos_queue_share, down, up),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        // The ViewModel may clamp the value (coupled limits), so the slider always mirrors the clamped percentage.
        val sliderState = rememberSliderState(percent.toFloat(), 19, 0f..100f)
        SideEffect { if (sliderState.value != percent.toFloat()) sliderState.value = percent.toFloat() }
        Slider(
            state = sliderState,
            onValueChange = { onChange(it.roundToInt()) },
        )
    }
}

@Composable
private fun DevicePriorityCard(device: QosDeviceItem, pending: Boolean, onPriority: (QosPriority) -> Unit) {
    val c = MaterialTheme.colorScheme
    val palette = priorityPalette(device.priority)
    Card(shape = CardShape, colors = CardDefaults.cardColors(containerColor = c.surfaceContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val icon = if (device.isGamePort) Icons.Filled.SportsEsports else (device.category ?: DeviceCategory.OTHER).icon()
                ShapeBadge(icon, palette.first, palette.second, 44.dp, device.priority.ordinal)
                Column(Modifier.weight(1f)) {
                    Text(device.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val sub = listOf(device.ip, device.mac).filter { it.isNotBlank() }.joinToString("  ·  ")
                    Text(sub, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (pending) LoadingIndicator(Modifier.size(28.dp))
                else if (!device.online) {
                    Surface(shape = RoundedCornerShape(12.dp), color = c.surfaceContainerHighest) {
                        Text(
                            stringResource(R.string.qos_device_offline),
                            style = MaterialTheme.typography.labelSmall,
                            color = c.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
            }
            if (device.isGamePort) {
                Surface(shape = RoundedCornerShape(16.dp), color = palette.first, contentColor = palette.second) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.SportsEsports, null, Modifier.size(18.dp))
                        Column {
                            Text(stringResource(R.string.qos_device_game_port), style = MaterialTheme.typography.labelLarge)
                            Text(stringResource(R.string.qos_game_port_locked), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            } else {
                val options = listOf(QosPriority.HIGH, QosPriority.MIDDLE, QosPriority.LOW)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    options.forEachIndexed { i, p ->
                        SegmentedButton(
                            selected = device.priority == p,
                            onClick = { onPriority(p) },
                            enabled = !pending,
                            shape = SegmentedButtonDefaults.itemShape(i, options.size),
                        ) { Text(stringResource(p.labelRes()), maxLines = 1) }
                    }
                }
            }
        }
    }
}

@Composable
private fun OtherRuleCard(rule: QosRule, pending: Boolean, onDelete: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val palette = priorityPalette(rule.priority)
    Card(shape = CardShape, colors = CardDefaults.cardColors(containerColor = c.surfaceContainer)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PriorityChip(rule.priority, palette)
            Text(ruleDescription(rule), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            if (pending) LoadingIndicator(Modifier.size(28.dp))
            else IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, stringResource(R.string.qos_rule_delete)) }
        }
    }
}

@Composable
private fun ruleDescription(rule: QosRule): String {
    val ports = if (rule.srcPort == rule.endPort || rule.endPort.isBlank()) rule.srcPort else "${rule.srcPort}-${rule.endPort}"
    return when (rule.type) {
        QosRuleType.CUSTOM_TCP -> stringResource(R.string.qos_rule_port, "TCP", ports)
        QosRuleType.CUSTOM_UDP -> stringResource(R.string.qos_rule_port, "UDP", ports)
        QosRuleType.CUSTOM_ALL -> stringResource(R.string.qos_rule_port, "TCP/UDP", ports)
        QosRuleType.SYSTEM -> stringResource(R.string.qos_rule_system)
        QosRuleType.DEVICE, QosRuleType.UNKNOWN -> stringResource(R.string.qos_rule_unknown, rule.typeCode)
    }
}

@Composable
private fun PriorityChip(priority: QosPriority, palette: Pair<Color, Color>) {
    Surface(shape = RoundedCornerShape(12.dp), color = palette.first, contentColor = palette.second) {
        Text(
            stringResource(priority.labelRes()),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun CardTitle(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column(Modifier.padding(top = 8.dp, start = 4.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Container / content colors per tier, from semantic tokens so they keep contrast in light and dark themes. */
@Composable
private fun priorityPalette(priority: QosPriority): Pair<Color, Color> {
    val c = MaterialTheme.colorScheme
    return when (priority) {
        QosPriority.HIGH -> c.primaryContainer to c.onPrimaryContainer
        QosPriority.MIDDLE -> c.secondaryContainer to c.onSecondaryContainer
        QosPriority.LOW -> c.tertiaryContainer to c.onTertiaryContainer
    }
}

private fun QosPriority.labelRes(): Int = when (this) {
    QosPriority.HIGH -> R.string.qos_priority_high
    QosPriority.MIDDLE -> R.string.qos_priority_middle
    QosPriority.LOW -> R.string.qos_priority_low
}

private fun QosError.messageRes(): Int = when (this) {
    QosError.NOT_CONNECTED -> R.string.qos_error_not_connected
    QosError.LOAD_FAILED -> R.string.qos_error_load
    QosError.SAVE_FAILED -> R.string.qos_error_save
    QosError.PRIORITY_FAILED -> R.string.qos_error_priority
    QosError.DELETE_FAILED -> R.string.qos_error_delete
}

/** `360 Mbps`, `7.5 Mbps`... */
internal fun formatRate(value: Double, unit: BandwidthUnit): String {
    val rounded = (value * 10).roundToInt() / 10.0
    val number = if (rounded % 1.0 == 0.0) rounded.toLong().toString() else rounded.toString()
    return "$number ${unit.label}"
}
