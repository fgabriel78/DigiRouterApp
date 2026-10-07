package es.routerapp.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import es.routerapp.protocol.pages.Group
import es.routerapp.protocol.speedtest.SpeedTestPhase
import es.routerapp.protocol.speedtest.SpeedTestProgress
import es.routerapp.protocol.speedtest.SpeedTestResult
import es.routerapp.protocol.speedtest.SpeedTestState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SpeedTestScreen(
    onBack: () -> Unit,
    vm: SpeedTestViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val lastResult by vm.lastResult.collectAsStateWithLifecycle()
    val snack = remember { SnackbarHostState() }
    val palette = groupPalette(Group.STATUS)

    ScreenScaffold(
        title = stringResource(R.string.speed_test_title),
        onBack = onBack,
        snack = snack,
        palette = palette,
        icon = pageIcon("speedtest"),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                AnimatedContent(
                    targetState = state,
                    transitionSpec = { fadeIn() + scaleIn(initialScale = 0.96f) togetherWith fadeOut() },
                    label = "speedtest_state",
                ) { targetState ->
                    when (targetState) {
                        is SpeedTestState.Idle -> IdleView(
                            lastResult = lastResult,
                            onStart = vm::startTest,
                        )
                        is SpeedTestState.Running -> RunningView(
                            progress = targetState.progress,
                            onStop = vm::stopTest,
                        )
                        is SpeedTestState.Completed -> CompletedView(
                            result = targetState.result,
                            onStartAgain = vm::startTest,
                        )
                        is SpeedTestState.Error -> ErrorView(
                            message = targetState.message,
                            partialResult = targetState.partialResult,
                            onRetry = vm::startTest,
                            onDismiss = vm::stopTest,
                        )
                    }
                }
            }

            if (state is SpeedTestState.Idle && lastResult != null) {
                item {
                    LastResultCard(lastResult = lastResult!!)
                }
            }
        }
    }
}

@Composable
private fun PhaseTracker(currentPhase: SpeedTestPhase) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val phases = listOf(
            Triple(SpeedTestPhase.PING, stringResource(R.string.speed_test_ping), Icons.Filled.Schedule),
            Triple(SpeedTestPhase.DOWNLOAD, stringResource(R.string.speed_test_download), Icons.Filled.ArrowDownward),
            Triple(SpeedTestPhase.UPLOAD, stringResource(R.string.speed_test_upload), Icons.Filled.ArrowUpward),
        )

        phases.forEach { (phase, label, icon) ->
            val isDone = currentPhase.ordinal > phase.ordinal
            val isActive = currentPhase == phase
            val color = when {
                isDone -> MaterialTheme.colorScheme.primary
                isActive -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            }

            AssistChip(
                onClick = {},
                enabled = false,
                label = { Text(label, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal) },
                leadingIcon = {
                    Icon(
                        if (isDone) Icons.Filled.Check else icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = color,
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    disabledContainerColor = if (isActive) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
                    else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                    disabledLabelColor = if (isActive) MaterialTheme.colorScheme.onTertiaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                shape = RoundedCornerShape(20.dp),
            )
        }
    }
}

@Composable
private fun IdleView(
    lastResult: SpeedTestResult?,
    onStart: () -> Unit,
) {
    val c = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = c.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.padding(28.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            ShapeBadge(
                icon = Icons.Filled.Speed,
                container = c.primaryContainer,
                content = c.onPrimaryContainer,
                size = 96.dp,
                shapeIndex = 1,
            )

            Text(
                text = stringResource(R.string.speed_test_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text = stringResource(R.string.speed_test_idle_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = c.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp),
            ) {
                Icon(Icons.Filled.PlayArrow, null, Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.speed_test_start),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

@Composable
private fun RunningView(
    progress: SpeedTestProgress,
    onStop: () -> Unit,
) {
    val c = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = c.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            PhaseTracker(currentPhase = progress.phase)

            // Speed gauge / display
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(220.dp).padding(8.dp),
            ) {
                val animatedProgress by animateFloatAsState(
                    targetValue = progress.progress,
                    label = "gauge_progress",
                )
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 14.dp,
                    color = when (progress.phase) {
                        SpeedTestPhase.PING -> c.primary
                        SpeedTestPhase.DOWNLOAD -> c.tertiary
                        SpeedTestPhase.UPLOAD -> c.secondary
                        else -> c.primary
                    },
                    trackColor = c.surfaceContainerHighest,
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    val displaySpeed = when (progress.phase) {
                        SpeedTestPhase.PING -> progress.pingMs?.let { "%.1f".format(it) } ?: "..."
                        SpeedTestPhase.DOWNLOAD -> "%.1f".format(progress.currentSpeedMbps)
                        SpeedTestPhase.UPLOAD -> "%.1f".format(progress.currentSpeedMbps)
                        else -> "0.0"
                    }
                    val unit = when (progress.phase) {
                        SpeedTestPhase.PING -> stringResource(R.string.speed_test_ms)
                        else -> stringResource(R.string.speed_test_mbps)
                    }

                    Text(
                        text = displaySpeed,
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = c.onSurface,
                    )
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.titleMedium,
                        color = c.onSurfaceVariant,
                    )
                }
            }

            // Phase status label
            val statusText = when (progress.phase) {
                SpeedTestPhase.PING -> stringResource(R.string.speed_test_running_ping)
                SpeedTestPhase.DOWNLOAD -> stringResource(R.string.speed_test_running_download)
                SpeedTestPhase.UPLOAD -> stringResource(R.string.speed_test_running_upload)
                else -> ""
            }
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = c.primary,
            )

            // Live metrics preview
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                MetricChip(
                    icon = Icons.Filled.Schedule,
                    label = stringResource(R.string.speed_test_ping),
                    value = progress.pingMs?.let { "%.0f ms".format(it) } ?: "--",
                )
                MetricChip(
                    icon = Icons.Filled.SwapHoriz,
                    label = stringResource(R.string.speed_test_jitter),
                    value = progress.jitterMs?.let { "%.0f ms".format(it) } ?: "--",
                )
                MetricChip(
                    icon = Icons.Filled.ArrowDownward,
                    label = stringResource(R.string.speed_test_download),
                    value = progress.downloadMbps?.let { "%.1f M".format(it) } ?: "--",
                )
            }

            OutlinedButton(
                onClick = onStop,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(24.dp),
            ) {
                Icon(Icons.Filled.Stop, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.speed_test_stop))
            }
        }
    }
}

