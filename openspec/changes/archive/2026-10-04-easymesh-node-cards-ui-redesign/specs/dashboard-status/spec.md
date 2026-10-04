# Spec Delta

## MODIFIED Requirements

### Requirement: EasyMesh Multi-Node Topology and Metrics Overview
The system SHALL aggregate and display operational status, backhaul link health, and client distribution across all EasyMesh nodes in a uniform, single-line card layout when an EasyMesh mesh network is configured.

#### Scenario: Display EasyMesh nodes overview when mesh nodes are active
- **GIVEN** an active EasyMesh network with one controller and one or more agent nodes queried from `DEV2_WIFI_APDEV`
- **WHEN** the summary dashboard renders
- **THEN** it renders an EasyMesh network section with uniform card dimensions for each node, where the primary router is indicated via an icon rather than text badges, and text fields (name, model, IP, and backhaul link info) are truncated with ellipsis on single lines to maintain equal card heights.

#### Scenario: Fallback to standalone router display when EasyMesh is unconfigured
- **GIVEN** `DEV2_WIFI_APDEV` returns no active agent nodes or EasyMesh is disabled
- **WHEN** the summary dashboard renders
- **THEN** it displays the standalone primary router hardware card and standard summary stats without the multi-node mesh section.
