package es.routerapp.protocol.vpn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OpenVpnProfileTransformerTest {

    private val sampleProfile = """
        client
        dev tun
        proto udp
        remote 192.168.1.1 1194
        resolv-retry infinite
        nobind
        persist-key
        persist-tun
        <ca>
        -----BEGIN CERTIFICATE-----
        MIIB...
        -----END CERTIFICATE-----
        </ca>
    """.trimIndent()

    @Test
    fun extractsRemoteHostAndPort() {
        assertEquals("192.168.1.1", OpenVpnProfileTransformer.extractRemoteHost(sampleProfile))
        assertEquals(1194, OpenVpnProfileTransformer.extractRemotePort(sampleProfile))
    }

    @Test
    fun returnsNullWhenNoRemoteDirective() {
        val noRemote = "client\ndev tun\nproto udp\n"
        assertNull(OpenVpnProfileTransformer.extractRemoteHost(noRemote))
        assertNull(OpenVpnProfileTransformer.extractRemotePort(noRemote))
    }

    @Test
    fun overridesRemoteHostSuccessfully() {
        val transformed = OpenVpnProfileTransformer.overrideRemoteHost(sampleProfile, "myhome.tplinkdns.com")
        assertEquals("myhome.tplinkdns.com", OpenVpnProfileTransformer.extractRemoteHost(transformed))
        assertEquals(1194, OpenVpnProfileTransformer.extractRemotePort(transformed))
    }

    @Test
    fun preservesOriginalWhenHostIsBlankOrNull() {
        val emptyOverride = OpenVpnProfileTransformer.overrideRemoteHost(sampleProfile, "")
        assertEquals(sampleProfile, emptyOverride)

        val nullOverride = OpenVpnProfileTransformer.overrideRemoteHost(sampleProfile, null)
        assertEquals(sampleProfile, nullOverride)
    }
}
