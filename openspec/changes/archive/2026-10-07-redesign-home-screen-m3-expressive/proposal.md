# Proposal

## Why

The current home screen organizes all 23 router configuration pages into a flat, uniform two-column grid of square cards. This causes uneven card heights when text wraps to multiple lines, leaves single "orphan" cards stranded on the left with empty spaces when categories contain odd item counts, and lacks visual hierarchy by giving obscure network protocols the same prominence as everyday essentials.

## What Changes

- Redesign the home screen into a structured Material Design 3 Expressive layout with clear informational hierarchy.
- Add an Expressive Hero Status Card at the top displaying the router model (`DIGI XGB430v Pro`), connection state, IP address, and a direct tap target into full system diagnostics.
- Introduce an equal-height 2x2 Quick Access Bento Grid for the 4 primary daily features: *Summary*, *Connected Devices*, *Speed Test*, and *Main Wi-Fi*.
- Replace the fragmented 2-column tiles for remaining configuration categories with elegant M3 Expressive Grouped Container Cards, presenting items in clean horizontal list rows with colorful shape badges and navigation chevrons.
- Adopt M3 tonal surface container styling (`surfaceContainer` / `surfaceContainerLow`) with vibrant tinted icon badges across all home screen components.

## Capabilities

### New Capabilities
*(None)*

### Modified Capabilities
- `form-ui`: Update screen rendering requirements to define the Expressive home dashboard layout, including the Hero status glance, equal-height quick access bento grid, and grouped category container cards.

## Impact

- **Affected code**:
  - `app`: `MainActivity.kt` (`HomeScreen`, `PageTile`, and new grouped container components).
  - Localization resources (`strings.xml`, `values-es/strings.xml`) for section and quick access labels if needed.
- **Dependencies & APIs**: None (pure Jetpack Compose Material 3 implementation).
- **Breaking changes**: None.
