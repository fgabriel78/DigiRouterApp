# Design

## Context

In `DashboardScreen` (`StatusScreens.kt`), the EasyMesh nodes overview renders a `Card` for each active mesh node in a 2-column grid span (`GridItemSpan(2)`).
Currently:
1. Inside the title row, an `AssistChip` displaying "Router Principal" or "Satélite Mesh" is placed next to the device name. Because long device hostnames (e.g. `XGB430v Pro_FD00`) do not leave sufficient horizontal space, the chip gets squeezed, forcing the text to wrap character-by-character into a vertical column ("Rou / ter / Pri / nci / pal").
2. The subtitle text in the satellite card contains model, IP, and backhaul information without truncation constraints, wrapping into multiple lines and making the satellite card significantly taller than the controller card.

## Goals / Non-Goals

**Goals:**
- Guarantee equal card heights across all EasyMesh nodes regardless of hostname length or connection details.
- Identify the primary router (controller) via an icon badge/indicator (e.g. `Icons.Filled.Star`) instead of a text chip, eliminating all vertical text wrapping.
- Constrain both the title and subtitle to single lines (`maxLines = 1`) with clean ellipsis truncation (`TextOverflow.Ellipsis`).
- Maintain accessibility by setting proper content descriptions on icon indicators.

**Non-Goals:**
- Altering the underlying `MeshNode` data model or network polling logic.
- Changing the Devices screen filtering behavior.

## Decisions

### Decision 1: Icon Indicator for Primary Router
- Remove `AssistChip` from the node card header.
- For the controller, render `Icon(Icons.Filled.Star, contentDescription = stringResource(R.string.mesh_role_controller), tint = c.primary, modifier = Modifier.size(16.dp))` adjacent to the node name.
- For satellite agents, no text chip is needed; their satellite role is already indicated by their distinct badge icon (`Icons.Filled.Lan` or `Icons.Filled.Wifi`) and backhaul connection metadata in the subtitle.

*Alternatives considered:*
- Stacking the text chip on a second row: rejected because it increases card height and wastes vertical screen real estate.
- Shortening the text: rejected because user explicitly requested an icon instead of text.

### Decision 2: Strict Single-Line Truncation for Uniform Card Dimensions
- Set `maxLines = 1` and `overflow = TextOverflow.Ellipsis` on both the node name `Text` (with `Modifier.weight(1f, fill = false)`) and the details `Text`.
- Apply uniform padding `16.dp` and vertical centering `Alignment.CenterVertically`.
- This ensures that every node card contains exactly:
  - 1 badge of fixed 52dp diameter
  - 2 lines of text (Title + Subtitle) in the center column
  - 1 trailing client count label on the right
- Thus, all cards have identical heights regardless of text lengths.

## Risks / Trade-offs

- **[Risk]** Very long hostnames or backhaul text will be clipped with ellipsis.
  - **Mitigation**: The most vital prefix information (device brand/model and IP address) remains visible, and clicking or inspecting details in the Devices tab provides full device identifiers.
