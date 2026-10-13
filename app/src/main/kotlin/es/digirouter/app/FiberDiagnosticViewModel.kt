package es.digirouter.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.routerapp.protocol.RouterClient
import es.routerapp.protocol.optical.DiagnosticSnapshot
import es.routerapp.protocol.optical.DiagnosticStepId
import es.routerapp.protocol.optical.FiberDiagnosticEngine
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

    private var diagnosticJob: Job? = null

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

    fun retry() {
        startDiagnostics()
    }
}
