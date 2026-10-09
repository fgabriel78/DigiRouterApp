package es.routerapp.protocol.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityAuditEngineTest {

    private fun safeCleanData() = SecurityRawData(
        dmz = listOf(mapOf("enable" to "0", "IPAddress" to "192.168.1.100")),
        wifi = listOf(
            mapOf(
                "primaryEnable" to "1",
                "primaryModeEnabled" to "WPA2-WPA3-Personal",
                "primaryPSK" to "SuperSecurePassphrase123!",
                "WPSEnable" to "0",
                "guestEnable" to "0",
                "guestLANAccessEnable" to "0",
            )
        ),
        upnpCfg = listOf(mapOf("enable" to "1")),
        upnpMappings = listOf(
            mapOf("externalPort" to "3074", "description" to "Xbox Live"),
            mapOf("externalPort" to "9308", "description" to "PlayStation"),
        ),
        portMappings = emptyList(),
        ddos = listOf(
            mapOf(
                "enable" to "1",
                "enableSynFilter" to "1",
                "enableUdpFilter" to "1",
                "enableIcmpFilter" to "1",
            )
        ),
        ftp = listOf(mapOf("enable" to "0", "accessFromInternet" to "0", "anonymous" to "0")),
        smb = listOf(mapOf("enable" to "0", "anonymous" to "0")),
    )

    @Test
    fun `perfect security configuration returns 100 score and EXCELLENT rating`() {
        val report = SecurityAuditEngine.evaluate(safeCleanData())
        assertEquals(100, report.score)
        assertEquals(HealthRating.EXCELLENT, report.rating)
        assertEquals(0, report.criticalCount)
        assertEquals(0, report.warningCount)
        assertEquals(10, report.passedCount)
        assertTrue(report.batchPlan.isEmpty())
    }

    @Test
    fun `active DMZ deducts 25 points and provides automatic remediation`() {
        val data = safeCleanData().copy(
            dmz = listOf(mapOf("enable" to "1", "IPAddress" to "192.168.1.50"))
        )
        val report = SecurityAuditEngine.evaluate(data)
        assertEquals(75, report.score)
        assertEquals(HealthRating.GOOD, report.rating)
        assertEquals(1, report.criticalCount)
        val dmzFinding = report.findings.first { it.checkId == "dmz" }
        assertFalse(dmzFinding.isPassed)
        assertEquals(Severity.CRITICAL, dmzFinding.severity)
        assertEquals(25, dmzFinding.penaltyPoints)
        assertNotNull(dmzFinding.remediation)
        val rem = dmzFinding.remediation!!
        assertTrue(rem.isAutomaticSafe)
        assertEquals("0", rem.changes["enable"])

        assertEquals(1, report.batchPlan.size)
        assertEquals("disable_dmz", report.batchPlan.first().id)
    }

    @Test
    fun `open unencrypted wifi deducts 25 points and flags critical finding without auto fix`() {
        val data = safeCleanData().copy(
            wifi = listOf(
                mapOf(
                    "primaryEnable" to "1",
                    "primaryModeEnabled" to "None",
                    "primaryPSK" to "",
                    "WPSEnable" to "0",
                )
            )
        )
        val report = SecurityAuditEngine.evaluate(data)
        assertEquals(75, report.score)
        assertEquals(1, report.criticalCount)
        val wifiFinding = report.findings.first { it.checkId == "wifi_mode" }
        assertFalse(wifiFinding.isPassed)
        assertEquals(Severity.CRITICAL, wifiFinding.severity)
        val wifiRem = wifiFinding.remediation
        assertNotNull(wifiRem)
        assertFalse(wifiRem!!.isAutomaticSafe)
        assertEquals("wifi", wifiRem.targetScreenId)
        assertTrue(report.batchPlan.isEmpty())
    }

    @Test
    fun `wps enabled deducts 10 points and provides safe remediation`() {
        val data = safeCleanData().copy(
            wifi = listOf(
                mapOf(
                    "primaryEnable" to "1",
                    "primaryModeEnabled" to "WPA2-Personal",
                    "primaryPSK" to "ValidSecretLongPhrase",
                    "WPSEnable" to "1",
                )
            )
        )
        val report = SecurityAuditEngine.evaluate(data)
        assertEquals(90, report.score)
        assertEquals(1, report.warningCount)
        val wpsFinding = report.findings.first { it.checkId == "wps" }
        assertFalse(wpsFinding.isPassed)
        assertEquals(Severity.MEDIUM, wpsFinding.severity)
        assertEquals(10, wpsFinding.penaltyPoints)
        val wpsRem = wpsFinding.remediation
        assertNotNull(wpsRem)
        assertTrue(wpsRem!!.isAutomaticSafe)
        assertEquals("0", wpsRem.changes["WPSEnable"])
    }

    @Test
    fun `guest network with LAN access enabled deducts 15 points and isolates via batch plan`() {
        val data = safeCleanData().copy(
            wifi = listOf(
                mapOf(
                    "primaryEnable" to "1",
                    "primaryModeEnabled" to "WPA2-Personal",
                    "primaryPSK" to "ValidSecretLongPhrase",
                    "WPSEnable" to "0",
                    "guestEnable" to "1",
                    "guestLANAccessEnable" to "1",
                )
            )
        )
        val report = SecurityAuditEngine.evaluate(data)
        assertEquals(85, report.score)
        val guestFinding = report.findings.first { it.checkId == "guest" }
        assertFalse(guestFinding.isPassed)
        assertEquals(Severity.HIGH, guestFinding.severity)
        assertEquals(15, guestFinding.penaltyPoints)
        val guestRem = guestFinding.remediation
        assertNotNull(guestRem)
        assertTrue(guestRem!!.isAutomaticSafe)
        assertEquals("0", guestRem.changes["guestLANAccessEnable"])
    }

    @Test
    fun `upnp with benign gaming ports does not penalize score`() {
        val data = safeCleanData().copy(
            upnpCfg = listOf(mapOf("enable" to "1")),
            upnpMappings = listOf(
                mapOf("externalPort" to "3074"), // Xbox Live
                mapOf("externalPort" to "3478"), // PSN
                mapOf("externalPort" to "49152"), // Ephemeral high port
            )
        )
        val report = SecurityAuditEngine.evaluate(data)
        val upnpFinding = report.findings.first { it.checkId == "upnp" }
        assertTrue(upnpFinding.isPassed)
        assertEquals(100, report.score)
    }

    @Test
    fun `upnp with dangerous port 3389 RDP deducts 15 points`() {
        val data = safeCleanData().copy(
            upnpCfg = listOf(mapOf("enable" to "1")),
            upnpMappings = listOf(
                mapOf("externalPort" to "3074"),
                mapOf("externalPort" to "3389"), // Dangerous RDP
            )
        )
        val report = SecurityAuditEngine.evaluate(data)
        assertEquals(85, report.score)
        val upnpFinding = report.findings.first { it.checkId == "upnp" }
        assertFalse(upnpFinding.isPassed)
        assertEquals(Severity.HIGH, upnpFinding.severity)
        assertEquals(15, upnpFinding.penaltyPoints)
    }

    @Test
    fun `disabled DoS protection deducts 10 points and provides safe remediation`() {
        val data = safeCleanData().copy(
            ddos = listOf(mapOf("enable" to "0"))
        )
        val report = SecurityAuditEngine.evaluate(data)
        assertEquals(90, report.score)
        val ddosFinding = report.findings.first { it.checkId == "ddos" }
        assertFalse(ddosFinding.isPassed)
        assertEquals(10, ddosFinding.penaltyPoints)
        val ddosRem = ddosFinding.remediation
        assertNotNull(ddosRem)
        assertTrue(ddosRem!!.isAutomaticSafe)
        assertEquals("1", ddosRem.changes["enable"])
        assertEquals("1", ddosRem.changes["enableSynFilter"])
    }

    @Test
    fun `exposed FTP server deducts 20 points and flags critical finding`() {
        val data = safeCleanData().copy(
            ftp = listOf(mapOf("enable" to "1", "accessFromInternet" to "1", "anonymous" to "1"))
        )
        val report = SecurityAuditEngine.evaluate(data)
        assertEquals(80, report.score)
        assertEquals(1, report.criticalCount)
        val ftpFinding = report.findings.first { it.checkId == "ftp" }
        assertFalse(ftpFinding.isPassed)
        assertEquals(Severity.CRITICAL, ftpFinding.severity)
        assertEquals(20, ftpFinding.penaltyPoints)
        assertTrue(ftpFinding.remediation!!.isAutomaticSafe)
    }

    @Test
    fun `anonymous SMB sharing deducts 10 points`() {
        val data = safeCleanData().copy(
            smb = listOf(mapOf("enable" to "1", "anonymous" to "1"))
        )
        val report = SecurityAuditEngine.evaluate(data)
        assertEquals(90, report.score)
        val smbFinding = report.findings.first { it.checkId == "smb" }
        assertFalse(smbFinding.isPassed)
        assertEquals(10, smbFinding.penaltyPoints)
        assertTrue(smbFinding.remediation!!.isAutomaticSafe)
    }

    @Test
    fun `score clamps to 0 when penalties exceed 100`() {
        val terribleData = SecurityRawData(
            dmz = listOf(mapOf("enable" to "1")), // -25
            wifi = listOf(
                mapOf(
                    "primaryEnable" to "1",
                    "primaryModeEnabled" to "None", // -25
                    "primaryPSK" to "",
                    "WPSEnable" to "1", // -10
                    "guestEnable" to "1",
                    "guestLANAccessEnable" to "1", // -15
                )
            ),
            upnpCfg = listOf(mapOf("enable" to "1")),
            upnpMappings = listOf(mapOf("externalPort" to "445")), // -15
            ddos = listOf(mapOf("enable" to "0")), // -10
            ftp = listOf(mapOf("enable" to "1", "accessFromInternet" to "1")), // -20
            smb = listOf(mapOf("enable" to "1", "anonymous" to "1")), // -10
        )
        val report = SecurityAuditEngine.evaluate(terribleData)
        assertEquals(0, report.score)
        assertEquals(HealthRating.POOR, report.rating)
        assertTrue(report.criticalCount >= 3)
        assertTrue(report.batchPlan.size >= 4)
    }
}
