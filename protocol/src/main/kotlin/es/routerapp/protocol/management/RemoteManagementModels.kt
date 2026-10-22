package es.routerapp.protocol.management

import kotlinx.serialization.Serializable

/**
 * Configuration parameters for the router's SSH daemon (`DEV2_SSH_CFG`).
 */
@Serializable
data class SshConfig(
    val localEnabled: Boolean = true,
    val localPort: Int = 22,
    val remoteEnabled: Boolean = false,
    val remotePort: Int = 22,
    val sessionLifeTime: Int = 10,
    val remoteAll: Boolean = true,
    val remoteHost: List<String> = emptyList(),
) {
    fun toMap(): Map<String, String> = buildMap {
        put("localEnabled", if (localEnabled) "1" else "0")
        put("localPort", localPort.toString())
        put("remoteEnabled", if (remoteEnabled) "1" else "0")
        put("remotePort", remotePort.toString())
        put("sessionLifeTime", sessionLifeTime.toString())
        put("remoteAll", if (remoteAll) "1" else "0")
        put("remoteHost", remoteHost.joinToString(","))
    }

    companion object {
        fun fromMap(m: Map<String, String>): SshConfig = SshConfig(
            localEnabled = m["localEnabled"] == "1",
            localPort = m["localPort"]?.toIntOrNull() ?: 22,
            remoteEnabled = m["remoteEnabled"] == "1",
            remotePort = m["remotePort"]?.toIntOrNull() ?: 22,
            sessionLifeTime = m["sessionLifeTime"]?.toIntOrNull() ?: 10,
            remoteAll = m["remoteAll"] != "0",
            remoteHost = m["remoteHost"]?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList(),
        )
    }
}

/**
 * Configuration parameters for the router's Telnet daemon (`DEV2_TELNET_CFG`).
 */
@Serializable
data class TelnetConfig(
    val localEnabled: Boolean = false,
    val localPort: Int = 23,
    val remoteEnabled: Boolean = false,
    val remotePort: Int = 23,
    val remoteAll: Boolean = true,
    val remoteHost: List<String> = emptyList(),
) {
    fun toMap(): Map<String, String> = buildMap {
        put("telnetLocalEnabled", if (localEnabled) "1" else "0")
        put("telnetLocalPort", localPort.toString())
        put("telnetRemoteEnabled", if (remoteEnabled) "1" else "0")
        put("telnetRemotePort", remotePort.toString())
        put("telnetRemoteAll", if (remoteAll) "1" else "0")
        put("telnetRemoteHost", remoteHost.joinToString(","))
    }

    companion object {
        fun fromMap(m: Map<String, String>): TelnetConfig = TelnetConfig(
            localEnabled = m["telnetLocalEnabled"] == "1",
            localPort = m["telnetLocalPort"]?.toIntOrNull() ?: 23,
            remoteEnabled = m["telnetRemoteEnabled"] == "1",
            remotePort = m["telnetRemotePort"]?.toIntOrNull() ?: 23,
            remoteAll = m["telnetRemoteAll"] != "0",
            remoteHost = m["telnetRemoteHost"]?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList(),
        )
    }
}

/**
 * Configuration parameters for Web Management and SSL (`DEV2_HTTP_CFG`).
 */
