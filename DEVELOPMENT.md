# Development Guide

## Table of Contents
1. [Setup](#setup)
2. [Project Structure](#project-structure)
3. [Development Workflow](#development-workflow)
4. [Adding New Capabilities](#adding-new-capabilities)
5. [Extending the AI](#extending-the-ai)
6. [Testing](#testing)
7. [Code Style](#code-style)
8. [Common Tasks](#common-tasks)

## Setup

### Prerequisites
- **Android Studio**: Iguana (2023.2.1) or later
- **JDK**: 17 or later
- **Kotlin**: 1.9.20+
- **Gradle**: 8.2+
- **Min SDK**: 26 (Android 8.0 Oreo)
- **Target SDK**: 34 (Android 14)

### Initial Setup

1. **Clone and Open**
   ```bash
   git clone https://github.com/Amank326/SELF-EVOLVING-AUTONOMOUS-AI-HUMAN-OS-SA-AIHOS-.git
   cd SELF-EVOLVING-AUTONOMOUS-AI-HUMAN-OS-SA-AIHOS-
   ```

2. **Open in Android Studio**
   - File → Open → Select project directory
   - Wait for Gradle sync to complete

3. **Run the App**
   - Connect device or start emulator
   - Click Run (▶) or press Shift+F10
   - Select deployment target

### Common Setup Issues

**Issue: Gradle sync failed**
```bash
# Solution: Clean and rebuild
./gradlew clean
./gradlew build --refresh-dependencies
```

**Issue: Kotlin version mismatch**
```bash
# Check kotlin version in build.gradle.kts
# Update to match: kotlin("android") version "1.9.20"
```

## Project Structure

```
SELF-EVOLVING-AUTONOMOUS-AI-HUMAN-OS-SA-AIHOS-/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/aihos/selfevolving/
│   │   │   │   ├── domain/           # Business logic (pure Kotlin)
│   │   │   │   │   ├── model/        # Data models
│   │   │   │   │   ├── repository/   # Repository interfaces
│   │   │   │   │   └── usecase/      # Use cases
│   │   │   │   ├── data/             # Data layer
│   │   │   │   │   ├── local/        # Room database
│   │   │   │   │   └── repository/   # Repository implementations
│   │   │   │   ├── presentation/     # UI layer
│   │   │   │   │   ├── home/         # Home screen
│   │   │   │   │   └── theme/        # Material theme
│   │   │   │   ├── di/               # Dependency injection
│   │   │   │   └── AiHosApplication.kt
│   │   │   ├── res/                  # Resources
│   │   │   └── AndroidManifest.xml
│   │   └── test/                     # Unit tests
│   └── build.gradle.kts
├── build.gradle.kts                  # Root build file
├── settings.gradle.kts
├── gradle.properties
├── README.md
├── ARCHITECTURE.md
├── EVOLUTION_EXPLAINED.md
└── DEVELOPMENT.md (this file)
```

## Development Workflow

### 1. Create a Feature Branch
```bash
git checkout -b feature/your-feature-name
```

### 2. Make Changes
Follow the architecture pattern:
- Domain models → Repository interfaces → Use cases
- Repository implementations → DAOs → Entities
- ViewModels → Compose screens

### 3. Test Your Changes
```bash
# Run unit tests
./gradlew test

# Run on device
./gradlew installDebug
```

### 4. Commit and Push
```bash
git add .
git commit -m "feat: add your feature"
git push origin feature/your-feature-name
```

### 5. Create Pull Request
- Go to GitHub
- Create PR from your branch to main
- Fill in PR template
- Wait for review

## Adding New Capabilities

### Example: Adding "Weather Awareness" Capability

#### Step 1: Define Domain Model

**File**: `domain/model/Models.kt`
```kotlin
// Add to existing file
data class WeatherData(
    val temperature: Float,
    val condition: String,
    val timestamp: Long
)

// Add new capability type
enum class CapabilityType {
    MEMORY_MANAGEMENT,
    REASONING,
    REFLECTION,
    WEATHER_AWARENESS  // New!
}
```

#### Step 2: Add Repository Interface

**File**: `domain/repository/Repositories.kt`
```kotlin
interface WeatherRepository {
    suspend fun recordWeather(data: WeatherData): Result<Unit>
    suspend fun getWeatherHistory(days: Int): Flow<List<WeatherData>>
    suspend fun analyzeWeatherPatterns(): Result<List<String>>
}
```

#### Step 3: Create Use Cases

**File**: `domain/usecase/WeatherUseCases.kt`
```kotlin
class RecordWeatherUseCase @Inject constructor(
    private val weatherRepository: WeatherRepository,
    private val evolveCapabilityUseCase: EvolveCapabilityUseCase
) {
    suspend operator fun invoke(data: WeatherData): Result<Unit> {
        val result = weatherRepository.recordWeather(data)
        
        if (result.isSuccess) {
            // Evolve weather capability
            evolveCapabilityUseCase(
                "Weather Awareness",
                "Recorded weather data"
            )
        }
        
        return result
    }
}
```

#### Step 4: Add Database Entity and DAO

**File**: `data/local/Database.kt`
```kotlin
@Entity(tableName = "weather_data")
data class WeatherEntity(
    @PrimaryKey val id: String,
    val temperature: Float,
    val condition: String,
    val timestamp: Long
)

@Dao
interface WeatherDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(weather: WeatherEntity)
    
    @Query("SELECT * FROM weather_data ORDER BY timestamp DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<WeatherEntity>>
}

// Add to AiHosDatabase
@Database(
    entities = [
        // ... existing entities
        WeatherEntity::class  // Add this
    ],
    version = 2,  // Increment version!
    exportSchema = false
)
abstract class AiHosDatabase : RoomDatabase() {
    // ... existing DAOs
    abstract fun weatherDao(): WeatherDao  // Add this
}
```

#### Step 5: Implement Repository

**File**: `data/repository/WeatherRepositoryImpl.kt`
```kotlin
class WeatherRepositoryImpl @Inject constructor(
    private val weatherDao: WeatherDao,
    private val gson: Gson
) : WeatherRepository {
    
    override suspend fun recordWeather(data: WeatherData): Result<Unit> = runCatching {
        val entity = data.toEntity()
        weatherDao.insert(entity)
    }
    
    override suspend fun getWeatherHistory(days: Int): Flow<List<WeatherData>> {
        return weatherDao.getRecent(days).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override suspend fun analyzeWeatherPatterns(): Result<List<String>> = runCatching {
        // Implement pattern analysis
        listOf("Pattern 1", "Pattern 2")
    }
}

// Extension functions
private fun WeatherData.toEntity() = WeatherEntity(
    id = java.util.UUID.randomUUID().toString(),
    temperature = temperature,
    condition = condition,
    timestamp = timestamp
)

private fun WeatherEntity.toDomain() = WeatherData(
    temperature = temperature,
    condition = condition,
    timestamp = timestamp
)
```

#### Step 6: Add to Dependency Injection

**File**: `di/AppModule.kt`
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    // ... existing providers
    
    @Provides
    fun provideWeatherDao(database: AiHosDatabase) = database.weatherDao()
    
    @Provides
    @Singleton
    fun provideWeatherRepository(
        weatherDao: WeatherDao,
        gson: Gson
    ): WeatherRepository {
        return WeatherRepositoryImpl(weatherDao, gson)
    }
}
```

#### Step 7: Add to ViewModel

**File**: `presentation/home/AiHosViewModel.kt`
```kotlin
@HiltViewModel
class AiHosViewModel @Inject constructor(
    // ... existing dependencies
    private val recordWeatherUseCase: RecordWeatherUseCase
) : ViewModel() {
    
    fun recordWeather(temperature: Float, condition: String) {
        viewModelScope.launch {
            val data = WeatherData(
                temperature = temperature,
                condition = condition,
                timestamp = System.currentTimeMillis()
            )
            
            recordWeatherUseCase(data)
                .onSuccess {
                    addLog("Weather recorded: $condition, ${temperature}°")
                }
                .onFailure { error ->
                    addLog("Failed to record weather: ${error.message}")
                }
        }
    }
}
```

#### Step 8: Add UI Button

**File**: `presentation/home/AiHosScreen.kt`
```kotlin
// Add to ControlButtons
ControlButton(
    text = "Weather",
    icon = Icons.Default.Cloud,
    onClick = { 
        // Show weather input dialog
    },
    enabled = !isProcessing,
    modifier = Modifier.weight(1f)
)
```

#### Step 9: Update Database Version

**Important**: When you add new entities, increment the database version:

```kotlin
@Database(
    entities = [...],
    version = 2,  // Increment from 1 to 2
    exportSchema = false
)
```

## Extending the AI

### Adding New Reasoning Algorithms

**File**: `domain/usecase/ReasoningAlgorithms.kt`
```kotlin
interface ReasoningAlgorithm {
    fun reason(
        query: String,
        memories: List<Memory>
    ): InferenceResult
}

class PatternMatchingAlgorithm : ReasoningAlgorithm {
    override fun reason(
        query: String,
        memories: List<Memory>
    ): InferenceResult {
        // Implement pattern matching logic
        return InferenceResult(
            conclusion = "...",
            confidence = 0.8f,
            steps = listOf(...)
        )
    }
}

// Use in PerformReasoningUseCase
class PerformReasoningUseCase @Inject constructor(
    // ...
    private val algorithms: Set<ReasoningAlgorithm>
) {
    suspend operator fun invoke(query: String): Result<ReasoningContext> {
        // Try multiple algorithms, use best result
        val results = algorithms.map { it.reason(query, memories) }
        val bestResult = results.maxByOrNull { it.confidence }
        // ...
    }
}
```

### Adding Custom Memory Types

**File**: `domain/model/Models.kt`
```kotlin
enum class MemoryType {
    SHORT_TERM,
    LONG_TERM,
    EPISODIC,
    SEMANTIC,
    PROCEDURAL,
    SPATIAL,        // New: Location-based memories
    EMOTIONAL,      // New: Sentiment-tagged memories
    TEMPORAL        // New: Time-sensitive memories
}

// Add metadata for custom types
data class Memory(
    // ... existing fields
    val metadata: Map<String, Any> = emptyMap()
)
```

### Adding Background Tasks

**File**: `data/worker/EvolutionWorker.kt`
```kotlin
@HiltWorker
class EvolutionWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val performReflectionUseCase: PerformReflectionUseCase
) : CoroutineWorker(appContext, workerParams) {
    
    override suspend fun doWork(): Result {
        return try {
            // Perform automatic reflection
            performReflectionUseCase()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

// Schedule in Application class
class AiHosApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Schedule daily evolution
        val workRequest = PeriodicWorkRequestBuilder<EvolutionWorker>(
            24, TimeUnit.HOURS
        ).build()
        
        WorkManager.getInstance(this).enqueue(workRequest)
    }
}
```

## Testing

### Unit Tests

**File**: `test/domain/usecase/StoreMemoryUseCaseTest.kt`
```kotlin
class StoreMemoryUseCaseTest {
    
    private lateinit var useCase: StoreMemoryUseCase
    private lateinit var repository: MemoryRepository
    
    @Before
    fun setup() {
        repository = mockk()
        useCase = StoreMemoryUseCase(repository)
    }
    
    @Test
    fun `storing memory succeeds`() = runTest {
        // Given
        val memory = Memory(
            id = "1",
            content = "Test",
            timestamp = 12345L,
            type = MemoryType.SHORT_TERM,
            importance = 0.5f
        )
        coEvery { repository.storeMemory(any()) } returns Result.success(Unit)
        
        // When
        val result = useCase(memory)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { repository.storeMemory(memory) }
    }
}
```

### UI Tests

**File**: `androidTest/presentation/AiHosScreenTest.kt`
```kotlin
@HiltAndroidTest
class AiHosScreenTest {
    
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()
    
    @Test
    fun memoryButtonIsDisplayed() {
        composeTestRule.onNodeWithText("Memory").assertIsDisplayed()
    }
    
    @Test
    fun clickingMemoryButtonShowsDialog() {
        composeTestRule.onNodeWithText("Memory").performClick()
        composeTestRule.onNodeWithText("Store Memory").assertIsDisplayed()
    }
}
```

## Code Style

### Kotlin Conventions

```kotlin
// Use meaningful names
val memoryRetentionThreshold = 0.3f  // Good
val x = 0.3f  // Bad

// Prefer expression bodies for simple functions
fun calculateConfidence(value: Float): Float = value.coerceIn(0f, 1f)

// Use when for multiple conditions
when (memoryType) {
    MemoryType.SHORT_TERM -> handleShortTerm()
    MemoryType.LONG_TERM -> handleLongTerm()
    else -> handleOther()
}

// Use scope functions appropriately
memory.copy(importance = 0.9f).also {
    repository.store(it)
}
```

### Architecture Rules

1. **Domain layer** should have no Android dependencies
2. **Data layer** implements interfaces from domain
3. **Presentation layer** only calls use cases, never repositories
4. **Use dependency injection** for all dependencies
5. **Keep functions small** and focused on single responsibility

### Documentation

```kotlin
/**
 * Stores a memory in the memory layer with automatic importance calculation.
 *
 * This use case handles the business logic for memory storage, including:
 * - Importance calculation based on content and context
 * - Memory type classification
 * - Association linking with existing memories
 *
 * @param memory The memory to store
 * @return Result indicating success or failure
 */
class StoreMemoryUseCase @Inject constructor(
    private val memoryRepository: MemoryRepository
) {
    suspend operator fun invoke(memory: Memory): Result<Unit> {
        // Implementation
    }
}
```

## Common Tasks

### Adding a New Screen

1. Create screen file in `presentation/newscreen/`
2. Create ViewModel
3. Add navigation (if needed)
4. Update MainActivity or navigation graph

### Modifying Database Schema

1. Update entity in `data/local/Database.kt`
2. **Increment database version**
3. Add migration (optional) or use `fallbackToDestructiveMigration()`
4. Update DAO methods
5. Update repository implementations

### Adding Dependencies

1. Add to `app/build.gradle.kts`
2. Sync Gradle
3. Add to DI module if needed

### Debugging Tips

```kotlin
// Use Logcat
import android.util.Log
Log.d("AiHos", "Memory stored: $memory")

// Use breakpoints in Android Studio
// Click left gutter to set breakpoint

// Use Layout Inspector for UI debugging
// Tools → Layout Inspector

// Check database contents
// View → Tool Windows → App Inspection → Database Inspector
```

### Performance Optimization

```kotlin
// Use Flow for reactive data
val memories: Flow<List<Memory>> = memoryDao.getAllMemories()

// Use LazyColumn for large lists
LazyColumn {
    items(logs) { log ->
        Text(log)
    }
}

// Use remember for expensive calculations
val expensiveValue = remember(dependency) {
    calculateExpensiveValue()
}

// Use side effects correctly
LaunchedEffect(key) {
    // Run once when key changes
}
```

## Contributing Checklist

Before submitting a PR:

- [ ] Code follows Kotlin style guide
- [ ] All tests pass (`./gradlew test`)
- [ ] New features have tests
- [ ] Documentation updated
- [ ] No compiler warnings
- [ ] No hardcoded strings (use strings.xml)
- [ ] Database version incremented if schema changed
- [ ] Code reviewed by yourself first
- [ ] Commit messages are clear
- [ ] Branch is up to date with main

## Getting Help

- **Documentation**: Read ARCHITECTURE.md and EVOLUTION_EXPLAINED.md
- **Issues**: Check existing GitHub issues
- **Discussions**: Use GitHub Discussions for questions
- **Code**: Read existing code for patterns and examples

## Resources

- [Kotlin Documentation](https://kotlinlang.org/docs/home.html)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Clean Architecture](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Room Database](https://developer.android.com/training/data-storage/room)
- [Hilt Dependency Injection](https://developer.android.com/training/dependency-injection/hilt-android)
- [Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)

Happy coding! 🚀
