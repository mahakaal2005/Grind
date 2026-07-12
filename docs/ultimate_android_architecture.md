# The Ultimate Android Architecture Guide

Welcome to the definitive guide on modern Android Architecture!
This guide serves as a template and learning resource for building highly scalable,
maintainable, and testable Android applications. It combines the best of
**Feature-Driven Architecture**, **Clean Architecture**, **Use Case-Driven Domain Logic**,
and **MVI (Unidirectional Data Flow) principles within MVVM**.

> **Who is this for?**
> A complete beginner can read this from top to bottom and independently build a production-grade
> Android app. A senior developer can use it as a reference template.

---

## 1. Core Architectural Concepts

Before diving into the code, you must understand the four pillars of this architecture:

1.  **Feature-Driven (Package by Feature):**
    Instead of grouping files by what they *are* (e.g., all ViewModels in one folder),
    we group files by what they *do* (e.g., everything related to "Authentication" in one folder).
    This creates isolated "mini-apps" that are easy to maintain and extract into Gradle
    multi-module projects later.

2.  **Clean Architecture (Data → Domain → Presentation):**
    Inside every feature, the code is separated by its technological responsibility.
    The strict dependency rule is: **Presentation depends on Domain. Data depends on Domain.
    Domain depends on nothing.**
    -   **Domain (The Brain):** Pure business rules. Zero Android dependencies.
    -   **Data (The Worker):** Database, network, and caching logic.
    -   **Presentation (The Face):** Jetpack Compose UI and ViewModels.

3.  **Use Case-Driven Domain Logic:**
    The ViewModel never talks directly to a Repository. Every action the user can take
    is represented by a dedicated `UseCase` class inside `domain/use_case/`. This keeps
    the ViewModel thin, the business logic testable, and the intent of the code crystal clear.

4.  **Modern MVVM + MVI (Unidirectional Data Flow):**
    Structurally, we use the Android `ViewModel` (this is MVVM). Behaviorally, we apply
    Unidirectional Data Flow (this is MVI-style). The UI sends structured `Actions` (Intents)
    to the ViewModel. The ViewModel exposes exactly **ONE immutable `State` object** for the
    UI to observe. Data flows in one direction only: UI → ViewModel → UseCase → Repository → back up.

---

## 2. The Complete Architecture Tree

Every single file and folder is listed below with its exact purpose and reason for existing.
There are no shortcuts and no omissions.

