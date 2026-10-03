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

    @Test
    fun `empty input gives empty list`() {
        assertTrue(ConnectedDevices.parse(emptyList(), emptyList()).isEmpty())
    }
}
