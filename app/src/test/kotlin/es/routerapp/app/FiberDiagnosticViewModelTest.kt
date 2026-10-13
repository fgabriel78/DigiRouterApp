package es.routerapp.app

import es.routerapp.protocol.RouterClient
import es.routerapp.protocol.optical.DiagnosticSnapshot
import es.routerapp.protocol.optical.DiagnosticStep
import es.routerapp.protocol.optical.DiagnosticStepId
import es.routerapp.protocol.optical.DiagnosticStepStatus
import es.routerapp.protocol.optical.FiberDiagnosticEngine
import es.routerapp.protocol.optical.NetworkProbe
import es.routerapp.protocol.optical.OpticalHealthStatus
import es.routerapp.protocol.optical.OpticalTelemetry
import es.routerapp.protocol.optical.ProbeResult
import es.routerapp.protocol.optical.RouterTelemetrySource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
                )
            )
            else -> emptyList()
        }

        override suspend fun get(oid: String): Map<String, String> = when (oid) {
            "DEV2_OPTC_GPON_CFG" -> mapOf("rxPower" to "-20.5 dBm", "txPower" to "+2.5 dBm")
            "DEV2_GPON_INTF_OMCI_STATS" -> mapOf("omciStatus" to "O5_Associated")
            else -> emptyMap()
        }
    }

    private class MockNetworkProbe : NetworkProbe {
        override suspend fun probeHost(host: String, port: Int, timeoutMs: Int): ProbeResult =
            ProbeResult(reachable = true, latencyMs = 8L)

        override suspend fun resolveDns(domain: String, timeoutMs: Int): Boolean = true
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
        val mockClient = RouterClient() // Default non-logged-in, but we can override engineFactory
        // We'll mock a client that is logged in by reflection or subclassing, or pass engine directly:
        val field = RouterClient::class.java.getDeclaredField("loggedIn").apply { isAccessible = true }
        field.set(mockClient, true)
        val cryptoField = RouterClient::class.java.getDeclaredField("crypto").apply { isAccessible = true }
        val dummyCrypto = es.routerapp.protocol.GdprCrypto("00".repeat(64), "010001", 1L, "admin", "password")
        cryptoField.set(mockClient, dummyCrypto)

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
        val mockClient = RouterClient()
        val field = RouterClient::class.java.getDeclaredField("loggedIn").apply { isAccessible = true }
        field.set(mockClient, true)
        val cryptoField = RouterClient::class.java.getDeclaredField("crypto").apply { isAccessible = true }
        val dummyCrypto = es.routerapp.protocol.GdprCrypto("00".repeat(64), "010001", 1L, "admin", "password")
        cryptoField.set(mockClient, dummyCrypto)

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
}
