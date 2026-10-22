package es.digirouter.app

enum class VpnReadinessLevel {
    READY_PUBLIC_IP,
    READY_DDNS,
    WARNING_CGNAT_NO_DDNS,
    WARNING_PRIVATE_IP_NO_DDNS,
    UNKNOWN,
}

data class VpnReadinessStatus(
    val level: VpnReadinessLevel,
    val wanIp: String,
    val ddnsDomain: String? = null,
    val isDdnsActive: Boolean = false,
)

object VpnReadinessEvaluator {

    /**
     * Checks if an IPv4 address belongs to the RFC 6598 Carrier-Grade NAT block (100.64.0.0/10).
     * The second octet must range from 64 to 127 inclusive.
     */
    fun isCgnat(ip: String): Boolean {
        val octets = parseOctets(ip) ?: return false
        return octets[0] == 100 && octets[1] in 64..127
    }

    /**
     * Checks if an IPv4 address belongs to RFC 1918 private address ranges or loopback/link-local.
     */
    fun isPrivateIp(ip: String): Boolean {
        val octets = parseOctets(ip) ?: return false
        val a = octets[0]
        val b = octets[1]
        return when {
            a == 10 -> true
            a == 172 && b in 16..31 -> true
            a == 192 && b == 168 -> true
            a == 127 -> true
            a == 169 && b == 254 -> true
            else -> false
        }
    }

    /**
     * Checks if an IPv4 address is a routable public Internet address.
     */
    fun isPublicIp(ip: String): Boolean {
        val octets = parseOctets(ip) ?: return false
        if (octets[0] == 0 || octets[0] >= 224) return false
        return !isCgnat(ip) && !isPrivateIp(ip)
    }

    /**
     * Evaluates incoming VPN connectivity readiness given the current WAN IP and DDNS configuration.
     */
    fun evaluate(
        wanIp: String?,
        ddnsDomain: String?,
        isDdnsActive: Boolean,
    ): VpnReadinessStatus {
        val ip = wanIp?.trim().orEmpty()
        val domain = ddnsDomain?.trim()?.ifEmpty { null }
        val hasActiveDdns = isDdnsActive && domain != null

        val level = when {
            hasActiveDdns -> VpnReadinessLevel.READY_DDNS
            ip.isEmpty() -> VpnReadinessLevel.UNKNOWN
            isCgnat(ip) -> VpnReadinessLevel.WARNING_CGNAT_NO_DDNS
            isPrivateIp(ip) -> VpnReadinessLevel.WARNING_PRIVATE_IP_NO_DDNS
            isPublicIp(ip) -> VpnReadinessLevel.READY_PUBLIC_IP
            else -> VpnReadinessLevel.UNKNOWN
        }

        return VpnReadinessStatus(
            level = level,
            wanIp = ip,
            ddnsDomain = domain,
            isDdnsActive = hasActiveDdns,
        )
    }

    private fun parseOctets(ip: String): IntArray? {
        val parts = ip.trim().split(".")
        if (parts.size != 4) return null
        val octets = IntArray(4)
        for (i in 0..3) {
            val num = parts[i].toIntOrNull() ?: return null
            if (num !in 0..255) return null
            octets[i] = num
        }
        return octets
    }
}
