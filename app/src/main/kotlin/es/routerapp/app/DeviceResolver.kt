package es.routerapp.app

/**
 * Origin source of the resolved manufacturer name.
 */
enum class VendorSource {
    /** Resolved via local network deep fingerprinting (mDNS/SSDP/HTTP UPnP). */
    DEEP_FINGERPRINT,

    /** Resolved via IEEE Organizationally Unique Identifier (OUI) from hardware MAC. */
    HARDWARE_OUI,

    /** Inferred via DHCP client hostname pattern heuristics. */
    HOSTNAME_HEURISTIC,

    /** No vendor identified. */
    NONE,
}

/**
 * Consolidated resolved display attributes for a client device.
 */
data class ResolvedDeviceInfo(
    val displayTitle: String,
    val vendor: String?,
    val vendorSource: VendorSource,
    val isPrivateMac: Boolean,
    val category: DeviceCategory?,
    val isCategoryInferred: Boolean,
    val exactModel: String? = null,
    val isDeepFingerprinted: Boolean = false,
)

/**
 * Unified resolver enforcing precedence across custom user metadata,
 * deep network fingerprinting, hardware OUI lookup, hostname heuristics,
 * and private MAC detection.
 */
object DeviceResolver {

    fun resolve(
        mac: String,
        hostname: String? = null,
        ip: String? = null,
        customMeta: DeviceCustomMetadata? = null,
        fingerprint: DeviceFingerprint? = null,
    ): ResolvedDeviceInfo {
        val ouiInfo = OuiLookup.resolve(mac)
        val heuristic = DeviceHostnameHeuristics.infer(hostname)

        // 1. Vendor & Source
        val (vendor, vendorSource) = when {
            fingerprint?.vendor != null -> fingerprint.vendor to VendorSource.DEEP_FINGERPRINT
            ouiInfo.vendor != null -> ouiInfo.vendor to VendorSource.HARDWARE_OUI
            heuristic.vendor != null -> heuristic.vendor to VendorSource.HOSTNAME_HEURISTIC
            else -> null to VendorSource.NONE
        }

        // 2. Category
        val (category, isCategoryInferred) = when {
            customMeta?.category != null -> customMeta.category to false
            fingerprint?.category != null -> fingerprint.category to false
            heuristic.category != null -> heuristic.category to true
            else -> null to false
        }

        // 3. Display Title
        val cleanHost = hostname?.takeIf { it.isNotBlank() }
        val cleanIp = ip?.takeIf { it.isNotBlank() }
        val displayTitle = customMeta?.alias?.takeIf { it.isNotBlank() }
            ?: fingerprint?.exactModel?.takeIf { it.isNotBlank() }
            ?: cleanHost
            ?: vendor
            ?: cleanIp
            ?: mac

        return ResolvedDeviceInfo(
            displayTitle = displayTitle,
            vendor = vendor,
            vendorSource = vendorSource,
            isPrivateMac = ouiInfo.isPrivate,
            category = category,
            isCategoryInferred = isCategoryInferred,
            exactModel = fingerprint?.exactModel,
            isDeepFingerprinted = fingerprint != null,
        )
    }
}
