# Proposal

## Why

The application currently uses `es.routerapp.app` as its Android `applicationId` and namespace. To align the application package identity with its brand and distribution requirements, the application identifier and primary package namespace must be renamed to `es.digirouter.app`.

## What Changes

- **Android Application Identifier (`applicationId`)**: Update `applicationId` from `es.routerapp.app` to `es.digirouter.app` in `app/build.gradle.kts`.
- **Android Gradle Namespace (`namespace`)**: Update `namespace` from `es.routerapp.app` to `es.digirouter.app` in `app/build.gradle.kts`.
- **Package Relocation (`:app`)**: Move all Kotlin source code files in `:app` from `es.routerapp.app` to `es.digirouter.app` (including test source directories).
- **Package Declarations & Imports**: Update all `package es.routerapp.app` declarations and references to generated R resources (`es.digirouter.app.R`) across the `:app` module.

## Capabilities

### New Capabilities

*(None)*

### Modified Capabilities

*(None)*

## Impact

- `:app`:
  - `app/build.gradle.kts`: update `namespace` and `applicationId`.
  - `app/src/main/kotlin/`: directory moved from `es/routerapp/app` to `es/digirouter/app`.
  - `app/src/test/kotlin/`: directory moved from `es/routerapp/app` to `es/digirouter/app`.
  - `AndroidManifest.xml`: relative activity `.MainActivity` automatically points to `es.digirouter.app.MainActivity`.
- `:protocol`:
  - Remains untouched as an independent pure Kotlin module under `es.routerapp.protocol`.
