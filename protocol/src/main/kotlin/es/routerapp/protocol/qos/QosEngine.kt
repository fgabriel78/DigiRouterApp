package es.routerapp.protocol.qos

import es.routerapp.protocol.RouterClient
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Subset of [RouterClient] used by [QosEngine]; replaceable by a fake in tests. */
interface QosRouterRpc {
    fun get(oid: String, attrs: Map<String, String> = emptyMap(), stack: String = "0,0,0,0,0,0"): JsonElement
    fun getList(oid: String, attrs: Map<String, String> = emptyMap(), stack: String = "0,0,0,0,0,0"): JsonElement
    fun set(oid: String, attrs: Map<String, String>, stack: String = "0,0,0,0,0,0"): JsonElement
    fun add(oid: String, attrs: Map<String, String>, parentStack: String = "0,0,0,0,0,0"): JsonElement
    fun delete(oid: String, stack: String = "0,0,0,0,0,0"): JsonElement
    fun operate(oid: String, attrs: Map<String, String> = emptyMap(), stack: String = "0,0,0,0,0,0"): JsonElement
}

/**
 * Intelligent QoS (`iqosSettings.htm`): global settings in `DEV2_IQOS`, per-device priority rules in
 * `DEV2_IQOS_RULE` and hardware Game Port clients in `DEV2_GAME_QOS_CLIENT`.
 *
 * Rules are never modified in place: like the web UI, a priority change deletes the old rule and adds a
 * new one, and a device without a rule is implicitly Middle priority.
 */
class QosEngine(private val rpc: QosRouterRpc) {

    constructor(client: RouterClient) : this(object : QosRouterRpc {
        override fun get(oid: String, attrs: Map<String, String>, stack: String) = client.get(oid, attrs, stack)
        override fun getList(oid: String, attrs: Map<String, String>, stack: String) = client.getList(oid, attrs, stack)
        override fun set(oid: String, attrs: Map<String, String>, stack: String) = client.set(oid, attrs, stack)
        override fun add(oid: String, attrs: Map<String, String>, parentStack: String) = client.add(oid, attrs, parentStack)
        override fun delete(oid: String, stack: String) = client.delete(oid, stack)
        override fun operate(oid: String, attrs: Map<String, String>, stack: String) = client.operate(oid, attrs, stack)
    })

    fun loadConfig(): Result<QosConfig> = runCatching { parseConfig(toObject(rpc.get(OID_IQOS))) }

    /**
     * Validates and writes [config]. The current object is re-read so companion settings the QoS screen does
     * not edit (IPTV / VoIP QoS) are sent back exactly as the web UI does; Guest QoS attributes are not sent and
     * therefore stay untouched.
     */
    fun saveConfig(config: QosConfig): Result<Unit> = runCatching {
        val errors = config.validate()
        if (errors.isNotEmpty()) throw QosValidationException(errors)
        val current = toObject(rpc.get(OID_IQOS))
        rpc.set(OID_IQOS, buildConfigAttributes(config, current))
        rpc.operate(ACT_SAVE_CFG)
    }

    fun loadRules(): Result<List<QosRule>> = runCatching {
        toList(rpc.getList(OID_RULE)).map { o ->
            val typeCode = o.int("type") ?: QosRuleType.UNKNOWN.code
            QosRule(
                stack = o.str("stack"),
                type = QosRuleType.fromCode(typeCode),
                typeCode = typeCode,
                priority = QosPriority.fromCode(o.int("class_")),
                macAddress = o.str("macAddress"),
                deviceName = o.str("deviceName"),
                srcPort = o.str("srcPort"),
                endPort = o.str("endPort"),
            )
        }
    }

    /**
     * Active clients on the hardware Game Port. Firmware without the gaming console feature rejects the
     * object; that case (and any other failure) yields an empty list.
     */
    fun loadGameClients(): List<GameQosClient> = runCatching {
        toList(rpc.getList(OID_GAME_CLIENT, GAME_CLIENT_ATTRS)).filter { o ->
            val agent = o.str("isAgentDevice").ifEmpty { o.str("IsAgentDevice") }
            o.str("gamePortClient") == "1" && o.str("active") == "1" && agent != "1"
        }.map { o -> GameQosClient(o.str("hostName"), o.str("physAddress"), o.str("IPAddress")) }
    }.getOrDefault(emptyList())

