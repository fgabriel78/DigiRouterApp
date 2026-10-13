package es.routerapp.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import es.routerapp.protocol.dualdiag.DualDiagnosticStage
import es.routerapp.protocol.dualdiag.DualDiagnosticState
import es.routerapp.protocol.dualdiag.DualDiagnosticVerdict
import es.routerapp.protocol.dualdiag.HopLatencyMetric
import es.routerapp.protocol.dualdiag.HopType
import es.routerapp.protocol.pages.Group
import java.util.Locale

@Composable
fun DualDiagnosticScreen(
    viewModel: DualDiagnosticViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val snack = remember { SnackbarHostState() }
    val palette = groupPalette(Group.ASSISTANT)

    ScreenScaffold(
        title = stringResource(R.string.dual_diag_title),
        onBack = onBack,
        snack = snack,
        palette = palette,
        icon = pageIcon("dual-diag"),
        actions = {
            IconButton(onClick = { viewModel.retry() }) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = stringResource(R.string.dual_diag_rerun),
                )
            }
        },
    ) {
        when (val s = state) {
            is DualDiagnosticUiState.Loading -> LoadingState()
            is DualDiagnosticUiState.Error -> ErrorState(s.message) { viewModel.retry() }
            is DualDiagnosticUiState.Ready -> DualDiagnosticContent(
                state = s.state,
                onRetry = { viewModel.retry() },
            )
        }
    }
}

@Composable
private fun DualDiagnosticContent(
    state: DualDiagnosticState,
    onRetry: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Progress Banner during test run
        if (state.isRunning) {
            item {
                ActiveProgressCard(stage = state.stage)
            }
        }

        // Verdict Banner when complete
        val verdict = state.verdict
        if (verdict != null) {
            item {
                VerdictCard(verdict = verdict, state = state)
            }
        }

        // Hop 1: Wi-Fi LAN
        item {
            HopComparisonCard(
                title = stringResource(R.string.dual_diag_hop_wifi),
                badgeIcon = Icons.Filled.Wifi,
                metric = state.wifiHop,
                tag = "Móvil → 192.168.1.1",
            )
        }

        // Hop 2: Router Hardware WAN (Fiber)
        item {
            HopComparisonCard(
                title = stringResource(R.string.dual_diag_hop_fiber),
                badgeIcon = Icons.Filled.Lan,
                metric = state.routerWanHop,
                tag = "Router → Cloudflare (1.1.1.1)",
                isEstimated = state.isRouterPingEstimated,
            )
        }

        // Hop 3: Mobile End-to-End Internet
        item {
            HopComparisonCard(
                title = stringResource(R.string.dual_diag_hop_mobile_net),
                badgeIcon = Icons.Filled.Public,
                metric = state.mobileInternetHop,
                tag = "Móvil → Cloudflare (1.1.1.1)",
            )
        }

        // Retry Action Button
        item {
            Button(
                onClick = onRetry,
                enabled = !state.isRunning,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.dual_diag_rerun))
            }
        }
    }
}

