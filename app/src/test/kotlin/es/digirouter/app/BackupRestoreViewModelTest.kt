package es.digirouter.app

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

@OptIn(ExperimentalCoroutinesApi::class)
class BackupRestoreViewModelTest {

    private lateinit var tempDir: File
    private lateinit var store: FileBackupStore
    private val testScope = CoroutineScope(Dispatchers.Unconfined)

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("vm_backup_test").toFile()
        store = FileBackupStore(tempDir)
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `initial state loads existing backups`() {
        store.saveBackup("old".toByteArray())
        val vm = BackupRestoreViewModel(
            clientProvider = { null },
            store = store,
            coroutineScope = testScope,
        )

        val state = vm.state.value
        assertTrue(state is BackupRestoreUiState.Idle)
        assertEquals(1, state.backups.size)
    }

    @Test
    fun `createBackup when client is null sets error`() {
        val vm = BackupRestoreViewModel(
            clientProvider = { null },
            store = store,
            coroutineScope = testScope,
        )

        vm.createBackup()
        val state = vm.state.value
        assertTrue(state is BackupRestoreUiState.Idle)
        val idle = state as BackupRestoreUiState.Idle
        assertTrue(idle.isError)
        assertEquals("Router session is not active", idle.message)
    }

    @Test
    fun `restore missing backup file sets error`() {
        val item = BackupItem("missing", "missing.bin", System.currentTimeMillis())
        val vm = BackupRestoreViewModel(
            clientProvider = { null },
            store = store,
            coroutineScope = testScope,
        )

        vm.restore(item)
        val state = vm.state.value
        assertTrue(state is BackupRestoreUiState.Idle)
        val idle = state as BackupRestoreUiState.Idle
        assertTrue(idle.isError)
        assertEquals("Backup file not found", idle.message)
    }

    @Test
    fun `deleteBackup removes item and updates state`() {
        val item = store.saveBackup("test".toByteArray())
        val vm = BackupRestoreViewModel(
            clientProvider = { null },
            store = store,
            coroutineScope = testScope,
        )
        assertEquals(1, vm.state.value.backups.size)

        vm.deleteBackup(item)
        assertEquals(0, vm.state.value.backups.size)
    }
}
