# Proposal

## Why

Google has released `androidx.compose.material3:material3:1.5.0-beta02` on Google Maven, providing the latest stabilization, bug fixes, and performance improvements for Jetpack Compose and Material 3 Expressive components. Upgrading ensures RouterApp stays aligned with upstream platform fixes and the latest Compose Material 3 API surface.

## What Changes

- Upgrade `material3` dependency catalog version from `1.5.0-beta01` to `1.5.0-beta02` in `gradle/libs.versions.toml`.
- Verify full project build and Compose unit test suite to ensure API compatibility across all Material 3 Expressive components (`rememberSliderState`, `ShapeBadge`, expressive buttons, cards, and containers).

## Capabilities

### New Capabilities
None. This is a library dependency maintenance upgrade that introduces no new behavioral capabilities.

### Modified Capabilities
None. Existing behavioral specifications remain unchanged.

## Impact

- Affected files: `gradle/libs.versions.toml`.
- Dependencies: `androidx.compose.material3:material3` upgraded to `1.5.0-beta02`.
- Affected module: `:app` (Compose UI rendering and tests). No impact on `:protocol`.
