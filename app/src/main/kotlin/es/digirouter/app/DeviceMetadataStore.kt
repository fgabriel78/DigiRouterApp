package es.digirouter.app

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

    fun getFingerprint(mac: String): DeviceFingerprint?
    fun saveFingerprint(mac: String, fingerprint: DeviceFingerprint)
    fun resetFingerprint(mac: String)
    fun getAllFingerprints(): Map<String, DeviceFingerprint>
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

    private fun fpKey(mac: String): String = "FP_${normalizeMac(mac)}"

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
            if (!k.startsWith("FP_") && v is String) {
                runCatching { json.decodeFromString<DeviceCustomMetadata>(v) }
                    .getOrNull()
                    ?.let { result[k] = it }
            }
        }
        return result
    }

    override fun getFingerprint(mac: String): DeviceFingerprint? {
        val norm = normalizeMac(mac)
        if (norm.isEmpty()) return null
        val raw = prefs.getString(fpKey(norm), null) ?: return null
        return runCatching { json.decodeFromString<DeviceFingerprint>(raw) }.getOrNull()
    }

    override fun saveFingerprint(mac: String, fingerprint: DeviceFingerprint) {
        val norm = normalizeMac(mac)
        if (norm.isEmpty()) return
        val encoded = runCatching { json.encodeToString(fingerprint) }.getOrNull() ?: return
        prefs.edit().putString(fpKey(norm), encoded).apply()
    }

    override fun resetFingerprint(mac: String) {
        val norm = normalizeMac(mac)
        if (norm.isEmpty()) return
        prefs.edit().remove(fpKey(norm)).apply()
    }

    override fun getAllFingerprints(): Map<String, DeviceFingerprint> {
        val allEntries = prefs.all ?: return emptyMap()
        val result = mutableMapOf<String, DeviceFingerprint>()
        for ((k, v) in allEntries) {
            if (k.startsWith("FP_") && v is String) {
                val mac = k.removePrefix("FP_")
                runCatching { json.decodeFromString<DeviceFingerprint>(v) }
                    .getOrNull()
                    ?.let { result[mac] = it }
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

/**
 * In-memory fallback / testing implementation of [DeviceMetadataStore].
 */
class InMemoryDeviceMetadataStore : DeviceMetadataStore {
    private val customData = mutableMapOf<String, DeviceCustomMetadata>()
    private val fingerprints = mutableMapOf<String, DeviceFingerprint>()

    private fun normalize(mac: String): String = mac.trim().uppercase()

    override fun get(mac: String): DeviceCustomMetadata? = customData[normalize(mac)]
    override fun save(mac: String, metadata: DeviceCustomMetadata) {
        val norm = normalize(mac)
        if (norm.isEmpty()) return
        if (metadata.alias.isNullOrBlank() && metadata.category == null && metadata.notes.isNullOrBlank()) {
            customData.remove(norm)
        } else {
            customData[norm] = metadata
        }
    }
    override fun reset(mac: String) { customData.remove(normalize(mac)) }
    override fun getAll(): Map<String, DeviceCustomMetadata> = customData.toMap()
    override fun clear() { customData.clear(); fingerprints.clear() }

    override fun getFingerprint(mac: String): DeviceFingerprint? = fingerprints[normalize(mac)]
    override fun saveFingerprint(mac: String, fingerprint: DeviceFingerprint) {
        val norm = normalize(mac)
        if (norm.isNotEmpty()) fingerprints[norm] = fingerprint
    }
    override fun resetFingerprint(mac: String) { fingerprints.remove(normalize(mac)) }
    override fun getAllFingerprints(): Map<String, DeviceFingerprint> = fingerprints.toMap()
}
