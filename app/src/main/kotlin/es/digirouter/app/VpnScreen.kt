package es.digirouter.app

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.routerapp.protocol.vpn.OpenVpnAccessMode
import es.routerapp.protocol.vpn.OpenVpnConfig
import es.routerapp.protocol.vpn.VpnClientSession
import es.routerapp.protocol.vpn.VpnProtocol

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VpnScreen(
    viewModel: VpnViewModel,
    onBack: () -> Unit,
    onNavigateToDdns: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var selectedTab by remember { mutableIntStateOf(0) }
    var showRegenConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissMessage()
        }
    }

    val createDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/x-openvpn")
    ) { uri ->
        if (uri != null) {
            val content = viewModel.getExportedProfileText().orEmpty()
            runCatching {
                val stream = context.contentResolver.openOutputStream(uri)
                stream?.use { os ->
                    os.write(content.toByteArray(Charsets.UTF_8))
                }
            }
            viewModel.dismissExportSheet()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.vpn_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::loadAll, enabled = !state.isLoading) {
                        Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.refresh))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Readiness Status Banner
            VpnReadinessBanner(
                readiness = state.readiness,
                onConfigureDdns = onNavigateToDdns,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Segmented Navigation Bar
            val clientsLabel = if (state.clientSessions.isNotEmpty()) {
                stringResource(R.string.vpn_tab_clients_count, state.clientSessions.size)
            } else {
                stringResource(R.string.vpn_tab_clients)
            }
            val tabOptions = listOf(
                stringResource(R.string.vpn_tab_openvpn),
                clientsLabel,
                stringResource(R.string.vpn_tab_ipsec_pptp)
            )

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                tabOptions.forEachIndexed { index, label ->
                    SegmentedButton(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = tabOptions.size)
                    ) {
                        Text(label, maxLines = 1)
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when (selectedTab) {
                    0 -> OpenVpnTabContent(
                        config = state.openVpnConfig,
                        isLoading = state.isLoading,
                        isDownloading = state.isDownloadingProfile,
                        onSave = viewModel::saveOpenVpnConfig,
                        onExport = viewModel::prepareProfileExport,
                        onRegenPrompt = { showRegenConfirm = true },
                    )
                    1 -> ConnectedClientsTabContent(
                        clients = state.clientSessions,
                    )
                    2 -> IpsecPptpTabContent(
                        state = state,
                    )
                }

                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }

    // Confirmation Dialog for Key Regeneration
    if (showRegenConfirm) {
        AlertDialog(
            onDismissRequest = { showRegenConfirm = false },
            icon = { Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(stringResource(R.string.vpn_regen_dialog_title)) },
            text = { Text(stringResource(R.string.vpn_regen_dialog_desc)) },
            confirmButton = {
                Button(
                    onClick = {
                        showRegenConfirm = false
                        viewModel.regenerateOpenVpnKeys()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.vpn_regen_keys_btn))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRegenConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Progress Dialog during RSA Key Regeneration
    if (state.isRegeneratingKeys) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.vpn_regen_keys_btn)) },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Text(stringResource(R.string.vpn_regen_progress))
                }
            },
            confirmButton = {}
        )
    }

    // Export Modal Bottom Sheet
    if (state.exportedProfileRaw != null) {
        val sheetState = rememberBottomSheetState(SheetValue.Expanded)
        ModalBottomSheet(
            onDismissRequest = viewModel::dismissExportSheet,
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            ExportProfileSheetContent(
                detectedHost = state.detectedRemoteHost,
                hostOverride = state.hostOverride,
                onHostOverrideChange = viewModel::updateHostOverride,
                onShare = {
                    val uri = viewModel.exportProfileToUri(context)
                    if (uri != null) {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/x-openvpn"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.vpn_action_share)))
                    }
                    viewModel.dismissExportSheet()
                },
                onSave = {
                    createDocLauncher.launch("client.ovpn")
                }
            )
        }
    }
}

