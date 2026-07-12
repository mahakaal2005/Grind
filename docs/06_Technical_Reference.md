# Technical Reference Document (TRD) — Thali
### Implementation Details, Dependencies, and Technical Decisions

---

## 1. Introduction

This document provides the concrete technical details for building Thali. It covers:
- **Exact dependencies and versions**
- **Configuration for each major tool (Room, Ktor, Koin, Coil)**
- **AI API integration strategy** (with swappable abstraction)
- **Database schema and design**
- **Error handling and retry logic**
- **Testing infrastructure**
- **Performance and optimization considerations**
- **Offline-first implementation specifics**

---

## 2. Dependencies & Gradle Setup

### Core Build Gradle (app/build.gradle.kts)

```kotlin
plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")  // For Room code generation
    kotlin("plugin.serialization")  // For Ktor serialization
}

android {
    namespace = "com.example.thali"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.thali"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
}

dependencies {
    // === Core Android & Jetpack ===
    implementation("androidx.core:core:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // === Jetpack Compose ===
    val composeBom = platform("androidx.compose:compose-bom:2024.01.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.foundation:foundation")

    // === Camera (CameraX) ===
    implementation("androidx.camera:camera-core:1.3.1")
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")
    implementation("androidx.camera:camera-extensions:1.3.1")

    // === Room Database ===
    implementation("androidx.room:room-runtime:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    testImplementation("androidx.room:room-testing:2.6.1")

    // === Networking (Ktor Client) ===
    implementation("io.ktor:ktor-client-core:2.3.7")
    implementation("io.ktor:ktor-client-android:2.3.7")
    implementation("io.ktor:ktor-client-serialization:2.3.7")
    implementation("io.ktor:ktor-client-logging:2.3.7")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.7")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")

    // === Data Storage (DataStore for preferences) ===
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // === Dependency Injection (Koin) ===
    implementation("io.insert-koin:koin-core:3.5.0")
    implementation("io.insert-koin:koin-android:3.5.0")
    implementation("io.insert-koin:koin-androidx-compose:3.5.0")

    // === Image Loading & Caching (Coil) ===
    implementation("io.coil-kt:coil-compose:2.5.0")

    // === Coroutines ===
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // === Serialization (for API responses) ===
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

    // === Jetpack Navigation (Compose) ===
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // === Testing ===
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockk:mockk:1.13.8")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.6.4")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.6.4")

    // === Permissions (optional, but recommended for CameraX) ===
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")
}
```

### Gradle Version Catalog (gradle/libs.versions.toml)

```toml
[versions]
android-gradle = "8.2.0"
kotlin = "1.9.21"
compose-bom = "2024.01.00"
androidx-lifecycle = "2.7.0"
room = "2.6.1"
ktor = "2.3.7"
koin = "3.5.0"
coil = "2.5.0"
coroutines = "1.7.3"
serialization = "1.6.2"

[libraries]
androidx-lifecycle-runtime = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "androidx-lifecycle" }
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
ktor-client-core = { group = "io.ktor", name = "ktor-client-core", version.ref = "ktor" }
koin-core = { group = "io.insert-koin", name = "koin-core", version.ref = "koin" }
coil-compose = { group = "io.coil-kt", name = "coil-compose", version.ref = "coil" }

[plugins]
android-app = { id = "com.android.application", version.ref = "android-gradle" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
```

---

## 3. Room Database Schema & Configuration

### Core AppDatabase

```kotlin
// core/database/AppDatabase.kt
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        UserProfileEntity::class,
        MealEntity::class,
        FoodEntity::class,
        SavedMealEntity::class
    ],
    version = 1,
    exportSchema = true  // For migrations in the future
)
@TypeConverters(LocalDateTimeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun mealDao(): MealDao
    abstract fun foodDao(): FoodDao
    abstract fun savedMealDao(): SavedMealDao

    companion object {
        const val DATABASE_NAME = "thali.db"
    }
}
```

