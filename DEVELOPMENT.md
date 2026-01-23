# SA-AIHOS Development Guide

This guide explains how to extend SA-AIHOS and understand its architecture for developers.

---

## Project Structure

```
SA-AIHOS/
├── app/src/main/
│   ├── AndroidManifest.xml
│   └── kotlin/com/aihos/
│       ├── ai/                         # AI Logic Layers
│       │   ├── autonomy/
│       │   │   ├── AutonomyController.kt
│       │   │   └── impl/DefaultAutonomyController.kt
│       │   ├── evolution/
│       │   │   ├── EvolutionEngine.kt
│       │   │   └── impl/DefaultEvolutionEngine.kt
│       │   ├── memory/
│       │   │   ├── MemoryLayer.kt
│       │   │   └── impl/DefaultMemoryLayer.kt
│       │   ├── reasoning/
│       │   │   ├── ReasoningLayer.kt
│       │   │   └── impl/DefaultReasoningLayer.kt
│       │   └── reflection/
│       │       ├── ReflectionLayer.kt
│       │       └── impl/DefaultReflectionLayer.kt
│       ├── data/                       # Data Layer
│       │   ├── db/
│       │   │   ├── Database.kt
│       │   │   ├── dao/DAOs.kt
│       │   │   └── entity/Entities.kt
│       │   └── repository/
│       │       └── MemoryRepository.kt
│       ├── di/                         # Dependency Injection
│       │   ├── Module.kt
│       │   └── Implementations.kt
│       ├── ui/                         # UI Layer
│       │   ├── MainActivity.kt
│       │   ├── SAIHOSApp.kt
│       │   ├── screens/
│       │   │   ├── DashboardScreen.kt
│       │   │   ├── MemoryScreen.kt
│       │   │   ├── EvolutionScreen.kt
│       │   │   └── SettingsScreen.kt
│       │   └── viewmodel/
│       │       └── SAIHOSViewModel.kt
│       └── SAIHOSApplication.kt        # App Entry Point
├── docs/
├── build.gradle.kts
├── settings.gradle.kts
├── ARCHITECTURE_GUIDE.md
├── QUICK_START.md
└── README.md
```

---

## Core Concepts

### 1. Clean Architecture Layers

**Domain Layer** (AI Logic)
```kotlin
interface MemoryLayer {
    suspend fun storeMemory(memory: MemoryItem): String
    suspend fun retrieveBySemantics(query: String, limit: Int): List<MemoryItem>
    // ... more methods
}
```

**Data Layer** (Persistence)
```kotlin
class MemoryRepository(private val dao: MemoryDao) : MemoryLayer {
    override suspend fun storeMemory(memory: MemoryItem): String {
        val entity = memory.toEntity()
        dao.insertMemory(entity)
        return entity.id
    }
    // ... implementation
}
```

**Presentation Layer** (UI)
```kotlin
class SAIHOSViewModel(
    private val memoryLayer: MemoryLayer
) : ViewModel() {
    // UI logic
}
```

### 2. MVVM Pattern

**Model**: Data classes (MemoryItem, ReasoningResult, etc)
**View**: Jetpack Compose screens
**ViewModel**: SAIHOSViewModel orchestrating everything

### 3. Dependency Injection (Hilt)

All dependencies provided in Module.kt:
```kotlin
@Provides
fun provideMemoryLayer(): MemoryLayer = DefaultMemoryLayer()

@Provides
fun provideDatabase(context: Context): SAIHOSDatabase = 
    SAIHOSDatabase.getInstance(context)
```

---

## Adding New AI Capabilities

### Step 1: Define the Interface

```kotlin
// ai/yourfeature/YourFeature.kt
interface YourFeature {
    suspend fun doSomething(input: String): Result
    suspend fun getStatus(): Status
}

@Serializable
data class Result(
    val success: Boolean,
    val data: String
)
```

### Step 2: Create Domain Models

```kotlin
// In same file or separate file
@Serializable
data class YourData(
    val id: String,
    val content: String,
    val metadata: Map<String, String>
)
```

### Step 3: Create Database Entity (if needed)

```kotlin
// data/db/entity/Entities.kt
@Entity(tableName = "your_data")
data class YourDataEntity(
    @PrimaryKey val id: String,
    val content: String,
    val metadata: String = "{}"
)
```

### Step 4: Create DAO

```kotlin
// data/db/dao/DAOs.kt
@Dao
interface YourDataDao {
    @Insert
    suspend fun insert(item: YourDataEntity)
    
    @Query("SELECT * FROM your_data WHERE id = :id")
    suspend fun getById(id: String): YourDataEntity?
    
    // ... more methods
}
```

### Step 5: Add DAO to Database

```kotlin
// data/db/Database.kt
@Database(entities = [..., YourDataEntity::class], ...)
abstract class SAIHOSDatabase : RoomDatabase() {
    abstract fun yourDataDao(): YourDataDao
}
```

### Step 6: Create Repository

