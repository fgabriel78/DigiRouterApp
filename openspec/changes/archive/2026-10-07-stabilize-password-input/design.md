# Design: Stabilize Password Input Field and Keyboard Insets

## Context

In `LoginScreen` (`app/src/main/kotlin/es/routerapp/app/MainActivity.kt`), users experience jumping while typing:
- The soft keyboard (Gboard on Pixel 9 Pro XL) partially overlaps the password input field because `AndroidManifest.xml` lacks `android:windowSoftInputMode="adjustResize"` and `LoginScreen` lacks `Modifier.imePadding()`.
- When typing, the IME updates composition and triggers `BringIntoViewRequester` in Compose. Because the scrollable container does not pad for the keyboard, Compose and Android's WindowManager pan mechanism fight each other, causing visual jumps on each keystroke.
- The 128.dp `ShapeBadge` header takes up significant vertical space, leaving little visible height above the ~400dp keyboard.
- An earlier attempt to reserve a static 28.dp blank Box below the password field introduced an unnatural and unsightly visual gap between the password field and the "Remember password" checkbox.

## Goals / Non-Goals

**Goals:**
- Eliminate soft keyboard overlap and pan fighting by properly configuring IME insets (`adjustResize` and `Modifier.imePadding()`).
- Adapt the header size reactively (`WindowInsets.isImeVisible`) so `ShapeBadge` scales down from 128.dp to 48.dp with the keyboard open, freeing ~80dp of vertical space.
- Maintain monospace advance metrics for masked text (`FontFamily.Monospace`, `letterSpacing = 2.sp` when `!reveal`).
- Eliminate the artificial empty space between the password field and the checkbox, restoring natural, tight form layout.
- Maintain a clean, distraction-free authentication interface with no password strength indicators.

**Non-Goals:**
- Introducing password strength meters, entropy scoring, or progress bars.
- Modifying authentication state flows, timeout recovery, or network protocol logic.

## Decisions

### 1. WindowSoftInputMode and Compose IME Padding
- **Decision:** Declare `android:windowSoftInputMode="adjustResize"` on `.MainActivity` in `app/src/main/AndroidManifest.xml`, and add `Modifier.imePadding()` to the scrollable `Column` in `LoginScreen`.
- **Rationale:** Gives Compose full ownership of keyboard animations and resizing. The scroll container shrinks its viewport by the keyboard's exact height, allowing Compose's `BringIntoViewRequester` to effortlessly keep the active input above the keyboard.

### 2. Adaptive Header Compaction with `isImeVisible`
- **Decision:** Use `WindowInsets.isImeVisible` to dynamically adapt the router badge size:
  ```kotlin
  val imeVisible = WindowInsets.isImeVisible
  val badgeSize = if (imeVisible) 48.dp else 128.dp
  ```
- **Rationale:** Immediately recovers ~80dp of vertical screen space when typing, ensuring all input fields stay cleanly in the upper visible half of the screen.

### 3. Monospace Typography for Masked Password
- **Decision:** Apply `FontFamily.Monospace` and `letterSpacing = 2.sp` when password is masked (`!reveal`). Revert to default when `reveal == true`.
- **Rationale:** Guarantees uniform advance widths for keystrokes and bullet glyphs (`•`), eliminating character width jitter.

### 4. Removal of Artificial Blank Supporting Space
- **Decision:** Remove the empty `Box(Modifier.fillMaxWidth().heightIn(min = 28.dp))` below the password field. Display `state.error` only when present, restoring natural spacing between the password field and the "Remember password" checkbox.
- **Rationale:** The static blank box created an unsightly gap during normal credential entry. Natural spacing combined with `imePadding()` provides a clean, jump-free experience.

## Risks / Trade-offs

- **[Risk] Multiple insets consumption**  
  → *Mitigation:* Ensure `imePadding()` is placed on the scrollable container before `.verticalScroll(rememberScrollState())` to correctly constrain the scroll viewport.
