# Design: Deep Network Fingerprinting Architecture

## Context

See `proposal.md` for background and motivation.

Currently, `:app` resolves device identity via `DeviceResolver.kt` by combining `OuiLookup.kt` (IEEE MAC prefixes) and `DeviceHostnameHeuristics.kt` (DHCP client hostnames). When connected to a local subnet, the router table provides client IPs and MACs. The mobile phone can communicate directly over the local Wi-Fi link to inspect common discovery protocols (mDNS on UDP 5353, SSDP on UDP 1900, and HTTP on ports 80/8008/8080).

## Goals / Non-Goals

**Goals:**
- Provide definitive hardware model names (e.g. "PlayStation 5", "LG OLED55C2", "Philips Hue Bridge", "Chromecast with Google TV") for common local network devices.
- Execute an automatic, non-blocking background discovery burst upon entering Connected Devices and Topology Explorer screens.
- Maintain battery efficiency by constraining multicast packet reception to an exact 3-second window.
- Persist discovered fingerprints locally by MAC address to eliminate redundant scans.
- Adhere strictly to the project architecture: pure Kotlin parsers and signature rules in `:protocol`, Android framework integrations and UI in `:app`.

**Non-Goals:**
- Full subnet port scanning or arbitrary vulnerability assessment.
- Continuous background daemon execution when the application is in the background or killed.
- Invasive or credentialed device probing (all queries are public standard discovery announcements or public metadata endpoints).

## Architecture Diagrams

### Component Architecture & Module Separation

```text
+--------------------------------------------------------------------------+
|                               MODULE: :app                               |
|                                                                          |
|  [StatusScreens / TopologyScreen]                                        |
|         | (LaunchedEffect on screen entry)                               |
|         v                                                                |
|  [NetworkFingerprintScanner]                                             |
|         | - Manages WifiManager.MulticastLock (3s timeout)               |
|         | - CoroutineScope(Dispatchers.IO)                               |
|         +-----------------------+-----------------------+                |
|                                 |                       |                |
|                                 v                       v                |
|                    [UDP Multicast Sockets]     [HTTP Prober (OkHttp)]    |
|                    - 239.255.255.250:1900      - Target IP endpoints     |
|                    - 224.0.0.251:5353          - Concurrency limit: 4    |
|                                 |                       |                |
+---------------------------------+-----------------------+----------------+
                                  |                       |
                                  | Raw bytes / headers   | Response text
                                  v                       v
+--------------------------------------------------------------------------+
|                             MODULE: :protocol                            |
|                                                                          |
|  [SsdpPacketParser]          [MdnsRecordParser]      [HttpBannerParser]  |
|   - Parses ST, USN,           - Parses PTR, SRV,      - UPnP XML models  |
|     SERVER, LOCATION            TXT key-values        - Cast eureka info |
|         |                            |                          |        |
|         +----------------------------+--------------------------+        |
|                                      |                                   |
|                                      v                                   |
|                        [DeviceSignatureMatcher]                          |
|                         - Maps signatures to:                            |
|                           * exactModel: String                           |
|                           * vendor: String                               |
|                           * category: DeviceCategory                     |
+--------------------------------------+-----------------------------------+
                                       |
                                       v
+--------------------------------------------------------------------------+
|                               MODULE: :app                               |
|                                                                          |
|  [DeviceMetadataStore] <-----+ [DeviceResolver]                          |
|   - Persists fingerprint       - Evaluates precedence:                   |
|     cache by MAC                 1. Custom user alias                    |
|                                  2. DEEP_FINGERPRINT                     |
|                                  3. HARDWARE_OUI                         |
|                                  4. HOSTNAME_HEURISTIC                   |
|                                  5. NONE / IP / MAC                      |
+--------------------------------------------------------------------------+
```

### State Machine of Device Identification

```text
       +-----------------------+
       |   Device Discovered   |
       |  (IP, MAC, Hostname)  |
       +-----------+-----------+
                   |
                   v
       +-----------------------+         YES       +---------------------+
       | Is MAC in Local Cache?| ----------------> | Immediate Display   |
       +-----------+-----------+                   | (0ms Network delay) |
                   | NO                            +---------------------+
                   v
       +-----------------------+
       | Multicast Window (3s) |
       | - SSDP M-SEARCH       |
       | - mDNS listener       |
       +-----------+-----------+
                   |
        +----------+----------+
        |                     |
        | Match found         | No multicast match
        v                     v
+----------------+   +-----------------------+
| Extract Model  |   | Targeted HTTP Fallback|
| & Category     |   | - Max 4 concurrent    |
+-------+--------+   | - Timeout: 1000ms     |
        |            +-----------+-----------+
        |                        |
        |            +-----------+-----------+
        |            | Match     | No match  |
        |            v           v           |
        |    +----------------+ +----------+ |
        +--> | Store in Cache | | Retain   | <+
             | & Emit State   | | Heuristic|
             +----------------+ +----------+
```

