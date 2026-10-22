package es.routerapp.protocol.optical

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GponAuthModelTest {

    @Test
    fun testOltModeFromCode() {
        assertEquals(OltMode.NORMAL, OltMode.fromCode(0))
        assertEquals(OltMode.MODE_H, OltMode.fromCode(1))
        assertEquals(OltMode.MODE_Z, OltMode.fromCode(2))
        assertEquals(OltMode.MODE_F, OltMode.fromCode(3))
        assertEquals(OltMode.MODE_X, OltMode.fromCode(4))
        assertEquals(OltMode.MODE_HH, OltMode.fromCode(5))
        assertEquals("HWTC", OltMode.MODE_HH.vendorId)
        assertEquals("TPLG", OltMode.NORMAL.vendorId)
        // Fallback
        assertEquals(OltMode.NORMAL, OltMode.fromCode(99))
    }

    @Test
    fun testGponAuthInfoFromMap() {
        val raw = mapOf(
            "gponSn" to "HWTC12345678",
            "snType" to "0",
            "gponPwd" to "digi1234",
            "pwdType" to "0",
            "oltMode" to "1",
        )
        val auth = GponAuthInfo.fromMap(raw)
        assertEquals("HWTC12345678", auth.rawSn)
        assertEquals("HWTC12345678", auth.snAscii)
        assertEquals("4857544312345678", auth.snHex)
        assertEquals("digi1234", auth.gponPwd)
        assertEquals(OltMode.MODE_H, auth.oltMode)
        assertTrue(auth.isPopulated)
    }

    @Test
    fun testWanRenewProgressState() {
        val idle = WanRenewProgress()
        assertFalse(idle.isInProgress)

        val inProgress = WanRenewProgress(stage = WanRenewStage.DISCONNECTING)
        assertTrue(inProgress.isInProgress)

        val verifying = WanRenewProgress(stage = WanRenewStage.VERIFYING)
        assertTrue(verifying.isInProgress)

        val success = WanRenewProgress(
            stage = WanRenewStage.SUCCESS,
            oldIp = "84.120.1.1",
            newIp = "84.120.1.2",
            elapsedSeconds = 3.5f,
        )
        assertFalse(success.isInProgress)
        assertEquals("84.120.1.2", success.newIp)
    }
}
