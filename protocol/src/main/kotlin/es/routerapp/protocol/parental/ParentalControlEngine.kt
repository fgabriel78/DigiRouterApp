package es.routerapp.protocol.parental

import es.routerapp.protocol.RouterClient
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

interface ParentalRouterRpc {
    fun get(oid: String, attrs: Map<String, String> = emptyMap(), stack: String = "0,0,0,0,0,0"): JsonElement
    fun getList(oid: String, attrs: Map<String, String> = emptyMap(), stack: String = "0,0,0,0,0,0"): JsonElement
    fun getSubList(oid: String, attrs: Map<String, String> = emptyMap(), stack: String = "0,0,0,0,0,0"): JsonElement
    fun set(oid: String, attrs: Map<String, String>, stack: String = "0,0,0,0,0,0"): JsonElement
    fun add(oid: String, attrs: Map<String, String>, parentStack: String = "0,0,0,0,0,0"): JsonElement
    fun delete(oid: String, stack: String = "0,0,0,0,0,0"): JsonElement
    fun operate(oid: String, attrs: Map<String, String> = emptyMap(), stack: String = "0,0,0,0,0,0"): JsonElement
}

class ParentalControlEngine(private val rpc: ParentalRouterRpc) {

    constructor(client: RouterClient) : this(object : ParentalRouterRpc {
        override fun get(oid: String, attrs: Map<String, String>, stack: String): JsonElement = client.get(oid, attrs, stack)
        override fun getList(oid: String, attrs: Map<String, String>, stack: String): JsonElement = client.getList(oid, attrs, stack)
        override fun getSubList(oid: String, attrs: Map<String, String>, stack: String): JsonElement = client.getSubList(oid, attrs, stack)
        override fun set(oid: String, attrs: Map<String, String>, stack: String): JsonElement = client.set(oid, attrs, stack)
        override fun add(oid: String, attrs: Map<String, String>, parentStack: String): JsonElement = client.add(oid, attrs, parentStack)
        override fun delete(oid: String, stack: String): JsonElement = client.delete(oid, stack)
        override fun operate(oid: String, attrs: Map<String, String>, stack: String): JsonElement = client.operate(oid, attrs, stack)
    })

    fun loadProfiles(): Result<List<ParentalProfile>> = runCatching {
        val listElem = rpc.getList("DEV2_PC_PROFILE")
        val array = toList(listElem)
        array.map { obj ->
            val stack = obj["stack"]?.jsonPrimitive?.content ?: ""
            val name = obj["name"]?.jsonPrimitive?.content ?: ""
            val filterLevelCode = obj["filterLevel"]?.jsonPrimitive?.content?.toIntOrNull() ?: 1
            val statusCode = obj["status"]?.jsonPrimitive?.content?.toIntOrNull() ?: 1

            val devices = runCatching { loadProfileDevices(stack) }.getOrDefault(emptyList())
            val weekday = runCatching { loadWeekday(stack) }.getOrDefault(TimeLimitConfig(dayMask = TimeLimitConfig.WEEKDAY_DEFAULT_MASK))
            val weekend = runCatching { loadWeekend(stack) }.getOrDefault(TimeLimitConfig(dayMask = TimeLimitConfig.WEEKEND_DEFAULT_MASK))
            val filterContent = runCatching { loadFilterContent(stack) }.getOrDefault(FilterCategories.forLevel(FilterLevel.fromCode(filterLevelCode)))
            val blacklist = runCatching { loadBlacklist(stack) }.getOrDefault(emptyList())

            ParentalProfile(
                stack = stack,
                name = name,
                filterLevel = FilterLevel.fromCode(filterLevelCode),
                status = ProfileStatus.fromInt(statusCode),
                devices = devices,
                weekdayLimit = weekday,
                weekendLimit = weekend,
                filterContent = filterContent,
                blackList = blacklist,
            )
        }
    }

    private fun loadProfileDevices(pstack: String): List<ProfileDevice> {
        val res = rpc.getSubList("DEV2_PC_PROFILE_DEVICE", mapOf("pstack" to pstack))
        return toList(res).map {
            ProfileDevice(
                stack = it["stack"]?.jsonPrimitive?.content ?: "",
                deviceMac = it["deviceMac"]?.jsonPrimitive?.content ?: "",
                entryName = it["entryName"]?.jsonPrimitive?.content ?: "",
                clientType = it["clientType"]?.jsonPrimitive?.content ?: "Other",
                pstack = pstack,
            )
        }
    }

