package es.routerapp.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalNetworkValidatorTest {

    @Test
    fun `extractHost correctly parses various host and URL inputs`() {
        assertEquals("192.168.1.1", LocalNetworkValidator.extractHost("192.168.1.1"))
        assertEquals("192.168.1.1", LocalNetworkValidator.extractHost("http://192.168.1.1"))
        assertEquals("192.168.1.1", LocalNetworkValidator.extractHost("http://192.168.1.1:80/"))
        assertEquals("192.168.1.1", LocalNetworkValidator.extractHost("https://192.168.1.1:8443/cgi/login"))
        assertEquals("router.local", LocalNetworkValidator.extractHost("http://router.local:8080"))
        assertEquals("fe80::1", LocalNetworkValidator.extractHost("[fe80::1]:80"))
    }

    @Test
    fun `isLocalAddress accepts RFC 1918 private IPv4 ranges`() {
        assertTrue(LocalNetworkValidator.isLocalAddress("192.168.1.1"))
        assertTrue(LocalNetworkValidator.isLocalAddress("192.168.0.1"))
        assertTrue(LocalNetworkValidator.isLocalAddress("192.168.100.254"))
        assertTrue(LocalNetworkValidator.isLocalAddress("10.0.0.1"))
        assertTrue(LocalNetworkValidator.isLocalAddress("10.254.1.5"))
        assertTrue(LocalNetworkValidator.isLocalAddress("172.16.0.1"))
        assertTrue(LocalNetworkValidator.isLocalAddress("172.31.255.254"))
    }

    @Test
    fun `isLocalAddress accepts loopback and link-local addresses`() {
        assertTrue(LocalNetworkValidator.isLocalAddress("127.0.0.1"))
        assertTrue(LocalNetworkValidator.isLocalAddress("169.254.1.1"))
    }

    @Test
    fun `isLocalAddress accepts local and router-specific domain names`() {
        assertTrue(LocalNetworkValidator.isLocalAddress("localhost"))
        assertTrue(LocalNetworkValidator.isLocalAddress("tplinkwifi.net"))
        assertTrue(LocalNetworkValidator.isLocalAddress("tplinklogin.net"))
        assertTrue(LocalNetworkValidator.isLocalAddress("router.local"))
        assertTrue(LocalNetworkValidator.isLocalAddress("myrouter.lan"))
        assertTrue(LocalNetworkValidator.isLocalAddress("gateway.home.arpa"))
        assertTrue(LocalNetworkValidator.isLocalAddress("router"))
    }

    @Test
    fun `isLocalAddress rejects public IP addresses`() {
        assertFalse(LocalNetworkValidator.isLocalAddress("8.8.8.8"))
        assertFalse(LocalNetworkValidator.isLocalAddress("1.1.1.1"))
        assertFalse(LocalNetworkValidator.isLocalAddress("93.184.216.34"))
        assertFalse(LocalNetworkValidator.isLocalAddress("172.32.0.1")) // Just above 172.31.x.x
        assertFalse(LocalNetworkValidator.isLocalAddress("192.169.1.1"))
        assertFalse(LocalNetworkValidator.isLocalAddress("11.0.0.1"))
    }

    @Test
    fun `isLocalAddress rejects public internet domains and empty strings`() {
        assertFalse(LocalNetworkValidator.isLocalAddress("example.com"))
        assertFalse(LocalNetworkValidator.isLocalAddress("google.com"))
        assertFalse(LocalNetworkValidator.isLocalAddress("api.github.com"))
        assertFalse(LocalNetworkValidator.isLocalAddress(""))
        assertFalse(LocalNetworkValidator.isLocalAddress("   "))
    }
}
