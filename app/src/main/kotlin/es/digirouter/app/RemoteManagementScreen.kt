package es.digirouter.app

import android.security.KeyChain
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import es.routerapp.protocol.management.HttpConfig
import es.routerapp.protocol.management.IspManagementAudit
import es.routerapp.protocol.management.SshConfig
import es.routerapp.protocol.management.TelnetConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteManagementScreen(
    viewModel: RemoteManagementViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val certEvent by viewModel.installCertEvent.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var selectedTab by remember { mutableIntStateOf(0) }

    val installCertLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.clearCertEvent()
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    LaunchedEffect(certEvent) {
        certEvent?.let { bytes ->
            val intent = KeyChain.createInstallIntent().apply {
                putExtra(KeyChain.EXTRA_CERTIFICATE, bytes)
                putExtra(KeyChain.EXTRA_NAME, "Router-CA")
            }
            installCertLauncher.launch(intent)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.remote_mgmt_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadAll() }, enabled = !state.isLoading) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.refresh))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                        ) {
                            Text(stringResource(R.string.remote_mgmt_tab_daemons))
                        }
                        SegmentedButton(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                        ) {
                            Text(stringResource(R.string.remote_mgmt_tab_web))
                        }
                        SegmentedButton(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                        ) {
                            Text(stringResource(R.string.remote_mgmt_tab_isp))
                        }
                    }
                }

                when (selectedTab) {
                    0 -> {
                        item {
                            DaemonsSection(
                                ssh = state.overview.ssh,
                                telnet = state.overview.telnet,
                                isSaving = state.isSaving,
                                onSaveSsh = viewModel::updateSsh,
                                onSaveTelnet = viewModel::updateTelnet,
                            )
                        }
                    }
                    1 -> {
                        item {
                            WebAndSslSection(
                                http = state.overview.http,
                                isSaving = state.isSaving,
                                isDownloadingCert = state.isDownloadingCert,
                                onSaveHttp = viewModel::updateHttp,
                                onInstallCert = viewModel::downloadAndInstallCert,
                            )
                        }
                    }
                    2 -> {
                        item {
                            IspAuditSection(
                                audit = state.overview.ispAudit,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DaemonsSection(
    ssh: SshConfig,
    telnet: TelnetConfig,
    isSaving: Boolean,
    onSaveSsh: (SshConfig) -> Unit,
    onSaveTelnet: (TelnetConfig) -> Unit,
) {
    var sshLocalEnabled by remember(ssh) { mutableStateOf(ssh.localEnabled) }
    var sshLocalPort by remember(ssh) { mutableStateOf(ssh.localPort.toString()) }
    var sshRemoteEnabled by remember(ssh) { mutableStateOf(ssh.remoteEnabled) }
    var sshRemotePort by remember(ssh) { mutableStateOf(ssh.remotePort.toString()) }
    var sshLifetime by remember(ssh) { mutableStateOf(ssh.sessionLifeTime.toString()) }

    var telnetLocalEnabled by remember(telnet) { mutableStateOf(telnet.localEnabled) }
    var telnetLocalPort by remember(telnet) { mutableStateOf(telnet.localPort.toString()) }
    var telnetRemoteEnabled by remember(telnet) { mutableStateOf(telnet.remoteEnabled) }
    var telnetRemotePort by remember(telnet) { mutableStateOf(telnet.remotePort.toString()) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // SSH Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        stringResource(R.string.remote_mgmt_ssh_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.remote_mgmt_ssh_local), style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = sshLocalEnabled, onCheckedChange = { sshLocalEnabled = it })
                }

                OutlinedTextField(
                    value = sshLocalPort,
                    onValueChange = { sshLocalPort = it.filter { ch -> ch.isDigit() } },
                    label = { Text(stringResource(R.string.remote_mgmt_ssh_local_port)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = sshLocalEnabled,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.remote_mgmt_ssh_remote), style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = sshRemoteEnabled, onCheckedChange = { sshRemoteEnabled = it })
                }

                OutlinedTextField(
                    value = sshRemotePort,
                    onValueChange = { sshRemotePort = it.filter { ch -> ch.isDigit() } },
                    label = { Text(stringResource(R.string.remote_mgmt_ssh_remote_port)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = sshRemoteEnabled,
                )

                OutlinedTextField(
                    value = sshLifetime,
                    onValueChange = { sshLifetime = it.filter { ch -> ch.isDigit() } },
                    label = { Text(stringResource(R.string.remote_mgmt_ssh_lifetime)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )

                AnimatedVisibility(visible = sshRemoteEnabled && (ssh.remoteAll || ssh.remoteHost.isEmpty())) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.remote_mgmt_risk_wan_warning),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        val lp = sshLocalPort.toIntOrNull() ?: 22
                        val rp = sshRemotePort.toIntOrNull() ?: 22
                        val lt = sshLifetime.toIntOrNull() ?: 10
                        onSaveSsh(
                            ssh.copy(
                                localEnabled = sshLocalEnabled,
                                localPort = lp,
                                remoteEnabled = sshRemoteEnabled,
                                remotePort = rp,
                                sessionLifeTime = lt,
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving,
                ) {
                    Text(stringResource(R.string.remote_mgmt_save_daemons))
                }
            }
        }

        // Telnet Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        stringResource(R.string.remote_mgmt_telnet_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.remote_mgmt_telnet_local), style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = telnetLocalEnabled, onCheckedChange = { telnetLocalEnabled = it })
                }

                OutlinedTextField(
                    value = telnetLocalPort,
                    onValueChange = { telnetLocalPort = it.filter { ch -> ch.isDigit() } },
                    label = { Text(stringResource(R.string.remote_mgmt_telnet_local_port)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = telnetLocalEnabled,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.remote_mgmt_telnet_remote), style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = telnetRemoteEnabled, onCheckedChange = { telnetRemoteEnabled = it })
                }

                OutlinedTextField(
                    value = telnetRemotePort,
                    onValueChange = { telnetRemotePort = it.filter { ch -> ch.isDigit() } },
                    label = { Text(stringResource(R.string.remote_mgmt_telnet_remote_port)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = telnetRemoteEnabled,
                )

                AnimatedVisibility(visible = telnetRemoteEnabled) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.remote_mgmt_risk_wan_warning),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        val lp = telnetLocalPort.toIntOrNull() ?: 23
                        val rp = telnetRemotePort.toIntOrNull() ?: 23
                        onSaveTelnet(
                            telnet.copy(
                                localEnabled = telnetLocalEnabled,
                                localPort = lp,
                                remoteEnabled = telnetRemoteEnabled,
                                remotePort = rp,
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving,
                ) {
                    Text(stringResource(R.string.remote_mgmt_save_daemons))
                }
            }
        }
    }
}

@Composable
private fun WebAndSslSection(
    http: HttpConfig,
    isSaving: Boolean,
    isDownloadingCert: Boolean,
    onSaveHttp: (HttpConfig) -> Unit,
    onInstallCert: () -> Unit,
) {
    var httpPort by remember(http) { mutableStateOf(http.httpLocalPort.toString()) }
    var httpsPort by remember(http) { mutableStateOf(http.httpsLocalPort.toString()) }
    var remoteHttpPort by remember(http) { mutableStateOf(http.httpRemotePort.toString()) }
    var remoteHttpsPort by remember(http) { mutableStateOf(http.httpsRemotePort.toString()) }
    var sessionTimeout by remember(http) { mutableStateOf(http.webSessionTimeout.toString()) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Ports Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        stringResource(R.string.remote_mgmt_web_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = httpPort,
                        onValueChange = { httpPort = it.filter { ch -> ch.isDigit() } },
                        label = { Text(stringResource(R.string.remote_mgmt_http_local)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = httpsPort,
                        onValueChange = { httpsPort = it.filter { ch -> ch.isDigit() } },
                        label = { Text(stringResource(R.string.remote_mgmt_https_local)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = remoteHttpPort,
                        onValueChange = { remoteHttpPort = it.filter { ch -> ch.isDigit() } },
                        label = { Text(stringResource(R.string.remote_mgmt_http_remote)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = remoteHttpsPort,
                        onValueChange = { remoteHttpsPort = it.filter { ch -> ch.isDigit() } },
                        label = { Text(stringResource(R.string.remote_mgmt_https_remote)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                }

                OutlinedTextField(
                    value = sessionTimeout,
                    onValueChange = { sessionTimeout = it.filter { ch -> ch.isDigit() } },
                    label = { Text(stringResource(R.string.remote_mgmt_session_timeout)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )

                Button(
                    onClick = {
                        val lpHttp = httpPort.toIntOrNull() ?: 80
                        val lpHttps = httpsPort.toIntOrNull() ?: 443
                        val rpHttp = remoteHttpPort.toIntOrNull() ?: 80
                        val rpHttps = remoteHttpsPort.toIntOrNull() ?: 443
                        val st = sessionTimeout.toIntOrNull() ?: 10
                        onSaveHttp(
                            http.copy(
                                httpLocalPort = lpHttp,
                                httpsLocalPort = lpHttps,
                                httpRemotePort = rpHttp,
                                httpsRemotePort = rpHttps,
                                webSessionTimeout = st,
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving,
                ) {
                    Text(stringResource(R.string.remote_mgmt_save_web))
                }
            }
        }

        // SSL Certificate Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        stringResource(R.string.remote_mgmt_ssl_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Text(
                    stringResource(R.string.remote_mgmt_ssl_install_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (http.certSerialNumber.isNotEmpty()) {
                    Text(
                        stringResource(R.string.remote_mgmt_ssl_serial, http.certSerialNumber),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                    )
                }

                Button(
                    onClick = onInstallCert,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isDownloadingCert,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                ) {
                    if (isDownloadingCert) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onTertiary)
                    } else {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.remote_mgmt_ssl_install_btn))
                    }
                }
            }
        }
    }
}

@Composable
private fun IspAuditSection(
    audit: IspManagementAudit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Explanatory Banner
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    stringResource(R.string.remote_mgmt_isp_readonly_note),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        // TR-069 Details Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(R.string.remote_mgmt_isp_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )

                Text(
                    stringResource(R.string.remote_mgmt_isp_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AuditItem(label = stringResource(R.string.remote_mgmt_isp_acs_url), value = audit.acsUrl.ifEmpty { "Not configured" })
                    AuditItem(label = stringResource(R.string.remote_mgmt_isp_bound_if), value = audit.boundInterface)
                    AuditItem(
                        label = stringResource(R.string.remote_mgmt_isp_conn_port),
                        value = audit.connReqPort.toString()
                    )
                    AuditItem(
                        label = "Periodic Inform",
                        value = stringResource(R.string.remote_mgmt_isp_inform_interval, audit.informInterval)
                    )
                    if (audit.operatorSubnets.isNotEmpty()) {
                        AuditItem(
                            label = stringResource(R.string.remote_mgmt_isp_subnets),
                            value = audit.operatorSubnets.joinToString("\n")
                        )
                    }
                }
            }
        }

        // TR-369 Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.remote_mgmt_isp_tr369_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    stringResource(R.string.remote_mgmt_isp_tr369_inactive),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

@Composable
private fun AuditItem(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}
