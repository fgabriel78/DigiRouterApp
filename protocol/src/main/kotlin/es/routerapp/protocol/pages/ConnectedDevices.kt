package es.routerapp.protocol.pages

import es.routerapp.protocol.RouterClient

/** A client known to the router (Wi-Fi or wired), as shown by the router's own "network map". */
data class ConnectedDevice(
    val name: String,
    val ip: String,
    val mac: String,
    val wifi: Boolean,
    /** Wi-Fi signal level (1..5), `null` for wired clients. */
    val level: Int?,
    /** Wi-Fi downlink rate or wired link speed, in Mbit/s. */
    val rate: Long?,
    val active: Boolean,
)

/**
 * Lists the clients connected to the router the same way the router's web UI does.
 *
 * `gl DEV2_HOST_ENTRY` is rejected by this firmware with error 71011 (`go` only works for the
 * first instances), and the web UI never calls it for the network map. Instead it runs
 * `ACT_UPDATE_MAPINFO` and then reads `DEV2_WIFI_APDEV_ASSOCDEV` (Wi-Fi clients) and
 * `DEV2_WIFI_APDEV_ETHASSOCDEV` (wired clients).
 */
object ConnectedDevices {

    /**
     * Refreshes the router's map info and reads both client lists. If one of the two lists cannot
     * be read the other one is still returned; if neither can, the first error is thrown.
     */
    fun load(client: RouterClient): List<ConnectedDevice> {
        // Best effort: a real session/connection problem will surface in the reads below.
        runCatching { client.operate("ACT_UPDATE_MAPINFO") }
        val wifi = runCatching { PageEngine.toInstances(client.getList("DEV2_WIFI_APDEV_ASSOCDEV")) }
        val wired = runCatching { PageEngine.toInstances(client.getList("DEV2_WIFI_APDEV_ETHASSOCDEV")) }
        if (wifi.isFailure && wired.isFailure) throw wifi.exceptionOrNull()!!
        return parse(wifi.getOrDefault(emptyList()), wired.getOrDefault(emptyList()))
    }

    /** Merges both lists (deduplicated by MAC, active entries win), active devices first, then by name. */
    fun parse(wifi: List<Instance>, wired: List<Instance>): List<ConnectedDevice> {
        val byMac = LinkedHashMap<String, ConnectedDevice>()
        var anonymous = 0
        for (d in wifi.map(::fromWifi) + wired.map(::fromWired)) {
            val key = d.mac.uppercase().ifEmpty { "?${anonymous++}" }
            val prev = byMac[key]
            if (prev == null || (!prev.active && d.active)) byMac[key] = d
        }
        return byMac.values.sortedWith(compareByDescending<ConnectedDevice> { it.active }.thenBy { it.name.lowercase() })
    }

    private fun fromWifi(i: Instance): ConnectedDevice {
        val v = i.values
        val ip = v["X_TP_IPAddress"].orEmpty()
        val mac = v["MACAddress"].orEmpty()
        return ConnectedDevice(
            name = v["X_TP_HostName"].orEmpty().ifEmpty { ip.ifEmpty { mac } },
            ip = ip,
            mac = mac,
            wifi = true,
            level = v["X_TP_SignalStrengthLevel"]?.toIntOrNull(),
            rate = v["lastDataDownlinkRate"]?.toLongOrNull()?.div(1000),
            active = v["active"] == "1",
        )
    }

    private fun fromWired(i: Instance): ConnectedDevice {
        val v = i.values
        val ip = v["IPAddress"].orEmpty()
        val mac = v["MACAddress"].orEmpty()
        return ConnectedDevice(
            name = v["X_TP_HostName"].orEmpty().ifEmpty { ip.ifEmpty { mac } },
            ip = ip,
            mac = mac,
            wifi = false,
            level = null,
            rate = v["linkSpeed"]?.toLongOrNull(),
            active = v["active"] == "1",
        )
    }
}
