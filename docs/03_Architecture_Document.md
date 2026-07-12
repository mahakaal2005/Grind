# Architecture Document — Thali
### Applied Clean Architecture + Feature-Driven + MVI/Unidirectional Data Flow

---

## 1. Executive Summary

This document describes the complete technical architecture for Thali — an AI-powered meal tracker built with **Kotlin, Android, Jetpack Compose, and Room**. The architecture follows:

- **Clean Architecture** — strict layering (Domain → Data → Presentation)
- **Feature-Driven Organization** — each feature is an isolated "mini-app"
- **MVVM + MVI** — use `ViewModel` for lifecycle, but apply Unidirectional Data Flow for state management
- **Use Case–Driven Business Logic** — every user action = one dedicated class in `domain/use_case/`
- **Offline-First with Room** — local database is the single source of truth; all data persists locally first

This architecture ensures:
- ✅ **Testability** — domain logic is decoupled from UI and networking
- ✅ **Scalability** — features are isolated; teams can work in parallel
- ✅ **Maintainability** — strict layering prevents spaghetti dependencies
- ✅ **Reusability** — use cases and repositories can be reused across screens
- ✅ **Adaptability** — switching the AI API backend is a single interface swap

---

## 2. Core Architectural Principles (The Four Pillars)

### A. Feature-Driven (Package by Feature, Not by Type)

**Instead of:**
```
com.example.thali/
├── ui/
│   ├── MainActivity.kt
│   ├── screens/
│   ├── viewmodels/
├── data/
├── domain/
```

**We do:**
```
com.example.thali/
├── feature_onboarding/
│   ├── domain/
│   ├── data/
│   ├── presentation/
├── feature_meal_logging/
│   ├── domain/
│   ├── data/
│   ├── presentation/
├── feature_history/
├── core/  <-- Shared across all features
```

**Why:**
- Each feature is a self-contained "mini-app" with all layers.
- Deleting a feature (e.g., onboarding after V1) = delete one folder. No orphaned code.
- Future: easily extract any feature into a separate Gradle module for testing or multi-team development.

---

### B. Clean Architecture: Strict Layering

Within every feature, code is organized by responsibility:

```
Presentation (UI) → Domain (Business Rules) ← Data (Network + Database)
```

**The Dependency Rule (NEVER VIOLATED):**
```
Presentation → Domain ← Data
Domain ← nothing (Domain imports nothing)
```

#### Domain Layer: The Brain
- **Zero Android dependencies.** Pure Kotlin. Could run on a server.
- **Contains:** Models, Repository interfaces, Use Cases
- **Responsibility:** Business rules, validation, orchestration
- **Example:** "A login requires an email and password. An email must have an @ sign."

#### Data Layer: The Worker
- **Contains:** Room DAOs, Retrofit APIs, mappers, repository implementations
- **Responsibility:** Fetching, caching, persisting data
- **Responsibility:** Deciding when to refresh vs. serve from cache
- **Example:** "Get meals from Room. If Room is empty, fetch from API, save to Room, then return."

#### Presentation Layer: The Face
- **Contains:** Jetpack Compose screens, ViewModels, State/Action classes
- **Responsibility:** Drawing UI and responding to user taps
- **Responsibility:** Observing State and sending Actions
- **Example:** "When the user taps 'Login', send LoginAction.OnLoginClicked to the ViewModel."

---

### C. Use Case–Driven Domain Logic

A **Use Case** is a single, testable Kotlin class representing **one user action**.

```kotlin
// domain/use_case/LogMealUseCase.kt
class LogMealUseCase(
    private val mealRepository: MealRepository,
    private val foodMemoryRepository: FoodMemoryRepository
) {
    suspend operator fun invoke(meal: Meal): Resource<Unit> {
        // Validate meal has at least one food item
        if (meal.foodItems.isEmpty()) {
            return Resource.Error("A meal must contain at least one food item")
        }
        // Ensure total calories are reasonable (sanity check)
        if (meal.totalCalories < 50 || meal.totalCalories > 5000) {
            return Resource.Error("Total calories seem unreasonable. Please review.")
        }
        // Delegate to repository for persistence
        return mealRepository.saveMeal(meal)
    }
}
```

**Why not just call the Repository from the ViewModel?**
- ViewModels get fat fast. A "Log Meal" action might need to validate, compute macros, save to two repositories, and trigger a notification. This belongs in a Use Case, not scattered in the ViewModel.
- Use Cases are trivially easy to unit test — no UI mocking needed.
- Use Cases can call other Use Cases, building complex workflows from simple, reusable pieces.

**Use Case Naming Conventions:**
| Prefix      | When to use                           | Example                    |
|-------------|---------------------------------------|----------------------------|
| `Get`       | Reading data (one-shot)              | `GetMealHistoryUseCase`    |
| `Observe`   | Reading data (live Flow)             | `ObserveUserTargetsUseCase` |
| `Submit`    | Sending data (to API or DB)          | `SubmitMealUseCase`        |
| `Validate`  | Pure local logic, no network or DB   | `ValidateMealUseCase`      |
| `Delete`    | Removing data                        | `DeleteMealUseCase`        |
| `Execute`   | Complex multi-step workflows         | `ExecuteMealClarificationUseCase` |

