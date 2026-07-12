# Phase 2: Core Foundations & Dependencies

## Completed
- [x] Created `Resource.kt` (Sealed class for Loading/Success/Error states).
- [x] Created `Constants.kt` (Centralized magic strings).
- [x] Implemented Version Catalog (`libs.versions.toml`).
- [x] Added core dependencies: Compose, Koin, Ktor, Room, DataStore, Kotlin Serialization.
- [x] Fixed KSP & Room version conflicts.

## Technical Decisions
- **Dependency Injection:** Koin (lightweight, Kotlin-first).
- **Networking:** Ktor Client (coroutine-native).
- **Local Storage:** Jetpack DataStore (preferences) and Room (relational data).
- **Annotation Processing:** KSP (modern alternative to KAPT).

## Next Phase
- Move to Database Setup.
