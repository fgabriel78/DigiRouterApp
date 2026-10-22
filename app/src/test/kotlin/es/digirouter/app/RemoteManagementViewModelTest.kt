package es.digirouter.app

import es.routerapp.protocol.RouterClient
import es.routerapp.protocol.management.HttpConfig
import es.routerapp.protocol.management.IspManagementAudit
import es.routerapp.protocol.management.RemoteManagementEngine
import es.routerapp.protocol.management.RemoteManagementOverview
import es.routerapp.protocol.management.RemoteManagementRouterRpc
import es.routerapp.protocol.management.SshConfig
import es.routerapp.protocol.management.SuperHttpConfig
import es.routerapp.protocol.management.TelnetConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RemoteManagementViewModelTest {

    private val testScope = CoroutineScope(Dispatchers.Unconfined)

    private class FakeRpc : RemoteManagementRouterRpc {
        val gets = mutableMapOf<String, JsonElement>()
        val sets = mutableListOf<Pair<String, Map<String, String>>>()
        var certBytes = byteArrayOf(0x0A, 0x0B, 0x0C)

        override fun get(oid: String, attrs: Map<String, String>, stack: String): JsonElement =
            gets[oid] ?: JsonObject(emptyMap())

        override fun getList(oid: String, attrs: Map<String, String>, stack: String): JsonElement =
            gets[oid] ?: JsonArray(emptyList())

        override fun set(oid: String, attrs: Map<String, String>, stack: String): JsonElement {
            sets.add(oid to attrs)
            return JsonObject(emptyMap())
        }

        override fun downloadCaCert(): ByteArray = certBytes
    }

    private fun createViewModel(
        clientAvailable: Boolean = true,
        rpc: FakeRpc = FakeRpc(),
    ): RemoteManagementViewModel {
        val mockClient = if (clientAvailable) {
            // Fake non-null RouterClient instance placeholder
            RouterClient()
        } else {
            null
        }

        return RemoteManagementViewModel(
            clientProvider = { mockClient },
            coroutineScope = testScope,
            ioDispatcher = Dispatchers.Unconfined,
            engineProvider = { RemoteManagementEngine(rpc) },
        )
    }

    @Test
    fun initialStateHandlesNullClientGracefully() {
        val vm = createViewModel(clientAvailable = false)
        val state = vm.state.value

        assertFalse(state.isLoading)
        assertTrue(state.isError)
        assertEquals("Not connected", state.message)
    }

    @Test
    fun initialStateLoadsOverviewSuccessfully() {
        val rpc = FakeRpc()
        val vm = createViewModel(clientAvailable = true, rpc = rpc)
        val state = vm.state.value

        assertFalse(state.isLoading)
        assertFalse(state.isError)
        assertNotNull(state.overview)
    }

    @Test
    fun updateSshValidatesPorts() {
        val vm = createViewModel(clientAvailable = true)
        val invalidSsh = SshConfig(localPort = 99999)

        vm.updateSsh(invalidSsh)

        val state = vm.state.value
        assertTrue(state.isError)
        assertTrue(state.message?.contains("Invalid SSH port") == true)
    }

    @Test
    fun updateSshSavesSuccessfully() {
        val rpc = FakeRpc()
        val vm = createViewModel(clientAvailable = true, rpc = rpc)
        val updated = SshConfig(localPort = 2222, remoteEnabled = true)

        vm.updateSsh(updated)

        val state = vm.state.value
        assertFalse(state.isError)
        assertEquals("SSH configuration saved", state.message)
        assertEquals(2222, state.overview.ssh.localPort)
        assertEquals(1, rpc.sets.size)
    }

    @Test
    fun updateTelnetSavesSuccessfully() {
        val rpc = FakeRpc()
        val vm = createViewModel(clientAvailable = true, rpc = rpc)
        val updated = TelnetConfig(localEnabled = true, localPort = 2323)

        vm.updateTelnet(updated)

        val state = vm.state.value
        assertFalse(state.isError)
        assertEquals("Telnet configuration saved", state.message)
        assertEquals(2323, state.overview.telnet.localPort)
    }

    @Test
    fun updateHttpSavesSuccessfully() {
        val rpc = FakeRpc()
        val vm = createViewModel(clientAvailable = true, rpc = rpc)
        val updated = HttpConfig(httpsLocalPort = 8443)

        vm.updateHttp(updated)

        val state = vm.state.value
        assertFalse(state.isError)
        assertEquals("Web management configuration saved", state.message)
        assertEquals(8443, state.overview.http.httpsLocalPort)
    }

    @Test
    fun downloadAndInstallCertDispatchesEvent() {
        val rpc = FakeRpc()
        val vm = createViewModel(clientAvailable = true, rpc = rpc)

        assertNull(vm.installCertEvent.value)

        vm.downloadAndInstallCert()

        val event = vm.installCertEvent.value
        assertNotNull(event)
        assertEquals(3, event?.size)

        vm.clearCertEvent()
        assertNull(vm.installCertEvent.value)
    }
}
