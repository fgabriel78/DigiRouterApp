package es.routerapp.protocol.fingerprint

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class HttpDescriptorParserTest {

    @Test
    fun testParseLgTvUpnpXml() {
        val xml = """
            <?xml version="1.0" encoding="utf-8"?>
            <root xmlns="urn:schemas-upnp-org:device-1-0">
                <device>
                    <deviceType>urn:schemas-upnp-org:device:MediaRenderer:1</deviceType>
                    <friendlyName>LG OLED55C26LD</friendlyName>
                    <manufacturer>LG Electronics</manufacturer>
                    <modelName>LG TV</modelName>
                    <modelNumber>OLED55C26LD</modelNumber>
                    <modelDescription>LG OLED TV Media Renderer</modelDescription>
                </device>
            </root>
        """.trimIndent()

        val desc = HttpDescriptorParser.parseUpnpXml(xml, "Linux/3.14 UPnP/1.0 webOS/7.0")
        assertNotNull(desc)
        assertEquals("LG OLED55C26LD", desc.friendlyName)
        assertEquals("LG Electronics", desc.manufacturer)
        assertEquals("LG TV", desc.modelName)
        assertEquals("OLED55C26LD", desc.modelNumber)
        assertEquals("Linux/3.14 UPnP/1.0 webOS/7.0", desc.serverHeader)
    }

    @Test
    fun testParsePhilipsHueXml() {
        val xml = """
            <root xmlns="urn:schemas-upnp-org:device-1-0">
                <device>
                    <deviceType>urn:schemas-upnp-org:device:Basic:1</deviceType>
                    <friendlyName><![CDATA[Philips hue (192.168.1.54)]]></friendlyName>
                    <manufacturer>Signify</manufacturer>
                    <modelName>Philips hue bridge 2015</modelName>
                    <modelNumber>BSB002</modelNumber>
                </device>
            </root>
        """.trimIndent()

        val desc = HttpDescriptorParser.parseAuto(xml)
        assertEquals("Philips hue (192.168.1.54)", desc.friendlyName)
        assertEquals("Signify", desc.manufacturer)
        assertEquals("Philips hue bridge 2015", desc.modelName)
        assertEquals("BSB002", desc.modelNumber)
    }

    @Test
    fun testParseGoogleCastJson() {
        val json = """
            {
                "name": "Living Room TV",
                "model_name": "Chromecast with Google TV",
                "manufacturer": "Google Inc."
            }
        """.trimIndent()

        val desc = HttpDescriptorParser.parseJsonMetadata(json)
        assertEquals("Living Room TV", desc.friendlyName)
        assertEquals("Chromecast with Google TV", desc.modelName)
        assertEquals("Google Inc.", desc.manufacturer)
    }

    @Test
    fun testParseShellyJson() {
        val json = """
            {
                "type": "SHSW-1",
                "mac": "A4C138123456",
                "auth": false
            }
        """.trimIndent()

        val desc = HttpDescriptorParser.parseAuto(json)
        assertEquals("Shelly", desc.manufacturer)
        assertEquals("SHSW-1", desc.modelName)
    }
}