### Type Converters

```kotlin
// core/database/LocalDateTimeConverters.kt
import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class LocalDateTimeConverters {
    @TypeConverter
    fun fromLocalDateTime(value: LocalDateTime?): Long? =
        value?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()

    @TypeConverter
    fun toLocalDateTime(value: Long?): LocalDateTime? =
        value?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDateTime() }

    @TypeConverter
    fun fromLocalDate(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let { LocalDate.parse(it) }
}
```

### User Profile Entity

```kotlin
// feature_onboarding/data/local/UserProfileEntity.kt
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: String = "user_1",  // Single user for V1
    val age: Int,
    val weightKg: Float,
    val heightCm: Float,
    val sex: String,  // "male" or "female"
    val activityLevel: String,  // "sedentary", "light", "moderate", "active", "very_active"
    val goal: String,  // "lose", "maintain", "gain"
    val dietaryPreference: String,  // "veg", "non_veg", "eggetarian", "vegan"
    val dailyCalorieTarget: Int,
    val dailyProteinGramTarget: Float,
    val dailyCarbsGramTarget: Float,
    val dailyFatGramTarget: Float,
    val dailyFiberGramTarget: Float,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
```

### Meal Entity

```kotlin
// feature_meal_confirmation/data/local/MealEntity.kt
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import java.time.LocalDateTime

@Entity(
    tableName = "meals",
    indices = [Index("date", "userId")]  // Query by date and user
)
data class MealEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String = "user_1",  // FK to UserProfile
    val date: LocalDate,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val totalCalories: Int,
    val totalProteinGrams: Float,
    val totalCarbsGrams: Float,
    val totalFatGrams: Float,
    val totalFiberGrams: Float,
    val notes: String = "",
    val photoPath: String? = null,  // Path to captured meal photo on device
    val aiConfidenceScore: Float? = null,  // 0.0 to 1.0, null if manual entry
    val foodMemoryId: String? = null,  // FK to SavedMeal if this was auto-suggested
    val foodItems: List<FoodItemSnapshot> = emptyList()  // Serialized JSON or separate table
)

@Entity(tableName = "food_items", foreignKeys = [
    ForeignKey(entity = MealEntity::class, parentColumns = ["id"], childColumns = ["mealId"], onDelete = ForeignKey.CASCADE)
])
data class FoodItemEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val mealId: String,  // FK
    val foodId: String? = null,  // FK to Food table (if from bundled DB)
    val name: String,
    val quantity: Float,
    val unit: String,  // "g", "ml", "piece", "cup", etc.
    val calories: Int,
    val proteinGrams: Float,
    val carbsGrams: Float,
    val fatGrams: Float,
    val fiberGrams: Float
)
```

### Food Database Entity (Bundled)

```kotlin
// feature_manual_entry/data/local/FoodEntity.kt
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "foods",
    indices = [Index("name")]
)
data class FoodEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val category: String,  // "grain", "legume", "vegetable", "fruit", "protein", "oil", etc.
    val caloriesPer100g: Int,
    val proteinPer100g: Float,
    val carbsPer100g: Float,
    val fatPer100g: Float,
    val fiberPer100g: Float,
    val regionKeywords: String = "",  // "north_indian", "south_indian" for relevance
    val source: String = "bundled"  // "bundled" or "user_added"
)
```

### Saved Meal (Food Memory) Entity

```kotlin
// feature_food_memory/data/local/SavedMealEntity.kt
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "saved_meals")
data class SavedMealEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String = "user_1",
    val name: String,  // "My usual dal-roti"
    val description: String = "",
    val totalCalories: Int,
    val totalProteinGrams: Float,
    val totalCarbsGrams: Float,
    val totalFatGrams: Float,
    val totalFiberGrams: Float,
    val thumbnailPath: String? = null,  // Photo of this meal
    val foodItems: List<FoodItemSnapshot> = emptyList(),  // Serialized
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val lastUsedAt: LocalDateTime = LocalDateTime.now(),
    val usageCount: Int = 0,
    val matchingThreshold: Float = 0.75f  // How similar a new photo must be to suggest this
)

// Serializable snapshot of a food item (for saving inside SavedMealEntity.foodItems)
@Serializable
data class FoodItemSnapshot(
    val name: String,
    val quantity: Float,
    val unit: String,
    val calories: Int,
    val proteinGrams: Float,
    val carbsGrams: Float,
    val fatGrams: Float,
    val fiberGrams: Float
)
```

