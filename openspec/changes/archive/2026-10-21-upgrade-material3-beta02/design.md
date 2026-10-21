# Design

## Context

See `proposal.md` for motivation. RouterApp manages UI components using Jetpack Compose and Material 3 Expressive (1.5.0 track). Dependency versions are managed through Gradle Version Catalogs in `gradle/libs.versions.toml`.

```
+-----------------------------------------------------------------------+
|                       Gradle Version Catalog                          |
|                     gradle/libs.versions.toml                         |
|                     [versions] material3 = "1.5.0-beta02"             |
+-----------------------------------------------------------------------+
                                   |
                                   v
+-----------------------------------------------------------------------+
|                           :app Module                                 |
|   androidx.compose.material3:material3:1.5.0-beta02                   |
|   +---------------------------------------------------------------+   |
|   |  Components.kt: ShapeBadge, Expressive Container Tokens       |   |
|   |  QosScreen.kt: rememberSliderState, Expressive Sliders        |   |
|   |  MainActivity.kt: NavigationSuite, TopAppBar, BottomSheet     |   |
|   +---------------------------------------------------------------+   |
+-----------------------------------------------------------------------+
```

## Goals / Non-Goals

**Goals:**
- Update `material3` catalog entry in `gradle/libs.versions.toml` to `1.5.0-beta02`.
- Verify binary and source compatibility for all Compose screens and custom Expressive UI tokens.
- Ensure JVM unit test suite (`./gradlew testDebugUnitTest`) compiles and passes without deprecation or runtime regressions.

**Non-Goals:**
- Adopting unreleased or separate Compose BOM upgrades that conflict with existing transitive dependencies.
- Refactoring existing screens or changing UI layouts beyond any required compatibility fixes for `1.5.0-beta02`.

## Decisions

### 1. Targeted Catalog Version Update
- **Decision:** Increment `material3 = "1.5.0-beta02"` in `gradle/libs.versions.toml` while keeping `composeBom = "2026.09.00"` intact.
- **Rationale:** The project explicitly manages the `material3` library override via `libs.androidx.compose.material3` referencing `version.ref = "material3"`.
- **Alternatives Considered:**
  - Upgrading the entire Compose BOM: Rejected, as BOM releases often follow separate monthly cadences and could introduce unnecessary churn across other stable compose runtime libraries.

### 2. Validation Strategy
- **Decision:** Run Gradle unit test tasks across the project (`./gradlew testDebugUnitTest`) and verify Kotlin compiler output for all Compose files.
- **Rationale:** Full compilation verifies that experimental expressive APIs (`rememberSliderState`, `ShapeBadge`, `surfaceContainerHigh`) maintain symbol and signature stability in `1.5.0-beta02`.

## Risks / Trade-offs

- **[Risk] Experimental API signature shift:** Beta releases can occasionally adjust experimental parameters (e.g., `rememberSliderState` positional vs named arguments).
  - *Mitigation:* Run `./gradlew testDebugUnitTest` immediately upon upgrading to identify any signature mismatch, with instant resolution in `QosScreen.kt` or `Components.kt`.
- **[Risk] Transitive compose compiler mismatch:**
  - *Mitigation:* Compose compiler plugin is driven by Kotlin 2.4.20 (`kotlin-compose` plugin), which supports modern Compose runtime semantics seamlessly.
