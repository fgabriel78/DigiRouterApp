package es.routerapp.app

import java.net.InetAddress

/**
 * Validates whether a given network address, hostname, or URL belongs to
 * a local or private network range (RFC 1918, loopback, link-local, or local domains).
 */
object LocalNetworkValidator {

    private val LOCAL_DOMAINS = setOf(
        "localhost",
        "tplinkwifi.net",
        "tplinklogin.net",
    )

    private val LOCAL_SUFFIXES = listOf(
        ".local",
        ".lan",
        ".home.arpa",
    )

    /**
     * Extracts the host portion from a user-supplied address or URL string,
     * stripping scheme, port, path, and surrounding brackets.
     */
    fun extractHost(input: String): String {
        var clean = input.trim()
        if (clean.startsWith("http://", ignoreCase = true)) {
            clean = clean.substring(7)
        } else if (clean.startsWith("https://", ignoreCase = true)) {
            clean = clean.substring(8)
        }
        val slashIndex = clean.indexOf('/')
        if (slashIndex != -1) {
            clean = clean.substring(0, slashIndex)
        }
        if (clean.startsWith("[") && clean.contains("]")) {
            val endBracket = clean.indexOf(']')
            return clean.substring(1, endBracket)
        }
        val colonIndex = clean.indexOf(':')
        if (colonIndex != -1) {
            clean = clean.substring(0, colonIndex)
        }
        return clean.trim()
    }

    /**
     * Returns true if [input] resolves to an RFC 1918 private IPv4 subnet,
     * loopback, link-local, IPv6 ULA, or known local domain name.
     */
    fun isLocalAddress(input: String): Boolean {
        val host = extractHost(input)
        if (host.isEmpty()) return false

        val lowerHost = host.lowercase()
        if (lowerHost in LOCAL_DOMAINS) return true
        if (LOCAL_SUFFIXES.any { lowerHost.endsWith(it) }) return true

        // Single-label hostnames without dots or colons (e.g., "router", "esdigi") resolve locally
        if (!host.contains('.') && !host.contains(':')) {
            return true
        }

        return try {
            if (isIpAddress(host)) {
                val addr = InetAddress.getByName(host)
                addr.isSiteLocalAddress || addr.isLoopbackAddress || addr.isLinkLocalAddress || isUlaIpv6(addr)
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun isIpAddress(host: String): Boolean {
        val parts = host.split('.')
        if (parts.size == 4 && parts.all { it.isNotEmpty() && it.all { c -> c.isDigit() } && it.toIntOrNull() in 0..255 }) {
            return true
        }
        if (host.contains(':')) {
            return true
        }
        return false
    }

    private fun isUlaIpv6(addr: InetAddress): Boolean {
        val bytes = addr.address
        if (bytes.size == 16) {
            val firstByte = bytes[0].toInt() and 0xFF
            return (firstByte and 0xFE) == 0xFC
        }
        return false
    }
}