### Database Initialization Module

```kotlin
// core/di/DatabaseModule.kt
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()  // For development; use proper migrations for production
            .build()
    }

    single { get<AppDatabase>().userProfileDao() }
    single { get<AppDatabase>().mealDao() }
    single { get<AppDatabase>().foodDao() }
    single { get<AppDatabase>().savedMealDao() }
}
```

---

## 4. Ktor Client Configuration

### Network Module

```kotlin
// core/network/KtorHttpClient.kt
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

fun createHttpClient(): HttpClient {
    return HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }

        install(Logging) {
            level = LogLevel.BODY  // Log full requests/responses; set to NONE for production
            logger = object : Logger {
                override fun log(message: String) {
                    Log.d("Ktor", message)
                }
            }
        }

        // Timeout configuration
        install(HttpTimeout) {
            requestTimeoutMillis = 30000  // 30 seconds
            connectTimeoutMillis = 15000  // 15 seconds
            socketTimeoutMillis = 15000   // 15 seconds
        }

        // User-Agent
        install(UserAgent) {
            agent = "Thali/1.0.0 (Android)"
        }
    }
}

// Network Module
val networkModule = module {
    single { createHttpClient() }
}
```

### API Client Interface (Swappable AI)

```kotlin
// feature_ai_analysis/data/remote/AiApiClient.kt
interface AiApiClient {
    suspend fun analyzeMealPhoto(imageBase64: String): Result<AiResponse>
    suspend fun generateClarificationQuestions(
        identifiedFoods: List<IdentifiedFood>,
        userContext: UserContext
    ): Result<List<ClarificationQuestion>>
    suspend fun reconcileWithAnswers(
        identifiedFoods: List<IdentifiedFood>,
        userAnswers: Map<String, String>
    ): Result<ReconciliationResult>
}

// Gemini Flash Implementation
class GeminiFlashClient(
    private val httpClient: HttpClient,
    private val apiKey: String
) : AiApiClient {

    companion object {
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
        private const val MODEL = "gemini-1.5-flash"
    }

    override suspend fun analyzeMealPhoto(imageBase64: String): Result<AiResponse> = try {
        val request = AnalysisRequest(imageBase64)
        val response = httpClient.post("$BASE_URL/$MODEL:generateContent?key=$apiKey") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        when (response.status) {
            HttpStatusCode.OK -> {
                val body = response.body<GeminiResponse>()
                Result.success(mapGeminiResponse(body))
            }
            HttpStatusCode.TooManyRequests -> {
                Result.failure(RateLimitException("API rate limited, retry after 60 seconds"))
            }
            else -> {
                Result.failure(ApiException("HTTP ${response.status}"))
            }
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun generateClarificationQuestions(
        identifiedFoods: List<IdentifiedFood>,
        userContext: UserContext
    ): Result<List<ClarificationQuestion>> = try {
        // Craft a structured prompt for Gemini
        val prompt = buildClarificationPrompt(identifiedFoods, userContext)
        val request = AnalysisRequest(prompt)
        // Call Gemini, parse response, return questions
        // Implementation similar to above
        Result.success(emptyList())  // Placeholder
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun reconcileWithAnswers(
        identifiedFoods: List<IdentifiedFood>,
        userAnswers: Map<String, String>
    ): Result<ReconciliationResult> = try {
        // Use Gemini to refine quantities and macros based on answers
        Result.success(ReconciliationResult(emptyList(), 0))  // Placeholder
    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun mapGeminiResponse(response: GeminiResponse): AiResponse {
        // Parse Gemini's response and extract identified foods with confidence scores
        return AiResponse(foods = emptyList())  // Placeholder
    }

    private fun buildClarificationPrompt(
        identifiedFoods: List<IdentifiedFood>,
        userContext: UserContext
    ): String {
        // Build a JSON prompt for Gemini asking for clarification
        return ""
    }
}

// Koin binding
val aiModule = module {
    single<AiApiClient> {
        GeminiFlashClient(
            httpClient = get(),
            apiKey = BuildConfig.GEMINI_API_KEY  // Set in BuildConfig or BuildType
        )
    }
}
```

