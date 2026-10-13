# Tasks

## 1. Gradle Configuration

- [x] 1.1 Update `namespace = "es.digirouter.app"` and `applicationId = "es.digirouter.app"` in `app/build.gradle.kts`.

## 2. Source Code & Package Relocation

- [x] 2.1 Relocate Kotlin main source files from `app/src/main/kotlin/es/routerapp/app` to `app/src/main/kotlin/es/digirouter/app` and update package headers to `package es.digirouter.app`.
- [x] 2.2 Relocate unit test files from `app/src/test/kotlin/es/routerapp/app` to `app/src/test/kotlin/es/digirouter/app` and update package headers to `package es.digirouter.app`.
- [x] 2.3 Remove empty legacy directories under `app/src/main/kotlin/es/routerapp` and `app/src/test/kotlin/es/routerapp`.

## 3. Verification & Validation

- [x] 3.1 Execute unit tests via `./gradlew testDebugUnitTest` to verify all tests pass with the new package namespace.
- [x] 3.2 Assemble debug APK via `./gradlew :app:assembleDebug` to verify end-to-end build and resource resolution.
- [x] 3.3 Validate OpenSpec change compliance via `openspec validate change-application-id`.