```text
com.yourcompany.app/
│
├── core/                               <-- THE GLOBAL TOOLBOX
│   │                                       WHAT: Contains code shared across multiple features.
│   │                                       WHY: Prevents code duplication. If 2+ features need it, it goes here.
│   │                                       RULE: If only ONE feature uses it, it stays inside that feature.
│   │
│   ├── common/                         <-- GENERIC, APP-WIDE UTILITIES
│   │   │                                   WHAT: Technology-agnostic helpers used everywhere.
│   │   │                                   WHY: Ensures every feature handles loading/error states identically.
│   │   ├── Resource.kt                 <-- Sealed class: Resource.Loading / Resource.Success / Resource.Error
│   │   └── Constants.kt               <-- BASE_URL, TIMEOUT_DURATION, PREFS_KEY_TOKEN, etc.
│   │
│   ├── domain/                         <-- SHARED BUSINESS CONTRACTS
│   │   │                                   WHAT: Interfaces for cross-feature business logic.
│   │   │                                   WHY: Allows features to consume shared behaviour without
│   │   │                                        depending on each other (e.g., feature_events needing auth state).
│   │   └── SessionManager.kt          <-- Interface with: isUserLoggedIn(): Flow<Boolean>, getToken(): String?
│   │
│   ├── data/                           <-- SHARED IMPLEMENTATIONS
│   │   │                                   WHAT: Concrete implementations of core/domain contracts.
│   │   │                                   WHY: The implementation (DataStore, SharedPrefs) lives in data,
│   │   │                                        but the contract lives in domain, keeping the separation clean.
│   │   └── SessionManagerImpl.kt      <-- Implements SessionManager using DataStore
│   │
│   ├── network/                        <-- GLOBAL NETWORK CONFIGURATION
│   │   │                                   WHAT: Retrofit builder, OkHttpClient, and interceptors.
│   │   │                                   WHY: The entire app shares one HTTP connection pool for efficiency.
│   │   └── AuthInterceptor.kt         <-- Reads token from SessionManager and attaches it to every API call
│   │
│   ├── database/                       <-- GLOBAL DATABASE CONFIGURATION
│   │   │                                   WHAT: The single Room AppDatabase class.
│   │   │                                   WHY: Room requires one database instance. All feature DAOs
│   │   │                                        are registered here as abstract properties.
│   │   └── AppDatabase.kt             <-- @Database(entities = [...], version = 1)
│   │
│   ├── presentation/                   <-- GLOBAL UI COMPONENTS
│   │   │                                   WHAT: Shared Compose components used across multiple screens.
│   │   │                                   WHY: Ensures visual consistency. A PrimaryButton looks the
│   │   │                                        same everywhere in the app.
│   │   ├── components/                <-- e.g., PrimaryButton.kt, LoadingSpinner.kt, ErrorScreen.kt
│   │   └── designsystem/              <-- AppTheme.kt, Color.kt, Typography.kt, Shape.kt
│   │
│   └── di/                             <-- GLOBAL DEPENDENCY INJECTION (Koin Modules)
│       │                                   WHAT: Koin modules that provide long-lived, app-wide singletons.
│       │                                   WHY: Retrofit and Room are expensive to create. They must be
│       │                                        created once and reused for the app's entire lifecycle.
│       ├── NetworkModule.kt           <-- Provides: Retrofit, OkHttpClient, AuthInterceptor
│       ├── DatabaseModule.kt          <-- Provides: AppDatabase and all DAOs
│       └── CoreModule.kt              <-- Provides: SessionManager (bound to SessionManagerImpl)
│
│
├── feature_auth/                       <-- FEATURE 1: AUTHENTICATION
│   │                                       WHAT: Everything needed to log in, register, and log out.
│   │                                       WHY: Total isolation. Deleting this folder removes auth completely
│   │                                            without affecting any other feature.
│   │
│   ├── domain/                         <-- THE BRAIN OF AUTH
│   │   │                                   WHAT: The rules of authentication. What does "login" mean?
│   │   │                                        What counts as a valid email? This layer decides.
│   │   │                                   WHY: Because these rules must never be coupled to a specific
│   │   │                                        technology (Retrofit, Room, etc.).
│   │   │
│   │   ├── model/                      <-- CLEAN DOMAIN MODELS
│   │   │   │                               WHAT: Pure Kotlin data classes. No @Entity, no @SerializedName.
│   │   │   │                               WHY: The rest of the app communicates using these clean objects,
│   │   │   │                                    never the messy DTO or Entity versions.
│   │   │   └── User.kt                <-- data class User(val id: String, val name: String, val email: String)
│   │   │
│   │   ├── repository/                 <-- THE CONTRACT (Interface Only)
│   │   │   │                               WHAT: An interface that declares what data operations exist.
│   │   │   │                               WHY: Domain dictates WHAT it needs. It does NOT care HOW it
│   │   │   │                                    gets fulfilled (internet vs local DB). That is the Data layer's job.
│   │   │   └── AuthRepository.kt      <-- interface AuthRepository {
│   │   │                                       suspend fun login(email: String, password: String): Resource<User>
│   │   │                                       suspend fun logout()
│   │   │                                   }
│   │   │
│   │   └── use_case/                   <-- THE ACTIONS (One class = One user action)
│   │       │                               WHAT: Each class represents exactly one thing the user can do.
│   │       │                               WHY: Keeps the ViewModel thin. Business logic (validation, combining
│   │       │                                    data from multiple repos) belongs here, NOT in the ViewModel.
│   │       │                               WHY: Each UseCase is a single class = trivially easy to unit test.
│   │       │                               NAMING RULE: Always named after the action. e.g., GetX, SubmitX, ValidateX.
│   │       ├── LoginUseCase.kt        <-- Validates email/password format, then calls AuthRepository.login()
│   │       ├── LogoutUseCase.kt       <-- Calls AuthRepository.logout() and clears the session
│   │       └── ValidateEmailUseCase.kt<-- Pure logic: checks email format using regex. No repository needed.
│   │
│   ├── data/                           <-- THE WORKER OF AUTH
│   │   │                                   WHAT: Handles the actual technology of logging in.
│   │   │                                   WHY: Hides the messy details (API calls, JSON parsing, token storage)
│   │   │                                        from the rest of the app. Only this layer knows we're using Retrofit.
│   │   │
│   │   ├── local/                      <-- LOCAL STORAGE
│   │   │   │                               WHAT: Anything persisted on the device for auth purposes.
│   │   │   │                               WHY: Storing the auth token locally allows the user to stay
│   │   │   │                                    logged in even after killing the app.
│   │   │   └── AuthPreferences.kt     <-- DataStore wrapper: saveToken(), getToken(), clearToken()
│   │   │
│   │   ├── remote/                     <-- NETWORK SOURCE
│   │   │   │                               WHAT: Retrofit interface and the raw JSON response models (DTOs).
│   │   │   │                               WHY: DTOs match the exact JSON shape from the server. They are
│   │   │   │                                    ugly and server-specific, so they stay strictly in the data layer.
│   │   │   ├── AuthApi.kt             <-- @POST("/login") suspend fun login(@Body request: LoginRequest): Response<UserDto>
│   │   │   └── UserDto.kt             <-- data class UserDto(@SerializedName("user_id") val userId: String, ...)
│   │   │
│   │   ├── mapper/                     <-- THE TRANSLATOR
│   │   │   │                               WHAT: Extension functions that convert DTOs and Entities into
│   │   │   │                                    clean Domain Models (and vice versa for saving).
│   │   │   │                               WHY: The Domain layer refuses to see or import DTOs/Entities.
│   │   │   │                                    This layer bridges the gap. It is the translator at the border.
│   │   │   └── AuthMapper.kt          <-- fun UserDto.toDomain(): User { return User(id = this.userId, ...) }
│   │   │
│   │   └── repository/                 <-- THE IMPLEMENTATION
│   │       │                               WHAT: The concrete implementation of the domain's AuthRepository interface.
│   │       │                               WHY: This is the brain of the data layer. It decides: do I hit the
│   │       │                                    network or read from cache? It fetches, maps, and returns data.
│   │       └── AuthRepositoryImpl.kt  <-- Implements AuthRepository: calls AuthApi, maps UserDto → User,
│   │                                        saves token to AuthPreferences, and returns Resource<User>
│   │
│   ├── presentation/                   <-- THE FACE OF AUTH
│   │   │                                   WHAT: Compose screens, ViewModels, and State definitions.
│   │   │                                   WHY: Separated by screen so each screen's ViewModel, State,
│   │   │                                        Actions, and UI file are always found together.
│   │   │
│   │   └── login/                      <-- LOGIN SCREEN (grouped by screen, not by type)
│   │       │
│   │       ├── LoginState.kt          <-- THE SINGLE SOURCE OF TRUTH FOR THE UI
│   │       │                               WHAT: One data class holding ALL variables the UI needs.
│   │       │                               WHY (IMPORTANT - See Section 4 for full explanation):
│   │       │                                    We unpack Resource<User> into flat fields here.
│   │       │                                    Do NOT store Resource<User> directly in State.
│   │       │                               LOOKS LIKE:
│   │       │                                    data class LoginState(
│   │       │                                        val isLoading: Boolean = false,
│   │       │                                        val user: User? = null,
│   │       │                                        val error: String? = null,
│   │       │                                        val emailInput: String = "",
│   │       │                                        val passwordInput: String = "",
│   │       │                                        val isPasswordVisible: Boolean = false
│   │       │                                    )
│   │       │
│   │       ├── LoginAction.kt         <-- THE USER'S VOCABULARY (Sealed Class)
│   │       │                               WHAT: Every possible thing the user can do on this screen.
│   │       │                               WHY: Instead of the UI calling random ViewModel functions,
│   │       │                                    it sends a structured, typed Action. This is the "Intent"
│   │       │                                    in MVI. It makes the ViewModel's job predictable.
│   │       │                               LOOKS LIKE:
│   │       │                                    sealed class LoginAction {
│   │       │                                        data class OnEmailChanged(val email: String) : LoginAction()
│   │       │                                        data class OnPasswordChanged(val password: String) : LoginAction()
│   │       │                                        object OnLoginClicked : LoginAction()
│   │       │                                        object OnTogglePasswordVisibility : LoginAction()
│   │       │                                    }
│   │       │
│   │       ├── LoginViewModel.kt      <-- THE MIDDLEMAN (MVVM + MVI combined)
│   │       │                               WHAT: Receives Actions, calls UseCases, emits new State.
│   │       │                               WHY: The ViewModel is the ONLY link between the UI and the Domain.
│   │       │                                    It holds the _state MutableStateFlow privately and exposes
│   │       │                                    a read-only state val to the UI.
│   │       │                               LOOKS LIKE:
│   │       │                                    class LoginViewModel(
│   │       │                                        private val loginUseCase: LoginUseCase
│   │       │                                    ) : ViewModel() {
│   │       │
│   │       │                                        private val _state = MutableStateFlow(LoginState())
│   │       │                                        val state = _state.asStateFlow()
│   │       │
│   │       │                                        fun onAction(action: LoginAction) {
│   │       │                                            when (action) {
│   │       │                                                is LoginAction.OnEmailChanged ->
│   │       │                                                    _state.update { it.copy(emailInput = action.email) }
│   │       │                                                is LoginAction.OnLoginClicked -> login()
│   │       │                                            }
│   │       │                                        }
│   │       │
│   │       │                                        private fun login() = viewModelScope.launch {
│   │       │                                            _state.update { it.copy(isLoading = true, error = null) }
│   │       │                                            val result = loginUseCase(state.value.emailInput, state.value.passwordInput)
│   │       │                                            when (result) {
│   │       │                                                is Resource.Success ->
│   │       │                                                    _state.update { it.copy(isLoading = false, user = result.data) }
│   │       │                                                is Resource.Error ->
│   │       │                                                    _state.update { it.copy(isLoading = false, error = result.message) }
│   │       │                                            }
│   │       │                                        }
│   │       │                                    }
│   │       │
│   │       └── LoginScreen.kt         <-- THE VIEW (Compose UI)
│   │                                       WHAT: Pure UI. Observes State and sends Actions. Nothing else.
│   │                                       WHY: The Screen knows nothing about UseCases or Repositories.
│   │                                            It only knows how to draw and how to forward user actions.
│   │                                       LOOKS LIKE:
│   │                                            @Composable
│   │                                            fun LoginScreen(viewModel: LoginViewModel = koinViewModel()) {
│   │                                                val state by viewModel.state.collectAsStateWithLifecycle()
│   │                                                if (state.isLoading) LoadingSpinner()
│   │                                                TextField(value = state.emailInput,
│   │                                                          onValueChange = { viewModel.onAction(LoginAction.OnEmailChanged(it)) })
│   │                                                Button(onClick = { viewModel.onAction(LoginAction.OnLoginClicked) }) {
│   │                                                    Text("Login")
│   │                                                }
│   │                                            }
│   │
│   └── di/                             <-- THE WIRING FOR AUTH
│       │                                   WHAT: A Koin module that connects all of the above.
│       │                                   WHY: The ViewModel does not create its own UseCase.
│       │                                        The UseCase does not create its own Repository.
│       │                                        Koin is the matchmaker that provides the right
│       │                                        implementation to the right class automatically.
│       └── AuthModule.kt              <-- val authModule = module {
│                                               single<AuthRepository> { AuthRepositoryImpl(get(), get()) }
│                                               factory { LoginUseCase(get()) }
│                                               factory { LogoutUseCase(get()) }
│                                               viewModel { LoginViewModel(get()) }
│                                           }
│
│
└── feature_items/                      <-- FEATURE 2 (Follows the IDENTICAL structure above)
    │                                       Every feature looks the same. No exceptions.
    ├── domain/
    │   ├── model/                      <-- Item.kt
    │   ├── repository/                 <-- ItemRepository.kt (interface)
    │   └── use_case/                   <-- GetItemsUseCase.kt, SubmitItemUseCase.kt, DeleteItemUseCase.kt
    │
    ├── data/
    │   ├── local/                      <-- ItemDao.kt, ItemEntity.kt
    │   ├── remote/                     <-- ItemApi.kt, ItemDto.kt
    │   ├── mapper/                     <-- ItemMapper.kt
    │   └── repository/                 <-- ItemRepositoryImpl.kt (offline-first caching lives here)
    │
    ├── presentation/
    │   ├── item_list/                  <-- ItemListScreen, ItemListViewModel, ItemListState, ItemListAction
    │   └── item_detail/                <-- ItemDetailScreen, ItemDetailViewModel, ItemDetailState, ItemDetailAction
    │
    └── di/
        └── ItemModule.kt
```

