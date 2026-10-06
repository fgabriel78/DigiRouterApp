# Tasks

## 1. Secure Credential Storage Layer

- [x] 1.1 Create `CredentialStore` interface and `SecureCredentialStore` implementation in `es.routerapp.app` using `EncryptedSharedPreferences` backed by Android KeyStore `MasterKey`, and verify implementation compiles.
- [x] 1.2 Implement a testable in-memory fake credential store and add unit tests verifying save, read, overwrite, and clear operations.

## 2. ViewModel & State Management

- [x] 2.1 Update `UiState` in `RouterViewModel.kt` to represent `rememberPassword: Boolean` and populate initial saved password state from `CredentialStore`.
- [x] 2.2 Update `RouterViewModel` login flow to persist credentials upon successful authentication when `rememberPassword` is true, update stored credentials if the password changed, and purge stored credentials immediately when `setRememberPassword(false)` is invoked.
- [x] 2.3 Add unit tests in `RouterViewModelTest.kt` verifying that credentials are saved on login success, purged when unchecked, and retained across session logouts.

## 3. UI & Localization

- [x] 3.1 Add `login_remember_password` string resource to `app/src/main/res/values/strings.xml` ("Remember password") and `app/src/main/res/values-es/strings.xml` ("Recordar contraseña").
- [x] 3.2 Add the "Remember password" checkbox control below the password field in `MainActivity.kt` (`LoginScreen`), hooking up state toggling and pre-filling the saved password, and verify UI layout rendering.

## 4. Verification

- [x] 4.1 Run `./gradlew test` to ensure all unit tests for storage, ViewModel, and authentication pass.
- [x] 4.2 Run `./gradlew assembleDebug` to verify end-to-end build and ProGuard/resource integrity.
