package es.digirouter.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.routerapp.protocol.RouterClient
import es.routerapp.protocol.log.LogEntry
import es.routerapp.protocol.log.LogSeverity
import es.routerapp.protocol.log.SyslogConfig
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Filter categories for grouping severity levels.
 */
enum class SeverityFilterCategory {
    ALL,
    ERRORS,    // EMERGENCY, ALERT, CRITICAL, ERROR (0..3)
    WARNINGS,  // WARNING (4)
    INFO,      // NOTICE, INFO (5..6)
    DEBUG,     // DEBUG (7)
}

data class SystemLogUiState(
    val logs: List<LogEntry> = emptyList(),
    val filteredLogs: List<LogEntry> = emptyList(),
    val availableModules: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val isLive: Boolean = true,
    val searchQuery: String = "",
    val severityFilter: SeverityFilterCategory = SeverityFilterCategory.ALL,
    val selectedModule: String? = null,
    val syslogConfig: SyslogConfig? = null,
    val isUpdatingConfig: Boolean = false,
    val errorMessage: String? = null,
)

class SystemLogViewModel(
    private val clientProvider: () -> RouterClient?,
    private val recoveryRunner: RecoveryRunner = RecoveryRunner.NoOp,
    coroutineScope: CoroutineScope? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val pollIntervalMs: Long = 3000L,
) : ViewModel() {

    private val effectiveScope = coroutineScope ?: viewModelScope
    private var tickerJob: Job? = null

    private val _state = MutableStateFlow(SystemLogUiState(isLoading = true))
    val state: StateFlow<SystemLogUiState> = _state.asStateFlow()

    init {
        loadSyslogConfig()
        startPolling()
    }

    fun startPolling() {
        tickerJob?.cancel()
        _state.update { it.copy(isLive = true) }
        tickerJob = effectiveScope.launch {
            while (isActive) {
                fetchLogs()
                delay(pollIntervalMs)
            }
        }
    }

    fun pausePolling() {
        tickerJob?.cancel()
        tickerJob = null
        _state.update { it.copy(isLive = false) }
    }

    fun toggleLive() {
        if (_state.value.isLive) {
            pausePolling()
        } else {
            startPolling()
        }
    }

    fun refresh() {
        effectiveScope.launch {
            fetchLogs()
        }
    }

    private suspend fun fetchLogs() {
        val client = clientProvider() ?: return
        try {
            val fetched = recoveryRunner.run {
                withContext(ioDispatcher) {
                    client.getSystemLogs()
                }
            }
            _state.update { current ->
                val modules = fetched.map { it.module }.distinct().sorted()
                val filtered = applyFilter(fetched, current.searchQuery, current.severityFilter, current.selectedModule)
                current.copy(
                    logs = fetched,
                    filteredLogs = filtered,
                    availableModules = modules,
                    isLoading = false,
                    errorMessage = null,
                )
            }
        } catch (e: Exception) {
            _state.update { it.copy(isLoading = false, errorMessage = e.message ?: "Failed to fetch logs") }
        }
    }

    fun loadSyslogConfig() {
        val client = clientProvider() ?: return
        effectiveScope.launch {
            try {
                val config = recoveryRunner.run {
                    withContext(ioDispatcher) {
                        client.getSyslogConfig()
                    }
                }
                _state.update { it.copy(syslogConfig = config) }
            } catch (_: Exception) {
                // Non-critical, ignore or keep existing config
            }
        }
    }

    fun updateLocalSeverity(severity: LogSeverity, onSuccess: (() -> Unit)? = null) {
        val client = clientProvider() ?: return
        _state.update { it.copy(isUpdatingConfig = true) }
        effectiveScope.launch {
            try {
                recoveryRunner.run {
                    withContext(ioDispatcher) {
                        client.setLocalSeverity(severity)
                    }
                }
                _state.update { current ->
                    current.copy(
                        isUpdatingConfig = false,
                        syslogConfig = current.syslogConfig?.copy(localSeverity = severity) ?: SyslogConfig(localSeverity = severity),
                    )
                }
                onSuccess?.invoke()
                fetchLogs()
            } catch (e: Exception) {
                _state.update { it.copy(isUpdatingConfig = false, errorMessage = e.message ?: "Failed to update severity") }
            }
        }
    }

    fun clearLogs(onCleared: (() -> Unit)? = null) {
        val client = clientProvider() ?: return
        _state.update { it.copy(isLoading = true) }
        effectiveScope.launch {
            try {
                recoveryRunner.run {
                    withContext(ioDispatcher) {
                        client.clearSystemLogs()
                    }
                }
                _state.update { it.copy(logs = emptyList(), filteredLogs = emptyList(), isLoading = false) }
                onCleared?.invoke()
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, errorMessage = e.message ?: "Failed to clear logs") }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _state.update { current ->
            current.copy(
                searchQuery = query,
                filteredLogs = applyFilter(current.logs, query, current.severityFilter, current.selectedModule),
            )
        }
    }

    fun setSeverityFilter(filter: SeverityFilterCategory) {
        _state.update { current ->
            current.copy(
                severityFilter = filter,
                filteredLogs = applyFilter(current.logs, current.searchQuery, filter, current.selectedModule),
            )
        }
    }

    fun setSelectedModule(module: String?) {
        _state.update { current ->
            current.copy(
                selectedModule = module,
                filteredLogs = applyFilter(current.logs, current.searchQuery, current.severityFilter, module),
            )
        }
    }

    private fun applyFilter(
        source: List<LogEntry>,
        query: String,
        severityCategory: SeverityFilterCategory,
        module: String?,
    ): List<LogEntry> {
        val q = query.trim().lowercase()
        return source.filter { entry ->
            val matchesQuery = q.isEmpty() ||
                entry.message.lowercase().contains(q) ||
                entry.module.lowercase().contains(q) ||
                entry.timestamp.contains(q)

            val matchesSeverity = when (severityCategory) {
                SeverityFilterCategory.ALL -> true
                SeverityFilterCategory.ERRORS -> entry.severity.code in 0..3
                SeverityFilterCategory.WARNINGS -> entry.severity.code == 4
                SeverityFilterCategory.INFO -> entry.severity.code in 5..6
                SeverityFilterCategory.DEBUG -> entry.severity.code == 7
            }

            val matchesModule = module == null || entry.module.equals(module, ignoreCase = true)

            matchesQuery && matchesSeverity && matchesModule
        }
    }

    override fun onCleared() {
        super.onCleared()
        tickerJob?.cancel()
    }
}