---

## 3. Deep Dive: Use Cases (The Most Important Section)

Use Cases are the heart of this architecture and the single biggest thing that separates
a beginner's codebase from a professional one.

### What is a Use Case?
A Use Case is a Kotlin class that represents **exactly one action the user can perform**.
It sits in `domain/use_case/` and is the bridge between the ViewModel and the Repository.

### Why not just call the Repository from the ViewModel directly?
Because ViewModels get fat. Consider a "Submit Post" feature:
1.  Validate the text is not empty.
2.  Check if the user is logged in.
3.  Compress the attached image.
4.  Call the API to upload.

If all of that lives in the ViewModel, the ViewModel becomes a monster that is impossible
to test or reuse. If each step is a separate UseCase, each one can be tested in isolation.

### What does a Use Case look like?

```kotlin
// domain/use_case/LoginUseCase.kt
class LoginUseCase(
    private val repository: AuthRepository,          // Injected via Koin
    private val validateEmail: ValidateEmailUseCase  // UseCases can call other UseCases!
) {
    // The invoke operator lets you call it like a function: loginUseCase(email, password)
    suspend operator fun invoke(email: String, password: String): Resource<User> {
        // Business logic lives HERE, not in the ViewModel
        if (!validateEmail(email)) {
            return Resource.Error("Invalid email format")
        }
        if (password.length < 6) {
            return Resource.Error("Password must be at least 6 characters")
        }
        // Delegate the actual data fetching to the repository
        return repository.login(email, password)
    }
}
```

