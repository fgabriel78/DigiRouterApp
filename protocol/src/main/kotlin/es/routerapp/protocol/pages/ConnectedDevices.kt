package es.routerapp.protocol.pages

import es.routerapp.protocol.RouterClient

/** An EasyMesh access point node (controller or satellite agent). */
data class MeshNode(
    val mac: String,
    val name: String,
    val model: String,
    val ip: String,
    val isController: Boolean,
    val active: Boolean,
    /** Backhaul connection medium ("Wi-Fi", "Ethernet"), null for the controller. */
    val backhaulType: String?,
    /** Backhaul signal strength RCPI / level, null for wired or controller. */
    val backhaulSignal: Int?,
    /** Backhaul link speed in Mbit/s. */
    val linkRate: Long?,
    /** Uptime in seconds. */
    val uptime: Long?,
    /** Number of active clients associated with this node. */
    val connectedClientsCount: Int = 0,
)

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
    /** MAC address of the EasyMesh node/AP device this client is connected to. */
    val nodeMac: String? = null,
    /** Display name of the EasyMesh node/AP device this client is connected to. */
    val nodeName: String? = null,
)

/** Composite result holding both connected devices and EasyMesh nodes. */
data class ConnectedDevicesData(
    val devices: List<ConnectedDevice>,
    val nodes: List<MeshNode>,
)

/**
 * Lists the clients and mesh nodes known to the router the same way the router's web UI does.
 *
 * `gl DEV2_HOST_ENTRY` is rejected by this firmware with error 71011 (`go` only works for the
 * first instances), and the web UI never calls it for the network map. Instead it runs
 * `ACT_UPDATE_MAPINFO` and then reads `DEV2_WIFI_APDEV_ASSOCDEV` (Wi-Fi clients),
 * `DEV2_WIFI_APDEV_ETHASSOCDEV` (wired clients), and `DEV2_WIFI_APDEV` (mesh access points).
 */
object ConnectedDevices {

    /**
     * Refreshes the router's map info and reads both client lists and mesh nodes.
     * If one of the client lists cannot be read the other is still returned;
     * if neither can, the first error is thrown. AP devices query failure is tolerated.
     */
    fun loadData(client: RouterClient): ConnectedDevicesData {
        // Best effort: a real session/connection problem will surface in the reads below.
        runCatching { client.operate("ACT_UPDATE_MAPINFO") }
        val wifi = runCatching { PageEngine.toInstances(client.getList("DEV2_WIFI_APDEV_ASSOCDEV")) }
        val wired = runCatching { PageEngine.toInstances(client.getList("DEV2_WIFI_APDEV_ETHASSOCDEV")) }
        val apDevs = runCatching { PageEngine.toInstances(client.getList("DEV2_WIFI_APDEV")) }
        if (wifi.isFailure && wired.isFailure) throw wifi.exceptionOrNull()!!
        return parseData(
            wifi.getOrDefault(emptyList()),
            wired.getOrDefault(emptyList()),
            apDevs.getOrDefault(emptyList()),
        )
    }

    /** Convenience method to load only devices, preserving existing signature. */
    fun load(client: RouterClient): List<ConnectedDevice> = loadData(client).devices

    /** Convenience method to load only mesh nodes. */
    fun loadNodes(client: RouterClient): List<MeshNode> = loadData(client).nodes

    /** Merges both lists (deduplicated by MAC, active entries win), active devices first, then by name. */
    fun parse(
        wifi: List<Instance>,
        wired: List<Instance>,
        apDevs: List<Instance> = emptyList(),
    ): List<ConnectedDevice> = parseData(wifi, wired, apDevs).devices

