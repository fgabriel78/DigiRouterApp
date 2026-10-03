# Proposal

## Why

Currently, `network_security_config.xml` permits cleartext HTTP traffic across the entire application using `<base-config cleartextTrafficPermitted="true" />`. While plain HTTP is required to communicate with the local router web interface (which does not provide TLS), permitting cleartext globally exposes the app to potential cleartext traffic leaks or man-in-the-middle risks against arbitrary internet hosts. Restricting cleartext communication exclusively to local network ranges hardens the application without disrupting normal local router administration.

## What Changes

- **Disable Global Cleartext**: Set `<base-config cleartextTrafficPermitted="false" />` in `network_security_config.xml` to disallow cleartext HTTP across the application by default.
- **Whitelist Local Gateways & Domains**: Configure `<domain-config cleartextTrafficPermitted="true">` in `network_security_config.xml` to explicitly allow cleartext traffic for standard router gateway IP addresses (`192.168.1.1`, `192.168.0.1`, `192.168.2.1`, `192.168.1.254`, `192.168.100.1`, `10.0.0.1`, `172.16.0.1`, `127.0.0.1`) and local network domain names (`localhost`, `*.local`, `*.lan`, `*.home.arpa`, `tplinkwifi.net`, `tplinklogin.net`).
- **Validate Local Address in Application**: Add local network range validation in `RouterViewModel` (or a dedicated IP validator) before initiating connections, ensuring user-specified router addresses fall within RFC 1918 private subnets, loopback, link-local, or approved local domain names.
- **Categorize Cleartext Security Errors**: Ensure network security policy rejections (e.g., attempting cleartext to a non-whitelisted or public endpoint) are properly recognized and presented as connectivity or security configuration errors in the login UI.

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `auth-session`: Updates the router address and endpoint configuration requirement to mandate that cleartext HTTP communication is restricted to local network ranges (private IP ranges, loopback, and local domain suffixes), rejecting cleartext connections to external or public hosts.

## Impact

- **Android Configuration**: `app/src/main/res/xml/network_security_config.xml` changes from global cleartext enablement to explicit local domain/IP whitelisting.
- **App Code**: `RouterViewModel` and login validation in `app/src/main/kotlin/es/routerapp/app/` to validate host addresses and handle security policy exceptions.
- **Dependencies & APIs**: No external dependencies added; uses standard Android/JDK network validation primitives (`InetAddress`, IP subnet checks).
