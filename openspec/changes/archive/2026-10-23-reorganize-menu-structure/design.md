# Design

## Context

See `proposal.md` for motivation. The app's navigation menu is driven declaratively by `Catalog.pages` (`:protocol` module) and rendered by `HomeScreen` in `MainActivity.kt` (`:app` module). Currently:
- `HomeScreen` defines `quickAccessIds = setOf("dashboard", "devices", "speedtest", "wifi")` and filters those IDs out of `categoryGroups`, causing `"wifi"` (*Main Wi-Fi*) to disappear from the **Wi-Fi** category card and `Group.STATUS` to become empty.
- `TopologyScreen` is wired in `AppNavigation` (`id == "topology"`), but has no `Page` entry in `Catalog.pages`.
- Six generic pages (`wps`, `mssid`, `arp`, `port-trigger`, `alg`, `led`, `stats`) expose only a single TR-181 section (often a single switch) as a top-level menu row.
- `SecurityAuditEngine` emits `RemediationAction` items with `targetScreenId` values (`"dmz"`, `"wifi"`, `"wps"`, `"guest"`, `"upnp"`, `"virtual-servers"`, `"ddos"`, `"storage"`) that `SecurityHealthScreen` passes to `onNavigateToPage` -> `AppNavigation` -> `Catalog.byId(id)`.

## Goals / Non-Goals

**Goals:**
- Reorganize the Home screen into 4 `Group.STATUS` Quick Access tiles (`dashboard`, `devices`, `topology`, `speedtest`) and 6 task-oriented category cards (`WIFI`, `NETWORK`, `NAT`, `SECURITY`, `ASSISTANT`, `SYSTEM`).
- Consolidate micro-pages into cohesive multi-section `Page` definitions without altering any TR-181 OIDs, `Section` definitions, `saveHook` functions, or `afterSave` triggers.
- Preserve backward compatibility for deep links and `SecurityAuditEngine` `targetScreenId` references via alias resolution in `Catalog.byId()`.
- Ensure every `Page` in `Catalog.pages` has a non-null `description`, complete Spanish translations in `SpanishTexts`, and a dedicated icon in `pageIcon()`.

**Non-Goals:**
- Changing the single-screen vertical scroll layout to a multi-tab bottom navigation bar.
- Altering custom screen implementations (`ParentalControlScreen`, `QosScreen`, `VpnScreen`, `SecurityHealthScreen`, `FiberDiagnosticScreen`, `DualDiagnosticScreen`, `SystemLogScreen`, `RemoteManagementScreen`, `BackupRestoreScreen`, `TopologyScreen`).
- Modifying TR-181 data models or `PageEngine` read/write logic.

## Decisions

### 1. Quick Access Aligned with `Group.STATUS` (`dashboard`, `devices`, `topology`, `speedtest`)
- **Decision**: Register `topology` (`id = "topology"`, `title = "Network topology"`, `group = Group.STATUS`, `custom = true`) in `Catalog.pages` and set `quickAccessIds = setOf("dashboard", "devices", "topology", "speedtest")`.
- **Rationale**: All four Quick Access tiles now represent live network status and inspection screens (`Group.STATUS`). Because `"wifi"` is no longer in `quickAccessIds`, *Main Wi-Fi* naturally renders as the first row inside the **Wi-Fi** category card right below Quick Access, fixing the missing primary Wi-Fi entry without duplicating items on the Home screen.
- **Alternatives Considered**: Keeping `"wifi"` in Quick Access and removing `.filter { it.id !in quickAccessIds }` for `"wifi"` only—rejected because it duplicates *Main Wi-Fi* twice above the fold and leaves `Network topology` hidden inside secondary screens.

