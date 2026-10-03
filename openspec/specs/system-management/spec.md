# system-management Specification

## Purpose
Manages router system administration functions, clock and NTP time synchronization, LED schedule and night mode, automated and immediate reboots, and interface traffic statistics collection.

## Requirements

### Requirement: System Time and NTP Synchronization
The system SHALL display the current router time and configure NTP synchronization servers, timezone offsets, and daylight saving rules.

#### Scenario: Display current time and NTP sync status
- **GIVEN** the system time section (`DEV2_TIME`)
- **WHEN** the section loads
- **THEN** the screen renders the read-only local time (`currentLocalTime`) and NTP connection status (`status`).

#### Scenario: Configure NTP servers and time zone
- **GIVEN** NTP synchronization enabled (`enable == "1"`)
- **WHEN** the user configures time settings
- **THEN** the system permits editing primary and secondary NTP server addresses, selecting a GMT time zone offset (-12:00 to +14:00), and enabling automatic daylight saving time adjustment.

### Requirement: LED Status and Night Schedule Automation
The system SHALL control the physical front-panel LED indicators and configure an automated night-time shutdown schedule.

#### Scenario: Master LED toggle
- **GIVEN** the LED section (`DEV2_LED_SCHEDULE_CFG`)
- **WHEN** the user toggles `masterEnable`
- **THEN** the router turns all physical LED indicators on or off.

#### Scenario: Configure LED night shutdown interval
- **GIVEN** `masterEnable == "1"`
- **WHEN** the user enables the night schedule (`enable == "1"`)
- **THEN** the view displays time-of-day pickers for turn-off time (`startTime`) and turn-on time (`endTime`), validating and saving minutes from midnight.

### Requirement: Scheduled System Reboot
The system SHALL configure an automated daily reboot schedule to maintain operational stability.

#### Scenario: Configure automated daily reboot
- **GIVEN** the scheduled reboot section (`DEV2_REBOOT_SCHEDULE_CFG`)
- **WHEN** the user enables automatic reboots
- **THEN** the system permits setting the target hour (0-23) and minute (0-59) for the scheduled reboot.

### Requirement: Immediate Reboot Trigger
The system SHALL provide an immediate reboot action protected by an explicit high-risk confirmation prompt.

#### Scenario: Trigger immediate router reboot
- **GIVEN** the reboot page action (`ACT_REBOOT`) marked as danger
- **WHEN** the user taps "Reboot the router now"
- **THEN** the system displays a confirmation dialog warning that the router will restart and network connectivity will be interrupted for 1-2 minutes; upon confirmation, the client dispatches `operate("ACT_REBOOT")`.

### Requirement: Traffic Statistics Collection
The system SHALL control interface traffic counter sampling and configure collection intervals.

#### Scenario: Configure statistics sampling
- **GIVEN** the traffic statistics section (`DEV2_STAT_CFG`)
- **WHEN** the user enables statistics
- **THEN** the system permits adjusting the sampling interval between 5 and 60 seconds.
