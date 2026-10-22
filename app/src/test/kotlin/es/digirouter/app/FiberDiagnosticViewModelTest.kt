package es.digirouter.app

import es.routerapp.protocol.RouterClient
import es.routerapp.protocol.optical.DiagnosticStepStatus
import es.routerapp.protocol.optical.FiberDiagnosticEngine
import es.routerapp.protocol.optical.NetworkProbe
import es.routerapp.protocol.optical.OmciRestartStage
import es.routerapp.protocol.optical.OpticalHealthStatus
import es.routerapp.protocol.optical.ProbeResult
import es.routerapp.protocol.optical.RouterTelemetrySource
import es.routerapp.protocol.optical.WanRenewStage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FiberDiagnosticViewModelTest {

    private class MockRouterTelemetrySource : RouterTelemetrySource {
        override suspend fun getList(oid: String): List<Map<String, String>> = when (oid) {
            "DEV2_ADT_WAN" -> listOf(
                mapOf(
                    "name" to "ipoe_eth_0",
                    "connStatusV4" to "Connected",
                    "externalIPAddress" to "84.120.45.10",
                    "defaultGateway" to "84.120.45.1",
                    "connType" to "PPPoE",
                    "stack" to "1,0,0,0,0,0",
                )
            )
            else -> emptyList()
        }

        override suspend fun get(oid: String): Map<String, String> = when (oid) {
            "DEV2_OPTC_GPON_CFG" -> mapOf("rxPower" to "-20.5 dBm", "txPower" to "+2.5 dBm")
            "DEV2_GPON_INTF_OMCI_STATS" -> mapOf("omciStatus" to "O5_Associated")
            "DEV2_GPON_AUTH" -> mapOf("gponSn" to "HWTC12345678", "oltMode" to "1")
            else -> emptyMap()
        }

        override suspend fun operate(oid: String, attrs: Map<String, String>, stack: String): Boolean = true
    }

    private class MockNetworkProbe : NetworkProbe {
        override suspend fun probeHost(host: String, port: Int, timeoutMs: Int): ProbeResult =
            ProbeResult(reachable = true, latencyMs = 8L)

        override suspend fun resolveDns(domain: String, timeoutMs: Int): Boolean = true
    }

    private fun createMockClient(): RouterClient {
        val mockClient = RouterClient()
        val field = RouterClient::class.java.getDeclaredField("loggedIn").apply { isAccessible = true }
        field.set(mockClient, true)
        val cryptoField = RouterClient::class.java.getDeclaredField("crypto").apply { isAccessible = true }
        val dummyCrypto = es.routerapp.protocol.GdprCrypto("00".repeat(64), "010001", 1L, "admin", "password")
        cryptoField.set(mockClient, dummyCrypto)
        return mockClient
    }

    @Test
    fun `when client is null or logged out ViewModel sets Error state`() {
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val vm = FiberDiagnosticViewModel(
            clientProvider = { null },
            coroutineScope = testScope,
        )

        val state = vm.state.value
        assertTrue(state is FiberDiagnosticUiState.Error)
        assertEquals("Router session is not active", (state as FiberDiagnosticUiState.Error).message)
    }

    @Test
    fun `when diagnostic runs successfully ViewModel emits Ready state with valid snapshot`() {
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val mockClient = createMockClient()

        val engine = FiberDiagnosticEngine(
            telemetrySource = MockRouterTelemetrySource(),
            networkProbe = MockNetworkProbe(),
        )

        val vm = FiberDiagnosticViewModel(
            clientProvider = { mockClient },
            engineFactory = { engine },
            coroutineScope = testScope,
        )

        val state = vm.state.value
        assertTrue(state is FiberDiagnosticUiState.Ready)
        val readyState = state as FiberDiagnosticUiState.Ready
        assertTrue(readyState.isSuccess)
        assertFalse(readyState.isRunning)
        assertEquals(OpticalHealthStatus.OPTIMAL, readyState.snapshot.opticalTelemetry?.status)
        assertEquals(5, readyState.snapshot.steps.size)
        assertTrue(readyState.snapshot.steps.all { it.status == DiagnosticStepStatus.SUCCESS })
    }

    @Test
    fun `retry re-executes diagnostics`() {
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val mockClient = createMockClient()

        var runs = 0
        val vm = FiberDiagnosticViewModel(
            clientProvider = { mockClient },
            engineFactory = {
                runs++
                FiberDiagnosticEngine(
                    telemetrySource = MockRouterTelemetrySource(),
                    networkProbe = MockNetworkProbe(),
                )
            },
            coroutineScope = testScope,
        )

        assertEquals(1, runs)
        vm.retry()
        assertEquals(2, runs)
        assertTrue(vm.state.value is FiberDiagnosticUiState.Ready)
    }

    @Test
    fun `restartOmci completes and reports success state`() {
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val mockClient = createMockClient()
        val engine = FiberDiagnosticEngine(
            telemetrySource = MockRouterTelemetrySource(),
            networkProbe = MockNetworkProbe(),
        )

        val vm = FiberDiagnosticViewModel(
            clientProvider = { mockClient },
            engineFactory = { engine },
            coroutineScope = testScope,
        )

        var completed = false
        vm.restartOmci(pollIntervalMs = 0L) { success -> completed = success }
        assertTrue(completed)
        assertEquals(OmciRestartStage.SUCCESS, vm.omciRestartState.value.stage)
    }

    @Test
    fun `renewWanSession completes and reports new IP`() {
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val mockClient = createMockClient()
        val engine = FiberDiagnosticEngine(
            telemetrySource = MockRouterTelemetrySource(),
            networkProbe = MockNetworkProbe(),
        )

        val vm = FiberDiagnosticViewModel(
            clientProvider = { mockClient },
            engineFactory = { engine },
            coroutineScope = testScope,
        )

        var completed = false
        var assignedIp: String? = null
        vm.renewWanSession(delayBetweenMs = 0L, pollIntervalMs = 0L) { success, ip ->
            completed = success
            assignedIp = ip
        }
        assertTrue(completed)
        assertEquals(WanRenewStage.SUCCESS, vm.wanRenewState.value.stage)
        assertEquals("84.120.45.10", assignedIp)
    }
}
