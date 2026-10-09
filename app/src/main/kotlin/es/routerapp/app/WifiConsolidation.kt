package es.routerapp.app

enum class WifiNetworkCategory {
    PRIMARY,
    GUEST,
    ADDITIONAL,
    MLO,
}

data class ConsolidatedWifiNetwork(
    val category: WifiNetworkCategory,
    val ssid: String,
    val bands: List<String>,
    val enabled: Boolean = true,
    val additionalIndex: Int? = null,
    val psk: String = "",
    val securityMode: String = "WPA2-Personal",
    val hidden: Boolean = false,
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
 * for dashboard status display and credential sharing.
 *
 * Rules:
 * - Aggregates all enabled profiles: primary, guest, additional (multi-SSID 1 & 2), and MLO.
 * - Extracts credentials (psk, securityMode, hidden flag) for each consolidated profile.
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
            val psk = instances.firstNotNullOfOrNull { it["primaryPSK"]?.takeIf { p -> p.isNotEmpty() } }.orEmpty()
            val mode = instances.firstNotNullOfOrNull { it["primaryModeEnabled"]?.takeIf { m -> m.isNotEmpty() } } ?: "WPA2-Personal"
            val hidden = instances.any { it["primarySSIDAdvertise"] == "0" }
            result.add(
                ConsolidatedWifiNetwork(
                    category = WifiNetworkCategory.PRIMARY,
                    ssid = ssid,
                    bands = bands,
                    enabled = true,
                    psk = psk,
                    securityMode = mode,
                    hidden = hidden,
                )
            )
        }
    }

    // 2. Wi-Fi 7 MLO
    val mloEnabled = rawBands.filter { it["mloEnable"] == "1" && !it["mloSSID"].isNullOrBlank() }
    if (mloEnabled.isNotEmpty()) {
        val grouped = mloEnabled.groupBy { it["mloSSID"].orEmpty() }
        for ((ssid, instances) in grouped) {
            val bands = instances.mapNotNull { it["band"] }.distinct().sortedBy(::bandSortOrder)
            val psk = instances.firstNotNullOfOrNull { it["mloPSK"]?.takeIf { p -> p.isNotEmpty() } }.orEmpty()
            val mode = instances.firstNotNullOfOrNull { it["mloModeEnabled"]?.takeIf { m -> m.isNotEmpty() } } ?: "WPA2-WPA3-Personal"
            val hidden = instances.any { it["mloSSIDAdvertise"] == "0" }
            result.add(
                ConsolidatedWifiNetwork(
                    category = WifiNetworkCategory.MLO,
                    ssid = ssid,
                    bands = bands,
                    enabled = true,
                    psk = psk,
                    securityMode = mode,
                    hidden = hidden,
                )
            )
        }
    }

    // 3. Guest Wi-Fi
    val guestEnabled = rawBands.filter { it["guestEnable"] == "1" && !it["guestSSID"].isNullOrBlank() }
    if (guestEnabled.isNotEmpty()) {
        val grouped = guestEnabled.groupBy { it["guestSSID"].orEmpty() }
        for ((ssid, instances) in grouped) {
            val bands = instances.mapNotNull { it["band"] }.distinct().sortedBy(::bandSortOrder)
            val psk = instances.firstNotNullOfOrNull { it["guestPSK"]?.takeIf { p -> p.isNotEmpty() } }.orEmpty()
            val mode = instances.firstNotNullOfOrNull { it["guestModeEnabled"]?.takeIf { m -> m.isNotEmpty() } } ?: "WPA2-Personal"
            val hidden = instances.any { it["guestSSIDAdvertise"] == "0" }
            result.add(
                ConsolidatedWifiNetwork(
                    category = WifiNetworkCategory.GUEST,
                    ssid = ssid,
                    bands = bands,
                    enabled = true,
                    psk = psk,
                    securityMode = mode,
                    hidden = hidden,
                )
            )
        }
    }

    // 4. Multi-SSID 1 & 2
    for (n in 1..2) {
        val keyEnable = "mssid${n}Enable"
        val keySSID = "mssid${n}SSID"
        val keyPSK = "mssid${n}PSK"
        val keyMode = "mssid${n}ModeEnabled"
        val keyAdv = "mssid${n}SSIDAdvertise"
        val mssidEnabled = rawBands.filter { it[keyEnable] == "1" && !it[keySSID].isNullOrBlank() }
        if (mssidEnabled.isNotEmpty()) {
            val grouped = mssidEnabled.groupBy { it[keySSID].orEmpty() }
            for ((ssid, instances) in grouped) {
                val bands = instances.mapNotNull { it["band"] }.distinct().sortedBy(::bandSortOrder)
                val psk = instances.firstNotNullOfOrNull { it[keyPSK]?.takeIf { p -> p.isNotEmpty() } }.orEmpty()
                val mode = instances.firstNotNullOfOrNull { it[keyMode]?.takeIf { m -> m.isNotEmpty() } } ?: "WPA2-Personal"
                val hidden = instances.any { it[keyAdv] == "0" }
                result.add(
                    ConsolidatedWifiNetwork(
                        category = WifiNetworkCategory.ADDITIONAL,
                        ssid = ssid,
                        bands = bands,
                        enabled = true,
                        additionalIndex = n,
                        psk = psk,
                        securityMode = mode,
                        hidden = hidden,
                    )
                )
            }
        }
    }

    // If primary was completely disabled, but other networks (MLO / guest / additional) are enabled:
    // Add a disabled primary network card so the user sees that primary Wi-Fi is off.
    if (primaryEnabled.isEmpty() && result.isNotEmpty() && mloEnabled.isEmpty()) {
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