```kotlin
// data/repository/YourRepository.kt
class YourRepository(
    private val dao: YourDataDao
) : YourFeature {
    override suspend fun doSomething(input: String): Result {
        // Implementation
    }
}
```

### Step 7: Implement Default Version (optional)

```kotlin
// ai/yourfeature/impl/DefaultYourFeature.kt
class DefaultYourFeature : YourFeature {
    override suspend fun doSomething(input: String): Result {
        // Simple in-memory implementation
    }
}
```

### Step 8: Inject in Module

```kotlin
// di/Module.kt
@Provides
fun provideYourRepository(database: SAIHOSDatabase): YourFeature {
    return YourRepository(database.yourDataDao())
}
```

### Step 9: Use in ViewModel

```kotlin
// ui/viewmodel/SAIHOSViewModel.kt
class SAIHOSViewModel(
    private val yourFeature: YourFeature
) : ViewModel() {
    fun useYourFeature() {
        viewModelScope.launch {
            val result = yourFeature.doSomething("input")
            // Update UI state
        }
    }
}
```

### Step 10: Build UI

```kotlin
// ui/screens/YourScreen.kt
@Composable
fun YourScreen(viewModel: SAIHOSViewModel) {
    // UI code using Compose
}
```

---

## Testing Strategy

### Unit Tests
Test individual AI layers in isolation

```kotlin
class MemoryLayerTest {
    @Test
    fun testStoreAndRetrieve() = runTest {
        val memory = MemoryLayer()
        val id = memory.storeMemory(MemoryItem(...))
        val retrieved = memory.retrieveById(id)
        assertEquals(id, retrieved?.id)
    }
}
```

### Integration Tests
Test interactions between layers

```kotlin
class MemoryRepositoryTest {
    @Test
    fun testDatabasePersistence() = runTest {
        val repo = MemoryRepository(dao)
        val id = repo.storeMemory(item)
        // Verify it's in database
    }
}
```

### UI Tests
Test Compose screens

```kotlin
class DashboardScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun testScreenRenders() {
        composeTestRule.setContent {
            DashboardScreen()
        }
        composeTestRule.onNodeWithText("Dashboard").assertIsDisplayed()
    }
}
```

---

## Common Patterns

### 1. Async Operations with Coroutines

```kotlin
class SAIHOSViewModel(
    private val memoryLayer: MemoryLayer
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    fun storeMemory(memory: MemoryItem) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val id = memoryLayer.storeMemory(memory)
                _uiState.value = UiState.Success(id)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
```

### 2. Flow for Real-time Updates

```kotlin
class RealtimeMonitor(
    private val evolutionEngine: EvolutionEngine
) {
    val metricsFlow: Flow<EvolutionMetrics> = flow {
        while (true) {
            emit(evolutionEngine.getEvolutionMetrics())
            delay(1000) // Update every second
        }
    }
}
```

### 3. Database Transactions

```kotlin
suspend fun updateMultiple() = withContext(Dispatchers.IO) {
    database.withTransaction {
        dao.insert(item1)
        dao.insert(item2)
        dao.update(item3)
        // All succeed or all fail
    }
}
```

### 4. Pagination

```kotlin
@Dao
interface PaginatedDao {
    @Query("SELECT * FROM memories LIMIT :limit OFFSET :offset")
    suspend fun getPage(limit: Int, offset: Int): List<MemoryEntity>
}
```

---

## Performance Considerations

### 1. Memory Management
- Use flows for large datasets
- Implement pagination
- Clean up old data regularly

### 2. Database Optimization
- Use indices for frequently queried fields
- Batch operations with transactions
- Consider table size growth

### 3. Background Work
Use WorkManager for long-running tasks:

```kotlin
class MemoryConsolidationWorker(context: Context, params: WorkerParameters) : 
    CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        // Consolidate learning, clean up old memories
        return Result.success()
    }
}
```

### 4. Monitoring
Add logging and metrics:

```kotlin
Timber.d("Memory stored: ${memory.id} (${memory.type})")
Timber.e(exception, "Error storing memory")
```

---

## Building for Production

### Pre-release Checklist

- [ ] All AI layers have default + production implementations
- [ ] Database migrations defined and tested
- [ ] Error handling comprehensive
- [ ] Logging configurable (debug vs release)
- [ ] Performance profiled
- [ ] Memory leaks checked
- [ ] Privacy audit completed
- [ ] Tests passing (90%+ coverage)
- [ ] Documentation updated
- [ ] CHANGELOG updated

---

## Useful Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run tests
./gradlew test

# Lint
./gradlew lint

# Clean
./gradlew clean

# Dependency tree
./gradlew dependencies
```

---

## Resources

- [Android Developer Docs](https://developer.android.com)
- [Compose Documentation](https://developer.android.com/jetpack/compose)
- [Room Database Guide](https://developer.android.com/training/data-storage/room)
- [Hilt Documentation](https://developer.android.com/training/dependency-injection/hilt-android)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)

---

## Getting Help

1. Check existing code in the repository
2. Read ARCHITECTURE_GUIDE.md
3. Review test examples
4. Create an issue with detailed description

---

Happy developing! 🚀
