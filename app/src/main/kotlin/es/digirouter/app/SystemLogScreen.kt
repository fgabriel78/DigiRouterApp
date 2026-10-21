package es.digirouter.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.routerapp.protocol.log.LogEntry
import es.routerapp.protocol.log.LogSeverity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemLogScreen(
    viewModel: SystemLogViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val colorScheme = MaterialTheme.colorScheme

    var showClearDialog by remember { mutableStateOf(false) }
    var showSensitivitySheet by remember { mutableStateOf(false) }

    DisposableEffect(viewModel) {
        onDispose {
            viewModel.pausePolling()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Column {
                LargeFlexibleTopAppBar(
                    title = { Text(stringResource(R.string.log_title)) },
                    subtitle = { Text(stringResource(R.string.log_subtitle)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        // Live pulse button
                        LiveStreamBadge(
                            isLive = state.isLive,
                            onClick = { viewModel.toggleLive() },
                        )
                        IconButton(onClick = { showSensitivitySheet = true }) {
                            Icon(Icons.Default.Tune, contentDescription = stringResource(R.string.log_settings_title))
                        }
                        IconButton(onClick = {
                            val textToShare = state.filteredLogs.joinToString("\n") {
                                "${it.timestamp} [${it.severity.code}] ${it.module}: ${it.message}"
                            }
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.log_share_title))
                                putExtra(Intent.EXTRA_TEXT, textToShare)
                            }
                            context.startActivity(Intent.createChooser(intent, context.getString(R.string.log_share_title)))
                        }) {
                            Icon(Icons.Default.Share, contentDescription = stringResource(R.string.log_share_title))
                        }
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = stringResource(R.string.log_clear_btn))
                        }
                    },
                    scrollBehavior = scrollBehavior,
                )
                if (state.isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Search Bar
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = viewModel::setSearchQuery,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                placeholder = { Text(stringResource(R.string.log_search_hint), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )

            // Severity Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = state.severityFilter == SeverityFilterCategory.ALL,
                    onClick = { viewModel.setSeverityFilter(SeverityFilterCategory.ALL) },
                    label = { Text(stringResource(R.string.log_filter_all)) },
                )
                FilterChip(
                    selected = state.severityFilter == SeverityFilterCategory.ERRORS,
                    onClick = { viewModel.setSeverityFilter(SeverityFilterCategory.ERRORS) },
                    label = { Text(stringResource(R.string.log_filter_errors)) },
                )
                FilterChip(
                    selected = state.severityFilter == SeverityFilterCategory.WARNINGS,
                    onClick = { viewModel.setSeverityFilter(SeverityFilterCategory.WARNINGS) },
                    label = { Text(stringResource(R.string.log_filter_warnings)) },
                )
                FilterChip(
                    selected = state.severityFilter == SeverityFilterCategory.INFO,
                    onClick = { viewModel.setSeverityFilter(SeverityFilterCategory.INFO) },
                    label = { Text(stringResource(R.string.log_filter_info)) },
                )
                FilterChip(
                    selected = state.severityFilter == SeverityFilterCategory.DEBUG,
                    onClick = { viewModel.setSeverityFilter(SeverityFilterCategory.DEBUG) },
                    label = { Text(stringResource(R.string.log_filter_debug)) },
                )
            }

            // Dynamic Module Chips
            if (state.availableModules.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = state.selectedModule == null,
                        onClick = { viewModel.setSelectedModule(null) },
                        label = { Text(stringResource(R.string.log_module_all)) },
                    )
                    state.availableModules.forEach { module ->
                        FilterChip(
                            selected = state.selectedModule == module,
                            onClick = { viewModel.setSelectedModule(if (state.selectedModule == module) null else module) },
                            label = { Text(module) },
                        )
                    }
                }
            }

            // Log entries list
            if (state.filteredLogs.isEmpty() && !state.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.log_empty_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.log_empty_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    itemsIndexed(state.filteredLogs, key = { index, item -> "${item.id}_$index" }) { _, entry ->
                        LogEntryCard(
                            entry = entry,
                            onCopy = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Log Entry", it))
                                Toast.makeText(context, R.string.log_copied, Toast.LENGTH_SHORT).show()
                            },
                        )
                    }
                }
            }
        }
    }

    // Clear Confirmation Dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(stringResource(R.string.log_clear_confirm_title)) },
            text = { Text(stringResource(R.string.log_clear_confirm_msg)) },
            confirmButton = {
                Button(onClick = {
                    showClearDialog = false
                    viewModel.clearLogs()
                }) {
                    Text(stringResource(R.string.log_clear_btn))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }

    // Sensitivity Settings Bottom Sheet
    if (showSensitivitySheet) {
        val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)
        ModalBottomSheet(
            onDismissRequest = { showSensitivitySheet = false },
            sheetState = sheetState,
        ) {
            SyslogSensitivitySheetContent(
                currentSeverity = state.syslogConfig?.localSeverity ?: LogSeverity.INFO,
                isUpdating = state.isUpdatingConfig,
                onSave = { selected ->
                    viewModel.updateLocalSeverity(selected) {
                        showSensitivitySheet = false
                        Toast.makeText(context, R.string.log_settings_success, Toast.LENGTH_SHORT).show()
                    }
                },
                onCancel = { showSensitivitySheet = false },
            )
        }
    }
}

