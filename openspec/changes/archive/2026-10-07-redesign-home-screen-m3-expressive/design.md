# Design

## Context

See `proposal.md` for problem background and motivation. Currently, `MainActivity.kt` renders `HomeScreen` using a `LazyVerticalGrid(columns = GridCells.Fixed(2))` mapping every category into uniform square `PageTile` cards. Because categories have odd item counts (3 in *Estado*, 3 in *Red local*, 5 in *Puertos y NAT*, 1 in *USB*), the grid leaves orphan cards stranded with empty holes. Furthermore, varying title lengths cause uneven vertical heights between columns, and all 23 settings share identical visual priority.

## Goals / Non-Goals

**Goals:**
- Provide a clear, modern visual hierarchy adhering to Material Design 3 Expressive guidelines.
- Add an Expressive Hero Status Card at the top displaying the router model (`DIGI XGB430v Pro`), connection state, IP address, and a direct tap target into full system diagnostics.
- Implement an equal-height 2x2 Quick Access Bento Grid for daily tasks: *Summary*, *Connected Devices*, *Speed Test*, and *Main Wi-Fi*.
- Implement M3 Expressive Grouped Container Cards for remaining categories, rendering items as clean horizontal rows with leading shape badges and trailing navigation chevrons.
- Use M3 tonal surface container styling (`surfaceContainer` / `surfaceContainerLow`) with vibrant tinted icon badges across all home screen components.

**Non-Goals:**
- Modifying underlying catalog metadata, router protocols, or individual configuration screens.
- Introducing bottom navigation bars or multi-tab routing (retaining a unified home hub).

## Decisions

### 1. Information Architecture & Section Segregation
- **Decision**: Segregate the home screen into three distinct visual tiers:
  1. **Hero Status Glance**: Top card showing router identity, connection status pill, and direct navigation to `dashboard`.
  2. **Quick Access Bento (2x2)**: Dedicated 4-tile grid for `dashboard`, `devices`, `speedtest`, and `wifiMain`.
  3. **Categorized Setting Containers**: Unified card containers for remaining categories (*Wi-Fi Settings*, *Local Network*, *Ports & NAT*, *Security*, *USB Storage*, *System*).
- **Alternatives considered**:
  - *Keep 2-column grid with dynamic spans*: While smart spanning prevents empty holes, it leaves the screen visually flat and does not provide enough horizontal space for longer text strings.
  - *Bottom navigation bar*: Splits items across tabs, which adds unnecessary navigation friction when accessing less-frequent settings.

### 2. Component Structure in `MainActivity.kt`
- **Decision**:
  - `HeroCard(routerModel, ipAddress, isOnline, onClick)`:
    - Card container (`surfaceContainer`) with prominent router icon badge, status pill, and subtitle.
  - `QuickAccessGrid(pages, onOpen)`:
    - 2-column layout with fixed-height cards (`112.dp`).
    - Stacks icon, title, and descriptive hint with equal vertical padding, guaranteeing symmetrical heights.
  - `CategoryGroupContainer(groupTitle, pages, onOpen)`:
    - Card container (`surfaceContainerLow`, `RoundedCornerShape(24.dp)`).
    - Contains horizontal `CategoryItemRow` items with `ShapeBadge`, title, optional description, and a subtle trailing arrow chevron (`>`).
    - Applies subtle horizontal dividers between items inside each container.

### 3. Tonal Color System & Expressive Shapes
- **Decision**:
  - Replace solid card color blocks (yellow/olive) with neutral M3 surface containers (`surfaceContainer`, `surfaceContainerLow`).
  - Use vibrant `ShapeBadge` containers with semantic color roles:
    - Primary (Teal/Cyan/Blue) for Wi-Fi and connectivity.
    - Tertiary (Purple/Indigo) for Speed Test and performance.
    - Amber/Coral for Security and firewall.
    - Secondary/Slate for System and administrative tools.
  - Use M3 expressive radii (`24.dp` to `28.dp`).

## Risks / Trade-offs

- **[Risk] Longer text in Spanish translations causing overflow**
  → *Mitigation*: Grouped container rows arrange text horizontally with flexible column budgets. Bento quick access cards use concise titles with graceful text truncation (`TextOverflow.Ellipsis`).
- **[Risk] Redundant presence of Quick Access items in lower categories**
  → *Mitigation*: Filter out the 4 primary pages (`dashboard`, `devices`, `speedtest`, `wifiMain`) from the lower grouped containers to maintain a clean, non-duplicated information architecture.
