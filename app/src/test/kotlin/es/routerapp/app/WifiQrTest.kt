package es.routerapp.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WifiQrTest {

    @Test
    fun `standard WPA2 protected network formats correct URI`() {
        val uri = WifiQr.formatWifiQrString(
            ssid = "MyHomeNetwork",
            psk = "SuperSecret123",
            securityMode = "WPA2-Personal",
            hidden = false,
        )
        assertEquals("WIFI:S:MyHomeNetwork;T:WPA;P:SuperSecret123;;", uri)
    }

    @Test
    fun `WPA3 mixed mode maps to WPA auth token`() {
        val uri = WifiQr.formatWifiQrString(
            ssid = "Fiber_7G",
            psk = "Password789",
            securityMode = "WPA2-WPA3-Personal",
        )
        assertEquals("WIFI:S:Fiber_7G;T:WPA;P:Password789;;", uri)
    }

    @Test
    fun `open network maps to nopass with omitted password field`() {
        val uriNone = WifiQr.formatWifiQrString(
            ssid = "Cafe_Guest",
            psk = "",
            securityMode = "None",
        )
        assertEquals("WIFI:S:Cafe_Guest;T:nopass;;", uriNone)

        val uriOwe = WifiQr.formatWifiQrString(
            ssid = "Airport_Open",
            psk = "ignored",
            securityMode = "OWE",
        )
        assertEquals("WIFI:S:Airport_Open;T:nopass;;", uriOwe)
    }

    @Test
    fun `hidden SSID sets hidden flag true`() {
        val uri = WifiQr.formatWifiQrString(
            ssid = "StealthNet",
            psk = "SecretKey456",
            securityMode = "WPA2-Personal",
            hidden = true,
        )
        assertEquals("WIFI:S:StealthNet;T:WPA;P:SecretKey456;H:true;;", uri)
    }

    @Test
    fun `special characters in SSID and password are escaped with backslash`() {
        val uri = WifiQr.formatWifiQrString(
            ssid = "My;Home:Net,work\"\\",
            psk = "P@ss:w;ord\\,\"",
            securityMode = "WPA2-Personal",
        )
        assertEquals(
            "WIFI:S:My\\;Home\\:Net\\,work\\\"\\\\;T:WPA;P:P@ss\\:w\\;ord\\\\\\,\\\";;",
            uri,
        )
    }

    @Test
    fun `escape helper handles plain strings without alterations`() {
        assertEquals("SimpleString123", WifiQr.escapeWifiString("SimpleString123"))
    }

    @Test
    fun `qr matrix generator produces valid dimensions and patterns`() {
        val matrix = WifiQr.generateQrMatrix("WIFI:S:Test;T:WPA;P:Test1234;;", size = 128)
        assertTrue(matrix.isNotEmpty())
        assertTrue(matrix[0].isNotEmpty())
        val width = matrix[0].size
        val height = matrix.size
        assertEquals(width, height)
        // Finder patterns in corners should have dark modules
        var hasTrue = false
        var hasFalse = false
        for (row in matrix) {
            for (cell in row) {
                if (cell) hasTrue = true else hasFalse = true
            }
        }
        assertTrue("Matrix must contain dark modules", hasTrue)
        assertTrue("Matrix must contain light modules", hasFalse)
    }

    @Test
    fun `extractWifiSectionData extracts primary credentials correctly`() {
        val values = mapOf(
            "primarySSID" to "MainNet",
            "primaryPSK" to "MainPass",
            "primaryModeEnabled" to "WPA2-Personal",
            "primarySSIDAdvertise" to "1",
            "primaryEnable" to "1",
            "band" to "5GHz",
        )
        val data = WifiQr.extractWifiSectionData("primary", values)
        org.junit.Assert.assertNotNull(data)
        assertEquals("MainNet", data?.ssid)
        assertEquals("MainPass", data?.psk)
        assertEquals("WPA2-Personal", data?.securityMode)
        org.junit.Assert.assertFalse(data?.hidden ?: true)
        assertTrue(data?.enabled ?: false)
        assertEquals(WifiNetworkCategory.PRIMARY, data?.category)
        assertEquals(listOf("5GHz"), data?.bands)
    }

    @Test
    fun `extractWifiSectionData extracts guest, mlo, and mssid correctly`() {
        val guest = WifiQr.extractWifiSectionData(
            "guest",
            mapOf("guestSSID" to "GuestNet", "guestPSK" to "Guest123", "guestEnable" to "1"),
        )
        assertEquals("GuestNet", guest?.ssid)
        assertEquals(WifiNetworkCategory.GUEST, guest?.category)

        val mlo = WifiQr.extractWifiSectionData(
            "mlo",
            mapOf("mloSSID" to "MloNet", "mloPSK" to "MloPass", "mloEnable" to "1"),
        )
        assertEquals("MloNet", mlo?.ssid)
        assertEquals(WifiNetworkCategory.MLO, mlo?.category)

        val mssid = WifiQr.extractWifiSectionData(
            "mssid2",
            mapOf("mssid2SSID" to "IoT2", "mssid2PSK" to "IoTPass", "mssid2Enable" to "1"),
        )
        assertEquals("IoT2", mssid?.ssid)
        assertEquals(WifiNetworkCategory.ADDITIONAL, mssid?.category)
        assertEquals(2, mssid?.additionalIndex)
    }

    @Test
    fun `extractWifiSectionData returns null for non wifi sections or empty SSID`() {
        org.junit.Assert.assertNull(WifiQr.extractWifiSectionData("wan", mapOf("name" to "WAN1")))
        org.junit.Assert.assertNull(WifiQr.extractWifiSectionData("primary", mapOf("primarySSID" to "")))
    }
}