@Serializable
data class HttpConfig(
    val httpLocalEnabled: Boolean = true,
    val httpLocalPort: Int = 80,
    val httpsLocalEnabled: Boolean = true,
    val httpsLocalPort: Int = 443,
    val httpRemoteEnabled: Boolean = false,
    val httpRemotePort: Int = 80,
    val httpsRemoteEnabled: Boolean = false,
    val httpsRemotePort: Int = 443,
    val remoteAll: Boolean = true,
    val remoteHost: List<String> = emptyList(),
    val webSessionTimeout: Int = 10,
    val certExist: Boolean = true,
    val certSerialNumber: String = "",
) {
    fun toMap(): Map<String, String> = buildMap {
        put("httpLocalEnabled", if (httpLocalEnabled) "1" else "0")
        put("httpLocalPort", httpLocalPort.toString())
        put("httpsLocalEnabled", if (httpsLocalEnabled) "1" else "0")
        put("httpsLocalPort", httpsLocalPort.toString())
        put("httpRemoteEnabled", if (httpRemoteEnabled) "1" else "0")
        put("httpRemotePort", httpRemotePort.toString())
        put("httpsRemoteEnabled", if (httpsRemoteEnabled) "1" else "0")
        put("httpsRemotePort", httpsRemotePort.toString())
        put("remoteAll", if (remoteAll) "1" else "0")
        put("remoteHost", remoteHost.joinToString(","))
        put("webSessionTimeout", webSessionTimeout.toString())
    }

    companion object {
        fun fromMap(m: Map<String, String>): HttpConfig = HttpConfig(
            httpLocalEnabled = m["httpLocalEnabled"] != "0",
            httpLocalPort = m["httpLocalPort"]?.toIntOrNull() ?: 80,
            httpsLocalEnabled = m["httpsLocalEnabled"] != "0",
            httpsLocalPort = m["httpsLocalPort"]?.toIntOrNull() ?: 443,
            httpRemoteEnabled = m["httpRemoteEnabled"] == "1",
            httpRemotePort = m["httpRemotePort"]?.toIntOrNull() ?: 80,
            httpsRemoteEnabled = m["httpsRemoteEnabled"] == "1",
            httpsRemotePort = m["httpsRemotePort"]?.toIntOrNull() ?: 443,
            remoteAll = m["remoteAll"] != "0",
            remoteHost = m["remoteHost"]?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList(),
            webSessionTimeout = m["webSessionTimeout"]?.toIntOrNull() ?: 10,
            certExist = m["certExist"] == "1",
            certSerialNumber = m["certSerialNumber"].orEmpty(),
        )
    }
}

/**
 * Superadmin / ISP remote HTTP management ports (`DEV2_SUPER_HTTP_CFG`).
 */
@Serializable
data class SuperHttpConfig(
    val sHttpRemoteEnabled: Boolean = false,
    val sHttpRemotePort: Int = 80,
    val sHttpsRemoteEnabled: Boolean = false,
    val sHttpsRemotePort: Int = 443,
) {
    fun toMap(): Map<String, String> = buildMap {
        put("SHttpRemoteEnabled", if (sHttpRemoteEnabled) "1" else "0")
        put("SHttpRemotePort", sHttpRemotePort.toString())
        put("SHttpsRemoteEnabled", if (sHttpsRemoteEnabled) "1" else "0")
        put("SHttpsRemotePort", sHttpsRemotePort.toString())
    }

    companion object {
        fun fromMap(m: Map<String, String>): SuperHttpConfig = SuperHttpConfig(
            sHttpRemoteEnabled = m["SHttpRemoteEnabled"] == "1",
            sHttpRemotePort = m["SHttpRemotePort"]?.toIntOrNull() ?: 80,
            sHttpsRemoteEnabled = m["SHttpsRemoteEnabled"] == "1",
            sHttpsRemotePort = m["SHttpsRemotePort"]?.toIntOrNull() ?: 443,
        )
    }
}

/**
 * Read-only audit data for operator remote management (TR-069 CWMP and TR-369).
 */
@Serializable
data class IspManagementAudit(
    val cwmpEnabled: Boolean = true,
    val acsUrl: String = "",
    val boundInterface: String = "pon.2",
    val informInterval: Int = 86400,
    val connReqPort: Int = 7547,
    val tr369Supported: Boolean = false,
    val operatorSubnets: List<String> = emptyList(),
    val cwmpVisibilityLevel: String = "1",
    val cwmpEditable: Boolean = false,
)

/**
 * Consolidated aggregate state for the remote management hub.
 */
@Serializable
data class RemoteManagementOverview(
    val ssh: SshConfig = SshConfig(),
    val telnet: TelnetConfig = TelnetConfig(),
    val http: HttpConfig = HttpConfig(),
    val superHttp: SuperHttpConfig? = null,
    val ispAudit: IspManagementAudit = IspManagementAudit(),
)
