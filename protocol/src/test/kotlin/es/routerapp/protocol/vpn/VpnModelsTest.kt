package es.routerapp.protocol.vpn

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VpnModelsTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun openVpnConfigSerializationAndToMap() {
        val config = OpenVpnConfig(
            enable = true,
            protocol = VpnProtocol.UDP,
            port = 1194,
            subnet = "10.8.0.0",
            netmask = "255.255.255.0",
            accessMode = OpenVpnAccessMode.INTERNET_AND_HOME_NETWORK,
        )

        val serialized = json.encodeToString(config)
        val decoded = json.decodeFromString<OpenVpnConfig>(serialized)
        assertEquals(config, decoded)

        val map = config.toMap()
        assertEquals("1", map["enable"])
        assertEquals("UDP", map["proto"])
        assertEquals("1194", map["port"])
        assertEquals("10.8.0.0", map["subnet"])
        assertEquals("255.255.255.0", map["netmask"])
        assertEquals("all", map["access"])
    }

    @Test
    fun vpnClientTypeDetection() {
        assertEquals(VpnClientType.OPENVPN, VpnClientType.fromString("OpenVPN-Client"))
        assertEquals(VpnClientType.IPSEC, VpnClientType.fromString("IPSec_Tunnel_1"))
        assertEquals(VpnClientType.PPTP, VpnClientType.fromString("pptp_user"))
        assertEquals(VpnClientType.OPENVPN, VpnClientType.fromString("unknown"))
    }

    @Test
    fun clientSessionSerialization() {
        val session = VpnClientSession(
            id = "sess-1",
            clientName = "Pixel_8",
            vpnType = VpnClientType.OPENVPN,
            virtualIp = "10.8.0.2",
            remoteIp = "84.120.55.10",
            connectedSince = "2026-10-22 07:15:00",
            bytesSent = 1048576L,
            bytesReceived = 4194304L,
        )

        val serialized = json.encodeToString(session)
        val decoded = json.decodeFromString<VpnClientSession>(serialized)
        assertEquals(session, decoded)
    }
}
