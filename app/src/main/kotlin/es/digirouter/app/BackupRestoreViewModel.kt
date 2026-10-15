package es.digirouter.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.routerapp.protocol.RouterClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

sealed interface BackupRestoreUiState {
    val backups: List<BackupItem>

    data class Idle(
        override val backups: List<BackupItem> = emptyList(),
        val message: String? = null,
        val isError: Boolean = false,
    ) : BackupRestoreUiState

    data class Downloading(
        override val backups: List<BackupItem> = emptyList(),
    ) : BackupRestoreUiState

    data class Restoring(
        val item: BackupItem,
        override val backups: List<BackupItem> = emptyList(),
    ) : BackupRestoreUiState

    data class Rebooting(
        val totalSeconds: Int = 90,
        val secondsRemaining: Int = 90,
        val isReconnected: Boolean = false,
        override val backups: List<BackupItem> = emptyList(),
    ) : BackupRestoreUiState
}

class BackupRestoreViewModel(
    private val clientProvider: () -> RouterClient?,
    private val store: BackupStore,
    private val recoveryRunner: RecoveryRunner = RecoveryRunner.NoOp,
    coroutineScope: CoroutineScope? = null,
    private val rebootCountdownSeconds: Int = 90,
) : ViewModel() {

    private val effectiveScope = coroutineScope ?: viewModelScope

    private val _state = MutableStateFlow<BackupRestoreUiState>(BackupRestoreUiState.Idle())
    val state: StateFlow<BackupRestoreUiState> = _state.asStateFlow()

    init {
        loadBackups()
    }

    fun loadBackups() {
        val list = store.listBackups()
        _state.update { current ->
            when (current) {
                is BackupRestoreUiState.Idle -> current.copy(backups = list)
                is BackupRestoreUiState.Downloading -> current.copy(backups = list)
                is BackupRestoreUiState.Restoring -> current.copy(backups = list)
                is BackupRestoreUiState.Rebooting -> current.copy(backups = list)
            }
        }
    }

    fun createBackup() {
        _state.update { BackupRestoreUiState.Downloading(backups = it.backups) }
        effectiveScope.launch {
            val client = clientProvider()
            if (client == null || !client.isLoggedIn) {
                _state.value = BackupRestoreUiState.Idle(
                    backups = store.listBackups(),
                    message = "Router session is not active",
                    isError = true,
                )
                return@launch
            }

            val result = withContext(Dispatchers.IO) {
                runCatching {
                    recoveryRunner.run {
                        val info = runCatching {
                            val data = client.get("DEV2_DEV_INFO")
                            (data as? JsonObject)?.mapValues { (_, v) -> (v as? JsonPrimitive)?.content ?: v.toString() }
                        }.getOrNull() ?: emptyMap()
                        val model = info["modelName"] ?: ""
                        val fw = info["softwareVersion"] ?: info["X_TP_SoftwareRevision"] ?: ""

                        val bytes = client.downloadBackup()
                        store.saveBackup(bytes, deviceModel = model, firmwareVersion = fw)
                    }
                }
            }

            if (result.isSuccess) {
                _state.value = BackupRestoreUiState.Idle(
                    backups = store.listBackups(),
                    message = "Backup created successfully",
                    isError = false,
                )
            } else {
                val err = result.exceptionOrNull()?.message ?: "Failed to download backup"
                _state.value = BackupRestoreUiState.Idle(
                    backups = store.listBackups(),
                    message = err,
                    isError = true,
                )
            }
        }
    }

    fun restore(item: BackupItem) {
        val file = store.getBackupFile(item)
        if (file == null || !file.exists()) {
            _state.value = BackupRestoreUiState.Idle(
                backups = store.listBackups(),
                message = "Backup file not found",
                isError = true,
            )
            return
        }

        val data = file.readBytes()
        _state.value = BackupRestoreUiState.Restoring(item = item, backups = store.listBackups())

        effectiveScope.launch {
            val client = clientProvider()
            if (client == null) {
                _state.value = BackupRestoreUiState.Idle(
                    backups = store.listBackups(),
                    message = "Router client is not available",
                    isError = true,
                )
                return@launch
            }

            val uploadResult = withContext(Dispatchers.IO) {
                runCatching {
                    client.restoreBackup(data)
                }
            }

            if (uploadResult.isFailure) {
                val err = uploadResult.exceptionOrNull()?.message ?: "Upload failed"
                _state.value = BackupRestoreUiState.Idle(
                    backups = store.listBackups(),
                    message = err,
                    isError = true,
                )
                return@launch
            }

            startRebootCountdown()
        }
    }

    private suspend fun startRebootCountdown() {
        for (sec in rebootCountdownSeconds downTo 0) {
            _state.value = BackupRestoreUiState.Rebooting(
                totalSeconds = rebootCountdownSeconds,
                secondsRemaining = sec,
                isReconnected = false,
                backups = store.listBackups(),
            )

            if (sec in 1..(rebootCountdownSeconds - 15) && sec % 3 == 0) {
                val reconnected = withContext(Dispatchers.IO) {
                    runCatching {
                        clientProvider()?.busy() != null
                    }.getOrDefault(false)
                }
                if (reconnected) {
                    _state.value = BackupRestoreUiState.Rebooting(
                        totalSeconds = rebootCountdownSeconds,
                        secondsRemaining = sec,
                        isReconnected = true,
                        backups = store.listBackups(),
                    )
                    return
                }
            }

            delay(1000)
        }

        _state.value = BackupRestoreUiState.Rebooting(
            totalSeconds = rebootCountdownSeconds,
            secondsRemaining = 0,
            isReconnected = false,
            backups = store.listBackups(),
        )
    }

    fun dismissReboot() {
        _state.value = BackupRestoreUiState.Idle(backups = store.listBackups())
    }

    fun deleteBackup(item: BackupItem) {
        store.deleteBackup(item)
        _state.value = BackupRestoreUiState.Idle(backups = store.listBackups())
    }

    fun clearMessage() {
        _state.update { current ->
            if (current is BackupRestoreUiState.Idle) current.copy(message = null) else current
        }
    }
}
