package es.routerapp.protocol.qos

import es.routerapp.protocol.pages.Catalog
import es.routerapp.protocol.pages.Group
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QosEngineTest {

    private class FakeRpc(
        var iqos: Map<String, String> = DEFAULT_IQOS,
        var rules: List<Map<String, String>> = emptyList(),
        var gameClients: List<Map<String, String>>? = emptyList(),
    ) : QosRouterRpc {
        val log = mutableListOf<String>()
        val sets = mutableListOf<Pair<String, Map<String, String>>>()
        val adds = mutableListOf<Pair<String, Map<String, String>>>()
        val deletes = mutableListOf<Pair<String, String>>()

        private fun obj(m: Map<String, String>) = JsonObject(m.mapValues { JsonPrimitive(it.value) })

        override fun get(oid: String, attrs: Map<String, String>, stack: String): JsonElement {
            log += "get:$oid"
            return if (oid == "DEV2_IQOS") obj(iqos) else JsonObject(emptyMap())
        }

        override fun getList(oid: String, attrs: Map<String, String>, stack: String): JsonElement {
            log += "getList:$oid"
            return when (oid) {
                "DEV2_IQOS_RULE" -> JsonArray(rules.map { obj(it) })
                "DEV2_GAME_QOS_CLIENT" -> gameClients?.let { list -> JsonArray(list.map { obj(it) }) }
                    ?: throw IllegalStateException("error 9999")
                else -> JsonArray(emptyList())
            }
        }

        override fun set(oid: String, attrs: Map<String, String>, stack: String): JsonElement {
            log += "set:$oid"; sets += oid to attrs; return JsonObject(emptyMap())
        }

        override fun add(oid: String, attrs: Map<String, String>, parentStack: String): JsonElement {
            log += "add:$oid"; adds += oid to attrs; return JsonObject(emptyMap())
        }

        override fun delete(oid: String, stack: String): JsonElement {
            log += "delete:$oid:$stack"; deletes += oid to stack; return JsonObject(emptyMap())
        }

        override fun operate(oid: String, attrs: Map<String, String>, stack: String): JsonElement {
            log += "op:$oid"; return JsonObject(emptyMap())
        }
    }

    companion object {
        val DEFAULT_IQOS = mapOf(
            "enable" to "1", "linkType" to "0",
            "upTotalBW" to "100", "upUnit" to "1", "downTotalBW" to "600", "downUnit" to "1",
            "high" to "60", "middle" to "30", "low" to "10",
            "iptvEnable" to "1", "iptvClass" to "4", "iptvUnit" to "1", "iptvBW" to "1",
            "voIPEnable" to "0", "voIPClass" to "3",
            "guestEnable" to "1", "guestClass" to "2", "version" to "1.2.3",
        )

        fun deviceRule(stack: String, mac: String, cls: Int, name: String = "dev") =
            mapOf("stack" to stack, "type" to "11", "class_" to cls.toString(), "macAddress" to mac, "deviceName" to name)
    }

    // ---------- DEV2_IQOS ----------

    @Test
    fun `loadConfig parses global settings`() {
        val cfg = QosEngine(FakeRpc()).loadConfig().getOrThrow()
        assertTrue(cfg.enabled)
        assertEquals(100, cfg.upBandwidth)
        assertEquals(BandwidthUnit.MBPS, cfg.upUnit)
        assertEquals(600, cfg.downBandwidth)
        assertEquals(QueueAllocation(60, 30, 10), cfg.queues)
        assertEquals("1.2.3", cfg.raw["version"])
    }

    @Test
    fun `loadConfig maps kbps unit and missing values`() {
        val rpc = FakeRpc(iqos = mapOf("enable" to "0", "upUnit" to "2", "downUnit" to "2"))
        val cfg = QosEngine(rpc).loadConfig().getOrThrow()
        assertFalse(cfg.enabled)
        assertEquals(BandwidthUnit.KBPS, cfg.upUnit)
        assertEquals(BandwidthUnit.KBPS, cfg.downUnit)
        assertEquals(0, cfg.upBandwidth)
        assertEquals(0, cfg.downBandwidth)
    }

    @Test
    fun `saveConfig writes managed fields, preserves IPTV and VoIP, and commits`() {
        val rpc = FakeRpc()
        val engine = QosEngine(rpc)
        val cfg = engine.loadConfig().getOrThrow().copy(
            enabled = false, upBandwidth = 50, upUnit = BandwidthUnit.KBPS, downBandwidth = 900,
            queues = QueueAllocation(70, 20, 10),
        )
        engine.saveConfig(cfg).getOrThrow()

        val (oid, attrs) = rpc.sets.single()
        assertEquals("DEV2_IQOS", oid)
        assertEquals("0", attrs["enable"])
        assertEquals("50", attrs["upTotalBW"])
        assertEquals("2", attrs["upUnit"])
        assertEquals("900", attrs["downTotalBW"])
        assertEquals("1", attrs["downUnit"])
        assertEquals("70", attrs["high"]); assertEquals("20", attrs["middle"]); assertEquals("10", attrs["low"])
        assertEquals("0", attrs["linkType"])
        // IPTV enabled -> all IPTV attributes re-sent unchanged
        assertEquals("1", attrs["iptvEnable"]); assertEquals("4", attrs["iptvClass"]); assertEquals("1", attrs["iptvBW"])
        // VoIP disabled -> only the switch is re-sent
        assertEquals("0", attrs["voIPEnable"]); assertNull(attrs["voIPClass"])
        // Guest QoS and read-only attributes are not touched
        assertNull(attrs["guestEnable"]); assertNull(attrs["version"]); assertNull(attrs["stack"])
        assertEquals(listOf("get:DEV2_IQOS", "get:DEV2_IQOS", "set:DEV2_IQOS", "op:ACT_SAVE_CFG"), rpc.log)
    }

    @Test
    fun `saveConfig defaults linkType when router omits it`() {
        val rpc = FakeRpc(iqos = DEFAULT_IQOS - "linkType" - "iptvEnable" - "voIPEnable")
        val engine = QosEngine(rpc)
        engine.saveConfig(engine.loadConfig().getOrThrow()).getOrThrow()
        val attrs = rpc.sets.single().second
        assertEquals("0", attrs["linkType"])
        assertFalse(attrs.containsKey("iptvEnable"))
        assertFalse(attrs.containsKey("voIPEnable"))
    }

    @Test
    fun `saveConfig rejects invalid bandwidth and queues without writing`() {
        val rpc = FakeRpc()
        val engine = QosEngine(rpc)
        val base = engine.loadConfig().getOrThrow()
        val bad = base.copy(upBandwidth = 0, downBandwidth = 10001, queues = QueueAllocation(50, 50, 10))
        val err = engine.saveConfig(bad).exceptionOrNull()
        assertTrue(err is QosValidationException)
        assertEquals(
            setOf(QosValidationError.UP_BANDWIDTH_INVALID, QosValidationError.DOWN_BANDWIDTH_INVALID, QosValidationError.QUEUE_ALLOCATION_INVALID),
            (err as QosValidationException).errors,
        )
        assertTrue(rpc.sets.isEmpty())
        assertFalse(rpc.log.contains("op:ACT_SAVE_CFG"))
    }

    // ---------- Queue allocation ----------

    @Test
    fun `queue allocation validity follows firmware limits`() {
        assertTrue(QueueAllocation(60, 30, 10).isValid())
        assertTrue(QueueAllocation(15, 10, 5).isValid())
        assertTrue(QueueAllocation(85, 10, 5).isValid())
        assertFalse(QueueAllocation(62, 28, 10).isValid()) // not 5% steps
        assertFalse(QueueAllocation(10, 5, 5).isValid()) // below minimums
        assertFalse(QueueAllocation(40, 40, 10).isValid()) // middle must be below high
        assertFalse(QueueAllocation(50, 20, 20).isValid()) // low must be below middle
        assertFalse(QueueAllocation(60, 35, 10).isValid()) // over 100%
    }

    @Test
    fun `raising high pulls middle and low down`() {
        val a = QueueAllocation(50, 35, 15).withHigh(80)
        assertEquals(QueueAllocation(80, 15, 5), a)
        assertTrue(a.isValid())
    }

    @Test
    fun `lowering high below middle clamps middle and low`() {
        val a = QueueAllocation(60, 30, 10).withHigh(20)
        assertEquals(QueueAllocation(20, 15, 10), a)
        assertTrue(a.isValid())
    }

    @Test
    fun `high is snapped to 5 percent and clamped to its range`() {
        assertEquals(60, QueueAllocation(50, 30, 10).withHigh(62).high)
        assertEquals(15, QueueAllocation(50, 30, 10).withHigh(0).high)
        assertEquals(85, QueueAllocation(50, 30, 10).withHigh(100).high)
    }

    @Test
    fun `middle cannot exceed high minus 5 or remaining room`() {
        assertEquals(QueueAllocation(60, 35, 5), QueueAllocation(60, 30, 10).withMiddle(55))
        assertEquals(QueueAllocation(40, 35, 10), QueueAllocation(40, 20, 10).withMiddle(50))
        assertEquals(10, QueueAllocation(60, 30, 10).withMiddle(0).middle)
    }

    @Test
    fun `lowering middle pulls low down`() {
        assertEquals(QueueAllocation(50, 15, 10), QueueAllocation(50, 30, 20).withMiddle(15))
    }

    @Test
    fun `low cannot exceed middle minus 5 or remaining room`() {
        assertEquals(QueueAllocation(60, 30, 10), QueueAllocation(60, 30, 5).withLow(25))
        assertEquals(QueueAllocation(50, 20, 15), QueueAllocation(50, 20, 5).withLow(40))
        assertEquals(5, QueueAllocation(60, 30, 10).withLow(0).low)
    }

    @Test
    fun `normalized fixes an uninitialised router allocation`() {
        val n = QueueAllocation(0, 0, 0).normalized()
        assertTrue(n.isValid())
        assertEquals(QueueAllocation(15, 10, 5), n)
        assertEquals(QueueAllocation(60, 30, 10), QueueAllocation(60, 30, 10).normalized())
    }

    @Test
    fun `slider maxima reflect coupling`() {
        val a = QueueAllocation(60, 25, 10)
        assertEquals(65, a.maxHigh())
        assertEquals(30, a.maxMiddle())
        assertEquals(15, a.maxLow())
    }

    @Test
    fun `bandwidth share computes guaranteed rate`() {
        assertEquals(360.0, bandwidthShare(600, 60), 0.0001)
        assertEquals(7.5, bandwidthShare(50, 15), 0.0001)
    }

    @Test
    fun `bandwidth bounds are 1 to 10000`() {
        val ok = QosConfig(true, 1, BandwidthUnit.MBPS, 10000, BandwidthUnit.MBPS, QueueAllocation.DEFAULT)
        assertTrue(ok.validate().isEmpty())
        assertEquals(setOf(QosValidationError.UP_BANDWIDTH_INVALID), ok.copy(upBandwidth = -5).validate())
    }

    // ---------- DEV2_IQOS_RULE ----------

    @Test
    fun `loadRules parses device and port rules`() {
        val rpc = FakeRpc(
            rules = listOf(
                deviceRule("1,0,0,0,0,0", "aa-bb-cc-dd-ee-01", 4, "Gaming PC"),
                mapOf("stack" to "2,0,0,0,0,0", "type" to "21", "class_" to "2", "srcPort" to "6881", "endPort" to "6889"),
                mapOf("stack" to "3,0,0,0,0,0", "type" to "77", "class_" to "9"),
            ),
        )
        val rules = QosEngine(rpc).loadRules().getOrThrow()
        assertEquals(3, rules.size)
        assertEquals(QosRuleType.DEVICE, rules[0].type)
        assertEquals(QosPriority.HIGH, rules[0].priority)
        assertEquals("AA:BB:CC:DD:EE:01", rules[0].normalizedMac)
        assertEquals("Gaming PC", rules[0].deviceName)
        assertEquals(QosRuleType.CUSTOM_TCP, rules[1].type)
        assertEquals(QosPriority.LOW, rules[1].priority)
        assertEquals("6881", rules[1].srcPort)
        assertEquals(QosRuleType.UNKNOWN, rules[2].type)
        assertEquals(77, rules[2].typeCode)
    }

    @Test
    fun `default device promoted to high adds a device rule`() {
        val rpc = FakeRpc()
        val changed = QosEngine(rpc).setDevicePriority("aa:bb:cc:dd:ee:01", "Gaming PC", QosPriority.HIGH, emptyList()).getOrThrow()
        assertTrue(changed)
        val (oid, attrs) = rpc.adds.single()
        assertEquals("DEV2_IQOS_RULE", oid)
        assertEquals(mapOf("deviceName" to "Gaming PC", "macAddress" to "AA:BB:CC:DD:EE:01", "class_" to "4", "type" to "11"), attrs)
        assertTrue(rpc.deletes.isEmpty())
        assertEquals("op:ACT_SAVE_CFG", rpc.log.last())
    }

    @Test
    fun `default device demoted to low adds a low rule and falls back to MAC for blank name`() {
        val rpc = FakeRpc()
        QosEngine(rpc).setDevicePriority("AA:BB:CC:DD:EE:02", " ", QosPriority.LOW, emptyList()).getOrThrow()
        assertEquals("2", rpc.adds.single().second["class_"])
        assertEquals("AA:BB:CC:DD:EE:02", rpc.adds.single().second["deviceName"])
    }

    @Test
    fun `switching between high and low deletes then adds`() {
        val rpc = FakeRpc()
        val existing = QosRule("5,0,0,0,0,0", QosRuleType.DEVICE, 11, QosPriority.HIGH, "AA-BB-CC-DD-EE-01", "PC")
        QosEngine(rpc).setDevicePriority("AA:BB:CC:DD:EE:01", "PC", QosPriority.LOW, listOf(existing)).getOrThrow()
        assertEquals(listOf("delete:DEV2_IQOS_RULE:5,0,0,0,0,0", "add:DEV2_IQOS_RULE", "op:ACT_SAVE_CFG"), rpc.log)
        assertEquals("2", rpc.adds.single().second["class_"])
    }

    @Test
    fun `returning to middle deletes the rule without adding`() {
        val rpc = FakeRpc()
        val existing = QosRule("5,0,0,0,0,0", QosRuleType.DEVICE, 11, QosPriority.LOW, "AA:BB:CC:DD:EE:01")
        QosEngine(rpc).setDevicePriority("AA:BB:CC:DD:EE:01", "PC", QosPriority.MIDDLE, listOf(existing)).getOrThrow()
        assertEquals(listOf("delete:DEV2_IQOS_RULE:5,0,0,0,0,0", "op:ACT_SAVE_CFG"), rpc.log)
        assertTrue(rpc.adds.isEmpty())
    }

    @Test
    fun `unchanged priority performs no router call`() {
        val rpc = FakeRpc()
        val engine = QosEngine(rpc)
        assertFalse(engine.setDevicePriority("AA:BB:CC:DD:EE:01", "PC", QosPriority.MIDDLE, emptyList()).getOrThrow())
        val existing = QosRule("5,0,0,0,0,0", QosRuleType.DEVICE, 11, QosPriority.HIGH, "AA:BB:CC:DD:EE:01")
        assertFalse(engine.setDevicePriority("AA:BB:CC:DD:EE:01", "PC", QosPriority.HIGH, listOf(existing)).getOrThrow())
        assertTrue(rpc.log.isEmpty())
    }

    @Test
    fun `duplicate rules for one device are all replaced`() {
        val rpc = FakeRpc()
        val rules = listOf(
            QosRule("5,0,0,0,0,0", QosRuleType.DEVICE, 11, QosPriority.HIGH, "AA:BB:CC:DD:EE:01"),
            QosRule("6,0,0,0,0,0", QosRuleType.DEVICE, 11, QosPriority.LOW, "aa:bb:cc:dd:ee:01"),
            QosRule("7,0,0,0,0,0", QosRuleType.DEVICE, 11, QosPriority.LOW, "AA:BB:CC:DD:EE:99"),
        )
        QosEngine(rpc).setDevicePriority("AA:BB:CC:DD:EE:01", "PC", QosPriority.HIGH, rules).getOrThrow()
        assertEquals(listOf("5,0,0,0,0,0", "6,0,0,0,0,0"), rpc.deletes.map { it.second })
        assertEquals(1, rpc.adds.size)
    }

    @Test
    fun `deleteRule removes by stack and commits`() {
        val rpc = FakeRpc()
        QosEngine(rpc).deleteRule("2,0,0,0,0,0").getOrThrow()
        assertEquals(listOf("delete:DEV2_IQOS_RULE:2,0,0,0,0,0", "op:ACT_SAVE_CFG"), rpc.log)
        assertTrue(QosEngine(rpc).deleteRule("").isFailure)
    }

    // ---------- DEV2_GAME_QOS_CLIENT ----------

    @Test
    fun `game clients are filtered to active non-agent game port clients`() {
        val rpc = FakeRpc(
            gameClients = listOf(
                mapOf("hostName" to "PS5", "physAddress" to "AA:BB:CC:00:00:01", "IPAddress" to "192.168.1.40", "active" to "1", "gamePortClient" to "1", "isAgentDevice" to "0"),
                mapOf("hostName" to "Offline", "physAddress" to "AA:BB:CC:00:00:02", "active" to "0", "gamePortClient" to "1"),
                mapOf("hostName" to "Laptop", "physAddress" to "AA:BB:CC:00:00:03", "active" to "1", "gamePortClient" to "0"),
                mapOf("hostName" to "Mesh", "physAddress" to "AA:BB:CC:00:00:04", "active" to "1", "gamePortClient" to "1", "IsAgentDevice" to "1"),
            ),
        )
        val clients = QosEngine(rpc).loadGameClients()
        assertEquals(listOf(GameQosClient("PS5", "AA:BB:CC:00:00:01", "192.168.1.40")), clients)
    }

    @Test
    fun `game clients degrade to empty list when the object is unsupported`() {
        val rpc = FakeRpc(gameClients = null)
        assertTrue(QosEngine(rpc).loadGameClients().isEmpty())
    }

    // ---------- Catalog ----------

    @Test
    fun `qos page is admin only under local network`() {
        val page = Catalog.byId("qos")!!
        assertEquals("Intelligent QoS", page.title)
        assertEquals(Group.NETWORK, page.group)
        assertTrue(page.adminOnly)
        assertTrue(page.custom)
        assertTrue(Catalog.visiblePages("admin").any { it.id == "qos" })
        assertFalse(Catalog.visiblePages("user").any { it.id == "qos" })
    }
}
