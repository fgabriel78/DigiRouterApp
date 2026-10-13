# Tasks

## 1. Pure Protocol Discovery Parsers & Signatures (:protocol)

- [x] 1.1 Implement pure Kotlin SSDP packet parser (`SsdpPacketParser.kt`) extracting `ST`, `USN`, `LOCATION`, and `SERVER` headers from UDP datagram payloads, and verify with unit tests in `./gradlew :protocol:test`.
- [x] 1.2 Implement lightweight mDNS record parser (`MdnsRecordParser.kt`) decoding DNS-SD PTR, SRV, and TXT key-value attributes from raw DNS packets, and verify with unit tests in `./gradlew :protocol:test`.
- [x] 1.3 Implement UPnP XML descriptor and HTTP metadata parser (`HttpDescriptorParser.kt`) extracting model and manufacturer fields, and verify with unit tests in `./gradlew :protocol:test`.
- [x] 1.4 Implement `DeviceSignatureMatcher.kt` mapping raw discovery attributes to exact hardware models (PlayStation 5, LG Smart TV, Philips Hue, Chromecast, etc.) and categories, and verify with comprehensive fixture unit tests in `./gradlew :protocol:test`.

## 2. Android Network Scanning & Persistence (:app)

- [x] 2.1 Add `android.permission.CHANGE_WIFI_MULTICAST_STATE` to `app/src/main/AndroidManifest.xml` and verify successful manifest compilation via `./gradlew processDebugManifest`.
- [x] 2.2 Implement `NetworkFingerprintScanner.kt` orchestrating `WifiManager.MulticastLock` with a 3.0s timeout window, SSDP M-SEARCH broadcast, mDNS listener, and bounded fallback HTTP probes (Semaphore 4, timeout 1000ms), and verify with JVM unit tests simulating socket responses.
- [x] 2.3 Extend `DeviceMetadataStore.kt` with fingerprint cache persistence keyed by client MAC address, and verify serialization and cache retrieval via `./gradlew testDebugUnitTest`.

## 3. Resolver Precedence & State Integration

- [x] 3.1 Update `DeviceResolver.kt` and `ResolvedDeviceInfo` to introduce `VendorSource.DEEP_FINGERPRINT` and exact model title, verifying precedence ladder (Custom Alias > Deep Fingerprint > Hardware OUI > Hostname Heuristics) in `DeviceResolverTest.kt`.
- [x] 3.2 Connect `NetworkFingerprintScanner` into the client devices pipeline in `RouterViewModel.kt`, triggering non-blocking scans upon host list refresh and emitting live updates via `StateFlow`, and verify state mapping with unit tests.

## 4. Material 3 Expressive UI & Localization

- [x] 4.1 Add bilingual strings for verified network fingerprint badges and indicators in `app/src/main/res/values/strings.xml` and `app/src/main/res/values-es/strings.xml`.
- [x] 4.2 Update `StatusScreens.kt` device cards to display the exact model title and a verified fingerprint badge chip when `vendorSource == VendorSource.DEEP_FINGERPRINT`.
- [x] 4.3 Update `DeviceDetailSheet.kt` to present the verified hardware model name, discovery source chip, and protocol details, and verify UI compilation with `./gradlew :app:assembleDebug`.

## 5. Integration Verification & Spec Coherence

- [x] 5.1 Execute all unit tests across both modules via `./gradlew testDebugUnitTest :protocol:test` to verify zero test regressions.
- [x] 5.2 Validate OpenSpec change coherence and schema compliance via `openspec validate`.
