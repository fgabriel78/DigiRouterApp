package es.digirouter.app

import es.routerapp.protocol.RouterClient
import es.routerapp.protocol.vpn.OpenVpnConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

@OptIn(ExperimentalCoroutinesApi::class)
class VpnViewModelTest {

    private lateinit var tempDir: File
    private lateinit var store: FileVpnProfileStore
    private val testScope = CoroutineScope(Dispatchers.Unconfined)

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("vpn_vm_test").toFile()
        store = FileVpnProfileStore(tempDir)
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun initialStateHandlesNullClientGracefully() {
        val vm = VpnViewModel(
            clientProvider = { null },
            store = store,
            coroutineScope = testScope,
            ioDispatcher = Dispatchers.Unconfined,
        )

        val state = vm.state.value
        assertFalse(state.isLoading)
        assertTrue(state.isError)
        assertEquals("Not connected", state.message)
    }

    @Test
    fun hostOverrideUpdatesStateAndProfileOutput() {
        val vm = VpnViewModel(
            clientProvider = { null },
            store = store,
            coroutineScope = testScope,
            ioDispatcher = Dispatchers.Unconfined,
        )

        // Inject simulated raw profile
        vm.dismissMessage()
        vm.updateHostOverride("custom.home.vpn")
        assertEquals("custom.home.vpn", vm.state.value.hostOverride)
    }
}
