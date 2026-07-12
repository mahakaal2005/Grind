# Phase 1: Architecture & Layers

## Completed
- [x] Defined feature-driven Clean Architecture structure (`core` and `features` packages).
- [x] Adopted MVI (Unidirectional Data Flow).
- [x] Scaffolding created for `core` sub-packages (common, database, network, di, domain, data, presentation).
- [x] Scaffolding created for `features/feature_onboarding` (domain, data, presentation, di).
- [x] Migrated Design System (`Theme.kt`, `Color.kt`, `Type.kt`) to `core/presentation/designsystem`.

## Technical Decisions
- **Domain:** Pure Kotlin. Defines models and interfaces.
- **Data:** Implements domain interfaces. Handles Room (local) and Ktor (remote).
- **Presentation:** Handles UI (Compose) and State (ViewModel).

## Next Phase
- Move to Gradle & Dependencies.
