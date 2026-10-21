# Design

## Context

See `proposal.md` for background and user motivation.

The router firmware exposes parental control through TR-181/OID data models (`DEV2_PC_*`) as discovered in `main_parentCtrl_v2.htm`. In the current project architecture:
- `:protocol` is a pure Kotlin module handling wire communication, OID mappings, and data models without Android framework dependencies.
- `:app` is the Jetpack Compose Material 3 Expressive UI layer.
- `Catalog.kt` defines all navigable pages and their accessibility per user role (`adminOnly`).
- Device discovery is powered by `ConnectedDevices` (pulling from `DEV2_HOST_ENTRY`, `DEV2_ARP_ENTRY`, and `DEV2_ADT_LAN`).

## Goals / Non-Goals

**Goals:**
- Provide a responsive, Material 3 Expressive interface for managing child profiles, scheduled downtime, and screen time quotas.
- Restrict parental control access strictly to administrative logins under `Group.SECURITY`.
- Provide a live device selection picker populated from active network clients to eliminate manual MAC address typing.
- Implement full profile CRUD, instant pause/resume, and category/domain filtering.
- Provide historical Web Insights (online duration, top visited domains) and a blocked access log with 1-tap unblocking.
- Guarantee router NVRAM persistence by triggering `ACT_SAVE_CFG` after mutations.

**Non-Goals:**
- External cloud push notifications when access is blocked (the router logs events locally without a cloud messaging gateway).
- Deep packet inspection for mobile apps beyond DNS/SNI hostname keywords.
- Multi-admin role permissions (the router has a single `admin` role and a standard `user` role).

## Architecture & Data Flow

```
+-----------------------------------------------------------------------------------------+
|                                    Android App (:app)                                   |
|                                                                                         |
|  HomeScreen (Group.SECURITY)                                                            |
|       |                                                                                 |
|       +--> ParentalControlViewModel                                                     |
|                 |                                                                       |
|                 +---> ParentalControlScreen (Compose Bento Grid & Controls)             |
|                 |        |                                                              |
|                 |        +---> ProfileCard (Quick Pause/Resume, Quota Progress)         |
|                 |        +---> ProfileEditorBottomSheet (Name, Filter, Bedtime)         |
|                 |        |        ^                                                     |
|                 |        |        +--- LiveDevicePicker (from ConnectedDevices)         |
|                 |        +---> WebInsightsSheet (Daily Charts, Top Domains, Block Log)  |
+-----------------|-----------------------------------------------------------------------+
                  | Coroutine Flow via Dispatchers.IO
                  v
+-----------------------------------------------------------------------------------------+
|                                Pure Kotlin Engine (:protocol)                           |
|                                                                                         |
|  ParentalControlEngine                                                                  |
|  - loadProfiles(): List<ParentalProfile>                                                |
|  - saveProfile(profile, isNew): Result<Unit>                                            |
|  - deleteProfile(profileStack): Result<Unit>                                            |
|  - setPauseState(profileStack, isPaused): Result<Unit>                                  |
|  - fetchInsights(profileStack, dateOffset): Result<WebInsights>                         |
|  - unblockDomain(profileStack, url): Result<Unit>                                       |
|  - addBlacklistDomain(profileStack, url): Result<Unit>                                  |
|       |                                                                                 |
|       v                                                                                 |
|  RouterClient (Encrypted POST /cgi_gdpr?9)                                              |
|  - gl/gs/go/so/ao/do on DEV2_PC_*                                                       |
|  - op on ACT_SAVE_CFG (Flash Commit)                                                    |
+-----------------------------------------------------------------------------------------+
```

## Decisions

### Decision 1: Dedicated `ParentalControlEngine` in `:protocol`
- **Choice**: Encapsulate all multi-step OID interactions, stack transformations, and bitmask conversions inside `ParentalControlEngine` rather than adding methods directly to `RouterClient`.
- **Rationale**: Keeps `RouterClient` lightweight and focused on low-level encryption and HTTP dispatch. Mirrors existing domain engines (`FiberDiagnosticEngine`, `SecurityAuditEngine`, `DualDiagnosticEngine`).
- **Alternatives Considered**: Direct calls from ViewModel via `RouterClient.getList` (rejected: duplicates complex stack resolution and payload building in the presentation layer).

### Decision 2: Live Device Picker via `ConnectedDevices`
- **Choice**: Populate device selection from the existing `ConnectedDevices` scanner and host table, showing current hostname, vendor icon, and IP address.
- **Rationale**: Manual MAC entry is tedious and prone to typos. Parents recognize their children's devices by hostname or icon.
- **Alternatives Considered**: Raw text field for MAC addresses (rejected: poor UX), or displaying MAC addresses only without hostname resolution (rejected: unhelpful for non-technical users).

### Decision 3: Atomic Mutation with Final Flash Commit (`ACT_SAVE_CFG`)
- **Choice**: Coordinate multi-instance updates (profile metadata, child device list changes, filter settings, schedules) sequentially and issue a single `ACT_SAVE_CFG` operation upon completion.
- **Rationale**: Saving config after each individual child object addition causes unnecessary flash writes and slows down UI interactions.

### Decision 4: Composite Stack Indexing for Web Insights
- **Choice**: Query `DEV2_PC_VISITRANK` and `DEV2_PC_VISITRANK_ENTRY` using the router's date offset convention (`profileStack` with index 1 replaced by `dateOffset`: `1` for Today, `2` for Yesterday, `3` for 2 days ago).
- **Rationale**: Matches the router firmware's internal indexing discovered in `main_parentCtrl_v2.htm`.

## State Transitions & Mutation Sequence

```
[ User saves profile ]
          |
          v
   Is new profile?
    /         \
  (Yes)       (No)
   /             \
  v               v
ao: DEV2_PC_     so: DEV2_PC_PROFILE (update name, filterLevel)
PROFILE           |
  |               +--> Diff existing vs new devices:
  |               |     - do: DEV2_PC_PROFILE_DEVICE for removed
  |               |     - ao: DEV2_PC_PROFILE_DEVICE for added
  |               +--> Diff blacklist entries (do removed, ao added)
  v               v
so: DEV2_PC_FILTERCONTENT (update category flags)
  |
  v
so: DEV2_PC_WEEKDAY (update timeLimit, bedTime, bitmask index = 31)
  |
  v
so: DEV2_PC_WEEKEND (update timeLimit, bedTime, bitmask index = 96)
  |
  v
op: ACT_SAVE_CFG (commit to router NVRAM)
  |
  v
[ Reload profile list & refresh UI ]
```

## Risks / Trade-offs

- **[Risk] Device reassignment conflict**: A parent might assign a MAC address that is already associated with another child profile.
  - *Mitigation*: The UI detects and flags devices already claimed by other profiles, displaying a clear prompt asking if the device should be moved to the new profile.
- **[Risk] Session timeout during multi-step save**: Router may drop HTTP session midway through batch operations.
  - *Mitigation*: Leverage `RouterClient.isSessionOrStreamFailure` and `RouterViewModel` re-authentication recovery runner to transparently renew token and retry the mutation.
- **[Risk] Offline devices missing from live picker**: A child's device may currently be powered off or out of range.
  - *Mitigation*: The picker includes recently seen offline DHCP clients from `DEV2_HOST_ENTRY` alongside active clients, and preserves already-bound devices even if offline.
- **[Risk] Dark Theme readability for charts and gauges**: Screen time progress bars must maintain accessibility in dark and light modes.
  - *Mitigation*: Use Material 3 Expressive tokens (`primary`, `surfaceContainerHighest`, `outlineVariant`) for progress bars and timeline indicators.
