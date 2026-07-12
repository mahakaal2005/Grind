# Phase 4: Dependency Injection (Koin)

## Status: COMPLETED (Session Closed Here)

## Completed
- [x] Created `core/di/DatabaseModule.kt` using Koin's `module` and `single` builder (Room).
- [x] Created `core/di/NetworkModule.kt` (Ktor + Kotlinx Serialization).
- [x] Created `core/di/CoreModule.kt` (DataStore using Property Delegation).
- [x] Initialized Koin in `GrindApp.kt` and registered in Manifest.
- [x] All changes committed.

## Technical Decisions
- **Koin vs Hilt:** We are using Koin because it is a lightweight, Kotlin-first service locator that doesn't require complex code generation or heavy annotations like Hilt.
- **Singletons:** Core resources (DB, Network, DataStore) are provided as singletons (`single`) to prevent memory leaks and redundant instantiation.

## Next Session Instructions
1. **Agent Behavior Rules (CRITICAL):**
   - The user is building a production-grade app and preparing for FAANG/Google interviews. Do NOT gloss over details. 
   - Explain the *mechanics* of every line of code word-by-word (e.g., classes, objects, constructors, trailing lambdas, property delegation, scoping).
   - Adhere STRICTLY to the rules in `docs/ultimate_android_architecture.md`. Do not hardcode strings if a `Constants` file exists.
   - Treat the user like an engineer who needs to understand the "why", not just copy-paste the "how".
2. **Next Steps:** Proceed to Phase 5. Review MVP docs to decide the next feature to build (likely Data/Domain layer implementation).
