package es.routerapp.app

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Interface for persisting and retrieving user-assigned device metadata (aliases, categories, notes).
 */
interface DeviceMetadataStore {
    fun get(mac: String): DeviceCustomMetadata?
    fun save(mac: String, metadata: DeviceCustomMetadata)
    fun reset(mac: String)
    fun getAll(): Map<String, DeviceCustomMetadata>
    fun clear()
}

/**
 * SharedPreferences-backed implementation of [DeviceMetadataStore] storing serialized JSON.
 */
class SharedPrefsDeviceMetadataStore(
    private val prefs: SharedPreferences,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : DeviceMetadataStore {

    constructor(context: Context, prefsName: String = PREFS_NAME) : this(
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
    )

    private fun normalizeMac(mac: String): String =
        mac.trim().uppercase()

    override fun get(mac: String): DeviceCustomMetadata? {
        val key = normalizeMac(mac)
        if (key.isEmpty()) return null
        val raw = prefs.getString(key, null) ?: return null
        return runCatching { json.decodeFromString<DeviceCustomMetadata>(raw) }.getOrNull()
    }

    override fun save(mac: String, metadata: DeviceCustomMetadata) {
        val key = normalizeMac(mac)
        if (key.isEmpty()) return
        if (metadata.alias.isNullOrBlank() && metadata.category == null && metadata.notes.isNullOrBlank()) {
            reset(mac)
            return
        }
        val encoded = runCatching { json.encodeToString(metadata) }.getOrNull() ?: return
        prefs.edit().putString(key, encoded).apply()
    }

    override fun reset(mac: String) {
        val key = normalizeMac(mac)
        if (key.isEmpty()) return
        prefs.edit().remove(key).apply()
    }

    override fun getAll(): Map<String, DeviceCustomMetadata> {
        val allEntries = prefs.all ?: return emptyMap()
        val result = mutableMapOf<String, DeviceCustomMetadata>()
        for ((k, v) in allEntries) {
            if (v is String) {
                runCatching { json.decodeFromString<DeviceCustomMetadata>(v) }
                    .getOrNull()
                    ?.let { result[k] = it }
            }
        }
        return result
    }

    override fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        const val PREFS_NAME = "device_metadata"
    }
}
