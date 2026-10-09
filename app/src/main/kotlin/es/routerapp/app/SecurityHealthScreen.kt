package es.routerapp.app

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.SheetValue
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import es.routerapp.protocol.security.HealthRating
import es.routerapp.protocol.security.SecurityFinding
import es.routerapp.protocol.security.SecurityReport
import es.routerapp.protocol.security.Severity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityHealthScreen(
    viewModel: SecurityHealthViewModel,
    onBack: () -> Unit,
    onNavigateToPage: (String) -> Unit = {},
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showBatchDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.scan()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.security_health_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.scan() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.security_scan_again))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (val s = state) {
                is SecurityUiState.Loading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        ContainedLoadingIndicator()
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.security_scanning),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                is SecurityUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            Icons.Filled.Security,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error,
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = s.message,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { viewModel.scan() }) {
                            Text(stringResource(R.string.security_scan_again))
                        }
                    }
                }
                is SecurityUiState.Success -> {
                    SecurityHealthContent(
                        report = s.report,
                        selectedFilter = s.filter,
                        onFilterSelected = { viewModel.setFilter(it) },
                        onFixSingle = { action ->
                            viewModel.applySingleRemediation(action) { success ->
                                scope.launch {
                                    val msg = if (success) {
                                        context.getString(R.string.security_fix_success)
                                    } else {
                                        context.getString(R.string.security_fix_failed)
                                    }
                                    snackbarHostState.showSnackbar(msg)
                                }
                            }
                        },
                        onNavigateToPage = onNavigateToPage,
                        onOpenBatchDialog = { showBatchDialog = true },
                    )

                    if (showBatchDialog && s.report.batchPlan.isNotEmpty()) {
                        BatchRemediationBottomSheet(
                            report = s.report,
                            isApplying = s.isBatchApplying,
                            progress = s.batchProgress,
                            onDismiss = { if (!s.isBatchApplying) showBatchDialog = false },
                            onApply = {
                                viewModel.applyBatchRemediation { success, errorMsg ->
                                    showBatchDialog = false
                                    scope.launch {
                                        val msg = if (success) {
                                            context.getString(R.string.security_batch_fix_done)
                                        } else {
                                            context.getString(R.string.security_batch_fix_partial, errorMsg.orEmpty())
                                        }
                                        snackbarHostState.showSnackbar(msg)
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SecurityHealthContent(
    report: SecurityReport,
    selectedFilter: SecurityFilter,
    onFilterSelected: (SecurityFilter) -> Unit,
    onFixSingle: (es.routerapp.protocol.security.RemediationAction) -> Unit,
    onNavigateToPage: (String) -> Unit,
    onOpenBatchDialog: () -> Unit,
) {
    val visibleFindings = remember(report, selectedFilter) {
        when (selectedFilter) {
            SecurityFilter.ALL -> report.findings
            SecurityFilter.AT_RISK -> report.findings.filter { !it.isPassed }
            SecurityFilter.PASSED -> report.findings.filter { it.isPassed }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Hero Score & Health Rating Card
        item {
            SecurityScoreHeaderCard(report = report)
        }

        // 1-Tap Batch Action Button (if remediable issues exist)
        if (report.batchPlan.isNotEmpty()) {
            item {
                Button(
                    onClick = onOpenBatchDialog,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(
                        Icons.Filled.AutoFixHigh,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = stringResource(R.string.security_batch_fix_button) + " (${report.batchPlan.size})",
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        // Filter Chips
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                item {
                    FilterChip(
                        selected = selectedFilter == SecurityFilter.ALL,
                        onClick = { onFilterSelected(SecurityFilter.ALL) },
                        label = { Text(stringResource(R.string.security_filter_all, report.findings.size)) },
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == SecurityFilter.AT_RISK,
                        onClick = { onFilterSelected(SecurityFilter.AT_RISK) },
                        label = {
                            Text(
                                stringResource(
                                    R.string.security_filter_at_risk,
                                    report.findings.count { !it.isPassed },
                                )
                            )
                        },
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == SecurityFilter.PASSED,
                        onClick = { onFilterSelected(SecurityFilter.PASSED) },
                        label = {
                            Text(
                                stringResource(
                                    R.string.security_filter_passed,
                                    report.passedCount,
                                )
                            )
                        },
                    )
                }
            }
        }

        // Finding Cards
        items(visibleFindings, key = { it.id }) { finding ->
            SecurityFindingCard(
                finding = finding,
                onFixSingle = onFixSingle,
                onNavigateToPage = onNavigateToPage,
            )
        }

        item {
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SecurityScoreHeaderCard(report: SecurityReport) {
    val colors = MaterialTheme.colorScheme
    val animatedProgress by animateFloatAsState(
        targetValue = report.score / 100f,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "scoreProgress",
    )

    val gaugeColor = when (report.rating) {
        HealthRating.EXCELLENT -> colors.primary
        HealthRating.GOOD -> colors.tertiary
        HealthRating.POOR -> colors.error
    }

    val ratingTextRes = when (report.rating) {
        HealthRating.EXCELLENT -> R.string.security_rating_excellent
        HealthRating.GOOD -> R.string.security_rating_good
        HealthRating.POOR -> R.string.security_rating_poor
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                // Circular score gauge
                Box(contentAlignment = Alignment.Center) {
                    val trackColor = colors.surfaceContainerHighest
                    Canvas(modifier = Modifier.size(120.dp)) {
                        val strokeWidth = 12.dp.toPx()
                        drawArc(
                            color = trackColor,
                            startAngle = 135f,
                            sweepAngle = 270f,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                        )
                        drawArc(
                            color = gaugeColor,
                            startAngle = 135f,
                            sweepAngle = 270f * animatedProgress,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = report.score.toString(),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface,
                        )
                        Text(
                            text = "/ 100",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant,
                        )
                    }
                }

                // Summary stats & Rating
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.Start,
                ) {
                    Text(
                        text = stringResource(ratingTextRes),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = gaugeColor,
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SeverityTag(
                            count = report.criticalCount,
                            label = stringResource(R.string.security_critical_count, report.criticalCount),
                            container = colors.errorContainer,
                            content = colors.onErrorContainer,
                        )
                        SeverityTag(
                            count = report.warningCount,
                            label = stringResource(R.string.security_warning_count, report.warningCount),
                            container = colors.tertiaryContainer,
                            content = colors.onTertiaryContainer,
                        )
                    }

                    SeverityTag(
                        count = report.passedCount,
                        label = stringResource(R.string.security_passed_count, report.passedCount),
                        container = colors.primaryContainer.copy(alpha = 0.7f),
                        content = colors.onPrimaryContainer,
                    )
                }
            }
        }
    }
}

@Composable
private fun SeverityTag(
    count: Int,
    label: String,
    container: Color,
    content: Color,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (count > 0) container else container.copy(alpha = 0.3f),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (count > 0) content else content.copy(alpha = 0.5f),
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun SecurityFindingCard(
    finding: SecurityFinding,
    onFixSingle: (es.routerapp.protocol.security.RemediationAction) -> Unit,
    onNavigateToPage: (String) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val cardBg = if (finding.isPassed) colors.surfaceContainerLow else colors.surfaceContainer

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val statusIcon = if (finding.isPassed) Icons.Filled.CheckCircle else Icons.Filled.Warning
                    val statusTint = if (finding.isPassed) colors.primary else if (finding.severity == Severity.CRITICAL) colors.error else colors.tertiary

                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        tint = statusTint,
                        modifier = Modifier.size(24.dp),
                    )

                    Text(
                        text = finding.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                if (!finding.isPassed && finding.penaltyPoints > 0) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = colors.errorContainer,
                    ) {
                        Text(
                            text = "-${finding.penaltyPoints} pts",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onErrorContainer,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
            }

            Text(
                text = finding.description,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )

            val details = finding.details
            if (!details.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = colors.surfaceContainerHighest,
                ) {
                    Text(
                        text = details,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurface,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }

            val remediation = finding.remediation
            if (!finding.isPassed && remediation != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    val targetScreen = remediation.targetScreenId
                    if (remediation.isAutomaticSafe) {
                        var isFixing by remember(finding.id) { mutableStateOf(false) }
                        Button(
                            onClick = {
                                isFixing = true
                                onFixSingle(remediation)
                            },
                            enabled = !isFixing,
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.size(6.dp))
                            Text(stringResource(R.string.security_fix_now))
                        }
                    } else if (targetScreen != null) {
                        OutlinedButton(
                            onClick = { onNavigateToPage(targetScreen) },
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text(stringResource(R.string.security_configure))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BatchRemediationBottomSheet(
    report: SecurityReport,
    isApplying: Boolean,
    progress: Pair<Int, Int>?,
    onDismiss: () -> Unit,
    onApply: () -> Unit,
) {
    val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    Icons.Filled.AutoFixHigh,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp),
                )
                Text(
                    text = stringResource(R.string.security_batch_fix_dialog_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }

            Text(
                text = stringResource(R.string.security_batch_fix_dialog_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // List of actions
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (action in report.batchPlan) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = action.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Filled.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = stringResource(R.string.security_batch_fix_dialog_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }

            if (isApplying) {
                val (curr, total) = progress ?: (0 to report.batchPlan.size)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    LinearProgressIndicator(
                        progress = { if (total > 0) curr.toFloat() / total.toFloat() else 0f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = stringResource(R.string.security_batch_fix_progress, curr, total),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text(stringResource(R.string.cancel))
                    }

                    Button(
                        onClick = onApply,
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text(stringResource(R.string.security_batch_fix_apply, report.batchPlan.size))
                    }
                }
            }
        }
    }
}
