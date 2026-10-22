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
    val gponAuth: GponAuthInfo? = null,
    val activeWan: Map<String, String>? = null,
    val isSuccess: Boolean = false,
    val failureStep: DiagnosticStepId? = null,
    val summaryMessage: String? = null,
) {
    val hasWarnings: Boolean get() = steps.any { it.status == DiagnosticStepStatus.WARNING }
}

/**
 * Supported OLT vendor interoperability modes configured in the firmware.
 */
enum class OltMode(val code: Int, val displayName: String, val vendorId: String) {
    NORMAL(0, "Normal Mode", "TPLG"),
    MODE_H(1, "Mode H", "TPLG"),
    MODE_Z(2, "Mode Z", "TPLG"),
    MODE_F(3, "Mode F", "TPLG"),
    MODE_X(4, "Mode X", "TPLG"),
    MODE_HH(5, "Mode HH", "HWTC");

    companion object {
        fun fromCode(code: Int): OltMode = entries.firstOrNull { it.code == code } ?: NORMAL
    }
}

/**
 * GPON ONT registration parameters retrieved from `DEV2_GPON_AUTH`.
 */
data class GponAuthInfo(
    val rawSn: String = "",
    val snType: Int = 0,
    val snAscii: String = "",
    val snHex: String = "",
    val gponPwd: String = "",
    val pwdType: Int = 0,
    val oltMode: OltMode = OltMode.NORMAL,
) {
    val isPopulated: Boolean get() = rawSn.isNotBlank() || gponPwd.isNotBlank()

    companion object {
        fun fromMap(data: Map<String, String>): GponAuthInfo {
            val sn = data["gponSn"].orEmpty()
            val snType = data["snType"]?.toIntOrNull() ?: 0
            val normalized = GponHexUtil.normalize(sn)
            val ascii = normalized?.first ?: sn
            val hex = normalized?.second ?: (GponHexUtil.asciiToHex(sn) ?: sn)
            val pwd = data["gponPwd"].orEmpty()
            val pwdType = data["pwdType"]?.toIntOrNull() ?: 0
            val mode = OltMode.fromCode(data["oltMode"]?.toIntOrNull() ?: 0)
            return GponAuthInfo(
                rawSn = sn,
                snType = snType,
                snAscii = ascii,
                snHex = hex,
                gponPwd = pwd,
                pwdType = pwdType,
                oltMode = mode,
            )
        }
    }
}

/**
 * Stages in the on-demand WAN session recycling lifecycle.
 */
enum class WanRenewStage {
    IDLE,
    DISCONNECTING,
    RECONNECTING,
    VERIFYING,
    SUCCESS,
    ERROR,
}

/**
 * Observable progress and result state for WAN IP renewal / reconnection.
 */
data class WanRenewProgress(
    val stage: WanRenewStage = WanRenewStage.IDLE,
    val oldIp: String? = null,
    val newIp: String? = null,
    val elapsedSeconds: Float = 0f,
    val errorMessage: String? = null,
) {
    val isInProgress: Boolean get() = stage in setOf(
        WanRenewStage.DISCONNECTING,
        WanRenewStage.RECONNECTING,
        WanRenewStage.VERIFYING,
    )
}

/**
 * Stages in the on-demand OMCI stack restart lifecycle.
 */
enum class OmciRestartStage {
    IDLE,
    RESTARTING,
    AWAITING_O5,
    SUCCESS,
    ERROR,
}

/**
 * Observable progress and result state for OMCI stack restart.
 */
data class OmciRestartProgress(
    val stage: OmciRestartStage = OmciRestartStage.IDLE,
    val statusText: String? = null,
    val errorMessage: String? = null,
) {
    val isInProgress: Boolean get() = stage in setOf(
        OmciRestartStage.RESTARTING,
        OmciRestartStage.AWAITING_O5,
    )
}

