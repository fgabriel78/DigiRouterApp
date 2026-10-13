package es.routerapp.app

import kotlinx.serialization.Serializable

/**
 * Predefined consumer device categories for icon assignment and grouping.
 */
@Serializable
enum class DeviceCategory {
    PHONE,
    TABLET,
    COMPUTER,
    TV,
    CONSOLE,
    IOT,
    PRINTER,
    ROUTER,
    OTHER,
}

/**
 * User-assigned custom metadata associated with a client device (keyed by MAC address).
 */
@Serializable
data class DeviceCustomMetadata(
    val alias: String? = null,
    val category: DeviceCategory? = null,
    val notes: String? = null,
)

/**
 * Cached hardware deep fingerprint resolved via local network discovery protocols.
 */
@Serializable
data class DeviceFingerprint(
    val exactModel: String,
    val vendor: String,
    val category: DeviceCategory,
    val discoveryMethod: String,
    val timestampMs: Long = System.currentTimeMillis(),
)

