package es.routerapp.protocol.fingerprint

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.StandardCharsets

class SsdpPacketParserTest {

    @Test
    fun testBuildMSearch() {
        val bytes = SsdpPacketParser.buildMSearch("ssdp:all", 3)
        val text = String(bytes, StandardCharsets.UTF_8)
        assertTrue(text.startsWith("M-SEARCH * HTTP/1.1\r\n"))
        assertTrue(text.contains("HOST: 239.255.255.250:1900"))
        assertTrue(text.contains("ST: ssdp:all"))
        assertTrue(text.contains("MX: 3"))
        assertTrue(text.endsWith("\r\n\r\n"))
    }

    @Test
    fun testParseStandardSsdpResponse() {
        val raw = """
            HTTP/1.1 200 OK
            CACHE-CONTROL: max-age=1800
            DATE: Mon, 12 Oct 2026 21:00:00 GMT
            EXT:
            LOCATION: http://192.168.1.150:9197/dmr
            SERVER: Linux/3.14.0 UPnP/1.0 PRO-ROKU/10.5
            ST: urn:schemas-upnp-org:device:MediaRenderer:1
            USN: uuid:12345678-1234-1234-1234-123456789012::urn:schemas-upnp-org:device:MediaRenderer:1
        """.trimIndent().replace("\n", "\r\n")

        val resp = SsdpPacketParser.parse(raw.toByteArray(StandardCharsets.UTF_8))
        assertNotNull(resp)
        assertEquals("HTTP/1.1 200 OK", resp!!.startLine)
        assertEquals("http://192.168.1.150:9197/dmr", resp.location)
        assertEquals("Linux/3.14.0 UPnP/1.0 PRO-ROKU/10.5", resp.server)
        assertEquals("urn:schemas-upnp-org:device:MediaRenderer:1", resp.st)
        assertEquals("uuid:12345678-1234-1234-1234-123456789012::urn:schemas-upnp-org:device:MediaRenderer:1", resp.usn)
    }

    @Test
    fun testParseNotifyPacket() {
        val raw = """
            NOTIFY * HTTP/1.1
            HOST: 239.255.255.250:1900
            LOCATION: http://192.168.1.50:8008/ssdp/device-desc.xml
            NT: urn:dial-multiscreen-org:service:dial:1
            NTS: ssdp:alive
            SERVER: Linux/4.19 UPnP/1.0 Google-Home/1.0
            USN: uuid:google-cast-1
        """.trimIndent().replace("\n", "\r\n")

        val resp = SsdpPacketParser.parse(raw.toByteArray(StandardCharsets.UTF_8))
        assertNotNull(resp)
        assertEquals("NOTIFY * HTTP/1.1", resp!!.startLine)
        assertEquals("http://192.168.1.50:8008/ssdp/device-desc.xml", resp.location)
        assertEquals("urn:dial-multiscreen-org:service:dial:1", resp.st)
        assertEquals("Linux/4.19 UPnP/1.0 Google-Home/1.0", resp.server)
        assertEquals("uuid:google-cast-1", resp.usn)
    }

    @Test
    fun testParseInvalidPacket() {
        val raw = "GARBAGE DATA WITHOUT HTTP OR NOTIFY"
        val resp = SsdpPacketParser.parse(raw.toByteArray(StandardCharsets.UTF_8))
        assertNull(resp)
    }
}