---

## 5. Error Handling & Retry Logic

### Resource Sealed Class

```kotlin
// core/common/Resource.kt
sealed class Resource<out T> {
    data class Success<T>(val data: T) : Resource<T>()
    data class Error(val message: String, val exception: Throwable? = null, val data: Any? = null) : Resource<Nothing>()
    data class Loading<T>(val data: T? = null) : Resource<T>()

    val isLoading: Boolean
        get() = this is Loading

    val isSuccess: Boolean
        get() = this is Success

    val isError: Boolean
        get() = this is Error
}

// Utility functions
inline fun <T, R> Resource<T>.map(transform: (T) -> R): Resource<R> = when (this) {
    is Resource.Success -> Resource.Success(transform(data))
    is Resource.Error -> Resource.Error(message, exception, data)
    is Resource.Loading -> Resource.Loading()
}

suspend inline fun <T> runResourceCatchingIO(crossinline block: suspend () -> T): Resource<T> = try {
    Resource.Success(withContext(Dispatchers.IO) { block() })
} catch (e: CancellationException) {
    throw e  // Don't catch cancellation
} catch (e: HttpRequestTimeoutException) {
    Resource.Error("Request timeout. Check your internet connection.", e)
} catch (e: ConnectException) {
    Resource.Error("Network error. Are you offline?", e)
} catch (e: SerializationException) {
    Resource.Error("Server response format error", e)
} catch (e: Exception) {
    Resource.Error(e.message ?: "Unknown error", e)
}
```

### Retry Logic with Exponential Backoff

```kotlin
// core/network/RetryPolicy.kt
suspend inline fun <T> retryWithBackoff(
    maxAttempts: Int = 3,
    initialDelayMs: Long = 100,
    maxDelayMs: Long = 3000,
    backoffMultiplier: Float = 2f,
    block: suspend () -> Resource<T>
): Resource<T> {
    var currentDelayMs = initialDelayMs

    for (attempt in 1..maxAttempts) {
        val result = block()
        if (result is Resource.Success) return result

        if (attempt == maxAttempts) return result

        // Only retry on specific errors
        if (result is Resource.Error && result.exception !is SerializationException) {
            delay(currentDelayMs)
            currentDelayMs = (currentDelayMs * backoffMultiplier).toLong().coerceAtMost(maxDelayMs)
        } else {
            return result
        }
    }

    return Resource.Error("Max retries exceeded")
}
```

### Usage in Repository

```kotlin
// feature_ai_analysis/data/repository/AiRepositoryImpl.kt
override suspend fun analyzeMealPhoto(photoPath: String): Resource<AiResponse> =
    retryWithBackoff(maxAttempts = 2) {
        val photoBase64 = convertPhotoToBase64(photoPath)
        runResourceCatchingIO {
            aiApiClient.analyzeMealPhoto(photoBase64).getOrThrow()
        }
    }
```

---

## 6. AI API Integration: Strategy for Swappability

### Abstract Repository Pattern

