package es.routerapp.protocol.qos

/** Bandwidth unit codes used by `DEV2_IQOS.upUnit` / `downUnit`. */
enum class BandwidthUnit(val code: Int, val label: String) {
    MBPS(1, "Mbps"),
    KBPS(2, "Kbps");

    companion object {
        fun fromCode(code: Int?): BandwidthUnit = entries.firstOrNull { it.code == code } ?: MBPS
    }
}

/** Priority classes used by `DEV2_IQOS_RULE.class_`. Devices without a rule are treated as [MIDDLE]. */
enum class QosPriority(val code: Int) {
    HIGH(4),
    MIDDLE(3),
    LOW(2);

    companion object {
        fun fromCode(code: Int?): QosPriority = entries.firstOrNull { it.code == code } ?: MIDDLE
    }
}

/** Rule kinds used by `DEV2_IQOS_RULE.type`. */
enum class QosRuleType(val code: Int) {
    DEVICE(11),
    CUSTOM_TCP(21),
    CUSTOM_UDP(22),
    CUSTOM_ALL(23),
    SYSTEM(30),
    UNKNOWN(-1);

    companion object {
        fun fromCode(code: Int?): QosRuleType = entries.firstOrNull { it.code == code } ?: UNKNOWN
    }
}

/** Reasons a QoS configuration cannot be written to the router. */
enum class QosValidationError {
    UP_BANDWIDTH_INVALID,
    DOWN_BANDWIDTH_INVALID,
    QUEUE_ALLOCATION_INVALID,
}

/**
 * High / Middle / Low queue percentages. Mirrors the coupled slider limits of the router web UI:
 * 5% steps, `high >= 15`, `middle >= 10`, `low >= 5`, `middle <= high - 5`, `low <= middle - 5`
 * and `high + middle + low <= 100`.
 */
data class QueueAllocation(val high: Int, val middle: Int, val low: Int) {

    val total: Int get() = high + middle + low

    fun isValid(): Boolean =
        listOf(high, middle, low).all { it % STEP == 0 } &&
            high >= MIN_HIGH && middle >= MIN_MIDDLE && low >= MIN_LOW &&
            middle <= high - STEP && low <= middle - STEP &&
            total <= TOTAL

    /** Sets High (snapped and clamped) and pulls Middle and Low down as needed to keep the allocation valid. */
    fun withHigh(value: Int): QueueAllocation {
        val h = snap(value).coerceIn(MIN_HIGH, TOTAL - MIN_MIDDLE - MIN_LOW)
        val m = snap(middle).coerceIn(MIN_MIDDLE, minOf(h - STEP, TOTAL - h - MIN_LOW))
        val l = snap(low).coerceIn(MIN_LOW, minOf(m - STEP, TOTAL - h - m))
        return QueueAllocation(h, m, l)
    }

    /** Sets Middle within the room left by High and pulls Low down as needed. */
    fun withMiddle(value: Int): QueueAllocation {
        val base = normalized()
        val m = snap(value).coerceIn(MIN_MIDDLE, minOf(base.high - STEP, TOTAL - base.high - MIN_LOW))
        val l = base.low.coerceIn(MIN_LOW, minOf(m - STEP, TOTAL - base.high - m))
        return QueueAllocation(base.high, m, l)
    }

    /** Sets Low within the room left by High and Middle. */
    fun withLow(value: Int): QueueAllocation {
        val base = normalized()
        val l = snap(value).coerceIn(MIN_LOW, minOf(base.middle - STEP, TOTAL - base.high - base.middle))
        return base.copy(low = l)
    }

    /** Closest valid allocation (keeps High first, then Middle, then Low). */
    fun normalized(): QueueAllocation = withHigh(high)

    /** Largest value each slider may currently take, given the other two. */
    fun maxHigh(): Int = TOTAL - middle - low
    fun maxMiddle(): Int = minOf(high - STEP, TOTAL - high - low)
    fun maxLow(): Int = minOf(middle - STEP, TOTAL - high - middle)

    companion object {
        const val STEP = 5
        const val MIN_HIGH = 15
        const val MIN_MIDDLE = 10
        const val MIN_LOW = 5
        const val TOTAL = 100

        val DEFAULT = QueueAllocation(60, 30, 10)

        private fun snap(v: Int): Int = Math.round(v / STEP.toDouble()).toInt() * STEP
    }
}

/** Global QoS settings stored in `DEV2_IQOS`. [raw] keeps every attribute returned by the router. */
data class QosConfig(
    val enabled: Boolean,
    val upBandwidth: Int,
    val upUnit: BandwidthUnit,
    val downBandwidth: Int,
    val downUnit: BandwidthUnit,
    val queues: QueueAllocation,
    val raw: Map<String, String> = emptyMap(),
) {
    fun validate(): Set<QosValidationError> = buildSet {
        if (upBandwidth !in MIN_BANDWIDTH..MAX_BANDWIDTH) add(QosValidationError.UP_BANDWIDTH_INVALID)
        if (downBandwidth !in MIN_BANDWIDTH..MAX_BANDWIDTH) add(QosValidationError.DOWN_BANDWIDTH_INVALID)
        if (!queues.isValid()) add(QosValidationError.QUEUE_ALLOCATION_INVALID)
    }

    companion object {
        const val MIN_BANDWIDTH = 1
        const val MAX_BANDWIDTH = 10000
        /** Value proposed to the user when the router has no bandwidth configured yet (`0`). */
        const val DEFAULT_BANDWIDTH = 1000
    }
}

/** Bandwidth guaranteed to a queue holding [percent] of [total]. */
fun bandwidthShare(total: Int, percent: Int): Double = total * percent / 100.0

/** One `DEV2_IQOS_RULE` instance. */
data class QosRule(
    val stack: String,
    val type: QosRuleType,
    val typeCode: Int,
    val priority: QosPriority,
    val macAddress: String = "",
    val deviceName: String = "",
    val srcPort: String = "",
    val endPort: String = "",
) {
    val isDevice: Boolean get() = type == QosRuleType.DEVICE
    val normalizedMac: String get() = normalizeQosMac(macAddress)
}

/** An active client plugged into the router's hardware Game Port (always High priority). */
data class GameQosClient(
    val hostName: String,
    val macAddress: String,
    val ipAddress: String,
) {
    val normalizedMac: String get() = normalizeQosMac(macAddress)
}

/** Canonical MAC form used for joins: uppercase, colon separated. */
fun normalizeQosMac(mac: String): String = mac.trim().uppercase().replace('-', ':')