@Composable
private fun LiveStreamBadge(
    isLive: Boolean,
    onClick: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val transition = rememberInfiniteTransition(label = "pulse")
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isLive) colorScheme.primaryContainer else colorScheme.surfaceContainerHigh,
        modifier = Modifier.padding(end = 4.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isLive) Color(0xFF4CAF50) else Color.Gray)
                    .then(if (isLive) Modifier.alpha(alpha) else Modifier),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(if (isLive) R.string.log_live_active else R.string.log_live_paused),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (isLive) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LogEntryCard(
    entry: LogEntry,
    onCopy: (String) -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val (badgeBg, badgeFg) = when (entry.severity) {
        LogSeverity.EMERGENCY, LogSeverity.ALERT, LogSeverity.CRITICAL, LogSeverity.ERROR ->
            colorScheme.errorContainer to colorScheme.onErrorContainer
        LogSeverity.WARNING ->
            colorScheme.tertiaryContainer to colorScheme.onTertiaryContainer
        LogSeverity.NOTICE, LogSeverity.INFO ->
            colorScheme.secondaryContainer to colorScheme.onSecondaryContainer
        LogSeverity.DEBUG ->
            colorScheme.surfaceContainerHighest to colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCopy("${entry.timestamp} [${entry.severity.label}] ${entry.module}: ${entry.message}") },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLow),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = badgeBg,
                    ) {
                        Text(
                            text = entry.severity.label.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = badgeFg,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = entry.module,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.primary,
                    )
                }
                Text(
                    text = entry.timestamp,
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = entry.message,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                ),
                color = colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun SyslogSensitivitySheetContent(
    currentSeverity: LogSeverity,
    isUpdating: Boolean,
    onSave: (LogSeverity) -> Unit,
    onCancel: () -> Unit,
) {
    var selected by remember(currentSeverity) { mutableStateOf(currentSeverity) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
    ) {
        Text(
            text = stringResource(R.string.log_settings_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.log_settings_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(16.dp))

        LogSeverity.entries.forEach { severity ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selected = severity }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = selected == severity,
                    onClick = { selected = severity },
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "${severity.label} (Level ${severity.code})",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (selected == severity) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onCancel, enabled = !isUpdating) {
                Text(stringResource(android.R.string.cancel))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { onSave(selected) },
                enabled = !isUpdating,
            ) {
                Text(stringResource(if (isUpdating) R.string.log_settings_saving else R.string.log_settings_save))
            }
        }
    }
}
