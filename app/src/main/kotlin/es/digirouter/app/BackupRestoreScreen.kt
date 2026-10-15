package es.digirouter.app

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import es.routerapp.protocol.pages.Group

@Composable
fun BackupRestoreScreen(
    viewModel: BackupRestoreViewModel,
    store: BackupStore,
    onBack: () -> Unit,
    onNavigateToLogin: () -> Unit = onBack,
) {
    val state by viewModel.state.collectAsState()
    val snack = remember { SnackbarHostState() }
    val ctx = LocalContext.current

    var restoreTarget by remember { mutableStateOf<BackupItem?>(null) }
    var deleteTarget by remember { mutableStateOf<BackupItem?>(null) }

    LaunchedEffect(state) {
        if (state is BackupRestoreUiState.Idle) {
            val msg = (state as BackupRestoreUiState.Idle).message
            if (msg != null) {
                snack.showSnackbar(msg)
                viewModel.clearMessage()
            }
        }
    }

    ScreenScaffold(
        title = stringResource(R.string.backup_title),
        onBack = onBack,
        snack = snack,
        palette = groupPalette(Group.SYSTEM),
        icon = pageIcon("backup"),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Hero Card: Manual Backup Trigger
            item(key = "backup_hero") {
                BackupHeroCard(
                    isDownloading = state is BackupRestoreUiState.Downloading,
                    onCreateBackup = viewModel::createBackup,
                )
            }

            // Header for history
            item(key = "history_header") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.backup_history_title, state.backups.size, 5),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            if (state.backups.isEmpty()) {
                item(key = "empty_history") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                Icons.Filled.Restore,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            )
                            Text(
                                text = stringResource(R.string.backup_history_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else {
                items(state.backups, key = { it.id }) { item ->
                    BackupItemCard(
                        item = item,
                        onRestore = { restoreTarget = item },
                        onShare = { shareBackup(ctx, store, item) },
                        onDelete = { deleteTarget = item },
                    )
                }
            }
        }
    }

    // High-risk restore confirmation dialog
    restoreTarget?.let { item ->
        AlertDialog(
            onDismissRequest = { restoreTarget = null },
            icon = { Icon(Icons.Filled.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(stringResource(R.string.backup_restore_confirm_title)) },
            text = { Text(stringResource(R.string.backup_restore_confirm_msg)) },
            shape = RoundedCornerShape(28.dp),
            confirmButton = {
                Button(
                    onClick = {
                        val target = restoreTarget
                        restoreTarget = null
                        if (target != null) {
                            viewModel.restore(target)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text(stringResource(R.string.continue_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { restoreTarget = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    // Delete confirmation dialog
    deleteTarget?.let { item ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            icon = { Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(stringResource(R.string.backup_delete_confirm_title)) },
            text = { Text(stringResource(R.string.backup_delete_confirm_msg, item.displayName)) },
            shape = RoundedCornerShape(28.dp),
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteBackup(item)
                        deleteTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    // Reboot countdown overlay dialog
    val rebootState = state as? BackupRestoreUiState.Rebooting
    if (rebootState != null) {
        RebootCountdownDialog(
            state = rebootState,
            onDismiss = {
                viewModel.dismissReboot()
                if (rebootState.isReconnected) {
                    onNavigateToLogin()
                }
            },
        )
    }
}

@Composable
private fun BackupHeroCard(
    isDownloading: Boolean,
    onCreateBackup: () -> Unit,
) {
    val c = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = c.surfaceContainerHighest),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ShapeBadge(
                    icon = Icons.Filled.Restore,
                    container = c.primaryContainer,
                    content = c.onPrimaryContainer,
                    size = 56.dp,
                    shapeIndex = 1,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.backup_hero_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.backup_hero_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.onSurfaceVariant,
                    )
                }
            }

            Button(
                onClick = onCreateBackup,
                enabled = !isDownloading,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = c.primary),
                contentPadding = PaddingValues(14.dp),
            ) {
                if (isDownloading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = c.onPrimary,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(R.string.backup_creating))
                } else {
                    Icon(Icons.Filled.Restore, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.backup_btn_create), fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun BackupItemCard(
    item: BackupItem,
    onRestore: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
) {
    val c = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = c.surfaceContainerHigh),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                ShapeBadge(
                    icon = Icons.Filled.Restore,
                    container = c.secondaryContainer,
                    content = c.onSecondaryContainer,
                    size = 44.dp,
                    shapeIndex = 0,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    val sizeKb = (item.sizeBytes / 1024).coerceAtLeast(1)
                    val subtitle = if (item.deviceModel.isNotBlank()) {
                        "${item.deviceModel} · $sizeKb KB"
                    } else {
                        "$sizeKb KB"
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = c.onSurfaceVariant,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledTonalButton(
                    onClick = onRestore,
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Icon(Icons.Filled.Restore, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.backup_action_restore))
                }

                Spacer(Modifier.width(8.dp))

                IconButton(
                    onClick = onShare,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(),
                ) {
                    Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.backup_action_share), Modifier.size(18.dp))
                }

                Spacer(Modifier.width(4.dp))

                IconButton(
                    onClick = onDelete,
                    colors = IconButtonDefaults.iconButtonColors(contentColor = c.error),
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.backup_action_delete), Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun RebootCountdownDialog(
    state: BackupRestoreUiState.Rebooting,
    onDismiss: () -> Unit,
) {
    val c = MaterialTheme.colorScheme
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = c.surfaceContainerHighest),
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (state.isReconnected) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = c.primary,
                    )
                    Text(
                        text = stringResource(R.string.backup_reboot_reconnected),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                    )
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                    ) {
                        Text(stringResource(R.string.backup_reboot_close))
                    }
                } else {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(100.dp)) {
                        val progress = if (state.totalSeconds > 0) {
                            state.secondsRemaining.toFloat() / state.totalSeconds.toFloat()
                        } else 0f

                        CircularProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.size(90.dp),
                            strokeWidth = 6.dp,
                            color = c.primary,
                            trackColor = c.surfaceContainerHigh,
                        )
                        Text(
                            text = "${state.secondsRemaining}s",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    Text(
                        text = stringResource(R.string.backup_reboot_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )

                    Text(
                        text = if (state.secondsRemaining > 0) {
                            stringResource(R.string.backup_reboot_desc)
                        } else {
                            stringResource(R.string.backup_reboot_timeout)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )

                    if (state.secondsRemaining == 0) {
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                        ) {
                            Text(stringResource(R.string.backup_reboot_close))
                        }
                    }
                }
            }
        }
    }
}

private fun shareBackup(context: Context, store: BackupStore, item: BackupItem) {
    val uri = store.getShareableUri(item, context) ?: return
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/octet-stream"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, item.displayName)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.backup_action_share)))
}
