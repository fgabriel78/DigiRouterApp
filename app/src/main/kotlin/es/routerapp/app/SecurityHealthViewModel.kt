package es.routerapp.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.routerapp.protocol.RouterClient
import es.routerapp.protocol.security.RemediationAction
import es.routerapp.protocol.security.SecurityAuditEngine
import es.routerapp.protocol.security.SecurityFinding
import es.routerapp.protocol.security.SecurityReport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SecurityFilter {
    ALL,
    AT_RISK,
    PASSED,
}

sealed interface SecurityUiState {
    data object Loading : SecurityUiState
    data class Success(
        val report: SecurityReport,
        val filter: SecurityFilter = SecurityFilter.ALL,
        val isBatchApplying: Boolean = false,
        val batchProgress: Pair<Int, Int>? = null, // current, total
        val bannerDismissed: Boolean = false,
    ) : SecurityUiState {
        val visibleFindings: List<SecurityFinding>
            get() = when (filter) {
                SecurityFilter.ALL -> report.findings
                SecurityFilter.AT_RISK -> report.findings.filter { !it.isPassed }
                SecurityFilter.PASSED -> report.findings.filter { it.isPassed }
            }
    }
    data class Error(val message: String) : SecurityUiState
}

class SecurityHealthViewModel(
    private val clientProvider: () -> RouterClient?,
    private val recoveryRunner: RecoveryRunner = RecoveryRunner.NoOp,
    private val auditEngine: SecurityAuditEngine = SecurityAuditEngine,
    coroutineScope: CoroutineScope? = null,
) : ViewModel() {

    private val effectiveScope = coroutineScope ?: viewModelScope

    private val _state = MutableStateFlow<SecurityUiState>(SecurityUiState.Loading)
    val state: StateFlow<SecurityUiState> = _state.asStateFlow()

    fun scan() {
        _state.value = SecurityUiState.Loading
        effectiveScope.launch {
            val client = clientProvider()
            if (client == null || !client.isLoggedIn) {
                _state.value = SecurityUiState.Error("Router session is not active")
                return@launch
            }

            val result = withContext(Dispatchers.IO) {
                runCatching {
                    recoveryRunner.run {
                        auditEngine.audit(client)
                    }
                }
            }

            result.fold(
                onSuccess = { report ->
                    _state.value = SecurityUiState.Success(report = report)
                },
                onFailure = { err ->
                    _state.value = SecurityUiState.Error(err.message ?: "Failed to audit security posture")
                }
            )
        }
    }

    fun setFilter(filter: SecurityFilter) {
        _state.update { current ->
            if (current is SecurityUiState.Success) {
                current.copy(filter = filter)
            } else current
        }
    }

    fun dismissBanner() {
        _state.update { current ->
            if (current is SecurityUiState.Success) {
                current.copy(bannerDismissed = true)
            } else current
        }
    }

    fun applySingleRemediation(action: RemediationAction, onComplete: (Boolean) -> Unit = {}) {
        effectiveScope.launch {
            val client = clientProvider()
            if (client == null || !client.isLoggedIn) {
                onComplete(false)
                return@launch
            }

            val success = withContext(Dispatchers.IO) {
                runCatching {
                    recoveryRunner.run {
                        auditEngine.executeRemediation(client, action)
                    }
                }.isSuccess
            }

            if (success) {
                withContext(Dispatchers.IO) {
                    kotlinx.coroutines.delay(600)
                    val newReport = runCatching {
                        recoveryRunner.run {
                            auditEngine.audit(client)
                        }
                    }.getOrNull()

                    if (newReport != null) {
                        _state.update { current ->
                            val currentFilter = (current as? SecurityUiState.Success)?.filter ?: SecurityFilter.ALL
                            val dismissed = (current as? SecurityUiState.Success)?.bannerDismissed ?: false
                            SecurityUiState.Success(
                                report = newReport,
                                filter = currentFilter,
                                bannerDismissed = dismissed,
                            )
                        }
                    }
                }
            }
            onComplete(success)
        }
    }

    fun applyBatchRemediation(onComplete: (Boolean, String?) -> Unit = { _, _ -> }) {
        val currentState = _state.value as? SecurityUiState.Success ?: return
        val plan = currentState.report.batchPlan
        if (plan.isEmpty()) {
            onComplete(true, null)
            return
        }

        _state.update { (it as SecurityUiState.Success).copy(isBatchApplying = true, batchProgress = 0 to plan.size) }

        effectiveScope.launch {
            val client = clientProvider()
            if (client == null || !client.isLoggedIn) {
                _state.update { (it as? SecurityUiState.Success)?.copy(isBatchApplying = false, batchProgress = null) ?: it }
                onComplete(false, "Router session is not active")
                return@launch
            }

            val failedActions = mutableListOf<String>()

            withContext(Dispatchers.IO) {
                recoveryRunner.run {
                    for ((index, action) in plan.withIndex()) {
                        _state.update { (it as? SecurityUiState.Success)?.copy(batchProgress = (index + 1) to plan.size) ?: it }
                        val res = runCatching {
                            auditEngine.executeRemediation(client, action)
                        }
                        if (res.isFailure) {
                            failedActions.add(action.title)
                        }
                    }
                }

                kotlinx.coroutines.delay(600)
                val newReport = runCatching {
                    recoveryRunner.run {
                        auditEngine.audit(client)
                    }
                }.getOrNull()

                if (newReport != null) {
                    _state.update { current ->
                        val currentFilter = (current as? SecurityUiState.Success)?.filter ?: SecurityFilter.ALL
                        val dismissed = (current as? SecurityUiState.Success)?.bannerDismissed ?: false
                        SecurityUiState.Success(
                            report = newReport,
                            filter = currentFilter,
                            isBatchApplying = false,
                            batchProgress = null,
                            bannerDismissed = dismissed,
                        )
                    }
                } else {
                    _state.update { (it as? SecurityUiState.Success)?.copy(isBatchApplying = false, batchProgress = null) ?: it }
                }
            }

            if (failedActions.isEmpty()) {
                onComplete(true, null)
            } else {
                onComplete(false, failedActions.joinToString(", "))
            }
        }
    }
}
