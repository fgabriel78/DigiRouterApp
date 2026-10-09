package es.routerapp.protocol.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RiskyPortCatalogTest {

    @Test
    fun `detects all specified dangerous ports`() {
        val dangerousPorts = listOf(21, 22, 23, 25, 53, 80, 137, 138, 139, 445, 1433, 3306, 3389, 5432, 5900, 6379, 27017)
        for (port in dangerousPorts) {
            assertTrue("Expected port $port to be dangerous", RiskyPortCatalog.isDangerous(port))
            val info = RiskyPortCatalog.evaluatePort(port)
            assertNotNull("Expected info for port $port", info)
            assertEquals(port, info?.port)
        }
    }

    @Test
    fun `identifies safe gaming ports correctly`() {
        val gamingPorts = listOf(3074, 3478, 9308, 27015, 35000, 49152)
        for (port in gamingPorts) {
            assertFalse("Expected port $port not to be dangerous", RiskyPortCatalog.isDangerous(port))
            assertNull("Expected null info for safe port $port", RiskyPortCatalog.evaluatePort(port))
            assertTrue("Expected port $port to be recognized as safe gaming", RiskyPortCatalog.isKnownSafeGamingPort(port))
        }
    }

    @Test
    fun `evaluates non-dangerous non-gaming standard port`() {
        val normalPort = 8443
        assertFalse(RiskyPortCatalog.isDangerous(normalPort))
        assertNull(RiskyPortCatalog.evaluatePort(normalPort))
    }
}
