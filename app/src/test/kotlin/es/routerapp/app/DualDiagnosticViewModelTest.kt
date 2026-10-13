package es.routerapp.app

import es.routerapp.protocol.RouterClient
import es.routerapp.protocol.dualdiag.DualDiagnosticEngine
import es.routerapp.protocol.dualdiag.DualDiagnosticStage
import es.routerapp.protocol.dualdiag.DualDiagnosticState
import es.routerapp.protocol.dualdiag.DualDiagnosticVerdict
import es.routerapp.protocol.dualdiag.DualNetworkProbe
import es.routerapp.protocol.dualdiag.HopLatencyMetric
import es.routerapp.protocol.dualdiag.HopType
import es.routerapp.protocol.dualdiag.RouterDiagnosticClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DualDiagnosticViewModelTest {

    private class MockRouterDiagnosticClient : RouterDiagnosticClient {
        override suspend fun set(oid: String, attrs: Map<String, String>): Boolean = true
        override suspend fun get(oid: String): Map<String, String> = mapOf(
            "diagnosticsState" to "Complete",
            "averageResponseTime" to "14",
            "failureCount" to "0",
            "successCount" to "4",
        )
    }

    private class MockDualNetworkProbe : DualNetworkProbe {
        override suspend fun probeSamples(
            host: String,
            port: Int,
            samples: Int,
            timeoutMs: Int,
        ): HopLatencyMetric = if (host == "1.1.1.1") {
            HopLatencyMetric(
                type = HopType.MOBILE_INTERNET,
                targetHost = host,
                avgMs = 20.0,
                jitterMs = 2.0,
                isSuccess = true,
            )
        } else {
            HopLatencyMetric(
                type = HopType.WIFI_LAN,
                targetHost = host,
                avgMs = 8.0,
                jitterMs = 1.0,
                isSuccess = true,
            )
        }
    }

    @Test
    fun `when client is null or logged out ViewModel sets Error state`() {
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val vm = DualDiagnosticViewModel(
            clientProvider = { null },
            coroutineScope = testScope,
        )

        val state = vm.state.value
        assertTrue(state is DualDiagnosticUiState.Error)
        assertEquals("Router session is not active", (state as DualDiagnosticUiState.Error).message)
    }

    @Test
    fun `when diagnostic runs successfully ViewModel emits Ready state with valid verdict`() {
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val mockClient = RouterClient()
        val field = RouterClient::class.java.getDeclaredField("loggedIn").apply { isAccessible = true }
        field.set(mockClient, true)
        val cryptoField = RouterClient::class.java.getDeclaredField("crypto").apply { isAccessible = true }
        val dummyCrypto = es.routerapp.protocol.GdprCrypto("00".repeat(64), "010001", 1L, "admin", "password")
        cryptoField.set(mockClient, dummyCrypto)

        val engine = DualDiagnosticEngine(
            routerClient = MockRouterDiagnosticClient(),
            networkProbe = MockDualNetworkProbe(),
            pollDelayMs = 0L,
            maxPollAttempts = 2,
        )

        val vm = DualDiagnosticViewModel(
            clientProvider = { mockClient },
            engineFactory = { engine },
            coroutineScope = testScope,
        )

        val state = vm.state.value
        assertTrue(state is DualDiagnosticUiState.Ready)
        val readyState = state as DualDiagnosticUiState.Ready
        assertFalse(readyState.isRunning)
        assertEquals(DualDiagnosticStage.COMPLETED, readyState.state.stage)
        assertEquals(DualDiagnosticVerdict.ALL_OPTIMAL, readyState.verdict)
        assertNotNull(readyState.state.wifiHop)
        assertNotNull(readyState.state.routerWanHop)
    }

    @Test
    fun `retry re-executes diagnostics`() {
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        var clientLogged = false
        val mockClient = RouterClient()
        val cryptoField = RouterClient::class.java.getDeclaredField("crypto").apply { isAccessible = true }
        cryptoField.set(mockClient, es.routerapp.protocol.GdprCrypto("00".repeat(64), "010001", 1L, "admin", "password"))

        val vm = DualDiagnosticViewModel(
            clientProvider = {
                val field = RouterClient::class.java.getDeclaredField("loggedIn").apply { isAccessible = true }
                field.set(mockClient, clientLogged)
                mockClient
            },
            engineFactory = {
                DualDiagnosticEngine(
                    routerClient = MockRouterDiagnosticClient(),
                    networkProbe = MockDualNetworkProbe(),
                    pollDelayMs = 0L,
                    maxPollAttempts = 2,
                )
            },
            coroutineScope = testScope,
        )

        assertTrue(vm.state.value is DualDiagnosticUiState.Error)

        clientLogged = true
        vm.retry()

        assertTrue(vm.state.value is DualDiagnosticUiState.Ready)
    }
}
