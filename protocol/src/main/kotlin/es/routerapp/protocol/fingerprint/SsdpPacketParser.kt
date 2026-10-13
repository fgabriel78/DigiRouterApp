package es.routerapp.protocol.fingerprint

import java.nio.charset.StandardCharsets

data class SsdpResponse(
    val startLine: String,
    val headers: Map<String, String>,
    val st: String?,
    val usn: String?,
    val location: String?,
    val server: String?,
)

object SsdpPacketParser {

    /**
     * Constructs a standard UPnP / SSDP M-SEARCH datagram packet.
     */
    fun buildMSearch(searchTarget: String = "ssdp:all", mx: Int = 2): ByteArray {
        val payload = "M-SEARCH * HTTP/1.1\r\n" +
            "HOST: 239.255.255.250:1900\r\n" +
            "MAN: \"ssdp:discover\"\r\n" +
            "MX: $mx\r\n" +
            "ST: $searchTarget\r\n" +
            "\r\n"
        return payload.toByteArray(StandardCharsets.UTF_8)
    }

    /**
     * Parses an incoming raw UDP payload representing an SSDP response or NOTIFY packet.
     */
    fun parse(data: ByteArray, length: Int = data.size): SsdpResponse? {
        if (length <= 0) return null
        val raw = String(data, 0, length, StandardCharsets.UTF_8)
        val lines = raw.lines()
        if (lines.isEmpty()) return null

        val startLine = lines.first().trim()
        if (!startLine.startsWith("HTTP/1.") && !startLine.startsWith("NOTIFY") && !startLine.startsWith("M-SEARCH")) {
            return null
        }

        val headers = mutableMapOf<String, String>()
        for (i in 1 until lines.size) {
            val line = lines[i].trim()
            if (line.isEmpty()) continue
            val colon = line.indexOf(':')
            if (colon > 0) {
                val key = line.substring(0, colon).trim().uppercase()
                val value = line.substring(colon + 1).trim()
                headers[key] = value
            }
        }

        val st = headers["ST"] ?: headers["NT"]
        val usn = headers["USN"]
        val location = headers["LOCATION"]
        val server = headers["SERVER"]

        return SsdpResponse(
            startLine = startLine,
            headers = headers,
            st = st,
            usn = usn,
            location = location,
            server = server,
        )
    }
}
