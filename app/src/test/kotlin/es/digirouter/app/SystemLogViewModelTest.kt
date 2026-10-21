package es.digirouter.app

import es.routerapp.protocol.RouterClient
import es.routerapp.protocol.log.LogSeverity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SystemLogViewModelTest {

    private val testScope = CoroutineScope(Dispatchers.Unconfined)

    private fun createMockClient(
        logBody: String = "0\r\n2026-10-20 22:50:00 [3] OMCI: Loss of signal\r\n2026-10-20 22:50:05 [6] DHCP: ACK sent\r\n",
    ): RouterClient {
        val interceptor = Interceptor { chain ->
            val req = chain.request()
            val url = req.url.toString()
            val responseBody = when {
                url.contains("/cgi/log?clr") -> "0\r\n"
                url.contains("/cgi/log") -> logBody
                else -> "{}"
            }
            Response.Builder()
                .request(req)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(responseBody.toResponseBody("text/plain".toMediaType()))
                .build()
        }
        val okHttp = OkHttpClient.Builder().addInterceptor(interceptor).build()
        return RouterClient("http://192.168.1.1", okHttp)
    }

    @Test
    fun `initial state loads logs and populates available modules`() {
        val client = createMockClient()
        val vm = SystemLogViewModel(
            clientProvider = { client },
            coroutineScope = testScope,
            ioDispatcher = Dispatchers.Unconfined,
            pollIntervalMs = 10000L,
        )

        val state = vm.state.value
        assertEquals(2, state.logs.size)
        assertEquals(2, state.filteredLogs.size)
        assertEquals(listOf("DHCP", "OMCI"), state.availableModules)
        assertTrue(state.isLive)
    }

    @Test
    fun `pause and resume toggles live streaming state`() {
        val client = createMockClient()
        val vm = SystemLogViewModel(
            clientProvider = { client },
            coroutineScope = testScope,
            ioDispatcher = Dispatchers.Unconfined,
        )

        assertTrue(vm.state.value.isLive)
        vm.pausePolling()
        assertFalse(vm.state.value.isLive)
        vm.startPolling()
        assertTrue(vm.state.value.isLive)
        vm.toggleLive()
        assertFalse(vm.state.value.isLive)
    }

    @Test
    fun `client-side filtering by severity category works correctly`() {
        val client = createMockClient()
        val vm = SystemLogViewModel(
            clientProvider = { client },
            coroutineScope = testScope,
            ioDispatcher = Dispatchers.Unconfined,
        )

        // Filter only ERRORS (code in 0..3)
        vm.setSeverityFilter(SeverityFilterCategory.ERRORS)
        assertEquals(1, vm.state.value.filteredLogs.size)
        assertEquals("OMCI", vm.state.value.filteredLogs[0].module)

        // Filter only INFO (code in 5..6)
        vm.setSeverityFilter(SeverityFilterCategory.INFO)
        assertEquals(1, vm.state.value.filteredLogs.size)
        assertEquals("DHCP", vm.state.value.filteredLogs[0].module)

        // Filter ALL
        vm.setSeverityFilter(SeverityFilterCategory.ALL)
        assertEquals(2, vm.state.value.filteredLogs.size)
    }

    @Test
    fun `client-side filtering by module and search query`() {
        val client = createMockClient()
        val vm = SystemLogViewModel(
            clientProvider = { client },
            coroutineScope = testScope,
            ioDispatcher = Dispatchers.Unconfined,
        )

        vm.setSelectedModule("DHCP")
        assertEquals(1, vm.state.value.filteredLogs.size)
        assertEquals("DHCP", vm.state.value.filteredLogs[0].module)

        vm.setSelectedModule(null)
        vm.setSearchQuery("signal")
        assertEquals(1, vm.state.value.filteredLogs.size)
        assertEquals("OMCI", vm.state.value.filteredLogs[0].module)

        vm.setSearchQuery("nonexistent")
        assertTrue(vm.state.value.filteredLogs.isEmpty())
    }

    @Test
    fun `clear logs resets in-memory logs`() {
        val client = createMockClient()
        val vm = SystemLogViewModel(
            clientProvider = { client },
            coroutineScope = testScope,
            ioDispatcher = Dispatchers.Unconfined,
        )

        assertEquals(2, vm.state.value.logs.size)
        vm.clearLogs()
        assertTrue(vm.state.value.logs.isEmpty())
        assertTrue(vm.state.value.filteredLogs.isEmpty())
    }
}
