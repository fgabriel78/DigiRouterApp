package es.routerapp.protocol.optical

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FiberDiagnosticEngineTest {

    private class FakeTelemetrySource(
        private val oids: Map<String, Any>,
        val ops: MutableList<Pair<String, String>> = mutableListOf(),
    ) : RouterTelemetrySource {
        @Suppress("UNCHECKED_CAST")
        override suspend fun getList(oid: String): List<Map<String, String>> =
            (oids[oid] as? List<Map<String, String>>).orEmpty()

        @Suppress("UNCHECKED_CAST")
        override suspend fun get(oid: String): Map<String, String> =
            (oids[oid] as? Map<String, String>).orEmpty()

        override suspend fun get(oid: String, stack: String): Map<String, String> =
            get(oid)

        override suspend fun operate(oid: String, attrs: Map<String, String>, stack: String): Boolean {
            ops.add(oid to stack)
            return true
        }
    }

    private class FakeNetworkProbe(
        var hostReachable: Boolean = true,
        var hostLatency: Long = 12L,
        var dnsResolves: Boolean = true,
    ) : NetworkProbe {
        override suspend fun probeHost(host: String, port: Int, timeoutMs: Int): ProbeResult =
            ProbeResult(reachable = hostReachable, latencyMs = if (hostReachable) hostLatency else null)

        override suspend fun resolveDns(domain: String, timeoutMs: Int): Boolean = dnsResolves
    }

    private fun standardHealthyOids(): Map<String, Any> = mapOf(
        "DEV2_OPTC_GPON_CFG" to mapOf(
            "rxPower" to "-21.40 dBm",
            "txPower" to "+2.30 dBm",
        ),
        "DEV2_GPON_INTF_OMCI_STATS" to mapOf(
            "omciStatus" to "O5_Associated",
            "status" to "Up",
        ),
        "DEV2_ADT_WAN" to listOf(
            mapOf(
                "name" to "ipoe_eth_0",
                "connStatusV4" to "Connected",
                "externalIPAddress" to "84.120.45.10",
                "defaultGateway" to "84.120.45.1",
                "connType" to "PPPoE",
            )
        ),
    )

    @Test
    fun allStepsSucceed() = runBlocking {
        val engine = FiberDiagnosticEngine(
            telemetrySource = FakeTelemetrySource(standardHealthyOids()),
            networkProbe = FakeNetworkProbe(hostReachable = true, hostLatency = 14L, dnsResolves = true),
        )

        val snapshots = engine.runDiagnostics().toList()
        val finalState = snapshots.last()

        assertFalse(finalState.isRunning)
        assertTrue(finalState.isSuccess)
        assertFalse(finalState.hasWarnings)
        assertEquals("All connectivity layers operational", finalState.summaryMessage)
        assertNull(finalState.failureStep)
        assertNotNull(finalState.opticalTelemetry)
        assertEquals(OpticalHealthStatus.OPTIMAL, finalState.opticalTelemetry?.status)
        assertEquals(5, finalState.steps.size)
        assertTrue(finalState.steps.all { it.status == DiagnosticStepStatus.SUCCESS })
    }

    @Test
    fun opticalCriticalAttenuationHaltsPipeline() = runBlocking {
        val oids = standardHealthyOids().toMutableMap().apply {
            put("DEV2_OPTC_GPON_CFG", mapOf("rxPower" to "-29.50 dBm"))
        }
        val engine = FiberDiagnosticEngine(
            telemetrySource = FakeTelemetrySource(oids),
            networkProbe = FakeNetworkProbe(),
        )

        val snapshots = engine.runDiagnostics().toList()
        val finalState = snapshots.last()

        assertFalse(finalState.isRunning)
        assertFalse(finalState.isSuccess)
        assertEquals(DiagnosticStepId.OPTICAL_LINK, finalState.failureStep)
        assertEquals(DiagnosticStepStatus.FAILED, finalState.steps[0].status)
        assertEquals(DiagnosticStepStatus.SKIPPED, finalState.steps[1].status)
        assertEquals(DiagnosticStepStatus.SKIPPED, finalState.steps[2].status)
        assertEquals(DiagnosticStepStatus.SKIPPED, finalState.steps[3].status)
        assertEquals(DiagnosticStepStatus.SKIPPED, finalState.steps[4].status)
    }

    @Test
    fun opticalMarginalAllowsSubsequentStepsToComplete() = runBlocking {
        val oids = standardHealthyOids().toMutableMap().apply {
            put("DEV2_OPTC_GPON_CFG", mapOf("rxPower" to "-26.40 dBm"))
        }
        val engine = FiberDiagnosticEngine(
            telemetrySource = FakeTelemetrySource(oids),
            networkProbe = FakeNetworkProbe(),
        )

        val finalState = engine.runDiagnostics().toList().last()

        assertFalse(finalState.isRunning)
        assertTrue(finalState.isSuccess)
        assertEquals(DiagnosticStepStatus.WARNING, finalState.steps[0].status)
        assertEquals(DiagnosticStepStatus.SUCCESS, finalState.steps[1].status)
        assertEquals(DiagnosticStepStatus.SUCCESS, finalState.steps[2].status)
    }

    @Test
    fun externalOntBypassesOpticalAndOmciChecks() = runBlocking {
        val oids = mapOf(
            "DEV2_OPTC_GPON_CFG" to emptyMap<String, String>(),
            "DEV2_GPON_INTF_STATS" to emptyMap<String, String>(),
            "DEV2_ADT_WAN" to listOf(
                mapOf(
                    "name" to "IP_Routed_ETH",
                    "type" to "Ethernet",
                    "connStatusV4" to "Connected",
                    "ip" to "192.168.100.5",
                    "gateway" to "192.168.100.1",
                )
            ),
        )
        val engine = FiberDiagnosticEngine(
            telemetrySource = FakeTelemetrySource(oids),
            networkProbe = FakeNetworkProbe(),
        )

        val finalState = engine.runDiagnostics().toList().last()

        assertFalse(finalState.isRunning)
        assertTrue(finalState.isSuccess)
        assertEquals(OpticalHealthStatus.EXTERNAL_ONT, finalState.opticalTelemetry?.status)
        assertEquals(DiagnosticStepStatus.SUCCESS, finalState.steps[0].status)
        assertEquals(DiagnosticStepStatus.SUCCESS, finalState.steps[1].status)
        assertEquals("ONT External", finalState.steps[1].metricSummary)
        assertEquals(DiagnosticStepStatus.SUCCESS, finalState.steps[2].status)
    }

    @Test
    fun omciRegistrationFailureHaltsPipeline() = runBlocking {
        val oids = standardHealthyOids().toMutableMap().apply {
            put("DEV2_GPON_INTF_OMCI_STATS", mapOf("omciStatus" to "O2", "status" to "Down"))
            put("DEV2_GPON_INTF_STATS", mapOf("status" to "Down"))
        }
        val engine = FiberDiagnosticEngine(
            telemetrySource = FakeTelemetrySource(oids),
            networkProbe = FakeNetworkProbe(),
        )

        val finalState = engine.runDiagnostics().toList().last()

        assertFalse(finalState.isSuccess)
        assertEquals(DiagnosticStepId.OMCI_REGISTRATION, finalState.failureStep)
        assertEquals(DiagnosticStepStatus.SUCCESS, finalState.steps[0].status)
        assertEquals(DiagnosticStepStatus.FAILED, finalState.steps[1].status)
        assertEquals(DiagnosticStepStatus.SKIPPED, finalState.steps[2].status)
        assertEquals(DiagnosticStepStatus.SKIPPED, finalState.steps[3].status)
        assertEquals(DiagnosticStepStatus.SKIPPED, finalState.steps[4].status)
    }

    @Test
    fun wanDisconnectedHaltsPipeline() = runBlocking {
        val oids = standardHealthyOids().toMutableMap().apply {
            put(
                "DEV2_ADT_WAN",
                listOf(
                    mapOf(
                        "name" to "ipoe_eth_0",
                        "connStatusV4" to "Disconnected",
                        "externalIPAddress" to "",
                    )
                )
            )
        }
        val engine = FiberDiagnosticEngine(
            telemetrySource = FakeTelemetrySource(oids),
            networkProbe = FakeNetworkProbe(),
        )

        val finalState = engine.runDiagnostics().toList().last()

        assertFalse(finalState.isSuccess)
        assertEquals(DiagnosticStepId.WAN_SESSION, finalState.failureStep)
        assertEquals(DiagnosticStepStatus.SUCCESS, finalState.steps[0].status)
        assertEquals(DiagnosticStepStatus.SUCCESS, finalState.steps[1].status)
        assertEquals(DiagnosticStepStatus.FAILED, finalState.steps[2].status)
        assertEquals(DiagnosticStepStatus.SKIPPED, finalState.steps[3].status)
        assertEquals(DiagnosticStepStatus.SKIPPED, finalState.steps[4].status)
    }

    @Test
    fun gatewayUnreachableHaltsPipeline() = runBlocking {
        val engine = FiberDiagnosticEngine(
            telemetrySource = FakeTelemetrySource(standardHealthyOids()),
            networkProbe = FakeNetworkProbe(hostReachable = false),
        )

        val finalState = engine.runDiagnostics().toList().last()

        assertFalse(finalState.isSuccess)
        assertEquals(DiagnosticStepId.GATEWAY_PING, finalState.failureStep)
        assertEquals(DiagnosticStepStatus.SUCCESS, finalState.steps[2].status)
        assertEquals(DiagnosticStepStatus.FAILED, finalState.steps[3].status)
        assertEquals(DiagnosticStepStatus.SKIPPED, finalState.steps[4].status)
    }

    @Test
    fun dnsFailureHaltsPipeline() = runBlocking {
        val engine = FiberDiagnosticEngine(
            telemetrySource = FakeTelemetrySource(standardHealthyOids()),
            networkProbe = FakeNetworkProbe(hostReachable = true, dnsResolves = false),
        )

        val finalState = engine.runDiagnostics().toList().last()

        assertFalse(finalState.isSuccess)
        assertEquals(DiagnosticStepId.DNS_INTERNET, finalState.failureStep)
        assertEquals(DiagnosticStepStatus.SUCCESS, finalState.steps[3].status)
        assertEquals(DiagnosticStepStatus.FAILED, finalState.steps[4].status)
    }

    @Test
    fun unexposedOpticalTelemetryWithActiveWanDegradesToWarningAndSucceeds() = runBlocking {
        val oids = mapOf(
            "DEV2_OPTC_GPON_CFG" to emptyMap<String, String>(),
            "DEV2_GPON_INTF_STATS" to emptyMap<String, String>(),
            "DEV2_ADT_WAN" to listOf(
                mapOf(
                    "name" to "pppoe_0_1_d",
                    "connStatusV4" to "Connected",
                    "connIPv4Address" to "84.120.45.10",
                    "defaultGateway" to "84.120.45.1",
                    "connType" to "PPPoE",
                )
            ),
        )
        val engine = FiberDiagnosticEngine(
            telemetrySource = FakeTelemetrySource(oids),
            networkProbe = FakeNetworkProbe(hostReachable = true, hostLatency = 11L, dnsResolves = true),
        )

        val finalState = engine.runDiagnostics().toList().last()

        assertTrue(finalState.isSuccess)
        assertTrue(finalState.hasWarnings)
        assertEquals("Operational with advisory: Fiber link active (optical dBm power unexposed by firmware)", finalState.summaryMessage)
        assertFalse(finalState.isRunning)
        assertNull(finalState.failureStep)
        // Step 1 degrades gracefully to WARNING instead of failing
        assertEquals(DiagnosticStepStatus.WARNING, finalState.steps[0].status)
        assertEquals("Active", finalState.steps[0].metricSummary)
        // Subsequent steps complete with SUCCESS
        assertEquals(DiagnosticStepStatus.SUCCESS, finalState.steps[1].status)
        assertEquals(DiagnosticStepStatus.SUCCESS, finalState.steps[2].status)
        assertEquals(DiagnosticStepStatus.SUCCESS, finalState.steps[3].status)
        assertEquals(DiagnosticStepStatus.SUCCESS, finalState.steps[4].status)
    }

    @Test
    fun unexposedOpticalTelemetryWithDisconnectedWanHaltsPipeline() = runBlocking {
        val oids = mapOf(
            "DEV2_OPTC_GPON_CFG" to emptyMap<String, String>(),
            "DEV2_GPON_INTF_STATS" to emptyMap<String, String>(),
            "DEV2_ADT_WAN" to listOf(
                mapOf(
                    "name" to "pppoe_0_1_d",
                    "connStatusV4" to "Disconnected",
                )
            ),
        )
        val engine = FiberDiagnosticEngine(
            telemetrySource = FakeTelemetrySource(oids),
            networkProbe = FakeNetworkProbe(),
        )

        val finalState = engine.runDiagnostics().toList().last()

        assertFalse(finalState.isSuccess)
        assertEquals(DiagnosticStepId.OPTICAL_LINK, finalState.failureStep)
        assertEquals(DiagnosticStepStatus.FAILED, finalState.steps[0].status)
        assertEquals(DiagnosticStepStatus.SKIPPED, finalState.steps[1].status)
    }

    @Test
    fun step2PopulatesGponAuthInfo() = runBlocking {
        val oids = standardHealthyOids().toMutableMap().apply {
            put(
                "DEV2_GPON_AUTH",
                mapOf(
                    "gponSn" to "HWTC12345678",
                    "snType" to "0",
                    "gponPwd" to "secretPloam",
                    "pwdType" to "0",
                    "oltMode" to "1",
                ),
            )
        }
        val engine = FiberDiagnosticEngine(
            telemetrySource = FakeTelemetrySource(oids),
            networkProbe = FakeNetworkProbe(),
        )

        val finalState = engine.runDiagnostics().toList().last()
        assertNotNull(finalState.gponAuth)
        assertEquals("HWTC12345678", finalState.gponAuth?.snAscii)
        assertEquals("4857544312345678", finalState.gponAuth?.snHex)
        assertEquals("secretPloam", finalState.gponAuth?.gponPwd)
        assertEquals(OltMode.MODE_H, finalState.gponAuth?.oltMode)
    }

    @Test
    fun restartOmciSuccess() = runBlocking {
        val oids = mapOf(
            "DEV2_GPON_INTF_OMCI_STATS" to mapOf("omciStatus" to "O5_Associated"),
        )
        val fakeSource = FakeTelemetrySource(oids)
        val engine = FiberDiagnosticEngine(telemetrySource = fakeSource, networkProbe = FakeNetworkProbe())

        val states = engine.restartOmci(pollIntervalMs = 1L).toList()
        assertEquals(OmciRestartStage.RESTARTING, states[0].stage)
        assertEquals(OmciRestartStage.AWAITING_O5, states[1].stage)
        assertEquals(OmciRestartStage.SUCCESS, states[2].stage)
        assertTrue(fakeSource.ops.any { it.first == FiberDiagnosticEngine.ACT_OMCI_RESTART })
    }

    @Test
    fun renewWanSessionPppoeSuccess() = runBlocking {
        val oids = mapOf(
            "DEV2_ADT_WAN" to listOf(
                mapOf(
                    "name" to "pppoe_0_1",
                    "connStatusV4" to "Connected",
                    "connType" to "PPPoE",
                    "externalIPAddress" to "84.120.45.10",
                    "stack" to "1,0,0,0,0,0",
                ),
            ),
        )
        val fakeSource = FakeTelemetrySource(oids)
        val engine = FiberDiagnosticEngine(telemetrySource = fakeSource, networkProbe = FakeNetworkProbe())

        val states = engine.renewWanSession(delayBetweenMs = 1L, pollIntervalMs = 1L).toList()
        assertEquals(WanRenewStage.DISCONNECTING, states[0].stage)
        assertEquals(WanRenewStage.RECONNECTING, states[1].stage)
        assertEquals(WanRenewStage.VERIFYING, states[2].stage)
        assertEquals(WanRenewStage.SUCCESS, states[3].stage)
        assertEquals("84.120.45.10", states[3].newIp)
        assertTrue(fakeSource.ops.any { it.first == FiberDiagnosticEngine.ACT_PPP_DISCONN && it.second == "1,0,0,0,0,0" })
        assertTrue(fakeSource.ops.any { it.first == FiberDiagnosticEngine.ACT_PPP_CONN && it.second == "1,0,0,0,0,0" })
    }

    @Test
    fun renewWanSessionDhcpSuccess() = runBlocking {
        val oids = mapOf(
            "DEV2_ADT_WAN" to listOf(
                mapOf(
                    "name" to "ipoe_eth_0",
                    "connStatusV4" to "Connected",
                    "connType" to "DHCP",
                    "externalIPAddress" to "88.26.12.34",
                    "stack" to "2,0,0,0,0,0",
                ),
            ),
        )
        val fakeSource = FakeTelemetrySource(oids)
        val engine = FiberDiagnosticEngine(telemetrySource = fakeSource, networkProbe = FakeNetworkProbe())

        val states = engine.renewWanSession(delayBetweenMs = 1L, pollIntervalMs = 1L).toList()
        assertEquals(WanRenewStage.SUCCESS, states.last().stage)
        assertEquals("88.26.12.34", states.last().newIp)
        assertTrue(fakeSource.ops.any { it.first == FiberDiagnosticEngine.ACT_DHCP_RELEASE && it.second == "2,0,0,0,0,0" })
        assertTrue(fakeSource.ops.any { it.first == FiberDiagnosticEngine.ACT_DHCP_RENEW && it.second == "2,0,0,0,0,0" })
    }
}
