# Phase 5: Data Models & Persistence Design

## 1. Overview
This design covers the Domain and Data layer implementations for Phase 5 of the MVP. It defines how we model, store, and retrieve Meals, Food Items, and User Goals using Room (for relational data) and DataStore (for configuration). It also defines the architecture for uploading images (Cloudinary) and resolving exact food macros via AI web searches.

## 2. Architecture & Modules
To follow Feature-Driven Clean Architecture, the data layer is split into specialized modules:
*   **`feature_meals`**: Handles all local logging of meals and user food memory via Room Database.
*   **`feature_goals`**: Handles the user's onboarding profile and calculates their macro targets via DataStore Preferences.
*   **`core/media`**: Handles uploading images to Cloudinary (used for meals and food items).
*   **`core/ai`** (or within `feature_meals`): Handles fetching exact macros and images from the web via AI when a user types a specific branded item (e.g., "Pintola high protein peanut butter").

## 3. Domain Models

### In `feature_meals/domain/model/`
```kotlin
data class Meal(
    val id: String,
    val mealType: MealType,
    val timestamp: Long,
    val imageUrl: String?,
    val items: List<FoodItem>
)

enum class MealType { BREAKFAST, LUNCH, DINNER, SNACK }

data class FoodItem(
    val id: String,
    val name: String,
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val fiber: Float,
    val imageUrl: String?, // Fetched from web or uploaded by user
    val confidenceScore: Float // Used to flag low-confidence AI predictions for the UI
)

data class SavedFood(
    val id: String,
    val name: String,
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val fiber: Float,
    val imageUrl: String? // Saved along with the memory
)
```

### In `feature_goals/domain/model/`
```kotlin
enum class GoalType { LOSE_WEIGHT, MAINTAIN, BUILD_MUSCLE }
enum class ActivityLevel { SEDENTARY, LIGHT, MODERATE, VERY_ACTIVE }

data class UserProfile(
    val weightKg: Float,
    val heightCm: Float,
    val age: Int,
    val gender: String,
    val goalType: GoalType,
    val activityLevel: ActivityLevel
)

data class DailyTarget(
    val targetCalories: Int,
    val targetProtein: Float,
    val targetCarbs: Float,
    val targetFat: Float
)
```

## 4. Data Layer (Room)
The local database handles the one-to-many relationship between a Meal and its FoodItems to ensure historical immutability.

### Entities
*   **`MealEntity`**: Stores `id` (PK), `mealType`, `timestamp`, `imageUrl`.
*   **`FoodItemEntity`**: Stores `id` (PK), `mealId` (Foreign Key -> `MealEntity`), `name`, macros, `imageUrl`, and `confidenceScore`.
*   **`SavedFoodEntity`**: Stores `id` (PK), `name`, macros, and `imageUrl`. This is entirely separate from `FoodItemEntity` to act as the user's templates.
*   **`MealWithItems`**: A POJO data class containing `@Embedded val meal: MealEntity` and `@Relation(parentColumn = "id", entityColumn = "mealId") val items: List<FoodItemEntity>`.

## 5. Data Layer (DataStore, Cloudinary, AI)
*   **Goals Storage**: `feature_goals/data/local/GoalPreferences.kt` will store the `UserProfile`. A UseCase (e.g., `CalculateDailyTargetUseCase`) will compute the `DailyTarget` dynamically based on the profile, or the Target can be cached alongside the profile.
*   **Image Upload**: `ImageRepository` will handle the HTTP POST request to the Cloudinary API.
*   **AI Food Resolver**: `AiFoodRepository` will handle making requests to the AI to fetch exact macros and web images based on specific text prompts.

## 6. Repository Contracts (Domain Interfaces)

### `MealRepository`
```kotlin
interface MealRepository {
    fun getMealsForDate(startOfDay: Long, endOfDay: Long): Flow<List<Meal>>
    suspend fun insertMeal(meal: Meal)
    suspend fun deleteMeal(mealId: String)
    fun searchSavedFoods(query: String): Flow<List<SavedFood>>
    suspend fun saveFoodToMemory(food: SavedFood)
}
```

### `GoalRepository`
```kotlin
interface GoalRepository {
    fun getUserProfile(): Flow<UserProfile?>
    suspend fun saveUserProfile(profile: UserProfile)
    fun getDailyTarget(): Flow<DailyTarget?> // Computed from UserProfile
}
```

### `ImageRepository`
```kotlin
interface ImageRepository {
    suspend fun uploadImage(imageBytes: ByteArray): Resource<String>
}
```

### `AiFoodRepository`
```kotlin
interface AiFoodRepository {
    suspend fun fetchExactFoodDetails(description: String): Resource<FoodItem>
}
```

## 7. Next Steps (Implementation Plan)
1.  Implement `feature_goals` models, DataStore, and Repository (including the math logic for `DailyTarget`).
2.  Implement `feature_meals` Domain models.
3.  Implement `feature_meals` Room entities, DAOs, and Database config.
4.  Implement `MealRepositoryImpl` (mapping entities to domain models).
5.  Implement `ImageRepository` for Cloudinary and `AiFoodRepository` stubs.
6.  Wire everything together with Koin DI.