## Decisions

### 1. Protocol Parsing in `:protocol` vs Execution in `:app`
- **Decision**: Implement all packet encoders/decoders (`SsdpPacketParser`, `MdnsRecordParser`, `HttpDescriptorParser`, `DeviceSignatureMatcher`) as pure Kotlin in `:protocol`. Implement Android networking (`WifiManager.MulticastLock`, DatagramSockets, coroutine dispatcher orchestration) in `:app`.
- **Rationale**: Complies with the architectural constraint in `openspec/config.yaml`. Allows complete unit testing of discovery protocols in JVM without Android mocks.
- **Alternatives Considered**: 
  - Using Android's `NsdManager`: Rejected due to known historical OEM reliability issues, listener leaks, and inability to control raw SSDP/UPnP discovery.
  - Putting all networking in `:app`: Rejected because it would mix protocol serialization logic with Android framework lifecycle.

### 2. Transient Lifecycle for `WifiManager.MulticastLock`
- **Decision**: Acquire `MulticastLock` only during an active 3.0-second discovery window, enclosed in `try { ... } finally { lock.release() }`.
- **Rationale**: Multicast packet filtering is Android's primary mechanism for Wi-Fi radio power saving. Leaving the lock active drains battery rapidly.
- **Alternatives Considered**:
  - Continuous listening while screen is active: Rejected due to unacceptable battery impact on mobile devices.
  - No MulticastLock (unicast only): Rejected because mDNS (224.0.0.251) and SSDP (239.255.255.250) multicasts are dropped by Android without it.

### 3. Bounded Fallback HTTP Probing
- **Decision**: Perform targeted HTTP queries only for connected IP addresses that remain unidentified after the multicast burst, constrained by a coroutine `Semaphore(4)` and a strict 1000ms timeout per probe.
- **Rationale**: Many IoT and media devices (such as Google Cast on port 8008 or Philips Hue on port 80) expose rich JSON/XML metadata on HTTP, but blind or unbounded socket sweeps would freeze coroutine threads and congest router ARP tables.
- **Alternatives Considered**:
  - Probing every IP simultaneously: Rejected because 50+ clients on a LAN would cause socket exhaustion and radio spikes.

### 4. Cache Persistence in `DeviceMetadataStore`
- **Decision**: Persist deep fingerprint records (`FingerprintRecord(exactModel, vendor, category, timestamp)`) keyed by client MAC address in `DeviceMetadataStore`.
- **Rationale**: Device hardware models do not change. Caching by MAC ensures that subsequent screen visits display exact device names instantly without repeated radio bursts.
- **Alternatives Considered**:
  - Memory-only cache: Rejected because users would see device names revert to hostnames or IPs on every app relaunch until the 3-second scan completed.

### 5. Extended Resolver Precedence
- **Decision**: Place `VendorSource.DEEP_FINGERPRINT` below user custom alias/category, but above `HARDWARE_OUI` and `HOSTNAME_HEURISTIC`.
- **Rationale**: Deep fingerprinting yields verified model names (e.g., "LG OLED55C2"), whereas OUI only yields the raw silicon or board manufacturer (e.g., "LG Innotek" or "Murata Manufacturing") and hostname heuristics are only probabilistic pattern matches. Custom user aliases must always reign supreme.

## Edge Cases & Error Handling

- **Wi-Fi Not Connected or Cellular/VPN Active**: If `WifiManager` indicates no active Wi-Fi connection or local IP is in a VPN tunnel, bypass the multicast scan gracefully and log a debug message without throwing errors.
- **Device Offline or Sleeping (Packet Drop)**: Sockets and HTTP connections employ non-blocking IO with aggressive timeouts (1000ms). If a device fails to respond, it cleanly falls back to existing hostname heuristics without blocking other devices.
- **MulticastLock Failure**: If `WifiManager` throws a `SecurityException` or fails to acquire the lock, log the warning and execute only the unicast HTTP fallback.
- **Dark Theme Contrast**: Badges for verified deep fingerprinting use Material 3 Expressive semantic tokens (`surfaceContainerHighest`, `tertiaryContainer`, `onTertiaryContainer`) to ensure high contrast in both Light and Dark themes.

## Risks / Trade-offs

- **[Risk] Multicast blocked by AP Isolation or Mesh guest network** → *Mitigation*: If the router isolates wireless clients from each other, multicast packets will not reach the phone. Fallback HTTP queries directly to the client's router-assigned IP mitigate this, and existing OUI/heuristic resolution remains intact.
- **[Risk] Android cleartext HTTP restriction** → *Mitigation*: The app already configures local IP ranges in `network_security_config.xml` and validates local RFC1918 ranges via `LocalNetworkValidator`.
