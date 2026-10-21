package es.routerapp.protocol.parental

enum class FilterLevel(val code: Int, val tag: String) {
    CHILD(0, "child"),
    PRE_TEEN(1, "pre_teen"),
    TEEN(2, "teen"),
    ADULT(3, "adult"),
    CUSTOM(4, "custom");

    companion object {
        fun fromCode(code: Int): FilterLevel = entries.find { it.code == code } ?: CUSTOM
        fun fromString(value: String): FilterLevel = value.toIntOrNull()?.let { fromCode(it) } ?: CUSTOM
    }
}

enum class ProfileStatus(val value: Int) {
    PAUSED(0),
    ACTIVE(1);

    companion object {
        fun fromInt(value: Int): ProfileStatus = if (value == 1) ACTIVE else PAUSED
    }
}

data class ProfileDevice(
    val stack: String = "",
    val deviceMac: String,
    val entryName: String = "",
    val clientType: String = "Other",
    val pstack: String = "",
)

data class TimeLimitConfig(
    val timeLimitEnable: Boolean = false,
    val timeLimitMinutes: Int = 120,
    val bedTimeEnable: Boolean = false,
    val bedTimeBeginMinutes: Int = 1320, // 22:00
    val bedTimeEndMinutes: Int = 360,    // 06:00
    val dayMask: Int = 0,
    val stack: String = "",
) {
    companion object {
        const val WEEKDAY_DEFAULT_MASK = 31 // Bits 0..4 (Mon-Fri)
        const val WEEKEND_DEFAULT_MASK = 96 // Bits 5..6 (Sat-Sun)

        fun formatTime(minutes: Int): String {
            val normalized = ((minutes % 1440) + 1440) % 1440
            val h = normalized / 60
            val m = normalized % 60
            return "%02d:%02d".format(h, m)
        }
    }

    val bedTimeBeginFormatted: String get() = formatTime(bedTimeBeginMinutes)
    val bedTimeEndFormatted: String get() = formatTime(bedTimeEndMinutes)
}

data class FilterCategories(
    val adultContentEnable: Boolean = false,
    val gamblingEnable: Boolean = false,
    val gamesEnable: Boolean = false,
    val mediaEnable: Boolean = false,
    val onlineCommunicationEnable: Boolean = false,
    val payToSurfEnable: Boolean = false,
    val socialNetworkingEnable: Boolean = false,
    val downLoadEnable: Boolean = false,
    val sexEducationEnable: Boolean = false,
    val stack: String = "",
) {
    companion object {
        fun forLevel(level: FilterLevel): FilterCategories = when (level) {
            FilterLevel.CHILD -> FilterCategories(
                adultContentEnable = true,
                gamblingEnable = true,
                gamesEnable = true,
                mediaEnable = true,
                onlineCommunicationEnable = true,
                payToSurfEnable = true,
                socialNetworkingEnable = true,
                downLoadEnable = true,
                sexEducationEnable = false,
            )
            FilterLevel.PRE_TEEN -> FilterCategories(
                adultContentEnable = true,
                gamblingEnable = true,
                socialNetworkingEnable = true,
            )
            FilterLevel.TEEN -> FilterCategories(
                adultContentEnable = true,
                gamblingEnable = true,
            )
            FilterLevel.ADULT, FilterLevel.CUSTOM -> FilterCategories()
        }
    }

    fun toAttributeMap(): Map<String, String> = mapOf(
        "adultContentEnable" to if (adultContentEnable) "1" else "0",
        "gambleEnable" to if (gamblingEnable) "1" else "0",
        "gamesEnable" to if (gamesEnable) "1" else "0",
        "mediaEnable" to if (mediaEnable) "1" else "0",
        "onlineCommunicationEnable" to if (onlineCommunicationEnable) "1" else "0",
        "payToSurfEnable" to if (payToSurfEnable) "1" else "0",
        "socialNetworkingEnable" to if (socialNetworkingEnable) "1" else "0",
        "downLoadEnable" to if (downLoadEnable) "1" else "0",
        "sexEducationEnable" to if (sexEducationEnable) "1" else "0",
    )
}

data class BlacklistEntry(
    val stack: String = "",
    val url: String,
    val pstack: String = "",
)

data class VisitedDomain(
    val stack: String = "",
    val url: String,
    val visitCount: Int = 0,
    val timeSpentMinutes: Int = 0,
)

data class BlockedHistoryEntry(
    val stack: String = "",
    val url: String,
    val timestamp: String = "",
    val isUnblocked: Boolean = false,
)

data class WebInsights(
    val dateOffset: Int = 1, // 1: Today, 2: Yesterday, 3: 2 days ago
    val dailyTimeUsedMinutes: Int = 0,
    val topVisited: List<VisitedDomain> = emptyList(),
    val blockedHistory: List<BlockedHistoryEntry> = emptyList(),
)

data class ParentalProfile(
    val stack: String = "",
    val name: String,
    val filterLevel: FilterLevel = FilterLevel.PRE_TEEN,
    val status: ProfileStatus = ProfileStatus.ACTIVE,
    val devices: List<ProfileDevice> = emptyList(),
    val weekdayLimit: TimeLimitConfig = TimeLimitConfig(dayMask = TimeLimitConfig.WEEKDAY_DEFAULT_MASK),
    val weekendLimit: TimeLimitConfig = TimeLimitConfig(dayMask = TimeLimitConfig.WEEKEND_DEFAULT_MASK),
    val filterContent: FilterCategories = FilterCategories.forLevel(filterLevel),
    val blackList: List<BlacklistEntry> = emptyList(),
)
