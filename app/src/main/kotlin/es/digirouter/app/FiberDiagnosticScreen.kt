package es.digirouter.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import es.routerapp.protocol.optical.DiagnosticSnapshot
import es.routerapp.protocol.optical.DiagnosticStep
import es.routerapp.protocol.optical.DiagnosticStepId
import es.routerapp.protocol.optical.DiagnosticStepStatus
import es.routerapp.protocol.optical.OpticalHealthStatus
import es.routerapp.protocol.optical.OpticalTelemetry
import es.routerapp.protocol.pages.Group

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
    val snack = remember { SnackbarHostState() }
    val palette = groupPalette(Group.ASSISTANT)

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
                onRetry = { viewModel.retry() },
            )
        }
    }
}

@Composable
private fun FiberDiagnosticContent(
    snapshot: DiagnosticSnapshot,
    onRetry: () -> Unit,
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
            DiagnosticStepCard(step = step)
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

            // Power Value Display
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = statusTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = contentColor,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = statusDesc,
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.onSurfaceVariant,
                    )
                }
            }

            // Metric Value Row (Rx / Tx / Temp / Voltage)
            if (telemetry != null && (telemetry.rxPowerDbm != null || telemetry.isExternalOnt)) {
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
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
