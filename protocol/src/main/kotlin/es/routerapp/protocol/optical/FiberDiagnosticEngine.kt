package es.routerapp.protocol.optical

import es.routerapp.protocol.RouterClient
import es.routerapp.protocol.pages.PageEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.system.measureTimeMillis

data class ProbeResult(
    val reachable: Boolean,
    val latencyMs: Long? = null,
    val error: String? = null,
)

interface NetworkProbe {
    suspend fun probeHost(host: String, port: Int = 80, timeoutMs: Int = 3000): ProbeResult
    suspend fun resolveDns(domain: String, timeoutMs: Int = 3000): Boolean
}

class DefaultNetworkProbe : NetworkProbe {
    override suspend fun probeHost(host: String, port: Int, timeoutMs: Int): ProbeResult = withContext(Dispatchers.IO) {
        try {
            val time = measureTimeMillis {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(host, port), timeoutMs)
                }
            }
            ProbeResult(reachable = true, latencyMs = time)
        } catch (e: Exception) {
            try {
                var reachable = false
                val time = measureTimeMillis {
                    reachable = InetAddress.getByName(host).isReachable(timeoutMs)
                }
                if (reachable) {
                    ProbeResult(reachable = true, latencyMs = time)
                } else {
                    ProbeResult(reachable = false, error = e.message ?: "Unreachable")
                }
            } catch (_: Exception) {
                ProbeResult(reachable = false, error = e.message ?: "Unreachable")
            }
        }
    }

    override suspend fun resolveDns(domain: String, timeoutMs: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val addresses = InetAddress.getAllByName(domain)
            addresses.isNotEmpty()
        } catch (_: Exception) {
            false
        }
    }
}

interface RouterTelemetrySource {
    suspend fun getList(oid: String): List<Map<String, String>>
    suspend fun get(oid: String): Map<String, String>
    suspend fun get(oid: String, stack: String): Map<String, String> = get(oid)
    suspend fun operate(oid: String, attrs: Map<String, String> = emptyMap(), stack: String = RouterClient.DEFAULT_STACK): Boolean = true
}

class ClientTelemetrySource(private val client: RouterClient) : RouterTelemetrySource {
    override suspend fun getList(oid: String): List<Map<String, String>> = withContext(Dispatchers.IO) {
        val listResult = runCatching {
            PageEngine.toInstances(client.getList(oid)).map { it.values }
        }.getOrNull()
        if (!listResult.isNullOrEmpty()) return@withContext listResult

        val singleResult = runCatching {
            PageEngine.toInstances(client.get(oid)).map { it.values }
        }.getOrNull()
        singleResult.orEmpty()
    }

    override suspend fun get(oid: String): Map<String, String> = withContext(Dispatchers.IO) {
        // Try getList first (most status/interface OIDs in TP-Link/DIGI are multi-instance tables)
        val fromList = runCatching {
            PageEngine.toInstances(client.getList(oid)).firstOrNull()?.values.orEmpty()
        }.getOrNull()
        if (!fromList.isNullOrEmpty()) return@withContext fromList

        // Fallback to get ("go")
        val fromGet = runCatching {
            PageEngine.toInstances(client.get(oid)).firstOrNull()?.values.orEmpty()
        }.getOrNull()
        fromGet.orEmpty()
    }

    override suspend fun get(oid: String, stack: String): Map<String, String> = withContext(Dispatchers.IO) {
        val fromGet = runCatching {
            PageEngine.toInstances(client.get(oid, stack = stack)).firstOrNull()?.values.orEmpty()
        }.getOrNull()
        if (!fromGet.isNullOrEmpty()) return@withContext fromGet
        get(oid)
    }

    override suspend fun operate(oid: String, attrs: Map<String, String>, stack: String): Boolean = withContext(Dispatchers.IO) {
        val primary = runCatching { client.operate(oid, attrs, stack) }.isSuccess
        if (primary) return@withContext true
        val altOid = if (oid.startsWith("ACT_OP_")) oid.replace("ACT_OP_", "ACT_") else oid.replace("ACT_", "ACT_OP_")
        runCatching { client.operate(altOid, attrs, stack) }.isSuccess
    }
}

/**
 * Executes the layered 5-step diagnostic sequence for DIGI fiber connections:
 * 1. Physical optical link & power margin
 * 2. OMCI link & OLT registration
 * 3. WAN session & IP address lease
 * 4. Default gateway ping & latency
 * 5. DNS resolution & internet reachability
 */