### Use Case Naming Rules
| Prefix     | When to use                      | Example                    |
|------------|----------------------------------|----------------------------|
| `Get`      | Fetching / reading data          | `GetEventsUseCase`         |
| `Submit`   | Sending data to the server       | `SubmitCommentUseCase`     |
| `Validate` | Pure local logic, no network     | `ValidateEmailUseCase`     |
| `Delete`   | Removing data                    | `DeleteHabitUseCase`       |
| `Observe`  | Returning a Flow (live updates)  | `ObserveUserSessionUseCase`|

---

## 4. The Critical Decision: Resource.kt vs MVI State

This is the single most important design decision in the entire architecture, and it
affects every screen you build. You must be explicit about this.

### The Problem
Your `LoginUseCase` returns a `Resource<User>` (Success/Error/Loading).
Your `LoginState` is what the UI observes. **Where do these two meet?**

**Option A (Wrong):** Store `Resource<User>` directly inside the State.
```kotlin
// BAD — Do NOT do this
data class LoginState(
    val userResult: Resource<User>? = null // ❌ The UI now has to unwrap a sealed class inside a data class
)
```
Why is this bad? Because the UI then has to write `when (state.userResult) { is Resource.Loading -> ... }`.
This leaks data-layer concerns into the UI, and you still have no clean place to store
transient UI fields like `emailInput` or `isPasswordVisible`.

