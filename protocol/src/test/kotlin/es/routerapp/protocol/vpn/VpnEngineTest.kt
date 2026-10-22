package es.routerapp.protocol.vpn

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VpnEngineTest {

    private class FakeVpnRpc : VpnRouterRpc {
        val gets = mutableMapOf<String, JsonElement>()
        val sets = mutableListOf<Pair<String, Map<String, String>>>()
        val ops = mutableListOf<String>()
        var ovpnProfileContent = "client\nremote 192.168.1.1 1194\nproto udp\n"

        override fun get(oid: String, attrs: Map<String, String>, stack: String): JsonElement =
            gets[oid] ?: JsonObject(emptyMap())

        override fun getList(oid: String, attrs: Map<String, String>, stack: String): JsonElement =
            gets[oid] ?: JsonArray(emptyList())

        override fun set(oid: String, attrs: Map<String, String>, stack: String): JsonElement {
            sets.add(oid to attrs)
            return JsonObject(emptyMap())
        }

        override fun operate(oid: String, attrs: Map<String, String>, stack: String): JsonElement {
            ops.add(oid)
            return JsonObject(emptyMap())
        }

        override fun downloadOpenVpnProfile(): String = ovpnProfileContent
    }

    @Test
    fun loadAndSaveOpenVpnConfig() {
        val rpc = FakeVpnRpc()
        rpc.gets[VpnEngine.OID_OPENVPN] = JsonObject(
            mapOf(
                "enable" to JsonPrimitive("1"),
                "proto" to JsonPrimitive("UDP"),
                "port" to JsonPrimitive("1194"),
                "subnet" to JsonPrimitive("10.8.0.0"),
                "netmask" to JsonPrimitive("255.255.255.0"),
                "access" to JsonPrimitive("all"),
                "certStatus" to JsonPrimitive("Valid"),
            )
        )

        val engine = VpnEngine(rpc)
        val config = engine.loadOpenVpnConfig().getOrThrow()

        assertTrue(config.enable)
        assertEquals(VpnProtocol.UDP, config.protocol)
        assertEquals(1194, config.port)
        assertEquals("10.8.0.0", config.subnet)
        assertEquals(OpenVpnAccessMode.INTERNET_AND_HOME_NETWORK, config.accessMode)

        // Save modification
        val updated = config.copy(port = 1195, protocol = VpnProtocol.TCP)
        engine.saveOpenVpnConfig(updated).getOrThrow()

        assertEquals(1, rpc.sets.size)
        assertEquals(VpnEngine.OID_OPENVPN, rpc.sets[0].first)
        assertEquals("1195", rpc.sets[0].second["port"])
        assertEquals("TCP", rpc.sets[0].second["proto"])
    }

    @Test
    fun regenerateOpenVpnKeysCallsCorrectAction() {
        val rpc = FakeVpnRpc()
        val engine = VpnEngine(rpc)
        engine.regenerateOpenVpnKeys().getOrThrow()

        assertEquals(listOf(VpnEngine.ACT_REGEN_KEYS), rpc.ops)
    }

    @Test
    fun downloadProfileWithHostOverride() {
        val rpc = FakeVpnRpc()
        val engine = VpnEngine(rpc)

        val profile = engine.downloadOpenVpnProfile("myhome.duckdns.org").getOrThrow()
        assertEquals("myhome.duckdns.org", OpenVpnProfileTransformer.extractRemoteHost(profile))
    }

    @Test
    fun loadClientSessionsParsesTable() {
        val rpc = FakeVpnRpc()
        rpc.gets[VpnEngine.OID_OPENVPN_STATUS] = JsonArray(
            listOf(
                JsonObject(
                    mapOf(
                        "name" to JsonPrimitive("Laptop"),
                        "type" to JsonPrimitive("OpenVPN"),
                        "virtualIP" to JsonPrimitive("10.8.0.2"),
                        "remoteIP" to JsonPrimitive("84.120.1.5"),
                        "connectedTime" to JsonPrimitive("01:23:45"),
                        "bytesSent" to JsonPrimitive("1024"),
                        "bytesReceived" to JsonPrimitive("2048"),
                    )
                )
            )
        )

        val engine = VpnEngine(rpc)
        val sessions = engine.loadClientSessions().getOrThrow()

        assertEquals(1, sessions.size)
        assertEquals("Laptop", sessions[0].clientName)
        assertEquals(VpnClientType.OPENVPN, sessions[0].vpnType)
        assertEquals("10.8.0.2", sessions[0].virtualIp)
        assertEquals(1024L, sessions[0].bytesSent)
        assertEquals(2048L, sessions[0].bytesReceived)
    }
}
