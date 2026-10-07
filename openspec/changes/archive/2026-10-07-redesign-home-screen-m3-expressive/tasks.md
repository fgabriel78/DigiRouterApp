# Tasks

## 1. Localization & String Resources

- [x] 1.1 Add localized string resources for the home screen hierarchy (`home_quick_access`, `home_hero_diagnostics`, `home_hero_online`, etc.) in `values/strings.xml` and `values-es/strings.xml`.

## 2. Expressive UI Components

- [x] 2.1 Implement `HeroStatusCard` in `MainActivity.kt` displaying router identity, live status pill, address, and click-to-diagnose action, verified with `./gradlew.bat :app:compileDebugKotlin`.
- [x] 2.2 Implement `QuickAccessBentoGrid` in `MainActivity.kt` rendering the 4 primary daily features (Summary, Connected Devices, Speed Test, Main Wi-Fi) with fixed equal heights, verified with `./gradlew.bat :app:compileDebugKotlin`.
- [x] 2.3 Implement `CategoryGroupContainer` and `CategoryItemRow` in `MainActivity.kt` rendering M3 surface container cards with leading shape badges, horizontal labels, and trailing navigation chevrons, verified with `./gradlew.bat :app:compileDebugKotlin`.

## 3. Screen Integration & Assembly

- [x] 3.1 Refactor `HomeScreen` in `MainActivity.kt` to assemble the Hero card, Quick Access grid, and grouped category containers without duplicate items or orphan cards, verified with `./gradlew.bat :app:compileDebugKotlin`.

## 4. Verification

- [x] 4.1 Run full project compilation and test suite with `./gradlew.bat test compileDebugSources` to verify complete UI and build integrity.