    /** Parses both devices and mesh nodes, mapping client node associations and client counts. */
    fun parseData(
        wifi: List<Instance>,
        wired: List<Instance>,
        apDevs: List<Instance> = emptyList(),
    ): ConnectedDevicesData {
        val rawNodes = parseNodes(apDevs)
        val nodesByMac = rawNodes.associateBy { it.mac.uppercase() }

        val byMac = LinkedHashMap<String, ConnectedDevice>()
        var anonymous = 0
        for (d in wifi.map { fromWifi(it, nodesByMac) } + wired.map { fromWired(it, nodesByMac) }) {
            val key = d.mac.uppercase().ifEmpty { "?${anonymous++}" }
            val prev = byMac[key]
            if (prev == null || (!prev.active && d.active)) byMac[key] = d
        }
        val devices = byMac.values.sortedWith(compareByDescending<ConnectedDevice> { it.active }.thenBy { it.name.lowercase() })

        val clientCounts = devices.filter { it.active && it.nodeMac != null }
            .groupingBy { it.nodeMac!!.uppercase() }
            .eachCount()

        val nodes = rawNodes.map { node ->
            node.copy(connectedClientsCount = clientCounts[node.mac.uppercase()] ?: 0)
        }

        return ConnectedDevicesData(devices = devices, nodes = nodes)
    }

    fun parseNodes(apDevs: List<Instance>): List<MeshNode> {
        val list = apDevs.mapNotNull { i ->
            val v = i.values
            val mac = v["MACAddress"].orEmpty().ifEmpty { v["X_TP_MACAddress"].orEmpty() }.trim()
            if (mac.isEmpty()) return@mapNotNull null
            val isController = v["X_TP_IsController"] == "1"
            val hostName = v["X_TP_HostName"].orEmpty().trim()
            val name = hostName.ifEmpty { if (isController) "Router" else "Mesh Agent" }
            val model = v["X_TP_ModelName"].orEmpty().ifEmpty { v["X_TP_DeviceType"].orEmpty() }.trim()
            val ip = v["X_TP_IPAddress"].orEmpty().trim()
            val active = v["X_TP_Active"] != "0"
            val backhaulType = if (isController) null else v["backhaulLinkType"]?.trim()?.ifEmpty { null }
            val signal = v["backhaulSignalStrength"]?.trim()?.toIntOrNull()
            val linkRate = v["X_TP_LinkRate"]?.trim()?.toLongOrNull()
            val uptime = (v["X_TP_UpTime"] ?: v["upTime"])?.trim()?.toLongOrNull()
            MeshNode(
                mac = mac.uppercase(),
                name = name,
                model = model,
                ip = ip,
                isController = isController,
                active = active,
                backhaulType = backhaulType,
                backhaulSignal = signal,
                linkRate = linkRate,
                uptime = uptime,
            )
        }
        return list.sortedWith(compareByDescending<MeshNode> { it.isController }.thenByDescending { it.active }.thenBy { it.name.lowercase() })
    }

    private fun fromWifi(i: Instance, nodesByMac: Map<String, MeshNode>): ConnectedDevice {
        val v = i.values
        val ip = v["X_TP_IPAddress"].orEmpty()
        val mac = v["MACAddress"].orEmpty()
        val nodeMac = v["X_TP_ApDeviceMac"]?.trim()?.uppercase()?.ifEmpty { null }
        val nodeName = nodeMac?.let { nodesByMac[it]?.name }
        return ConnectedDevice(
            name = v["X_TP_HostName"].orEmpty().ifEmpty { ip.ifEmpty { mac } },
            ip = ip,
            mac = mac,
            wifi = true,
            level = v["X_TP_SignalStrengthLevel"]?.toIntOrNull(),
            rate = v["lastDataDownlinkRate"]?.toLongOrNull()?.div(1000),
            active = v["active"] == "1",
            nodeMac = nodeMac,
            nodeName = nodeName,
        )
    }

    private fun fromWired(i: Instance, nodesByMac: Map<String, MeshNode>): ConnectedDevice {
        val v = i.values
        val ip = v["IPAddress"].orEmpty()
        val mac = v["MACAddress"].orEmpty()
        val nodeMac = v["APDeviceMACAddress"]?.trim()?.uppercase()?.ifEmpty { null }
        val nodeName = nodeMac?.let { nodesByMac[it]?.name }
        return ConnectedDevice(
            name = v["X_TP_HostName"].orEmpty().ifEmpty { ip.ifEmpty { mac } },
            ip = ip,
            mac = mac,
            wifi = false,
            level = null,
            rate = v["linkSpeed"]?.toLongOrNull(),
            active = v["active"] == "1",
            nodeMac = nodeMac,
            nodeName = nodeName,
        )
    }
}

