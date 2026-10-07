package es.routerapp.app

enum class WifiNetworkCategory {
    PRIMARY,
    GUEST,
    ADDITIONAL,
}

data class ConsolidatedWifiNetwork(
    val category: WifiNetworkCategory,
    val ssid: String,
    val bands: List<String>,
    val enabled: Boolean = true,
    val additionalIndex: Int? = null,
)

internal fun bandSortOrder(band: String?): Int = when {
    band == null -> 99
    band.startsWith("2") -> 0
    band.startsWith("5") -> 1
    band.startsWith("6") -> 2
    else -> 10
}

/**
 * Consolidates Wi-Fi network instances (from DEV2_ADT_WIFI_COMMON) into a unified list
 * for dashboard status display.
 *
 * Rules:
 * - Aggregates all enabled profiles: primary, guest, and additional (multi-SSID 1 & 2).
 * - Networks of the same type sharing an identical SSID across multiple frequency bands
 *   are consolidated into a single card with combined bands.
 * - If the primary network is disabled across all bands but another network is active,
 *   a primary disabled card is placed first so the user is informed that main Wi-Fi is off.
 * - If no wireless network is enabled across any band, returns an empty list, allowing
 *   the dashboard to show a single global fallback card.
 */
fun consolidateWifiNetworks(rawBands: List<Map<String, String>>): List<ConsolidatedWifiNetwork> {
    if (rawBands.isEmpty()) return emptyList()

    val result = mutableListOf<ConsolidatedWifiNetwork>()

    // 1. Primary Wi-Fi
    val primaryEnabled = rawBands.filter { it["primaryEnable"] == "1" && !it["primarySSID"].isNullOrBlank() }
    if (primaryEnabled.isNotEmpty()) {
        val grouped = primaryEnabled.groupBy { it["primarySSID"].orEmpty() }
        for ((ssid, instances) in grouped) {
            val bands = instances.mapNotNull { it["band"] }.distinct().sortedBy(::bandSortOrder)
            result.add(
                ConsolidatedWifiNetwork(
                    category = WifiNetworkCategory.PRIMARY,
                    ssid = ssid,
                    bands = bands,
                    enabled = true,
                )
            )
        }
    }

    // 2. Guest Wi-Fi
    val guestEnabled = rawBands.filter { it["guestEnable"] == "1" && !it["guestSSID"].isNullOrBlank() }
    if (guestEnabled.isNotEmpty()) {
        val grouped = guestEnabled.groupBy { it["guestSSID"].orEmpty() }
        for ((ssid, instances) in grouped) {
            val bands = instances.mapNotNull { it["band"] }.distinct().sortedBy(::bandSortOrder)
            result.add(
                ConsolidatedWifiNetwork(
                    category = WifiNetworkCategory.GUEST,
                    ssid = ssid,
                    bands = bands,
                    enabled = true,
                )
            )
        }
    }

    // 3. Multi-SSID 1 & 2
    for (n in 1..2) {
        val keyEnable = "mssid${n}Enable"
        val keySSID = "mssid${n}SSID"
        val mssidEnabled = rawBands.filter { it[keyEnable] == "1" && !it[keySSID].isNullOrBlank() }
        if (mssidEnabled.isNotEmpty()) {
            val grouped = mssidEnabled.groupBy { it[keySSID].orEmpty() }
            for ((ssid, instances) in grouped) {
                val bands = instances.mapNotNull { it["band"] }.distinct().sortedBy(::bandSortOrder)
                result.add(
                    ConsolidatedWifiNetwork(
                        category = WifiNetworkCategory.ADDITIONAL,
                        ssid = ssid,
                        bands = bands,
                        enabled = true,
                        additionalIndex = n,
                    )
                )
            }
        }
    }

    // If primary was completely disabled, but other networks (guest / additional) are enabled:
    // Add a disabled primary network card so the user sees that primary Wi-Fi is off.
    if (primaryEnabled.isEmpty() && result.isNotEmpty()) {
        result.add(
            0,
            ConsolidatedWifiNetwork(
                category = WifiNetworkCategory.PRIMARY,
                ssid = "",
                bands = emptyList(),
                enabled = false,
            )
        )
    }

    return result
}
