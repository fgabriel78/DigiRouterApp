package es.routerapp.protocol.optical

/**
 * Optical signal classification for GPON and XGS-PON fiber connections.
 */
enum class OpticalHealthStatus {
    /** Optimal reception power (-15.0 to -25.0 dBm). Safe margin, zero packet drop. */
    OPTIMAL,

    /** Marginal attenuation (-25.1 to -27.9 dBm). Elevated insertion loss or dirty connector. */
    MARGINAL,

    /** Critical attenuation (< -28.0 dBm). Imminent disconnect or severe fiber bend. */
    CRITICAL,

    /** Optical receiver saturation (> -8.0 dBm). Excess light power. */
    SATURATED,

    /** Router operating via external ONT connected over Ethernet WAN port. */
    EXTERNAL_ONT,

    /** Telemetry absent, unparsed, or link down without optical data. */
    UNKNOWN,
}

/**
 * Normalized optical transceiver metrics extracted from the router.
 */
data class OpticalTelemetry(
    val rxPowerDbm: Float? = null,
    val txPowerDbm: Float? = null,
    val temperatureC: Float? = null,
    val voltageV: Float? = null,
    val biasCurrentMa: Float? = null,
    val status: OpticalHealthStatus = OpticalHealthStatus.UNKNOWN,
    val rawValues: Map<String, String> = emptyMap(),
) {
    val isExternalOnt: Boolean get() = status == OpticalHealthStatus.EXTERNAL_ONT
    val hasValidSignal: Boolean get() = rxPowerDbm != null && status in setOf(
        OpticalHealthStatus.OPTIMAL,
        OpticalHealthStatus.MARGINAL,
        OpticalHealthStatus.CRITICAL,
        OpticalHealthStatus.SATURATED,
    )
}

/**
 * Diagnostic step identifiers executed sequentially during the end-to-end connectivity test.
 */
enum class DiagnosticStepId {
    OPTICAL_LINK,
    OMCI_REGISTRATION,
    WAN_SESSION,
    GATEWAY_PING,
    DNS_INTERNET,
}

/**
 * Lifecycle and execution status for an individual diagnostic step.
 */
enum class DiagnosticStepStatus {
    PENDING,
    RUNNING,
    SUCCESS,
    WARNING,
    FAILED,
    SKIPPED,
}

/**
 * State and results for an individual diagnostic step.
 */
data class DiagnosticStep(
    val id: DiagnosticStepId,
    val status: DiagnosticStepStatus = DiagnosticStepStatus.PENDING,
    val metricSummary: String? = null,
    val detailMessage: String? = null,
    val latencyMs: Long? = null,
)

/**
 * Full state snapshot of an active or completed diagnostic run.
 */
data class DiagnosticSnapshot(
    val isRunning: Boolean = false,
    val steps: List<DiagnosticStep> = DiagnosticStepId.entries.map { DiagnosticStep(it) },
    val opticalTelemetry: OpticalTelemetry? = null,
    val isSuccess: Boolean = false,
    val failureStep: DiagnosticStepId? = null,
    val summaryMessage: String? = null,
) {
    val hasWarnings: Boolean get() = steps.any { it.status == DiagnosticStepStatus.WARNING }
}
