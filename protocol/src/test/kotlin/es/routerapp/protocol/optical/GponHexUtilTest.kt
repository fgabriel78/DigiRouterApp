package es.routerapp.protocol.optical

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GponHexUtilTest {

    @Test
    fun testAsciiToHex() {
        // HWTC: 'H'=0x48, 'W'=0x57, 'T'=0x54, 'C'=0x43
        val hex = GponHexUtil.asciiToHex("HWTC12345678")
        assertEquals("4857544312345678", hex)

        // TPLG: 'T'=0x54, 'P'=0x50, 'L'=0x4c, 'G'=0x47
        val hexTplg = GponHexUtil.asciiToHex("TPLGabcdef01")
        assertEquals("54504C47ABCDEF01", hexTplg)

        // Invalid inputs
        assertNull(GponHexUtil.asciiToHex("SHORT"))
        assertNull(GponHexUtil.asciiToHex("HWTC1234567G")) // G is not a hex digit
    }

    @Test
    fun testHexToAscii() {
        val ascii = GponHexUtil.hexToAscii("4857544312345678")
        assertEquals("HWTC12345678", ascii)

        val asciiTplg = GponHexUtil.hexToAscii("54504C47ABCDEF01")
        assertEquals("TPLGABCDEF01", asciiTplg)

        // Invalid inputs
        assertNull(GponHexUtil.hexToAscii("48575443")) // Too short
        assertNull(GponHexUtil.hexToAscii("0000000012345678")) // Non-printable null chars
    }

    @Test
    fun testNormalize() {
        // From ASCII+HEX
        val fromAscii = GponHexUtil.normalize("HWTC12345678")
        assertNotNull(fromAscii)
        assertEquals("HWTC12345678", fromAscii?.first)
        assertEquals("4857544312345678", fromAscii?.second)

        // From pure HEX
        val fromHex = GponHexUtil.normalize("4857544312345678")
        assertNotNull(fromHex)
        assertEquals("HWTC12345678", fromHex?.first)
        assertEquals("4857544312345678", fromHex?.second)

        // Invalid
        assertNull(GponHexUtil.normalize("INVALID_LENGTH"))
    }

    @Test
    fun testGenerateSfpExportSnippet() {
        val snippet = GponHexUtil.generateSfpExportSnippet(
            snAscii = "HWTC12345678",
            snHex = "4857544312345678",
            ploamPassword = "secretPassword",
            oltModeName = "Mode H",
        )
        assertTrue(snippet.contains("GPON_SN_ASCII=\"HWTC12345678\""))
        assertTrue(snippet.contains("GPON_SN_HEX=\"4857544312345678\""))
        assertTrue(snippet.contains("PLOAM_PASSWORD=\"secretPassword\""))
        assertTrue(snippet.contains("OLT_MODE=\"Mode H\""))
        assertTrue(snippet.contains("fw_setenv gpon_sn 4857544312345678"))
    }
}
