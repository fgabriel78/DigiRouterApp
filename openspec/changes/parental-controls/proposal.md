# Proposal

## Why

Parents and network administrators managing home networks lack a native mobile interface to oversee their children's screen time, enforce bedtime schedules, and protect devices from inappropriate online content. While the router firmware exposes parental control and web insight endpoints (`DEV2_PC_*`), the Android app currently lacks support for this functionality. Adding Parental Controls & Screen Time Insights provides administrators with centralized device management, scheduled downtime, and real-time usage insights directly from the mobile app.

## What Changes

- **Admin-Gated Security Navigation**: Register Parental Controls under `Group.SECURITY` in the catalog, restricted strictly to administrative logins (`adminOnly = true`).
- **Child Profiles Management**: Support creating, editing, and deleting child profiles (`DEV2_PC_PROFILE`) with custom names and age-appropriate content filter presets.
- **Live Device Assignment**: Provide a live device picker sourced from active network clients (`ConnectedDevices`), binding client MAC addresses to profiles (`DEV2_PC_PROFILE_DEVICE`) without manual entry.
- **Scheduled Bedtime & Usage Quotas**: Configure daily screen time limits and bedtime access shutoffs differentiating weekdays from weekends (`DEV2_PC_WEEKDAY`, `DEV2_PC_WEEKEND`).
- **Content Filtering & Domain Blacklist**: Enforce content category filtering (`DEV2_PC_FILTERCONTENT`) and custom domain keyword blacklists (`DEV2_PC_BLACKLIST`).
- **Instant Internet Pause**: Provide a 1-tap pause and resume toggle to immediately block or restore internet access across all devices in a profile.
- **Web Insights & Blocked Access Log**: Display daily screen time usage (`DEV2_PC_VISITRANK`), top visited domains (`DEV2_PC_VISITRANK_ENTRY`), and an audit trail of blocked website access attempts (`DEV2_PC_BLOCKHISTORY_ENTRY`) with 1-tap unblocking.
- **Flash Commit & Persistence**: Automatically persist mutations to router NVRAM via `ACT_SAVE_CFG`.

## Capabilities

### New Capabilities
- `parental-controls`: Child profiles grouping device MAC addresses, scheduled bedtime intervals, weekday/weekend screen time limits, category-based content filtering, domain blacklists, instant internet pause, and screen time insights with blocked access attempt history.

### Modified Capabilities
*(None. Existing capabilities remain unchanged.)*

## Impact

- **`:protocol`**:
  - Adds `ParentalControlEngine` and domain data models for profiles, devices, schedules, filters, and insights.
  - Adds `Page(id = "parental-control", title = "Parental Controls", group = Group.SECURITY, adminOnly = true, custom = true)` to `Catalog.kt`.
  - Implements wire serialization and stack resolution for OIDs `DEV2_PC_PROFILE`, `DEV2_PC_PROFILE_DEVICE`, `DEV2_PC_WEEKDAY`, `DEV2_PC_WEEKEND`, `DEV2_PC_FILTERCONTENT`, `DEV2_PC_BLACKLIST`, `DEV2_PC_UNBLOCKEDLIST`, `DEV2_PC_VISITRANK`, `DEV2_PC_VISITRANK_ENTRY`, and `DEV2_PC_BLOCKHISTORY_ENTRY`.
- **`:app`**:
  - Implements `ParentalControlScreen` with Bento-style profile cards, instant pause switches, and screen time gauges.
  - Implements `ProfileEditorBottomSheet` featuring live device selection from active network clients, time slider controls, and filter toggles.
  - Implements `WebInsightsScreen` / `WebInsightsSheet` showing time spent online, top visited domains, and blocked attempt history with 1-tap unblock.
  - Adds bilingual localization strings in `values/strings.xml` and `values-es/strings.xml`.
  - Integrates navigation in `MainActivity.kt`.