**Option B (Correct):** Unpack `Resource` into flat, explicit fields inside the State.
```kotlin
// GOOD — Always do this
data class LoginState(
    val isLoading: Boolean = false,       // ✅ From Resource.Loading
    val user: User? = null,               // ✅ From Resource.Success
    val error: String? = null,            // ✅ From Resource.Error
    // Plus all the UI-specific fields:
    val emailInput: String = "",          // ✅ Pure UI state, not from the repository at all
    val passwordInput: String = "",
    val isPasswordVisible: Boolean = false
)
```

### How the ViewModel unpacks Resource into State

```kotlin
private fun login() = viewModelScope.launch {
    // Set loading state BEFORE calling the use case
    _state.update { it.copy(isLoading = true, error = null) }

    val result = loginUseCase(
        email = state.value.emailInput,
        password = state.value.passwordInput
    )

    // Unpack the Resource here in the ViewModel. The UI never sees Resource.
    when (result) {
        is Resource.Success -> {
            _state.update { it.copy(isLoading = false, user = result.data) }
        }
        is Resource.Error -> {
            _state.update { it.copy(isLoading = false, error = result.message) }
        }
        is Resource.Loading -> {
            // Usually handled before the use case call, but can be emitted mid-stream for Flow-based use cases
            _state.update { it.copy(isLoading = true) }
        }
    }
}
```

