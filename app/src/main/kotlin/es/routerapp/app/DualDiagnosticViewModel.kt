package es.routerapp.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.routerapp.protocol.RouterClient
import es.routerapp.protocol.dualdiag.DualDiagnosticEngine
import es.routerapp.protocol.dualdiag.DualDiagnosticState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI presentation state for the Dual Diagnostic screen.
 */
sealed interface DualDiagnosticUiState {
    data object Loading : DualDiagnosticUiState
    data class Ready(val state: DualDiagnosticState) : DualDiagnosticUiState {
        val isRunning: Boolean get() = state.isRunning
        val verdict get() = state.verdict
    }
    data class Error(val message: String) : DualDiagnosticUiState
}

/**
 * ViewModel orchestrating dual diagnostics between the router hardware and mobile client.
 */
class DualDiagnosticViewModel(
    private val clientProvider: () -> RouterClient?,
    private val recoveryRunner: RecoveryRunner = RecoveryRunner.NoOp,
    private val engineFactory: (RouterClient) -> DualDiagnosticEngine = { DualDiagnosticEngine(it) },
    coroutineScope: CoroutineScope? = null,
) : ViewModel() {

    private val effectiveScope = coroutineScope ?: viewModelScope
    private val _state = MutableStateFlow<DualDiagnosticUiState>(DualDiagnosticUiState.Loading)
    val state: StateFlow<DualDiagnosticUiState> = _state.asStateFlow()

    private var diagnosticJob: Job? = null

    init {
        startDiagnostics()
    }

    fun startDiagnostics() {
        diagnosticJob?.cancel()
        _state.value = DualDiagnosticUiState.Loading

        diagnosticJob = effectiveScope.launch {
            val client = clientProvider()
            if (client == null || !client.isLoggedIn) {
                _state.value = DualDiagnosticUiState.Error("Router session is not active")
                return@launch
            }

            try {
                recoveryRunner.run {
                    val engine = engineFactory(client)
                    engine.runDiagnostics().collect { diagnosticState ->
                        _state.value = DualDiagnosticUiState.Ready(diagnosticState)
                    }
                }
            } catch (e: Exception) {
                _state.value = DualDiagnosticUiState.Error(e.message ?: "Dual diagnostic failed unexpectedly")
            }
        }
    }

    fun retry() {
        startDiagnostics()
    }
}