```kotlin
// feature_ai_analysis/domain/repository/AiRepository.kt
interface AiRepository {
    suspend fun analyzeMealPhoto(photoPath: String): Resource<AiResponse>
    suspend fun generateClarificationQuestions(context: ClarificationContext): Resource<List<ClarificationQuestion>>
    suspend fun reconcileMealWithAnswers(data: ReconciliationData): Resource<ReconciliationResult>
}

// Implementations can be swapped at runtime
class GeminiFlashAiRepository(private val client: AiApiClient) : AiRepository {
    override suspend fun analyzeMealPhoto(photoPath: String): Resource<AiResponse> {
        // Gemini-specific logic
    }
}

class OpenAiAiRepository(private val client: OpenAiClient) : AiRepository {
    override suspend fun analyzeMealPhoto(photoPath: String): Resource<AiResponse> {
        // OpenAI-specific logic
    }
}

class MockAiRepository : AiRepository {
    override suspend fun analyzeMealPhoto(photoPath: String): Resource<AiResponse> {
        // Returns hardcoded test data
    }
}
```

### Koin Configuration for Easy Swapping

```kotlin
// core/di/KoinModule.kt (Entry point)
val aiModule = when (BuildConfig.AI_PROVIDER) {
    "gemini" -> buildGeminiModule()
    "openai" -> buildOpenAiModule()
    "mock" -> buildMockModule()
    else -> throw IllegalArgumentException("Unknown AI provider")
}

fun buildGeminiModule() = module {
    single<AiApiClient> { GeminiFlashClient(get(), BuildConfig.GEMINI_API_KEY) }
    single<AiRepository> { GeminiFlashAiRepository(get()) }
}

fun buildOpenAiModule() = module {
    single<AiApiClient> { OpenAiClient(get(), BuildConfig.OPENAI_API_KEY) }
    single<AiRepository> { OpenAiAiRepository(get()) }
}

fun buildMockModule() = module {
    single<AiRepository> { MockAiRepository() }
}
```

### BuildConfig Configuration

In `app/build.gradle.kts`:

```kotlin
buildTypes {
    debug {
        buildConfigField("String", "GEMINI_API_KEY", "\"${project.properties["gemini.api.key"] as String?}\"")
        buildConfigField("String", "AI_PROVIDER", "\"gemini\"")
    }
    release {
        buildConfigField("String", "GEMINI_API_KEY", "\"${project.properties["gemini.api.key"] as String?}\"")
        buildConfigField("String", "AI_PROVIDER", "\"gemini\"")
    }
}
```

In `gradle.properties` (local, not committed):

```properties
gemini.api.key=YOUR_API_KEY_HERE
```

---

## 7. Offline-First Implementation

### Network Monitor

```kotlin
// core/network/NetworkMonitor.kt
interface NetworkMonitor {
    fun isOnline(): Boolean
    fun observeNetworkStatus(): Flow<Boolean>
}

class NetworkMonitorImpl(context: Context) : NetworkMonitor {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    override fun isOnline(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_INTERNET)
    }

    override fun observeNetworkStatus(): Flow<Boolean> = flow {
        emit(isOnline())
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = Unit
            override fun onLost(network: Network) = Unit
        }
        connectivityManager.registerDefaultNetworkCallback(callback)
        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }
}
```

### Offline-First Repository Pattern

```kotlin
// feature_meal_confirmation/data/repository/MealRepositoryImpl.kt
override fun getMeals(date: LocalDate): Flow<Resource<List<Meal>>> = flow {
    // Step 1: Emit loading with cached data
    emit(Resource.Loading())
    val cachedMeals = mealDao.getMealsForDate(date).map { it.toDomain() }
    emit(Resource.Loading(data = cachedMeals))

    // Step 2: If offline, return cached data
    if (!networkMonitor.isOnline()) {
        emit(Resource.Success(cachedMeals))
        return@flow
    }

    // Step 3: If online, attempt refresh (not relevant for V1 since all data is local)
    // Skip for now since Thali doesn't have a backend

    // Step 4: Return cached data as success
    emit(Resource.Success(cachedMeals))
}

override suspend fun saveMeal(meal: Meal): Resource<Unit> = runResourceCatchingIO {
    // Always save to Room first (device local)
    mealDao.insert(meal.toEntity())
    Unit
}
```

