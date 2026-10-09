# Tasks

## 1. OUI Vendor Resolution and Private MAC Detection

- [x] 1.1 Implement `OuiLookup` utility with curated consumer manufacturer database and IEEE locally administered / private MAC detection, verifying with unit tests in `app/src/test/` across known manufacturers (Apple, Sony, Samsung, Espressif) and randomized MAC addresses.

## 2. Persistent Device Metadata Storage

- [x] 2.1 Define `DeviceCategory` enum and `DeviceCustomMetadata` data model with JSON serialization.
- [x] 2.2 Implement `DeviceMetadataStore` backed by `SharedPreferences` providing get, save, and reset methods for MAC-keyed metadata, verifying with unit tests.

## 3. UI Resources and Device Detail Sheet

- [x] 3.1 Add string resources for device categories, vendor labels, private MAC badges, and bottom sheet actions in `app/src/main/res/values/strings.xml` and `app/src/main/res/values-es/strings.xml`.
- [x] 3.2 Implement `DeviceDetailSheet` composable in `app` featuring category selection chips, alias text field, detailed hardware/connection metrics (IP, MAC, vendor, signal, EasyMesh node), and save/reset actions.

## 4. DevicesScreen Integration and Verification

- [x] 4.1 Update device cards in `DevicesScreen` (`StatusScreens.kt`) to render custom alias (as primary title), vendor badge, and custom category icon, wiring card click events to open `DeviceDetailSheet`.
- [x] 4.2 Run unit tests and build check via `./gradlew test assembleDebug` to verify end-to-end compilation and test pass.
