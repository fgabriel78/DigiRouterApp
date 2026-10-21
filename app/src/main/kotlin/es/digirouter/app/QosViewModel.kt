package es.digirouter.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.routerapp.protocol.RouterClient
import es.routerapp.protocol.pages.ConnectedDevice
import es.routerapp.protocol.pages.ConnectedDevices
import es.routerapp.protocol.qos.BandwidthUnit
import es.routerapp.protocol.qos.GameQosClient
import es.routerapp.protocol.qos.QosConfig
import es.routerapp.protocol.qos.QosEngine
import es.routerapp.protocol.qos.QosPriority
import es.routerapp.protocol.qos.QosRule
import es.routerapp.protocol.qos.QosValidationError
import es.routerapp.protocol.qos.QueueAllocation
import es.routerapp.protocol.qos.normalizeQosMac
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** A client shown in the QoS device list, with its effective priority. */
data class QosDeviceItem(
    /** Normalized MAC (`AA:BB:CC:DD:EE:FF`). */
    val mac: String,
    /** Alias, fingerprint model, hostname... resolved like the rest of the app. */
    val title: String,
    /** Router-side hostname, used as the rule's `deviceName`. */
    val hostName: String,
    val ip: String,
    val category: DeviceCategory?,
    val online: Boolean,
    val priority: QosPriority,
    /** Device rules currently bound to this MAC (empty = default Middle priority). */
    val rules: List<QosRule>,
    /** Plugged into the hardware Game Port: pinned to High, cannot be changed. */
    val isGamePort: Boolean,
)

/** Editable copy of the global settings; bandwidths are kept as text while the user types. */
data class QosDraft(
    val enabled: Boolean,
    val upText: String,
    val upUnit: BandwidthUnit,
    val downText: String,
    val downUnit: BandwidthUnit,
    val queues: QueueAllocation,
) {
    val upBandwidth: Int? get() = upText.toIntOrNull()
    val downBandwidth: Int? get() = downText.toIntOrNull()

    fun toConfig(base: QosConfig): QosConfig = base.copy(
        enabled = enabled,
        upBandwidth = upBandwidth ?: -1,
        upUnit = upUnit,
        downBandwidth = downBandwidth ?: -1,
        downUnit = downUnit,
        queues = queues,
    )

    companion object {
        /** Unconfigured routers report `0` bandwidth and `0%` queues: propose valid defaults instead. */
        fun from(config: QosConfig): QosDraft = QosDraft(
            enabled = config.enabled,
            upText = config.upBandwidth.takeIf { it > 0 }?.toString() ?: QosConfig.DEFAULT_BANDWIDTH.toString(),
            upUnit = config.upUnit,
            downText = config.downBandwidth.takeIf { it > 0 }?.toString() ?: QosConfig.DEFAULT_BANDWIDTH.toString(),
            downUnit = config.downUnit,
            queues = if (config.queues.total == 0) QueueAllocation.DEFAULT else config.queues.normalized(),
        )
    }
}

enum class QosError { NOT_CONNECTED, LOAD_FAILED, SAVE_FAILED, PRIORITY_FAILED, DELETE_FAILED }

data class QosUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val config: QosConfig? = null,
    val draft: QosDraft? = null,
    val devices: List<QosDeviceItem> = emptyList(),
    /** Port / system rules created elsewhere; shown read-only with a delete action. */
    val otherRules: List<QosRule> = emptyList(),
    val pendingMacs: Set<String> = emptySet(),
    val pendingRuleStacks: Set<String> = emptySet(),
    val validationErrors: Set<QosValidationError> = emptySet(),
    val error: QosError? = null,
    val errorDetail: String? = null,
    /** One-shot flag raised after a successful save. */
    val saved: Boolean = false,
) {
    val isDirty: Boolean get() = config != null && draft != null && draft != QosDraft.from(config)
}

/**
 * Joins the router's device rules and Game Port clients with the discovered clients and local metadata.
 * Rule-bound devices that are offline are kept so they can still be changed. Order: Game Port, High, Middle,
 * Low; online before offline; then by name.
 */
