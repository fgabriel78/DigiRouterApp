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
}
