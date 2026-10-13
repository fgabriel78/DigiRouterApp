package es.routerapp.protocol.dualdiag

import es.routerapp.protocol.RouterClient
import es.routerapp.protocol.pages.PageEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.math.abs
import kotlin.math.max
import kotlin.system.measureTimeMillis

/**
 * Interface abstraction for router hardware diagnostic operations (TR-181 OIDs).
 */
interface RouterDiagnosticClient {
    suspend fun set(oid: String, attrs: Map<String, String>): Boolean
    suspend fun get(oid: String): Map<String, String>
}

/**
 * Default implementation of [RouterDiagnosticClient] delegating to [RouterClient].
 */
class DefaultRouterDiagnosticClient(private val client: RouterClient) : RouterDiagnosticClient {
    override suspend fun set(oid: String, attrs: Map<String, String>): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            client.set(oid, attrs)
            true
        }.getOrDefault(false)
    }

    override suspend fun get(oid: String): Map<String, String> = withContext(Dispatchers.IO) {
        val fromGet = runCatching {
            PageEngine.toInstances(client.get(oid)).firstOrNull()?.values.orEmpty()
        }.getOrNull()
        if (!fromGet.isNullOrEmpty()) return@withContext fromGet

        runCatching {
            PageEngine.toInstances(client.getList(oid)).firstOrNull()?.values.orEmpty()
        }.getOrDefault(emptyMap())
    }
}

/**
 * Network latency probe executing socket round-trip probes from the mobile client.
 */
interface DualNetworkProbe {
    suspend fun probeSamples(
        host: String,
        port: Int = 80,
        samples: Int = 4,
        timeoutMs: Int = 1500,
    ): HopLatencyMetric
}

/**
 * Default implementation of [DualNetworkProbe] performing TCP socket probes.
 */
class DefaultDualNetworkProbe : DualNetworkProbe {
    override suspend fun probeSamples(
        host: String,
        port: Int,
        samples: Int,
        timeoutMs: Int,
    ): HopLatencyMetric = withContext(Dispatchers.IO) {
        val latencies = mutableListOf<Double>()
        var failed = 0
        val hopType = if (host == DualDiagnosticEngine.CLOUDFLARE_DNS) {
            HopType.MOBILE_INTERNET
        } else {
            HopType.WIFI_LAN
        }

        repeat(samples) {
            val sampleMs = probeSingleSample(host, port, timeoutMs)
            if (sampleMs != null) {
                latencies.add(sampleMs)
            } else {
                failed++
            }
        }

        if (latencies.isEmpty()) {
            return@withContext HopLatencyMetric(
                type = hopType,
                targetHost = host,
                avgMs = null,
                minMs = null,
                maxMs = null,
                packetLossPercent = 100,
                jitterMs = null,
                isSuccess = false,
                error = "Host $host unreachable",
            )
        }

        val avg = latencies.average()
        val min = latencies.minOrNull() ?: avg
        val max = latencies.maxOrNull() ?: avg
        val jitter = if (latencies.size > 1) {
            latencies.zipWithNext { a, b -> abs(a - b) }.average()
        } else {
            0.0
        }
        val lossPercent = ((failed * 100) / samples)

        HopLatencyMetric(
            type = hopType,
            targetHost = host,
            avgMs = avg,
            minMs = min,
            maxMs = max,
            packetLossPercent = lossPercent,
            jitterMs = jitter,
            isSuccess = true,
        )
    }

    private fun probeSingleSample(host: String, port: Int, timeoutMs: Int): Double? {
        return try {
            val time = measureTimeMillis {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(host, port), timeoutMs)
                }
            }
            time.toDouble()
        } catch (_: Exception) {
            try {
                var reachable = false
                val time = measureTimeMillis {
                    reachable = InetAddress.getByName(host).isReachable(timeoutMs)
                }
                if (reachable) time.toDouble() else null
            } catch (_: Exception) {
                null
            }
        }
    }
}

/**
 * Coordinates dual diagnostic latency measurements:
 * 1. Router hardware ICMP ping to Cloudflare (1.1.1.1) via DEV2_DIAG_IPPING
 * 2. Mobile client TCP probe to router gateway (192.168.1.1)
 * 3. Mobile client TCP probe to Cloudflare (1.1.1.1)
 *
 * Compares hops to isolate in-home Wi-Fi latency bottlenecks from carrier fiber latency.
 */