@Composable
private fun MetricChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(icon, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CompletedView(
    result: SpeedTestResult,
    onStartAgain: () -> Unit,
) {
    val c = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = c.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ShapeBadge(Icons.Filled.CloudDone, c.primaryContainer, c.onPrimaryContainer, 40.dp, 0)
                Text(
                    text = stringResource(R.string.speed_test_completed),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }

            // Results grid
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ResultCard(
                    title = stringResource(R.string.speed_test_download),
                    value = "%.1f".format(result.downloadMbps),
                    unit = stringResource(R.string.speed_test_mbps),
                    icon = Icons.Filled.ArrowDownward,
                    containerColor = c.tertiaryContainer,
                    contentColor = c.onTertiaryContainer,
                    modifier = Modifier.weight(1f),
                )
                ResultCard(
                    title = stringResource(R.string.speed_test_upload),
                    value = "%.1f".format(result.uploadMbps),
                    unit = stringResource(R.string.speed_test_mbps),
                    icon = Icons.Filled.ArrowUpward,
                    containerColor = c.secondaryContainer,
                    contentColor = c.onSecondaryContainer,
                    modifier = Modifier.weight(1f),
                )
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ResultCard(
                    title = stringResource(R.string.speed_test_ping),
                    value = "%.1f".format(result.pingMs),
                    unit = stringResource(R.string.speed_test_ms),
                    icon = Icons.Filled.Schedule,
                    containerColor = c.surfaceContainerHighest,
                    contentColor = c.onSurface,
                    modifier = Modifier.weight(1f),
                )
                ResultCard(
                    title = stringResource(R.string.speed_test_jitter),
                    value = "%.1f".format(result.jitterMs),
                    unit = stringResource(R.string.speed_test_ms),
                    icon = Icons.Filled.SwapHoriz,
                    containerColor = c.surfaceContainerHighest,
                    contentColor = c.onSurface,
                    modifier = Modifier.weight(1f),
                )
            }

            if (result.serverLocation != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Dns, null, Modifier.size(16.dp), tint = c.onSurfaceVariant)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${stringResource(R.string.speed_test_server)}: ${result.serverLocation}",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.onSurfaceVariant,
                    )
                }
            }

            Button(
                onClick = onStartAgain,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(26.dp),
            ) {
                Icon(Icons.Filled.PlayArrow, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.speed_test_start))
            }
        }
    }
}

@Composable
private fun ResultCard(
    title: String,
    value: String,
    unit: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(icon, null, Modifier.size(18.dp), tint = contentColor.copy(alpha = 0.8f))
                Text(title, style = MaterialTheme.typography.labelMedium, color = contentColor.copy(alpha = 0.8f))
            }
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(unit, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 4.dp))
            }
        }
    }
}

@Composable
private fun LastResultCard(lastResult: SpeedTestResult) {
    val c = MaterialTheme.colorScheme
    val timeStr = remember(lastResult.timestamp) {
        SimpleDateFormat("HH:mm · dd/MM/yyyy", Locale.getDefault()).format(Date(lastResult.timestamp))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = c.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.speed_test_last_result),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = timeStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = c.onSurfaceVariant,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                MetricSummary(
                    label = stringResource(R.string.speed_test_download),
                    value = "%.1f %s".format(lastResult.downloadMbps, stringResource(R.string.speed_test_mbps)),
                )
                MetricSummary(
                    label = stringResource(R.string.speed_test_upload),
                    value = "%.1f %s".format(lastResult.uploadMbps, stringResource(R.string.speed_test_mbps)),
                )
                MetricSummary(
                    label = stringResource(R.string.speed_test_ping),
                    value = "%.0f %s".format(lastResult.pingMs, stringResource(R.string.speed_test_ms)),
                )
            }
        }
    }
}

@Composable
private fun MetricSummary(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ErrorView(
    message: String,
    partialResult: SpeedTestResult?,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ShapeBadge(
                icon = Icons.Filled.Speed,
                container = MaterialTheme.colorScheme.error,
                content = MaterialTheme.colorScheme.onError,
                size = 64.dp,
                shapeIndex = 3,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
                Button(onClick = onRetry) {
                    Text(stringResource(R.string.retry))
                }
            }
        }
    }
}
