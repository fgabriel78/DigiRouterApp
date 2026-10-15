package es.digirouter.app

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class BackupStoreTest {

    private lateinit var tempDir: File
    private lateinit var store: FileBackupStore

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("backup_test").toFile()
        store = FileBackupStore(tempDir, maxBackups = 5)
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `saveBackup persists file and metadata`() {
        val payload = byteArrayOf(1, 2, 3, 4, 5)
        val item = store.saveBackup(payload, deviceModel = "XGB430v Pro", firmwareVersion = "1.0.0")

        assertEquals(5L, item.sizeBytes)
        assertEquals("XGB430v Pro", item.deviceModel)
        assertEquals("1.0.0", item.firmwareVersion)
        assertTrue(item.displayName.startsWith("Copia "))

        val file = store.getBackupFile(item)
        assertNotNull(file)
        assertTrue(file!!.exists())
        assertEquals(5, file.readBytes().size)

        val list = store.listBackups()
        assertEquals(1, list.size)
        assertEquals(item.id, list[0].id)
    }

    @Test
    fun `strict 5-slot FIFO retention prunes oldest on 6th save`() {
        val createdFiles = mutableListOf<File>()

        for (i in 1..6) {
            // slight delay to ensure distinct timestamps
            Thread.sleep(15)
            val item = store.saveBackup(
                data = "backup-$i".toByteArray(),
                deviceModel = "XGB430v Pro",
                firmwareVersion = "v$i"
            )
            createdFiles.add(File(tempDir, item.filename))
        }

        val backups = store.listBackups()
        assertEquals(5, backups.size)

        // The oldest backup (i=1) must have been deleted
        val firstFile = createdFiles[0]
        assertFalse("Oldest backup file should be deleted from disk", firstFile.exists())

        // The remaining 5 files must still exist
        for (i in 1 until createdFiles.size) {
            assertTrue("Backup $i should still exist", createdFiles[i].exists())
        }

        // Most recent should be v6
        assertEquals("v6", backups[0].firmwareVersion)
        // Oldest retained should be v2
        assertEquals("v2", backups[4].firmwareVersion)
    }

    @Test
    fun `deleteBackup removes file from disk and index`() {
        val item = store.saveBackup("sample".toByteArray())
        val file = store.getBackupFile(item)
        assertNotNull(file)
        assertTrue(file!!.exists())

        val deleted = store.deleteBackup(item)
        assertTrue(deleted)
        assertFalse(file.exists())
        assertEquals(0, store.listBackups().size)
    }

    @Test
    fun `listBackups ignores missing files and repairs index`() {
        val item1 = store.saveBackup("b1".toByteArray())
        val item2 = store.saveBackup("b2".toByteArray())

        // Manually delete item1 file from disk
        val file1 = store.getBackupFile(item1)!!
        file1.delete()

        val list = store.listBackups()
        assertEquals(1, list.size)
        assertEquals(item2.id, list[0].id)
    }

    @Test
    fun `clearAll removes all files and resets index`() {
        store.saveBackup("1".toByteArray())
        store.saveBackup("2".toByteArray())
        assertEquals(2, store.listBackups().size)

        store.clearAll()
        assertEquals(0, store.listBackups().size)
    }
}
