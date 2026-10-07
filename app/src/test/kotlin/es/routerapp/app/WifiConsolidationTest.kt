package es.routerapp.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WifiConsolidationTest {

    @Test
    fun `empty raw bands returns empty list`() {
        val result = consolidateWifiNetworks(emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `single band primary network is parsed correctly`() {
        val raw = listOf(
            mapOf(
                "band" to "2.4GHz",
                "primaryEnable" to "1",
                "primarySSID" to "MyHomeWifi",
                "guestEnable" to "0",
            )
        )
        val result = consolidateWifiNetworks(raw)
        assertEquals(1, result.size)
        val net = result[0]
        assertEquals(WifiNetworkCategory.PRIMARY, net.category)
        assertEquals("MyHomeWifi", net.ssid)
        assertEquals(listOf("2.4GHz"), net.bands)
        assertTrue(net.enabled)
    }

    @Test
    fun `dual band with matching SSID consolidates into a single card`() {
        val raw = listOf(
            mapOf(
                "band" to "2.4GHz",
                "primaryEnable" to "1",
                "primarySSID" to "DIGI_Fiber",
                "guestEnable" to "0",
            ),
            mapOf(
                "band" to "5GHz",
                "primaryEnable" to "1",
                "primarySSID" to "DIGI_Fiber",
                "guestEnable" to "0",
            ),
        )
        val result = consolidateWifiNetworks(raw)
        assertEquals(1, result.size)
        val net = result[0]
        assertEquals(WifiNetworkCategory.PRIMARY, net.category)
        assertEquals("DIGI_Fiber", net.ssid)
        assertEquals(listOf("2.4GHz", "5GHz"), net.bands)
        assertTrue(net.enabled)
    }

    @Test
    fun `dual band with different SSIDs produces two cards`() {
        val raw = listOf(
            mapOf(
                "band" to "2.4GHz",
                "primaryEnable" to "1",
                "primarySSID" to "DIGI_24",
                "guestEnable" to "0",
            ),
            mapOf(
                "band" to "5GHz",
                "primaryEnable" to "1",
                "primarySSID" to "DIGI_50",
                "guestEnable" to "0",
            ),
        )
        val result = consolidateWifiNetworks(raw)
        assertEquals(2, result.size)
        assertEquals("DIGI_24", result[0].ssid)
        assertEquals(listOf("2.4GHz"), result[0].bands)
        assertEquals("DIGI_50", result[1].ssid)
        assertEquals(listOf("5GHz"), result[1].bands)
    }

    @Test
    fun `guest network is extracted and consolidated across bands`() {
        val raw = listOf(
            mapOf(
                "band" to "2.4GHz",
                "primaryEnable" to "1",
                "primarySSID" to "DIGI_Main",
                "guestEnable" to "1",
                "guestSSID" to "DIGI_Guest",
            ),
            mapOf(
                "band" to "5GHz",
                "primaryEnable" to "1",
                "primarySSID" to "DIGI_Main",
                "guestEnable" to "1",
                "guestSSID" to "DIGI_Guest",
            ),
        )
        val result = consolidateWifiNetworks(raw)
        assertEquals(2, result.size)

        val primary = result[0]
        assertEquals(WifiNetworkCategory.PRIMARY, primary.category)
        assertEquals("DIGI_Main", primary.ssid)
        assertEquals(listOf("2.4GHz", "5GHz"), primary.bands)

        val guest = result[1]
        assertEquals(WifiNetworkCategory.GUEST, guest.category)
        assertEquals("DIGI_Guest", guest.ssid)
        assertEquals(listOf("2.4GHz", "5GHz"), guest.bands)
        assertTrue(guest.enabled)
    }

    @Test
    fun `additional multi-SSID networks are extracted`() {
        val raw = listOf(
            mapOf(
                "band" to "2.4GHz",
                "primaryEnable" to "1",
                "primarySSID" to "DIGI_Main",
                "guestEnable" to "0",
                "mssid1Enable" to "1",
                "mssid1SSID" to "IoT_Sensors",
                "mssid2Enable" to "1",
                "mssid2SSID" to "Office_Work",
            )
        )
        val result = consolidateWifiNetworks(raw)
        assertEquals(3, result.size)

        assertEquals(WifiNetworkCategory.PRIMARY, result[0].category)
        assertEquals("DIGI_Main", result[0].ssid)

        assertEquals(WifiNetworkCategory.ADDITIONAL, result[1].category)
        assertEquals("IoT_Sensors", result[1].ssid)
        assertEquals(1, result[1].additionalIndex)

        assertEquals(WifiNetworkCategory.ADDITIONAL, result[2].category)
        assertEquals("Office_Work", result[2].ssid)
        assertEquals(2, result[2].additionalIndex)
    }

    @Test
    fun `primary disabled state is included when guest network is active`() {
        val raw = listOf(
            mapOf(
                "band" to "2.4GHz",
                "primaryEnable" to "0",
                "primarySSID" to "DIGI_Main",
                "guestEnable" to "1",
                "guestSSID" to "DIGI_Guest",
            )
        )
        val result = consolidateWifiNetworks(raw)
        assertEquals(2, result.size)

        val primary = result[0]
        assertEquals(WifiNetworkCategory.PRIMARY, primary.category)
        assertFalse(primary.enabled)

        val guest = result[1]
        assertEquals(WifiNetworkCategory.GUEST, guest.category)
        assertEquals("DIGI_Guest", guest.ssid)
        assertTrue(guest.enabled)
    }

    @Test
    fun `all disabled networks returns empty list`() {
        val raw = listOf(
            mapOf(
                "band" to "2.4GHz",
                "primaryEnable" to "0",
                "primarySSID" to "DIGI_Main",
                "guestEnable" to "0",
                "guestSSID" to "DIGI_Guest",
                "mssid1Enable" to "0",
                "mssid2Enable" to "0",
            ),
            mapOf(
                "band" to "5GHz",
                "primaryEnable" to "0",
                "primarySSID" to "DIGI_Main",
                "guestEnable" to "0",
                "guestSSID" to "DIGI_Guest",
            )
        )
        val result = consolidateWifiNetworks(raw)
        assertTrue(result.isEmpty())
    }
}