### The Firm Rule
> **`Resource<T>` is used inside the Domain and Data layers as a transport mechanism.
> By the time data reaches the ViewModel, it must be unpacked into flat, named fields
> inside the State data class. The UI layer never imports or interacts with `Resource` directly.**

---

## 5. Advanced Concepts

### A. Cross-Feature Communication (Shared Domain Contracts)

**The Problem:**
`feature_items` needs to know if the user is logged in before letting them submit.
But features are strictly forbidden from importing each other.

**The Solution:**
Elevate the shared contract to `core/domain/SessionManager.kt` (an interface).
The implementation (`SessionManagerImpl`) lives in `core/data/`.
The binding is wired in `core/di/CoreModule.kt`.

```kotlin
// core/domain/SessionManager.kt
interface SessionManager {
    fun isUserLoggedIn(): Flow<Boolean>
    fun getToken(): String?
}

// feature_items/domain/use_case/SubmitItemUseCase.kt
class SubmitItemUseCase(
    private val itemRepository: ItemRepository,
    private val sessionManager: SessionManager  // Injected from core! No feature_auth import needed.
) {
    suspend operator fun invoke(item: Item): Resource<Unit> {
        if (!sessionManager.isUserLoggedIn().first()) {
            return Resource.Error("You must be logged in to submit.")
        }
        return itemRepository.submit(item)
    }
}
```

### B. Offline-First Caching (Single Source of Truth)

**The Rule:** The Local Database (Room) is always the Single Source of Truth.
The UI only ever reads from Room. The Repository decides when to refresh from the network.
The Caching logic lives 100% inside `data/repository/`. Domain and UI never know it exists.

```kotlin
// feature_items/data/repository/ItemRepositoryImpl.kt
override fun getItems(): Flow<Resource<List<Item>>> = flow {
    // Step 1: Emit loading state with old cached data immediately
    emit(Resource.Loading())
    val cachedItems = dao.getAll().map { it.toDomain() }
    emit(Resource.Loading(data = cachedItems))  // User sees old data instantly

    try {
        // Step 2: Fetch fresh data from the network
        val remoteItems = api.fetchItems()

        // Step 3: Save fresh data into the local cache (Room)
        dao.deleteAll()
        dao.insertAll(remoteItems.map { it.toEntity() })

    } catch (e: HttpException) {
        // Step 4: If offline, show error but keep displaying cached data
        emit(Resource.Error("No internet connection", data = cachedItems))
        return@flow
    }

    // Step 5: Read the freshly saved data from Room and emit as Success
    val updatedItems = dao.getAll().map { it.toDomain() }
    emit(Resource.Success(updatedItems))
}
```

