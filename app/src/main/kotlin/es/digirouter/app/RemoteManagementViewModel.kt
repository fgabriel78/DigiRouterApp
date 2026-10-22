package es.digirouter.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.routerapp.protocol.RouterClient
import es.routerapp.protocol.management.HttpConfig
import es.routerapp.protocol.management.RemoteManagementEngine
import es.routerapp.protocol.management.RemoteManagementOverview
import es.routerapp.protocol.management.SshConfig
import es.routerapp.protocol.management.TelnetConfig
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class RemoteManagementUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isDownloadingCert: Boolean = false,
    val overview: RemoteManagementOverview = RemoteManagementOverview(),
    val message: String? = null,
    val isError: Boolean = false,
)

class RemoteManagementViewModel(
    private val clientProvider: () -> RouterClient?,
    private val recoveryRunner: RecoveryRunner = RecoveryRunner.NoOp,
    coroutineScope: CoroutineScope? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val engineProvider: (RouterClient) -> RemoteManagementEngine = { RemoteManagementEngine(it) },
) : ViewModel() {

    private val effectiveScope = coroutineScope ?: viewModelScope

    private val _state = MutableStateFlow(RemoteManagementUiState(isLoading = true))
    val state: StateFlow<RemoteManagementUiState> = _state.asStateFlow()

    private val _installCertEvent = MutableStateFlow<ByteArray?>(null)
    val installCertEvent: StateFlow<ByteArray?> = _installCertEvent.asStateFlow()

    init {
        loadAll()
    }

    fun loadAll() {
        effectiveScope.launch {
            _state.update { it.copy(isLoading = true, message = null) }
            try {
                recoveryRunner.run {
                    withContext(ioDispatcher) {
                        val client = clientProvider() ?: throw IllegalStateException("Not connected")
                        val engine = engineProvider(client)
                        val overview = engine.loadOverview().getOrThrow()
                        _state.update {
                            it.copy(
                                isLoading = false,
                                overview = overview,
                                isError = false,
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        isError = true,
                        message = e.message ?: "Failed to load management overview",
                    )
                }
            }
        }
    }

    fun updateSsh(config: SshConfig) {
        if (!validatePort(config.localPort) || !validatePort(config.remotePort)) {
            _state.update { it.copy(isError = true, message = "Invalid SSH port (must be between 1 and 65535)") }
            return
        }
        effectiveScope.launch {
            _state.update { it.copy(isSaving = true, message = null) }
            try {
                recoveryRunner.run {
                    withContext(ioDispatcher) {
                        val client = clientProvider() ?: throw IllegalStateException("Not connected")
                        val engine = engineProvider(client)
                        engine.saveSshConfig(config).getOrThrow()
                        _state.update {
                            it.copy(
                                isSaving = false,
                                overview = it.overview.copy(ssh = config),
                                message = "SSH configuration saved",
                                isError = false,
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isSaving = false,
                        isError = true,
                        message = e.message ?: "Failed to save SSH configuration",
                    )
                }
            }
        }
    }

    fun updateTelnet(config: TelnetConfig) {
        if (!validatePort(config.localPort) || !validatePort(config.remotePort)) {
            _state.update { it.copy(isError = true, message = "Invalid Telnet port (must be between 1 and 65535)") }
            return
        }
        effectiveScope.launch {
            _state.update { it.copy(isSaving = true, message = null) }
            try {
                recoveryRunner.run {
                    withContext(ioDispatcher) {
                        val client = clientProvider() ?: throw IllegalStateException("Not connected")
                        val engine = engineProvider(client)
                        engine.saveTelnetConfig(config).getOrThrow()
                        _state.update {
                            it.copy(
                                isSaving = false,
                                overview = it.overview.copy(telnet = config),
                                message = "Telnet configuration saved",
                                isError = false,
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isSaving = false,
                        isError = true,
                        message = e.message ?: "Failed to save Telnet configuration",
                    )
                }
            }
        }
    }

    fun updateHttp(config: HttpConfig) {
        if (!validatePort(config.httpLocalPort) || !validatePort(config.httpsLocalPort) ||
            !validatePort(config.httpRemotePort) || !validatePort(config.httpsRemotePort)
        ) {
            _state.update { it.copy(isError = true, message = "Invalid Web port (must be between 1 and 65535)") }
            return
        }
        effectiveScope.launch {
            _state.update { it.copy(isSaving = true, message = null) }
            try {
                recoveryRunner.run {
                    withContext(ioDispatcher) {
                        val client = clientProvider() ?: throw IllegalStateException("Not connected")
                        val engine = engineProvider(client)
                        engine.saveHttpConfig(config).getOrThrow()
                        _state.update {
                            it.copy(
                                isSaving = false,
                                overview = it.overview.copy(http = config),
                                message = "Web management configuration saved",
                                isError = false,
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isSaving = false,
                        isError = true,
                        message = e.message ?: "Failed to save Web configuration",
                    )
                }
            }
        }
    }

    fun downloadAndInstallCert() {
        effectiveScope.launch {
            _state.update { it.copy(isDownloadingCert = true, message = null) }
            try {
                recoveryRunner.run {
                    withContext(ioDispatcher) {
                        val client = clientProvider() ?: throw IllegalStateException("Not connected")
                        val engine = engineProvider(client)
                        val bytes = engine.downloadCaCert().getOrThrow()
                        if (bytes.isEmpty()) throw IllegalStateException("Received empty CA certificate")
                        _state.update { it.copy(isDownloadingCert = false, isError = false) }
                        _installCertEvent.value = bytes
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isDownloadingCert = false,
                        isError = true,
                        message = e.message ?: "Failed to download CA certificate",
                    )
                }
            }
        }
    }

    fun clearCertEvent() {
        _installCertEvent.value = null
    }

    fun clearMessage() {
        _state.update { it.copy(message = null, isError = false) }
    }

    private fun validatePort(port: Int): Boolean = port in 1..65535
}
