# Proposal

## Why

Users managing their home router need a direct and convenient way to measure their Wi-Fi connection quality and internet throughput without leaving the app or installing third-party ad-heavy speed test applications. Adding a built-in speed test provides immediate feedback on ping/latency, download speed, and upload speed directly within RouterApp.

## What Changes

- Add a native Wi-Fi/Internet speed test engine leveraging public, high-capacity, unauthenticated edge measurement endpoints (Cloudflare Speed Test service).
- Provide real-time measurement of:
  - Latency / ping (RTT in ms) and jitter (ms variation).
  - Download throughput (Mbit/s) with real-time transfer progress.
  - Upload throughput (Mbit/s) with real-time transfer progress.
  - Measurement metadata (public IP, server location / data center code if available).
- Introduce a dedicated, Material 3-styled speed test screen (`SpeedTestScreen`) featuring interactive test controls (Start / Stop), circular or linear progress indicators, real-time live speed metrics, and a summary card of previous test results.
- Register the Speed Test screen in `Catalog.pages` under `Group.STATUS` as a custom page, including Spanish translations in `SpanishTexts` to preserve localization coverage and test compliance.
- Wire navigation in `MainActivity.kt` (`AppNavigation`) to navigate to `SpeedTestScreen` when the speed test catalog entry is selected.

## Capabilities

### New Capabilities
- `network-speed-test`: End-to-end network performance testing measuring download throughput, upload throughput, ping latency, and jitter, with real-time progress reporting and reactive UI visualization.

### Modified Capabilities
*(None. Existing catalog, dashboard, and Wi-Fi management requirements remain unchanged.)*

## Impact

- **Code modules**:
  - `protocol`: Adds speed test client model and execution engine (`SpeedTestEngine`, `SpeedTestResult`, `SpeedTestProgress`) using existing OkHttp and Coroutines dependencies. Registers the `speedtest` page in `Catalog.kt` and translations in `SpanishTexts.kt`.
  - `app`: Adds `SpeedTestScreen.kt` with Material 3 expressive UI components; connects navigation in `MainActivity.kt`.
- **Dependencies**: No new external dependencies required; utilizes existing OkHttp (`okhttp3`) and Kotlin coroutines (`kotlinx-coroutines-core`, `kotlinx-coroutines-android`).
- **Permissions**: Requires existing `android.permission.INTERNET` (already declared for router communication).
- **Breaking changes**: None.
