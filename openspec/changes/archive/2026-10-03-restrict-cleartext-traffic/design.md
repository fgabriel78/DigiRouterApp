# Design

## Context

The TP-Link XGB430v Pro router serves its web administration interface strictly over unencrypted HTTP on the local network (no TLS certificate or HTTPS endpoint is available on the device). Currently, `app/src/main/res/xml/network_security_config.xml` enables cleartext globally (`<base-config cleartextTrafficPermitted="true" />`).

Android's platform Network Security Configuration (`network_security_config.xml`) does not support CIDR notation (such as `192.168.0.0/16`) or IP wildcards in the `<domain>` tag; it only accepts literal hostnames/IPs or domain names with `includeSubdomains="true"`. To properly restrict cleartext traffic while preserving normal router management, we adopt a layered defense-in-depth approach.

See `proposal.md` for problem motivation and `specs/auth-session/spec.md` for updated requirement specifications.

## Goals / Non-Goals

**Goals:**
- Eliminate global cleartext HTTP enablement across the application.
- Configure `network_security_config.xml` with `<base-config cleartextTrafficPermitted="false" />` to block cleartext by default.
- Whitelist standard router gateway addresses (`192.168.1.1`, `192.168.0.1`, `192.168.2.1`, `192.168.1.254`, `192.168.100.1`, `10.0.0.1`, `172.16.0.1`, `127.0.0.1`) and local domain name patterns (`localhost`, `*.local`, `*.lan`, `*.home.arpa`, `tplinkwifi.net`, `tplinklogin.net`) in `<domain-config>`.
- Implement client-side address validation in the app to reject public/external IP addresses or non-local domains before initiating cleartext HTTP communication.
- Gracefully handle and report network security policy rejections in `RouterViewModel` and `LoginError`.

**Non-Goals:**
- Enabling HTTPS on the router (hardware limitation of the router firmware).
- Intercepting or rewriting third-party network traffic.

## Decisions

### Decision 1: Layered defense combining declarative Android XML config and application-layer address validation

**Rationale:**
Android's `network_security_config.xml` provides platform-level enforcement: any cleartext socket attempt to a host not matched by `<domain-config>` is terminated immediately by the Android runtime before transmission. However, because Android's XML schema cannot express arbitrary CIDR subnets, enumerating common default gateway IPs (`192.168.1.1`, `192.168.0.1`, `192.168.2.1`, `192.168.1.254`, `192.168.100.1`, `10.0.0.1`, `172.16.0.1`, `127.0.0.1`) alongside local domain extensions (`*.local`, `*.lan`, `*.home.arpa`) covers standard router deployments.

Supplementing this with application-level validation ensures that user input is verified to be a private IP (RFC 1918 `10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`, link-local `169.254.0.0/16`, loopback `127.0.0.0/8`) or local domain before any network connection is attempted, preventing arbitrary cleartext communication attempts to external internet targets.

**Alternatives considered:**
- *XML-only with a single IP (`192.168.1.1`)*: Breaks users with standard alternative subnets (`192.168.0.1` or `10.0.0.1`).
- *App-validation only without XML restriction*: Leaves global cleartext permitted in Android OS, allowing third-party dependencies or misconfigurations to leak cleartext traffic to the internet.

### Decision 2: Enhanced login error categorization for cleartext security blocks

**Rationale:**
When a connection is attempted to an address blocked by Android's `NetworkSecurityPolicy`, OkHttp throws an `IOException` or `UnknownServiceException` with message `Cleartext HTTP traffic to ... not permitted`. Rather than grouping this as an ambiguous network timeout (`LoginError.Unreachable`), `RouterViewModel` checks for network security policy violations and non-local addresses, mapping them to a dedicated error state or helpful diagnostic.

**Alternatives considered:**
- *Treat as generic timeout*: Confuses users if their entered address fails due to security policy rather than network reachability.

## Risks / Trade-offs

- **[Risk]** User assigns an uncommon private IP (e.g. `192.168.42.1`) not enumerated in `network_security_config.xml` `<domain-config>`.
  - **Mitigation:** Whitelist the top 10 most common home router gateway IPs and common local DNS names (`tplinkwifi.net`, `tplinklogin.net`, `*.local`, `*.lan`, `*.home.arpa`). If an unlisted IP is blocked by the platform, the app reports an informative error explaining the local cleartext restriction.
- **[Risk]** Address validation overhead or blocking main thread.
  - **Mitigation:** IP address parsing and validation is performed using non-blocking string/byte inspection on `Dispatchers.IO` before socket connection.

## Migration Plan

No database or persistence migration is required. The default address remains `192.168.1.1`, which is explicitly permitted in the updated network security config.
