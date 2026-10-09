package es.routerapp.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OuiLookupTest {

    @Test
    fun `resolves known manufacturers accurately`() {
        // Apple
        val apple = OuiLookup.resolve("BC:92:6B:11:22:33")
        assertFalse(apple.isPrivate)
        assertEquals("Apple", apple.vendor)
        assertEquals("Apple", apple.displayVendor)

        // Apple with lowercase and hyphens
        val appleLower = OuiLookup.resolve("00-17-f2-aa-bb-cc")
        assertFalse(appleLower.isPrivate)
        assertEquals("Apple", appleLower.vendor)

        // Sony
        val sony = OuiLookup.resolve("F0:BF:97:12:34:56")
        assertFalse(sony.isPrivate)
        assertEquals("Sony", sony.vendor)
        assertEquals("Sony", sony.displayVendor)

        // Samsung
        val samsung = OuiLookup.resolve("50:85:69:AA:BB:CC")
        assertFalse(samsung.isPrivate)
        assertEquals("Samsung", samsung.vendor)
        assertEquals("Samsung", samsung.displayVendor)

        // Espressif
        val espressif = OuiLookup.resolve("24:0A:C4:00:11:22")
        assertFalse(espressif.isPrivate)
        assertEquals("Espressif", espressif.vendor)
        assertEquals("Espressif", espressif.displayVendor)
    }

    @Test
    fun `detects IEEE 802 locally administered randomized MAC addresses`() {
        // Second least-significant bit of first byte is set (x2, x6, xA, xE)
        val privateMacs = listOf(
            "02:00:00:11:22:33",
            "DA:A1:19:22:33:44",
            "7E:11:22:33:44:55",
            "06:12:34:56:78:90",
            "FE:AA:BB:CC:DD:EE",
        )

        for (mac in privateMacs) {
            assertTrue("MAC $mac should be identified as locally administered", OuiLookup.isLocallyAdministered(mac))
            val res = OuiLookup.resolve(mac)
            assertTrue("MAC $mac should be flagged as private", res.isPrivate)
            assertNull("Private MAC $mac should not match a vendor", res.vendor)
            assertNull("Private MAC $mac should not have a display vendor", res.displayVendor)
        }
    }

    @Test
    fun `globally unique unknown MAC returns null vendor and not private`() {
        val unknown = OuiLookup.resolve("00:00:01:22:33:44")
        assertFalse(unknown.isPrivate)
        assertNull(unknown.vendor)
        assertNull(unknown.displayVendor)
    }

    @Test
    fun `handles empty and invalid MAC strings gracefully`() {
        assertFalse(OuiLookup.isLocallyAdministered(""))
        assertFalse(OuiLookup.isLocallyAdministered("Z"))

        val emptyRes = OuiLookup.resolve("")
        assertFalse(emptyRes.isPrivate)
        assertNull(emptyRes.vendor)

        val invalidRes = OuiLookup.resolve("invalid-mac")
        assertFalse(invalidRes.isPrivate)
        assertNull(invalidRes.vendor)
    }
}
