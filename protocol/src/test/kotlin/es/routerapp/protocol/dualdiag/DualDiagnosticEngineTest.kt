package es.routerapp.protocol.dualdiag

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DualDiagnosticEngineTest {

    private class FakeRouterDiagnosticClient(
        var setSuccess: Boolean = true,
        var getResponses: List<Map<String, String>> = listOf(
            mapOf("diagnosticsState" to "Complete", "averageResponseTime" to "12", "failureCount" to "0", "successCount" to "4"),
        ),
    ) : RouterDiagnosticClient {
        private var getIndex = 0

        override suspend fun set(oid: String, attrs: Map<String, String>): Boolean = setSuccess

        override suspend fun get(oid: String): Map<String, String> {
            if (getResponses.isEmpty()) return emptyMap()
            val resp = getResponses[getIndex.coerceAtMost(getResponses.lastIndex)]
            if (getIndex < getResponses.lastIndex) getIndex++
            return resp
        }
    }

    private class FakeDualNetworkProbe(
        private val wifiMetric: HopLatencyMetric = HopLatencyMetric(
            type = HopType.WIFI_LAN,
            targetHost = "192.168.1.1",
            avgMs = 6.0,
            minMs = 5.0,
            maxMs = 7.0,
            jitterMs = 1.0,
            packetLossPercent = 0,
            isSuccess = true,
        ),
        private val internetMetric: HopLatencyMetric = HopLatencyMetric(
            type = HopType.MOBILE_INTERNET,
            targetHost = "1.1.1.1",
            avgMs = 18.0,
            minMs = 15.0,
            maxMs = 21.0,
            jitterMs = 2.0,
            packetLossPercent = 0,
            isSuccess = true,
        ),
    ) : DualNetworkProbe {
        override suspend fun probeSamples(
            host: String,
            port: Int,
            samples: Int,
            timeoutMs: Int,
        ): HopLatencyMetric = if (host == "1.1.1.1") internetMetric else wifiMetric
    }

    @Test
    fun testVerdictWifiBottleneck() {
        val wifiHop = HopLatencyMetric(
            type = HopType.WIFI_LAN,
            targetHost = "192.168.1.1",
            avgMs = 38.0,
            jitterMs = 18.0,
            isSuccess = true,
        )
        val routerHop = HopLatencyMetric(
            type = HopType.ROUTER_WAN,
            targetHost = "1.1.1.1",
            avgMs = 14.0,
            packetLossPercent = 0,
            isSuccess = true,
        )

        val verdict = DualDiagnosticVerdict.evaluate(wifiHop, routerHop)
        assertEquals(DualDiagnosticVerdict.WIFI_BOTTLENECK, verdict)
    }

    @Test
    fun testVerdictFiberBottleneckHighLatency() {
        val wifiHop = HopLatencyMetric(
            type = HopType.WIFI_LAN,
            targetHost = "192.168.1.1",
            avgMs = 7.0,
            jitterMs = 2.0,
            isSuccess = true,
        )
        val routerHop = HopLatencyMetric(
            type = HopType.ROUTER_WAN,
            targetHost = "1.1.1.1",
            avgMs = 62.0,
            packetLossPercent = 0,
            isSuccess = true,
        )

        val verdict = DualDiagnosticVerdict.evaluate(wifiHop, routerHop)
        assertEquals(DualDiagnosticVerdict.FIBER_BOTTLENECK, verdict)
    }

    @Test
    fun testVerdictFiberBottleneckPacketLoss() {
        val wifiHop = HopLatencyMetric(
            type = HopType.WIFI_LAN,
            targetHost = "192.168.1.1",
            avgMs = 8.0,
            jitterMs = 1.0,
            isSuccess = true,
        )
        val routerHop = HopLatencyMetric(
            type = HopType.ROUTER_WAN,
            targetHost = "1.1.1.1",
            avgMs = 15.0,
            packetLossPercent = 25,
            isSuccess = true,
        )

        val verdict = DualDiagnosticVerdict.evaluate(wifiHop, routerHop)
        assertEquals(DualDiagnosticVerdict.FIBER_BOTTLENECK, verdict)
    }

    @Test
    fun testVerdictAllOptimal() {
        val wifiHop = HopLatencyMetric(
            type = HopType.WIFI_LAN,
            targetHost = "192.168.1.1",
            avgMs = 8.0,
            jitterMs = 2.0,
            isSuccess = true,
        )
        val routerHop = HopLatencyMetric(
            type = HopType.ROUTER_WAN,
            targetHost = "1.1.1.1",
            avgMs = 15.0,
            packetLossPercent = 0,
            isSuccess = true,
        )

        val verdict = DualDiagnosticVerdict.evaluate(wifiHop, routerHop)
        assertEquals(DualDiagnosticVerdict.ALL_OPTIMAL, verdict)
    }

    @Test
    fun testVerdictBothDegraded() {
        val wifiHop = HopLatencyMetric(
            type = HopType.WIFI_LAN,
            targetHost = "192.168.1.1",
            avgMs = 45.0,
            jitterMs = 20.0,
            isSuccess = true,
        )
        val routerHop = HopLatencyMetric(
            type = HopType.ROUTER_WAN,
            targetHost = "1.1.1.1",
            avgMs = 70.0,
            packetLossPercent = 10,
            isSuccess = true,
        )

        val verdict = DualDiagnosticVerdict.evaluate(wifiHop, routerHop)
        assertEquals(DualDiagnosticVerdict.BOTH_DEGRADED, verdict)
    }

    @Test
    fun testVerdictInconclusiveWhenFailed() {
        val wifiHop = HopLatencyMetric(
            type = HopType.WIFI_LAN,
            targetHost = "192.168.1.1",
            isSuccess = false,
        )
        val routerHop = HopLatencyMetric(
            type = HopType.ROUTER_WAN,
            targetHost = "1.1.1.1",
            isSuccess = true,
            avgMs = 12.0,
        )

        assertEquals(DualDiagnosticVerdict.INCONCLUSIVE, DualDiagnosticVerdict.evaluate(wifiHop, routerHop))
        assertEquals(DualDiagnosticVerdict.INCONCLUSIVE, DualDiagnosticVerdict.evaluate(null, routerHop))
    }

    @Test
    fun testDualDiagnosticEngineWithRouterHardwarePing() = runBlocking {
        val fakeClient = FakeRouterDiagnosticClient(
            setSuccess = true,
            getResponses = listOf(
                mapOf("diagnosticsState" to "Requested"),
                mapOf(
                    "diagnosticsState" to "Complete",
                    "averageResponseTime" to "11",
                    "minimumResponseTime" to "9",
                    "maximumResponseTime" to "14",
                    "failureCount" to "0",
                    "successCount" to "4",
                ),
            ),
        )
        val fakeProbe = FakeDualNetworkProbe(
            wifiMetric = HopLatencyMetric(
                type = HopType.WIFI_LAN,
                targetHost = "192.168.1.1",
                avgMs = 6.0,
                jitterMs = 1.0,
                isSuccess = true,
            ),
        )

        val engine = DualDiagnosticEngine(
            routerClient = fakeClient,
            networkProbe = fakeProbe,
            pollDelayMs = 0L,
            maxPollAttempts = 3,
        )

        val states = engine.runDiagnostics().toList()
        assertTrue(states.size >= 4)

        val finalState = states.last()
        assertFalse(finalState.isRunning)
        assertEquals(DualDiagnosticStage.COMPLETED, finalState.stage)
        assertEquals(DualDiagnosticVerdict.ALL_OPTIMAL, finalState.verdict)
        assertFalse(finalState.isRouterPingEstimated)
        assertNotNull(finalState.wifiHop)
        assertNotNull(finalState.routerWanHop)
        assertEquals(11.0, finalState.routerWanHop?.avgMs)
        assertEquals(6.0, finalState.wifiHop?.avgMs)
    }

    @Test
    fun testDualDiagnosticEngineFallbackWhenRouterPingFails() = runBlocking {
        val fakeClient = FakeRouterDiagnosticClient(
            setSuccess = false,
            getResponses = emptyList(),
        )
        val fakeProbe = FakeDualNetworkProbe(
            wifiMetric = HopLatencyMetric(
                type = HopType.WIFI_LAN,
                targetHost = "192.168.1.1",
                avgMs = 7.0,
                minMs = 6.0,
                maxMs = 8.0,
                jitterMs = 1.0,
                isSuccess = true,
            ),
            internetMetric = HopLatencyMetric(
                type = HopType.MOBILE_INTERNET,
                targetHost = "1.1.1.1",
                avgMs = 22.0,
                minMs = 20.0,
                maxMs = 25.0,
                jitterMs = 2.0,
                isSuccess = true,
            ),
        )

        val engine = DualDiagnosticEngine(
            routerClient = fakeClient,
            networkProbe = fakeProbe,
            pollDelayMs = 0L,
            maxPollAttempts = 2,
        )

        val states = engine.runDiagnostics().toList()
        val finalState = states.last()

        assertFalse(finalState.isRunning)
        assertEquals(DualDiagnosticStage.COMPLETED, finalState.stage)
        assertTrue(finalState.isRouterPingEstimated)
        assertNotNull(finalState.routerWanHop)
        // Estimated router WAN avg: 22.0 - 7.0 = 15.0 ms
        assertEquals(15.0, finalState.routerWanHop?.avgMs)
        assertEquals(DualDiagnosticVerdict.ALL_OPTIMAL, finalState.verdict)
    }
}
