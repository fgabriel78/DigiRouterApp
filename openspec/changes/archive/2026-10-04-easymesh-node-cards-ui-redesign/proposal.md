# Proposal

## Why

In the EasyMesh network overview on the Summary ("Resumen") screen, node cards currently exhibit uneven heights and poor typography. Specifically, long device names squeeze the "Router Principal" label into a narrow horizontal space, causing its text to wrap vertically into an awkward column of letters ("Rou / ter / Pri / nci / pal"). Additionally, the second node's details wrap onto multiple lines causing that card to be much taller than the primary router's card. This change refines the card layout so that all mesh node cards have uniform dimensions, truncates overflowing text with ellipsis, and replaces the text chip with a clean icon-based indicator for the primary router.

## What Changes

- **Uniform Card Dimensions**:
  - Enforce equal heights and balanced padding across all EasyMesh node cards in the Summary screen.
- **Text Truncation**:
  - Truncate node names with ellipsis (`TextOverflow.Ellipsis`) on a single line (`maxLines = 1`) to prevent pushing adjacent layout elements.
  - Truncate subtitle metadata (model, IP, backhaul medium) on a single line with ellipsis to prevent multi-line card expansion.
- **Icon-Based Role Identification**:
  - Replace the text-based `AssistChip` ("Router Principal") with a distinct icon indicator (e.g., star/primary badge icon) displayed alongside the node title, eliminating awkward vertical text wrapping.
  - Retain clear visual distinction between the main router (controller) and satellite mesh agents through badge icons and colors.

## Capabilities

### New Capabilities
*(None)*

### Modified Capabilities
- `dashboard-status`: Refine the EasyMesh multi-node overview requirement to mandate uniform card sizing, single-line text truncation with ellipsis, and icon-based primary router identification without text-based vertical chips.

## Impact

- **UI Layer (`StatusScreens.kt`)**:
  - Refactor the EasyMesh node card composable inside `DashboardScreen`.
  - Apply `TextOverflow.Ellipsis`, `maxLines = 1`, and flexible weight distribution.
  - Replace the role `AssistChip` with an icon badge/indicator for the controller.
- **Resources**:
  - No breaking string or protocol API changes; existing data structures (`MeshNode`) remain intact.