---

### D. MVVM + MVI: Unidirectional Data Flow

**Architecture:** MVVM (ViewModel survives config changes)  
**Pattern:** MVI (Unidirectional Data Flow — Action → ViewModel → State)

```
User taps "Log Meal"
          ↓
  Screen sends LoginAction.OnLoginClicked
          ↓
  ViewModel receives Action
          ↓
  ViewModel calls LoginUseCase
          ↓
  UseCase returns Resource<User>
          ↓
  ViewModel unpacks Resource into State fields
          ↓
  State updates (immutable copy)
          ↓
  UI observes State and recomposes
```

**Key:** Data flows in **ONE direction only**. The UI never pushes data back up; it only sends discrete Actions.

---

## 3. Thali Feature Breakdown and Folder Structure

Based on the MVP and build order, Thali will have these features:

```
com.example.thali/
│
├── core/                                  ← SHARED TOOLBOX
│   ├── common/
│   │   ├── Resource.kt                    ← Sealed class for Success/Error/Loading
│   │   └── Constants.kt                   ← Timeouts, URLs, config
│   │
│   ├── domain/
│   │   └── model/                         ← Shared domain models (User, Session, etc.)
│   │
│   ├── data/
│   │   └── preference/                    ← DataStore for app-wide preferences
│   │
│   ├── network/
│   │   ├── KtorHttpClient.kt              ← Ktor client config
│   │   └── interceptors/                  ← Auth, logging, etc.
│   │
│   ├── database/
│   │   └── AppDatabase.kt                 ← Room database with all DAOs
│   │
│   ├── presentation/
│   │   ├── components/                    ← Shared Compose widgets
│   │   ├── designsystem/                  ← Theme, colors, typography
│   │   └── navigation/                    ← Top-level nav graph (Jetpack Nav Compose)
│   │
│   └── di/
│       ├── KoinModule.kt                  ← Entry point for all Koin modules
│       ├── NetworkModule.kt
│       ├── DatabaseModule.kt
│       └── CoreModule.kt
│
├── feature_onboarding/                    ← PHASE 1: Onboarding
│   ├── domain/
│   │   ├── model/
│   │   │   ├── UserProfile.kt
│   │   │   └── DailyTargets.kt
│   │   ├── repository/
│   │   │   └── OnboardingRepository.kt
│   │   └── use_case/
│   │       ├── ComputeTargetsUseCase.kt
│   │       ├── SaveUserProfileUseCase.kt
│   │       └── ValidateBasicInfoUseCase.kt
│   │
│   ├── data/
│   │   ├── local/
│   │   │   ├── UserProfileDao.kt
│   │   │   ├── UserProfileEntity.kt
│   │   │   └── OnboardingPreferences.kt
│   │   ├── mapper/
│   │   │   └── UserProfileMapper.kt
│   │   └── repository/
│   │       └── OnboardingRepositoryImpl.kt
│   │
│   ├── presentation/
│   │   ├── basic_info/
│   │   │   ├── BasicInfoState.kt
│   │   │   ├── BasicInfoAction.kt
│   │   │   ├── BasicInfoViewModel.kt
│   │   │   └── BasicInfoScreen.kt
│   │   │
│   │   ├── goal_preference/
│   │   │   ├── GoalPreferenceState.kt
│   │   │   ├── GoalPreferenceAction.kt
│   │   │   ├── GoalPreferenceViewModel.kt
│   │   │   └── GoalPreferenceScreen.kt
│   │   │
│   │   └── target_confirmation/
│   │       ├── TargetConfirmationState.kt
│   │       ├── TargetConfirmationAction.kt
│   │       ├── TargetConfirmationViewModel.kt
│   │       └── TargetConfirmationScreen.kt
│   │
│   └── di/
│       └── OnboardingModule.kt
│
├── feature_home_dashboard/                ← PHASE 1: Daily dashboard
│   ├── domain/
│   │   ├── model/
│   │   │   ├── DailyMealSummary.kt
│   │   │   └── MacroBreakdown.kt
│   │   ├── repository/
│   │   │   └── DailyDashboardRepository.kt
│   │   └── use_case/
│   │       ├── GetTodaysMealsUseCase.kt
│   │       ├── ComputeMacroProgressUseCase.kt
│   │       └── ObserveDailyProgressUseCase.kt
│   │
│   ├── data/
│   │   ├── local/
│   │   │   └── (Shares MealDao from core/database)
│   │   ├── mapper/
│   │   └── repository/
│   │       └── DailyDashboardRepositoryImpl.kt
│   │
│   ├── presentation/
│   │   ├── HomeState.kt
│   │   ├── HomeAction.kt
│   │   ├── HomeViewModel.kt
│   │   └── HomeScreen.kt
│   │
│   └── di/
│       └── HomeModule.kt
│
├── feature_manual_entry/                  ← PHASE 1: Manual food search + entry
│   ├── domain/
│   │   ├── model/
│   │   │   ├── FoodItem.kt
│   │   │   └── SearchResult.kt
│   │   ├── repository/
│   │   │   └── FoodRepository.kt
│   │   └── use_case/
│   │       ├── SearchFoodUseCase.kt
│   │       ├── ValidateFoodQuantityUseCase.kt
│   │       └── GetCommonFoodsUseCase.kt
│   │
│   ├── data/
│   │   ├── local/
│   │   │   ├── FoodDao.kt
│   │   │   ├── FoodEntity.kt
│   │   │   └── common_foods.json (bundled seed data)
│   │   ├── mapper/
│   │   │   └── FoodMapper.kt
│   │   └── repository/
│   │       └── FoodRepositoryImpl.kt
│   │
│   ├── presentation/
│   │   ├── ManualEntryState.kt
│   │   ├── ManualEntryAction.kt
│   │   ├── ManualEntryViewModel.kt
│   │   └── ManualEntryScreen.kt
│   │
│   └── di/
│       └── ManualEntryModule.kt
│
├── feature_camera/                        ← PHASE 2: Photo capture
│   ├── domain/
│   │   ├── model/
│   │   │   └── CameraConfig.kt
│   │   ├── repository/
│   │   │   └── CameraRepository.kt
│   │   └── use_case/
│   │       ├── RequestCameraPermissionUseCase.kt
│   │       └── ProcessCapturedPhotoUseCase.kt
│   │
│   ├── data/
│   │   ├── local/
│   │   │   └── PhotoCache.kt (temp file storage)
│   │   └── repository/
│   │       └── CameraRepositoryImpl.kt
│   │
│   ├── presentation/
│   │   ├── CameraState.kt
│   │   ├── CameraAction.kt
│   │   ├── CameraViewModel.kt
│   │   └── CameraScreen.kt
│   │
│   └── di/
│       └── CameraModule.kt
│
├── feature_ai_analysis/                   ← PHASE 2: AI food identification
│   ├── domain/
│   │   ├── model/
│   │   │   ├── IdentifiedFood.kt
│   │   │   ├── AiResponse.kt
│   │   │   └── FoodConfidence.kt
│   │   ├── repository/
│   │   │   └── AiRepository.kt
│   │   └── use_case/
│   │       ├── AnalyzeMealPhotoUseCase.kt
│   │       ├── DetermineClarificationNeededUseCase.kt
│   │       └── ValidateAiResponseUseCase.kt
│   │
│   ├── data/
│   │   ├── remote/
│   │   │   ├── AiApiClient.kt (Ktor interface)
│   │   │   └── AiResponseDto.kt
│   │   ├── mapper/
│   │   │   └── AiResponseMapper.kt
│   │   └── repository/
│   │       └── AiRepositoryImpl.kt
│   │
│   ├── presentation/
│   │   ├── analyzing/
│   │   │   ├── AnalyzingState.kt
│   │   │   ├── AnalyzingAction.kt
│   │   │   ├── AnalyzingViewModel.kt
│   │   │   └── AnalyzingScreen.kt
│   │   │
│   │   └── result/
│   │       ├── AiResultState.kt
│   │       ├── AiResultAction.kt
│   │       ├── AiResultViewModel.kt
│   │       └── AiResultScreen.kt
│   │
│   └── di/
│       └── AiModule.kt
│
├── feature_clarification/                 ← PHASE 3: Confidence-gated clarification chat
│   ├── domain/
│   │   ├── model/
│   │   │   ├── ClarificationQuestion.kt
│   │   │   ├── ClarificationContext.kt
│   │   │   └── ReconciliationResult.kt
│   │   ├── repository/
│   │   │   └── ClarificationRepository.kt
│   │   └── use_case/
│   │       ├── GenerateClarificationQuestionsUseCase.kt
│   │       ├── ReconcileMealWithAnswersUseCase.kt
│   │       └── ValidateClarificationAnswersUseCase.kt
│   │
│   ├── data/
│   │   ├── remote/
│   │   │   └── ClarificationAiClient.kt
│   │   ├── mapper/
│   │   └── repository/
│   │       └── ClarificationRepositoryImpl.kt
│   │
│   ├── presentation/
│   │   ├── ClarificationChatState.kt
│   │   ├── ClarificationChatAction.kt
│   │   ├── ClarificationChatViewModel.kt
│   │   ├── ClarificationChatScreen.kt
│   │   └── components/
│   │       ├── QuestionCard.kt
│   │       └── ChipReplyRow.kt
│   │
│   └── di/
│       └── ClarificationModule.kt
│
├── feature_meal_confirmation/             ← PHASE 2-3: Confirm & log screen
│   ├── domain/
│   │   ├── model/
│   │   │   └── ConfirmationData.kt
│   │   ├── repository/
│   │   │   └── MealConfirmationRepository.kt
│   │   └── use_case/
│   │       ├── ConfirmAndLogMealUseCase.kt
│   │       ├── UpdateMealMacrosUseCase.kt
│   │       └── SaveAsFoodMemoryUseCase.kt
│   │
│   ├── data/
│   │   ├── local/
│   │   │   ├── MealDao.kt
│   │   │   └── MealEntity.kt
│   │   ├── mapper/
│   │   └── repository/
│   │       └── MealConfirmationRepositoryImpl.kt
│   │
│   ├── presentation/
│   │   ├── ConfirmationState.kt
│   │   ├── ConfirmationAction.kt
│   │   ├── ConfirmationViewModel.kt
│   │   ├── ConfirmationScreen.kt
│   │   └── components/
│   │       ├── FoodItemEditor.kt
│   │       └── MacroSummary.kt
│   │
│   └── di/
│       └── ConfirmationModule.kt
│
├── feature_food_memory/                   ← PHASE 4: Personal food memory
│   ├── domain/
│   │   ├── model/
│   │   │   ├── SavedMealTemplate.kt
│   │   │   └── FoodMemoryMatch.kt
│   │   ├── repository/
│   │   │   └── FoodMemoryRepository.kt
│   │   └── use_case/
│   │       ├── SaveMealAsTemplateUseCase.kt
│   │       ├── FindSimilarSavedMealsUseCase.kt
│   │       └── DeleteFoodMemoryUseCase.kt
│   │
│   ├── data/
│   │   ├── local/
│   │   │   ├── SavedMealDao.kt
│   │   │   ├── SavedMealEntity.kt
│   │   │   └── FoodMemoryPreferences.kt
│   │   ├── mapper/
│   │   │   └── SavedMealMapper.kt
│   │   └── repository/
│   │       └── FoodMemoryRepositoryImpl.kt
│   │
│   ├── presentation/
│   │   ├── FoodMemoryState.kt
│   │   ├── FoodMemoryAction.kt
│   │   ├── FoodMemoryViewModel.kt
│   │   └── FoodMemoryScreen.kt
│   │
│   └── di/
│       └── FoodMemoryModule.kt
│
├── feature_history/                       ← PHASE 4-5: Meal history & edit
│   ├── domain/
│   │   ├── model/
│   │   │   └── HistoryMeal.kt
│   │   ├── repository/
│   │   │   └── HistoryRepository.kt
│   │   └── use_case/
│   │       ├── GetMealHistoryUseCase.kt
│   │       ├── GetMealDetailUseCase.kt
│   │       ├── UpdateMealUseCase.kt
│   │       └── DeleteMealUseCase.kt
│   │
│   ├── data/
│   │   ├── local/
│   │   │   └── (Shares MealDao with core/database)
│   │   ├── mapper/
│   │   └── repository/
│   │       └── HistoryRepositoryImpl.kt
│   │
│   ├── presentation/
│   │   ├── history_list/
│   │   │   ├── HistoryListState.kt
│   │   │   ├── HistoryListAction.kt
│   │   │   ├── HistoryListViewModel.kt
│   │   │   └── HistoryListScreen.kt
│   │   │
│   │   └── meal_detail/
│   │       ├── MealDetailState.kt
│   │       ├── MealDetailAction.kt
│   │       ├── MealDetailViewModel.kt
│   │       └── MealDetailScreen.kt
│   │
│   └── di/
│       └── HistoryModule.kt
│
├── feature_settings/                      ← PHASE 5: Profile & settings
│   ├── domain/
│   │   ├── model/
│   │   │   └── UserSettings.kt
│   │   ├── repository/
│   │   │   └── SettingsRepository.kt
│   │   └── use_case/
│   │       ├── GetUserSettingsUseCase.kt
│   │       ├── UpdateTargetsUseCase.kt
│   │       ├── UpdateDietaryPreferenceUseCase.kt
│   │       └── ManageFoodMemoryUseCase.kt
│   │
│   ├── data/
│   │   ├── local/
│   │   │   └── SettingsPreferences.kt
│   │   ├── mapper/
│   │   └── repository/
│   │       └── SettingsRepositoryImpl.kt
│   │
│   ├── presentation/
│   │   ├── SettingsState.kt
│   │   ├── SettingsAction.kt
│   │   ├── SettingsViewModel.kt
│   │   └── SettingsScreen.kt
│   │
│   └── di/
│       └── SettingsModule.kt
│
└── MainActivity.kt                        ← Entry point

```

