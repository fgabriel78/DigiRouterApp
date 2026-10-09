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