---

## 8. Photo Handling & Caching

### Photo Cache Manager

```kotlin
// feature_camera/data/local/PhotoCache.kt
class PhotoCacheManager(context: Context) {
    private val cacheDir = File(context.cacheDir, "meal_photos")

    init {
        cacheDir.mkdirs()
    }

    fun saveCapturedPhoto(bitmap: Bitmap): String {
        val fileName = "meal_${System.currentTimeMillis()}.jpg"
        val file = File(cacheDir, fileName)

        FileOutputStream(file).use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, output)
            output.flush()
        }

        return file.absolutePath
    }

    fun loadPhotoAsBitmap(photoPath: String): Bitmap? = try {
        BitmapFactory.decodeFile(photoPath)
    } catch (e: Exception) {
        null
    }

    fun convertPhotoToBase64(photoPath: String): String? = try {
        val bitmap = loadPhotoAsBitmap(photoPath) ?: return null
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        val byteArray = outputStream.toByteArray()
        Base64.getEncoder().encodeToString(byteArray)
    } catch (e: Exception) {
        null
    }

    fun cleanupOldPhotos(olderThanHours: Int = 24) {
        val cutoffTime = System.currentTimeMillis() - (olderThanHours * 3600000)
        cacheDir.listFiles()?.forEach { file ->
            if (file.lastModified() < cutoffTime) {
                file.delete()
            }
        }
    }
}
```

### Image Loading with Coil

```kotlin
// Displaying meal photo in Compose
@Composable
fun MealPhotoDisplay(photoPath: String, modifier: Modifier = Modifier) {
    Image(
        painter = rememberAsyncImagePainter(
            model = ImageRequest.Builder(LocalContext.current)
                .data(File(photoPath))
                .crossfade(true)
                .build(),
            contentScale = ContentScale.Crop
        ),
        contentDescription = "Meal Photo",
        modifier = modifier
    )
}
```

---

## 9. Testing Infrastructure

### Unit Tests: Use Case

```kotlin
// feature_meal_confirmation/domain/use_case/ConfirmAndLogMealUseCase_Test.kt
class ConfirmAndLogMealUseCase_Test {

    private val mockRepository = mockk<MealConfirmationRepository>()
    private val mockValidateUseCase = mockk<ValidateMealUseCase>()
    private val useCase = ConfirmAndLogMealUseCase(mockRepository, mockValidateUseCase)

    @Test
    fun validMeal_savesSuccessfully() = runBlocking {
        // Arrange
        val meal = Meal(id = "1", foods = listOf(...))
        coEvery { mockValidateUseCase(meal) } returns Resource.Success(Unit)
        coEvery { mockRepository.saveMeal(meal) } returns Resource.Success(Unit)

        // Act
        val result = useCase(meal)

        // Assert
        assertTrue(result is Resource.Success)
        coVerify { mockRepository.saveMeal(meal) }
    }

    @Test
    fun invalidMeal_returnsError() = runBlocking {
        // Arrange
        val meal = Meal(id = "1", foods = emptyList())
        coEvery { mockValidateUseCase(meal) } returns Resource.Error("No foods")

        // Act
        val result = useCase(meal)

        // Assert
        assertTrue(result is Resource.Error)
        coVerify(exactly = 0) { mockRepository.saveMeal(any()) }
    }
}
```

### Integration Tests: Room + Repository

