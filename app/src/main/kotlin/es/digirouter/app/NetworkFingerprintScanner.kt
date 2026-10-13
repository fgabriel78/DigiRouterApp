package es.digirouter.app

import android.content.Context
import android.net.wifi.WifiManager
import es.routerapp.protocol.fingerprint.DeviceSignatureMatcher
import es.routerapp.protocol.fingerprint.HttpDescriptor
import es.routerapp.protocol.fingerprint.HttpDescriptorParser
import es.routerapp.protocol.fingerprint.MdnsPacket
import es.routerapp.protocol.fingerprint.MdnsRecordParser
import es.routerapp.protocol.fingerprint.SsdpPacketParser
import es.routerapp.protocol.fingerprint.SsdpResponse
import es.routerapp.protocol.pages.ConnectedDevice
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.MulticastSocket
import java.util.concurrent.TimeUnit

/**
 * Orchestrator for local deep network discovery (mDNS, SSDP, targeted HTTP).
 */
class NetworkFingerprintScanner(
    private val context: Context? = null,
    private val metadataStore: DeviceMetadataStore? = null,
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(1000, TimeUnit.MILLISECONDS)
        .readTimeout(1000, TimeUnit.MILLISECONDS)
        .callTimeout(1200, TimeUnit.MILLISECONDS)
        .build(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val multicastBurstMs: Long = MULTICAST_BURST_MS,
) {

    /**
     * Scans active connected devices in the background and emits newly discovered or cached fingerprints.
     */
    fun scanFlow(devices: List<ConnectedDevice>): Flow<Pair<String, DeviceFingerprint>> = channelFlow {
        val activeClients = devices.filter { it.active && it.ip.isNotBlank() }
        val ipToDevice = activeClients.associateBy { it.ip.trim() }
        val unresolvedMacs = mutableSetOf<String>()

        // 1. Emit existing cached fingerprints immediately (0 ms network delay)
        for (d in activeClients) {
            val mac = d.mac.trim().uppercase()
            val cached = metadataStore?.getFingerprint(mac)
            if (cached != null) {
                send(mac to cached)
            } else {
                unresolvedMacs.add(mac)
            }
        }

        if (unresolvedMacs.isEmpty() || activeClients.isEmpty()) return@channelFlow

        // 2. Perform transient Multicast discovery burst (max 3.0 seconds)
        val discoveredMacs = mutableSetOf<String>()
        runCatching {
            executeMulticastBurst(
                unresolvedIps = ipToDevice.filterKeys { ip ->
                    val mac = ipToDevice[ip]?.mac?.trim()?.uppercase()
                    mac != null && unresolvedMacs.contains(mac)
                },
                onMatchFound = { mac, fp ->
                    discoveredMacs.add(mac)
                    unresolvedMacs.remove(mac)
                    metadataStore?.saveFingerprint(mac, fp)
                    send(mac to fp)
                },
            )
        }

        // 3. Targeted HTTP fallback probes for unresolved devices
        val pendingForHttp = activeClients.filter { unresolvedMacs.contains(it.mac.trim().uppercase()) }
        if (pendingForHttp.isNotEmpty()) {
            executeHttpFallbackProbes(
                targets = pendingForHttp,
                onMatchFound = { mac, fp ->
                    metadataStore?.saveFingerprint(mac, fp)
                    send(mac to fp)
                },
            )
        }
    }.flowOn(ioDispatcher)

    private suspend fun executeMulticastBurst(
        unresolvedIps: Map<String, ConnectedDevice>,
        onMatchFound: suspend (String, DeviceFingerprint) -> Unit,
    ) = withContext(ioDispatcher) {
        if (multicastBurstMs <= 0L) return@withContext
        val wifiManager = context?.applicationContext?.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val multicastLock = runCatching {
            wifiManager?.createMulticastLock("RouterApp:DeepFingerprint")?.apply {
                setReferenceCounted(false)
            }
        }.getOrNull()

        try {
            runCatching { multicastLock?.acquire() }

            withTimeoutOrNull(multicastBurstMs) {
                coroutineScope {
                    // SSDP listener & sender
                    launchSsdpTask(unresolvedIps, onMatchFound)
                    // mDNS listener & sender
                    launchMdnsTask(unresolvedIps, onMatchFound)
                }
            }
        } finally {
            runCatching {
                if (multicastLock?.isHeld == true) {
                    multicastLock.release()
                }
            }
        }
    }

    private suspend fun launchSsdpTask(
        unresolvedIps: Map<String, ConnectedDevice>,
        onMatchFound: suspend (String, DeviceFingerprint) -> Unit,
    ) = withContext(ioDispatcher) {
        val socket = runCatching {
            MulticastSocket(0).apply {
                soTimeout = 1000
                reuseAddress = true
            }
        }.getOrNull() ?: return@withContext

        try {
            val ssdpGroup = InetAddress.getByName("239.255.255.250")
            val mSearchBytes = SsdpPacketParser.buildMSearch("ssdp:all", mx = 2)
            val sendPacket = DatagramPacket(mSearchBytes, mSearchBytes.size, ssdpGroup, 1900)
            runCatching { socket.send(sendPacket) }

            val buffer = ByteArray(4096)
            while (isActive) {
                val recvPacket = DatagramPacket(buffer, buffer.size)
                val ok = runCatching { socket.receive(recvPacket) }.isSuccess
                if (!ok) break

                val senderIp = recvPacket.address?.hostAddress ?: continue
                val targetDevice = unresolvedIps[senderIp] ?: continue
                val mac = targetDevice.mac.trim().uppercase()

                val ssdpResp = SsdpPacketParser.parse(recvPacket.data, recvPacket.length)
                if (ssdpResp != null) {
                    val httpDesc = ssdpResp.location?.let { fetchDescriptor(it) }
                    val match = DeviceSignatureMatcher.match(
                        ssdp = ssdpResp,
                        httpDesc = httpDesc,
                        fallbackHostname = targetDevice.name,
                    )
                    if (match != null) {
                        val fp = toDeviceFingerprint(match)
                        onMatchFound(mac, fp)
                    }
                }
            }
        } finally {
            runCatching { socket.close() }
        }
    }

    private suspend fun launchMdnsTask(
        unresolvedIps: Map<String, ConnectedDevice>,
        onMatchFound: suspend (String, DeviceFingerprint) -> Unit,
    ) = withContext(ioDispatcher) {
        val socket = runCatching {
            MulticastSocket(0).apply {
                soTimeout = 1000
                reuseAddress = true
            }
        }.getOrNull() ?: return@withContext

        try {
            val mdnsGroup = InetAddress.getByName("224.0.0.251")
            val queryBytes = MdnsRecordParser.buildQuery(
                listOf(
                    "_googlecast._tcp.local",
                    "_airplay._tcp.local",
                    "_hue._tcp.local",
                    "_printer._tcp.local",
                    "_services._dns-sd._udp.local",
                ),
            )
            val sendPacket = DatagramPacket(queryBytes, queryBytes.size, mdnsGroup, 5353)
            runCatching { socket.send(sendPacket) }

            val buffer = ByteArray(4096)
            while (isActive) {
                val recvPacket = DatagramPacket(buffer, buffer.size)
                val ok = runCatching { socket.receive(recvPacket) }.isSuccess
                if (!ok) break

                val senderIp = recvPacket.address?.hostAddress ?: continue
                val targetDevice = unresolvedIps[senderIp] ?: continue
                val mac = targetDevice.mac.trim().uppercase()

                val packet = MdnsRecordParser.parse(recvPacket.data, recvPacket.length)
                if (packet != null && packet.isResponse) {
                    val match = DeviceSignatureMatcher.match(
                        mdns = packet,
                        fallbackHostname = targetDevice.name,
                    )
                    if (match != null) {
                        val fp = toDeviceFingerprint(match)
                        onMatchFound(mac, fp)
                    }
                }
            }
        } finally {
            runCatching { socket.close() }
        }
    }

    private suspend fun executeHttpFallbackProbes(
        targets: List<ConnectedDevice>,
        onMatchFound: suspend (String, DeviceFingerprint) -> Unit,
    ) = coroutineScope {
        val semaphore = Semaphore(MAX_PARALLEL_HTTP_PROBES)
        val probeEndpoints = listOf(
            "/description.xml" to 80,
            "/setup/eureka_info" to 8008,
            "/shelly" to 80,
        )

        val jobs = targets.map { device ->
            async(ioDispatcher) {
                semaphore.withPermit {
                    val mac = device.mac.trim().uppercase()
                    for ((path, port) in probeEndpoints) {
                        val portStr = if (port == 80) "" else ":$port"
                        val url = "http://${device.ip}$portStr$path"
                        val desc = fetchHttpMetadata(url)
                        if (desc != null) {
                            val match = DeviceSignatureMatcher.match(
                                httpDesc = desc,
                                fallbackHostname = device.name,
                            )
                            if (match != null) {
                                val fp = toDeviceFingerprint(match)
                                onMatchFound(mac, fp)
                                break
                            }
                        }
                    }
                }
            }
        }
        jobs.awaitAll()
    }

    private fun fetchDescriptor(locationUrl: String): HttpDescriptor? = runCatching {
        val req = Request.Builder().url(locationUrl).build()
        okHttpClient.newCall(req).execute().use { resp ->
            if (resp.isSuccessful) {
                val body = resp.body.string()
                val server = resp.header("Server")
                HttpDescriptorParser.parseAuto(body, server)
            } else {
                null
            }
        }
    }.getOrNull()

    private fun fetchHttpMetadata(url: String): HttpDescriptor? = runCatching {
        val req = Request.Builder().url(url).build()
        okHttpClient.newCall(req).execute().use { resp ->
            if (resp.isSuccessful) {
                val body = resp.body.string()
                val server = resp.header("Server")
                HttpDescriptorParser.parseAuto(body, server)
            } else {
                null
            }
        }
    }.getOrNull()

    private fun toDeviceFingerprint(match: es.routerapp.protocol.fingerprint.FingerprintMatch): DeviceFingerprint {
        val category = runCatching {
            DeviceCategory.valueOf(match.category.name)
        }.getOrDefault(DeviceCategory.OTHER)

        return DeviceFingerprint(
            exactModel = match.exactModel,
            vendor = match.vendor,
            category = category,
            discoveryMethod = match.discoveryMethod,
        )
    }

    companion object {
        const val MULTICAST_BURST_MS = 3000L
        const val MAX_PARALLEL_HTTP_PROBES = 4
    }
}