    private fun loadWeekday(stack: String): TimeLimitConfig {
        val res = rpc.get("DEV2_PC_WEEKDAY", mapOf("stack" to stack), stack = stack)
        val obj = toObject(res)
        return TimeLimitConfig(
            timeLimitEnable = obj["timeLimitEnable"]?.jsonPrimitive?.content == "1",
            timeLimitMinutes = obj["timeLimit"]?.jsonPrimitive?.content?.toIntOrNull() ?: 120,
            bedTimeEnable = obj["bedTimeEnable"]?.jsonPrimitive?.content == "1",
            bedTimeBeginMinutes = obj["bedTimeBegin"]?.jsonPrimitive?.content?.toIntOrNull() ?: 1320,
            bedTimeEndMinutes = obj["bedTimeEnd"]?.jsonPrimitive?.content?.toIntOrNull() ?: 360,
            dayMask = obj["index"]?.jsonPrimitive?.content?.toIntOrNull() ?: TimeLimitConfig.WEEKDAY_DEFAULT_MASK,
            stack = stack,
        )
    }

    private fun loadWeekend(stack: String): TimeLimitConfig {
        val res = rpc.get("DEV2_PC_WEEKEND", mapOf("stack" to stack), stack = stack)
        val obj = toObject(res)
        return TimeLimitConfig(
            timeLimitEnable = obj["timeLimitEnable"]?.jsonPrimitive?.content == "1",
            timeLimitMinutes = obj["timeLimit"]?.jsonPrimitive?.content?.toIntOrNull() ?: 120,
            bedTimeEnable = obj["bedTimeEnable"]?.jsonPrimitive?.content == "1",
            bedTimeBeginMinutes = obj["bedTimeBegin"]?.jsonPrimitive?.content?.toIntOrNull() ?: 1320,
            bedTimeEndMinutes = obj["bedTimeEnd"]?.jsonPrimitive?.content?.toIntOrNull() ?: 360,
            dayMask = obj["index"]?.jsonPrimitive?.content?.toIntOrNull() ?: TimeLimitConfig.WEEKEND_DEFAULT_MASK,
            stack = stack,
        )
    }

    private fun loadFilterContent(stack: String): FilterCategories {
        val res = rpc.get("DEV2_PC_FILTERCONTENT", mapOf("stack" to stack), stack = stack)
        val obj = toObject(res)
        return FilterCategories(
            adultContentEnable = obj["adultContentEnable"]?.jsonPrimitive?.content == "1",
            gamblingEnable = obj["gambleEnable"]?.jsonPrimitive?.content == "1",
            gamesEnable = obj["gamesEnable"]?.jsonPrimitive?.content == "1",
            mediaEnable = obj["mediaEnable"]?.jsonPrimitive?.content == "1",
            onlineCommunicationEnable = obj["onlineCommunicationEnable"]?.jsonPrimitive?.content == "1",
            payToSurfEnable = obj["payToSurfEnable"]?.jsonPrimitive?.content == "1",
            socialNetworkingEnable = obj["socialNetworkingEnable"]?.jsonPrimitive?.content == "1",
            downLoadEnable = obj["downLoadEnable"]?.jsonPrimitive?.content == "1",
            sexEducationEnable = obj["sexEducationEnable"]?.jsonPrimitive?.content == "1",
            stack = stack,
        )
    }

    private fun loadBlacklist(pstack: String): List<BlacklistEntry> {
        val res = rpc.getSubList("DEV2_PC_BLACKLIST", mapOf("pstack" to pstack))
        return toList(res).map {
            BlacklistEntry(
                stack = it["stack"]?.jsonPrimitive?.content ?: "",
                url = it["url"]?.jsonPrimitive?.content ?: "",
                pstack = pstack,
            )
        }
    }

    fun togglePause(profile: ParentalProfile): Result<ProfileStatus> = runCatching {
        val newStatus = if (profile.status == ProfileStatus.ACTIVE) ProfileStatus.PAUSED else ProfileStatus.ACTIVE
        rpc.set(
            "DEV2_PC_PROFILE",
            mapOf("status" to newStatus.value.toString(), "stack" to profile.stack),
            stack = profile.stack,
        )
        rpc.operate("ACT_SAVE_CFG")
        newStatus
    }

