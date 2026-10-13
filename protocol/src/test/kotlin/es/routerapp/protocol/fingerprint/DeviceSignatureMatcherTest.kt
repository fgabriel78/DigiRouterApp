package es.routerapp.protocol.fingerprint

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class DeviceSignatureMatcherTest {

    @Test
    fun testMatchPlayStation5Ssdp() {
        val ssdp = SsdpResponse(
            startLine = "HTTP/1.1 200 OK",
            headers = emptyMap(),
            st = "urn:schemas-sony-com:service:PRX:1",
            usn = "uuid:ps5-123",
            location = "http://192.168.1.50:9295/description.xml",
            server = "PlayStation 5/1.0",
        )
        val match = DeviceSignatureMatcher.match(ssdp = ssdp)
        assertNotNull(match)
        assertEquals("PlayStation 5", match!!.exactModel)
        assertEquals("Sony", match.vendor)
        assertEquals(FingerprintCategory.CONSOLE, match.category)
    }

    @Test
    fun testMatchLgOledTvUpnp() {
        val http = HttpDescriptor(
            friendlyName = "LG OLED55C26LD",
            manufacturer = "LG Electronics",
            modelName = "LG TV",
            modelNumber = "OLED55C26LD",
        )
        val match = DeviceSignatureMatcher.match(httpDesc = http)
        assertNotNull(match)
        assertEquals("LG OLED55C2", match!!.exactModel)
        assertEquals("LG", match.vendor)
        assertEquals(FingerprintCategory.TV, match.category)
    }

    @Test
    fun testMatchPhilipsHueBridge() {
        val http = HttpDescriptor(
            friendlyName = "Philips hue (192.168.1.54)",
            manufacturer = "Signify",
            modelName = "Philips hue bridge 2015",
            modelNumber = "BSB002",
        )
        val match = DeviceSignatureMatcher.match(httpDesc = http)
        assertNotNull(match)
        assertEquals("Philips Hue Bridge", match!!.exactModel)
        assertEquals("Philips", match.vendor)
        assertEquals(FingerprintCategory.IOT, match.category)
    }

    @Test
    fun testMatchGoogleChromecastMdns() {
        val packet = MdnsPacket(
            transactionId = 0,
            isResponse = true,
            questions = emptyList(),
            records = listOf(
                MdnsRecord(
                    name = "Living Room._googlecast._tcp.local",
                    type = MdnsRecordParser.TYPE_TXT,
                    ttl = 120,
                    txtEntries = mapOf("md" to "Chromecast with Google TV", "fn" to "Living Room"),
                ),
            ),
        )
        val match = DeviceSignatureMatcher.match(mdns = packet)
        assertNotNull(match)
        assertEquals("Chromecast with Google TV", match!!.exactModel)
        assertEquals("Google", match.vendor)
        assertEquals(FingerprintCategory.TV, match.category)
    }

    @Test
    fun testMatchAppleMacBookProMdns() {
        val packet = MdnsPacket(
            transactionId = 0,
            isResponse = true,
            questions = emptyList(),
            records = listOf(
                MdnsRecord(
                    name = "MacBook._airplay._tcp.local",
                    type = MdnsRecordParser.TYPE_TXT,
                    ttl = 120,
                    txtEntries = mapOf("model" to "MacBookPro18,1"),
                ),
            ),
        )
        val match = DeviceSignatureMatcher.match(mdns = packet)
        assertNotNull(match)
        assertEquals("MacBook Pro", match!!.exactModel)
        assertEquals("Apple", match.vendor)
        assertEquals(FingerprintCategory.COMPUTER, match.category)
    }

    @Test
    fun testMatchShellyIot() {
        val http = HttpDescriptor(
            manufacturer = "Shelly",
            modelName = "SHSW-1",
            extraProperties = mapOf("shelly_type" to "SHSW-1"),
        )
        val match = DeviceSignatureMatcher.match(httpDesc = http)
        assertNotNull(match)
        assertEquals("Shelly SHSW-1", match!!.exactModel)
        assertEquals("Shelly", match.vendor)
        assertEquals(FingerprintCategory.IOT, match.category)
    }
}