@Composable
private fun ActiveProgressCard(stage: DualDiagnosticStage) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ShapeBadge(
                    icon = Icons.Filled.SwapHoriz,
                    container = MaterialTheme.colorScheme.primaryContainer,
                    content = MaterialTheme.colorScheme.onPrimaryContainer,
                    size = 40.dp,
                )
                Column {
                    Text(
                        text = stringResource(R.string.dual_diag_running),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = when (stage) {
                            DualDiagnosticStage.PROBING_WIFI -> "Midiendo latencia Wi-Fi local..."
                            DualDiagnosticStage.PROBING_MOBILE_INTERNET -> "Midiendo conexión móvil hacia Cloudflare..."
                            DualDiagnosticStage.PROBING_ROUTER_WAN -> "Consultando ping hardware del router..."
                            DualDiagnosticStage.EVALUATING -> "Evaluando aislamiento de cuello de botella..."
                            else -> "Iniciando pruebas..."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun VerdictCard(
    verdict: DualDiagnosticVerdict,
    state: DualDiagnosticState,
) {
    val (titleRes, descRes, adviceRes, icon, containerColor, contentColor) = when (verdict) {
        DualDiagnosticVerdict.WIFI_BOTTLENECK -> VerdictVisuals(
            titleRes = R.string.dual_verdict_wifi_title,
            descRes = R.string.dual_verdict_wifi_desc,
            adviceRes = R.string.dual_verdict_wifi_advice,
            icon = Icons.Filled.Wifi,
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        )
        DualDiagnosticVerdict.FIBER_BOTTLENECK -> VerdictVisuals(
            titleRes = R.string.dual_verdict_fiber_title,
            descRes = R.string.dual_verdict_fiber_desc,
            adviceRes = R.string.dual_verdict_fiber_advice,
            icon = Icons.Filled.Lan,
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        )
        DualDiagnosticVerdict.ALL_OPTIMAL -> VerdictVisuals(
            titleRes = R.string.dual_verdict_optimal_title,
            descRes = R.string.dual_verdict_optimal_desc,
            adviceRes = R.string.dual_verdict_optimal_advice,
            icon = Icons.Filled.CheckCircle,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        DualDiagnosticVerdict.BOTH_DEGRADED -> VerdictVisuals(
            titleRes = R.string.dual_verdict_both_title,
            descRes = R.string.dual_verdict_both_desc,
            adviceRes = R.string.dual_verdict_both_advice,
            icon = Icons.Filled.Warning,
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        )
        DualDiagnosticVerdict.INCONCLUSIVE -> VerdictVisuals(
            titleRes = R.string.dual_verdict_inconclusive_title,
            descRes = R.string.dual_verdict_inconclusive_desc,
            adviceRes = R.string.dual_verdict_inconclusive_advice,
            icon = Icons.Filled.Info,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = MaterialTheme.colorScheme.onSurface,
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                ShapeBadge(
                    icon = icon,
                    container = contentColor.copy(alpha = 0.15f),
                    content = contentColor,
                    size = 48.dp,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(titleRes),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = contentColor,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(descRes),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = contentColor,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = contentColor.copy(alpha = 0.08f),
            ) {
                Text(
                    text = stringResource(adviceRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
    }
}

private data class VerdictVisuals(
    val titleRes: Int,
    val descRes: Int,
    val adviceRes: Int,
    val icon: ImageVector,
    val containerColor: androidx.compose.ui.graphics.Color,
    val contentColor: androidx.compose.ui.graphics.Color,
)

@Composable
private fun HopComparisonCard(
    title: String,
    badgeIcon: ImageVector,
    metric: HopLatencyMetric?,
    tag: String,
    isEstimated: Boolean = false,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ShapeBadge(
                        icon = badgeIcon,
                        container = MaterialTheme.colorScheme.surfaceContainerHigh,
                        content = MaterialTheme.colorScheme.primary,
                        size = 44.dp,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                if (metric?.avgMs != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Text(
                            text = String.format(Locale.US, "%.1f ms", metric.avgMs),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (metric != null && metric.isSuccess) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    MetricMiniItem(
                        label = stringResource(R.string.dual_diag_metric_min_max),
                        value = if (metric.minMs != null && metric.maxMs != null) {
                            String.format(Locale.US, "%.0f / %.0f ms", metric.minMs, metric.maxMs)
                        } else {
                            "-"
                        },
                    )

                    if (metric.jitterMs != null) {
                        MetricMiniItem(
                            label = stringResource(R.string.dual_diag_metric_jitter),
                            value = String.format(Locale.US, "%.1f ms", metric.jitterMs),
                        )
                    }

                    MetricMiniItem(
                        label = stringResource(R.string.dual_diag_metric_loss),
                        value = "${metric.packetLossPercent}%",
                    )
                }

                if (isEstimated) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.dual_diag_estimated_notice),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            } else {
                val err = metric?.error
                if (err != null) {
                    Text(
                        text = err,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                } else {
                    Text(
                        text = "Pendiente de medición...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricMiniItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
