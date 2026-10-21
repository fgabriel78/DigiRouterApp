# Tasks

## 1. Protocol Decryption & Parser Hardening

- [x] 1.1 Update `RouterClient.getSystemLogs()` in `protocol/src/main/kotlin/es/routerapp/protocol/RouterClient.kt` to invoke `rawGet("/cgi/log", decrypt = true)`, verified via `./gradlew :protocol:compileKotlin`.
- [x] 1.2 Update `LOG_LINE_REGEX` in `protocol/src/main/kotlin/es/routerapp/protocol/log/LogParser.kt` to match module names with `[\w.-]+` and flexible whitespace `\s+`, verified via `./gradlew :protocol:compileKotlin`.
- [x] 1.3 Add unit tests in `protocol/src/test/kotlin/es/routerapp/protocol/log/LogParserTest.kt` validating module names containing hyphens, underscores, and dots, verified via `./gradlew :protocol:test --tests "es.routerapp.protocol.log.LogParserTest"`.

## 2. Decryption Testing & Project Verification

- [x] 2.1 Add unit tests in `protocol/src/test/kotlin/es/routerapp/protocol/RouterClientLogTest.kt` verifying that AES-encrypted `/cgi/log` responses are decrypted and parsed when session crypto is initialized, verified via `./gradlew :protocol:test --tests "es.routerapp.protocol.RouterClientLogTest"`.
- [x] 2.2 Execute all unit tests across the project via `./gradlew testDebugUnitTest` and `./gradlew :protocol:test` to confirm clean compilation and zero regressions.