    fun saveProfile(profile: ParentalProfile, isNew: Boolean): Result<ParentalProfile> = runCatching {
        val cleanName = profile.name.trim()
        require(cleanName.isNotBlank()) { "Profile name cannot be blank" }
        require(!cleanName.contains(Regex("""[\\/":*?<>|&]"""))) {
            "Profile name contains invalid characters: \\ / \" : * ? < > | &"
        }
        val safeProfile = profile.copy(name = cleanName)

        val stack = if (isNew) {
            // Strategy 1: Add with (name, filterLevel, status) as in web UI
            val addResult = runCatching {
                rpc.add(
                    "DEV2_PC_PROFILE",
                    mapOf(
                        "name" to cleanName,
                        "filterLevel" to safeProfile.filterLevel.code.toString(),
                        "status" to safeProfile.status.value.toString(),
                    ),
                )
            }.recoverCatching { err ->
                // Strategy 2: If router rejects compound attributes during ao (e.g. error 4734),
                // add with just "name", then configure filterLevel and status via so
                val res = rpc.add("DEV2_PC_PROFILE", mapOf("name" to cleanName))
                val s = toObject(res)["stack"]?.jsonPrimitive?.content
                    ?: throw err
                rpc.set(
                    "DEV2_PC_PROFILE",
                    mapOf(
                        "filterLevel" to safeProfile.filterLevel.code.toString(),
                        "status" to safeProfile.status.value.toString(),
                        "stack" to s,
                    ),
                    stack = s,
                )
                res
            }.recoverCatching { err ->
                // Strategy 3: TR-181 AddObject with empty map, followed by so
                val res = rpc.add("DEV2_PC_PROFILE", emptyMap())
                val s = toObject(res)["stack"]?.jsonPrimitive?.content
                    ?: throw err
                rpc.set(
                    "DEV2_PC_PROFILE",
                    mapOf(
                        "name" to cleanName,
                        "filterLevel" to safeProfile.filterLevel.code.toString(),
                        "status" to safeProfile.status.value.toString(),
                        "stack" to s,
                    ),
                    stack = s,
                )
                res
            }.getOrThrow()

            toObject(addResult)["stack"]?.jsonPrimitive?.content
                ?: throw IllegalStateException("Router did not return stack for new profile")
        } else {
            rpc.set(
                "DEV2_PC_PROFILE",
                mapOf(
                    "name" to cleanName,
                    "filterLevel" to safeProfile.filterLevel.code.toString(),
                    "status" to safeProfile.status.value.toString(),
                    "stack" to safeProfile.stack,
                ),
                stack = safeProfile.stack,
            )
            safeProfile.stack
        }

        // Synchronize devices
        val existingDevices = if (isNew) emptyList() else runCatching { loadProfileDevices(stack) }.getOrDefault(emptyList())
        for (existing in existingDevices) {
            if (profile.devices.none { it.deviceMac.equals(existing.deviceMac, ignoreCase = true) }) {
                rpc.delete("DEV2_PC_PROFILE_DEVICE", stack = existing.stack)
            }
        }
        for (device in profile.devices) {
            if (existingDevices.none { it.deviceMac.equals(device.deviceMac, ignoreCase = true) }) {
                rpc.add(
                    "DEV2_PC_PROFILE_DEVICE",
                    mapOf(
                        "deviceMac" to device.deviceMac,
                        "entryName" to device.entryName,
                        "clientType" to device.clientType,
                        "pstack" to stack,
                    ),
                    parentStack = stack,
                )
            }
        }

        // Set Filter content
        rpc.set(
            "DEV2_PC_FILTERCONTENT",
            profile.filterContent.toAttributeMap() + ("stack" to stack),
            stack = stack,
        )

        // Synchronize blacklist
        val existingBlacklist = if (isNew) emptyList() else runCatching { loadBlacklist(stack) }.getOrDefault(emptyList())
        for (existing in existingBlacklist) {
            if (profile.blackList.none { it.url.equals(existing.url, ignoreCase = true) }) {
                rpc.delete("DEV2_PC_BLACKLIST", stack = existing.stack)
            }
        }
        for (item in profile.blackList) {
            if (existingBlacklist.none { it.url.equals(item.url, ignoreCase = true) }) {
                rpc.add(
                    "DEV2_PC_BLACKLIST",
                    mapOf("url" to item.url, "pstack" to stack),
                    parentStack = stack,
                )
            }
        }

        // Set weekday limit
        rpc.set(
            "DEV2_PC_WEEKDAY",
            mapOf(
                "timeLimitEnable" to if (profile.weekdayLimit.timeLimitEnable) "1" else "0",
                "timeLimit" to profile.weekdayLimit.timeLimitMinutes.toString(),
                "bedTimeEnable" to if (profile.weekdayLimit.bedTimeEnable) "1" else "0",
                "bedTimeBegin" to profile.weekdayLimit.bedTimeBeginMinutes.toString(),
                "bedTimeEnd" to profile.weekdayLimit.bedTimeEndMinutes.toString(),
                "index" to profile.weekdayLimit.dayMask.toString(),
                "stack" to stack,
            ),
            stack = stack,
        )

        // Set weekend limit
        rpc.set(
            "DEV2_PC_WEEKEND",
            mapOf(
                "timeLimitEnable" to if (profile.weekendLimit.timeLimitEnable) "1" else "0",
                "timeLimit" to profile.weekendLimit.timeLimitMinutes.toString(),
                "bedTimeEnable" to if (profile.weekendLimit.bedTimeEnable) "1" else "0",
                "bedTimeBegin" to profile.weekendLimit.bedTimeBeginMinutes.toString(),
                "bedTimeEnd" to profile.weekendLimit.bedTimeEndMinutes.toString(),
                "index" to profile.weekendLimit.dayMask.toString(),
                "stack" to stack,
            ),
            stack = stack,
        )

        // Commit to NVRAM
        rpc.operate("ACT_SAVE_CFG")

        safeProfile.copy(stack = stack)
    }

