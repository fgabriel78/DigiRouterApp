# Proposal

## Why

Home users frequently need secure remote access to their home LAN devices (NAS, local servers, IP cameras, home automation) and wish to tunnel mobile internet traffic securely through their home connection when connected to untrusted public Wi-Fi networks. The TP-Link router firmware provides embedded OpenVPN, IPSec, and PPTP server daemons alongside key regeneration actions, status reporting, and client configuration download endpoints, but these capabilities are currently inaccessible from RouterApp. Exposing these capabilities directly within the companion app empowers users to host and manage their personal VPN with one-tap configuration sharing on mobile.

## What Changes

- Add VPN Server configuration management to `Group.NETWORK`, restricted to the router `admin` role (`adminOnly = true`).
- Implement OpenVPN server control: enable/disable toggle, protocol selection (UDP vs TCP), listening port (default 1194), tunnel subnet/netmask, and client access mode (`LAN only` vs `Internet and LAN`).
- Provide cryptographic key regeneration (`ACT_OP_OVPN_REGEN_KEYS`) for OpenVPN with confirmation warning and visual indeterminate progress handling.
- Support downloading `.ovpn` configuration profiles via `/cgi/openvpn?cliOvpnDown` with pre-export endpoint inspection and optional host/domain override.
- Integrate one-tap mobile export using Android `FileProvider` and `Intent.ACTION_SEND` (Android Share Sheet targeting OpenVPN Connect, OpenVPN for Android, email, cloud storage) plus Storage Access Framework (SAF) file saving.
- Implement IPSec server and tunnel configuration management (`DEV2_IPSEC`, `DEV2_IPSEC_CFG`) and legacy PPTP server configuration (`DEV2_PPTP`).
- Provide a live VPN Client Status table displaying currently connected clients, assigned virtual IPs, public remote endpoints, and session uptime.
- Introduce network intelligence to detect whether the router WAN IP is behind Carrier-Grade NAT (CGNAT) and whether Dynamic DNS (DDNS) is active, offering an actionable guidance banner.
- Add complete bilingual string definitions (English and Spanish) across both modules.

## Capabilities

### New Capabilities
- `vpn-servers`: Covers home VPN server configuration (OpenVPN, IPSec, PPTP), cryptographic key lifecycle, live client status monitoring, CGNAT/DDNS readiness detection, and `.ovpn` mobile profile export with host override.

### Modified Capabilities
- `catalog-engine`: Exposes the new custom VPN page within `Group.NETWORK` restricted to the `admin` role in the catalog registry.

## Impact

- `:protocol`:
  * Add `VpnModels.kt` containing data models for OpenVPN, IPSec, PPTP configurations, and active client sessions.
  * Add `VpnEngine.kt` to coordinate TR-181 operations (`DEV2_OPENVPN`, `ACT_OP_OVPN_REGEN_KEYS`, `DEV2_IPSEC`, `DEV2_IPSEC_CFG`, `DEV2_PPTP`, and status objects).
  * Extend `RouterClient` with endpoints for raw/authenticated `.ovpn` download (`/cgi/openvpn?cliOvpnDown`) and key regeneration action triggers.
  * Register `Page(id = "vpn", title = "VPN Server", group = Group.NETWORK, ... adminOnly = true)` in `Catalog.kt` and add translations to `SpanishTexts.kt`.
- `:app`:
  * Add `VpnViewModel.kt` handling state flows, CGNAT/DDNS analysis, key regeneration progress, and profile transformation.
  * Add `VpnScreen.kt` featuring an M3 Expressive layout with CGNAT/DDNS status banner, segmented tabs (OpenVPN, Connected Clients, IPSec & PPTP), and bottom sheet for `.ovpn` export and host override.
  * Update `MainActivity.kt` navigation to route `"vpn"` page requests to `VpnScreen`.
  * Update `AndroidManifest.xml` and `res/xml/file_paths.xml` to ensure VPN profiles in cache/files directory can be safely shared via `FileProvider`.
  * Add bilingual string resources to `values/strings.xml` and `values-es/strings.xml`.
- Testing:
  * Unit tests in `:protocol` for VPN model serialization, `.ovpn` host replacement logic, and TR-181 mappings.
  * Unit tests in `:app` for `VpnViewModel` state transitions, CGNAT/DDNS heuristic evaluations, and error handling.
