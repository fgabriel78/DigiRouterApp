# Tasks

## 1. Engine & Heuristics Implementation

- [x] 1.1 Implement `DeviceHostnameHeuristics` in `app/src/main/kotlin/es/routerapp/app/DeviceHostnameHeuristics.kt` with regex/token rules for Apple, Samsung, Google, Xiaomi, Amazon, Nintendo, Sony, Microsoft, and PC/Android categories, and verify with unit tests in `DeviceHostnameHeuristicsTest.kt`.
- [x] 1.2 Implement the unified device resolution model and pipeline (`ResolvedDeviceInfo` / `VendorSource`), enforcing precedence (User Custom Metadata > Hardware OUI > Hostname Heuristic > Private MAC Flag > Fallback), and verify with unit tests.

## 2. Localization & Resources

- [x] 2.1 Add localized strings to `values/strings.xml` and `values-es/strings.xml` for inferred vendor labeling (e.g. `(detectado por nombre)`) and educational private MAC explanation, and verify string loading.

## 3. UI Integration

- [x] 3.1 Update `StatusScreens.kt` device cards to display inferred vendor badges and use inferred category icons as defaults when no custom category is configured, verifying device list rendering.
- [x] 3.2 Update `DeviceDetailSheet.kt` to display vendor source indication, pre-select inferred category in the category picker, and display an educational explanation for randomized private MAC addresses.

## 4. Verification & Testing

- [x] 4.1 Run `./gradlew testDebugUnitTest` and verify all tests pass without errors or regressions.