fun buildQosDevices(
    connected: List<ConnectedDevice>,
    rules: List<QosRule>,
    gameClients: List<GameQosClient>,
    metadataStore: DeviceMetadataStore? = null,
): List<QosDeviceItem> {
    val rulesByMac = rules.filter { it.isDevice && it.macAddress.isNotBlank() }.groupBy { it.normalizedMac }
    val gameByMac = gameClients.filter { it.macAddress.isNotBlank() }.associateBy { it.normalizedMac }
    val connectedByMac = LinkedHashMap<String, ConnectedDevice>()
    connected.forEach { d -> if (d.mac.isNotBlank()) connectedByMac.putIfAbsent(normalizeQosMac(d.mac), d) }

    val macs = LinkedHashSet<String>().apply {
        addAll(connectedByMac.keys); addAll(gameByMac.keys); addAll(rulesByMac.keys)
    }
    return macs.map { mac ->
        val device = connectedByMac[mac]
        val game = gameByMac[mac]
        val bound = rulesByMac[mac].orEmpty()
        val rawMac = device?.mac ?: game?.macAddress ?: bound.first().macAddress
        val hostName = device?.name?.takeIf { it.isNotBlank() }
            ?: game?.hostName?.takeIf { it.isNotBlank() }
            ?: bound.firstOrNull { it.deviceName.isNotBlank() }?.deviceName
            ?: ""
        val ip = device?.ip?.takeIf { it.isNotBlank() } ?: game?.ipAddress.orEmpty()
        val meta = metadataStore?.let { it.get(rawMac) ?: it.get(mac) }
        val fingerprint = metadataStore?.let { it.getFingerprint(rawMac) ?: it.getFingerprint(mac) }
        val resolved = DeviceResolver.resolve(mac = mac, hostname = hostName, ip = ip, customMeta = meta, fingerprint = fingerprint)
        QosDeviceItem(
            mac = mac,
            title = resolved.displayTitle,
            hostName = hostName,
            ip = ip,
            category = resolved.category,
            online = game != null || device?.active == true,
            priority = when {
                game != null -> QosPriority.HIGH
                bound.isNotEmpty() -> bound.first().priority
                else -> QosPriority.MIDDLE
            },
            rules = bound,
            isGamePort = game != null,
        )
    }.sortedWith(
        compareByDescending<QosDeviceItem> { it.isGamePort }
            .thenByDescending { it.priority.code }
            .thenByDescending { it.online }
            .thenBy { it.title.lowercase() },
    )
}

