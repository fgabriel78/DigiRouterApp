package es.routerapp.protocol.vpn

/**
 * Parses and manipulates OpenVPN client profile (`.ovpn`) text content.
 * Enables inspecting the configured remote host and replacing it with an override
 * hostname or domain (e.g., DDNS hostname or external public IP) prior to client distribution.
 */
object OpenVpnProfileTransformer {

    private val REMOTE_REGEX = Regex("""(?m)^remote\s+(\S+)\s+(\d+)(.*)$""")

    /**
     * Extracts the configured remote target host from an `.ovpn` configuration string.
     * Returns null if no `remote` directive is found.
     */
    fun extractRemoteHost(ovpnContent: String): String? {
        val match = REMOTE_REGEX.find(ovpnContent) ?: return null
        return match.groupValues[1]
    }

    /**
     * Extracts the configured port from an `.ovpn` configuration string.
     */
    fun extractRemotePort(ovpnContent: String): Int? {
        val match = REMOTE_REGEX.find(ovpnContent) ?: return null
        return match.groupValues[2].toIntOrNull()
    }

    /**
     * Overrides the remote host directive in the `.ovpn` configuration text.
     * If [newHost] is blank, the original content is returned unchanged.
     */
    fun overrideRemoteHost(ovpnContent: String, newHost: String?): String {
        val target = newHost?.trim()
        if (target.isNullOrEmpty()) return ovpnContent

        return REMOTE_REGEX.replace(ovpnContent) { matchResult ->
            val port = matchResult.groupValues[2]
            val trailing = matchResult.groupValues[3]
            "remote $target $port$trailing"
        }
    }
}
