# Design

## Context

The DIGI Router application previously required users to re-enter their router administration password on every session, adhering to an ephemeral in-memory credential design. To reduce repetitive manual input while preserving strong security, the application will introduce an opt-in "Remember password" mechanism.

The project already includes `androidx.security.crypto:security-crypto` in `app/build.gradle.kts` and has `android:allowBackup="false"` in `AndroidManifest.xml`. The target SDK is 36 with a `minSdk` of 26, which is fully compatible with Android KeyStore-backed `MasterKey` and `EncryptedSharedPreferences`.

## Goals / Non-Goals

**Goals:**
- Provide a secure local storage abstraction (`CredentialStore`) using `EncryptedSharedPreferences` backed by `MasterKey` in the Android KeyStore (`AES256_GCM` values, `AES256_SIV` keys).
- Add a "Remember password" checkbox toggle to `LoginScreen` below the administration password field, with Material 3 styling and localized labels.
- Pre-populate the password input field and check the checkbox on app launch if a saved password is present.
- Persist the password securely upon successful login when "Remember password" is enabled.
- Immediately delete saved credentials from persistent storage if the user unchecks the option on the login screen.
- Overwrite previously saved credentials when the user enters a modified password and logs in successfully with the option checked.
- Gracefully handle KeyStore exceptions without crashing the application.

**Non-Goals:**
- Biometric authentication (fingerprint/face unlock) is out of scope for this change.
- Cloud syncing or auto-backup of router credentials across devices.
- Storing router passwords in plain unencrypted `SharedPreferences`.
- Bypassing router authentication (the app still performs live authentication against `/cgi/login` on the router).

## Decisions

### Decision 1: Use `EncryptedSharedPreferences` for credential storage
- **Choice**: Encrypt stored credentials via `androidx.security.crypto.EncryptedSharedPreferences` with `MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()`.
- **Rationale**: Administrative router credentials must not be stored in plaintext. `EncryptedSharedPreferences` transparently encrypts keys and values using keys stored in the hardware-backed Android KeyStore.
- **Alternatives Considered**:
  - Plain `SharedPreferences`: Insecure; exposes administrative passwords in plaintext XML on rootable devices or backups.
  - Raw Android `KeyStore` + custom Cipher helper: Unnecessary complexity and prone to cryptography pitfalls when Jetpack's official crypto library is already integrated into the project dependencies.

### Decision 2: Abstract storage behind `CredentialStore` interface
- **Choice**: Define a `CredentialStore` interface (with `SecureCredentialStore` implementation):
  ```kotlin
  interface CredentialStore {
      fun getSavedPassword(): String?
      fun savePassword(password: String)
      fun clear()
      fun hasSavedPassword(): Boolean
  }
  ```
- **Rationale**: Decouples `RouterViewModel` from Android Context / KeyStore APIs, making `RouterViewModel` easily unit-testable using mock or in-memory implementations.
- **Alternatives Considered**: Directly calling `EncryptedSharedPreferences` inside `RouterViewModel` (makes ViewModel unit tests brittle and requires Robolectric / Android instrumentation).

### Decision 3: Credential clearing lifecycle
- **Choice**:
  1. When "Remember password" is unchecked by the user in `LoginScreen`, trigger an immediate call to `vm.setRememberPassword(false)` which purges stored credentials from `CredentialStore`.
  2. When the user successfully signs in with "Remember password" checked, save/update the password in `CredentialStore`.
  3. Explicit logout from within the app does *not* clear the saved credential if the user wanted it remembered, allowing seamless re-login later.
- **Rationale**: Provides clear and predictable user control: unchecking the box guarantees removal from disk even if the user exits before submitting.

### Decision 4: UI integration in `LoginScreen`
- **Choice**: Place a horizontal `Row` with `Checkbox` and `Text` directly below the password `OutlinedTextField`. Use `Modifier.clickable` or `Modifier.toggleable` on the row so tapping the label also toggles the checkbox.
- **Rationale**: Follows standard Android and Material 3 design patterns for login forms.

## Risks / Trade-offs

- **[Risk]** KeyStore / crypto initialization failures on certain devices or OS versions (e.g., KeystoreProvider errors).
  - **Mitigation**: Wrap `EncryptedSharedPreferences` instantiation and read/write methods in `runCatching`. If initialization fails, log the error, clear any corrupted storage file, and fall back to ephemeral behavior (returning null for saved password) without crashing the app.
- **[Risk]** Synchronous disk I/O on ViewModel initialization.
  - **Mitigation**: The credential file is minimal (a single string value). Disk operations will be dispatched appropriately (writes on IO / coroutine dispatchers where suitable).
- **[Risk]** Outdated saved password if router password is changed externally.
  - **Mitigation**: Existing `LoginError.WrongPassword` error handling already alerts the user on failed authentication. The user can simply re-type the new password, which updates storage upon successful login.
