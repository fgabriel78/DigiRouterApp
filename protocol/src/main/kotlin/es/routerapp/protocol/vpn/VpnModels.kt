package es.routerapp.protocol.vpn

import kotlinx.serialization.Serializable

/** Transport protocol used by OpenVPN Server. */
@Serializable
enum class VpnProtocol(val value: String) {
    UDP("UDP"),
    TCP("TCP");

    companion object {
        fun fromValue(v: String?): VpnProtocol =
            entries.firstOrNull { it.value.equals(v, ignoreCase = true) } ?: UDP
    }
}

/** Client routing permission when connecting through the home OpenVPN tunnel. */
@Serializable
enum class OpenVpnAccessMode(val value: String) {
    HOME_NETWORK_ONLY("home"),
    INTERNET_AND_HOME_NETWORK("all");

    companion object {
        fun fromValue(v: String?): OpenVpnAccessMode =
            entries.firstOrNull { it.value.equals(v, ignoreCase = true) } ?: INTERNET_AND_HOME_NETWORK
    }
}

/** Configuration parameters for TP-Link TR-181 `DEV2_OPENVPN`. */
@Serializable
data class OpenVpnConfig(
    val enable: Boolean = false,
    val protocol: VpnProtocol = VpnProtocol.UDP,
    val port: Int = 1194,
    val subnet: String = "10.8.0.0",
    val netmask: String = "255.255.255.0",
    val accessMode: OpenVpnAccessMode = OpenVpnAccessMode.INTERNET_AND_HOME_NETWORK,
    val certificateStatus: String = "Valid",
) {
    fun toMap(): Map<String, String> = mapOf(
        "enable" to if (enable) "1" else "0",
        "proto" to protocol.value,
        "port" to port.toString(),
        "subnet" to subnet,
        "netmask" to netmask,
        "access" to accessMode.value,
    )
}

/** Individual IPSec VPN Tunnel configuration. */
@Serializable
data class IpsecTunnel(
    val id: String = "",
    val name: String = "",
    val remoteGateway: String = "",
    val localSubnet: String = "",
    val remoteSubnet: String = "",
    val preSharedKey: String = "",
    val ikeVersion: String = "IKEv1",
    val enable: Boolean = true,
)

/** Configuration parameters for TP-Link TR-181 `DEV2_IPSEC`. */
@Serializable
data class IpsecConfig(
    val enable: Boolean = false,
    val tunnels: List<IpsecTunnel> = emptyList(),
)

/** PPTP VPN Server user credentials. */
@Serializable
data class PptpAccount(
    val username: String = "",
    val password: String = "",
)

/** Configuration parameters for TP-Link TR-181 `DEV2_PPTP`. */
@Serializable
data class PptpConfig(
    val enable: Boolean = false,
    val serverIp: String = "10.7.0.1",
    val clientIpStart: String = "10.7.0.100",
    val clientIpEnd: String = "10.7.0.200",
    val mppeEncryption: Boolean = true,
    val accounts: List<PptpAccount> = emptyList(),
)

/** Protocol classification for active remote VPN client sessions. */
@Serializable
enum class VpnClientType(val label: String) {
    OPENVPN("OpenVPN"),
    IPSEC("IPSec"),
    PPTP("PPTP");

    companion object {
        fun fromString(s: String?): VpnClientType = when {
            s?.contains("open", ignoreCase = true) == true -> OPENVPN
            s?.contains("ipsec", ignoreCase = true) == true -> IPSEC
            s?.contains("pptp", ignoreCase = true) == true -> PPTP
            else -> OPENVPN
        }
    }
}

/** Active client session connected to any of the router's hosted VPN servers. */
@Serializable
data class VpnClientSession(
    val id: String,
    val clientName: String,
    val vpnType: VpnClientType,
    val virtualIp: String,
    val remoteIp: String,
    val connectedSince: String,
    val bytesSent: Long = 0L,
    val bytesReceived: Long = 0L,
)
