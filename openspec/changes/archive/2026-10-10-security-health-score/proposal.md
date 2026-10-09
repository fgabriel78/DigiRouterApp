# Proposal

## Why

Router configuration interfaces present dozens of isolated security, Wi-Fi, and firewall settings without providing users with a holistic assessment of their network's vulnerability. Home users often unknowingly leave critical exposure vectors active—such as an unshielded DMZ host, vulnerable WPS pins, guest networks bridged directly to private LAN devices, or silent UPnP port mappings created by background software. Furthermore, traditional diagnostic tools only report problems without offering immediate remediation.

This change introduces a comprehensive, automated **Security Health Score** (0–100) and auditing system into RouterApp. It allows users to assess their network posture in under a second, highlights critical vulnerabilities directly on the main dashboard, avoids false positives on benign UPnP gaming traffic, and provides an actionable **"1-Tap Fix All"** batch remediation workflow.

## What Changes

- **Security Health Evaluation Engine (`:protocol`)**:
  - Pure Kotlin evaluation engine (`SecurityAuditEngine`) that queries router OIDs (`DEV2_DMZ_HOST_CFG`, `DEV2_ADT_WIFI_COMMON`, `DEV2_UPNP_CFG`, `DEV2_UPNP_PORTMAPPING`, `DEV2_PORTMAPPING`, `DEV2_DDOS_CFG`, `DEV2_FTP_SERVER`, `DEV2_SMB_SERVICE`).
  - Computes an aggregate health score (0–100) with categorized findings (`Critical`, `Warning`, `Recommendation`, `Passed`).
  - **Intelligent UPnP Port Risk Detection**: Evaluates active UPnP port mappings against a catalog of high-risk services (e.g., Telnet 23, FTP 21, SMB 445, RDP 3389, unencrypted databases). Does not penalize benign high-port gaming/voice mappings (e.g., PlayStation Network, Xbox Live, WebRTC).
  - Computes executable remediation actions (`RemediationAction`) for automated batch application (`BatchRemediationPlan`).
- **Dashboard Critical Alert Banner & Entry Point (`:app`)**:
  - High-priority banner displayed at the top of the main Dashboard (`DashboardScreen`) when one or more `Critical` vulnerabilities exist (e.g., active DMZ, open Wi-Fi network).
  - 1-tap immediate remediation button right on the banner, plus a navigation shortcut to the full audit screen.
  - Bento-style summary card displaying the current score, health status badge, and findings summary.
- **Dedicated Security Health Screen (`:app`)**:
  - Built with Material 3 Expressive components: animated circular gauge showing the score (0–100), color-coded state, and severity breakdown chips.
  - Filter chips to filter checks by status: *All*, *At Risk*, and *Passed*.
  - Expandable finding cards with clear explanations ("Why is this a risk?") and individual "Fix Now" buttons.
  - **"Resolver todo con 1 toque" (1-Tap Auto-Remediate)**: A confirmation bottom sheet listing all safe automated fixes (disabling DMZ, disabling WPS, isolating guest network from LAN, enabling DoS defense, closing anonymous FTP/SMB) and executing them sequentially with real-time feedback.
- **Bilingual Localization**:
  - Full English and Spanish translations for all check titles, descriptions, recommendations, and UI buttons.

## Capabilities

### New Capabilities
- `security-health-score`: Defines automated security posture auditing across router subsystems, intelligent UPnP port risk filtering, multi-level severity scoring (0–100), and automated batch remediation execution.

### Modified Capabilities
- `dashboard-status`: Adds requirements for presenting high-priority critical security alert banners and a persistent security health score summary entry card on the main dashboard.

## Impact

- **`:protocol` module**:
  - Adds `SecurityAuditEngine`, `SecurityCheck`, `SecurityReport`, `SecurityFinding`, `RiskyPortCatalog`, and remediation plan definitions.
  - No new external dependencies; pure Kotlin, fully covered by JVM unit tests.
- **`:app` module**:
  - Adds `SecurityHealthScreen.kt`, `SecurityHealthViewModel.kt`, and updates `StatusScreens.kt` (`DashboardScreen`).
  - Adds navigation routes and Material 3 Expressive UI components.
  - Updates `app/src/main/res/values/strings.xml` and `app/src/main/res/values-es/strings.xml`.
- **Router Compatibility**:
  - Works entirely within the unprivileged `user` account of the DIGI TP-Link XGB430v Pro router without requiring operator `admin` credentials.
