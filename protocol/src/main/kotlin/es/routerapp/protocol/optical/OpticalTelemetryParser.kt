package es.routerapp.protocol.optical

import kotlin.math.abs

/**
 * Robust parser and normalizer for GPON and XGS-PON optical transceiver metrics.
 */
object OpticalTelemetryParser {

    private val RX_KEYS = listOf(
        "rxPower", "rxOptPower", "rx_power", "opticalRxPower", "rxOpticalPower",
        "opticalSignalLevel", "OpticalSignalLevel", "rxSignalPower", "rxPowerDbm",
        "rxDbm", "rx_dbm", "rxPwr", "receivePower", "receive_power", "opticalPower", "rx",
    )
    private val TX_KEYS = listOf(
        "txPower", "txOptPower", "tx_power", "opticalTxPower", "txOpticalPower",
        "transmitOpticalLevel", "TransmitOpticalLevel", "txSignalPower", "txPowerDbm",
        "txDbm", "tx_dbm", "txPwr", "transmitPower", "transmit_power", "laserTxPower", "tx",
    )
    private val TEMP_KEYS = listOf(
        "temperature", "optTemp", "temp", "opticalTemp", "moduleTemp", "transceiverTemp", "laserTemp",
    )
    private val VOLT_KEYS = listOf(
        "voltage", "optVolt", "volt", "opticalVolt", "supplyVoltage", "vcc",
    )
    private val CURRENT_KEYS = listOf(
        "biasCurrent", "bias_current", "optCurrent", "current", "laserBiasCurrent", "bias",
    )

    /**
     * Parses optical configuration and stats maps into standardized [OpticalTelemetry].
     *
     * @param opticalValues Map of optical transceiver values (e.g. from `DEV2_OPTC_GPON_CFG` or `DEV2_GPON_INTF_STATS`).
     * @param wanValues Map of active WAN connection (e.g. from `DEV2_ADT_WAN`) to detect Ethernet WAN setups.
     */
    fun parse(
        opticalValues: Map<String, String>?,
        wanValues: Map<String, String>? = null,
    ): OpticalTelemetry {
        val raw = opticalValues.orEmpty()
        val isEthernetWan = isEthernetWanConnection(wanValues, raw)

        val rxRaw = findValue(raw, RX_KEYS)
        val txRaw = findValue(raw, TX_KEYS)
        val tempRaw = findValue(raw, TEMP_KEYS)
        val voltRaw = findValue(raw, VOLT_KEYS)
        val currentRaw = findValue(raw, CURRENT_KEYS)

        val rawRxDbm = parseDbm(rxRaw)
        // Fiber GPON reception power is always negative. Values around 0 dBm or non-negative
        // represent uninitialized or unmeasured metrics from router firmware.
        val rxDbm = rawRxDbm?.takeIf { it < -0.05f }
        val txDbm = parseDbm(txRaw)
        val tempC = parseTemperature(tempRaw)
        val voltV = parseVoltage(voltRaw)
        val currentMa = parseCurrent(currentRaw)

        val status = evaluateStatus(rxDbm, isEthernetWan, raw)

        return OpticalTelemetry(
            rxPowerDbm = rxDbm,
            txPowerDbm = txDbm,
            temperatureC = tempC,
            voltageV = voltV,
            biasCurrentMa = currentMa,
            status = status,
            rawValues = raw,
        )
    }

    fun parseDbm(raw: String?): Float? {
        if (raw.isNullOrBlank()) return null
        val cleaned = raw
            .replace("dBm", "", ignoreCase = true)
            .replace("dbm", "", ignoreCase = true)
            .replace("uW", "", ignoreCase = true)
            .trim()
        val value = cleaned.toFloatOrNull() ?: return null

        // Normalization:
        // - Standard floats (e.g. -21.45) have |v| < 100
        // - Hundredths of dBm (e.g. -2145 -> -21.45 dBm) have 100 <= |v| < 10000
        // - Thousandths of dBm (e.g. -28900 -> -28.9 dBm) have |v| >= 10000
        return when {
            abs(value) >= 10000f -> value / 1000f
            abs(value) >= 100f -> value / 100f
            else -> value
        }
    }

    fun parseTemperature(raw: String?): Float? {
        if (raw.isNullOrBlank()) return null
        val cleaned = raw.replace("C", "", ignoreCase = true).replace("°", "").trim()
        val value = cleaned.toFloatOrNull() ?: return null
        return when {
            abs(value) >= 10000f -> value / 1000f
            abs(value) >= 200f -> value / 100f
            else -> value
        }
    }

    fun parseVoltage(raw: String?): Float? {
        if (raw.isNullOrBlank()) return null
        val cleaned = raw.replace("V", "", ignoreCase = true).replace("mV", "", ignoreCase = true).trim()
        val value = cleaned.toFloatOrNull() ?: return null
        return when {
            abs(value) >= 100f -> value / 1000f
            else -> value
        }
    }

    fun parseCurrent(raw: String?): Float? {
        if (raw.isNullOrBlank()) return null
        val cleaned = raw.replace("mA", "", ignoreCase = true).replace("uA", "", ignoreCase = true).trim()
        val value = cleaned.toFloatOrNull() ?: return null
        return when {
            abs(value) >= 1000f -> value / 1000f
            else -> value
        }
    }

    private fun evaluateStatus(
        rxDbm: Float?,
        isEthernetWan: Boolean,
        raw: Map<String, String>,
    ): OpticalHealthStatus {
        if (isEthernetWan && (rxDbm == null || rxDbm >= -0.05f || raw.isEmpty())) {
            return OpticalHealthStatus.EXTERNAL_ONT
        }

        if (rxDbm == null || rxDbm >= -0.05f) {
            return if (isEthernetWan) OpticalHealthStatus.EXTERNAL_ONT else OpticalHealthStatus.UNKNOWN
        }

        return when {
            rxDbm > -8.0f -> OpticalHealthStatus.SATURATED
            rxDbm >= -25.0f -> OpticalHealthStatus.OPTIMAL
            rxDbm >= -28.0f -> OpticalHealthStatus.MARGINAL
            else -> OpticalHealthStatus.CRITICAL
        }
    }

    private fun isEthernetWanConnection(
        wanValues: Map<String, String>?,
        rawOptical: Map<String, String>,
    ): Boolean {
        if (rawOptical.isEmpty()) {
            val type = wanValues?.get("type") ?: wanValues?.get("name") ?: ""
            if (type.contains("ETH", ignoreCase = true) || type.contains("Ethernet", ignoreCase = true)) {
                return true
            }
        }
        val wanName = wanValues?.get("name").orEmpty()
        val wanType = wanValues?.get("type").orEmpty()
        val ifType = wanValues?.get("X_TP_IfType").orEmpty()
        return (wanName.contains("ETH", ignoreCase = true) ||
            wanType.contains("Ethernet", ignoreCase = true) ||
            ifType.contains("ETH", ignoreCase = true)) &&
            (rawOptical.isEmpty() || rawOptical.values.all { it.isBlank() || it == "0" })
    }

    private fun findValue(map: Map<String, String>, candidateKeys: List<String>): String? {
        for (key in candidateKeys) {
            val direct = map[key]
            if (!direct.isNullOrBlank()) return direct
        }
        for ((k, v) in map) {
            if (v.isBlank()) continue
            for (candidate in candidateKeys) {
                if (k.equals(candidate, ignoreCase = true)) return v
            }
        }
        return null
    }
}