---

## 4. Critical Design Decision: Resource<T> vs State

### The Problem

Your `AnalyzeMealPhotoUseCase` returns `Resource<AiResponse>` (Success/Error/Loading).  
Your `AnalyzingState` is what the UI observes.  
**Where do these two meet?**

### Option A (Wrong): Store Resource Directly in State

```kotlin
// ❌ DO NOT DO THIS
data class AnalyzingState(
    val aiResult: Resource<AiResponse>? = null
)

// UI would have to:
when (state.aiResult) {
    is Resource.Loading -> ShowSpinner()
    is Resource.Success -> ShowResult(state.aiResult.data)
    is Resource.Error -> ShowError(state.aiResult.message)
}
```

**Problems:**
- Leaks data-layer abstractions into the UI
- No clean place for transient UI state like `isCameraFlashEnabled`
- The UI becomes tangled in when/is checks

### Option B (Correct): Unpack Resource into Flat Fields

```kotlin
// ✅ ALWAYS DO THIS
data class AnalyzingState(
    val isLoading: Boolean = false,           // From Resource.Loading
    val identifiedFoods: List<IdentifiedFood>? = null,  // From Resource.Success
    val errorMessage: String? = null,         // From Resource.Error
    // Plus UI-specific fields:
    val isCameraFlashEnabled: Boolean = false,
    val analysisElapsedMs: Long = 0L
)
```

