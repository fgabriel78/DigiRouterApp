# Tasks

## 1. Protocol Catalog, Groups, Consolidation, and Localization (`:protocol`)

- [x] 1.1 Update `Group` titles in `protocol/src/main/kotlin/es/routerapp/protocol/pages/Spec.kt` (`ASSISTANT("Diagnostics")`, `NETWORK("Local network & storage")`, `NAT("Ports & remote access")`, `SECURITY("Security & family")`) and verify compilation of `:protocol`.
- [x] 1.2 Update `Catalog.kt` (`protocol/src/main/kotlin/es/routerapp/protocol/pages/Catalog.kt`) to register the `topology` page (`Group.STATUS`), consolidate micro-pages into multi-section pages (`guest` + `mssid`, `wifi-adv` + `wps`, `dhcp-static` + `arp`, `upnp` + `port-trigger`, `dmz` + `alg`, `time` + `led` + `stats`), reassign `storage` to `Group.NETWORK`, `ddns` and `vpn` to `Group.NAT`, and `system-log` to `Group.ASSISTANT`, ensure every page has a non-null `description`, and add legacy page ID alias resolution in `Catalog.byId()`. Verify by running `./gradlew :protocol:compileKotlin`.
- [x] 1.3 Add Spanish translations for all new/updated group titles, page titles, and page descriptions in `protocol/src/main/kotlin/es/routerapp/protocol/i18n/SpanishTexts.kt` and verify that `./gradlew :protocol:test --tests es.routerapp.protocol.CatalogI18nTest` passes with zero missing translations.
- [x] 1.4 Update `CatalogRoleTest.kt` (`protocol/src/test/kotlin/es/routerapp/protocol/pages/CatalogRoleTest.kt`) to verify the updated group assignments (`vpn` in `Group.NAT`, `system-log` in `Group.ASSISTANT`, `topology` in `Group.STATUS`), multi-section page consolidations, non-null page descriptions, and legacy alias resolution via `Catalog.byId()`, and verify with `./gradlew :protocol:test --tests es.routerapp.protocol.pages.CatalogRoleTest`.

## 2. Home Screen Quick Access, Category Layout, and Iconography (`:app`)

- [x] 2.1 Update `HomeScreen` and `QuickAccessTile` in `app/src/main/kotlin/es/digirouter/app/MainActivity.kt` so `quickAccessIds = setOf("dashboard", "devices", "topology", "speedtest")` renders the 4 `Group.STATUS` overview items in the 2x2 Bento grid (leaving `Main Wi-Fi` at the top of the `Wi-Fi` category card) and applies expressive badge colors for `"topology"`. Verify `:app` compiles cleanly.
- [x] 2.2 Update `pageIcon()` in `app/src/main/kotlin/es/digirouter/app/Components.kt` to provide dedicated Material icons for `"topology"` and `"system-log"`, and verify full unit test suite execution via `./gradlew testDebugUnitTest`.

## Workflow follow-up

- Archive the change with `/opsx-archive` after implementation and verification are complete.
