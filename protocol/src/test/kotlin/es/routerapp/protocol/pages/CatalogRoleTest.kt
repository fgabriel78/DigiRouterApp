package es.routerapp.protocol.pages

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogRoleTest {

    @Test
    fun testParentalControlsAdminRestricted() {
        val page = Catalog.byId("parental-control")
        assertNotNull(page)
        assertEquals("Parental Controls", page?.title)
        assertEquals(Group.SECURITY, page?.group)
        assertTrue(page?.adminOnly == true)
        assertTrue(page?.custom == true)

        val adminPages = Catalog.visiblePages("admin")
        assertTrue(adminPages.any { it.id == "parental-control" })

        val userPages = Catalog.visiblePages("user")
        assertFalse(userPages.any { it.id == "parental-control" })
    }

    @Test
    fun testVpnServerAdminRestricted() {
        val page = Catalog.byId("vpn")
        assertNotNull(page)
        assertEquals("VPN Server", page?.title)
        assertEquals(Group.NETWORK, page?.group)
        assertTrue(page?.adminOnly == true)
        assertTrue(page?.custom == true)

        val adminPages = Catalog.visiblePages("admin")
        assertTrue(adminPages.any { it.id == "vpn" })

        val userPages = Catalog.visiblePages("user")
        assertFalse(userPages.any { it.id == "vpn" })
    }

    @Test
    fun testRemoteManagementAdminRestricted() {
        val page = Catalog.byId("remote-mgmt")
        assertNotNull(page)
        assertEquals("Remote Management", page?.title)
        assertEquals(Group.SYSTEM, page?.group)
        assertTrue(page?.adminOnly == true)
        assertTrue(page?.custom == true)

        val adminPages = Catalog.visiblePages("admin")
        assertTrue(adminPages.any { it.id == "remote-mgmt" })

        val userPages = Catalog.visiblePages("user")
        assertFalse(userPages.any { it.id == "remote-mgmt" })
    }
}
