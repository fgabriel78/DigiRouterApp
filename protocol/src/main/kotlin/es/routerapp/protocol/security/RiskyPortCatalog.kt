package es.routerapp.protocol.security

/**
 * Categorizes network ports into risk levels to evaluate UPnP mappings and NAT virtual servers.
 */
object RiskyPortCatalog {

    data class PortRiskInfo(
        val port: Int,
        val serviceName: String,
        val description: String,
        val isCritical: Boolean = true,
    )

    // Critical or dangerous ports often targeted by automated internet scans / malware
    private val DANGEROUS_PORTS = mapOf(
        21 to PortRiskInfo(21, "FTP", "Plaintext file transfer protocol"),
        22 to PortRiskInfo(22, "SSH", "Remote shell service exposed to brute-force"),
        23 to PortRiskInfo(23, "Telnet", "Unencrypted remote administration"),
        25 to PortRiskInfo(25, "SMTP", "Mail relay service"),
        53 to PortRiskInfo(53, "DNS", "DNS resolver open to amplification attacks"),
        80 to PortRiskInfo(80, "HTTP", "Plaintext web administration or server"),
        137 to PortRiskInfo(137, "NetBIOS-NS", "Windows NetBIOS Name Service"),
        138 to PortRiskInfo(138, "NetBIOS-DGM", "Windows NetBIOS Datagram Service"),
        139 to PortRiskInfo(139, "NetBIOS-SSN", "Windows NetBIOS Session Service"),
        445 to PortRiskInfo(445, "SMB", "Windows file sharing susceptible to ransomware (WannaCry/EternalBlue)"),
        1433 to PortRiskInfo(1433, "MSSQL", "Microsoft SQL Server database"),
        3306 to PortRiskInfo(3306, "MySQL", "MySQL database service"),
        3389 to PortRiskInfo(3389, "RDP", "Windows Remote Desktop exposed to automated brute-force"),
        5432 to PortRiskInfo(5432, "PostgreSQL", "PostgreSQL database service"),
        5900 to PortRiskInfo(5900, "VNC", "Virtual Network Computing remote screen"),
        6379 to PortRiskInfo(6379, "Redis", "In-memory database often unauthenticated"),
        27017 to PortRiskInfo(27017, "MongoDB", "NoSQL database service"),
    )

    // Known benign ports for online gaming, voice, and streaming (UDP/TCP)
    private val KNOWN_SAFE_GAMING_PORTS = setOf(
        3074, // Xbox Live / COD
        3478, 3479, 3480, // PlayStation Network / STUN
        9308, // PlayStation Network
        27015, 27036, // Steam game networking
    )

    /**
     * Checks if an external port is considered risky.
     */
    fun evaluatePort(port: Int): PortRiskInfo? {
        return DANGEROUS_PORTS[port]
    }

    /**
     * Returns true if the port is in the dangerous port list.
     */
    fun isDangerous(port: Int): Boolean = DANGEROUS_PORTS.containsKey(port)

    /**
     * Checks if a port is known to be benign (e.g. Xbox Live, PSN, or high ephemeral gaming port).
     */
    fun isKnownSafeGamingPort(port: Int): Boolean {
        if (port in 30000..65535) return true
        return KNOWN_SAFE_GAMING_PORTS.contains(port)
    }
}
