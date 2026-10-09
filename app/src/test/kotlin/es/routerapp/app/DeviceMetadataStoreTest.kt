package es.routerapp.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeviceMetadataStoreTest {

    private lateinit var prefs: FakeSharedPreferences
    private lateinit var store: DeviceMetadataStore

    @Before
    fun setUp() {
        prefs = FakeSharedPreferences()
        store = SharedPrefsDeviceMetadataStore(prefs)
    }

    @Test
    fun `get returns null when no metadata stored`() {
        assertNull(store.get("AA:BB:CC:DD:EE:FF"))
    }

    @Test
    fun `save and get preserves device custom metadata`() {
        val meta = DeviceCustomMetadata(
            alias = "Living Room TV",
            category = DeviceCategory.TV,
            notes = "OLED 55 inch",
        )

        store.save("aa:bb:cc:dd:ee:ff", meta)

        // Query with uppercase and lowercase
        val retrievedUpper = store.get("AA:BB:CC:DD:EE:FF")
        val retrievedLower = store.get("aa:bb:cc:dd:ee:ff")

        assertNotNull(retrievedUpper)
        assertEquals("Living Room TV", retrievedUpper?.alias)
        assertEquals(DeviceCategory.TV, retrievedUpper?.category)
        assertEquals("OLED 55 inch", retrievedUpper?.notes)
        assertEquals(retrievedUpper, retrievedLower)
    }

    @Test
    fun `reset removes metadata for MAC`() {
        val meta = DeviceCustomMetadata(alias = "Work Laptop", category = DeviceCategory.COMPUTER)
        store.save("11:22:33:44:55:66", meta)
        assertNotNull(store.get("11:22:33:44:55:66"))

        store.reset("11:22:33:44:55:66")
        assertNull(store.get("11:22:33:44:55:66"))
    }

    @Test
    fun `saving blank metadata resets entry`() {
        val meta = DeviceCustomMetadata(alias = "Phone", category = DeviceCategory.PHONE)
        store.save("11:22:33:44:55:66", meta)

        store.save("11:22:33:44:55:66", DeviceCustomMetadata(alias = "", category = null, notes = ""))
        assertNull(store.get("11:22:33:44:55:66"))
    }

    @Test
    fun `getAll returns all saved entries and clear empties store`() {
        store.save("00:11:22:33:44:55", DeviceCustomMetadata(alias = "Device 1", category = DeviceCategory.PHONE))
        store.save("AA:BB:CC:DD:EE:FF", DeviceCustomMetadata(alias = "Device 2", category = DeviceCategory.TV))

        val all = store.getAll()
        assertEquals(2, all.size)
        assertEquals("Device 1", all["00:11:22:33:44:55"]?.alias)
        assertEquals("Device 2", all["AA:BB:CC:DD:EE:FF"]?.alias)

        store.clear()
        assertTrue(store.getAll().isEmpty())
    }
}
