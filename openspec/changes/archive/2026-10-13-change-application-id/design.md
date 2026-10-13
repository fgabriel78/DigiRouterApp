# Design: Application ID & Package Namespace Migration to `es.digirouter.app`

## Context

See `proposal.md` for motivation. The application module (`:app`) is currently configured with `applicationId = "es.routerapp.app"` and `namespace = "es.routerapp.app"`.

All Kotlin source files in `app/src/main/kotlin/es/routerapp/app` and tests in `app/src/test/kotlin/es/routerapp/app` declare `package es.routerapp.app`.

## Goals / Non-Goals

**Goals:**
- Update `applicationId` to `es.digirouter.app` in `app/build.gradle.kts`.
- Update `namespace` to `es.digirouter.app` in `app/build.gradle.kts`.
- Relocate all `:app` Kotlin source files to directory `es/digirouter/app`.
- Update package declarations to `package es.digirouter.app`.
- Ensure clean compilation and test suite execution with 0 warnings or errors.

**Non-Goals:**
- Renaming the `:protocol` module (it remains `es.routerapp.protocol`, an independent pure Kotlin engine).
- Changing application UI, themes, strings, or functional logic.

## Decisions

### 1. Full Package Structure Relocation

```
BEFORE:
  app/src/main/kotlin/es/routerapp/app/
    ├── MainActivity.kt (package es.routerapp.app)
    ├── ...
  app/src/test/kotlin/es/routerapp/app/
    ├── ...

AFTER:
  app/src/main/kotlin/es/digirouter/app/
    ├── MainActivity.kt (package es.digirouter.app)
    ├── ...
  app/src/test/kotlin/es/digirouter/app/
    ├── ...
```

- **Rationale**: Aligns both the user-facing OS package identifier (`applicationId`) and the code namespace (`namespace` / package path) cleanly under `es.digirouter.app`, avoiding architectural drift between build identifier and code packages.
- **Alternatives Considered**: Modifying only `applicationId` while leaving `namespace` as `es.routerapp.app`. Rejected per user preference for complete alignment.

### 2. Preserve Module Boundary for `:protocol`

- `:protocol` continues to use `es.routerapp.protocol`.
- `:app` files will continue to import `es.routerapp.protocol.*` as usual.

## Risks / Trade-offs

- **[Risk] Existing Installed Builds on Test Devices** -> *Mitigation*: Changing `applicationId` creates a new package identity on Android devices; existing test apps will not upgrade in-place. Users should uninstall old `es.routerapp.app` or install `es.digirouter.app` cleanly via `adb install -r`.
- **[Risk] R Resource Resolution & Imports** -> *Mitigation*: With `namespace = "es.digirouter.app"`, Gradle generates `es.digirouter.app.R`. All source files inside package `es.digirouter.app` resolve `R` automatically without explicit imports.