class QosViewModel(
    private val clientProvider: () -> RouterClient?,
    private val metadataStore: DeviceMetadataStore? = null,
    private val recoveryRunner: RecoveryRunner = RecoveryRunner.NoOp,
    private val engineProvider: (RouterClient) -> QosEngine = { QosEngine(it) },
    private val devicesLoader: (RouterClient) -> List<ConnectedDevice> = { ConnectedDevices.load(it) },
    coroutineScope: CoroutineScope? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    private val scope = coroutineScope ?: viewModelScope
    private val _state = MutableStateFlow(QosUiState(isLoading = true))
    val state: StateFlow<QosUiState> = _state.asStateFlow()

    /** Last discovered clients, reused when only the rules change. */
    private var connected: List<ConnectedDevice> = emptyList()

    init {
        loadData()
    }

    /** Full reload: settings (discarding unsaved edits), rules, Game Port clients and discovered devices. */
    fun loadData() {
        scope.launch {
            _state.update { it.copy(isLoading = true, error = null, errorDetail = null) }
            val client = clientProvider() ?: return@launch fail(QosError.NOT_CONNECTED, null) { copy(isLoading = false) }
            try {
                val snapshot = withContext(ioDispatcher) {
                    recoveryRunner.run {
                        val engine = engineProvider(client)
                        val config = engine.loadConfig().getOrThrow()
                        val rules = engine.loadRules().getOrThrow()
                        val games = engine.loadGameClients()
                        val devices = runCatching { devicesLoader(client) }.getOrDefault(emptyList())
                        Snapshot(config, rules, games, devices)
                    }
                }
                connected = snapshot.devices
                _state.update {
                    it.copy(
                        isLoading = false,
                        config = snapshot.config,
                        draft = QosDraft.from(snapshot.config),
                        devices = buildQosDevices(snapshot.devices, snapshot.rules, snapshot.games, metadataStore),
                        otherRules = snapshot.rules.filter { r -> !r.isDevice },
                        validationErrors = emptySet(),
                    )
                }
            } catch (e: Exception) {
                fail(QosError.LOAD_FAILED, e) { copy(isLoading = false) }
            }
        }
    }

    // ---------- Global settings editing ----------

    fun setEnabled(enabled: Boolean) = editDraft { copy(enabled = enabled) }
    fun setUpBandwidth(text: String) = editDraft { copy(upText = sanitize(text)) }
    fun setDownBandwidth(text: String) = editDraft { copy(downText = sanitize(text)) }
    fun setUpUnit(unit: BandwidthUnit) = editDraft { copy(upUnit = unit) }
    fun setDownUnit(unit: BandwidthUnit) = editDraft { copy(downUnit = unit) }
    fun setHigh(percent: Int) = editDraft { copy(queues = queues.withHigh(percent)) }
    fun setMiddle(percent: Int) = editDraft { copy(queues = queues.withMiddle(percent)) }
    fun setLow(percent: Int) = editDraft { copy(queues = queues.withLow(percent)) }

    fun saveConfig() {
        val current = _state.value
        val base = current.config ?: return
        val draft = current.draft ?: return
        if (current.isSaving) return
        val config = draft.toConfig(base)
        val errors = config.validate()
        if (errors.isNotEmpty()) {
            _state.update { it.copy(validationErrors = errors) }
            return
        }
        scope.launch {
            _state.update { it.copy(isSaving = true, validationErrors = emptySet(), error = null, errorDetail = null) }
            val client = clientProvider() ?: return@launch fail(QosError.NOT_CONNECTED, null) { copy(isSaving = false) }
            try {
                val reloaded = withContext(ioDispatcher) {
                    recoveryRunner.run {
                        val engine = engineProvider(client)
                        engine.saveConfig(config).getOrThrow()
                        engine.loadConfig().getOrDefault(config)
                    }
                }
                _state.update {
                    it.copy(isSaving = false, config = reloaded, draft = QosDraft.from(reloaded), saved = true)
                }
            } catch (e: Exception) {
                fail(QosError.SAVE_FAILED, e) { copy(isSaving = false) }
            }
        }
    }

    // ---------- Device priorities ----------

    fun setDevicePriority(device: QosDeviceItem, priority: QosPriority) {
        if (device.isGamePort || (device.priority == priority && device.rules.size <= 1)) return
        if (device.mac in _state.value.pendingMacs) return
        mutateRules(
            markPending = { copy(pendingMacs = pendingMacs + device.mac) },
            clearPending = { copy(pendingMacs = pendingMacs - device.mac) },
            error = QosError.PRIORITY_FAILED,
        ) { engine -> engine.setDevicePriority(device.mac, device.hostName, priority, device.rules).getOrThrow() }
    }

    fun deleteRule(rule: QosRule) {
        if (rule.stack in _state.value.pendingRuleStacks) return
        mutateRules(
            markPending = { copy(pendingRuleStacks = pendingRuleStacks + rule.stack) },
            clearPending = { copy(pendingRuleStacks = pendingRuleStacks - rule.stack) },
            error = QosError.DELETE_FAILED,
        ) { engine -> engine.deleteRule(rule.stack).getOrThrow() }
    }

    fun clearError() = _state.update { it.copy(error = null, errorDetail = null) }
    fun consumeSaved() = _state.update { it.copy(saved = false) }

    /** Runs a rule mutation, then re-reads the rules so the list always mirrors the router (even after a partial failure). */
    private fun mutateRules(
        markPending: QosUiState.() -> QosUiState,
        clearPending: QosUiState.() -> QosUiState,
        error: QosError,
        block: (QosEngine) -> Unit,
    ) {
        scope.launch {
            _state.update { it.markPending().copy(error = null, errorDetail = null) }
            val client = clientProvider() ?: return@launch fail(QosError.NOT_CONNECTED, null, clearPending)
            val engineFailure = try {
                withContext(ioDispatcher) { recoveryRunner.run { block(engineProvider(client)) } }
                null
            } catch (e: Exception) {
                e
            }
            val refreshed = runCatching {
                withContext(ioDispatcher) {
                    recoveryRunner.run {
                        val engine = engineProvider(client)
                        engine.loadRules().getOrThrow() to engine.loadGameClients()
                    }
                }
            }.getOrNull()
            _state.update { s ->
                val base = s.clearPending()
                if (refreshed == null) base else base.copy(
                    devices = buildQosDevices(connected, refreshed.first, refreshed.second, metadataStore),
                    otherRules = refreshed.first.filter { !it.isDevice },
                )
            }
            if (engineFailure != null) fail(error, engineFailure) { this }
        }
    }

    private fun editDraft(change: QosDraft.() -> QosDraft) {
        _state.update { s -> s.draft?.let { s.copy(draft = it.change(), validationErrors = emptySet()) } ?: s }
    }

    private fun fail(error: QosError, cause: Throwable?, extra: QosUiState.() -> QosUiState) {
        _state.update { it.extra().copy(error = error, errorDetail = cause?.message) }
    }

    private fun sanitize(text: String): String = text.filter { it.isDigit() }.take(MAX_BANDWIDTH_DIGITS)

    private data class Snapshot(
        val config: QosConfig,
        val rules: List<QosRule>,
        val games: List<GameQosClient>,
        val devices: List<ConnectedDevice>,
    )

    companion object {
        private const val MAX_BANDWIDTH_DIGITS = 5
    }
}
