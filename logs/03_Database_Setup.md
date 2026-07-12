# Phase 3: Database Setup

## Completed
- [x] Created `AppDatabase.kt` in `core/database/`.
- [x] Configured as an abstract `RoomDatabase`.
- [x] Enabled schema export for future migrations.

## Technical Decisions
- Entities array is empty temporarily; will populate feature-by-feature.
- Used `exportSchema = true` to safely track database migrations.

## Next Phase
- Configure Koin Dependency Injection (DatabaseModule, NetworkModule, CoreModule).
