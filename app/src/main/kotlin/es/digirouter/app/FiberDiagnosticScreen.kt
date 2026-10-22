package es.digirouter.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import es.routerapp.protocol.optical.DiagnosticSnapshot
import es.routerapp.protocol.optical.DiagnosticStep
import es.routerapp.protocol.optical.DiagnosticStepId
import es.routerapp.protocol.optical.DiagnosticStepStatus
import es.routerapp.protocol.optical.GponAuthInfo
import es.routerapp.protocol.optical.GponHexUtil
import es.routerapp.protocol.optical.OmciRestartProgress
import es.routerapp.protocol.optical.OmciRestartStage
import es.routerapp.protocol.optical.OpticalHealthStatus
import es.routerapp.protocol.optical.OpticalTelemetry
import es.routerapp.protocol.optical.WanRenewProgress
import es.routerapp.protocol.optical.WanRenewStage
import es.routerapp.protocol.pages.Group
import kotlinx.coroutines.launch

private val ColorOptimal = Color(0xFF2E7D32)
private val ColorMarginal = Color(0xFFED6C02)
private val ColorCritical = Color(0xFFD32F2F)
private val ColorSaturated = Color(0xFF9C27B0)
private val ColorExternal = Color(0xFF0288D1)

@Composable
fun FiberDiagnosticScreen(
    viewModel: FiberDiagnosticViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val wanRenewState by viewModel.wanRenewState.collectAsState()
    val omciRestartState by viewModel.omciRestartState.collectAsState()
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val palette = groupPalette(Group.ASSISTANT)
    val androidContext = LocalContext.current
    val clipboard = androidContext.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager

    var showOmciRestartDialog by remember { mutableStateOf(false) }
    var showWanRenewDialog by remember { mutableStateOf(false) }

    if (showOmciRestartDialog) {
        AlertDialog(
            onDismissRequest = { showOmciRestartDialog = false },
            title = { Text(stringResource(R.string.fiber_ont_restart_omci_dialog_title)) },
            text = { Text(stringResource(R.string.fiber_ont_restart_omci_dialog_desc)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showOmciRestartDialog = false
                        viewModel.restartOmci { success ->
                            scope.launch {
                                snack.showSnackbar(
                                    androidContext.getString(
                                        if (success) R.string.fiber_ont_restart_omci_success
                                        else R.string.fiber_ont_restart_omci_failed
                                    )
                                )
                            }
                        }
                    }
                ) {
                    Text(stringResource(R.string.fiber_ont_restart_omci_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showOmciRestartDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showWanRenewDialog) {
        AlertDialog(
            onDismissRequest = { showWanRenewDialog = false },
            title = { Text(stringResource(R.string.fiber_wan_renew_dialog_title)) },
            text = { Text(stringResource(R.string.fiber_wan_renew_dialog_desc)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showWanRenewDialog = false
                        viewModel.renewWanSession { success, newIp ->
                            scope.launch {
                                if (success && newIp != null) {
                                    val elapsed = viewModel.wanRenewState.value.elapsedSeconds
                                    snack.showSnackbar(
                                        androidContext.getString(R.string.fiber_wan_renew_success, elapsed, newIp)
                                    )
                                } else {
                                    snack.showSnackbar(androidContext.getString(R.string.fiber_wan_renew_failed))
                                }
                            }
                        }
                    }
                ) {
                    Text(stringResource(R.string.fiber_wan_renew_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showWanRenewDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    ScreenScaffold(
        title = stringResource(R.string.fiber_diag_title),
        onBack = onBack,
        snack = snack,
        palette = palette,
        icon = pageIcon("fiber-diag"),
        actions = {
            IconButton(onClick = { viewModel.retry() }) {
                Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.fiber_diag_rerun))
            }
        },
    ) {
        when (val s = state) {
            is FiberDiagnosticUiState.Loading -> LoadingState()
            is FiberDiagnosticUiState.Error -> ErrorState(s.message) { viewModel.retry() }
            is FiberDiagnosticUiState.Ready -> FiberDiagnosticContent(
                snapshot = s.snapshot,
                wanRenewState = wanRenewState,
                omciRestartState = omciRestartState,
                onRetry = { viewModel.retry() },
                onRequestOmciRestart = { showOmciRestartDialog = true },
                onRequestWanRenew = { showWanRenewDialog = true },
                onCopy = { label, text ->
                    clipboard?.setPrimaryClip(ClipData.newPlainText("Fiber Data", text))
                    scope.launch { snack.showSnackbar(label) }
                },
            )
        }
    }
}

@Composable
private fun FiberDiagnosticContent(
    snapshot: DiagnosticSnapshot,
    wanRenewState: WanRenewProgress,
    omciRestartState: OmciRestartProgress,
    onRetry: () -> Unit,
    onRequestOmciRestart: () -> Unit,
    onRequestWanRenew: () -> Unit,
    onCopy: (String, String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // 1. Optical Traffic Light Hero Card
        item {
            OpticalTrafficLightCard(telemetry = snapshot.opticalTelemetry)
        }

        // 2. Connectivity Test Steps Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.fiber_diag_subtitle),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (snapshot.isRunning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        // 3. Step Cards
        items(snapshot.steps, key = { it.id.name }) { step ->
            DiagnosticStepCard(
                step = step,
                snapshot = snapshot,
                wanRenewState = wanRenewState,
                omciRestartState = omciRestartState,
                onRequestOmciRestart = onRequestOmciRestart,
                onRequestWanRenew = onRequestWanRenew,
                onCopy = onCopy,
            )
        }

        // 4. Overall Execution Summary & Retry Card
        item {
            OverallSummaryCard(snapshot = snapshot, onRetry = onRetry)
        }
    }
}

@Composable
private fun OpticalTrafficLightCard(
    telemetry: OpticalTelemetry?,
) {
    val status = telemetry?.status ?: OpticalHealthStatus.UNKNOWN
    val c = MaterialTheme.colorScheme

    val (cardColor, contentColor, statusTitle, statusDesc, badgeColor) = when (status) {
        OpticalHealthStatus.OPTIMAL -> TrafficLightVisuals(
            containerColor = ColorOptimal.copy(alpha = 0.12f),
            textColor = c.onSurface,
            title = stringResource(R.string.fiber_optical_optimal_title),
            desc = stringResource(R.string.fiber_optical_optimal_desc),
            badgeColor = ColorOptimal,
        )
        OpticalHealthStatus.MARGINAL -> TrafficLightVisuals(
            containerColor = ColorMarginal.copy(alpha = 0.14f),
            textColor = c.onSurface,
            title = stringResource(R.string.fiber_optical_marginal_title),
            desc = stringResource(R.string.fiber_optical_marginal_desc),
            badgeColor = ColorMarginal,
        )
        OpticalHealthStatus.CRITICAL -> TrafficLightVisuals(
            containerColor = ColorCritical.copy(alpha = 0.14f),
            textColor = c.onSurface,
            title = stringResource(R.string.fiber_optical_critical_title),
            desc = stringResource(R.string.fiber_optical_critical_desc),
            badgeColor = ColorCritical,
        )
        OpticalHealthStatus.SATURATED -> TrafficLightVisuals(
            containerColor = ColorSaturated.copy(alpha = 0.14f),
            textColor = c.onSurface,
            title = stringResource(R.string.fiber_optical_saturated_title),
            desc = stringResource(R.string.fiber_optical_saturated_desc),
            badgeColor = ColorSaturated,
        )
        OpticalHealthStatus.EXTERNAL_ONT -> TrafficLightVisuals(
            containerColor = ColorExternal.copy(alpha = 0.12f),
            textColor = c.onSurface,
            title = stringResource(R.string.fiber_optical_external_title),
            desc = stringResource(R.string.fiber_optical_external_desc),
            badgeColor = ColorExternal,
        )
        OpticalHealthStatus.UNKNOWN -> TrafficLightVisuals(
            containerColor = c.surfaceContainerHigh,
            textColor = c.onSurface,
            title = stringResource(R.string.fiber_optical_unknown_title),
            desc = stringResource(R.string.fiber_optical_unknown_desc),
            badgeColor = c.outline,
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = BorderStroke(1.5.dp, badgeColor.copy(alpha = 0.4f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Header Row: Traffic Light dot + Category Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(badgeColor),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.fiber_optical_title),
                        style = MaterialTheme.typography.labelLarge,
                        color = c.onSurfaceVariant,
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = badgeColor.copy(alpha = 0.18f),
                ) {
                    Text(
                        text = status.name,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }

            // Status Title and Description
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = statusTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                )
                Text(
                    text = statusDesc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.onSurfaceVariant,
                )
            }

            // Transceiver Telemetry Grid
            if (telemetry != null && !telemetry.isExternalOnt && telemetry.hasValidSignal) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = c.surface.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                    ) {
                        MetricItem(
                            label = stringResource(R.string.fiber_metric_rx),
                            value = telemetry.rxPowerDbm?.let { String.format("%.2f dBm", it) } ?: "N/A",
                            color = badgeColor,
                        )
                        telemetry.txPowerDbm?.let { tx ->
                            MetricItem(
                                label = stringResource(R.string.fiber_metric_tx),
                                value = String.format("%.2f dBm", tx),
                            )
                        }
                        telemetry.temperatureC?.let { temp ->
                            MetricItem(
                                label = stringResource(R.string.fiber_metric_temp),
                                value = String.format("%.1f °C", temp),
                            )
                        }
                        telemetry.voltageV?.let { volt ->
                            MetricItem(
                                label = stringResource(R.string.fiber_metric_voltage),
                                value = String.format("%.2f V", volt),
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class TrafficLightVisuals(
    val containerColor: Color,
    val textColor: Color,
    val title: String,
    val desc: String,
    val badgeColor: Color,
)

@Composable
private fun MetricItem(
    label: String,
    value: String,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = color,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DiagnosticStepCard(
    step: DiagnosticStep,
    snapshot: DiagnosticSnapshot,
    wanRenewState: WanRenewProgress,
    omciRestartState: OmciRestartProgress,
    onRequestOmciRestart: () -> Unit,
    onRequestWanRenew: () -> Unit,
    onCopy: (String, String) -> Unit,
) {
    val c = MaterialTheme.colorScheme
    val title = when (step.id) {
        DiagnosticStepId.OPTICAL_LINK -> stringResource(R.string.fiber_step_optical_title)
        DiagnosticStepId.OMCI_REGISTRATION -> stringResource(R.string.fiber_step_omci_title)
        DiagnosticStepId.WAN_SESSION -> stringResource(R.string.fiber_step_wan_title)
        DiagnosticStepId.GATEWAY_PING -> stringResource(R.string.fiber_step_gateway_title)
        DiagnosticStepId.DNS_INTERNET -> stringResource(R.string.fiber_step_dns_title)
    }

    val (icon, tint) = when (step.status) {
        DiagnosticStepStatus.PENDING -> Icons.Filled.HourglassEmpty to c.outline
        DiagnosticStepStatus.RUNNING -> null to c.primary
        DiagnosticStepStatus.SUCCESS -> Icons.Filled.CheckCircle to ColorOptimal
        DiagnosticStepStatus.WARNING -> Icons.Filled.Warning to ColorMarginal
        DiagnosticStepStatus.FAILED -> Icons.Filled.Cancel to ColorCritical
        DiagnosticStepStatus.SKIPPED -> Icons.Filled.RemoveCircleOutline to c.outline.copy(alpha = 0.5f)
    }

    val containerColor = when (step.status) {
        DiagnosticStepStatus.FAILED -> ColorCritical.copy(alpha = 0.08f)
        DiagnosticStepStatus.RUNNING -> c.primaryContainer.copy(alpha = 0.25f)
        else -> c.surfaceContainerHigh
    }

    var isExpanded by remember { mutableStateOf(false) }
    val isOmciStep = step.id == DiagnosticStepId.OMCI_REGISTRATION && snapshot.gponAuth != null
    val isWanStep = step.id == DiagnosticStepId.WAN_SESSION

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Status Icon / Spinner
                Box(
                    modifier = Modifier.size(28.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (step.status == DiagnosticStepStatus.RUNNING) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp,
                            color = c.primary,
                        )
                    } else if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = step.status.name,
                            tint = tint,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                }

                // Step Details Column
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = c.onSurface,
                    )
                    step.detailMessage?.let { detail ->
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (step.status == DiagnosticStepStatus.FAILED) ColorCritical else c.onSurfaceVariant,
                        )
                    }
                }

                // Metric Summary Chip
                step.metricSummary?.let { metric ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = when (step.status) {
                            DiagnosticStepStatus.SUCCESS -> ColorOptimal.copy(alpha = 0.15f)
                            DiagnosticStepStatus.WARNING -> ColorMarginal.copy(alpha = 0.15f)
                            DiagnosticStepStatus.FAILED -> ColorCritical.copy(alpha = 0.15f)
                            else -> c.surfaceContainerHighest
                        },
                    ) {
                        Text(
                            text = metric,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = when (step.status) {
                                DiagnosticStepStatus.SUCCESS -> ColorOptimal
                                DiagnosticStepStatus.WARNING -> ColorMarginal
                                DiagnosticStepStatus.FAILED -> ColorCritical
                                else -> c.onSurface
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }

                // Expansion toggle button for interactive steps
                if (isOmciStep || isWanStep) {
                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            contentDescription = null,
                            tint = c.onSurfaceVariant,
                        )
                    }
                }
            }

            // Expandable OMCI ONT Registration Details
            if (isOmciStep && isExpanded) {
                snapshot.gponAuth?.let { auth ->
                    OntRegistrationSection(
                        auth = auth,
                        omciRestartState = omciRestartState,
                        onRequestOmciRestart = onRequestOmciRestart,
                        onCopy = onCopy,
                    )
                }
            }

            // Expandable WAN Session Controls
            if (isWanStep && isExpanded) {
                WanControlSection(
                    activeWan = snapshot.activeWan,
                    wanRenewState = wanRenewState,
                    onRequestWanRenew = onRequestWanRenew,
                )
            }
        }
    }
}

@Composable
private fun OntRegistrationSection(
    auth: GponAuthInfo,
    omciRestartState: OmciRestartProgress,
    onRequestOmciRestart: () -> Unit,
    onCopy: (String, String) -> Unit,
) {
    val c = MaterialTheme.colorScheme
    var passwordRevealed by remember { mutableStateOf(false) }
    val copiedLabel = stringResource(R.string.fiber_ont_copied)
    val exportCopiedLabel = stringResource(R.string.fiber_ont_export_copied)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(14.dp),
        color = c.surfaceContainerLowest,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Header Row: Title and Read-Only badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.fiber_ont_details_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = c.onSurface,
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = c.secondaryContainer.copy(alpha = 0.5f),
                ) {
                    Text(
                        text = stringResource(R.string.fiber_ont_read_only_badge),
                        style = MaterialTheme.typography.labelSmall,
                        color = c.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }

            // GPON SN (ASCII)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(stringResource(R.string.fiber_ont_sn_ascii), style = MaterialTheme.typography.labelSmall, color = c.onSurfaceVariant)
                    Text(auth.snAscii.ifBlank { "N/A" }, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = { onCopy(copiedLabel, auth.snAscii) }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = stringResource(R.string.fiber_ont_copy), modifier = Modifier.size(16.dp))
                }
            }

            // GPON SN (HEX)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(stringResource(R.string.fiber_ont_sn_hex), style = MaterialTheme.typography.labelSmall, color = c.onSurfaceVariant)
                    Text(auth.snHex.ifBlank { "N/A" }, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = { onCopy(copiedLabel, auth.snHex) }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = stringResource(R.string.fiber_ont_copy), modifier = Modifier.size(16.dp))
                }
            }

            // PLOAM Password
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(stringResource(R.string.fiber_ont_ploam_pwd), style = MaterialTheme.typography.labelSmall, color = c.onSurfaceVariant)
                    val displayPwd = if (passwordRevealed) auth.gponPwd.ifBlank { "N/A" } else "••••••••••••"
                    Text(displayPwd, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
                Row {
                    IconButton(onClick = { passwordRevealed = !passwordRevealed }, modifier = Modifier.size(32.dp)) {
                        Icon(if (passwordRevealed) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = { onCopy(copiedLabel, auth.gponPwd) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = stringResource(R.string.fiber_ont_copy), modifier = Modifier.size(16.dp))
                    }
                }
            }

            // OLT Mode
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(stringResource(R.string.fiber_ont_olt_mode), style = MaterialTheme.typography.labelSmall, color = c.onSurfaceVariant)
                    Text("${auth.oltMode.displayName} (Vendor: ${auth.oltMode.vendorId})", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        val snippet = GponHexUtil.generateSfpExportSnippet(
                            snAscii = auth.snAscii,
                            snHex = auth.snHex,
                            ploamPassword = auth.gponPwd,
                            oltModeName = auth.oltMode.displayName,
                        )
                        onCopy(exportCopiedLabel, snippet)
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.fiber_ont_export_config), style = MaterialTheme.typography.labelMedium)
                }

                Button(
                    onClick = onRequestOmciRestart,
                    enabled = !omciRestartState.isInProgress,
                    colors = ButtonDefaults.buttonColors(containerColor = c.primary),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.fiber_ont_restart_omci), style = MaterialTheme.typography.labelMedium)
                }
            }

            // In-flight progress for OMCI Restart
            if (omciRestartState.isInProgress) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(
                        text = omciRestartState.statusText ?: stringResource(R.string.fiber_ont_restart_omci_in_progress),
                        style = MaterialTheme.typography.labelSmall,
                        color = c.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun WanControlSection(
    activeWan: Map<String, String>?,
    wanRenewState: WanRenewProgress,
    onRequestWanRenew: () -> Unit,
) {
    val c = MaterialTheme.colorScheme

    val proto = activeWan?.get("connType") ?: "PPPoE"
    val ip = activeWan?.get("externalIPAddress")
        ?: activeWan?.get("connIPv4Address")
        ?: activeWan?.get("ip")
        ?: "N/A"
    val gw = activeWan?.get("defaultGateway") ?: "N/A"
    val dns = activeWan?.get("DNSServers") ?: activeWan?.get("dnsServer") ?: "N/A"

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(14.dp),
        color = c.surfaceContainerLowest,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.fiber_wan_details_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = c.onSurface,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(stringResource(R.string.fiber_wan_proto), style = MaterialTheme.typography.labelSmall, color = c.onSurfaceVariant)
                    Text(proto, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(stringResource(R.string.fiber_wan_ip), style = MaterialTheme.typography.labelSmall, color = c.onSurfaceVariant)
                    Text(ip, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(stringResource(R.string.fiber_wan_gateway), style = MaterialTheme.typography.labelSmall, color = c.onSurfaceVariant)
                    Text(gw, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(stringResource(R.string.fiber_wan_dns), style = MaterialTheme.typography.labelSmall, color = c.onSurfaceVariant)
                    Text(dns, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                }
            }

            FilledTonalButton(
                onClick = onRequestWanRenew,
                enabled = !wanRenewState.isInProgress,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.fiber_wan_renew_button))
            }

            // In-flight progress for WAN Renewal
            if (wanRenewState.isInProgress) {
                val stageText = when (wanRenewState.stage) {
                    WanRenewStage.DISCONNECTING -> stringResource(R.string.fiber_wan_renew_in_progress_disconn)
                    WanRenewStage.RECONNECTING -> stringResource(R.string.fiber_wan_renew_in_progress_reconn)
                    WanRenewStage.VERIFYING -> stringResource(R.string.fiber_wan_renew_in_progress_verify)
                    else -> ""
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(
                        text = stageText,
                        style = MaterialTheme.typography.labelSmall,
                        color = c.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun OverallSummaryCard(
    snapshot: DiagnosticSnapshot,
    onRetry: () -> Unit,
) {
    val c = MaterialTheme.colorScheme

    when {
        snapshot.isRunning -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = c.surfaceContainerHigh),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = stringResource(R.string.fiber_diag_running),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = c.onSurface,
                    )
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        }
        snapshot.isSuccess && !snapshot.hasWarnings -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ColorOptimal.copy(alpha = 0.12f)),
                border = BorderStroke(1.dp, ColorOptimal.copy(alpha = 0.3f)),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = ColorOptimal,
                        modifier = Modifier.size(36.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.fiber_diag_all_passed),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ColorOptimal,
                        )
                        Text(
                            text = snapshot.summaryMessage.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = c.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        snapshot.isSuccess && snapshot.hasWarnings -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ColorMarginal.copy(alpha = 0.14f)),
                border = BorderStroke(1.dp, ColorMarginal.copy(alpha = 0.4f)),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = null,
                            tint = ColorMarginal,
                            modifier = Modifier.size(36.dp),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.fiber_diag_passed_with_warnings),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ColorMarginal,
                            )
                            Text(
                                text = snapshot.summaryMessage.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = c.onSurface,
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.fiber_diag_warning_explanation),
                        style = MaterialTheme.typography.bodySmall,
                        color = c.onSurfaceVariant,
                    )
                }
            }
        }
        else -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ColorCritical.copy(alpha = 0.12f)),
                border = BorderStroke(1.dp, ColorCritical.copy(alpha = 0.3f)),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Cancel,
                            contentDescription = null,
                            tint = ColorCritical,
                            modifier = Modifier.size(32.dp),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.fiber_diag_has_issues),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ColorCritical,
                            )
                            snapshot.summaryMessage?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = c.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(containerColor = c.error),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, Modifier.size(ButtonDefaults.IconSize))
                        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                        Text(stringResource(R.string.fiber_diag_rerun))
                    }
                }
            }
        }
    }
}