**Why this works:**
- The UI never sees `Resource`. It only sees clean, flat fields.
- Business logic (loading/error) is kept separate from UI state.
- You have a natural place for every piece of transient UI state.

### The ViewModel's Job: Unpacking

```kotlin
private fun analyzeMeal(photoPath: String) = viewModelScope.launch {
    // 1. Signal loading state BEFORE calling the use case
    _state.update { it.copy(isLoading = true, errorMessage = null) }

    // 2. Call the use case
    val result = analyzeMealPhotoUseCase(photoPath)

    // 3. Unpack the Resource into flat fields
    when (result) {
        is Resource.Success -> {
            _state.update { it.copy(
                isLoading = false,
                identifiedFoods = result.data.foods,
                errorMessage = null
            ) }
            // Decide: does this need clarification chat?
            if (needsClarification(result.data)) {
                // Route to clarification flow
            } else {
                // Route to result/confirmation
            }
        }
        is Resource.Error -> {
            _state.update { it.copy(
                isLoading = false,
                errorMessage = result.message
            ) }
        }
        is Resource.Loading -> {
            // Usually already set above, but if the use case emits Resource.Loading mid-stream:
            _state.update { it.copy(isLoading = true) }
        }
    }
}
```

### The Firm Rule
> **`Resource<T>` lives in Domain and Data layers only.**  
> **By the time data reaches the State, it must be unpacked into flat, explicit fields.**  
> **The UI layer never imports `Resource` directly.**

