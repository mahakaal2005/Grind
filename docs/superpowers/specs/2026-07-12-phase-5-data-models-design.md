# Phase 5: Data Models & Persistence Design

## 1. Overview
This design covers the Domain and Data layer implementations for Phase 5 of the MVP. It defines how we model, store, and retrieve Meals, Food Items, and User Goals using Room (for relational data) and DataStore (for configuration). It also defines the architecture for uploading images to Cloudinary.

## 2. Architecture & Modules
To follow Feature-Driven Clean Architecture, the data layer is split into specialized modules:
*   **`feature_meals`**: Handles all local logging of meals and user food memory via Room Database.
*   **`feature_goals`**: Handles the user's daily macro targets via DataStore Preferences.
*   **`core/media`** (or within `feature_meals`'s data layer if strictly isolated): Handles uploading images to Cloudinary.

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
    val confidenceScore: Float // Used to flag low-confidence AI predictions for the UI
)

data class SavedFood(
    val id: String,
    val name: String,
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val fiber: Float
)
```

### In `feature_goals/domain/model/`
```kotlin
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
*   **`FoodItemEntity`**: Stores `id` (PK), `mealId` (Foreign Key -> `MealEntity`), `name`, macros, and `confidenceScore`.
*   **`SavedFoodEntity`**: Stores `id` (PK), `name`, and macros. This is entirely separate from `FoodItemEntity` to act as the user's templates.
*   **`MealWithItems`**: A POJO data class containing `@Embedded val meal: MealEntity` and `@Relation(parentColumn = "id", entityColumn = "mealId") val items: List<FoodItemEntity>`.

## 5. Data Layer (DataStore & Cloudinary)
*   **Goals Storage**: `feature_goals/data/local/GoalPreferences.kt` will use AndroidX DataStore to store the user's `DailyTarget` globally.
*   **Image Upload**: `feature_meals/data/remote/ImageUploadService.kt` will handle the HTTP POST request to the Cloudinary API to upload an image and return the hosted URL.

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
    fun getDailyTarget(): Flow<DailyTarget?>
    suspend fun setDailyTarget(target: DailyTarget)
}
```

### `ImageRepository`
```kotlin
interface ImageRepository {
    suspend fun uploadImage(imageBytes: ByteArray): Resource<String>
}
```

## 7. Next Steps (Implementation Plan)
1.  Implement `feature_goals` models, DataStore, and Repository.
2.  Implement `feature_meals` Domain models.
3.  Implement `feature_meals` Room entities, DAOs, and Database config.
4.  Implement `MealRepositoryImpl` (mapping entities to domain models).
5.  Implement `ImageRepository` for Cloudinary.
6.  Wire everything together with Koin DI.
