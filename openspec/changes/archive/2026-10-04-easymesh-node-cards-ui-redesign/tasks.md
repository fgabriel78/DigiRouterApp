# Tasks

## 1. EasyMesh Node Card Layout Refinement

- [x] 1.1 Replace the role text `AssistChip` with an icon indicator (`Icons.Filled.Star`) for the primary router in `DashboardScreen` of `app/src/main/kotlin/es/routerapp/app/StatusScreens.kt`, removing vertical text wrapping.
- [x] 1.2 Apply single-line ellipsis truncation (`maxLines = 1`, `overflow = TextOverflow.Ellipsis`) to both title and subtitle texts in `StatusScreens.kt` to ensure uniform card heights across all mesh nodes.
- [x] 1.3 Verify compilation and UI test builds with `./gradlew test` and `./gradlew assembleDebug` to confirm all tasks pass cleanly.
