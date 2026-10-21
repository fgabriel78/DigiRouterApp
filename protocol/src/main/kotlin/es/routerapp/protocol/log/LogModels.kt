package es.routerapp.protocol.log

import kotlinx.serialization.Serializable

/**
 * Standard syslog severity levels supported by the router (levels 0 through 7).
 */
@Serializable
enum class LogSeverity(val code: Int, val label: String) {
    EMERGENCY(0, "Emergency"),
    ALERT(1, "Alert"),
    CRITICAL(2, "Critical"),
    ERROR(3, "Error"),
    WARNING(4, "Warning"),
    NOTICE(5, "Notice"),
    INFO(6, "Info"),
    DEBUG(7, "Debug");

    companion object {
        fun fromCode(code: Int): LogSeverity =
            entries.firstOrNull { it.code == code } ?: INFO

        fun fromName(name: String): LogSeverity =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) || it.label.equals(name, ignoreCase = true) } ?: INFO
    }
}

/**
 * Represents a single decoded system event entry from the router's circular log buffer.
 */
@Serializable
data class LogEntry(
    val timestamp: String,
    val severity: LogSeverity,
    val module: String,
    val message: String,
    val id: String = "$timestamp-$module-$message",
)

/**
 * Configuration parameters for the router's internal syslog daemon (OID: DEV2_SYSLOG_CFG).
 */
@Serializable
data class SyslogConfig(
    val logToLocal: Boolean = true,
    val localSeverity: LogSeverity = LogSeverity.INFO,
    val logToRemote: Boolean = false,
    val remoteSeverity: LogSeverity = LogSeverity.INFO,
    val serverIp: String = "",
    val serverPort: Int = 514,
    val facility: Int = 1,
)