```kotlin
// feature_meal_confirmation/data/repository/MealRepositoryImpl_Test.kt
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
        ).allowMainThreadQueries().build()

        mealDao = database.mealDao()
        repository = MealRepositoryImpl(mealDao)
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun saveMeal_shouldInsertIntoDatabase() = runBlocking {
        // Arrange
        val mealEntity = MealEntity(
            id = "1",
            date = LocalDate.now(),
            totalCalories = 600,
            totalProteinGrams = 20f,
            totalCarbsGrams = 75f,
            totalFatGrams = 15f,
            totalFiberGrams = 5f
        )

        // Act
        repository.saveMeal(mealEntity.toDomain())

        // Assert
        val saved = mealDao.getMealById("1").first()
        assertEquals(saved.id, "1")
        assertEquals(saved.totalCalories, 600)
    }

    @Test
    fun getMealsForDate_shouldReturnMultipleMeals() = runBlocking {
        // Arrange
        val today = LocalDate.now()
        val meal1 = MealEntity(id = "1", date = today, totalCalories = 500, totalProteinGrams = 15f, totalCarbsGrams = 60f, totalFatGrams = 10f, totalFiberGrams = 3f)
        val meal2 = MealEntity(id = "2", date = today, totalCalories = 700, totalProteinGrams = 25f, totalCarbsGrams = 80f, totalFatGrams = 20f, totalFiberGrams = 5f)

        mealDao.insert(meal1)
        mealDao.insert(meal2)

        // Act
        val result = mealDao.getMealsForDate(today).first()

        // Assert
        assertEquals(result.size, 2)
        assertEquals(result.sumOf { it.totalCalories }, 1200)
    }
}
```

### Compose UI Tests

```kotlin
// feature_meal_confirmation/presentation/ConfirmationScreen_Test.kt
@RunWith(AndroidJUnit4::class)
class ConfirmationScreen_Test {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val mockViewModel = mockk<ConfirmationViewModel>(relaxed = true)

    @Before
    fun setup() {
        composeTestRule.setContent {
            ConfirmationScreen(viewModel = mockViewModel)
        }
    }

    @Test
    fun tappingConfirmButton_sendsAction() {
        composeTestRule.onNodeWithText("Confirm & Log").performClick()
        verify { mockViewModel.onAction(ConfirmationAction.OnConfirmAndLogClicked) }
    }

    @Test
    fun editingQuantity_updatesState() {
        composeTestRule.onNodeWithTag("quantity_input").performTextReplacement("200")
        // Assert via state observations
    }
}
```

---

## 10. Performance Optimization

### LazyColumn for Long Lists

```kotlin
@Composable
fun MealHistoryList(meals: List<HistoryMeal>, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier) {
        items(
            items = meals,
            key = { it.id }
        ) { meal ->
            MealHistoryItem(meal)
        }
    }
}
```

### Image Optimization

```kotlin
// Reduce memory footprint
val imageRequest = ImageRequest.Builder(context)
    .data(photoPath)
    .size(800, 800)  // Downscale to device resolution
    .build()
```

### Database Indexes

```kotlin
@Entity(
    tableName = "meals",
    indices = [
        Index("date", "userId"),  // For queries like "get meals for date"
        Index("userId"),           // For user-specific queries
        Index("createdAt")         // For ordering
    ]
)
data class MealEntity(...)
```

---

## 11. Constants & Configuration

```kotlin
// core/common/Constants.kt
object Constants {
    // API Configuration
    const val GEMINI_API_TIMEOUT_SECONDS = 30
    const val AI_ANALYSIS_MAX_RETRIES = 2
    const val MEAL_PHOTO_MAX_SIZE_MB = 5

    // Database
    const val DATABASE_NAME = "thali.db"
    const val DATABASE_VERSION = 1

    // UI Constants
    const val DEBOUNCE_DELAY_MS = 300L
    const val LOADING_TIMEOUT_MS = 4000L  // Show error if AI takes > 4s

    // Macro Calculation
    const val CALORIE_PER_GRAM_PROTEIN = 4f
    const val CALORIE_PER_GRAM_CARBS = 4f
    const val CALORIE_PER_GRAM_FAT = 9f

    // Clarification Logic
    const val CONFIDENCE_THRESHOLD_FOR_CLARIFICATION = 0.75f
    const val MAX_CLARIFICATION_QUESTIONS = 3

    // Food Memory
    const val FOOD_MEMORY_SIMILARITY_THRESHOLD = 0.75f
    const val FOOD_MEMORY_RETENTION_DAYS = 90
}
```

