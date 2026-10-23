# Proposal

## Why

The current Home screen menu scatters 28 items across 8 `Group` definitions with uneven granularity, router-firmware jargon, and accidental omissions—such as "Main Wi-Fi" disappearing from the Wi-Fi category because it is filtered into Quick Access, "Network Topology" being unreachable from the main menu, "USB and storage" rendering a single-item category card, and multiple 1-toggle screens (`WPS`, `ARP`, `LED`, `Traffic statistics`, `DMZ`) competing visually with full feature suites. Reorganizing the menu hierarchy around user intent and consolidating micro-pages into multi-section screens improves scannability, discoverability, and visual consistency.

## What Changes

- **Quick Access Bento Grid (`Group.STATUS`)**: Register `topology` ("Network topology") as a first-class custom `Page` in `Group.STATUS` and update the 2x2 Quick Access grid on `HomeScreen` to feature the 4 live status and overview screens (`dashboard`, `devices`, `topology`, `speedtest`). This keeps "Main Wi-Fi" (`wifi`) as the lead entry inside the **Wi-Fi** category card.
- **Micro-Page Consolidation (28 -> 20 Pages)**: Combine single-switch and closely related generic configuration sections into cohesive multi-section `Page` definitions while preserving all underlying TR-181 `Section` definitions:
  - Merge `mssid` into `guest` (*"Guest & extra networks"*).
  - Merge `wps` into `wifi-adv` (*"Advanced Wi-Fi & WPS"*).
  - Merge `arp` into `dhcp-static` (*"DHCP reservations & ARP"*).
  - Merge `port-trigger` into `upnp` (*"UPnP & port triggering"*).
  - Merge `alg` into `dmz` (*"DMZ & NAT gateways (ALG)"*).
  - Merge `led` and `stats` into `time` (*"System preferences"*).
- **Backward-Compatible Alias Resolution**: Update `Catalog.byId(id)` to resolve merged legacy page IDs (`mssid` -> `guest`, `wps` -> `wifi-adv`, `arp` -> `dhcp-static`, `port-trigger` -> `upnp`, `alg` -> `dmz`, `led` / `stats` -> `time`) so existing deep links and `SecurityAuditEngine` remediation targets (`targetScreenId`) continue to navigate to the appropriate screen without breaking.
- **Task-Oriented Category Regrouping & Jargon Removal**:
  - Rename *"Virtual servers"* (`virtual-servers`) to *"Port forwarding"* for clarity.
  - Move `ddns` (*"Dynamic DNS"*) and `vpn` (*"VPN Server"*) from `Group.NETWORK` to `Group.NAT` (*"Ports & remote access"*) alongside port forwarding, UPnP, and DMZ.
  - Move `storage` (*"File servers"*) into `Group.NETWORK` (*"Local network & storage"*), eliminating the orphaned single-item `STORAGE` card.
  - Move `system-log` (*"System Log"*) from `Group.SYSTEM` to `Group.ASSISTANT` (*"Diagnostics"*) alongside *Fiber Diagnostics* and *Dual Diagnostic*.
  - Refine group titles and ensure every `Page` in `Catalog.pages` has a descriptive subtitle (`description`) in both English and Spanish (`SpanishTexts`), and a dedicated icon in `pageIcon()`.

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `catalog-engine`: Update declarative catalog hierarchy, functional group assignments, multi-section page consolidations, `topology` page registration, bilingual dictionary entries, and legacy page ID alias resolution in `Catalog.byId()`.
- `form-ui`: Update the Home screen Quick Access 2x2 Bento grid items (`Summary`, `Connected devices`, `Network topology`, `Speed test`), category container card groupings, and page iconography mapping.

## Impact

- **`:protocol` module**:
  - `es.routerapp.protocol.pages.Spec`: Update `Group` enum titles to reflect task-oriented categories.
  - `es.routerapp.protocol.pages.Catalog`: Register `topology` page, consolidate micro-pages into multi-section pages, update `Group` assignments and descriptions, and add legacy ID alias lookup to `Catalog.byId()`.
  - `es.routerapp.protocol.i18n.SpanishTexts`: Add Spanish translations for new/updated group titles, page titles, and page descriptions.
  - Unit tests (`CatalogRoleTest`, `CatalogI18nTest`): Update group assertions and verify alias resolution and Spanish translation coverage.
- **`:app` module**:
  - `es.digirouter.app.MainActivity`: Update `quickAccessIds` in `HomeScreen` to `setOf("dashboard", "devices", "topology", "speedtest")` and update `QuickAccessTile` badge styling for `topology`.
  - `es.digirouter.app.Components`: Add dedicated icons for `topology` and `system-log` in `pageIcon()`, and align `CategoryItemRow` / `groupPalette` colors.
