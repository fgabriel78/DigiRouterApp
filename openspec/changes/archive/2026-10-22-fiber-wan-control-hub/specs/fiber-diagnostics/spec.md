# Spec Delta

## ADDED Requirements

### Requirement: GPON ONT Registration Telemetry and Identity Inspection
The system SHALL query GPON authentication parameters (`DEV2_GPON_AUTH`) on stack `1,0,0,0,0,0` and present the ONT registration parameters (GPON Serial Number, PLOAM password, and OLT interoperability mode) in read-only and exportable formats.

#### Scenario: Dual GPON Serial Number presentation (ASCII+HEX and pure HEX)
- **WHEN** the ONT registration details are loaded from `DEV2_GPON_AUTH`
- **THEN** the system displays the GPON Serial Number in both standard 12-character ASCII+HEX format (e.g. `HWTC12345678`) and raw 16-character hexadecimal format (e.g. `4857544331323334`), with individual copy actions for each format.

#### Scenario: PLOAM password masking and reveal controls
- **WHEN** viewing the GPON / PLOAM password
- **THEN** the system masks the password characters by default and provides a toggle button to reveal the cleartext password and copy it to the clipboard.

#### Scenario: OLT interoperability mode display
- **WHEN** the OLT mode attribute (`oltMode`) is retrieved
- **THEN** the system maps the numeric mode index to its friendly profile name (`Normal Mode`, `Mode H`, `Mode Z`, `Mode F`, `Mode X`, or `Mode HH`).

#### Scenario: SFP ONT configuration export snippet
- **WHEN** the user selects the "Export Config" action
- **THEN** the system formats the active GPON SN, PLOAM password, and OLT vendor profile into a standardized configuration snippet and copies it to the clipboard for use in third-party SFP ONT modules.

#### Scenario: Read-only protection for ONT credentials
- **WHEN** interacting with the ONT registration card in the user interface
- **THEN** the parameters are strictly presented as read-only and non-editable, preventing accidental modifications that could sever optical connectivity.

### Requirement: On-Demand OMCI State Machine Restart
The system SHALL provide an on-demand action to restart the router's OMCI state machine via `ACT_OMCI_RESTART` without requiring a full hardware reboot.

#### Scenario: Triggering OMCI restart with confirmation
- **WHEN** the user confirms an OMCI restart action from the OMCI Registration card
- **THEN** the system dispatches `operate("ACT_OMCI_RESTART")` to the router and transitions the step into a temporary progress state.

#### Scenario: OMCI restart tracking and re-association
- **WHEN** an OMCI restart is in flight
- **THEN** the system polls `DEV2_GPON_INTF_OMCI_STATS` until the ONT returns to the `O5 Associated` registration state or reaches a timeout.

### Requirement: On-Demand WAN Session Reconnection and Public IP Renewal
The system SHALL support on-demand WAN session termination and re-establishment to refresh the public IP address lease without restarting router hardware.

#### Scenario: PPPoE session renewal cycle
- **GIVEN** an active WAN connection with connection type `PPPoE`
- **WHEN** the user triggers the "Renew Public IP / Reconnect" action
- **THEN** the system executes `operate("ACT_PPP_DISCONN", stack)`, pauses for a minimum stabilizing delay of 1.5 seconds, and then executes `operate("ACT_PPP_CONN", stack)`.

#### Scenario: DHCP lease renewal cycle
- **GIVEN** an active WAN connection with connection type `DHCP`
- **WHEN** the user triggers the "Renew Public IP / Reconnect" action
- **THEN** the system executes `operate("ACT_DHCP_RELEASE", stack)`, pauses for a minimum stabilizing delay of 1.5 seconds, and then executes `operate("ACT_DHCP_RENEW", stack)`.

#### Scenario: In-flight progress and new IP address verification
- **WHEN** a WAN session renewal cycle is initiated
- **THEN** the system displays in-flight status progress, polls `DEV2_ADT_WAN` until connection status returns to `Connected`, captures the newly assigned IP address, and presents an outcome toast comparing the prior IP with the renewed IP.