---

## 5. Data Flow: A Complete Example

### Use Case: "Log a Meal After Clarification"

```
1. USER TAPS "Confirm & Log"
   ↓
   HomeScreen sends ConfirmationAction.OnConfirmAndLogClicked to ViewModel
   ↓

2. VIEWMODEL RECEIVES ACTION
   fun onAction(action: ConfirmationAction) {
       when (action) {
           is ConfirmationAction.OnConfirmAndLogClicked -> logMeal()
       }
   }
   ↓

3. VIEWMODEL CALLS USE CASE
   private fun logMeal() = viewModelScope.launch {
       val confirmationData = state.value.toMealEntity()
       val result = confirmAndLogMealUseCase(confirmationData)
       // Unpack result into state
   }
   ↓

4. USE CASE EXECUTES BUSINESS LOGIC
   class ConfirmAndLogMealUseCase(
       private val mealRepository: MealConfirmationRepository,
       private val validateMealUseCase: ValidateMealUseCase
   ) {
       suspend operator fun invoke(meal: Meal): Resource<Unit> {
           // Validate meal
           val validation = validateMealUseCase(meal)
           if (validation is Resource.Error) return validation

           // Delegate to repository
           return mealRepository.saveMeal(meal)
       }
   }
   ↓

5. REPOSITORY PERSISTS DATA
   override suspend fun saveMeal(meal: Meal): Resource<Unit> = try {
       mealDao.insert(meal.toEntity())
       // Optionally, offer to save as Food Memory
       Resource.Success(Unit)
   } catch (e: Exception) {
       Resource.Error("Failed to save meal: ${e.message}")
   }
   ↓

6. USE CASE RETURNS RESOURCE<UNIT>
   ↓

7. VIEWMODEL UNPACKS AND UPDATES STATE
   when (result) {
       is Resource.Success -> {
           _state.update { it.copy(isLoading = false, saved = true) }
           // Navigate to home
       }
       is Resource.Error -> {
           _state.update { it.copy(isLoading = false, error = result.message) }
       }
   }
   ↓

8. STATE CHANGES
   ↓

9. UI OBSERVES STATE AND RECOMPOSES
   val state by viewModel.state.collectAsStateWithLifecycle()
   if (state.saved) {
       // Show toast, navigate home
   }
   if (state.error != null) {
       ErrorDialog(state.error)
   }
```

---

## 6. Dependency Injection with Koin

**Every feature has a Koin module that wires up all its dependencies.**

```kotlin
// feature_meal_confirmation/di/ConfirmationModule.kt
val confirmationModule = module {
    single<MealConfirmationRepository> {
        MealConfirmationRepositoryImpl(
            mealDao = get(),  // From DatabaseModule
            foodMemoryRepository = get()  // From FoodMemoryModule
        )
    }

    factory { ValidateMealUseCase() }  // No dependencies

    factory { ConfirmAndLogMealUseCase(
        mealRepository = get(),
        validateMealUseCase = get()
    ) }

    factory { SaveAsFoodMemoryUseCase(
        foodMemoryRepository = get()
    ) }

    viewModel { ConfirmationViewModel(
        confirmAndLogMealUseCase = get(),
        saveAsFoodMemoryUseCase = get()
    ) }
}
```

