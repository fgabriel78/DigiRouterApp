# Proposal

## Why

In the current connected devices screen, clients appear with cryptic, auto-generated hostnames (such as `android-93fae12` or `DESKTOP-4J8K`) or with no name at all, forcing users to identify devices solely by raw MAC or IP addresses. Additionally, hardware manufacturers are invisible, and randomized MAC addresses are indistinguishable from real hardware addresses.

Allowing users to assign friendly aliases (e.g. "Living Room TV"), select category icons (Phone, TV, Laptop, IoT, Console, etc.), and automatically resolving hardware vendors via offline IEEE OUI lookups transforms the devices view into a personalized, best-in-class home network inventory.

## What Changes

- **Offline OUI Vendor Lookup**:
  - Implement an offline MAC address parser and vendor lookup utility covering top consumer device manufacturers (Apple, Samsung, Google, Sony, Xiaomi, LG, Nintendo, Microsoft, Amazon, Espressif, Philips/Signify, Tuya, Intel, Realtek, TP-Link, Synology, Raspberry Pi, Sonos, etc.).
  - Detect IEEE 802 locally administered / randomized MAC addresses (bit 1 of the initial octet) and tag them as "Private MAC address" instead of unknown hardware.
- **Local Device Metadata Store**:
  - Introduce `DeviceMetadataStore` to persist user-assigned aliases, chosen category icons, and optional notes locally on the device (keyed by client MAC address).
- **Enriched Device Cards**:
  - Update device cards in `DevicesScreen`:
    - When an alias is configured: display the alias as the primary title, accompanied by a vendor tag/badge and the IP address.
    - When no alias is configured: display the router-reported hostname as the title with a vendor badge or "Private MAC" indicator.
    - Display category-specific icons (Phone, Tablet, Laptop/PC, Smart TV, Gaming Console, Smart Home/IoT, Printer, Network/AP) when customized.
- **Interactive Device Details Bottom Sheet**:
  - Tapping a device card opens a Material 3 `ModalBottomSheet` showing complete connection and hardware metrics (IP, MAC, vendor, EasyMesh AP node, Wi-Fi link speed, signal level) and allowing the user to edit the alias, pick a category icon, or reset custom metadata.

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `dashboard-status`: Enrich connected host discovery and inspection with offline OUI vendor resolution, private MAC identification, locally persistent device aliases and category icons, and an interactive device detail modal sheet.

## Impact
- **App Module**:
  - New `DeviceMetadataStore` (backed by `SharedPreferences`).
  - New `OuiLookup` utility with curated consumer manufacturer database.
  - Updates to `StatusScreens.kt` (`DevicesScreen`, device card composables, and new `DeviceDetailSheet`).
  - String resources in `values/strings.xml` and `values-es/strings.xml` for categories, badges, and editing actions.
- **Protocol Module**:
  - No breaking wire protocol or router API changes required; works entirely with existing client queries.
- **Dependencies**: No new external dependencies needed (standard Compose and Android APIs).
