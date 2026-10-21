# Tasks

## 1. Version Catalog Upgrade

- [x] 1.1 Update `material3` version from `1.5.0-beta01` to `1.5.0-beta02` in `gradle/libs.versions.toml` and verify Gradle sync resolves the artifact without dependency resolution conflicts.

## 2. Compilation and API Verification

- [x] 2.1 Compile `:app` with Kotlin 2.4.20 compiler and verify all Compose screens (including `QosScreen.kt`, `ParentalControlScreen.kt`, `Components.kt`, `MainActivity.kt`) compile without deprecation warnings or signature changes under `@ExperimentalMaterial3ExpressiveApi`.
- [x] 2.2 Run JVM unit tests via `./gradlew testDebugUnitTest` and verify all 114 tests in `:app` and 149 tests in `:protocol` pass cleanly.
