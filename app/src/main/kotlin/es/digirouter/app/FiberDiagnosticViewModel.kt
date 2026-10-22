package es.digirouter.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.routerapp.protocol.RouterClient
import es.routerapp.protocol.optical.DiagnosticSnapshot
import es.routerapp.protocol.optical.DiagnosticStepId
import es.routerapp.protocol.optical.FiberDiagnosticEngine
import es.routerapp.protocol.optical.OmciRestartProgress
import es.routerapp.protocol.optical.OmciRestartStage
import es.routerapp.protocol.optical.WanRenewProgress
import es.routerapp.protocol.optical.WanRenewStage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface FiberDiagnosticUiState {
    data object Loading : FiberDiagnosticUiState
    data class Ready(
        val snapshot: DiagnosticSnapshot,
    ) : FiberDiagnosticUiState {
        val isRunning: Boolean get() = snapshot.isRunning
        val isSuccess: Boolean get() = snapshot.isSuccess
        val failureStep: DiagnosticStepId? get() = snapshot.failureStep
    }
    data class Error(val message: String) : FiberDiagnosticUiState
}

class FiberDiagnosticViewModel(
    private val clientProvider: () -> RouterClient?,
    private val recoveryRunner: RecoveryRunner = RecoveryRunner.NoOp,
    private val engineFactory: (RouterClient) -> FiberDiagnosticEngine = { FiberDiagnosticEngine(it) },
    coroutineScope: CoroutineScope? = null,
) : ViewModel() {

    private val effectiveScope = coroutineScope ?: viewModelScope
    private val _state = MutableStateFlow<FiberDiagnosticUiState>(FiberDiagnosticUiState.Loading)
    val state: StateFlow<FiberDiagnosticUiState> = _state.asStateFlow()

    private val _wanRenewState = MutableStateFlow(WanRenewProgress())
    val wanRenewState: StateFlow<WanRenewProgress> = _wanRenewState.asStateFlow()

    private val _omciRestartState = MutableStateFlow(OmciRestartProgress())
    val omciRestartState: StateFlow<OmciRestartProgress> = _omciRestartState.asStateFlow()

    private var diagnosticJob: Job? = null
    private var actionJob: Job? = null

    init {
        startDiagnostics()
    }

    fun startDiagnostics() {
        diagnosticJob?.cancel()
        _state.value = FiberDiagnosticUiState.Loading

        diagnosticJob = effectiveScope.launch {
            val client = clientProvider()
            if (client == null || !client.isLoggedIn) {
                _state.value = FiberDiagnosticUiState.Error("Router session is not active")
                return@launch
            }

            try {
                recoveryRunner.run {
                    val engine = engineFactory(client)
                    engine.runDiagnostics().collect { snapshot ->
                        _state.value = FiberDiagnosticUiState.Ready(snapshot)
                    }
                }
            } catch (e: Exception) {
                _state.value = FiberDiagnosticUiState.Error(e.message ?: "Diagnostic failed unexpectedly")
            }
        }
    }

    fun restartOmci(
        pollIntervalMs: Long = 1000L,
        onCompleted: ((Boolean) -> Unit)? = null,
    ) {
        val client = clientProvider() ?: return
        if (!client.isLoggedIn) return
        actionJob?.cancel()

        actionJob = effectiveScope.launch {
            try {
                recoveryRunner.run {
                    val engine = engineFactory(client)
                    engine.restartOmci(pollIntervalMs = pollIntervalMs).collect { progress ->
                        _omciRestartState.value = progress
                        if (progress.stage == OmciRestartStage.SUCCESS) {
                            onCompleted?.invoke(true)
                            // Refresh diagnostics in background after successful OMCI handshake
                            startDiagnostics()
                        } else if (progress.stage == OmciRestartStage.ERROR) {
                            onCompleted?.invoke(false)
                        }
                    }
                }
            } catch (e: Exception) {
                _omciRestartState.value = OmciRestartProgress(
                    stage = OmciRestartStage.ERROR,
                    errorMessage = e.message ?: "OMCI restart error",
                )
                onCompleted?.invoke(false)
            }
        }
    }

    fun renewWanSession(
        delayBetweenMs: Long = 1500L,
        pollIntervalMs: Long = 1000L,
        onCompleted: ((Boolean, String?) -> Unit)? = null,
    ) {
        val client = clientProvider() ?: return
        if (!client.isLoggedIn) return
        actionJob?.cancel()

        actionJob = effectiveScope.launch {
            try {
                recoveryRunner.run {
                    val engine = engineFactory(client)
                    engine.renewWanSession(
                        delayBetweenMs = delayBetweenMs,
                        pollIntervalMs = pollIntervalMs,
                    ).collect { progress ->
                        _wanRenewState.value = progress
                        if (progress.stage == WanRenewStage.SUCCESS) {
                            onCompleted?.invoke(true, progress.newIp)
                            // Refresh diagnostics in background after new IP assigned
                            startDiagnostics()
                        } else if (progress.stage == WanRenewStage.ERROR) {
                            onCompleted?.invoke(false, null)
                        }
                    }
                }
            } catch (e: Exception) {
                _wanRenewState.value = WanRenewProgress(
                    stage = WanRenewStage.ERROR,
                    errorMessage = e.message ?: "WAN session renewal error",
                )
                onCompleted?.invoke(false, null)
            }
        }
    }

    fun retry() {
        startDiagnostics()
    }
}
