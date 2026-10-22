# Design

## Context

The RouterApp codebase is strictly divided into two Gradle modules:
- `:protocol`: Pure Kotlin engine handling router communication (TR-181 OID models, PageEngine, Catalog, action calls `ACT_*`, device discovery) without Android framework dependencies.
- `:app`: Android application module hosting Jetpack Compose screens, ViewModels, and platform integrations (FileProvider, Intent Share Sheet, Storage Access Framework).

Home VPN servers in the TP-Link firmware are managed through TR-181 objects (`DEV2_OPENVPN`, `DEV2_IPSEC`, `DEV2_IPSEC_CFG`, `DEV2_PPTP`), actions (`ACT_OP_OVPN_REGEN_KEYS`), status tables (`DEV2_OPENVPN_STATUS`), and HTTP endpoints (`/cgi/openvpn?cliOvpnDown`).

See `proposal.md` for user motivation and problem statement.

## Goals / Non-Goals

**Goals:**
- Provide a dedicated, responsive Material 3 Expressive screen (`VpnScreen`) categorized under `Group.NETWORK` for managing OpenVPN, IPSec, and PPTP home servers.
- Implement an authenticated download flow in `:protocol` for the `.ovpn` configuration profile.
- Enable client endpoint inspection and optional hostname override in `:app` before exporting.
- Seamlessly export `.ovpn` profiles into the Android Share Sheet via `FileProvider` (`Intent.ACTION_SEND`) and offer direct file saving via Storage Access Framework (`CreateDocument`).
- Implement an interactive confirmation and progress flow for router cryptographic key regeneration (`ACT_OP_OVPN_REGEN_KEYS`).
- Display live connected client sessions with virtual IP assignments and connection durations.
- Automatically detect Carrier-Grade NAT (CGNAT) vs Public WAN IP and DDNS availability, presenting an actionable status banner.

**Non-Goals:**
- Embedding a full OpenVPN client connection tunnel engine inside the app (the app acts as the router server manager and profile exporter; actual VPN connections are handled by Android VPN clients such as *OpenVPN for Android* or *OpenVPN Connect*).
- Modifying router firmware cryptographic algorithms beyond the controls exposed by the router data model.

## Architecture & Data Flow

```
+----------------------------------------------------------------------------------+
|                                    UI LAYER (:app)                               |
|                                                                                  |
|  [VpnScreen]                                                                     |
|    |-- CGNAT / DDNS Readiness Banner                                             |
|    |-- Segmented Tabs: [OpenVPN] | [Connected Clients (N)] | [IPSec & PPTP]       |
|    +-- Profile Export Bottom Sheet (Host Override Field + Share / Save Actions)  |
|                                                                                  |
|  [VpnViewModel]                                                                  |
|    |-- State: Loading, Success(config, clients, wanState), RegeneratingKeys     |
|    |-- Overrides `remote <host> <port>` in .ovpn string                          |
|    +-- Writes to cacheDir/vpn/client.ovpn and dispatches Intent.ACTION_SEND       |
+----------------------------------------------------------------------------------+
                                        |
                                        v
+----------------------------------------------------------------------------------+
|                                PROTOCOL LAYER (:protocol)                        |
|                                                                                  |
|  [VpnEngine]                                                                     |
|    |-- loadOpenVpnConfig() / saveOpenVpnConfig() -> DEV2_OPENVPN                 |
|    |-- regenerateOpenVpnKeys()                   -> ACT_OP_OVPN_REGEN_KEYS       |
|    |-- loadVpnClientStatus()                     -> DEV2_OPENVPN_STATUS          |
|    +-- loadIpsecConfig() / loadPptpConfig()                                      |
|                                                                                  |
|  [RouterClient]                                                                  |
|    +-- downloadOpenVpnProfile(): String                                          |
|         Authenticated GET /cgi/openvpn?cliOvpnDown                               |
+----------------------------------------------------------------------------------+
                                        |
                                        v POST /cgi_gdpr?9 & Authenticated GET
+----------------------------------------------------------------------------------+
|                                  ROUTER FIRMWARE                                 |
+----------------------------------------------------------------------------------+
```

## Decisions