### 2. Multi-Section Page Consolidation with Legacy ID Aliasing
- **Decision**: Combine closely related `Section` objects into existing parent `Page` definitions and add an `aliases: Map<String, String>` lookup table inside `Catalog.byId(id)`:
  - `guest` (*"Guest & extra networks"*) -> sections: `guest`, `mssid(1)`, `mssid(2)` (alias: `"mssid"` -> `"guest"`)
  - `wifi-adv` (*"Advanced Wi-Fi & WPS"*) -> sections: `adv`, `steering`, `wps` (alias: `"wps"` -> `"wifi-adv"`)
  - `dhcp-static` (*"DHCP reservations & ARP"*) -> sections: `static`, `arp` (alias: `"arp"` -> `"dhcp-static"`)
  - `upnp` (*"UPnP & port triggering"*) -> sections: `upnp`, `upnpmap`, `pt` (alias: `"port-trigger"` -> `"upnp"`)
  - `dmz` (*"DMZ & NAT gateways (ALG)"*) -> sections: `dmz`, `alg` (alias: `"alg"` -> `"dmz"`)
  - `time` (*"System preferences"*) -> sections: `time`, `led`, `stat` (aliases: `"led"` -> `"time"`, `"stats"` -> `"time"`)
- **Rationale**: `GenericPageScreen` and `PageEngine` already load and save each `Section` in `page.sections` independently with its own card, save button, `saveHook`, and `saveWarning`. Merging sections at the `Page` level reduces menu clutter from 28 to 20 pages while keeping each section's persistence and error handling completely isolated. Adding alias resolution in `Catalog.byId()` ensures `SecurityAuditEngine` findings (e.g., `targetScreenId = "wps"`) navigate seamlessly to the consolidated screen.
- **Alternatives Considered**: Updating `SecurityAuditEngine` only without `Catalog.byId()` aliases—rejected because supporting aliases in `Catalog.byId()` is trivial, defensive, and protects any saved navigation state or caller using the old IDs.

```text
+-------------------------------------------------------------------------+
|                    NAVIGATION & ALIAS RESOLUTION FLOW                   |
+-------------------------------------------------------------------------+

  [HomeScreen]                      [SecurityHealthScreen / Deep Link]
       |                                            |
       | onOpen(page)                               | onNavigateToPage("wps")
       v                                            v
  +--------------------------------------------------------------------+
  |                     AppNavigation (pageId)                         |
  +--------------------------------------------------------------------+
                                    |
                                    v
  +--------------------------------------------------------------------+
  |                     Catalog.byId(id: String)                       |
  |   1. Direct match in Catalog.pages (e.g. "wifi-adv", "topology")   |
  |   2. Fallback via alias map ("wps" -> "wifi-adv", "arp" -> ...)    |
  +--------------------------------------------------------------------+
                  |                                   |
        (custom == true)                    (custom == false)
                  v                                   v
     [Dedicated Custom Screen]              [GenericPageScreen]
     (e.g. TopologyScreen,                  (Iterates page.sections,
      VpnScreen, QosScreen)                  loads/saves each Section
                                             independently via PageEngine)
```

### 3. Six Task-Oriented Category Groups
- **Decision**: Organize the 16 non-status pages into 6 category groups on the Home screen:
  1. `Group.WIFI` (*"Wi-Fi"*): `wifi`, `guest`, `wifi-radio`, `wifi-adv`
  2. `Group.NETWORK` (*"Local network & storage"*): `lan`, `dhcp-static`, `qos` (admin), `storage`
  3. `Group.NAT` (*"Ports & remote access"*): `virtual-servers` (*"Port forwarding"*), `upnp`, `dmz`, `ddns`, `vpn` (admin)
  4. `Group.SECURITY` (*"Security & family"*): `security-health`, `parental-control` (admin), `ddos`
  5. `Group.ASSISTANT` (*"Diagnostics"*): `fiber-diag`, `dual-diag`, `system-log` (admin)
  6. `Group.SYSTEM` (*"System"*): `time` (*"System preferences"*), `backup`, `remote-mgmt` (admin), `reboot`
- **Rationale**:
  - Absorbing `storage` into `Group.NETWORK` eliminates the 1-item `STORAGE` category card (the `Group.STORAGE` enum entry can be retained if needed for binary compatibility or removed if unused outside `Catalog`/`Components`).
  - Moving `ddns` and `vpn` into `Group.NAT` (*"Ports & remote access"*) co-locates all WAN/inbound reachability features (`VpnScreen` already cross-links to `ddns`).
  - Moving `system-log` into `Group.ASSISTANT` (*"Diagnostics"*) groups all troubleshooting tools together while keeping **Diagnostics** and **System** as separate groups.

