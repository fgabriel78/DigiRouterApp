# Proposal

## Why

Network administrators and advanced users currently lack in-app visibility and control over the router's remote administration daemons, web ports, SSL certificate infrastructure, and ISP remote management mechanisms. Managing SSH/Telnet daemons, securing administrative web ports, and inspecting operator TR-069 ACS communication currently requires digging into hidden web interface menus or raw network probes. Exposing these administrative protocols securely within an admin-restricted screen strengthens operational control and provides immediate optical awareness of operator overrides.

## What Changes

- **Terminal Daemons Control (SSH & Telnet)**: Provide explicit administrative controls to inspect and toggle local and remote WAN access, configure ports (default 22/23 or custom high ports), and configure session lifetimes for SSH and Telnet via `DEV2_SSH_CFG` and `DEV2_TELNET_CFG`.
- **Remote Access High-Risk Safety Guardrails**: Render contextual Material 3 Expressive risk warning banners when remote WAN exposure is toggled without CIDR IP subnet filtering.
- **Web Administration Port Management**: Allow configuring local and remote HTTP (default 80) and HTTPS (default 443) ports, session timeouts, and display certificate serial numbers using `DEV2_HTTP_CFG` and `DEV2_SUPER_HTTP_CFG`.
- **Direct CA Certificate Installation**: Add a one-tap workflow to fetch the router's CA root certificate from `/cgi/https?downCACert` and trigger Android's native certificate installer via `KeyChain.createInstallIntent()`.
- **ISP Remote Management Audit (TR-069 & TR-369)**: Provide a read-only audit panel displaying operator Auto Configuration Server (ACS) connection status, bound interface (`pon.2` on GPON VLAN 2), periodic inform intervals, connection request port, and operator-whitelisted subnets (`188.26.208.0/26`, etc.). Clarify TR-369 status as unsupported/inactive in this firmware build.
- **Dedicated Admin-Only Navigation**: Add `remote-mgmt` under `Group.SYSTEM` in the navigation catalog, strictly visible to users authenticated with the `admin` role.

## Capabilities

### New Capabilities
- `remote-management`: Covers terminal daemons (SSH, Telnet), web management HTTP/HTTPS port controls, CA certificate acquisition and native Android KeyChain dispatch, and read-only operator TR-069 CWMP / TR-369 remote management inspection.

### Modified Capabilities
None.

## Impact

- **`:protocol` Module**:
  - Adds `RemoteManagementModels.kt` defining configurations for SSH, Telnet, HTTP, Super HTTP, and TR-069 audit.
  - Adds `RemoteManagementEngine.kt` implementing load, save, and raw CA certificate retrieval (`/cgi/https?downCACert`) via `RouterClient`.
  - Adds unit test suite `RemoteManagementEngineTest.kt` verifying serialization, deserialization, and safe defaults.
- **`:app` Module**:
  - Adds `RemoteManagementViewModel.kt` orchestrating state loading, validation, saving via recovery runner, and certificate intent emission.
  - Adds `RemoteManagementScreen.kt` using Jetpack Compose with Material 3 Expressive tokens, risk badges, and segmented Bento-style cards.
  - Updates `Catalog.kt` to register the `remote-mgmt` page in `Group.SYSTEM` with `adminOnly = true`.
  - Updates `MainActivity.kt` navigation routing for the `remote-mgmt` destination.
  - Adds bilingual localized string resources in English (`values/strings.xml`) and Spanish (`values-es/strings.xml`).
