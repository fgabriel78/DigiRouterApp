# Proposal

## Why

Currently, the application enforces a strict ephemeral credential policy: the router administration password is kept exclusively in memory during an active session and is never persisted to local storage. While secure, this requires users to re-enter their router password every time the app is launched, creating repetitive friction for users managing their home router on a personal device. Adding an opt-in "Remember password" setting provides convenience without compromising security by leveraging Android's hardware-backed encrypted storage.

## What Changes

- **"Remember password" Login UI Option**: Add a checkbox control ("Remember password" / "Recordar contraseña") directly under the password input field on the login screen.
- **Secure Encrypted Credential Persistence**: When "Remember password" is checked and authentication succeeds, store the password in `EncryptedSharedPreferences` backed by the Android KeyStore (`MasterKey`).
- **Auto-populate Saved Credentials**: Upon app startup or displaying the login screen, check if a password was previously remembered. If present, pre-fill the password field and pre-check the "Remember password" checkbox.
- **Credential Modification & Clearing Lifecycle**:
  - If the user enters a new password and logs in successfully with the checkbox checked, update the securely stored password.
  - If the user unchecks "Remember password", immediately purge any stored password from secure persistent storage so that subsequent app launches start with an empty password field.
- **Specification Update**: Revise the `auth-session` capability to replace the unconditional ephemeral credential requirement with an opt-in secure persistence model.

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `auth-session`: Updates credential lifecycle requirements to allow encrypted non-volatile local storage when the user opts in via "Remember password", while maintaining ephemeral in-memory handling when unchecked.

## Impact

- **UI Layer**: `app/src/main/kotlin/es/routerapp/app/MainActivity.kt` (`LoginScreen` Composable) gains a Checkbox and label, plus state binding for the remember toggle.
- **ViewModel & Storage**: `RouterViewModel` integrates with a secure storage component (`CredentialStore` using `androidx.security.crypto.EncryptedSharedPreferences`) to load, persist, and clear saved credentials.
- **Localization**: Adds `login_remember_password` string resource to both `res/values/strings.xml` and `res/values-es/strings.xml`.
- **Dependencies**: Uses the existing `androidx.security.crypto` dependency already present in `app/build.gradle.kts`.
