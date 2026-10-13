package es.routerapp.protocol.dualdiag

/**
 * Identifies the network hop tested in the dual diagnostic run.
 */
enum class HopType {
    /** Mobile client to local router gateway (Wi-Fi hop). */
    WIFI_LAN,

    /** Router hardware to Cloudflare (DIGI Fiber hop via DEV2_DIAG_IPPING or fallback). */
    ROUTER_WAN,

    /** Mobile client to Cloudflare (end-to-end total path). */
    MOBILE_INTERNET,
}

/**
 * Metric summary for a specific network hop latency probe.
 */
data class HopLatencyMetric(
    val type: HopType,
    val targetHost: String,
    val avgMs: Double? = null,
    val minMs: Double? = null,
    val maxMs: Double? = null,
    val packetLossPercent: Int = 0,
    val jitterMs: Double? = null,
    val isSuccess: Boolean = true,
    val error: String? = null,
)

/**
 * Root-cause verdict determined by comparing Wi-Fi hop vs Fiber WAN hop.
 */
enum class DualDiagnosticVerdict {
    /**
     * Router WAN latency is optimal with zero packet loss, but Mobile Wi-Fi is elevated.
     * Bottleneck is in the home wireless network.
     */
    WIFI_BOTTLENECK,

    /**
     * Mobile Wi-Fi hop is optimal, but Router WAN latency is elevated or dropping packets.
     * Bottleneck is in the external fiber or carrier routing.
     */
    FIBER_BOTTLENECK,

    /**
     * Both Wi-Fi and Fiber links are operating with low latency and zero packet loss.
     */
    ALL_OPTIMAL,

    /**
     * Both wireless and fiber links show elevated latency or packet loss.
     */
    BOTH_DEGRADED,

    /**
     * One or more probes failed completely or results are inconclusive.
     */
    INCONCLUSIVE;

    companion object {
        /**
         * Evaluates metrics from Wi-Fi LAN hop and Router WAN hop to reach a root-cause verdict.
         */
        fun evaluate(
            wifiHop: HopLatencyMetric?,
            routerWanHop: HopLatencyMetric?,
        ): DualDiagnosticVerdict {
            if (wifiHop == null || routerWanHop == null) return INCONCLUSIVE
            if (!wifiHop.isSuccess || !routerWanHop.isSuccess) return INCONCLUSIVE

            val wifiAvg = wifiHop.avgMs ?: return INCONCLUSIVE
            val routerAvg = routerWanHop.avgMs ?: return INCONCLUSIVE
            val wifiJitter = wifiHop.jitterMs ?: 0.0
            val routerLoss = routerWanHop.packetLossPercent

            val isWifiHigh = wifiAvg > 25.0 || wifiJitter > 15.0
            val isWifiOptimal = wifiAvg <= 12.0 && wifiJitter <= 8.0
            val isRouterHigh = routerAvg > 45.0 || routerLoss > 0
            val isRouterOptimal = routerAvg <= 25.0 && routerLoss == 0

            return when {
                // Both degraded
                isWifiHigh && isRouterHigh -> BOTH_DEGRADED

                // Clear Wi-Fi bottleneck: Router WAN is fast & clean, Wi-Fi is slow or jittery
                isWifiHigh && routerAvg <= 25.0 && routerLoss == 0 -> WIFI_BOTTLENECK

                // Clear Fiber bottleneck: Wi-Fi is fast & clean, Router WAN is slow or dropping
                isRouterHigh && wifiAvg <= 15.0 -> FIBER_BOTTLENECK

                // Both optimal
                isWifiOptimal && isRouterOptimal -> ALL_OPTIMAL

                // Intermediate heuristics
                wifiAvg > routerAvg + 15.0 -> WIFI_BOTTLENECK
                routerAvg > wifiAvg + 30.0 || routerLoss > 0 -> FIBER_BOTTLENECK
                wifiAvg <= 18.0 && routerAvg <= 32.0 && routerLoss == 0 -> ALL_OPTIMAL
                else -> BOTH_DEGRADED
            }
        }
    }
}

/**
 * Execution stages of a dual diagnostic run.
 */
enum class DualDiagnosticStage {
    IDLE,
    PROBING_WIFI,
    PROBING_ROUTER_WAN,
    PROBING_MOBILE_INTERNET,
    EVALUATING,
    COMPLETED,
    FAILED,
}

/**
 * Full state snapshot of an active or completed dual diagnostic run.
 */
data class DualDiagnosticState(
    val isRunning: Boolean = false,
    val stage: DualDiagnosticStage = DualDiagnosticStage.IDLE,
    val wifiHop: HopLatencyMetric? = null,
    val routerWanHop: HopLatencyMetric? = null,
    val mobileInternetHop: HopLatencyMetric? = null,
    val verdict: DualDiagnosticVerdict? = null,
    val isRouterPingEstimated: Boolean = false,
    val summaryTitle: String? = null,
    val summaryDescription: String? = null,
    val recommendedAction: String? = null,
    val errorMessage: String? = null,
)