@Composable
private fun VpnReadinessBanner(
    readiness: VpnReadinessStatus,
    onConfigureDdns: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = MaterialTheme.colorScheme
    when (readiness.level) {
        VpnReadinessLevel.WARNING_CGNAT_NO_DDNS -> {
            Card(
                modifier = modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = c.errorContainer, contentColor = c.onErrorContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = c.error)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.vpn_banner_cgnat_title), fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.vpn_banner_cgnat_desc, readiness.wanIp))
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = onConfigureDdns) {
                        Text(stringResource(R.string.vpn_action_configure_ddns))
                    }
                }
            }
        }
        VpnReadinessLevel.WARNING_PRIVATE_IP_NO_DDNS -> {
            Card(
                modifier = modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = c.tertiaryContainer, contentColor = c.onTertiaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Info, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.vpn_banner_private_title), fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.vpn_banner_private_desc, readiness.wanIp))
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = onConfigureDdns) {
                        Text(stringResource(R.string.vpn_action_configure_ddns))
                    }
                }
            }
        }
        VpnReadinessLevel.READY_DDNS -> {
            Card(
                modifier = modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = c.surfaceContainerHighest, contentColor = c.onSurface)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Cloud, contentDescription = null, tint = c.primary)
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.vpn_banner_ddns_active, readiness.ddnsDomain.orEmpty()))
                }
            }
        }
        VpnReadinessLevel.READY_PUBLIC_IP -> {
            Card(
                modifier = modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = c.surfaceContainerHighest, contentColor = c.onSurface)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Public, contentDescription = null, tint = c.primary)
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.vpn_banner_public_ip, readiness.wanIp))
                }
            }
        }
        VpnReadinessLevel.UNKNOWN -> Unit
    }
}

