# parental-controls Specification

## Purpose
Enables network administrators to configure parental controls, enforce bedtime access schedules and daily usage quotas across child devices, filter content and domain blacklists, and inspect screen time insights with blocked access history.

## Requirements

### Requirement: Admin-Restricted Parental Controls Navigation
The system SHALL expose Parental Controls within the Security group exclusively to users authenticated with administrative credentials.

#### Scenario: Admin user navigation
- **WHEN** user logs in with the username "admin"
- **THEN** Parental Controls is visible and navigable under the Security category

#### Scenario: Non-admin user access restriction
- **WHEN** user logs in with standard non-admin credentials
- **THEN** Parental Controls is excluded from the visible catalog and inaccessible

### Requirement: Child Profile Lifecycle Management
The system SHALL allow administrators to create, update, list, and delete child profiles with distinct names and baseline filter levels.

#### Scenario: Profile creation
- **WHEN** administrator submits a new profile with a unique name and filter level
- **THEN** the system adds the profile instance on the router and assigns it an instance stack identifier

#### Scenario: Profile deletion
- **WHEN** administrator deletes an existing profile
- **THEN** the system removes the profile instance, its device bindings, its custom blacklist rules, and its schedules from the router

### Requirement: Live Device Binding and Selection
The system SHALL allow administrators to bind client devices to a child profile by selecting active devices discovered on the local network.

#### Scenario: Assigning active devices
- **WHEN** administrator opens the device assignment picker
- **THEN** the system presents the list of currently active LAN and Wi-Fi devices showing hostnames and MAC addresses for multi-device selection

#### Scenario: Device binding persistence
- **WHEN** administrator confirms device selections for a profile
- **THEN** the system updates the profile device association list on the router using each selected device's physical MAC address

### Requirement: Differentiated Bedtime and Usage Quota Scheduling
The system SHALL configure daily screen time allowances and bedtime blackout windows independently for weekdays and weekends.

#### Scenario: Setting weekday and weekend limits
- **WHEN** administrator configures daily quota minutes and bedtime start and end times for weekdays and weekends
- **THEN** the system updates the weekday schedule with Monday-Friday day bitmask and weekend schedule with Saturday-Sunday day bitmask

#### Scenario: Disabling bedtime or time limit
- **WHEN** administrator disables either bedtime or screen time quota for a profile
- **THEN** the corresponding schedule entry on the router is updated with the enable flag set to inactive

### Requirement: Content Category and Domain Filtering
The system SHALL enforce content category restrictions and custom domain keyword blacklists for each child profile.

#### Scenario: Applying content category filters
- **WHEN** administrator enables or disables content categories such as adult content, gambling, social networks, or games
- **THEN** the system sets the corresponding filter flags on the profile's content filter configuration

#### Scenario: Adding custom blacklist rule
- **WHEN** administrator adds a custom domain or keyword to a profile's blacklist
- **THEN** the system adds the URL entry associated with the profile stack on the router

### Requirement: Instant Internet Access Pause and Resume
The system SHALL provide an instant internet pause switch that immediately suspends or restores internet connectivity for all devices assigned to a profile.

#### Scenario: Pausing internet connectivity
- **WHEN** administrator toggles the internet pause switch to paused
- **THEN** the system sets the profile status to inactive and immediately interrupts internet access for the profile's bound devices

#### Scenario: Resuming internet connectivity
- **WHEN** administrator toggles the internet pause switch to active
- **THEN** the system sets the profile status to active and restores network access subject to active schedule constraints

### Requirement: Screen Time Insights and Domain Telemetry
The system SHALL retrieve daily online duration and ranked top visited domains for a selected child profile across supported historical dates.

#### Scenario: Fetching today's insights
- **WHEN** administrator views web insights for today
- **THEN** the system queries the router visit rank records for the profile and displays total minutes spent online and top visited domains

#### Scenario: Switching insight date
- **WHEN** administrator selects yesterday or an earlier supported date
- **THEN** the system queries the visit rank with the adjusted date offset and updates the displayed telemetry and domain rankings

### Requirement: Blocked Access Log and Whitelist Resolution
The system SHALL display an audit log of blocked access attempts for a child profile and allow administrators to permit access via 1-tap unblocking.

#### Scenario: Inspecting blocked attempts
- **WHEN** administrator reviews the blocked history for a profile
- **THEN** the system retrieves and displays blocked URLs alongside their attempt timestamps

#### Scenario: Unblocking a blocked domain
- **WHEN** administrator selects unblock on a blocked history entry
- **THEN** the system adds the domain to the profile's unblocked whitelist on the router and updates the entry status to allowed

### Requirement: Router Configuration NVRAM Persistence
The system SHALL commit parental control modifications to router non-volatile memory to prevent configuration loss on reboot.

#### Scenario: Committing changes on profile mutation
- **WHEN** administrator completes adding, editing, pausing, or deleting a profile or blacklist item
- **THEN** the system executes the router configuration save operation
