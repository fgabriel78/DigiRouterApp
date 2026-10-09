package es.routerapp.protocol.security

import es.routerapp.protocol.RouterClient
import es.routerapp.protocol.pages.PageEngine

object SecurityAuditEngine {

    private val WEAK_PASSWORDS = setOf(
        "12345678", "123456789", "1234567890", "password", "admin123", "qwertyuiop"
    )

    fun loadRawData(client: RouterClient): SecurityRawData {
        fun safeGet(oid: String): List<Map<String, String>> = runCatching {
            PageEngine.toInstances(client.getList(oid)).map { it.values }
        }.getOrDefault(emptyList())

        return SecurityRawData(
            dmz = safeGet("DEV2_DMZ_HOST_CFG"),
            wifi = safeGet("DEV2_ADT_WIFI_COMMON"),
            upnpCfg = safeGet("DEV2_UPNP_CFG"),
            upnpMappings = safeGet("DEV2_UPNP_PORTMAPPING"),
            portMappings = safeGet("DEV2_PORTMAPPING"),
            ddos = safeGet("DEV2_DDOS_CFG"),
            ftp = safeGet("DEV2_FTP_SERVER"),
            smb = safeGet("DEV2_SMB_SERVICE"),
        )
    }

    fun audit(client: RouterClient): SecurityReport {
        return evaluate(loadRawData(client))
    }