**In your `Application` class:**

```kotlin
class ThaliApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@ThaliApplication)
            modules(
                // Core modules (loaded first)
                coreModule,
                networkModule,
                databaseModule,

                // Feature modules (order doesn't matter if they use Koin)
                onboardingModule,
                homeModule,
                manualEntryModule,
                cameraModule,
                aiModule,
                clarificationModule,
                confirmationModule,
                foodMemoryModule,
                historyModule,
                settingsModule
            )
        }
    }
}
```

---

## 7. Offline-First Strategy: Room is the Single Source of Truth

**Core Principle:** The local Room database is always read first. The network is only used to refresh or backfill.

```kotlin
// feature_history/data/repository/HistoryRepositoryImpl.kt
override fun getMealHistory(date: LocalDate): Flow<Resource<List<HistoryMeal>>> = flow {
    // Step 1: Emit loading state with cached data
    emit(Resource.Loading())
    val cachedMeals = mealDao.getMealsForDate(date).map { it.toDomain() }
    emit(Resource.Loading(data = cachedMeals))

    // Step 2: If offline, serve cached data
    if (!networkMonitor.isOnline()) {
        emit(Resource.Success(cachedMeals))
        return@flow
    }

    // Step 3: Try to fetch fresh data (optional for V1 — all data is local)
    // In V1, we skip this because all data is local.

    // Step 4: Emit final success with cached data
    emit(Resource.Success(cachedMeals))
}
```

**Why this works for Thali:**
- All meal logging is device-local (no cloud sync in V1).
- Users never lose a meal — it's in Room before they see a confirmation toast.
- The app works offline by default.
- Network calls (to AI APIs) are for *reading* (analysis), not *writing* (persistence).

---

## 8. Navigation: Jetpack Navigation Compose

**All screen transitions are declared in a type-safe nav graph in `core/presentation/navigation/`.**

```kotlin
// core/presentation/navigation/ThaliNavigation.kt
sealed class Route {
    data object Onboarding : Route()
    data object Home : Route()
    data class Camera(val loggingContext: String) : Route()
    data class Analyzing(val photoPath: String) : Route()
    data class Confirmation(val mealId: String) : Route()
    data class ClarificationChat(val mealId: String) : Route()
    data class MealDetail(val mealId: String) : Route()
}

@Composable
fun ThaliNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(navController, startDestination = Route.Home) {
        composable<Route.Home> { HomeScreen(navController) }
        composable<Route.Camera> { backStackEntry ->
            val loggingContext = backStackEntry.toRoute<Route.Camera>().loggingContext
            CameraScreen(loggingContext, navController)
        }
        composable<Route.Confirmation> { backStackEntry ->
            val mealId = backStackEntry.toRoute<Route.Confirmation>().mealId
            ConfirmationScreen(mealId, navController)
        }
        // ... more screens
    }
}
```

**Benefits:**
- Type-safe routes (no string-based "destination" keys).
- Arguments are serializable (no boilerplate).
- Back stack is handled automatically.

---

## 9. Testing Strategy (Overview)

### Domain Layer (Unit Tests) — The Easiest

```kotlin
// feature_ai_analysis/domain/use_case/DetermineClarificationNeededUseCase_Test.kt
class DetermineClarificationNeededUseCase_Test {
    @Test
    fun lowConfidenceOnMixedDish_returnsTrue() {
        val aiResponse = AiResponse(
            foods = listOf(
                IdentifiedFood("dal", confidence = 0.65),
                IdentifiedFood("roti", confidence = 0.60),
                IdentifiedFood("rice", confidence = 0.55)
            )
        )
        val result = DetermineClarificationNeededUseCase().invoke(aiResponse)
        assertTrue(result)
    }
}
```

### Data Layer (Integration Tests with Room) — Medium Difficulty

```kotlin
@RunWith(AndroidJUnit4::class)
class MealRepositoryImpl_Test {
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: AppDatabase
    private lateinit var mealDao: MealDao
    private lateinit var repository: MealRepositoryImpl

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().context,
            AppDatabase::class.java
        ).build()
        mealDao = database.mealDao()
        repository = MealRepositoryImpl(mealDao)
    }

    @Test
    fun saveMeal_shouldInsertIntoDb() = runBlocking {
        val meal = Meal(id = "1", foods = listOf(...))
        repository.saveMeal(meal)
        val saved = mealDao.getMealById("1").first()
        assertEquals(saved.id, "1")
    }
}
```

### Presentation Layer (Compose UI Tests) — Hard

```kotlin
@RunWith(AndroidJUnit4::class)
class ConfirmationScreen_Test {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun tappingConfirmButton_callsViewModel() {
        val viewModel = mockk<ConfirmationViewModel>()
        composeTestRule.setContent {
            ConfirmationScreen(viewModel)
        }
        composeTestRule.onNodeWithText("Confirm & Log").performClick()
        verify { viewModel.onAction(ConfirmationAction.OnConfirmAndLogClicked) }
    }
}
```

