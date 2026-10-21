# Proposal

## Why

During household bandwidth saturation (large game downloads, 4K streaming, cloud backups), latency-sensitive traffic such as online gaming, video conferencing, and remote work suffers from bufferbloat and packet queuing delays. The router firmware provides a hardware/software Intelligent QoS engine (`main_iqosSettings.htm` backed by `DEV2_IQOS`, `DEV2_IQOS_RULE`, and `DEV2_GAME_QOS_CLIENT`), but it is currently inaccessible from the mobile companion app and relies on a desktop-only drag-and-drop web UI that also suffers from `DEV2_HOST_ENTRY` list query limitations on this firmware. Exposing Intelligent QoS in RouterApp lets administrators cap upstream/downstream bandwidth, tune priority queue percentages, and assign individual devices (e.g., Gaming PC, work laptop, console) to High, Middle, or Low priority in a single tap.

## What Changes

- **Protocol QoS Engine & Models (`:protocol`)**:
  - Introduce `es.routerapp.protocol.qos` (`QosModels.kt` and `QosEngine.kt`) with a testable `QosRouterRpc` abstraction.
  - Read and update global QoS settings via `DEV2_IQOS`: master `enable` toggle, upstream and downstream total bandwidth caps (`upTotalBW`, `downTotalBW`) and units (`upUnit`, `downUnit` for `Mbps`/`Kbps`), and priority queue percentage allocations (`high`, `middle`, `low`), while preserving existing companion attributes (`iptv*`, `voIP*`, `guest*`, `linkType`).
  - Enforce firmware-aligned coupled queue slider constraints (`5%` step increments, `high >= 15%`, `middle >= 10%`, `low >= 5%`, `middle <= high - 5`, `low <= middle - 5`, and `high + middle + low <= 100%`) and bandwidth bounds (`1..10000`).
  - Manage per-device priority rules via `DEV2_IQOS_RULE` (`type = 11`, `class_` = `4` High, `3` Middle, `2` Low), supporting adding rules for default devices, replacing rules across priority classes (`delete` + `add`), and deleting rules to return a device to default Middle priority, followed by `ACT_SAVE_CFG` NVRAM persistence.
  - Query `DEV2_GAME_QOS_CLIENT` with graceful degradation (`runCatching`) to surface active hardware Game Port clients (`gamePortClient == "1" && active == "1" && isAgentDevice != "1"`) pinned to High Priority.
  - Register the custom `qos` page (`id = "qos"`, title `"Intelligent QoS"`, `group = Group.NETWORK`, `custom = true`, `adminOnly = true`) in `Catalog.kt` and add Spanish translations in `SpanishTexts.kt`.
- **Android QoS UI & ViewModel (`:app`)**:
  - Introduce `QosViewModel.kt` and `QosScreen.kt` using Material 3 Expressive components, wired into `MainActivity.kt`.
  - Join `DEV2_IQOS_RULE` and `DEV2_GAME_QOS_CLIENT` with `ConnectedDevices.load(client)` and `DeviceMetadataStore` so devices display their resolved custom aliases, categories (e.g. Gaming Console, Computer), IP/MAC addresses, and online status.
  - Provide an expressive control panel for global QoS enablement, upstream/downstream bandwidth caps with `Mbps`/`Kbps` unit selectors, coupled High/Middle/Low percentage sliders with live effective bandwidth readout, and 1-tap per-device priority tier assignment (`High`, `Middle`, `Low`), plus read-only/deletable visibility for any pre-existing non-device QoS rules.
  - Add complete English (`values/strings.xml`) and Spanish (`values-es/strings.xml`) localization strings.

## Capabilities

### New Capabilities
- `qos-management`: Global QoS engine configuration (`DEV2_IQOS`), coupled High/Middle/Low priority queue percentage allocation, per-device bandwidth prioritization (`DEV2_IQOS_RULE`), and hardware Game Port client detection (`DEV2_GAME_QOS_CLIENT`).

### Modified Capabilities
<!-- No existing capability requirements are modified; QoS is registered as a new admin-only custom page in the catalog. -->

## Impact

- **`:protocol` module**:
  - New package `es.routerapp.protocol.qos` (`QosModels.kt`, `QosEngine.kt`) and JVM unit tests (`QosEngineTest.kt`).
  - Updated `Catalog.kt` (adding `qos` page under `Group.NETWORK` with `adminOnly = true`) and `SpanishTexts.kt`.
- **`:app` module**:
  - New `QosViewModel.kt`, `QosScreen.kt`, and JVM unit tests (`QosViewModelTest.kt`).
  - Updated `MainActivity.kt` navigation routing and `strings.xml` (English and Spanish).