### C. Architecture is MVVM, Pattern is MVI

A common point of confusion:

| Question                     | Answer                                                      |
|------------------------------|-------------------------------------------------------------|
| What is the architecture?    | **MVVM** — we use Android's ViewModel to survive lifecycle  |
| What is the state pattern?   | **MVI / UDF** — single State, sealed Action, one-way flow   |
| Are they in conflict?        | No. MVI is applied *inside* MVVM. They are complementary.  |

**How to answer in an interview:**
*"Structurally we use MVVM using Android Architecture Components. For state management
we apply Unidirectional Data Flow following MVI principles — the UI only emits Actions,
and the ViewModel only exposes a single immutable State. This eliminates race conditions
and makes the UI completely predictable."*

---

## 6. Dependency Flow (The Golden Rule Visualized)

```
┌─────────────────────────────────────────────────────────┐
│                     PRESENTATION                        │
│         (Screen → ViewModel → Action/State)             │
│              Depends on: DOMAIN only                    │
└──────────────────────┬──────────────────────────────────┘
                       │ calls UseCases
┌──────────────────────▼──────────────────────────────────┐
│                       DOMAIN                            │
│        (UseCase → Repository Interface → Model)         │
│              Depends on: NOTHING                        │
└──────────────────────┬──────────────────────────────────┘
                       │ implemented by
┌──────────────────────▼──────────────────────────────────┐
│                        DATA                             │
│     (RepositoryImpl → Remote API + Local DB + Mapper)   │
│              Depends on: DOMAIN only                    │
└─────────────────────────────────────────────────────────┘
```

**Arrows only go inward toward Domain. Domain never imports Data or Presentation.**

---

## 7. Summary Checklist: Building Any New Feature From Scratch

Follow this checklist every single time. Do not skip steps.

### Step 1 — Create the folder structure
- [ ] Create `feature_[name]/` with subfolders: `domain/`, `data/`, `presentation/`, `di/`
- [ ] Inside `domain/`: create `model/`, `repository/`, `use_case/`
- [ ] Inside `data/`: create `local/`, `remote/`, `mapper/`, `repository/`
- [ ] Inside `presentation/`: create a subfolder per screen (e.g., `item_list/`, `item_detail/`)

### Step 2 — Domain layer first (Start here. Always.)
- [ ] Write the Domain Model (clean data class, no annotations)
- [ ] Write the Repository Interface (define what data operations you need)
- [ ] Write each Use Case (one class per user action)

### Step 3 — Data layer second
- [ ] Write the Remote API interface (Retrofit) and DTO
- [ ] Write the Local DAO interface (Room) and Entity
- [ ] Write the Mapper functions (DTO → Domain, Entity → Domain)
- [ ] Write the Repository Implementation (caching logic goes here)

### Step 4 — Presentation layer third
- [ ] Create `State.kt` (flat data class — unpack Resource here, not in the UI)
- [ ] Create `Action.kt` (sealed class — one entry per user interaction)
- [ ] Create `ViewModel.kt` (receives Actions, calls UseCases, updates State)
- [ ] Create `Screen.kt` (observes State, sends Actions — nothing else)

### Step 5 — Wire it up with Koin
- [ ] Create `di/FeatureModule.kt`
- [ ] Provide: `single<Repository> { RepositoryImpl(get(), get()) }`
- [ ] Provide: `factory { SomeUseCase(get()) }` for each Use Case
- [ ] Provide: `viewModel { SomeViewModel(get(), get()) }` for each ViewModel
- [ ] Register the module in your `Application` class `startKoin { modules(...) }`
