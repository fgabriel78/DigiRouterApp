# Design

## Context

See `proposal.md` for user motivation and problem statement.
The application currently divides responsibilities between `:protocol` (pure Kotlin, TR-181/OID data models, RouterClient HTTP communication) and `:app` (Android Jetpack Compose with Material 3 Expressive, ViewModels).
The router exposes administration daemons via `DEV2_SSH_CFG`, `DEV2_TELNET_CFG`, `DEV2_HTTP_CFG`, and `DEV2_SUPER_HTTP_CFG`. It also exposes root certificate download via `/cgi/https?downCACert`, and ISP CWMP (TR-069) state on the dedicated GPON VLAN 2 interface via `DEV2_MANAGEMENT_SERVER`. Firmware flags indicate `INCLUDE_CWMP=1` is active, whereas `INCLUDE_TR369=0` is disabled.

## Goals / Non-Goals

**Goals:**
- Provide a dedicated, high-security admin screen (`RemoteManagementScreen`) under `Group.SYSTEM` with Material 3 Expressive styling.
- Enable viewing and updating terminal daemon settings (SSH, Telnet) and administrative web ports (HTTP, HTTPS).
- Display contextual security risk warnings when WAN remote access is enabled without restrictive IP whitelisting.
- Streamline CA root certificate installation by integrating directly with Android's system `KeyChain.createInstallIntent()`.
- Deliver a read-only audit of operator TR-069 ACS settings, bound GPON interfaces, and operator management IP subnets.

**Non-Goals:**
- Allowing modification of operator TR-069 ACS endpoints or attempting to disable operator TR-069 provisioning (read-only audit by design).
- Implementing TR-369 / USP controls (the underlying router firmware disables USP).
- Exposing the remote management screen to standard (non-admin) user accounts.

## Decisions

### 1. Dedicated Hub Screen vs. Generic Catalog Form
* **Decision**: Implement a custom Jetpack Compose screen (`RemoteManagementScreen`) and dedicated `RemoteManagementViewModel` / `RemoteManagementEngine`, registered in `Catalog.kt` as `custom = true, adminOnly = true`.
* **Rationale**: Generic catalog forms (`GenericPageScreen`) cannot handle binary CA certificate streaming, intent-based KeyChain launching, or dynamic high-risk security warning banners for unconstrained WAN ports.
* **Alternative Considered**: Adding multiple declarative pages in `Catalog.kt`. Rejected because it fragments related security protocols across multiple screens and lacks native Android intent integration.

### 2. Android Certificate Installer Dispatch
* **Decision**: Use `android.security.KeyChain.createInstallIntent()`:
  ```kotlin
  val intent = KeyChain.createInstallIntent().apply {
      putExtra(KeyChain.EXTRA_CERTIFICATE, certBytes)
      putExtra(KeyChain.EXTRA_NAME, "Router-CA")
  }
  ```
* **Rationale**: This is the standard, secure Android framework mechanism for user-initiated CA certificate installation. It prompts the user with OS-level confirmation without requiring filesystem file-sharing permissions or external storage access.
* **Alternative Considered**: Saving the certificate to `Environment.DIRECTORY_DOWNLOADS` and asking the user to manually browse and import it. Rejected as high-friction and error-prone.

### 3. Graceful Partial Loading Architecture
* **Decision**: `RemoteManagementEngine.loadOverview()` fetches OIDs with graceful fallback: if `DEV2_SUPER_HTTP_CFG` or `DEV2_MANAGEMENT_SERVER` fails or returns 403, the remaining sections (SSH, Telnet, standard HTTP) still render successfully.
* **Rationale**: Conforms to the project architecture standard where dashboards and admin screens must tolerate partial metric/OID load failures without failing the whole screen.

### 4. Component Architecture & Data Flow

