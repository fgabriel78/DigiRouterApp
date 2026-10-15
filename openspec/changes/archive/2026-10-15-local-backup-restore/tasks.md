# Tasks

## 1. Protocol Layer & RouterClient Endpoints

- [x] 1.1 Add `downloadBackup()` in `RouterClient` to query `DEV2_DEV_INFO`, construct the dynamic firmware binary URL or fallback `/cgi/conf.bin`, and return the raw configuration `ByteArray`. Verify with unit tests in `protocol/src/test/kotlin/es/routerapp/protocol/BackupEndpointTest.kt` validating URL formation and HTTP download handling.
- [x] 1.2 Add `restoreBackup(data: ByteArray)` in `RouterClient` executing multipart POST to `/cgi/confup` (`filename`) followed by `/cgi/bnr` trigger. Verify with unit tests in `BackupEndpointTest.kt` simulating multipart structure and error response handling.
- [x] 1.3 Register `backup` page definition in `Catalog.kt` under `Group.SYSTEM` and add translations to `SpanishTexts.kt`. Verify by running `./gradlew :protocol:test`.

## 2. Local Backup Storage & Retention Engine

- [x] 2.1 Implement `BackupStore` in `:app` handling file persistence in `context.filesDir/backups/`, JSON metadata serialization (filename, timestamp, device model, firmware version, size), and strict 5-slot FIFO retention. Verify with JVM unit tests in `BackupStoreTest.kt` ensuring the oldest entry is pruned when a 6th backup is created.
- [x] 2.2 Add deletion and file sharing utilities in `BackupStore` to safely remove files from disk and create content URIs for Android sharing intents. Verify with unit tests in `BackupStoreTest.kt`.

## 3. ViewModel & State Orchestration

- [x] 3.1 Implement `BackupRestoreViewModel` orchestrating download, save, restore, deletion, sharing, and reboot recovery states (`Idle`, `Downloading`, `Restoring`, `Rebooting`). Verify with JVM unit tests in `BackupRestoreViewModelTest.kt` mocking client and store interactions.
- [x] 3.2 Add bilingual string resources for all backup/restore labels, confirmations, warnings, and countdown states in `app/src/main/res/values/strings.xml` and `values-es/strings.xml`. Verify compilation via `./gradlew compileDebugKotlin`.

## 4. UI Implementation (Material 3 Expressive) & Navigation

- [x] 4.1 Implement `BackupRestoreScreen` in Jetpack Compose using Material 3 Expressive tokens, hero card for "Crear copia de seguridad ahora", and card list displaying up to 5 saved backups with auto-formatted date/time ("Copia dd/MM/yyyy HH:mm"), Restore, Share, and Delete actions.
- [x] 4.2 Add high-risk confirmation dialog and animated circular reboot countdown overlay (~90 seconds) with periodic reconnect polling.
- [x] 4.3 Wire `backup` route in `MainActivity.kt` and integrate into `HomeScreen` navigation under `Group.SYSTEM`. Verify full test suite passes with `./gradlew testDebugUnitTest`.
