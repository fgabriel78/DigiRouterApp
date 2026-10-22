package es.digirouter.app

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class VpnProfileShareTest {

    private lateinit var tempDir: File
    private lateinit var store: FileVpnProfileStore

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("vpn_profile_test").toFile()
        store = FileVpnProfileStore(tempDir)
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun saveAndRetrieveProfileFile() {
        val ovpnContent = "client\nremote myhome.org 1194\n"
        val file = store.saveProfile(ovpnContent, "client.ovpn")

        assertTrue(file.exists())
        assertEquals(ovpnContent, file.readText())

        val retrieved = store.getProfileFile("client.ovpn")
        assertNotNull(retrieved)
        assertEquals(file.absolutePath, retrieved!!.absolutePath)
    }

    @Test
    fun clearProfilesRemovesFiles() {
        store.saveProfile("content1", "client1.ovpn")
        store.saveProfile("content2", "client2.ovpn")
        assertEquals(2, tempDir.listFiles()?.size)

        store.clearProfiles()
        assertEquals(0, tempDir.listFiles()?.size)
        assertNull(store.getProfileFile("client1.ovpn"))
    }
}