```
+----------------------------------------------------------------------------------------------------+
|                                      ARCHITECTURE & FLOW DIAGRAM                                   |
+----------------------------------------------------------------------------------------------------+
|                                                                                                    |
|  [ User: Admin ]                                                                                   |
|         |                                                                                          |
|         v                                                                                          |
|  +-----------------------------------------------------------------------------------------------+ |
|  | RemoteManagementScreen (Compose M3 Expressive)                                                | |
|  |   - Tab / Section 1: Daemons (SSH/Telnet switches, port inputs, session timeouts, risk alert) | |
|  |   - Tab / Section 2: Web Management & SSL (Ports 80/443, Cert status, "Install CA" action)    | |
|  |   - Tab / Section 3: ISP CWMP Audit (ACS URL, pon.2 binding, Inform interval, Operator CIDRs) | |
|  +-----------------------------------------------------------------------------------------------+ |
|         |                     ^                                  |                                 |
|         | Events (Save, Cert) | State (Loading/Success/Error)     | One-tap Install Cert            |
|         v                     |                                  v                                 |
|  +---------------------------------------------------------------+  +----------------------------+ |
|  | RemoteManagementViewModel                                     |  | Android KeyChain Intent    | |
|  |   - state: StateFlow<RemoteManagementUiState>                 |  | createInstallIntent()      | |
|  |   - events: SharedFlow<RemoteManagementEvent>                 |  +----------------------------+ |
|  +---------------------------------------------------------------+                                 |
|         |                                                                                          |
|         v                                                                                          |
|  +---------------------------------------------------------------+                                 |
|  | [:protocol] RemoteManagementEngine                            |                                 |
|  |   - loadOverview(): RemoteManagementOverview                  |                                 |
|  |   - saveSsh(SshConfig): Boolean                               |                                 |
|  |   - saveTelnet(TelnetConfig): Boolean                         |                                 |
|  |   - saveHttp(HttpConfig): Boolean                             |                                 |
|  |   - downloadCaCert(): ByteArray (raw GET /cgi/https?downCACert)|                                |
|  +---------------------------------------------------------------+                                 |
|         |                                                                                          |
|         v                                                                                          |
|  +---------------------------------------------------------------+                                 |
|  | RouterClient (HTTP POST dm.get / dm.set, GET /cgi/https)      |                                 |
|  +---------------------------------------------------------------+                                 |
+----------------------------------------------------------------------------------------------------+
```

## Risks / Trade-offs

- **[Risk] High-risk port exposure on WAN**: Enabling SSH (port 22) or Telnet (port 23) on the WAN interface without IP filtering makes the router vulnerable to internet credential brute-forcing.
  - **Mitigation**: The UI renders a prominent warning banner (`surfaceContainerHighest` with error container highlights) whenever WAN remote access is enabled with `remoteAll = 1`.
- **[Risk] Management lockout via invalid port configuration**: Setting the local HTTPS port to a blocked or conflicting port could disrupt web administration access.
  - **Mitigation**: Input validation enforces standard port ranges (`80`, `443`, or high unprivileged ports `1024..65535`), rejecting reserved internal ports.
- **[Risk] Inactive or unsupported TR-369**: Users might expect TR-369 configurations to be present.
  - **Mitigation**: The UI explicitly flags TR-369 as inactive in this firmware build (`INCLUDE_TR369=0`) with an informational chip badge.
- **[Risk] Session timeout or concurrent login conflict**: Saving management settings while another session is open or on connection drops.
  - **Mitigation**: ViewModel operations wrap calls with the existing `recoveryRunner` (`RouterViewModel`), automatically refreshing session tokens on expiry.

## Edge Cases

- **Dark Theme Contrast**: Color tokens use `MaterialTheme.colorScheme` semantic roles (`error`, `onErrorContainer`, `surfaceContainerHigh`) ensuring high contrast in both Light and Dark themes.
- **Offline / Partial Load State**: If the device loses connection while fetching CA certificate bytes, display an explicit inline error snackbar/banner with a Retry action.
- **Malformed CA Certificate Bytes**: If `/cgi/https?downCACert` returns an empty payload or error string, the ViewModel checks `certBytes.isNotEmpty()` before dispatching the KeyChain intent.