class DualDiagnosticEngine(
    private val routerClient: RouterDiagnosticClient,
    private val networkProbe: DualNetworkProbe = DefaultDualNetworkProbe(),
    private val targetHost: String = CLOUDFLARE_DNS,
    private val routerGatewayIp: String = DEFAULT_GATEWAY_IP,
    private val pollDelayMs: Long = 800L,
    private val maxPollAttempts: Int = 6,
) {
    constructor(
        client: RouterClient,
        networkProbe: DualNetworkProbe = DefaultDualNetworkProbe(),
        targetHost: String = CLOUDFLARE_DNS,
        routerGatewayIp: String = DEFAULT_GATEWAY_IP,
    ) : this(
        routerClient = DefaultRouterDiagnosticClient(client),
        networkProbe = networkProbe,
        targetHost = targetHost,
        routerGatewayIp = routerGatewayIp,
    )

    companion object {
        const val CLOUDFLARE_DNS = "1.1.1.1"
        const val DEFAULT_GATEWAY_IP = "192.168.1.1"
        const val OID_DEV2_DIAG_IPPING = "DEV2_DIAG_IPPING"
    }

    fun runDiagnostics(): Flow<DualDiagnosticState> = flow {
        var state = DualDiagnosticState(
            isRunning = true,
            stage = DualDiagnosticStage.PROBING_WIFI,
        )
        emit(state)

        // Trigger router hardware ping early so it runs while mobile probes are executed
        val routerPingInitiated = runCatching {
            routerClient.set(
                OID_DEV2_DIAG_IPPING,
                mapOf(
                    "diagnosticsState" to "Requested",
                    "host" to targetHost,
                    "numberOfRepetitions" to "4",
                ),
            )
        }.getOrDefault(false)

        // 1. Mobile Client Wi-Fi Hop probe (Mobile -> Gateway 192.168.1.1)
        val wifiHop = networkProbe.probeSamples(routerGatewayIp, port = 80, samples = 4)
        state = state.copy(
            wifiHop = wifiHop,
            stage = DualDiagnosticStage.PROBING_MOBILE_INTERNET,
        )
        emit(state)

        // 2. Mobile Client Internet Hop probe (Mobile -> 1.1.1.1)
        val mobileInternetHop = networkProbe.probeSamples(targetHost, port = 80, samples = 4)
        state = state.copy(
            mobileInternetHop = mobileInternetHop,
            stage = DualDiagnosticStage.PROBING_ROUTER_WAN,
        )
        emit(state)

        // 3. Poll router hardware ping results
        var routerHop: HopLatencyMetric? = null
        var isEstimated = false

        if (routerPingInitiated) {
            for (attempt in 1..maxPollAttempts) {
                if (pollDelayMs > 0) {
                    kotlinx.coroutines.delay(pollDelayMs)
                }
                val data = routerClient.get(OID_DEV2_DIAG_IPPING)
                val diagState = data["diagnosticsState"] ?: data["DiagnosticsState"]
                if (diagState.equals("Complete", ignoreCase = true) || diagState.equals("Completed", ignoreCase = true)) {
                    val avg = data["averageResponseTime"]?.toDoubleOrNull()
                        ?: data["AverageResponseTime"]?.toDoubleOrNull()
                    val min = data["minimumResponseTime"]?.toDoubleOrNull()
                        ?: data["MinimumResponseTime"]?.toDoubleOrNull()
                    val max = data["maximumResponseTime"]?.toDoubleOrNull()
                        ?: data["MaximumResponseTime"]?.toDoubleOrNull()
                    val failureCount = data["failureCount"]?.toIntOrNull()
                        ?: data["FailureCount"]?.toIntOrNull() ?: 0
                    val successCount = data["successCount"]?.toIntOrNull()
                        ?: data["SuccessCount"]?.toIntOrNull() ?: 4
                    val total = failureCount + successCount
                    val loss = if (total > 0) (failureCount * 100) / total else 0

                    if (avg != null) {
                        routerHop = HopLatencyMetric(
                            type = HopType.ROUTER_WAN,
                            targetHost = targetHost,
                            avgMs = avg,
                            minMs = min ?: avg,
                            maxMs = max ?: avg,
                            packetLossPercent = loss,
                            jitterMs = 0.0,
                            isSuccess = true,
                        )
                    }
                    break
                } else if (diagState?.contains("Error", ignoreCase = true) == true) {
                    break
                }
            }
        }

        // Graceful fallback if router OID ping is unexposed or timed out
        if (routerHop == null) {
            if (mobileInternetHop.isSuccess && wifiHop.isSuccess &&
                mobileInternetHop.avgMs != null && wifiHop.avgMs != null
            ) {
                val netAvg = mobileInternetHop.avgMs
                val wifiAvg = wifiHop.avgMs
                val estimatedAvg = max(1.0, netAvg - wifiAvg)
                val netMin = mobileInternetHop.minMs ?: netAvg
                val wifiMin = wifiHop.minMs ?: wifiAvg
                val estimatedMin = max(1.0, netMin - wifiMin)
                val netMax = mobileInternetHop.maxMs ?: netAvg
                val wifiMax = wifiHop.maxMs ?: wifiAvg
                val estimatedMax = max(1.0, netMax - wifiMax)

                routerHop = HopLatencyMetric(
                    type = HopType.ROUTER_WAN,
                    targetHost = targetHost,
                    avgMs = estimatedAvg,
                    minMs = estimatedMin,
                    maxMs = estimatedMax,
                    packetLossPercent = mobileInternetHop.packetLossPercent,
                    jitterMs = 0.0,
                    isSuccess = true,
                )
                isEstimated = true
            } else {
                routerHop = HopLatencyMetric(
                    type = HopType.ROUTER_WAN,
                    targetHost = targetHost,
                    isSuccess = false,
                    error = "Router hardware ping unavailable and mobile probe failed",
                )
            }
        }

        // 4. Evaluate Verdict & Root Cause
        state = state.copy(
            stage = DualDiagnosticStage.EVALUATING,
            routerWanHop = routerHop,
            isRouterPingEstimated = isEstimated,
        )
        emit(state)

        val verdict = DualDiagnosticVerdict.evaluate(wifiHop, routerHop)
        val (title, description, advice) = when (verdict) {
            DualDiagnosticVerdict.WIFI_BOTTLENECK -> Triple(
                "Local Wi-Fi Bottleneck",
                "El problema de velocidad está en tu señal Wi-Fi dentro de casa, no en la fibra de DIGI.",
                "Conéctate a la banda Wi-Fi de 5 GHz, acércate al router o reduce interferencias.",
            )
            DualDiagnosticVerdict.FIBER_BOTTLENECK -> Triple(
                "Carrier Fiber Bottleneck",
                "El problema está en la red exterior de fibra, tu conexión Wi-Fi es óptima.",
                "La red de fibra presenta latencia elevada o pérdida de paquetes externa. Contacta con DIGI si persiste.",
            )
            DualDiagnosticVerdict.ALL_OPTIMAL -> Triple(
                "Connection Optimal",
                "Tanto la fibra de DIGI como tu red Wi-Fi funcionan a pleno rendimiento.",
                "Tu latencia es excelente tanto en la red local como en la salida a Internet.",
            )
            DualDiagnosticVerdict.BOTH_DEGRADED -> Triple(
                "Degraded Performance",
                "Se detecta degradación tanto en tu red Wi-Fi como en la red exterior.",
                "Reinicia el router y comprueba si hay descargas intensivas en curso en otros dispositivos.",
            )
            DualDiagnosticVerdict.INCONCLUSIVE -> Triple(
                "Diagnostic Inconclusive",
                "No se han podido obtener métricas suficientes para aislar el problema.",
                "Comprueba que el dispositivo esté conectado a la red Wi-Fi del router y repite la prueba.",
            )
        }

        state = state.copy(
            isRunning = false,
            stage = if (verdict != DualDiagnosticVerdict.INCONCLUSIVE) DualDiagnosticStage.COMPLETED else DualDiagnosticStage.FAILED,
            verdict = verdict,
            summaryTitle = title,
            summaryDescription = description,
            recommendedAction = advice,
        )
        emit(state)
    }
}
