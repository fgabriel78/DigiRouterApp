package es.routerapp.protocol.management

import es.routerapp.protocol.RouterClient
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * RPC abstraction over [RouterClient] enabling unit testing with fakes.
 */
interface RemoteManagementRouterRpc {
    fun get(oid: String, attrs: Map<String, String> = emptyMap(), stack: String = "0,0,0,0,0,0"): JsonElement
    fun getList(oid: String, attrs: Map<String, String> = emptyMap(), stack: String = "0,0,0,0,0,0"): JsonElement
    fun set(oid: String, attrs: Map<String, String>, stack: String = "0,0,0,0,0,0"): JsonElement
    fun downloadCaCert(): ByteArray
}

/**
 * Coordinates queries and configuration updates for SSH, Telnet, HTTP/HTTPS web management,
 * SSL CA certificate retrieval, and TR-069 ACS remote management audits.
 */
class RemoteManagementEngine(private val rpc: RemoteManagementRouterRpc) {

    constructor(client: RouterClient) : this(object : RemoteManagementRouterRpc {
        override fun get(oid: String, attrs: Map<String, String>, stack: String): JsonElement =
            client.get(oid, attrs, stack)

        override fun getList(oid: String, attrs: Map<String, String>, stack: String): JsonElement =
            client.getList(oid, attrs, stack)

        override fun set(oid: String, attrs: Map<String, String>, stack: String): JsonElement =
            client.set(oid, attrs, stack)

        override fun downloadCaCert(): ByteArray = client.downloadCaCert()
    })

    companion object {
        const val OID_SSH = "DEV2_SSH_CFG"
        const val OID_TELNET = "DEV2_TELNET_CFG"
        const val OID_HTTP = "DEV2_HTTP_CFG"
        const val OID_SUPER_HTTP = "DEV2_SUPER_HTTP_CFG"
        const val OID_MANAGEMENT_SERVER = "DEV2_MANAGEMENT_SERVER"
        const val OID_PAGE_PERMISSIONS = "DEV2_PAGE_ACCESS_PERMISSION"
    }

    /** Loads active SSH daemon configuration. */
    fun loadSshConfig(): Result<SshConfig> = runCatching {
        val obj = toObject(rpc.get(OID_SSH))
        SshConfig(
            localEnabled = obj.str("localEnabled") == "1",
            localPort = obj.str("localPort").toIntOrNull() ?: 22,
            remoteEnabled = obj.str("remoteEnabled") == "1",
            remotePort = obj.str("remotePort").toIntOrNull() ?: 22,
            sessionLifeTime = obj.str("sessionLifeTime").toIntOrNull() ?: 10,
            remoteAll = obj.str("remoteAll") != "0",
            remoteHost = obj.str("remoteHost").split(",").map { it.trim() }.filter { it.isNotEmpty() },
        )
    }

    /** Persists SSH daemon configuration. */
    fun saveSshConfig(config: SshConfig): Result<Unit> = runCatching {
        rpc.set(OID_SSH, config.toMap())
    }

    /** Loads active Telnet daemon configuration. */
    fun loadTelnetConfig(): Result<TelnetConfig> = runCatching {
        val obj = toObject(rpc.get(OID_TELNET))
        TelnetConfig(
            localEnabled = obj.str("telnetLocalEnabled") == "1",
            localPort = obj.str("telnetLocalPort").toIntOrNull() ?: 23,
            remoteEnabled = obj.str("telnetRemoteEnabled") == "1",
            remotePort = obj.str("telnetRemotePort").toIntOrNull() ?: 23,
            remoteAll = obj.str("telnetRemoteAll") != "0",
            remoteHost = obj.str("telnetRemoteHost").split(",").map { it.trim() }.filter { it.isNotEmpty() },
        )
    }

    /** Persists Telnet daemon configuration. */
    fun saveTelnetConfig(config: TelnetConfig): Result<Unit> = runCatching {
        rpc.set(OID_TELNET, config.toMap())
    }