---

## 12. Manifest Permissions

```xml
<!-- AndroidManifest.xml -->
<manifest ...>
    <!-- Camera -->
    <uses-permission android:name="android.permission.CAMERA" />
    
    <!-- Network -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    
    <!-- Storage (for saving photos locally) -->
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />

    <application ...>
        <!-- Permissions for CameraX -->
        <service android:name=".service.CameraService" />
    </application>
</manifest>
```

---

## 13. BuildTypes & Flavors (Optional for V1)

```kotlin
// app/build.gradle.kts
android {
    buildTypes {
        debug {
            debuggable = true
            buildConfigField("Boolean", "DEBUG_LOGGING", "true")
            buildConfigField("String", "AI_PROVIDER", "\"gemini\"")
            buildConfigField("Long", "LOADING_TIMEOUT_MS", "4000L")
        }

        release {
            debuggable = false
            minifyEnabled = true
            shrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            buildConfigField("Boolean", "DEBUG_LOGGING", "false")
            buildConfigField("String", "AI_PROVIDER", "\"gemini\"")
            buildConfigField("Long", "LOADING_TIMEOUT_MS", "2000L")
        }
    }

    flavorDimensions.add("environment")
    productFlavors {
        create("dev") {
            dimension = "environment"
            buildConfigField("String", "API_BASE_URL", "\"https://dev-api.example.com\"")
        }
        create("prod") {
            dimension = "environment"
            buildConfigField("String", "API_BASE_URL", "\"https://api.example.com\"")
        }
    }
}
```

---

## 14. Key Implementation Decisions & Rationale

| Decision | Rationale |
|----------|-----------|
| Room as Single Source of Truth | Offline-tolerant by design; no data loss |
| Ktor over Retrofit | Kotlin-first, coroutine-native, multiplatform-ready |
| Coil over Glide | Lighter weight, Compose-native, Kotlin-first |
| Jetpack Nav Compose | Type-safe routes, official, handles back stack |
| Koin over Hilt | Simpler for a single-module app, easier per-feature configuration |
| MVI pattern in MVVM | Single immutable State eliminates race conditions |
| Use Cases as orchestrators | Keeps ViewModels thin, logic testable, reusable |
| Swappable AI interface | Future-proofs for API swaps without code changes |
| Local caching with async image loading | Fast UI, graceful offline mode |
| Compose over XML layouts | Modern, declarative, rapid prototyping |

---

## 15. Future Scalability

### Multi-Module Refactor (Post-V1)

```
// Current (single module, V1)
app/ → all code

// Future (multi-module, V2+)
app/ → only MainActivity, Application class
feature_onboarding/ (Gradle module)
feature_meal_logging/ (Gradle module)
feature_camera/ (Gradle module)
feature_ai_analysis/ (Gradle module)
feature_history/ (Gradle module)
core/ (Gradle module)
```

### Backend Integration (Post-V1)

When V2 adds cloud sync and a backend:

```kotlin
// Create core/backend/ with Ktor client for cloud APIs
class CloudMealRepository(
    private val localRepository: MealRepository,
    private val cloudApi: CloudMealApi,
    private val networkMonitor: NetworkMonitor
) : MealRepository {
    override suspend fun saveMeal(meal: Meal): Resource<Unit> {
        // Save locally first (always succeeds)
        val local = localRepository.saveMeal(meal)
        
        // If online, sync to cloud
        if (networkMonitor.isOnline()) {
            try {
                cloudApi.syncMeal(meal)
            } catch (e: Exception) {
                // Log, but don't fail — local save already succeeded
            }
        }
        
        return local
    }
}
```

---

**This TRD provides everything needed to build Thali from scratch with the chosen tech stack.**

