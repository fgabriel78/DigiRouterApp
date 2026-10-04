package es.routerapp.protocol

import es.routerapp.protocol.pages.ConnectedDevices
import es.routerapp.protocol.pages.Instance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConnectedDevicesTest {

    private fun inst(vararg kv: Pair<String, String>) = Instance(mapOf(*kv), "0,0,0,0,0,0")

    private fun wifi(mac: String, name: String = "", ip: String = "", active: String = "1", level: String = "4", rate: String = "48000") =
        inst("MACAddress" to mac, "X_TP_HostName" to name, "X_TP_IPAddress" to ip, "active" to active,
            "X_TP_SignalStrengthLevel" to level, "lastDataDownlinkRate" to rate)

    private fun wired(mac: String, name: String = "", ip: String = "", active: String = "1", speed: String = "1000") =
        inst("MACAddress" to mac, "X_TP_HostName" to name, "IPAddress" to ip, "active" to active, "linkSpeed" to speed)

    @Test
    fun `maps wifi and wired clients`() {
        val list = ConnectedDevices.parse(
            listOf(wifi("AA:00:00:00:00:01", "Phone", "192.168.1.10", level = "3", rate = "6000")),
            listOf(wired("AA:00:00:00:00:02", "Desktop", "192.168.1.20")),
        )
        assertEquals(2, list.size)
        val phone = list.first { it.name == "Phone" }
        assertTrue(phone.wifi)
        assertEquals(3, phone.level)
        assertEquals(6L, phone.rate) // kbit/s -> Mbit/s
        val desktop = list.first { it.name == "Desktop" }
        assertFalse(desktop.wifi)
        assertNull(desktop.level)
        assertEquals(1000L, desktop.rate)
        assertEquals("192.168.1.20", desktop.ip)
    }

    @Test
    fun `name falls back to ip then mac`() {
        val list = ConnectedDevices.parse(
            listOf(wifi("AA:00:00:00:00:01", "", "192.168.1.10"), wifi("AA:00:00:00:00:02", "", "")),
            emptyList(),
        )
        assertEquals(setOf("192.168.1.10", "AA:00:00:00:00:02"), list.map { it.name }.toSet())
    }

    @Test
    fun `active devices first then sorted by name`() {
        val list = ConnectedDevices.parse(
            listOf(wifi("AA:00:00:00:00:01", "zeta"), wifi("AA:00:00:00:00:02", "Beta", active = "0")),
            listOf(wired("AA:00:00:00:00:03", "alpha")),
        )
        assertEquals(listOf("alpha", "zeta", "Beta"), list.map { it.name })
    }

    @Test
    fun `duplicated mac keeps the active entry`() {
        val list = ConnectedDevices.parse(
            listOf(wifi("aa:00:00:00:00:01", "old", active = "0"), wifi("AA:00:00:00:00:01", "new", active = "1")),
            emptyList(),
        )
        assertEquals(1, list.size)
        assertEquals("new", list.single().name)
        assertTrue(list.single().active)
    }

    @Test
    fun `entries without mac are not merged`() {
        val list = ConnectedDevices.parse(listOf(wifi("", "a"), wifi("", "b")), emptyList())
        assertEquals(2, list.size)
    }

    private fun node(
        mac: String,
        name: String = "",
        model: String = "EX530v",
        ip: String = "192.168.1.1",
        isController: String = "1",
        active: String = "1",
        backhaul: String = "",
        signal: String = "",
        rate: String = "",
        uptime: String = "3600",
    ) = inst(
        "MACAddress" to mac, "X_TP_HostName" to name, "X_TP_ModelName" to model, "X_TP_IPAddress" to ip,
        "X_TP_IsController" to isController, "X_TP_Active" to active, "backhaulLinkType" to backhaul,
        "backhaulSignalStrength" to signal, "X_TP_LinkRate" to rate, "X_TP_UpTime" to uptime,
    )

    private fun wifiWithNode(mac: String, nodeMac: String, name: String = "", ip: String = "") =
        inst("MACAddress" to mac, "X_TP_HostName" to name, "X_TP_IPAddress" to ip, "active" to "1",
            "X_TP_SignalStrengthLevel" to "4", "lastDataDownlinkRate" to "48000", "X_TP_ApDeviceMac" to nodeMac)

    private fun wiredWithNode(mac: String, nodeMac: String, name: String = "", ip: String = "") =
        inst("MACAddress" to mac, "X_TP_HostName" to name, "IPAddress" to ip, "active" to "1",
            "linkSpeed" to "1000", "APDeviceMACAddress" to nodeMac)

    @Test
    fun `parses mesh nodes and sorts controller first`() {
        val agent = node("AA:00:00:00:00:02", "Living Room", model = "HX220", ip = "192.168.1.2",
            isController = "0", backhaul = "Wi-Fi", signal = "140", rate = "866", uptime = "7200")
        val controller = node("AA:00:00:00:00:01", "Main Router", model = "EX530v", ip = "192.168.1.1",
            isController = "1", uptime = "14400")
        val nodes = ConnectedDevices.parseNodes(listOf(agent, controller))

        assertEquals(2, nodes.size)
        val first = nodes[0]
        assertTrue(first.isController)
        assertEquals("Main Router", first.name)
        assertEquals("EX530v", first.model)
        assertNull(first.backhaulType)

        val second = nodes[1]
        assertFalse(second.isController)
        assertEquals("Living Room", second.name)
        assertEquals("HX220", second.model)
        assertEquals("Wi-Fi", second.backhaulType)
        assertEquals(140, second.backhaulSignal)
        assertEquals(866L, second.linkRate)
        assertEquals(7200L, second.uptime)
    }

    @Test
    fun `maps clients to connected mesh node and counts clients per node`() {
        val controller = node("AA:00:00:00:00:01", "Main Router", isController = "1")
        val agent = node("AA:00:00:00:00:02", "Office", isController = "0")
        val wifiClient = wifiWithNode("CC:00:00:00:00:01", "AA:00:00:00:00:02", "Laptop")
        val wiredClient = wiredWithNode("CC:00:00:00:00:02", "AA:00:00:00:00:01", "Desktop")

        val data = ConnectedDevices.parseData(
            wifi = listOf(wifiClient),
            wired = listOf(wiredClient),
            apDevs = listOf(controller, agent),
        )

        assertEquals(2, data.devices.size)
        val laptop = data.devices.first { it.name == "Laptop" }
        assertEquals("AA:00:00:00:00:02", laptop.nodeMac)
        assertEquals("Office", laptop.nodeName)

        val desktop = data.devices.first { it.name == "Desktop" }
        assertEquals("AA:00:00:00:00:01", desktop.nodeMac)
        assertEquals("Main Router", desktop.nodeName)

        val ctrlNode = data.nodes.first { it.mac == "AA:00:00:00:00:01" }
        val agentNode = data.nodes.first { it.mac == "AA:00:00:00:00:02" }
        assertEquals(1, ctrlNode.connectedClientsCount)
        assertEquals(1, agentNode.connectedClientsCount)
    }

    @Test
    fun `empty input gives empty list`() {
        assertTrue(ConnectedDevices.parse(emptyList(), emptyList()).isEmpty())
    }
}

