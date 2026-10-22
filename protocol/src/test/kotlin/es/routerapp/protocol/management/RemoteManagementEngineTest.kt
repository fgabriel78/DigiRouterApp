package es.routerapp.protocol.management

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteManagementEngineTest {

    private class FakeRemoteManagementRpc : RemoteManagementRouterRpc {
        val gets = mutableMapOf<String, JsonElement>()
        val sets = mutableListOf<Pair<String, Map<String, String>>>()
        var certBytes = byteArrayOf(0x30, 0x82.toByte(), 0x01, 0x0a)

        override fun get(oid: String, attrs: Map<String, String>, stack: String): JsonElement =
            gets[oid] ?: JsonObject(emptyMap())

        override fun getList(oid: String, attrs: Map<String, String>, stack: String): JsonElement =
            gets[oid] ?: JsonArray(emptyList())

        override fun set(oid: String, attrs: Map<String, String>, stack: String): JsonElement {
            sets.add(oid to attrs)
            return JsonObject(emptyMap())
        }

        override fun downloadCaCert(): ByteArray = certBytes
    }

    @Test
    fun loadAndSaveSshConfig() {
        val rpc = FakeRemoteManagementRpc()
        rpc.gets[RemoteManagementEngine.OID_SSH] = JsonObject(
            mapOf(
                "localEnabled" to JsonPrimitive("1"),
                "localPort" to JsonPrimitive("2222"),
                "remoteEnabled" to JsonPrimitive("0"),
                "remotePort" to JsonPrimitive("2222"),
                "sessionLifeTime" to JsonPrimitive("15"),
                "remoteAll" to JsonPrimitive("0"),
                "remoteHost" to JsonPrimitive("192.168.1.50/32,10.0.0.0/24"),
            )
        )

        val engine = RemoteManagementEngine(rpc)
        val ssh = engine.loadSshConfig().getOrThrow()

        assertTrue(ssh.localEnabled)
        assertEquals(2222, ssh.localPort)
        assertFalse(ssh.remoteEnabled)
        assertEquals(15, ssh.sessionLifeTime)
        assertFalse(ssh.remoteAll)
        assertEquals(listOf("192.168.1.50/32", "10.0.0.0/24"), ssh.remoteHost)

        val updated = ssh.copy(remoteEnabled = true, remotePort = 2200)
        engine.saveSshConfig(updated).getOrThrow()

        assertEquals(1, rpc.sets.size)
        val (oid, map) = rpc.sets.first()
        assertEquals(RemoteManagementEngine.OID_SSH, oid)
        assertEquals("1", map["remoteEnabled"])
        assertEquals("2200", map["remotePort"])
    }

    @Test
    fun loadAndSaveTelnetConfig() {
        val rpc = FakeRemoteManagementRpc()
        rpc.gets[RemoteManagementEngine.OID_TELNET] = JsonObject(
            mapOf(
                "telnetLocalEnabled" to JsonPrimitive("0"),
                "telnetLocalPort" to JsonPrimitive("23"),
                "telnetRemoteEnabled" to JsonPrimitive("0"),
                "telnetRemotePort" to JsonPrimitive("2323"),
                "telnetRemoteAll" to JsonPrimitive("1"),
                "telnetRemoteHost" to JsonPrimitive(""),
            )
        )

        val engine = RemoteManagementEngine(rpc)
        val telnet = engine.loadTelnetConfig().getOrThrow()

        assertFalse(telnet.localEnabled)
        assertEquals(23, telnet.localPort)
        assertFalse(telnet.remoteEnabled)
        assertEquals(2323, telnet.remotePort)
        assertTrue(telnet.remoteAll)

        val enabled = telnet.copy(localEnabled = true)
        engine.saveTelnetConfig(enabled).getOrThrow()

        assertEquals(1, rpc.sets.size)
        val (oid, map) = rpc.sets.first()
        assertEquals(RemoteManagementEngine.OID_TELNET, oid)
        assertEquals("1", map["telnetLocalEnabled"])
    }

    @Test
    fun loadAndSaveHttpConfig() {
        val rpc = FakeRemoteManagementRpc()
        rpc.gets[RemoteManagementEngine.OID_HTTP] = JsonObject(
            mapOf(
                "httpLocalEnabled" to JsonPrimitive("1"),
                "httpLocalPort" to JsonPrimitive("80"),
                "httpsLocalEnabled" to JsonPrimitive("1"),
                "httpsLocalPort" to JsonPrimitive("443"),
                "httpRemoteEnabled" to JsonPrimitive("0"),
                "httpRemotePort" to JsonPrimitive("8080"),
                "httpsRemoteEnabled" to JsonPrimitive("1"),
                "httpsRemotePort" to JsonPrimitive("8443"),
                "remoteAll" to JsonPrimitive("0"),
                "remoteHost" to JsonPrimitive("188.26.208.0/26"),
                "webSessionTimeout" to JsonPrimitive("20"),
                "certExist" to JsonPrimitive("1"),
                "certSerialNumber" to JsonPrimitive("987654321"),
            )
        )

        val engine = RemoteManagementEngine(rpc)
        val http = engine.loadHttpConfig().getOrThrow()

        assertTrue(http.httpLocalEnabled)
        assertEquals(80, http.httpLocalPort)
        assertTrue(http.httpsLocalEnabled)
        assertEquals(443, http.httpsLocalPort)
        assertFalse(http.httpRemoteEnabled)
        assertTrue(http.httpsRemoteEnabled)
        assertEquals(8443, http.httpsRemotePort)
        assertFalse(http.remoteAll)
        assertEquals(listOf("188.26.208.0/26"), http.remoteHost)
        assertEquals(20, http.webSessionTimeout)
        assertTrue(http.certExist)
        assertEquals("987654321", http.certSerialNumber)

        engine.saveHttpConfig(http.copy(webSessionTimeout = 30)).getOrThrow()
        assertEquals(1, rpc.sets.size)
        assertEquals("30", rpc.sets.first().second["webSessionTimeout"])
    }

    @Test
    fun loadIspAudit() {
        val rpc = FakeRemoteManagementRpc()
        rpc.gets[RemoteManagementEngine.OID_MANAGEMENT_SERVER] = JsonObject(
            mapOf(
                "enableCWMP" to JsonPrimitive("1"),
                "URL" to JsonPrimitive("https://acs.digi.es:7547/acs"),
                "X_TP_BoundIfName" to JsonPrimitive("pon.2"),
                "PeriodicInformInterval" to JsonPrimitive("86400"),
                "X_TP_ConnReqPort" to JsonPrimitive("7547"),
            )
        )
        rpc.gets[RemoteManagementEngine.OID_HTTP] = JsonObject(
            mapOf(
                "remoteHost" to JsonPrimitive("10.13.255.2/32,188.26.208.0/26"),
            )
        )
        rpc.gets[RemoteManagementEngine.OID_PAGE_PERMISSIONS] = JsonArray(
            listOf(
                JsonObject(
                    mapOf(
                        "name" to JsonPrimitive("cwmp"),
                        "visibilityLevel" to JsonPrimitive("1"),
                        "editable" to JsonPrimitive("0"),
                    )
                )
            )
        )

        val engine = RemoteManagementEngine(rpc)
        val audit = engine.loadIspAudit().getOrThrow()

        assertTrue(audit.cwmpEnabled)
        assertEquals("https://acs.digi.es:7547/acs", audit.acsUrl)
        assertEquals("pon.2", audit.boundInterface)
        assertEquals(86400, audit.informInterval)
        assertEquals(7547, audit.connReqPort)
        assertFalse(audit.tr369Supported)
        assertEquals(listOf("10.13.255.2/32", "188.26.208.0/26"), audit.operatorSubnets)
        assertEquals("1", audit.cwmpVisibilityLevel)
        assertFalse(audit.cwmpEditable)
    }

    @Test
    fun loadOverviewAggregatesGracefully() {
        val rpc = FakeRemoteManagementRpc()
        rpc.gets[RemoteManagementEngine.OID_SUPER_HTTP] = JsonObject(
            mapOf(
                "SHttpRemoteEnabled" to JsonPrimitive("1"),
                "SHttpRemotePort" to JsonPrimitive("8081"),
                "SHttpsRemoteEnabled" to JsonPrimitive("1"),
                "SHttpsRemotePort" to JsonPrimitive("8444"),
            )
        )

        val engine = RemoteManagementEngine(rpc)
        val overview = engine.loadOverview().getOrThrow()

        assertNotNull(overview.ssh)
        assertNotNull(overview.telnet)
        assertNotNull(overview.http)
        assertNotNull(overview.superHttp)
        assertEquals(8081, overview.superHttp?.sHttpRemotePort)
        assertNotNull(overview.ispAudit)
    }

    @Test
    fun downloadCaCertReturnsBytes() {
        val rpc = FakeRemoteManagementRpc()
        rpc.certBytes = byteArrayOf(0x01, 0x02, 0x03)

        val engine = RemoteManagementEngine(rpc)
        val bytes = engine.downloadCaCert().getOrThrow()

        assertEquals(3, bytes.size)
        assertEquals(0x01.toByte(), bytes[0])
    }
}
