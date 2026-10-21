package es.digirouter.app

import es.routerapp.protocol.RouterClient
import es.routerapp.protocol.pages.ConnectedDevice
import es.routerapp.protocol.qos.BandwidthUnit
import es.routerapp.protocol.qos.GameQosClient
import es.routerapp.protocol.qos.QosEngine
import es.routerapp.protocol.qos.QosPriority
import es.routerapp.protocol.qos.QosRouterRpc
import es.routerapp.protocol.qos.QosRule
import es.routerapp.protocol.qos.QosRuleType
import es.routerapp.protocol.qos.QosValidationError
import es.routerapp.protocol.qos.QueueAllocation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QosViewModelTest {

    /** Stateful fake router: rule adds/deletes are reflected on the next read. */
    private class FakeRpc(
        var iqos: Map<String, String> = mapOf(
            "enable" to "1", "upTotalBW" to "100", "upUnit" to "1", "downTotalBW" to "600", "downUnit" to "1",
            "high" to "60", "middle" to "30", "low" to "10",
        ),
        rules: List<Map<String, String>> = emptyList(),
        var gameClients: List<Map<String, String>> = emptyList(),
        var failAdds: Boolean = false,
    ) : QosRouterRpc {
        val rules = rules.toMutableList()
        val sets = mutableListOf<Map<String, String>>()
        val adds = mutableListOf<Map<String, String>>()
        val deletes = mutableListOf<String>()
        private var nextStack = 50

        private fun obj(m: Map<String, String>) = JsonObject(m.mapValues { JsonPrimitive(it.value) })

        override fun get(oid: String, attrs: Map<String, String>, stack: String): JsonElement = obj(iqos)
        override fun getList(oid: String, attrs: Map<String, String>, stack: String): JsonElement = when (oid) {
            "DEV2_IQOS_RULE" -> JsonArray(rules.map { obj(it) })
            "DEV2_GAME_QOS_CLIENT" -> JsonArray(gameClients.map { obj(it) })
            else -> JsonArray(emptyList())
        }
        override fun set(oid: String, attrs: Map<String, String>, stack: String): JsonElement {
            sets += attrs
            iqos = iqos + attrs
            return JsonObject(emptyMap())
        }
        override fun add(oid: String, attrs: Map<String, String>, parentStack: String): JsonElement {
            if (failAdds) throw IllegalStateException("router error 5000")
            adds += attrs
            rules += attrs + ("stack" to "${nextStack++},0,0,0,0,0")
            return JsonObject(emptyMap())
        }
        override fun delete(oid: String, stack: String): JsonElement {
            deletes += stack
            rules.removeAll { it["stack"] == stack }
            return JsonObject(emptyMap())
        }
        override fun operate(oid: String, attrs: Map<String, String>, stack: String): JsonElement = JsonObject(emptyMap())
    }

    private val client = RouterClient("http://127.0.0.1")

    private fun device(mac: String, name: String, active: Boolean = true) =
        ConnectedDevice(name = name, ip = "192.168.1.${mac.takeLast(2).toInt(16)}", mac = mac, wifi = true, level = 4, rate = 400, active = active)

    private fun vm(
        rpc: FakeRpc,
        devices: List<ConnectedDevice> = emptyList(),
        store: DeviceMetadataStore? = null,
        loader: ((RouterClient) -> List<ConnectedDevice>)? = null,
    ) = QosViewModel(
        clientProvider = { client },
        metadataStore = store,
        engineProvider = { QosEngine(rpc) },
        devicesLoader = loader ?: { devices },
        coroutineScope = CoroutineScope(Dispatchers.Unconfined),
        ioDispatcher = Dispatchers.Unconfined,
    )

    private fun rule(stack: String, mac: String, cls: Int, name: String = "") =
        mapOf("stack" to stack, "type" to "11", "class_" to cls.toString(), "macAddress" to mac, "deviceName" to name)

    // ---------- Loading & merge ----------

    @Test
    fun `missing client reports not connected`() {
        val vm = QosViewModel(
            clientProvider = { null },
            coroutineScope = CoroutineScope(Dispatchers.Unconfined),
            ioDispatcher = Dispatchers.Unconfined,
        )
        assertEquals(QosError.NOT_CONNECTED, vm.state.value.error)
        assertFalse(vm.state.value.isLoading)
    }

    @Test
    fun `devices are merged with rules, game port clients and metadata`() {
        val store = InMemoryDeviceMetadataStore().apply {
            save("AA:BB:CC:DD:EE:01", DeviceCustomMetadata(alias = "Gaming PC", category = DeviceCategory.COMPUTER))
        }
        val rpc = FakeRpc(
            rules = listOf(
                rule("1,0,0,0,0,0", "aa-bb-cc-dd-ee-01", 4, "desktop"),
                rule("2,0,0,0,0,0", "AA:BB:CC:DD:EE:09", 2, "old-laptop"),
                mapOf("stack" to "3,0,0,0,0,0", "type" to "21", "class_" to "2", "srcPort" to "6881", "endPort" to "6889"),
            ),
            gameClients = listOf(
                mapOf("hostName" to "PS5", "physAddress" to "AA:BB:CC:DD:EE:04", "IPAddress" to "192.168.1.4", "active" to "1", "gamePortClient" to "1"),
            ),
        )
        val devices = listOf(
            device("aa:bb:cc:dd:ee:01", "desktop"),
            device("AA:BB:CC:DD:EE:02", "tv"),
            device("AA:BB:CC:DD:EE:03", "phone"),
        )
        val state = vm(rpc, devices, store).state.value

        assertNull(state.error)
        assertEquals(
            listOf("AA:BB:CC:DD:EE:04", "AA:BB:CC:DD:EE:01", "AA:BB:CC:DD:EE:03", "AA:BB:CC:DD:EE:02", "AA:BB:CC:DD:EE:09"),
            state.devices.map { it.mac },
        )
        val game = state.devices[0]
        assertTrue(game.isGamePort); assertEquals(QosPriority.HIGH, game.priority); assertTrue(game.online)
        val pc = state.devices[1]
        assertEquals("Gaming PC", pc.title); assertEquals(DeviceCategory.COMPUTER, pc.category)
        assertEquals(QosPriority.HIGH, pc.priority); assertEquals("desktop", pc.hostName)
        assertEquals(QosPriority.MIDDLE, state.devices[2].priority)
        assertTrue(state.devices[2].rules.isEmpty())
        val offline = state.devices[4]
        assertFalse(offline.online); assertEquals(QosPriority.LOW, offline.priority); assertEquals("old-laptop", offline.title)
        assertEquals(1, state.otherRules.size)
        assertEquals(QosRuleType.CUSTOM_TCP, state.otherRules.single().type)
    }

    @Test
    fun `device discovery failure does not block the screen`() {
        val rpc = FakeRpc(rules = listOf(rule("1,0,0,0,0,0", "AA:BB:CC:DD:EE:01", 4, "pc")))
        val state = vm(rpc, loader = { throw IllegalStateException("71011") }).state.value
        assertNull(state.error)
        assertEquals(1, state.devices.size)
        assertFalse(state.devices.single().online)
    }

    @Test
    fun `unconfigured router gets valid default draft`() {
        val rpc = FakeRpc(iqos = mapOf("enable" to "0", "upTotalBW" to "0", "downTotalBW" to "0", "high" to "0", "middle" to "0", "low" to "0"))
        val state = vm(rpc).state.value
        val draft = state.draft!!
        assertEquals("1000", draft.upText)
        assertEquals("1000", draft.downText)
        assertEquals(QueueAllocation.DEFAULT, draft.queues)
        assertFalse(state.isDirty)
    }

    @Test
    fun `buildQosDevices orders game port, then priority, online first, then name`() {
        val rules = listOf(
            QosRule("1", QosRuleType.DEVICE, 11, QosPriority.LOW, "AA:00:00:00:00:01", "zeta"),
            QosRule("2", QosRuleType.DEVICE, 11, QosPriority.HIGH, "AA:00:00:00:00:02", "beta"),
        )
        val list = buildQosDevices(
            connected = listOf(device("AA:00:00:00:00:03", "alpha"), device("AA:00:00:00:00:02", "beta", active = false)),
            rules = rules,
            gameClients = listOf(GameQosClient("console", "AA:00:00:00:00:04", "")),
        )
        assertEquals(listOf("console", "beta", "alpha", "zeta"), list.map { it.title })
    }

    // ---------- Settings editing & save ----------

    @Test
    fun `slider edits are coupled and mark state dirty`() {
        val vm = vm(FakeRpc())
        vm.setHigh(80)
        assertEquals(QueueAllocation(80, 15, 5), vm.state.value.draft!!.queues)
        assertTrue(vm.state.value.isDirty)
        vm.setMiddle(50)
        assertEquals(QueueAllocation(80, 15, 5), vm.state.value.draft!!.queues)
        vm.setHigh(60); vm.setMiddle(25); vm.setLow(50)
        assertEquals(QueueAllocation(60, 25, 15), vm.state.value.draft!!.queues)
    }

    @Test
    fun `bandwidth input keeps digits only`() {
        val vm = vm(FakeRpc())
        vm.setUpBandwidth("12a3456")
        assertEquals("12345", vm.state.value.draft!!.upText)
        vm.setDownUnit(BandwidthUnit.KBPS)
        assertEquals(BandwidthUnit.KBPS, vm.state.value.draft!!.downUnit)
    }

    @Test
    fun `invalid bandwidth blocks save`() {
        val rpc = FakeRpc()
        val vm = vm(rpc)
        vm.setUpBandwidth("")
        vm.setDownBandwidth("20000")
        vm.saveConfig()
        assertEquals(
            setOf(QosValidationError.UP_BANDWIDTH_INVALID, QosValidationError.DOWN_BANDWIDTH_INVALID),
            vm.state.value.validationErrors,
        )
        assertTrue(rpc.sets.isEmpty())
    }

    @Test
    fun `valid save writes the router and clears dirty state`() {
        val rpc = FakeRpc()
        val vm = vm(rpc)
        vm.setEnabled(false)
        vm.setUpBandwidth("80")
        vm.setHigh(70)
        vm.saveConfig()
        val written = rpc.sets.single()
        assertEquals("0", written["enable"])
        assertEquals("80", written["upTotalBW"])
        assertEquals("70", written["high"])
        val state = vm.state.value
        assertTrue(state.saved)
        assertFalse(state.isDirty)
        assertFalse(state.isSaving)
        vm.consumeSaved()
        assertFalse(vm.state.value.saved)
    }

    // ---------- Device priority ----------

    @Test
    fun `promoting a default device adds a rule and refreshes the list`() {
        val rpc = FakeRpc()
        val vm = vm(rpc, listOf(device("AA:BB:CC:DD:EE:02", "work-laptop")))
        val laptop = vm.state.value.devices.single()
        vm.setDevicePriority(laptop, QosPriority.HIGH)

        assertEquals("AA:BB:CC:DD:EE:02", rpc.adds.single()["macAddress"])
        assertEquals("work-laptop", rpc.adds.single()["deviceName"])
        val updated = vm.state.value.devices.single()
        assertEquals(QosPriority.HIGH, updated.priority)
        assertEquals(1, updated.rules.size)
        assertTrue(vm.state.value.pendingMacs.isEmpty())
    }

    @Test
    fun `returning a device to middle removes its rule`() {
        val rpc = FakeRpc(rules = listOf(rule("1,0,0,0,0,0", "AA:BB:CC:DD:EE:02", 2, "tv")))
        val vm = vm(rpc, listOf(device("AA:BB:CC:DD:EE:02", "tv")))
        vm.setDevicePriority(vm.state.value.devices.single(), QosPriority.MIDDLE)
        assertEquals(listOf("1,0,0,0,0,0"), rpc.deletes)
        assertEquals(QosPriority.MIDDLE, vm.state.value.devices.single().priority)
    }

    @Test
    fun `game port devices cannot be changed`() {
        val rpc = FakeRpc(gameClients = listOf(mapOf("hostName" to "PS5", "physAddress" to "AA:BB:CC:DD:EE:04", "active" to "1", "gamePortClient" to "1")))
        val vm = vm(rpc)
        vm.setDevicePriority(vm.state.value.devices.single(), QosPriority.LOW)
        assertTrue(rpc.adds.isEmpty())
        assertTrue(rpc.deletes.isEmpty())
    }

    @Test
    fun `priority failure reports error and clears pending`() {
        val rpc = FakeRpc(failAdds = true)
        val vm = vm(rpc, listOf(device("AA:BB:CC:DD:EE:02", "tv")))
        vm.setDevicePriority(vm.state.value.devices.single(), QosPriority.HIGH)
        val state = vm.state.value
        assertEquals(QosError.PRIORITY_FAILED, state.error)
        assertTrue(state.pendingMacs.isEmpty())
        assertEquals(QosPriority.MIDDLE, state.devices.single().priority)
        vm.clearError()
        assertNull(vm.state.value.error)
    }

    @Test
    fun `deleting a port rule removes it from other rules`() {
        val rpc = FakeRpc(rules = listOf(mapOf("stack" to "3,0,0,0,0,0", "type" to "22", "class_" to "4", "srcPort" to "3074", "endPort" to "3074")))
        val vm = vm(rpc)
        vm.deleteRule(vm.state.value.otherRules.single())
        assertEquals(listOf("3,0,0,0,0,0"), rpc.deletes)
        assertTrue(vm.state.value.otherRules.isEmpty())
    }
}
