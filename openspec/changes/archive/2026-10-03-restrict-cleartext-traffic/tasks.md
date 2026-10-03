# Tasks

## 1. Network Security Configuration Hardening

- [x] 1.1 Update `app/src/main/res/xml/network_security_config.xml` to disable cleartext traffic globally with `<base-config cleartextTrafficPermitted="false" />`.
- [x] 1.2 Add `<domain-config cleartextTrafficPermitted="true">` in `network_security_config.xml` for standard private router gateway IPs (`192.168.1.1`, `192.168.0.1`, `192.168.2.1`, `192.168.1.254`, `192.168.100.1`, `10.0.0.1`, `172.16.0.1`, `127.0.0.1`) and local domain names (`localhost`, `*.local`, `*.lan`, `*.home.arpa`, `tplinkwifi.net`, `tplinklogin.net`), verifying valid XML structure.

## 2. Local Network Address Validation

- [x] 2.1 Implement a `LocalNetworkValidator` utility in `app/src/main/kotlin/es/routerapp/app/` to verify host addresses belong to RFC 1918 private ranges, loopback, link-local, or local domains, and add unit tests verifying acceptance and rejection of sample addresses.
- [x] 2.2 Integrate `LocalNetworkValidator` into `RouterViewModel.login` to validate target router addresses before initiating network connections.

## 3. Error Handling and UI Reporting

- [x] 3.1 Add `CleartextRestricted` to `LoginError` in `RouterViewModel.kt` and catch `UnknownServiceException` / network security policy violations and non-local address validation errors.
- [x] 3.2 Add localized strings (English and Spanish) for the cleartext restriction error message and render the diagnostic in `LoginScreen` in `MainActivity.kt`.

## 4. Build and Integration Verification

- [x] 4.1 Run unit tests via `./gradlew test` to verify `LocalNetworkValidator` and `RouterViewModel` error handling.
- [x] 4.2 Verify the full debug APK builds cleanly with `./gradlew assembleDebug`.
