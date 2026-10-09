package es.routerapp.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceResolverTest {

    // Sony OUI (00:01:4A)
    private val HARDWARE_MAC_SONY = "00:01:4A:11:22:33"
    // Randomized / Locally Administered MAC (da:a1:...)
    private val PRIVATE_MAC = "DA:A1:19:33:44:55"

    @Test
    fun `user custom alias and category take highest precedence`() {
        val custom = DeviceCustomMetadata(
            alias = "Mi Tele del Salon",
            category = DeviceCategory.TV,
        )

        val resolved = DeviceResolver.resolve(
            mac = PRIVATE_MAC,
            hostname = "iPhone-de-Carlos",
            ip = "192.168.1.50",
            customMeta = custom,
        )

        assertEquals("Mi Tele del Salon", resolved.displayTitle)
        assertEquals(DeviceCategory.TV, resolved.category)
        assertFalse(resolved.isCategoryInferred)
        assertTrue(resolved.isPrivateMac)
        assertEquals("Apple", resolved.vendor)
        assertEquals(VendorSource.HOSTNAME_HEURISTIC, resolved.vendorSource)
    }

    @Test
    fun `hardware OUI takes precedence over heuristic vendor`() {
        // Sony physical MAC with a confusing hostname
        val resolved = DeviceResolver.resolve(
            mac = HARDWARE_MAC_SONY,
            hostname = "Pixel-Test-Rig",
            ip = "192.168.1.60",
        )

        assertEquals("Pixel-Test-Rig", resolved.displayTitle)
        assertEquals("Sony", resolved.vendor)
        assertEquals(VendorSource.HARDWARE_OUI, resolved.vendorSource)
        assertFalse(resolved.isPrivateMac)
        // Heuristic category can still be inferred if none is set
        assertEquals(DeviceCategory.PHONE, resolved.category)
        assertTrue(resolved.isCategoryInferred)
    }

    @Test
    fun `inferred vendor and category used when MAC is private`() {
        val resolved = DeviceResolver.resolve(
            mac = PRIVATE_MAC,
            hostname = "Galaxy-S23",
            ip = "192.168.1.70",
        )

        assertEquals("Galaxy-S23", resolved.displayTitle)
        assertEquals("Samsung", resolved.vendor)
        assertEquals(VendorSource.HOSTNAME_HEURISTIC, resolved.vendorSource)
        assertTrue(resolved.isPrivateMac)
        assertEquals(DeviceCategory.PHONE, resolved.category)
        assertTrue(resolved.isCategoryInferred)
    }

    @Test
    fun `handles unknown private MAC without matching heuristic`() {
        val resolved = DeviceResolver.resolve(
            mac = PRIVATE_MAC,
            hostname = "node-client-x",
            ip = "192.168.1.80",
        )

        assertEquals("node-client-x", resolved.displayTitle)
        assertNull(resolved.vendor)
        assertEquals(VendorSource.NONE, resolved.vendorSource)
        assertTrue(resolved.isPrivateMac)
        assertNull(resolved.category)
        assertFalse(resolved.isCategoryInferred)
    }

    @Test
    fun `fallback display title hierarchy when hostname is blank`() {
        // When hostname is empty, but heuristic or vendor exists
        val withVendor = DeviceResolver.resolve(
            mac = HARDWARE_MAC_SONY,
            hostname = "",
            ip = "192.168.1.90",
        )
        assertEquals("Sony", withVendor.displayTitle)

        // When no vendor and no hostname, fall back to IP
        val withIp = DeviceResolver.resolve(
            mac = "02:00:00:00:00:01",
            hostname = "",
            ip = "192.168.1.91",
        )
        assertEquals("192.168.1.91", withIp.displayTitle)

        // When no IP, fall back to MAC
        val withMac = DeviceResolver.resolve(
            mac = "02:00:00:00:00:02",
            hostname = null,
            ip = null,
        )
        assertEquals("02:00:00:00:00:02", withMac.displayTitle)
    }
}
