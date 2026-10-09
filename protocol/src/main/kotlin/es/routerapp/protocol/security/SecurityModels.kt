package es.routerapp.protocol.security

enum class Severity {
    CRITICAL,
    HIGH,
    MEDIUM,
    LOW,
    INFO,
}

enum class HealthRating {
    EXCELLENT,
    GOOD,
    POOR,
}

data class RemediationAction(
    val id: String,
    val title: String,
    val oid: String,
    val changes: Map<String, String>,
    val stack: String = "0,0,0,0,0,0",
    val isAutomaticSafe: Boolean = true,
    val targetScreenId: String? = null,
)

data class SecurityFinding(
    val id: String,
    val checkId: String,
    val title: String,
    val description: String,
    val severity: Severity,
    val penaltyPoints: Int,
    val isPassed: Boolean,
    val remediation: RemediationAction? = null,
    val details: String? = null,
)

data class SecurityReport(
    val score: Int,
    val rating: HealthRating,
    val findings: List<SecurityFinding>,
    val criticalCount: Int,
    val warningCount: Int,
    val passedCount: Int,
    val batchPlan: List<RemediationAction>,
)

data class SecurityRawData(
    val dmz: List<Map<String, String>> = emptyList(),
    val wifi: List<Map<String, String>> = emptyList(),
    val upnpCfg: List<Map<String, String>> = emptyList(),
    val upnpMappings: List<Map<String, String>> = emptyList(),
    val portMappings: List<Map<String, String>> = emptyList(),
    val ddos: List<Map<String, String>> = emptyList(),
    val ftp: List<Map<String, String>> = emptyList(),
    val smb: List<Map<String, String>> = emptyList(),
)
