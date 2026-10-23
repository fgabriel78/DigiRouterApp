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
        assertEquals(Group.NAT, page?.group)
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

    @Test
    fun testSystemLogAdminRestrictedInDiagnosticsGroup() {
        val page = Catalog.byId("system-log")
        assertNotNull(page)
        assertEquals("System Log", page?.title)
        assertEquals(Group.ASSISTANT, page?.group)
        assertTrue(page?.adminOnly == true)
        assertTrue(page?.custom == true)

        val adminPages = Catalog.visiblePages("admin")
        assertTrue(adminPages.any { it.id == "system-log" })

        val userPages = Catalog.visiblePages("user")
        assertFalse(userPages.any { it.id == "system-log" })
    }

    @Test
    fun testTopologyRegisteredInStatusGroup() {
        val page = Catalog.byId("topology")
        assertNotNull(page)
        assertEquals("Network topology", page?.title)
        assertEquals(Group.STATUS, page?.group)
        assertTrue(page?.custom == true)
        assertFalse(page?.adminOnly == true)
    }

    @Test
    fun testLegacyAliasesResolveToConsolidatedPages() {
        assertEquals("wifi-adv", Catalog.byId("wps")?.id)
        assertEquals("guest", Catalog.byId("mssid")?.id)
        assertEquals("dhcp-static", Catalog.byId("arp")?.id)
        assertEquals("upnp", Catalog.byId("port-trigger")?.id)
        assertEquals("dmz", Catalog.byId("alg")?.id)
        assertEquals("time", Catalog.byId("led")?.id)
        assertEquals("time", Catalog.byId("stats")?.id)
    }

    @Test
    fun testAllPagesHaveNonEmptyDescriptions() {
        for (page in Catalog.pages) {
            assertFalse(
                "Page ${page.id} has blank or missing description",
                page.description.isNullOrBlank(),
            )
        }
    }
}