    /** Loads active Web Management and SSL parameters. */
    fun loadHttpConfig(): Result<HttpConfig> = runCatching {
        val obj = toObject(rpc.get(OID_HTTP))
        HttpConfig(
            httpLocalEnabled = obj.str("httpLocalEnabled") != "0",
            httpLocalPort = obj.str("httpLocalPort").toIntOrNull() ?: 80,
            httpsLocalEnabled = obj.str("httpsLocalEnabled") != "0",
            httpsLocalPort = obj.str("httpsLocalPort").toIntOrNull() ?: 443,
            httpRemoteEnabled = obj.str("httpRemoteEnabled") == "1",
            httpRemotePort = obj.str("httpRemotePort").toIntOrNull() ?: 80,
            httpsRemoteEnabled = obj.str("httpsRemoteEnabled") == "1",
            httpsRemotePort = obj.str("httpsRemotePort").toIntOrNull() ?: 443,
            remoteAll = obj.str("remoteAll") != "0",
            remoteHost = obj.str("remoteHost").split(",").map { it.trim() }.filter { it.isNotEmpty() },
            webSessionTimeout = obj.str("webSessionTimeout").toIntOrNull() ?: 10,
            certExist = obj.str("certExist") == "1",
            certSerialNumber = obj.str("certSerialNumber"),
        )
    }

    /** Persists Web Management parameters. */
    fun saveHttpConfig(config: HttpConfig): Result<Unit> = runCatching {
        rpc.set(OID_HTTP, config.toMap())
    }

    /** Loads Super HTTP operator configuration (if accessible). */
    fun loadSuperHttpConfig(): Result<SuperHttpConfig> = runCatching {
        val obj = toObject(rpc.get(OID_SUPER_HTTP))
        SuperHttpConfig(
            sHttpRemoteEnabled = obj.str("SHttpRemoteEnabled") == "1",
            sHttpRemotePort = obj.str("SHttpRemotePort").toIntOrNull() ?: 80,
            sHttpsRemoteEnabled = obj.str("SHttpsRemoteEnabled") == "1",
            sHttpsRemotePort = obj.str("SHttpsRemotePort").toIntOrNull() ?: 443,
        )
    }

    /** Reads operator TR-069 ACS configuration and operator subnets. */
    fun loadIspAudit(): Result<IspManagementAudit> = runCatching {
        val mgmtObj = runCatching { toObject(rpc.get(OID_MANAGEMENT_SERVER)) }.getOrDefault(JsonObject(emptyMap()))
        val httpObj = runCatching { toObject(rpc.get(OID_HTTP)) }.getOrDefault(JsonObject(emptyMap()))
        val permsList = runCatching { toList(rpc.getList(OID_PAGE_PERMISSIONS)) }.getOrDefault(emptyList())

        val cwmpPerm = permsList.firstOrNull { it.str("name") == "cwmp" }

        val cwmpEnabled = mgmtObj.str("enableCWMP") != "0"
        val acsUrl = mgmtObj.str("URL")
        val boundIf = mgmtObj.str("X_TP_BoundIfName").ifEmpty { "pon.2" }
        val informInterval = mgmtObj.str("PeriodicInformInterval").toIntOrNull() ?: 86400
        val connReqPort = mgmtObj.str("X_TP_ConnReqPort").toIntOrNull() ?: 7547
        val operatorSubnets = httpObj.str("remoteHost").split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val cwmpVis = cwmpPerm?.str("visibilityLevel")?.ifEmpty { "1" } ?: "1"
        val cwmpEdit = cwmpPerm?.str("editable") == "1"

        IspManagementAudit(
            cwmpEnabled = cwmpEnabled,
            acsUrl = acsUrl,
            boundInterface = boundIf,
            informInterval = informInterval,
            connReqPort = connReqPort,
            tr369Supported = false, // Inactive / disabled on this firmware build (INCLUDE_TR369=0)
            operatorSubnets = operatorSubnets,
            cwmpVisibilityLevel = cwmpVis,
            cwmpEditable = cwmpEdit,
        )
    }

    /** Loads consolidated remote management overview with graceful degradation on optional OIDs. */
    fun loadOverview(): Result<RemoteManagementOverview> = runCatching {
        val ssh = loadSshConfig().getOrDefault(SshConfig())
        val telnet = loadTelnetConfig().getOrDefault(TelnetConfig())
        val http = loadHttpConfig().getOrDefault(HttpConfig())
        val superHttp = loadSuperHttpConfig().getOrNull()
        val ispAudit = loadIspAudit().getOrDefault(IspManagementAudit())

        RemoteManagementOverview(
            ssh = ssh,
            telnet = telnet,
            http = http,
            superHttp = superHttp,
            ispAudit = ispAudit,
        )
    }

    /** Downloads raw CA root certificate bytes from `/cgi/https?downCACert`. */
    fun downloadCaCert(): Result<ByteArray> = runCatching {
        rpc.downloadCaCert()
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