---

## 10. Build Order & Recommended Parallelization

**Phase 1 (Weeks 1–2):** Get a working app fast
1. Set up Koin DI, Room, Ktor
2. `feature_onboarding` — all three screens
3. `feature_home_dashboard` — empty state, basic UI
4. `feature_manual_entry` — search, quantity picker, log

**Parallel with Phase 1:**
5. `core/presentation/components` & `designsystem` — shared UI pieces

**Phase 2 (Weeks 3–4):** Prove AI integration works
6. `feature_camera` — CameraX integration, capture → preview
7. `feature_ai_analysis` — integrate Gemini Flash API, happy path
8. `feature_meal_confirmation` — reconcile AI result, show editable fields

**Phase 3 (Week 5):** The differentiator
9. `feature_clarification` — confidence-gated chat with AI

**Phase 4 (Week 6):** Memory & history
10. `feature_food_memory` — save templates, matching, relog
11. `feature_history` — list, detail, edit, delete

**Phase 5 (Week 7+):** Polish
12. `feature_settings` — profile, targets, preferences
13. Error states, offline states, loading states, edge cases
14. Testing, documentation, demo video

---

## 11. Key Files Checklists

### Every Feature Must Have:

- [ ] `domain/model/*.kt` — Clean domain models
- [ ] `domain/repository/*.kt` — Repository interface
- [ ] `domain/use_case/*.kt` — All use cases for this feature (minimum 2–3)
- [ ] `data/local/*.kt` — DAO, Entity (if using Room)
- [ ] `data/remote/*.kt` — API interface, DTO (if calling an API)
- [ ] `data/mapper/*.kt` — Entity ↔ Domain, DTO ↔ Domain
- [ ] `data/repository/*.kt` — Repository implementation
- [ ] `presentation/[screen_name]/*State.kt` — Immutable state data class
- [ ] `presentation/[screen_name]/*Action.kt` — Sealed class for all actions
- [ ] `presentation/[screen_name]/*ViewModel.kt` — MVVM + MVI pattern
- [ ] `presentation/[screen_name]/*Screen.kt` — Compose UI
- [ ] `di/*Module.kt` — Koin wiring

### Core Must Have:

- [ ] `common/Resource.kt` — Success/Error/Loading sealed class
- [ ] `common/Constants.kt` — API URLs, timeouts, etc.
- [ ] `network/KtorHttpClient.kt` — Configured Ktor client
- [ ] `database/AppDatabase.kt` — Room database with all DAOs
- [ ] `database/*Dao.kt` — All data access objects
- [ ] `di/KoinModule.kt` — Entry point for all modules
- [ ] `presentation/components/` — Shared Compose components (Button, TextField, etc.)
- [ ] `presentation/designsystem/` — Theme, colors, typography
- [ ] `presentation/navigation/` — Jetpack Nav Compose routes

---

## 12. Naming Conventions

### Files

| Layer | Type | Naming | Example |
|-------|------|--------|---------|
| Domain | Model | `[EntityName].kt` | `User.kt`, `Meal.kt`, `IdentifiedFood.kt` |
| Domain | Repository Interface | `[EntityName]Repository.kt` | `MealRepository.kt` |
| Domain | Use Case | `[VerbPhrase]UseCase.kt` | `LogMealUseCase.kt`, `AnalyzeMealPhotoUseCase.kt` |
| Data | Entity | `[EntityName]Entity.kt` | `UserEntity.kt` |
| Data | DAO | `[EntityName]Dao.kt` | `UserDao.kt` |
| Data | DTO | `[EntityName]Dto.kt` | `UserDto.kt` |
| Data | Mapper | `[EntityName]Mapper.kt` | `UserMapper.kt` |
| Data | Repository Impl | `[EntityName]RepositoryImpl.kt` | `UserRepositoryImpl.kt` |
| Presentation | Screen | `[ScreenName]Screen.kt` | `LoginScreen.kt` |
| Presentation | ViewModel | `[ScreenName]ViewModel.kt` | `LoginViewModel.kt` |
| Presentation | State | `[ScreenName]State.kt` | `LoginState.kt` |
| Presentation | Action | `[ScreenName]Action.kt` | `LoginAction.kt` |

### Classes & Objects

| Type | Naming | Example |
|------|--------|---------|
| Data class | `PascalCase` | `LoginState`, `UserEntity` |
| Sealed class | `PascalCase` | `LoginAction`, `Resource<T>` |
| Interface | `PascalCase` | `UserRepository`, `SessionManager` |
| Implementation | `PascalCase + Impl` | `UserRepositoryImpl` |
| Use Case | `[Verb]UseCase` | `LoginUseCase` |
| Component | `PascalCase` | `PrimaryButton`, `LoadingSpinner` |

### Functions

| Type | Naming | Example |
|------|--------|---------|
| ViewModel Action Handler | `onAction(action: *)` | Always use this name |
| Use Case Invoke | `suspend operator fun invoke(...)` | Allows `useCase(...)` syntax |
| Extension Functions | `[VerbPhrase]` | `toDomain()`, `toEntity()` |
| Private ViewModel Functions | `[verbPhrase]()` | `loadUser()`, `submitForm()` |

