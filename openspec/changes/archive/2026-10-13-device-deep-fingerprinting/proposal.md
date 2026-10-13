# Proposal: Deep Network Fingerprinting (Escaneo de huella digital profunda)

## Why

Currently, device identification on the network relies on offline IEEE OUI lookups (which fail on locally administered randomized MAC addresses) and DHCP hostname heuristics (which fail when clients report opaque strings like `android-xxxx` or blank hostnames). 

By performing lightweight local discovery via mDNS (Bonjour), SSDP (UPnP), and targeted HTTP service probes when the user visits device and topology screens, the application can definitively identify common smart devices (e.g., PlayStation 5, LG webOS TVs, Philips Hue Bridges, Chromecast, smart speakers) with exact model names and accurate categories without requiring manual user aliasing.

## What Changes

- **Automatic Hybrid Discovery Engine**: Automatically initiates a non-blocking background scan when navigating to the Connected Devices dashboard or Topology Explorer.
- **Transient Multicast Burst (mDNS + SSDP)**: Safely acquires an Android `WifiManager.MulticastLock` for an exact 3-second burst to broadcast SSDP `M-SEARCH` (239.255.255.250:1900) and listen for mDNS announcements (224.0.0.251:5353), releasing the lock immediately upon completion.
- **Targeted Fallback Probes**: Performs lightweight, concurrent (max 4 parallel workers) HTTP checks with strict timeouts (800ms–1200ms) against candidate ports (e.g. 80, 8008, 8080) for unresolved hosts to inspect UPnP XML descriptors (`/description.xml`), Google Cast info (`/setup/eureka_info`), and IoT status endpoints.
- **Precedence & Resolver Expansion**: Extends `DeviceResolver` with `VendorSource.DEEP_FINGERPRINT` which supersedes hostname heuristics and OUI vendor names while still respecting explicit user custom aliases.
- **Local Fingerprint Caching**: Caches resolved hardware fingerprints locally by client MAC address so subsequent sessions display exact models instantly with zero network overhead.
- **Expressive UI Integration**: Displays exact model titles, automated device categories, and a verified network fingerprint badge in both `StatusScreens` device cards and `DeviceDetailSheet`.

## Capabilities

### Modified Capabilities
- `dashboard-status`: Enhances connected host discovery with deep local network fingerprinting, verified exact model naming, and fingerprint badge indicators.

## Impact

- **Architecture & Modules**:
  - `:protocol`: Implements pure Kotlin SSDP packet builders/parsers, mDNS record extractors (PTR/SRV/TXT), UPnP descriptor XML parsers, and a device signature matching catalog with zero Android dependencies.
  - `:app`: Implements `NetworkFingerprintScanner` orchestrating Android coroutines, `WifiManager.MulticastLock`, `DeviceResolver` integration, local caching in `DeviceMetadataStore`, and Material 3 Expressive UI badges.
- **Permissions**: Adds `android.permission.CHANGE_WIFI_MULTICAST_STATE` in `app/src/main/AndroidManifest.xml`.
- **Localization**: Adds bilingual strings for fingerprint status and badges in `values/strings.xml` and `values-es/strings.xml`.
- **Testing**: Adds comprehensive JVM unit tests for SSDP/mDNS packet parsing, HTTP signature rules, and resolver precedence.
