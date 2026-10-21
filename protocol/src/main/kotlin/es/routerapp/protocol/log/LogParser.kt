package es.routerapp.protocol.log

/**
 * Parser for the raw text response from the router's `/cgi/log` endpoint.
 *
 * Wire format:
 * ```
 * 0\r\n
 * 2026-10-20 22:50:14 [3] OMCI: GPON link loss of signal\r\n
 * 2026-10-20 22:49:58 [4] WIFI: Station deauthenticated\r\n
 * ```
 */
object LogParser {

    private val LOG_LINE_REGEX = Regex("""^([\d-]+\s[\d:]+)\s+\[(\d)\]\s+(\w+):\s+(.*)$""")

    /**
     * Parses the raw string returned by `GET /cgi/log`.
     *
     * @param raw The raw HTTP response body text.
     * @return A list of [LogEntry] ordered newest-first (reverse chronological).
     */
    fun parse(raw: String?): List<LogEntry> {
        if (raw.isNullOrBlank()) return emptyList()

        val lines = raw.lines()
            .map { it.trimEnd('\r', '\n') }
            .filter { it.isNotBlank() }

        if (lines.isEmpty()) return emptyList()

        // The first line contains the router return code (0 = success)
        val returnCode = lines[0].trim().toIntOrNull()
        if (returnCode != 0) {
            return emptyList()
        }

        val entries = mutableListOf<LogEntry>()
        // Process remaining lines (skipping the status code at index 0)
        for (i in 1 until lines.size) {
            val line = lines[i]
            val match = LOG_LINE_REGEX.find(line) ?: continue
            val (timestamp, severityStr, module, message) = match.destructured
            val code = severityStr.toIntOrNull() ?: LogSeverity.INFO.code
            entries.add(
                LogEntry(
                    timestamp = timestamp,
                    severity = LogSeverity.fromCode(code),
                    module = module.uppercase(),
                    message = message.trim(),
                )
            )
        }

        // Router outputs in chronological order (oldest to newest); reverse to present newest first
        entries.reverse()
        return entries
    }
}