@Composable
private fun OpenVpnTabContent(
    config: OpenVpnConfig,
    isLoading: Boolean,
    isDownloading: Boolean,
    onSave: (OpenVpnConfig) -> Unit,
    onExport: () -> Unit,
    onRegenPrompt: () -> Unit,
) {
    var enabled by remember(config) { mutableStateOf(config.enable) }
    var protocol by remember(config) { mutableStateOf(config.protocol) }
    var portText by remember(config) { mutableStateOf(config.port.toString()) }
    var subnetText by remember(config) { mutableStateOf(config.subnet) }
    var netmaskText by remember(config) { mutableStateOf(config.netmask) }
    var accessMode by remember(config) { mutableStateOf(config.accessMode) }

    val hasChanges = enabled != config.enable ||
            protocol != config.protocol ||
            portText != config.port.toString() ||
            subnetText != config.subnet ||
            netmaskText != config.netmask ||
            accessMode != config.accessMode

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Main Server Parameters Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(stringResource(R.string.vpn_openvpn_server_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.vpn_openvpn_enable), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = enabled, onCheckedChange = { enabled = it })
                    }

                    Spacer(Modifier.height(16.dp))

                    // Protocol Selection
                    Text(stringResource(R.string.vpn_protocol), style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(4.dp))
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = protocol == VpnProtocol.UDP,
                            onClick = { protocol = VpnProtocol.UDP },
                            shape = SegmentedButtonDefaults.itemShape(0, 2)
                        ) {
                            Text(stringResource(R.string.vpn_proto_udp))
                        }
                        SegmentedButton(
                            selected = protocol == VpnProtocol.TCP,
                            onClick = { protocol = VpnProtocol.TCP },
                            shape = SegmentedButtonDefaults.itemShape(1, 2)
                        ) {
                            Text(stringResource(R.string.vpn_proto_tcp))
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = portText,
                        onValueChange = { portText = it },
                        label = { Text(stringResource(R.string.vpn_port)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = subnetText,
                            onValueChange = { subnetText = it },
                            label = { Text(stringResource(R.string.vpn_subnet)) },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = netmaskText,
                            onValueChange = { netmaskText = it },
                            label = { Text(stringResource(R.string.vpn_netmask)) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    // Client Access Mode
                    Text(stringResource(R.string.vpn_access_mode), style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(4.dp))
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = accessMode == OpenVpnAccessMode.INTERNET_AND_HOME_NETWORK,
                            onClick = { accessMode = OpenVpnAccessMode.INTERNET_AND_HOME_NETWORK },
                            shape = SegmentedButtonDefaults.itemShape(0, 2)
                        ) {
                            Text(stringResource(R.string.vpn_access_all), maxLines = 1)
                        }
                        SegmentedButton(
                            selected = accessMode == OpenVpnAccessMode.HOME_NETWORK_ONLY,
                            onClick = { accessMode = OpenVpnAccessMode.HOME_NETWORK_ONLY },
                            shape = SegmentedButtonDefaults.itemShape(1, 2)
                        ) {
                            Text(stringResource(R.string.vpn_access_home_only), maxLines = 1)
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val parsedPort = portText.toIntOrNull() ?: 1194
                            val newConfig = config.copy(
                                enable = enabled,
                                protocol = protocol,
                                port = parsedPort,
                                subnet = subnetText.trim(),
                                netmask = netmaskText.trim(),
                                accessMode = accessMode,
                            )
                            onSave(newConfig)
                        },
                        enabled = hasChanges && !isLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Save, contentDescription = null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.vpn_save_settings))
                    }
                }
            }
        }

        // Actions Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onExport,
                        enabled = !isDownloading && !isLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.vpn_export_profile_btn))
                    }

                    OutlinedButton(
                        onClick = onRegenPrompt,
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Key, contentDescription = null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.vpn_regen_keys_btn))
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectedClientsTabContent(clients: List<VpnClientSession>) {
    if (clients.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Card(
                modifier = Modifier.padding(24.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Filled.Devices, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.vpn_clients_empty), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(clients, key = { it.id }) { client ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(client.clientName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                            Text(stringResource(R.string.vpn_client_virtual_ip, client.virtualIp), style = MaterialTheme.typography.bodyMedium)
                            if (client.remoteIp.isNotEmpty()) {
                                Text(stringResource(R.string.vpn_client_remote_ip, client.remoteIp), style = MaterialTheme.typography.bodySmall)
                            }
                            if (client.connectedSince.isNotEmpty()) {
                                Text(stringResource(R.string.vpn_client_uptime, client.connectedSince), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IpsecPptpTabContent(state: VpnUiState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // IPSec Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.vpn_ipsec_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    if (state.ipsecConfig.tunnels.isEmpty()) {
                        Text(stringResource(R.string.vpn_ipsec_empty), style = MaterialTheme.typography.bodyMedium)
                    } else {
                        state.ipsecConfig.tunnels.forEach { tunnel ->
                            Text("${tunnel.name} (${tunnel.remoteGateway})", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

        // PPTP Legacy Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.vpn_pptp_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.vpn_pptp_warning), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.vpn_pptp_enable))
                        Switch(checked = state.pptpConfig.enable, onCheckedChange = {})
                    }
                }
            }
        }
    }
}

@Composable
private fun ExportProfileSheetContent(
    detectedHost: String?,
    hostOverride: String,
    onHostOverrideChange: (String) -> Unit,
    onShare: () -> Unit,
    onSave: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Text(stringResource(R.string.vpn_export_sheet_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        if (!detectedHost.isNullOrEmpty()) {
            Text(stringResource(R.string.vpn_export_detected_host, detectedHost), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
        }

        OutlinedTextField(
            value = hostOverride,
            onValueChange = onHostOverrideChange,
            label = { Text(stringResource(R.string.vpn_export_override_label)) },
            supportingText = { Text(stringResource(R.string.vpn_export_override_help)) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(20.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onShare,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.Share, contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.vpn_action_share))
            }

            OutlinedButton(
                onClick = onSave,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.Download, contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.vpn_action_save))
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}
