package es.routerapp.app

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeviceMetadataTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `serializes and deserializes DeviceCustomMetadata`() {
        val original = DeviceCustomMetadata(
            alias = "Living Room TV",
            category = DeviceCategory.TV,
            notes = "HDMI 1 connected to soundbar",
        )

        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<DeviceCustomMetadata>(encoded)

        assertEquals("Living Room TV", decoded.alias)
        assertEquals(DeviceCategory.TV, decoded.category)
        assertEquals("HDMI 1 connected to soundbar", decoded.notes)
    }

    @Test
    fun `handles partial and null fields during deserialization`() {
        val raw = """{"alias":"My Phone"}"""
        val decoded = json.decodeFromString<DeviceCustomMetadata>(raw)

        assertEquals("My Phone", decoded.alias)
        assertNull(decoded.category)
        assertNull(decoded.notes)
    }
}
