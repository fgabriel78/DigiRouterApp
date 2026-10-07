# Proposal: Stabilize Password Input Field and Keyboard Insets

## Why

When entering administrative credentials on the login screen (`LoginScreen`), users experience visual instability and jumping while typing, particularly on devices with large high-density screens and soft keyboards (e.g. Pixel 9 Pro XL with Gboard):
1. **Soft keyboard overlap and pan fighting:** In `AndroidManifest.xml`, `MainActivity` does not declare `android:windowSoftInputMode="adjustResize"`, and `LoginScreen` lacks `Modifier.imePadding()`. When the soft keyboard opens, Android resorts to legacy panning, partially overlapping the password input. Each keystroke causes the IME composition and Compose's internal `BringIntoViewRequester` to fight for scroll positioning, resulting in jittery jumps.
2. **Screen real-estate contention:** The 128.dp `ShapeBadge` header occupies significant vertical space (~220dp with title), leaving insufficient room above the ~400dp keyboard for all input fields.
3. **Artificial empty spacer rejection:** Reserving a static-height blank box between the password field and the "Remember password" checkbox created an unsightly visual gap and is discarded in favor of natural spacing.

Stabilizing IME window insets, making the header adapt to keyboard presence, maintaining monospace font metrics for masked text, and restoring natural form spacing delivers a smooth, clean Material 3 login experience.

## What Changes

- **Window and IME Insets Handling:** Set `android:windowSoftInputMode="adjustResize"` in `AndroidManifest.xml` and apply `Modifier.imePadding()` to the scrollable container in `LoginScreen`.
- **Keyboard-Responsive Header Compaction:** Adapt `ShapeBadge` size in `LoginScreen` from 128.dp to 48.dp when `WindowInsets.isImeVisible` is true, freeing ~80dp of vertical space so the input fields stay safely above the keyboard.
- **Monospace Font Masking:** Keep monospace typography (`FontFamily.Monospace`) and uniform character spacing (`2.sp`) while obscured to prevent glyph advance oscillations.
- **Natural Form Spacing:** Remove the artificial empty container below the password field; render error hints naturally without creating empty dead space.
- **Explicit Omission of Strength Meter:** Maintain a focused, minimalist router login form without strength progress bars or scoring widgets.

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `auth-session`: Update login form presentation requirements to ensure proper IME inset handling, responsive header sizing, stable masked typography, and clean form layout.

## Impact

- **Affected Code:** `app/src/main/AndroidManifest.xml`, `app/src/main/kotlin/es/routerapp/app/MainActivity.kt` (`LoginScreen`).
- **Dependencies & Protocol:** No new dependencies, no protocol or data model changes.