class FiberDiagnosticEngine(
    private val telemetrySource: RouterTelemetrySource,
    private val networkProbe: NetworkProbe = DefaultNetworkProbe(),
) {
    constructor(
        client: RouterClient,
        networkProbe: NetworkProbe = DefaultNetworkProbe(),
    ) : this(ClientTelemetrySource(client), networkProbe)

    fun runDiagnostics(): Flow<DiagnosticSnapshot> = flow {
        val initialSteps = DiagnosticStepId.entries.map { DiagnosticStep(it, DiagnosticStepStatus.PENDING) }
        var currentSnapshot = DiagnosticSnapshot(
            isRunning = true,
            steps = initialSteps,
            opticalTelemetry = null,
            isSuccess = false,
            failureStep = null,
            summaryMessage = null,
        )
        emit(currentSnapshot)

        fun updateStep(id: DiagnosticStepId, transform: (DiagnosticStep) -> DiagnosticStep) {
            val newSteps = currentSnapshot.steps.map { if (it.id == id) transform(it) else it }
            currentSnapshot = currentSnapshot.copy(steps = newSteps)
        }

        fun skipRemainingAfter(failedId: DiagnosticStepId) {
            var after = false
            val newSteps = currentSnapshot.steps.map { step ->
                if (after) {
                    step.copy(status = DiagnosticStepStatus.SKIPPED)
                } else if (step.id == failedId) {
                    after = true
                    step
                } else {
                    step
                }
            }
            currentSnapshot = currentSnapshot.copy(
                steps = newSteps,
                isRunning = false,
                isSuccess = false,
                failureStep = failedId,
            )
        }

        // -------------------------------------------------------------
        // Step 1: Physical Optical Link
        // -------------------------------------------------------------
        updateStep(DiagnosticStepId.OPTICAL_LINK) { it.copy(status = DiagnosticStepStatus.RUNNING) }
        emit(currentSnapshot)

        val gponCfg = telemetrySource.get("DEV2_OPTC_GPON_CFG")
        val gponStats = telemetrySource.get("DEV2_GPON_INTF_STATS")
        val optcIntf = telemetrySource.get("DEV2_OPTC_INTF")
        val rawOptical = (if (gponCfg.isNotEmpty()) gponCfg else emptyMap()) +
            (if (gponStats.isNotEmpty()) gponStats else emptyMap()) +
            (if (optcIntf.isNotEmpty()) optcIntf else emptyMap())

        val wans = telemetrySource.getList("DEV2_ADT_WAN")
        val activeWan = wans.firstOrNull { it["connStatusV4"] == "Connected" || it["connStatusV6"] == "Connected" }
            ?: wans.firstOrNull { it["name"]?.startsWith("usb_") == false }
            ?: wans.firstOrNull()

        val opticalTelemetry = OpticalTelemetryParser.parse(rawOptical, activeWan)
        currentSnapshot = currentSnapshot.copy(opticalTelemetry = opticalTelemetry)

        val isWanConnected = activeWan?.get("connStatusV4") == "Connected" ||
            activeWan?.get("connStatusV6") == "Connected" ||
            (rawOptical["bytesReceived"]?.toLongOrNull() ?: 0L) > 0L

        val opticalStep = when (opticalTelemetry.status) {
            OpticalHealthStatus.EXTERNAL_ONT -> DiagnosticStep(
                id = DiagnosticStepId.OPTICAL_LINK,
                status = DiagnosticStepStatus.SUCCESS,
                metricSummary = "External ONT",
                detailMessage = "Operating via external ONT over Ethernet WAN",
            )
            OpticalHealthStatus.OPTIMAL -> DiagnosticStep(
                id = DiagnosticStepId.OPTICAL_LINK,
                status = DiagnosticStepStatus.SUCCESS,
                metricSummary = "${opticalTelemetry.rxPowerDbm} dBm",
                detailMessage = "Optimal optical signal margin",
            )
            OpticalHealthStatus.MARGINAL -> DiagnosticStep(
                id = DiagnosticStepId.OPTICAL_LINK,
                status = DiagnosticStepStatus.WARNING,
                metricSummary = "${opticalTelemetry.rxPowerDbm} dBm",
                detailMessage = "Marginal optical power - possible connector contamination or bend",
            )
            OpticalHealthStatus.SATURATED -> DiagnosticStep(
                id = DiagnosticStepId.OPTICAL_LINK,
                status = DiagnosticStepStatus.WARNING,
                metricSummary = "${opticalTelemetry.rxPowerDbm} dBm",
                detailMessage = "Optical receiver power saturation warning",
            )
            OpticalHealthStatus.CRITICAL -> DiagnosticStep(
                id = DiagnosticStepId.OPTICAL_LINK,
                status = DiagnosticStepStatus.FAILED,
                metricSummary = "${opticalTelemetry.rxPowerDbm} dBm",
                detailMessage = "Critical optical power loss (< -28 dBm). Check fiber cable.",
            )
            OpticalHealthStatus.UNKNOWN -> {
                if (isWanConnected) {
                    DiagnosticStep(
                        id = DiagnosticStepId.OPTICAL_LINK,
                        status = DiagnosticStepStatus.WARNING,
                        metricSummary = "Active",
                        detailMessage = "Fiber link active (optical dBm power unexposed by firmware)",
                    )
                } else {
                    DiagnosticStep(
                        id = DiagnosticStepId.OPTICAL_LINK,
                        status = DiagnosticStepStatus.FAILED,
                        metricSummary = "No Signal",
                        detailMessage = "Optical transceiver signal down or disconnected",
                    )
                }
            }
        }

        updateStep(DiagnosticStepId.OPTICAL_LINK) { opticalStep }

        if (opticalStep.status == DiagnosticStepStatus.FAILED) {
            skipRemainingAfter(DiagnosticStepId.OPTICAL_LINK)
            currentSnapshot = currentSnapshot.copy(summaryMessage = opticalStep.detailMessage)
            emit(currentSnapshot)
            return@flow
        }
        emit(currentSnapshot)

        // -------------------------------------------------------------
        // Step 2: OMCI & OLT Registration
        // -------------------------------------------------------------
        updateStep(DiagnosticStepId.OMCI_REGISTRATION) { it.copy(status = DiagnosticStepStatus.RUNNING) }
        emit(currentSnapshot)

        // Read ONT authentication/registration telemetry
        val gponAuthRaw = runCatching { telemetrySource.get("DEV2_GPON_AUTH", stack = "1,0,0,0,0,0") }.getOrDefault(emptyMap())
        val gponAuth = if (gponAuthRaw.isNotEmpty()) GponAuthInfo.fromMap(gponAuthRaw) else null
        currentSnapshot = currentSnapshot.copy(gponAuth = gponAuth)

        val omciStep = if (opticalTelemetry.isExternalOnt) {
            DiagnosticStep(
                id = DiagnosticStepId.OMCI_REGISTRATION,
                status = DiagnosticStepStatus.SUCCESS,
                metricSummary = "ONT External",
                detailMessage = "OMCI registration bypassed (External ONT)",
            )
        } else {
            val omciStats = telemetrySource.get("DEV2_GPON_INTF_OMCI_STATS")
            val gponInterfaceStats = telemetrySource.get("DEV2_GPON_INTF_STATS")
            val combined = omciStats + gponInterfaceStats
            val omciStatusVal = combined["omciStatus"] ?: combined["status"] ?: combined["state"] ?: ""
            val isRegistered = when {
                omciStatusVal.equals("O5", ignoreCase = true) ||
                    omciStatusVal.contains("Associated", ignoreCase = true) ||
                    omciStatusVal.contains("Registered", ignoreCase = true) ||
                    omciStatusVal.contains("Operation", ignoreCase = true) ||
                    omciStatusVal.equals("Up", ignoreCase = true) -> true
                omciStatusVal.equals("O1", ignoreCase = true) ||
                    omciStatusVal.equals("O2", ignoreCase = true) ||
                    omciStatusVal.equals("O3", ignoreCase = true) ||
                    omciStatusVal.equals("O4", ignoreCase = true) ||
                    omciStatusVal.equals("Down", ignoreCase = true) -> false
                else -> opticalTelemetry.hasValidSignal || isWanConnected
            }

            if (isRegistered) {
                DiagnosticStep(
                    id = DiagnosticStepId.OMCI_REGISTRATION,
                    status = DiagnosticStepStatus.SUCCESS,
                    metricSummary = if (omciStatusVal.isNotBlank()) omciStatusVal else "Registered (O5)",
                    detailMessage = "OLT registration and OMCI channel established",
                )
            } else {
                DiagnosticStep(
                    id = DiagnosticStepId.OMCI_REGISTRATION,
                    status = DiagnosticStepStatus.FAILED,
                    metricSummary = if (omciStatusVal.isNotBlank()) omciStatusVal else "Unregistered",
                    detailMessage = "OMCI synchronization with OLT failed",
                )
            }
        }

        updateStep(DiagnosticStepId.OMCI_REGISTRATION) { omciStep }

        if (omciStep.status == DiagnosticStepStatus.FAILED) {
            skipRemainingAfter(DiagnosticStepId.OMCI_REGISTRATION)
            currentSnapshot = currentSnapshot.copy(summaryMessage = omciStep.detailMessage)
            emit(currentSnapshot)
            return@flow
        }
        emit(currentSnapshot)

        // -------------------------------------------------------------
        // Step 3: WAN Session & IP Lease
        // -------------------------------------------------------------
        updateStep(DiagnosticStepId.WAN_SESSION) { it.copy(status = DiagnosticStepStatus.RUNNING) }
        emit(currentSnapshot)

        val freshWans = telemetrySource.getList("DEV2_ADT_WAN")
        val currentWan = freshWans.firstOrNull { it["connStatusV4"] == "Connected" || it["connStatusV6"] == "Connected" }
            ?: freshWans.firstOrNull { it["connStatusV4"] == "Connecting" }
        currentSnapshot = currentSnapshot.copy(activeWan = currentWan)

        val wanStep = if (currentWan == null) {
            DiagnosticStep(
                id = DiagnosticStepId.WAN_SESSION,
                status = DiagnosticStepStatus.FAILED,
                metricSummary = "Disconnected",
                detailMessage = "No active WAN connection found",
            )
        } else {
            val statusV4 = currentWan["connStatusV4"].orEmpty()
            val ip = currentWan["connIPv4Address"]
                ?: currentWan["externalIPAddress"]
                ?: currentWan["ip"]
                ?: currentWan["ipAddress"]
                ?: currentWan["ipv4"]
                ?: ""
            val connType = currentWan["connType"] ?: currentWan["type"] ?: "WAN"

            if (statusV4.equals("Connected", ignoreCase = true) && ip.isNotBlank() && ip != "0.0.0.0") {
                DiagnosticStep(
                    id = DiagnosticStepId.WAN_SESSION,
                    status = DiagnosticStepStatus.SUCCESS,
                    metricSummary = "$ip ($connType)",
                    detailMessage = "Active WAN IP lease assigned",
                )
            } else {
                DiagnosticStep(
                    id = DiagnosticStepId.WAN_SESSION,
                    status = DiagnosticStepStatus.FAILED,
                    metricSummary = if (ip == "0.0.0.0" || ip.isBlank()) "No IP Lease" else statusV4,
                    detailMessage = "WAN session disconnected or no IP address leased",
                )
            }
        }

        updateStep(DiagnosticStepId.WAN_SESSION) { wanStep }

        if (wanStep.status == DiagnosticStepStatus.FAILED) {
            skipRemainingAfter(DiagnosticStepId.WAN_SESSION)
            currentSnapshot = currentSnapshot.copy(summaryMessage = wanStep.detailMessage)
            emit(currentSnapshot)
            return@flow
        }
        emit(currentSnapshot)

        // -------------------------------------------------------------
        // Step 4: Default Gateway Ping & Latency
        // -------------------------------------------------------------
        updateStep(DiagnosticStepId.GATEWAY_PING) { it.copy(status = DiagnosticStepStatus.RUNNING) }
        emit(currentSnapshot)

        val gatewayCandidate = currentWan?.get("defaultGateway")
            ?: currentWan?.get("gateway")
            ?: currentWan?.get("gatewayV4")
            ?: currentWan?.get("connGateway")
            ?: currentWan?.get("dns")?.split(",", " ")?.firstOrNull()
            ?: "1.1.1.1"

        val gwProbe = networkProbe.probeHost(gatewayCandidate, 80, 2500)
        val finalGwProbe = if (gwProbe.reachable) gwProbe else networkProbe.probeHost(gatewayCandidate, 53, 2500)

        val gatewayStep = if (finalGwProbe.reachable) {
            val lat = finalGwProbe.latencyMs ?: 5L
            DiagnosticStep(
                id = DiagnosticStepId.GATEWAY_PING,
                status = DiagnosticStepStatus.SUCCESS,
                metricSummary = "${lat} ms",
                detailMessage = "DIGI default gateway ($gatewayCandidate) reachable",
                latencyMs = lat,
            )
        } else {
            DiagnosticStep(
                id = DiagnosticStepId.GATEWAY_PING,
                status = DiagnosticStepStatus.FAILED,
                metricSummary = "Timeout",
                detailMessage = "Gateway $gatewayCandidate unreachable or packet drop detected",
            )
        }

        updateStep(DiagnosticStepId.GATEWAY_PING) { gatewayStep }

        if (gatewayStep.status == DiagnosticStepStatus.FAILED) {
            skipRemainingAfter(DiagnosticStepId.GATEWAY_PING)
            currentSnapshot = currentSnapshot.copy(summaryMessage = gatewayStep.detailMessage)
            emit(currentSnapshot)
            return@flow
        }
        emit(currentSnapshot)

        // -------------------------------------------------------------
        // Step 5: DNS & Global Internet
        // -------------------------------------------------------------
        updateStep(DiagnosticStepId.DNS_INTERNET) { it.copy(status = DiagnosticStepStatus.RUNNING) }
        emit(currentSnapshot)

        val dnsOk = networkProbe.resolveDns("google.com", 2500) ||
            networkProbe.resolveDns("one.one.one.one", 2500) ||
            networkProbe.resolveDns("digimobil.es", 2500)

        val dnsStep = if (!dnsOk) {
            DiagnosticStep(
                id = DiagnosticStepId.DNS_INTERNET,
                status = DiagnosticStepStatus.FAILED,
                metricSummary = "DNS Failed",
                detailMessage = "DNS resolution failed - DNS servers not answering",
            )
        } else {
            val internetProbe = networkProbe.probeHost("1.1.1.1", 53, 3000)
            val internetProbeAlt = if (internetProbe.reachable) internetProbe else networkProbe.probeHost("8.8.8.8", 53, 3000)
            if (internetProbeAlt.reachable) {
                val lat = internetProbeAlt.latencyMs ?: 15L
                DiagnosticStep(
                    id = DiagnosticStepId.DNS_INTERNET,
                    status = DiagnosticStepStatus.SUCCESS,
                    metricSummary = "${lat} ms",
                    detailMessage = "DNS resolution verified and global internet connected",
                    latencyMs = lat,
                )
            } else {
                DiagnosticStep(
                    id = DiagnosticStepId.DNS_INTERNET,
                    status = DiagnosticStepStatus.FAILED,
                    metricSummary = "No Route",
                    detailMessage = "DNS resolved but external hosts unreachable",
                )
            }
        }

        updateStep(DiagnosticStepId.DNS_INTERNET) { dnsStep }

        if (dnsStep.status == DiagnosticStepStatus.FAILED) {
            skipRemainingAfter(DiagnosticStepId.DNS_INTERNET)
            currentSnapshot = currentSnapshot.copy(summaryMessage = dnsStep.detailMessage)
            emit(currentSnapshot)
            return@flow
        }

        val hasWarnings = currentSnapshot.steps.any { it.status == DiagnosticStepStatus.WARNING }
        val finalSummary = if (hasWarnings) {
            val warningStep = currentSnapshot.steps.firstOrNull { it.status == DiagnosticStepStatus.WARNING }
            "Operational with advisory: ${warningStep?.detailMessage ?: "Check physical fiber parameters"}"
        } else {
            "All connectivity layers operational"
        }

        currentSnapshot = currentSnapshot.copy(
            isRunning = false,
            isSuccess = true,
            failureStep = null,
            summaryMessage = finalSummary,
        )
        emit(currentSnapshot)
    }

    /**
     * Triggers a non-destructive restart of the router's OMCI state machine via `ACT_OMCI_RESTART`,
     * and polls `DEV2_GPON_INTF_OMCI_STATS` until the registration transitions back to `O5 Associated`.
     */
    fun restartOmci(
        pollIntervalMs: Long = 1000L,
        maxAttempts: Int = 15,
    ): Flow<OmciRestartProgress> = flow {
        emit(OmciRestartProgress(stage = OmciRestartStage.RESTARTING, statusText = "Restarting OMCI stack..."))
        val success = telemetrySource.operate(ACT_OMCI_RESTART)
        if (!success) {
            emit(OmciRestartProgress(stage = OmciRestartStage.ERROR, errorMessage = "Failed to dispatch OMCI restart"))
            return@flow
        }

        emit(OmciRestartProgress(stage = OmciRestartStage.AWAITING_O5, statusText = "Awaiting OLT registration (O5)..."))
        var associated = false
        var lastStatus = ""
        for (i in 1..maxAttempts) {
            delay(pollIntervalMs)
            val omciStats = telemetrySource.get("DEV2_GPON_INTF_OMCI_STATS")
            val status = omciStats["omciStatus"] ?: omciStats["status"] ?: ""
            lastStatus = status
            if (status.contains("O5", ignoreCase = true) ||
                status.contains("Associated", ignoreCase = true) ||
                status.contains("Registered", ignoreCase = true) ||
                status.equals("Up", ignoreCase = true)
            ) {
                associated = true
                break
            }
        }

        if (associated) {
            emit(OmciRestartProgress(stage = OmciRestartStage.SUCCESS, statusText = "OMCI Associated (O5)"))
        } else {
            emit(OmciRestartProgress(stage = OmciRestartStage.ERROR, errorMessage = "OMCI registration timed out (last status: $lastStatus)"))
        }
    }

    /**
     * Executes on-demand WAN session termination and re-establishment:
     * - For PPPoE: `ACT_PPP_DISCONN` -> delay -> `ACT_PPP_CONN`
     * - For DHCP: `ACT_DHCP_RELEASE` -> delay -> `ACT_DHCP_RENEW`
     * Polls `DEV2_ADT_WAN` until connection status returns to `Connected`, confirming new IP acquisition.
     */
    fun renewWanSession(
        delayBetweenMs: Long = 1500L,
        pollIntervalMs: Long = 1000L,
        maxAttempts: Int = 12,
    ): Flow<WanRenewProgress> = flow {
        emit(WanRenewProgress(stage = WanRenewStage.DISCONNECTING))
        val startTime = System.currentTimeMillis()

        val wans = telemetrySource.getList("DEV2_ADT_WAN")
        val activeWan = wans.firstOrNull { it["connStatusV4"] == "Connected" }
            ?: wans.firstOrNull { it["name"]?.startsWith("usb_") == false }
            ?: wans.firstOrNull()

        if (activeWan == null) {
            emit(WanRenewProgress(stage = WanRenewStage.ERROR, errorMessage = "No active WAN connection found"))
            return@flow
        }

        val stack = activeWan["stack"] ?: RouterClient.DEFAULT_STACK
        val connType = activeWan["connType"].orEmpty()
        val oldIp = activeWan["externalIPAddress"]

        val disconnAct = if (connType.equals("DHCP", ignoreCase = true)) ACT_DHCP_RELEASE else ACT_PPP_DISCONN
        val connAct = if (connType.equals("DHCP", ignoreCase = true)) ACT_DHCP_RENEW else ACT_PPP_CONN

        // 1. Disconnect session
        telemetrySource.operate(disconnAct, stack = stack)
        delay(delayBetweenMs)

        // 2. Re-establish connection
        emit(WanRenewProgress(stage = WanRenewStage.RECONNECTING, oldIp = oldIp))
        telemetrySource.operate(connAct, stack = stack)

        // 3. Verify connection and capture renewed IP
        emit(WanRenewProgress(stage = WanRenewStage.VERIFYING, oldIp = oldIp))
        var reconnected = false
        var newIp: String? = null

        for (i in 1..maxAttempts) {
            delay(pollIntervalMs)
            val updatedWans = telemetrySource.getList("DEV2_ADT_WAN")
            val targetWan = updatedWans.firstOrNull { it["stack"] == stack || it["name"] == activeWan["name"] }
                ?: updatedWans.firstOrNull { it["connStatusV4"] == "Connected" }

            if (targetWan?.get("connStatusV4") == "Connected") {
                reconnected = true
                newIp = targetWan["externalIPAddress"]
                break
            }
        }

        val elapsed = (System.currentTimeMillis() - startTime) / 1000f
        if (reconnected) {
            emit(WanRenewProgress(
                stage = WanRenewStage.SUCCESS,
                oldIp = oldIp,
                newIp = newIp,
                elapsedSeconds = elapsed,
            ))
        } else {
            emit(WanRenewProgress(
                stage = WanRenewStage.ERROR,
                oldIp = oldIp,
                elapsedSeconds = elapsed,
                errorMessage = "WAN reconnection timed out",
            ))
        }
    }

    companion object {
        const val ACT_OMCI_RESTART = "ACT_OMCI_RESTART"
        const val ACT_PPP_DISCONN = "ACT_PPP_DISCONN"
        const val ACT_PPP_CONN = "ACT_PPP_CONN"
        const val ACT_DHCP_RELEASE = "ACT_DHCP_RELEASE"
        const val ACT_DHCP_RENEW = "ACT_DHCP_RENEW"
    }
}