    fun evaluate(data: SecurityRawData): SecurityReport {
        val findings = mutableListOf<SecurityFinding>()

        // 1. DMZ Host Check
        val dmzActive = data.dmz.any { it["enable"] == "1" }
        if (dmzActive) {
            val hostIp = data.dmz.firstOrNull { it["enable"] == "1" }?.get("IPAddress") ?: ""
            findings.add(
                SecurityFinding(
                    id = "dmz_exposure",
                    checkId = "dmz",
                    title = "DMZ Host Active",
                    description = "DMZ exposes all unmapped ports of an internal device to the Internet without firewall protection.",
                    severity = Severity.CRITICAL,
                    penaltyPoints = 25,
                    isPassed = false,
                    remediation = RemediationAction(
                        id = "disable_dmz",
                        title = "Disable DMZ Host",
                        oid = "DEV2_DMZ_HOST_CFG",
                        changes = mapOf("enable" to "0"),
                        isAutomaticSafe = true,
                        targetScreenId = "dmz",
                    ),
                    details = if (hostIp.isNotBlank()) "Target host: $hostIp" else null,
                )
            )
        } else {
            findings.add(
                SecurityFinding(
                    id = "dmz_exposure",
                    checkId = "dmz",
                    title = "DMZ Host Disabled",
                    description = "No local devices are exposed to unfiltered incoming Internet traffic.",
                    severity = Severity.CRITICAL,
                    penaltyPoints = 0,
                    isPassed = true,
                )
            )
        }

        // 2. Wi-Fi Encryption Mode
        val openWifi = data.wifi.any { it["primaryEnable"] == "1" && it["primaryModeEnabled"] in setOf("None", "WEP") }
        if (openWifi) {
            findings.add(
                SecurityFinding(
                    id = "wifi_encryption",
                    checkId = "wifi_mode",
                    title = "Unencrypted Wi-Fi Network",
                    description = "One or more Wi-Fi networks have no password encryption, allowing anyone nearby to join.",
                    severity = Severity.CRITICAL,
                    penaltyPoints = 25,
                    isPassed = false,
                    remediation = RemediationAction(
                        id = "secure_wifi",
                        title = "Configure Wi-Fi Password",
                        oid = "DEV2_ADT_WIFI_COMMON",
                        changes = emptyMap(),
                        isAutomaticSafe = false,
                        targetScreenId = "wifi",
                    ),
                )
            )
        } else {
            findings.add(
                SecurityFinding(
                    id = "wifi_encryption",
                    checkId = "wifi_mode",
                    title = "Wi-Fi Encryption Active",
                    description = "Wi-Fi networks use strong WPA2 or WPA3 personal encryption.",
                    severity = Severity.CRITICAL,
                    penaltyPoints = 0,
                    isPassed = true,
                )
            )
        }

        // 3. Wi-Fi Password Strength
        val weakWifiPwd = data.wifi.any {
            val enabled = it["primaryEnable"] == "1"
            val mode = it["primaryModeEnabled"]
            val psk = it["primaryPSK"].orEmpty()
            enabled && mode != "None" && (psk.length < 10 || psk in WEAK_PASSWORDS)
        }
        if (weakWifiPwd) {
            findings.add(
                SecurityFinding(
                    id = "wifi_password_strength",
                    checkId = "wifi_pwd",
                    title = "Weak Wi-Fi Password",
                    description = "Wi-Fi password has fewer than 10 characters or is easily guessable.",
                    severity = Severity.MEDIUM,
                    penaltyPoints = 10,
                    isPassed = false,
                    remediation = RemediationAction(
                        id = "strengthen_wifi_pwd",
                        title = "Strengthen Wi-Fi Password",
                        oid = "DEV2_ADT_WIFI_COMMON",
                        changes = emptyMap(),
                        isAutomaticSafe = false,
                        targetScreenId = "wifi",
                    ),
                )
            )
        } else {
            findings.add(
                SecurityFinding(
                    id = "wifi_password_strength",
                    checkId = "wifi_pwd",
                    title = "Strong Wi-Fi Password",
                    description = "Wi-Fi password has 10 or more characters.",
                    severity = Severity.MEDIUM,
                    penaltyPoints = 0,
                    isPassed = true,
                )
            )
        }

        // 4. WPS Status Check
        val wpsActive = data.wifi.any { it["WPSEnable"] == "1" }
        if (wpsActive) {
            findings.add(
                SecurityFinding(
                    id = "wps_status",
                    checkId = "wps",
                    title = "WPS Enabled",
                    description = "Wi-Fi Protected Setup (WPS) is susceptible to PIN brute-force and Pixie Dust attacks.",
                    severity = Severity.MEDIUM,
                    penaltyPoints = 10,
                    isPassed = false,
                    remediation = RemediationAction(
                        id = "disable_wps",
                        title = "Disable WPS",
                        oid = "DEV2_ADT_WIFI_COMMON",
                        changes = mapOf("WPSEnable" to "0"),
                        isAutomaticSafe = true,
                        targetScreenId = "wps",
                    ),
                )
            )
        } else {
            findings.add(
                SecurityFinding(
                    id = "wps_status",
                    checkId = "wps",
                    title = "WPS Disabled",
                    description = "WPS is disabled, protecting your network from WPS PIN exploits.",
                    severity = Severity.MEDIUM,
                    penaltyPoints = 0,
                    isPassed = true,
                )
            )
        }

        // 5. Guest Wi-Fi Isolation
        val guestExposed = data.wifi.any { it["guestEnable"] == "1" && it["guestLANAccessEnable"] == "1" }
        if (guestExposed) {
            findings.add(
                SecurityFinding(
                    id = "guest_lan_access",
                    checkId = "guest",
                    title = "Guest Wi-Fi Can Access Private LAN",
                    description = "Devices on the guest Wi-Fi network are allowed to communicate with private computers, printers, and NAS storage.",
                    severity = Severity.HIGH,
                    penaltyPoints = 15,
                    isPassed = false,
                    remediation = RemediationAction(
                        id = "isolate_guest",
                        title = "Isolate Guest Network from LAN",
                        oid = "DEV2_ADT_WIFI_COMMON",
                        changes = mapOf("guestLANAccessEnable" to "0"),
                        isAutomaticSafe = true,
                        targetScreenId = "guest",
                    ),
                )
            )
        } else {
            findings.add(
                SecurityFinding(
                    id = "guest_lan_access",
                    checkId = "guest",
                    title = "Guest Network Isolated",
                    description = "Guest network is properly separated from private local network devices.",
                    severity = Severity.HIGH,
                    penaltyPoints = 0,
                    isPassed = true,
                )
            )
        }

        // 6. UPnP & Dangerous Port Mappings
        val upnpEnabled = data.upnpCfg.any { it["enable"] == "1" }
        val riskyUpnpPorts = mutableListOf<Int>()
        if (upnpEnabled) {
            for (mapping in data.upnpMappings) {
                val extPort = mapping["externalPort"]?.toIntOrNull()
                    ?: mapping["port"]?.toIntOrNull()
                    ?: mapping["externalPortEndRange"]?.toIntOrNull()
                if (extPort != null && RiskyPortCatalog.isDangerous(extPort)) {
                    riskyUpnpPorts.add(extPort)
                }
            }
        }

        if (riskyUpnpPorts.isNotEmpty()) {
            findings.add(
                SecurityFinding(
                    id = "upnp_risky_ports",
                    checkId = "upnp",
                    title = "Dangerous UPnP Port Mappings",
                    description = "UPnP has opened sensitive ports to the Internet without explicit manual configuration.",
                    severity = Severity.HIGH,
                    penaltyPoints = 15,
                    isPassed = false,
                    remediation = RemediationAction(
                        id = "check_upnp",
                        title = "Review UPnP Mappings",
                        oid = "DEV2_UPNP_CFG",
                        changes = emptyMap(),
                        isAutomaticSafe = false,
                        targetScreenId = "upnp",
                    ),
                    details = "Exposed ports: ${riskyUpnpPorts.joinToString(", ")}",
                )
            )
        } else {
            findings.add(
                SecurityFinding(
                    id = "upnp_risky_ports",
                    checkId = "upnp",
                    title = "UPnP Secure",
                    description = if (upnpEnabled) "UPnP active without dangerous port mappings (benign gaming/voice traffic)." else "UPnP is disabled.",
                    severity = Severity.HIGH,
                    penaltyPoints = 0,
                    isPassed = true,
                )
            )
        }

        // 7. NAT Virtual Servers / Port Forwarding
        val riskyNatPorts = mutableListOf<Int>()
        for (rule in data.portMappings) {
            if (rule["enable"] == "1") {
                val ext = rule["externalPort"]?.toIntOrNull()
                val int = rule["internalPort"]?.toIntOrNull()
                if (ext != null && RiskyPortCatalog.isDangerous(ext)) riskyNatPorts.add(ext)
                else if (int != null && RiskyPortCatalog.isDangerous(int)) riskyNatPorts.add(int)
            }
        }

        if (riskyNatPorts.isNotEmpty()) {
            findings.add(
                SecurityFinding(
                    id = "nat_risky_ports",
                    checkId = "nat",
                    title = "Risky Port Forwarding Rules",
                    description = "Virtual server rules forward incoming Internet traffic to sensitive services (RDP, SMB, FTP, Telnet, or databases).",
                    severity = Severity.HIGH,
                    penaltyPoints = 15,
                    isPassed = false,
                    remediation = RemediationAction(
                        id = "check_nat",
                        title = "Review Port Forwarding Rules",
                        oid = "DEV2_PORTMAPPING",
                        changes = emptyMap(),
                        isAutomaticSafe = false,
                        targetScreenId = "virtual-servers",
                    ),
                    details = "Exposed ports: ${riskyNatPorts.distinct().joinToString(", ")}",
                )
            )
        } else {
            findings.add(
                SecurityFinding(
                    id = "nat_risky_ports",
                    checkId = "nat",
                    title = "Port Forwarding Secure",
                    description = "No sensitive administrative or unencrypted ports forwarded from WAN.",
                    severity = Severity.HIGH,
                    penaltyPoints = 0,
                    isPassed = true,
                )
            )
        }

        // 8. DoS Defense Check
        val ddosActive = data.ddos.any {
            it["enable"] == "1" && (it["enableSynFilter"] == "1" || it["enableUdpFilter"] == "1" || it["enableIcmpFilter"] == "1")
        }
        if (!ddosActive) {
            findings.add(
                SecurityFinding(
                    id = "ddos_protection",
                    checkId = "ddos",
                    title = "DoS Protection Disabled",
                    description = "Router firewall flood filters (SYN, UDP, ICMP) are not active.",
                    severity = Severity.MEDIUM,
                    penaltyPoints = 10,
                    isPassed = false,
                    remediation = RemediationAction(
                        id = "enable_ddos",
                        title = "Enable DoS Protection",
                        oid = "DEV2_DDOS_CFG",
                        changes = mapOf(
                            "enable" to "1",
                            "enableSynFilter" to "1",
                            "enableUdpFilter" to "1",
                            "enableIcmpFilter" to "1",
                        ),
                        isAutomaticSafe = true,
                        targetScreenId = "ddos",
                    ),
                )
            )
        } else {
            findings.add(
                SecurityFinding(
                    id = "ddos_protection",
                    checkId = "ddos",
                    title = "DoS Protection Active",
                    description = "Firewall filters protect against SYN, UDP, and ICMP flood attacks.",
                    severity = Severity.MEDIUM,
                    penaltyPoints = 0,
                    isPassed = true,
                )
            )
        }

        // 9. USB FTP Server Exposure
        val ftpExposed = data.ftp.any {
            it["enable"] == "1" && (it["accessFromInternet"] == "1" || it["anonymous"] == "1")
        }
        if (ftpExposed) {
            findings.add(
                SecurityFinding(
                    id = "ftp_exposure",
                    checkId = "ftp",
                    title = "USB FTP Server Exposed to Internet / Anonymous Access",
                    description = "Shared USB storage allows FTP access from Internet or unauthenticated anonymous logins.",
                    severity = Severity.CRITICAL,
                    penaltyPoints = 20,
                    isPassed = false,
                    remediation = RemediationAction(
                        id = "secure_ftp",
                        title = "Disable Internet/Anonymous FTP Access",
                        oid = "DEV2_FTP_SERVER",
                        changes = mapOf("accessFromInternet" to "0", "anonymous" to "0"),
                        isAutomaticSafe = true,
                        targetScreenId = "storage",
                    ),
                )
            )
        } else {
            findings.add(
                SecurityFinding(
                    id = "ftp_exposure",
                    checkId = "ftp",
                    title = "FTP File Sharing Secure",
                    description = "FTP server is either disabled or restricted to authenticated local users.",
                    severity = Severity.CRITICAL,
                    penaltyPoints = 0,
                    isPassed = true,
                )
            )
        }

        // 10. USB Samba Anonymous Sharing
        val smbAnon = data.smb.any { it["enable"] == "1" && it["anonymous"] == "1" }
        if (smbAnon) {
            findings.add(
                SecurityFinding(
                    id = "smb_anonymous",
                    checkId = "smb",
                    title = "Anonymous Samba (SMB) Sharing Enabled",
                    description = "Anyone connected to the Wi-Fi or LAN can access shared USB drive folders without logging in.",
                    severity = Severity.MEDIUM,
                    penaltyPoints = 10,
                    isPassed = false,
                    remediation = RemediationAction(
                        id = "disable_smb_anon",
                        title = "Disable Anonymous Samba Access",
                        oid = "DEV2_SMB_SERVICE",
                        changes = mapOf("anonymous" to "0"),
                        isAutomaticSafe = true,
                        targetScreenId = "storage",
                    ),
                )
            )
        } else {
            findings.add(
                SecurityFinding(
                    id = "smb_anonymous",
                    checkId = "smb",
                    title = "Samba (SMB) File Sharing Secure",
                    description = "SMB storage sharing is disabled or requires authentication.",
                    severity = Severity.MEDIUM,
                    penaltyPoints = 0,
                    isPassed = true,
                )
            )
        }

        var totalDeduction = 0
        for (f in findings) {
            if (!f.isPassed) {
                totalDeduction += f.penaltyPoints
            }
        }

        val rawScore = (100 - totalDeduction).coerceIn(0, 100)
        val rating = when {
            rawScore >= 90 -> HealthRating.EXCELLENT
            rawScore >= 70 -> HealthRating.GOOD
            else -> HealthRating.POOR
        }

        val criticalCount = findings.count { !it.isPassed && it.severity == Severity.CRITICAL }
        val warningCount = findings.count { !it.isPassed && (it.severity == Severity.HIGH || it.severity == Severity.MEDIUM || it.severity == Severity.LOW) }
        val passedCount = findings.count { it.isPassed }

        val batchPlan = findings
            .filter { !it.isPassed && it.remediation?.isAutomaticSafe == true }
            .mapNotNull { it.remediation }

        return SecurityReport(
            score = rawScore,
            rating = rating,
            findings = findings,
            criticalCount = criticalCount,
            warningCount = warningCount,
            passedCount = passedCount,
            batchPlan = batchPlan,
        )
    }

    fun executeRemediation(client: RouterClient, action: RemediationAction) {
        if (action.oid == "DEV2_ADT_WIFI_COMMON") {
            val instances = runCatching {
                PageEngine.toInstances(client.getList("DEV2_ADT_WIFI_COMMON"))
            }.getOrDefault(emptyList())
            if (instances.isNotEmpty()) {
                for (inst in instances) {
                    client.set("DEV2_ADT_WIFI_COMMON", action.changes, inst.stack)
                }
            } else {
                client.set(action.oid, action.changes, action.stack)
            }
        } else {
            client.set(action.oid, action.changes, action.stack)
        }
    }

    fun executeBatchPlan(client: RouterClient, plan: List<RemediationAction>): List<Pair<RemediationAction, Result<Unit>>> {
        return plan.map { action ->
            val result = runCatching {
                executeRemediation(client, action)
            }
            action to result
        }
    }
}
