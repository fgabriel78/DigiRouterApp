package es.routerapp.app

import es.routerapp.protocol.security.HealthRating
import es.routerapp.protocol.security.RemediationAction
import es.routerapp.protocol.security.SecurityFinding
import es.routerapp.protocol.security.SecurityReport
import es.routerapp.protocol.security.Severity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityHealthViewModelTest {

    private fun sampleReport(): SecurityReport {
        val findings = listOf(
            SecurityFinding(
                id = "f1",
                checkId = "c1",
                title = "DMZ Active",
                description = "DMZ exposed",
                severity = Severity.CRITICAL,
                penaltyPoints = 25,
                isPassed = false,
                remediation = RemediationAction(
                    id = "r1",
                    title = "Disable DMZ",
                    oid = "DEV2_DMZ_HOST_CFG",
                    changes = mapOf("enable" to "0"),
                    isAutomaticSafe = true,
                ),
            ),
            SecurityFinding(
                id = "f2",
                checkId = "c2",
                title = "Wi-Fi Encryption",
                description = "WPA3",
                severity = Severity.CRITICAL,
                penaltyPoints = 0,
                isPassed = true,
            ),
            SecurityFinding(
                id = "f3",
                checkId = "c3",
                title = "WPS Active",
                description = "WPS enabled",
                severity = Severity.MEDIUM,
                penaltyPoints = 10,
                isPassed = false,
                remediation = RemediationAction(
                    id = "r3",
                    title = "Disable WPS",
                    oid = "DEV2_ADT_WIFI_COMMON",
                    changes = mapOf("WPSEnable" to "0"),
                    isAutomaticSafe = true,
                ),
            )
        )
        return SecurityReport(
            score = 65,
            rating = HealthRating.POOR,
            findings = findings,
            criticalCount = 1,
            warningCount = 1,
            passedCount = 1,
            batchPlan = listOf(findings[0].remediation!!, findings[2].remediation!!),
        )
    }

    @Test
    fun `initial scan sets Error when client is null`() {
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val vm = SecurityHealthViewModel(clientProvider = { null }, coroutineScope = testScope)
        vm.scan()
        val state = vm.state.value
        assertTrue(state is SecurityUiState.Error)
    }

    @Test
    fun `visibleFindings filters properly across all filters`() {
        val report = sampleReport()
        val successState = SecurityUiState.Success(report = report, filter = SecurityFilter.ALL)

        assertEquals(3, successState.visibleFindings.size)

        val atRiskState = successState.copy(filter = SecurityFilter.AT_RISK)
        assertEquals(2, atRiskState.visibleFindings.size)
        assertTrue(atRiskState.visibleFindings.all { !it.isPassed })

        val passedState = successState.copy(filter = SecurityFilter.PASSED)
        assertEquals(1, passedState.visibleFindings.size)
        assertTrue(passedState.visibleFindings.all { it.isPassed })
    }

    @Test
    fun `dismissBanner updates bannerDismissed flag`() {
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val vm = SecurityHealthViewModel(clientProvider = { null }, coroutineScope = testScope)
        vm.dismissBanner()
        assertFalse(vm.state.value is SecurityUiState.Success)
    }
}