### 4. Visual Consistency, Dark Theme Contrast, and Edge Cases
- **Subtitle Consistency**: Every `Page` in `Catalog.pages` is given a concise 1-line `description` so all rows inside `CategoryGroupContainer` have uniform two-line height and visual rhythm.
- **Iconography & Expressive Tokens**:
  - `pageIcon()` in `Components.kt` is updated to map `"topology"` (e.g. `Icons.Filled.AccountTree` or `Icons.Filled.Hub` / `Icons.Filled.Lan`) and `"system-log"` (e.g. `Icons.Filled.Description` or `Icons.Filled.ListAlt` / `Icons.Filled.BarChart`).
  - `QuickAccessTile` in `MainActivity.kt` maps `"topology"` to `c.primaryContainer to c.onPrimaryContainer` (matching the 2x2 Bento palette in both light and dark themes).
- **Partial Section Load Failure / Offline Handling**: In `GenericPageScreen`, each `Section` in a consolidated `Page` loads its OIDs (`PageEngine.load(section)`) and handles save operations independently; a failure or warning on one section (e.g., `mlo` or `wps`) does not corrupt edits in sibling sections.
- **Credential Masking**: Merging `mssid(1)` and `mssid(2)` into `guest` preserves `FieldType.Secret` (`pwd(...)`) on `guestPSK`, `mssid1PSK`, and `mssid2PSK`, maintaining masked passphrases with explicit reveal toggles and QR sharing hooks.
- **Role-Based Filtering (`user` vs `admin`)**: None of the merged sections mix `adminOnly = true` and `adminOnly = false` pages—all 5 `adminOnly` pages (`qos`, `vpn`, `parental-control`, `system-log`, `remote-mgmt`) are custom screens that remain standalone `Page` entries. Even when logged in as `"user"`, every one of the 6 category groups has at least 2 visible items (no empty or 1-item category cards).

```text
+-------------------------------------------------------------------------+
|               VISIBLE ITEMS PER GROUP BY ROLE (USER vs ADMIN)           |
+-------------------------------------------------------------------------+
  Group                        User Role Count    Admin Role Count
  -------------------------    ---------------    ----------------
  Quick Access (STATUS)               4                  4
  1. Wi-Fi (WIFI)                     4                  4
  2. Local network & storage          3                  4 (+QoS)
  3. Ports & remote access            4                  5 (+VPN)
  4. Security & family                2                  3 (+Parental)
  5. Diagnostics                      2                  3 (+System Log)
  6. System                           3                  4 (+Remote Mgmt)
  -------------------------    ---------------    ----------------
  Total Visible Pages                22                 27 ( Wait: 15+5=20! )
```
*(Correction in table counts: 4 Quick Access + 4 Wi-Fi + 3/4 Network + 4/5 NAT + 2/3 Security + 2/3 Diagnostics + 3/4 System = 15 visible for `user`, 20 visible for `admin`—every group has >= 2 items in both roles!)*

## Risks / Trade-offs

- **[Risk] Longer scroll on consolidated pages (`guest` with 3 sections, `time` with 3 sections)** → **Mitigation**: `GenericPageScreen` uses a `LazyColumn` where each `Section` renders as a distinct titled card. Having 2–3 related cards on one screen is standard across the app (e.g., `storage` already renders 3 sections: Samba, FTP, and DLNA) and avoids jumping back and forth to the Home menu.
- **[Risk] Missing Spanish translations for renamed titles or new descriptions** → **Mitigation**: `CatalogI18nTest` automatically inspects every `Group.title`, `Page.title`, `Page.description`, `Section`, and `Field` in `Catalog` and fails the build if any string lacks an entry in `SpanishTexts.table`.
- **[Risk] Existing unit tests asserting old group assignments (e.g., `CatalogRoleTest` checking `vpn.group == Group.NETWORK`)** → **Mitigation**: Update `CatalogRoleTest` alongside the `Catalog` changes to assert the new group assignments and add tests for `Catalog.byId()` legacy alias resolution.
