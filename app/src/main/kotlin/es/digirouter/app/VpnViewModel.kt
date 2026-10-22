package es.digirouter.app

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.routerapp.protocol.RouterClient
import es.routerapp.protocol.vpn.IpsecConfig
import es.routerapp.protocol.vpn.OpenVpnConfig
import es.routerapp.protocol.vpn.OpenVpnProfileTransformer
import es.routerapp.protocol.vpn.PptpConfig
import es.routerapp.protocol.vpn.VpnClientSession
import es.routerapp.protocol.vpn.VpnEngine
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

data class VpnUiState(
    val isLoading: Boolean = false,
    val openVpnConfig: OpenVpnConfig = OpenVpnConfig(),
    val clientSessions: List<VpnClientSession> = emptyList(),
    val ipsecConfig: IpsecConfig = IpsecConfig(),
    val pptpConfig: PptpConfig = PptpConfig(),
    val readiness: VpnReadinessStatus = VpnReadinessStatus(VpnReadinessLevel.UNKNOWN, ""),
    val isRegeneratingKeys: Boolean = false,
    val isDownloadingProfile: Boolean = false,
    val exportedProfileRaw: String? = null,
    val detectedRemoteHost: String? = null,
    val hostOverride: String = "",
    val message: String? = null,
    val isError: Boolean = false,
)

class VpnViewModel(
    private val clientProvider: () -> RouterClient?,
    private val store: VpnProfileStore? = null,
    private val recoveryRunner: RecoveryRunner = RecoveryRunner.NoOp,
    coroutineScope: CoroutineScope? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    private val effectiveScope = coroutineScope ?: viewModelScope

    private val _state = MutableStateFlow(VpnUiState(isLoading = true))
    val state: StateFlow<VpnUiState> = _state.asStateFlow()

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
                        val engine = VpnEngine(client)

                        val ovpn = engine.loadOpenVpnConfig().getOrDefault(OpenVpnConfig())
                        val sessions = engine.loadClientSessions().getOrDefault(emptyList())
                        val ipsec = engine.loadIpsecConfig().getOrDefault(IpsecConfig())
                        val pptp = engine.loadPptpConfig().getOrDefault(PptpConfig())

                        // Query WAN IP & DDNS configuration for readiness detection
                        val wanIp = runCatching {
                            val wanObj = client.get("DEV2_WAN_IP_CONN") as? JsonObject
                            (wanObj?.get("externalIPAddress") as? JsonPrimitive)?.content
                        }.getOrNull()

                        val ddnsStatus = runCatching {
                            val ddnsObj = client.get("DEV2_DDNS_CFG") as? JsonObject
                            val domain = (ddnsObj?.get("domain") as? JsonPrimitive)?.content
                            val enable = (ddnsObj?.get("enable") as? JsonPrimitive)?.content == "1"
                            domain to enable
                        }.getOrNull()

                        val readiness = VpnReadinessEvaluator.evaluate(
                            wanIp = wanIp,
                            ddnsDomain = ddnsStatus?.first,
                            isDdnsActive = ddnsStatus?.second == true,
                        )

                        _state.update {
                            it.copy(
                                isLoading = false,
                                openVpnConfig = ovpn,
                                clientSessions = sessions,
                                ipsecConfig = ipsec,
                                pptpConfig = pptp,
                                readiness = readiness,
                                isError = false,
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        message = e.message ?: "Failed to load VPN configuration",
                        isError = true,
                    )
                }
            }
        }
    }

    fun saveOpenVpnConfig(config: OpenVpnConfig) {
        effectiveScope.launch {
            _state.update { it.copy(isLoading = true, message = null) }
            try {
                recoveryRunner.run {
                    withContext(ioDispatcher) {
                        val client = clientProvider() ?: throw IllegalStateException("Not connected")
                        val engine = VpnEngine(client)
                        engine.saveOpenVpnConfig(config).getOrThrow()
                        _state.update {
                            it.copy(
                                isLoading = false,
                                openVpnConfig = config,
                                message = "Configuration saved successfully",
                                isError = false,
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        message = e.message ?: "Failed to save configuration",
                        isError = true,
                    )
                }
            }
        }
    }

    fun regenerateOpenVpnKeys() {
        effectiveScope.launch {
            _state.update { it.copy(isRegeneratingKeys = true, message = null) }
            try {
                recoveryRunner.run {
                    withContext(ioDispatcher) {
                        val client = clientProvider() ?: throw IllegalStateException("Not connected")
                        val engine = VpnEngine(client)
                        engine.regenerateOpenVpnKeys().getOrThrow()
                        store?.clearProfiles()
                        _state.update {
                            it.copy(
                                isRegeneratingKeys = false,
                                message = "Certificates and security keys regenerated",
                                isError = false,
                            )
                        }
                    }
                }
                loadAll()
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isRegeneratingKeys = false,
                        message = e.message ?: "Key regeneration failed",
                        isError = true,
                    )
                }
            }
        }
    }

    fun prepareProfileExport() {
        effectiveScope.launch {
            _state.update { it.copy(isDownloadingProfile = true, message = null) }
            try {
                recoveryRunner.run {
                    withContext(ioDispatcher) {
                        val client = clientProvider() ?: throw IllegalStateException("Not connected")
                        val engine = VpnEngine(client)
                        val rawProfile = engine.downloadOpenVpnProfile().getOrThrow()
                        val detectedHost = OpenVpnProfileTransformer.extractRemoteHost(rawProfile)

                        _state.update {
                            it.copy(
                                isDownloadingProfile = false,
                                exportedProfileRaw = rawProfile,
                                detectedRemoteHost = detectedHost,
                                hostOverride = detectedHost.orEmpty(),
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isDownloadingProfile = false,
                        message = e.message ?: "Failed to download client profile",
                        isError = true,
                    )
                }
            }
        }
    }

    fun updateHostOverride(overrideHost: String) {
        _state.update { it.copy(hostOverride = overrideHost) }
    }

    fun exportProfileToUri(context: Context): Uri? {
        val raw = _state.value.exportedProfileRaw ?: return null
        val override = _state.value.hostOverride.trim()
        val finalProfile = OpenVpnProfileTransformer.overrideRemoteHost(raw, override.ifEmpty { null })
        val profileStore = store ?: FileVpnProfileStore(context)
        val file = profileStore.saveProfile(finalProfile, "client.ovpn")
        return profileStore.getShareableUri(file, context)
    }

    fun getExportedProfileText(): String? {
        val raw = _state.value.exportedProfileRaw ?: return null
        val override = _state.value.hostOverride.trim()
        return OpenVpnProfileTransformer.overrideRemoteHost(raw, override.ifEmpty { null })
    }

    fun dismissExportSheet() {
        _state.update { it.copy(exportedProfileRaw = null, detectedRemoteHost = null, hostOverride = "") }
    }

    fun dismissMessage() {
        _state.update { it.copy(message = null, isError = false) }
    }
}