    /**
     * Moves a device to [target]. [existing] are the device rules currently bound to its MAC (normally zero or one).
     * Middle removes the rules (default priority); High/Low replace them with a single new rule.
     * Returns `false` when nothing had to change.
     */
    fun setDevicePriority(mac: String, deviceName: String, target: QosPriority, existing: List<QosRule>): Result<Boolean> = runCatching {
        val normalized = normalizeQosMac(mac)
        require(normalized.isNotEmpty()) { "MAC address is required" }
        val bound = existing.filter { it.isDevice && it.normalizedMac == normalized }
        val unchanged = when {
            bound.isEmpty() -> target == QosPriority.MIDDLE
            else -> bound.size == 1 && bound.single().priority == target
        }
        if (unchanged) return@runCatching false

        bound.forEach { rpc.delete(OID_RULE, it.stack) }
        if (target != QosPriority.MIDDLE) {
            rpc.add(
                OID_RULE,
                mapOf(
                    "deviceName" to deviceName.ifBlank { normalized },
                    "macAddress" to normalized,
                    "class_" to target.code.toString(),
                    "type" to QosRuleType.DEVICE.code.toString(),
                ),
            )
        }
        rpc.operate(ACT_SAVE_CFG)
        true
    }

    fun deleteRule(stack: String): Result<Unit> = runCatching {
        require(stack.isNotBlank()) { "Rule stack is required" }
        rpc.delete(OID_RULE, stack)
        rpc.operate(ACT_SAVE_CFG)
    }

    companion object {
        const val OID_IQOS = "DEV2_IQOS"
        const val OID_RULE = "DEV2_IQOS_RULE"
        const val OID_GAME_CLIENT = "DEV2_GAME_QOS_CLIENT"
        const val ACT_SAVE_CFG = "ACT_SAVE_CFG"

        private val GAME_CLIENT_ATTRS = mapOf(
            "hostName" to "", "physAddress" to "", "IPAddress" to "",
            "active" to "", "IsAgentDevice" to "", "gamePortClient" to "",
        )

        fun parseConfig(o: JsonObject): QosConfig = QosConfig(
            enabled = o.str("enable") == "1",
            upBandwidth = o.int("upTotalBW") ?: 0,
            upUnit = BandwidthUnit.fromCode(o.int("upUnit")),
            downBandwidth = o.int("downTotalBW") ?: 0,
            downUnit = BandwidthUnit.fromCode(o.int("downUnit")),
            queues = QueueAllocation(o.int("high") ?: 0, o.int("middle") ?: 0, o.int("low") ?: 0),
            raw = o.mapValues { (_, v) -> (v as? JsonPrimitive)?.content.orEmpty() },
        )

        /** Attributes written to `DEV2_IQOS`, mirroring `doSave()` of the web UI. */
        fun buildConfigAttributes(config: QosConfig, current: JsonObject): Map<String, String> {
            val attrs = linkedMapOf(
                "enable" to if (config.enabled) "1" else "0",
                "linkType" to current.str("linkType").ifEmpty { "0" },
                "upTotalBW" to config.upBandwidth.toString(),
                "upUnit" to config.upUnit.code.toString(),
                "downTotalBW" to config.downBandwidth.toString(),
                "downUnit" to config.downUnit.code.toString(),
                "high" to config.queues.high.toString(),
                "middle" to config.queues.middle.toString(),
                "low" to config.queues.low.toString(),
            )
            // IPTV / VoIP QoS: re-send what the router already has, with the same conditions as the web UI.
            if (current.containsKey("iptvEnable")) {
                attrs["iptvEnable"] = current.str("iptvEnable")
                if (current.str("iptvEnable") == "1") {
                    for (k in listOf("iptvClass", "iptvUnit", "iptvBW")) if (current.containsKey(k)) attrs[k] = current.str(k)
                }
            }
            if (current.containsKey("voIPEnable")) {
                attrs["voIPEnable"] = current.str("voIPEnable")
                if (current.str("voIPEnable") == "1" && current.containsKey("voIPClass")) attrs["voIPClass"] = current.str("voIPClass")
            }
            return attrs
        }

        private fun JsonObject.str(key: String): String = (this[key] as? JsonPrimitive)?.content.orEmpty()
        private fun JsonObject.int(key: String): Int? = str(key).trim().toIntOrNull()

        private fun toList(element: JsonElement): List<JsonObject> = when (element) {
            is JsonArray -> element.mapNotNull { it as? JsonObject }
            is JsonObject -> listOf(element)
            else -> emptyList()
        }

        private fun toObject(element: JsonElement): JsonObject = when (element) {
            is JsonObject -> element
            is JsonArray -> element.firstOrNull() as? JsonObject ?: JsonObject(emptyMap())
            else -> JsonObject(emptyMap())
        }
    }
}

/** Thrown by [QosEngine.saveConfig] when the configuration is rejected before reaching the router. */
class QosValidationException(val errors: Set<QosValidationError>) :
    IllegalArgumentException("Invalid QoS configuration: $errors")
