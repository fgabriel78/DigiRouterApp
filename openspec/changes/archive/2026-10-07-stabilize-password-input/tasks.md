# Tasks: Stabilize Password Input Field and Keyboard Insets

## 1. Typography and Metrics Stabilization

- [x] 1.1 Update `LoginScreen` in `MainActivity.kt` to apply conditional monospace typography (`FontFamily.Monospace`) and uniform letter spacing (`2.sp`) when password is masked (`!reveal`), verified with `./gradlew.bat :app:compileDebugKotlin`.

## 2. Clean Form Layout and Spacer Removal

- [x] 2.1 Remove the empty `Box(heightIn(min = 28.dp))` below the password field in `LoginScreen`, restoring natural spacing between the password field and the "Remember password" checkbox, and rendering `state.error` only when present.

## 3. IME Configuration and Responsive Layout

- [x] 3.1 Configure `android:windowSoftInputMode="adjustResize"` on `.MainActivity` in `app/src/main/AndroidManifest.xml`.
- [x] 3.2 Add `Modifier.imePadding()` to the scrollable container in `LoginScreen` and make `ShapeBadge` responsive to keyboard visibility (`WindowInsets.isImeVisible`), shrinking to `48.dp` when the keyboard is open.

## 4. Verification

- [x] 4.1 Run full project compilation and unit test suite with `./gradlew.bat test compileDebugSources` to verify complete UI and build integrity.
