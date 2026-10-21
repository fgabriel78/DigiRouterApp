package es.routerapp.protocol.log

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LogParserTest {

    @Test
    fun testParseStandardEntries() {
        val raw = """
            0
            2026-10-20 22:45:00 [6] SYS: Router boot completed
            2026-10-20 22:46:12 [4] WIFI: Station deauthenticated MAC=aa:bb:cc:dd:ee:ff
            2026-10-20 22:47:30 [3] OMCI: GPON loss of signal detected
            2026-10-20 22:48:15 [0] EMERG: Hardware watchdog triggered
        """.trimIndent()

        val entries = LogParser.parse(raw)

        // Verifies reverse order (newest first)
        assertEquals(4, entries.size)
        assertEquals("2026-10-20 22:48:15", entries[0].timestamp)
        assertEquals(LogSeverity.EMERGENCY, entries[0].severity)
        assertEquals("EMERG", entries[0].module)
        assertEquals("Hardware watchdog triggered", entries[0].message)

        assertEquals("2026-10-20 22:47:30", entries[1].timestamp)
        assertEquals(LogSeverity.ERROR, entries[1].severity)
        assertEquals("OMCI", entries[1].module)

        assertEquals("2026-10-20 22:46:12", entries[2].timestamp)
        assertEquals(LogSeverity.WARNING, entries[2].severity)
        assertEquals("WIFI", entries[2].module)

        assertEquals("2026-10-20 22:45:00", entries[3].timestamp)
        assertEquals(LogSeverity.INFO, entries[3].severity)
        assertEquals("SYS", entries[3].module)
    }

    @Test
    fun testParseEmptyAndNullResponses() {
        assertTrue(LogParser.parse(null).isEmpty())
        assertTrue(LogParser.parse("").isEmpty())
        assertTrue(LogParser.parse("   \n  \r\n").isEmpty())
        assertTrue(LogParser.parse("0\n").isEmpty())
    }

    @Test
    fun testParseNonZeroReturnCode() {
        val raw = """
            71233
            2026-10-20 22:45:00 [6] SYS: Router boot completed
        """.trimIndent()

        // Non-zero return code signifies an error response from router
        val entries = LogParser.parse(raw)
        assertTrue(entries.isEmpty())
    }

    @Test
    fun testMalformedLinesAreSafelyIgnored() {
        val raw = """
            0
            2026-10-20 22:45:00 [6] SYS: Normal valid line
            Some garbage line from kernel printk
            Invalid date format [3] WIFI: Oops
            2026-10-20 22:50:00 [2] CRIT: Critical power anomaly
        """.trimIndent()

        val entries = LogParser.parse(raw)
        assertEquals(2, entries.size)
        assertEquals("2026-10-20 22:50:00", entries[0].timestamp)
        assertEquals(LogSeverity.CRITICAL, entries[0].severity)
        assertEquals("2026-10-20 22:45:00", entries[1].timestamp)
        assertEquals(LogSeverity.INFO, entries[1].severity)
    }

    @Test
    fun testSeverityFallbackAndMapping() {
        val raw = """
            0
            2026-10-20 22:45:00 [1] SEC: Intrusion alert
            2026-10-20 22:46:00 [5] NOTICE: DHCP lease extended
            2026-10-20 22:47:00 [7] DEBUG: Packet trace 192.168.1.1
            2026-10-20 22:48:00 [9] UNKNOWN: Unknown level defaults to INFO
        """.trimIndent()

        val entries = LogParser.parse(raw)
        assertEquals(4, entries.size)
        assertEquals(LogSeverity.INFO, entries[0].severity) // [9] falls back to INFO
        assertEquals(LogSeverity.DEBUG, entries[1].severity) // [7]
        assertEquals(LogSeverity.NOTICE, entries[2].severity) // [5]
        assertEquals(LogSeverity.ALERT, entries[3].severity) // [1]
    }
}
