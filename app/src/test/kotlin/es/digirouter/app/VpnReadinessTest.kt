package es.digirouter.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VpnReadinessTest {

    @Test
    fun detectsCgnatRangeCorrectly() {
        assertTrue(VpnReadinessEvaluator.isCgnat("100.64.0.1"))
        assertTrue(VpnReadinessEvaluator.isCgnat("100.100.50.25"))
        assertTrue(VpnReadinessEvaluator.isCgnat("100.127.255.254"))

        assertFalse(VpnReadinessEvaluator.isCgnat("100.63.255.255"))
        assertFalse(VpnReadinessEvaluator.isCgnat("100.128.0.1"))
        assertFalse(VpnReadinessEvaluator.isCgnat("84.120.10.1"))
        assertFalse(VpnReadinessEvaluator.isCgnat("192.168.1.1"))
    }

    @Test
    fun detectsPrivateRangesCorrectly() {
        assertTrue(VpnReadinessEvaluator.isPrivateIp("192.168.1.1"))
        assertTrue(VpnReadinessEvaluator.isPrivateIp("10.0.0.1"))
        assertTrue(VpnReadinessEvaluator.isPrivateIp("172.20.0.1"))
        assertTrue(VpnReadinessEvaluator.isPrivateIp("127.0.0.1"))

        assertFalse(VpnReadinessEvaluator.isPrivateIp("84.120.10.1"))
        assertFalse(VpnReadinessEvaluator.isPrivateIp("100.64.1.1"))
    }

    @Test
    fun evaluatesReadinessLevels() {
        // Active DDNS always gives READY_DDNS
        val ddnsResult = VpnReadinessEvaluator.evaluate(
            wanIp = "100.64.1.5",
            ddnsDomain = "myhome.tplinkdns.com",
            isDdnsActive = true,
        )
        assertEquals(VpnReadinessLevel.READY_DDNS, ddnsResult.level)

        // CGNAT without DDNS gives WARNING_CGNAT_NO_DDNS
        val cgnatResult = VpnReadinessEvaluator.evaluate(
            wanIp = "100.75.12.3",
            ddnsDomain = null,
            isDdnsActive = false,
        )
        assertEquals(VpnReadinessLevel.WARNING_CGNAT_NO_DDNS, cgnatResult.level)

        // RFC 1918 Private IP without DDNS gives WARNING_PRIVATE_IP_NO_DDNS
        val privateResult = VpnReadinessEvaluator.evaluate(
            wanIp = "192.168.100.1",
            ddnsDomain = "",
            isDdnsActive = false,
        )
        assertEquals(VpnReadinessLevel.WARNING_PRIVATE_IP_NO_DDNS, privateResult.level)

        // Public IP gives READY_PUBLIC_IP
        val publicResult = VpnReadinessEvaluator.evaluate(
            wanIp = "84.120.55.99",
            ddnsDomain = null,
            isDdnsActive = false,
        )
        assertEquals(VpnReadinessLevel.READY_PUBLIC_IP, publicResult.level)
    }
}
