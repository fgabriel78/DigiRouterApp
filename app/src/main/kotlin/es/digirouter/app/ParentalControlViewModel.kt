package es.digirouter.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.routerapp.protocol.RouterClient
import es.routerapp.protocol.pages.ConnectedDevice
import es.routerapp.protocol.pages.ConnectedDevices
import es.routerapp.protocol.parental.ParentalControlEngine
import es.routerapp.protocol.parental.ParentalProfile
import es.routerapp.protocol.parental.ProfileStatus
import es.routerapp.protocol.parental.WebInsights
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ParentalControlUiState(
    val isLoading: Boolean = false,
    val profiles: List<ParentalProfile> = emptyList(),
    val activeDevices: List<ConnectedDevice> = emptyList(),
    val selectedProfileInsights: WebInsights? = null,
    val isLoadingInsights: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)

class ParentalControlViewModel(
    private val clientProvider: () -> RouterClient?,
    private val recoveryRunner: RecoveryRunner = RecoveryRunner.NoOp,
    private val engineProvider: (RouterClient) -> ParentalControlEngine = { ParentalControlEngine(it) },
    coroutineScope: CoroutineScope? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    private val effectiveScope = coroutineScope ?: viewModelScope
    private val _state = MutableStateFlow(ParentalControlUiState(isLoading = true))
    val state: StateFlow<ParentalControlUiState> = _state.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        effectiveScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val client = clientProvider()
            if (client == null) {
                _state.update { it.copy(isLoading = false, errorMessage = "Router not connected") }
                return@launch
            }

            try {
                val (profiles, devices) = withContext(ioDispatcher) {
                    recoveryRunner.run {
                        val engine = engineProvider(client)
                        val p = engine.loadProfiles().getOrThrow()
                        val d = runCatching { ConnectedDevices.load(client) }.getOrDefault(emptyList())
                        p to d
                    }
                }
                _state.update {
                    it.copy(
                        isLoading = false,
                        profiles = profiles,
                        activeDevices = devices,
                        errorMessage = null,
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to load parental controls",
                    )
                }
            }
        }
    }

    fun togglePause(profile: ParentalProfile) {
        effectiveScope.launch {
            val client = clientProvider() ?: return@launch
            try {
                val newStatus = withContext(ioDispatcher) {
                    recoveryRunner.run {
                        val engine = engineProvider(client)
                        engine.togglePause(profile).getOrThrow()
                    }
                }
                _state.update { current ->
                    current.copy(
                        profiles = current.profiles.map {
                            if (it.stack == profile.stack) it.copy(status = newStatus) else it
                        }
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = e.message ?: "Failed to toggle pause") }
            }
        }
    }

    fun saveProfile(profile: ParentalProfile, isNew: Boolean, onDone: (() -> Unit)? = null) {
        effectiveScope.launch {
            _state.update { it.copy(isSaving = true, errorMessage = null) }
            val client = clientProvider()
            if (client == null) {
                _state.update { it.copy(isSaving = false, errorMessage = "Router not connected") }
                return@launch
            }

            try {
                withContext(ioDispatcher) {
                    recoveryRunner.run {
                        val engine = engineProvider(client)
                        engine.saveProfile(profile, isNew).getOrThrow()
                    }
                }
                loadData()
                _state.update { it.copy(isSaving = false) }
                onDone?.invoke()
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, errorMessage = e.message ?: "Failed to save profile") }
            }
        }
    }

    fun deleteProfile(profileStack: String, onDone: (() -> Unit)? = null) {
        effectiveScope.launch {
            _state.update { it.copy(isSaving = true, errorMessage = null) }
            val client = clientProvider()
            if (client == null) {
                _state.update { it.copy(isSaving = false, errorMessage = "Router not connected") }
                return@launch
            }

            try {
                withContext(ioDispatcher) {
                    recoveryRunner.run {
                        val engine = engineProvider(client)
                        engine.deleteProfile(profileStack).getOrThrow()
                    }
                }
                loadData()
                _state.update { it.copy(isSaving = false) }
                onDone?.invoke()
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, errorMessage = e.message ?: "Failed to delete profile") }
            }
        }
    }

    fun loadInsights(profileStack: String, dateOffset: Int = 1) {
        effectiveScope.launch {
            _state.update { it.copy(isLoadingInsights = true) }
            val client = clientProvider()
            if (client == null) {
                _state.update { it.copy(isLoadingInsights = false) }
                return@launch
            }

            try {
                val insights = withContext(ioDispatcher) {
                    recoveryRunner.run {
                        val engine = engineProvider(client)
                        engine.fetchInsights(profileStack, dateOffset).getOrThrow()
                    }
                }
                _state.update { it.copy(isLoadingInsights = false, selectedProfileInsights = insights) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoadingInsights = false, errorMessage = e.message ?: "Failed to load insights") }
            }
        }
    }

    fun unblockUrl(profileStack: String, url: String) {
        effectiveScope.launch {
            val client = clientProvider() ?: return@launch
            try {
                withContext(ioDispatcher) {
                    recoveryRunner.run {
                        val engine = engineProvider(client)
                        engine.unblockUrl(profileStack, url).getOrThrow()
                    }
                }
                // Reload current insights
                val currentOffset = _state.value.selectedProfileInsights?.dateOffset ?: 1
                loadInsights(profileStack, currentOffset)
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = e.message ?: "Failed to unblock URL") }
            }
        }
    }

    fun addBlacklist(profileStack: String, url: String) {
        effectiveScope.launch {
            val client = clientProvider() ?: return@launch
            try {
                withContext(ioDispatcher) {
                    recoveryRunner.run {
                        val engine = engineProvider(client)
                        engine.addBlacklistDomain(profileStack, url).getOrThrow()
                    }
                }
                loadData()
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = e.message ?: "Failed to add to blacklist") }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }
}