### 1. Separation of Profile Fetching and Host Override Logic
- **Decision:** `:protocol`'s `RouterClient.downloadOpenVpnProfile()` retrieves the raw `.ovpn` configuration text from `/cgi/openvpn?cliOvpnDown`. Host manipulation and validation reside in a pure Kotlin helper (`OpenVpnProfileTransformer`) in `:protocol` with zero Android dependencies, covered by unit tests.
- **Rationale:** Keeps `:protocol` completely testable on standard JVM unit test runners (`./gradlew testDebugUnitTest`) without mocking Android `Context` or `Uri`.
- **Alternatives Considered:** Performing regex replacements inside `:app` `ViewModel`. Placing the transformer in `:protocol` allows full testability and encapsulation of OpenVPN configuration parsing.

### 2. OpenVPN Remote Host Override Mechanism
- **Decision:** When downloading the profile, parse the `remote <host> <port>` directive using regex `(?m)^remote\s+(\S+)\s+(\d+)(.*)$`. If the user specifies an override host (such as a custom domain or DuckDNS hostname), rewrite that directive in-memory before exporting.
- **Rationale:** If the router is behind CGNAT or uses an external dynamic DNS service not configured on the router itself, the default `.ovpn` will contain an unreachable IP or domain. Allowing an optional override ensures the user can connect from outside.
- **Alternatives Considered:** Prompting the user to manually edit the file in a text editor after export. This creates friction and is error-prone on mobile devices.

### 3. Key Regeneration Lifecycle & State Transitions
- **Decision:** Provide an explicit two-step flow for `ACT_OP_OVPN_REGEN_KEYS`:
  1. Warning AlertDialog explaining that previous client certificates and profiles will be revoked.
  2. Loading overlay with indeterminate progress indicator while the router computes RSA keys (10–25s timeout).
- **Rationale:** Key generation on embedded router hardware is slow and blocking. Without explicit state management, users might assume the app has frozen.

```
 [Idle / Viewing Settings]
            |
            | Tap "Regenerate Keys"
            v
 [Confirmation Dialog]
     |              |
     | Cancel       | Confirm
     v              v
  [Idle]    [Regenerating State] (Indeterminate spinner + warning)
                    |
            +-------+-------+
            |               | Success
            v Failure       v
     [Error Snackbar]   [Success Snackbar] -> Profile cache invalidated
```

### 4. CGNAT & DDNS Detection Strategy
- **Decision:** Evaluate the active WAN IPv4 address obtained from `DEV2_WAN_IP_CONN` or router status.
  - Check if IPv4 matches the CGNAT block: `100.64.0.0/10` (first octet is 100, second octet between 64 and 127) or RFC 1918 private ranges.
  - Check whether DDNS (`DEV2_DDNS_CFG`) has an active enabled provider and domain.
  - If CGNAT without DDNS: show warning banner with shortcut button to open the `"ddns"` configuration screen.
- **Rationale:** Prevents user frustration where a properly configured home VPN fails silently due to carrier routing restrictions.

### 5. File Sharing & Storage Access
- **Decision:** Write the final `.ovpn` file to `context.cacheDir/vpn/client.ovpn`. Use `FileProvider.getUriForFile` with authority `${applicationId}.fileprovider`. Expose via `Intent(Intent.ACTION_SEND)` with MIME type `application/x-openvpn` and chooser title. Provide a secondary action to save directly to disk using `ActivityResultContracts.CreateDocument("application/x-openvpn")`.
- **Rationale:** Guarantees compatibility with Android 7.0+ strict mode, Android 11+ package visibility, and Android 14+ share sheet improvements.

## Edge Cases & Mitigations

- **Dark Theme Contrast:** Profile preview and code snippets must be rendered inside semantic surface containers (`surfaceContainerHighest`) with monospace typography and appropriate contrast.
- **Session Expiry During Key Regeneration:** If the router drops the session during the long-running key generation, `RouterViewModel` recovery runner intercepts the failure and attempts reconnection.
- **Invalid Port or Subnet Inputs:** Validation rules enforce integer port between 1 and 65535, non-overlapping subnet with home LAN (e.g. warning if OpenVPN subnet `192.168.1.0/24` collides with router LAN `192.168.1.0/24`).
- **Offline / Disconnected Router:** Graceful empty/error states with retry options.

## Migration Plan

No database migrations or breaking changes required. The new feature registers as an admin-only page within `Catalog` and attaches to the existing `MainActivity` Compose navigation tree.