### Package Structure

Always: `com.example.thali.feature_[name].[layer]`

Examples:
- `com.example.thali.feature_meal_logging.domain.use_case`
- `com.example.thali.feature_clarification.presentation.chat`
- `com.example.thali.core.network`

---

## 13. Summary: Build Checklist for Any New Screen

When you sit down to build a new screen (e.g., `ClarificationChatScreen`):

1. **Start with Domain (Business Rules):**
   - [ ] What data does this screen need? Define `domain/model/*.kt`
   - [ ] What operations does it perform? Define `domain/repository/*.kt` (interface)
   - [ ] What are the use cases? Define `domain/use_case/*.kt`

2. **Then Data Layer (Persistence & Fetching):**
   - [ ] How is data stored? Define `data/local/*.kt` (DAO, Entity)
   - [ ] How is data fetched? Define `data/remote/*.kt` (if needed)
   - [ ] How is data translated? Define `data/mapper/*.kt`
   - [ ] How does it all orchestrate? Define `data/repository/*.kt` (implementation)

3. **Then Presentation (UI):**
   - [ ] What state does the UI need? Define `presentation/*/State.kt`
   - [ ] What actions can the user take? Define `presentation/*/Action.kt`
   - [ ] How does it orchestrate? Define `presentation/*/ViewModel.kt`
   - [ ] What does it look like? Define `presentation/*/Screen.kt`

4. **Wire It Up:**
   - [ ] Define `di/[Feature]Module.kt` with all Koin bindings

5. **Register Navigation:**
   - [ ] Add a route to `core/presentation/navigation/ThaliNavigation.kt`
   - [ ] Wire up the screen in the NavHost composable

6. **Test (Optional for MVP, but do it):**
   - [ ] Unit test the use cases
   - [ ] Integration test the repository with Room
   - [ ] Compose UI test the screen

---

## 14. Rationale: Why This Architecture for Thali

| Aspect | Choice | Why |
|--------|--------|-----|
| Layering | Clean (Domain → Data ← Presentation) | Keeps business logic testable and decoupled from frameworks |
| Organization | Feature-Driven | Easier to parallelize work, isolate features, extract into modules later |
| State Pattern | MVI (Unidirectional) | Eliminates race conditions, makes UI behavior predictable and testable |
| Dependency Injection | Koin | Lightweight, Kotlin-first, easy to configure per-feature |
| Database | Room | Official, offline-first by default, type-safe SQL queries |
| Networking | Ktor Client | Kotlin-first, coroutine-native, multiplatform-ready for future |
| UI Framework | Jetpack Compose | Modern, declarative, Kotlin-first, rapid development |
| Navigation | Jetpack Nav Compose | Type-safe, official, handles back stack automatically |

---

## 15. Diagram: Complete Data Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ USER INTERACTION (e.g., tap "Log Meal after clarification")                  │
└──────────────────────────────────┬──────────────────────────────────────────┘
                                   │
                    Screen sends Action to ViewModel
                                   │
                                   ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│ PRESENTATION LAYER                                                            │
│ ConfirmationScreen observes State, sends ConfirmationAction.OnConfirmClicked  │
│                  ↓                                                            │
│ ConfirmationViewModel.onAction() receives action                             │
│                  ↓                                                            │
│ Calls confirmAndLogMealUseCase(meal)                                         │
│                  ↓                                                            │
│ Unpacks Resource<Unit> into State fields (isLoading, error, saved)          │
│                  ↓                                                            │
│ State updated → UI recomposes                                               │
└──────────────────────────────────┬──────────────────────────────────────────┘
                                   │
                    UseCase orchestrates business logic
                                   │
                                   ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│ DOMAIN LAYER                                                                  │
│ ConfirmAndLogMealUseCase.invoke(meal): Resource<Unit>                       │
│                  ↓                                                            │
│ Validates meal (ValidateMealUseCase)                                        │
│                  ↓                                                            │
│ Calls mealConfirmationRepository.saveMeal(meal)                             │
│                  ↓                                                            │
│ Returns Resource.Success(Unit) or Resource.Error(message)                   │
└──────────────────────────────────┬──────────────────────────────────────────┘
                                   │
                    Repository persists data
                                   │
                                   ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│ DATA LAYER                                                                    │
│ MealConfirmationRepositoryImpl.saveMeal(meal): Resource<Unit>               │
│                  ↓                                                            │
│ Converts meal: Meal → MealEntity (mapper)                                   │
│                  ↓                                                            │
│ Inserts into Room: mealDao.insert(mealEntity)                              │
│                  ↓                                                            │
│ Returns Resource.Success(Unit) or catches exception → Resource.Error        │
└──────────────────────────────────┬──────────────────────────────────────────┘
                                   │
                    Data persisted to Room
                                   │
                                   ▼
             Meal is saved locally, user sees confirmation
```

---

**This architecture is designed to scale from a portfolio project to a production app without major rewrites.**