    fun deleteProfile(profileStack: String): Result<Unit> = runCatching {
        rpc.delete("DEV2_PC_PROFILE", stack = profileStack)
        rpc.operate("ACT_SAVE_CFG")
    }

    fun fetchInsights(profileStack: String, dateOffset: Int = 1): Result<WebInsights> = runCatching {
        val queryStack = buildDateStack(profileStack, dateOffset)

        // Daily time spent online
        val rankObj = toObject(rpc.get("DEV2_PC_VISITRANK", mapOf("stack" to queryStack), stack = queryStack))
        val dailyMinutes = rankObj["dailyTimeUsed"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0

        // Top visited domains
        val topListRes = rpc.getSubList("DEV2_PC_VISITRANK_ENTRY", mapOf("pstack" to queryStack))
        val topVisited = toList(topListRes).map {
            VisitedDomain(
                stack = it["stack"]?.jsonPrimitive?.content ?: "",
                url = it["url"]?.jsonPrimitive?.content ?: "",
                visitCount = it["visitCount"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                timeSpentMinutes = it["timeSpent"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
            )
        }

        // Blocked attempts
        val blockedRes = rpc.getSubList("DEV2_PC_BLOCKHISTORY_ENTRY", mapOf("pstack" to queryStack))
        val whiteListRes = rpc.getSubList("DEV2_PC_UNBLOCKEDLIST", mapOf("pstack" to profileStack))
        val whiteUrls = toList(whiteListRes).mapNotNull { it["url"]?.jsonPrimitive?.content?.lowercase() }.toSet()

        val blockedHistory = toList(blockedRes).map {
            val url = it["url"]?.jsonPrimitive?.content ?: ""
            val time = it["time"]?.jsonPrimitive?.content ?: it["timestamp"]?.jsonPrimitive?.content ?: ""
            BlockedHistoryEntry(
                stack = it["stack"]?.jsonPrimitive?.content ?: "",
                url = url,
                timestamp = time,
                isUnblocked = whiteUrls.contains(url.lowercase()),
            )
        }

        WebInsights(
            dateOffset = dateOffset,
            dailyTimeUsedMinutes = dailyMinutes,
            topVisited = topVisited,
            blockedHistory = blockedHistory,
        )
    }

    fun unblockUrl(profileStack: String, url: String): Result<Unit> = runCatching {
        rpc.add("DEV2_PC_UNBLOCKEDLIST", mapOf("url" to url, "pstack" to profileStack), parentStack = profileStack)
        rpc.operate("ACT_SAVE_CFG")
    }

    fun addBlacklistDomain(profileStack: String, url: String): Result<Unit> = runCatching {
        rpc.add("DEV2_PC_BLACKLIST", mapOf("url" to url, "pstack" to profileStack), parentStack = profileStack)
        rpc.operate("ACT_SAVE_CFG")
    }

    private fun buildDateStack(profileStack: String, dateOffset: Int): String {
        val parts = profileStack.split(',').toMutableList()
        while (parts.size < 6) parts.add("0")
        if (parts.size >= 2) {
            parts[1] = dateOffset.toString()
        }
        return parts.joinToString(",")
    }

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
