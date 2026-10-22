package es.routerapp.protocol.vpn

import es.routerapp.protocol.RouterClient
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Subset of [RouterClient] operations required by [VpnEngine], replaceable by fakes in tests. */
interface VpnRouterRpc {
    fun get(oid: String, attrs: Map<String, String> = emptyMap(), stack: String = "0,0,0,0,0,0"): JsonElement
    fun getList(oid: String, attrs: Map<String, String> = emptyMap(), stack: String = "0,0,0,0,0,0"): JsonElement
    fun set(oid: String, attrs: Map<String, String>, stack: String = "0,0,0,0,0,0"): JsonElement
    fun operate(oid: String, attrs: Map<String, String> = emptyMap(), stack: String = "0,0,0,0,0,0"): JsonElement
    fun downloadOpenVpnProfile(): String
}

/**
 * Coordinates TR-181 data model queries, actions, and configuration persistence for
 * OpenVPN, IPSec, and PPTP home servers on the router.
 */
class VpnEngine(private val rpc: VpnRouterRpc) {

    constructor(client: RouterClient) : this(object : VpnRouterRpc {
        override fun get(oid: String, attrs: Map<String, String>, stack: String) = client.get(oid, attrs, stack)
        override fun getList(oid: String, attrs: Map<String, String>, stack: String) = client.getList(oid, attrs, stack)
        override fun set(oid: String, attrs: Map<String, String>, stack: String) = client.set(oid, attrs, stack)
        override fun operate(oid: String, attrs: Map<String, String>, stack: String) = client.operate(oid, attrs, stack)
        override fun downloadOpenVpnProfile(): String = client.downloadOpenVpnProfile()
    })

    companion object {
        const val OID_OPENVPN = "DEV2_OPENVPN"
        const val ACT_REGEN_KEYS = "ACT_OP_OVPN_REGEN_KEYS"
        const val OID_OPENVPN_STATUS = "DEV2_OPENVPN_STATUS"
        const val OID_IPSEC = "DEV2_IPSEC"
        const val OID_IPSEC_CFG = "DEV2_IPSEC_CFG"
        const val OID_PPTP = "DEV2_PPTP"
    }

    /** Reads the active OpenVPN server configuration from the router. */
    fun loadOpenVpnConfig(): Result<OpenVpnConfig> = runCatching {
        val obj = toObject(rpc.get(OID_OPENVPN))
        val enable = obj.str("enable") == "1" || obj.str("primaryEnable") == "1"
        val proto = VpnProtocol.fromValue(obj.str("proto").ifEmpty { obj.str("protocol") })
        val port = obj.str("port").toIntOrNull() ?: 1194
        val subnet = obj.str("subnet").ifEmpty { "10.8.0.0" }
        val netmask = obj.str("netmask").ifEmpty { "255.255.255.0" }
        val access = OpenVpnAccessMode.fromValue(obj.str("access").ifEmpty { obj.str("accessMode") })
        val certStatus = obj.str("certStatus").ifEmpty { "Valid" }

        OpenVpnConfig(
            enable = enable,
            protocol = proto,
            port = port,
            subnet = subnet,
            netmask = netmask,
            accessMode = access,
            certificateStatus = certStatus,
        )
    }

    /** Persists OpenVPN server configuration to `DEV2_OPENVPN`. */
    fun saveOpenVpnConfig(config: OpenVpnConfig): Result<Unit> = runCatching {
        rpc.set(OID_OPENVPN, config.toMap())
    }

    /** Triggers regeneration of OpenVPN certificates and DH parameters. */
    fun regenerateOpenVpnKeys(): Result<Unit> = runCatching {
        rpc.operate(ACT_REGEN_KEYS)
    }

    /**
     * Downloads the `.ovpn` configuration text and applies an optional remote endpoint host override.
     */
    fun downloadOpenVpnProfile(hostOverride: String? = null): Result<String> = runCatching {
        val raw = rpc.downloadOpenVpnProfile()
        OpenVpnProfileTransformer.overrideRemoteHost(raw, hostOverride)
    }

    /** Reads active connected VPN sessions across OpenVPN, PPTP, and IPSec. */
    fun loadClientSessions(): Result<List<VpnClientSession>> = runCatching {
        val rawList = runCatching { toList(rpc.getList(OID_OPENVPN_STATUS)) }.getOrDefault(emptyList())
        rawList.mapIndexed { index, item ->
            val id = item.str("stack").ifEmpty { "vpn-client-$index" }
            val clientName = item.str("name").ifEmpty { item.str("username").ifEmpty { "Client #$index" } }
            val vpnType = VpnClientType.fromString(item.str("type").ifEmpty { item.str("proto") })
            val virtualIp = item.str("virtualIP").ifEmpty { item.str("ip") }
            val remoteIp = item.str("remoteIP").ifEmpty { item.str("peerIP") }
            val connectedSince = item.str("connectedTime").ifEmpty { item.str("uptime") }
            val bytesSent = item.str("bytesSent").toLongOrNull() ?: 0L
            val bytesRecv = item.str("bytesReceived").toLongOrNull() ?: 0L

            VpnClientSession(
                id = id,
                clientName = clientName,
                vpnType = vpnType,
                virtualIp = virtualIp,
                remoteIp = remoteIp,
                connectedSince = connectedSince,
                bytesSent = bytesSent,
                bytesReceived = bytesRecv,
            )
        }
    }

    /** Reads IPSec server configuration and configured tunnels. */
    fun loadIpsecConfig(): Result<IpsecConfig> = runCatching {
        val cfgObj = runCatching { toObject(rpc.get(OID_IPSEC_CFG)) }.getOrDefault(JsonObject(emptyMap()))
        val enable = cfgObj.str("enable") == "1"
        val tunnelsList = runCatching { toList(rpc.getList(OID_IPSEC)) }.getOrDefault(emptyList())

        val tunnels = tunnelsList.map { t ->
            IpsecTunnel(
                id = t.str("stack"),
                name = t.str("name"),
                remoteGateway = t.str("remoteGateway"),
                localSubnet = t.str("localSubnet"),
                remoteSubnet = t.str("remoteSubnet"),
                preSharedKey = t.str("psk"),
                ikeVersion = t.str("ikeVersion").ifEmpty { "IKEv1" },
                enable = t.str("enable") == "1",
            )
        }

        IpsecConfig(enable = enable, tunnels = tunnels)
    }

    /** Reads PPTP server configuration. */
    fun loadPptpConfig(): Result<PptpConfig> = runCatching {
        val obj = toObject(rpc.get(OID_PPTP))
        PptpConfig(
            enable = obj.str("enable") == "1",
            serverIp = obj.str("serverIP").ifEmpty { "10.7.0.1" },
            clientIpStart = obj.str("clientIPStart").ifEmpty { "10.7.0.100" },
            clientIpEnd = obj.str("clientIPEnd").ifEmpty { "10.7.0.200" },
            mppeEncryption = obj.str("mppe") != "0",
        )
    }

    private fun toObject(elem: JsonElement): JsonObject = when (elem) {
        is JsonObject -> elem
        is JsonArray -> elem.firstOrNull() as? JsonObject ?: JsonObject(emptyMap())
        else -> JsonObject(emptyMap())
    }

    private fun toList(elem: JsonElement): List<JsonObject> = when (elem) {
        is JsonArray -> elem.filterIsInstance<JsonObject>()
        is JsonObject -> listOf(elem)
        else -> emptyList()
    }

    private fun JsonObject.str(key: String): String =
        (get(key) as? JsonPrimitive)?.content?.trim() ?: ""
}
