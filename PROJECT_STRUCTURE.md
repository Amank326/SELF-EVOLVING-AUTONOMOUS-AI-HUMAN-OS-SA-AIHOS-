# Project Structure

Complete overview of the SA-AIHOS project structure.

## Directory Tree

```
SELF-EVOLVING-AUTONOMOUS-AI-HUMAN-OS-SA-AIHOS-/
│
├── 📄 README.md                      # Main project documentation
├── 📄 ARCHITECTURE.md                # Technical architecture guide
├── 📄 EVOLUTION_EXPLAINED.md         # How AI evolves over time
├── 📄 DEVELOPMENT.md                 # Development guide for contributors
├── 📄 QUICKSTART.md                  # Quick start guide
├── 📄 PROJECT_STRUCTURE.md           # This file
├── 📄 LICENSE                        # MIT License
├── 📄 .gitignore                     # Git ignore rules
│
├── 📁 gradle/                        # Gradle wrapper files
│   └── wrapper/
│       └── gradle-wrapper.properties
│
├── 📄 build.gradle.kts               # Root build configuration
├── 📄 settings.gradle.kts            # Project settings
├── 📄 gradle.properties              # Gradle properties
│
└── 📁 app/                           # Main application module
    │
    ├── 📄 build.gradle.kts           # App module build config
    ├── 📄 proguard-rules.pro         # ProGuard rules
    │
    └── 📁 src/
        │
        ├── 📁 main/
        │   │
        │   ├── 📄 AndroidManifest.xml
        │   │
        │   ├── 📁 java/com/aihos/selfevolving/
        │   │   │
        │   │   ├── 📄 AiHosApplication.kt     # Application class
        │   │   │
        │   │   ├── 📁 domain/                  # 🎯 DOMAIN LAYER (Business Logic)
        │   │   │   │
        │   │   │   ├── 📁 model/
        │   │   │   │   └── 📄 Models.kt        # Domain models
        │   │   │   │       ├── Memory
        │   │   │   │       ├── ReasoningContext
        │   │   │   │       ├── ReflectionEntry
        │   │   │   │       ├── EvolutionState
        │   │   │   │       ├── AutonomousTask
        │   │   │   │       └── AiState
        │   │   │   │
        │   │   │   ├── 📁 repository/
        │   │   │   │   └── 📄 Repositories.kt  # Repository interfaces
        │   │   │   │       ├── MemoryRepository
        │   │   │   │       ├── ReasoningRepository
        │   │   │   │       ├── ReflectionRepository
        │   │   │   │       ├── EvolutionRepository
        │   │   │   │       ├── AutonomyRepository
        │   │   │   │       └── AiStateRepository
        │   │   │   │
        │   │   │   └── 📁 usecase/
        │   │   │       └── 📄 UseCases.kt      # Business logic operations
        │   │   │           ├── StoreMemoryUseCase
        │   │   │           ├── RetrieveRelevantMemoriesUseCase
        │   │   │           ├── PerformReasoningUseCase
        │   │   │           ├── PerformReflectionUseCase
        │   │   │           ├── EvolveCapabilityUseCase
        │   │   │           └── ScheduleAutonomousTaskUseCase
        │   │   │
        │   │   ├── 📁 data/                     # 💾 DATA LAYER (Storage & Implementation)
        │   │   │   │
        │   │   │   ├── 📁 local/
        │   │   │   │   └── 📄 Database.kt       # Room database
        │   │   │   │       ├── Entities:
        │   │   │   │       │   ├── MemoryEntity
        │   │   │   │       │   ├── ReasoningContextEntity
        │   │   │   │       │   ├── ReflectionEntity
        │   │   │   │       │   ├── EvolutionStateEntity
        │   │   │   │       │   ├── AutonomousTaskEntity
        │   │   │   │       │   └── AiStateEntity
        │   │   │   │       │
        │   │   │   │       ├── DAOs:
        │   │   │   │       │   ├── MemoryDao
        │   │   │   │       │   ├── ReasoningDao
        │   │   │   │       │   ├── ReflectionDao
        │   │   │   │       │   ├── EvolutionDao
        │   │   │   │       │   ├── AutonomyDao
        │   │   │   │       │   └── AiStateDao
        │   │   │   │       │
        │   │   │   │       └── AiHosDatabase
        │   │   │   │
        │   │   │   └── 📁 repository/
        │   │   │       └── 📄 RepositoryImpl.kt # Repository implementations
        │   │   │           ├── MemoryRepositoryImpl
        │   │   │           ├── ReasoningRepositoryImpl
        │   │   │           ├── ReflectionRepositoryImpl
        │   │   │           ├── EvolutionRepositoryImpl
        │   │   │           ├── AutonomyRepositoryImpl
        │   │   │           └── AiStateRepositoryImpl
        │   │   │
        │   │   ├── 📁 presentation/             # 🎨 PRESENTATION LAYER (UI)
        │   │   │   │
        │   │   │   ├── 📄 MainActivity.kt       # Main activity
        │   │   │   │
        │   │   │   ├── 📁 home/
        │   │   │   │   ├── 📄 AiHosViewModel.kt # ViewModel
        │   │   │   │   └── 📄 AiHosScreen.kt    # Compose UI
        │   │   │   │       ├── AiHosScreen
        │   │   │   │       ├── AiStatusCard
        │   │   │   │       ├── ControlButtons
        │   │   │   │       ├── LogsSection
        │   │   │   │       └── InputDialog
        │   │   │   │
        │   │   │   └── 📁 theme/
        │   │   │       ├── 📄 Color.kt          # Color definitions
        │   │   │       ├── 📄 Type.kt           # Typography
        │   │   │       └── 📄 Theme.kt          # Material theme
        │   │   │
        │   │   └── 📁 di/                       # 💉 DEPENDENCY INJECTION
        │   │       └── 📄 AppModule.kt          # Hilt modules
        │   │           ├── DatabaseModule
        │   │           └── RepositoryModule
        │   │
        │   └── 📁 res/                          # Android resources
        │       │
        │       ├── 📁 values/
        │       │   ├── 📄 strings.xml           # String resources
        │       │   └── 📄 themes.xml            # Theme definitions
        │       │
        │       └── 📁 mipmap-anydpi-v26/
        │           ├── 📄 ic_launcher.xml
        │           └── 📄 ic_launcher_round.xml
        │
        └── 📁 test/                             # Unit tests (to be added)
            └── 📁 java/com/aihos/selfevolving/
```

## Layer Breakdown

### 🎯 Domain Layer (Pure Kotlin)
**Location**: `app/src/main/java/com/aihos/selfevolving/domain/`

**Purpose**: Contains business logic and domain models

**Components**:
- **Models**: Core data structures (Memory, ReasoningContext, etc.)
- **Repository Interfaces**: Define data operations
- **Use Cases**: Implement business logic

**Dependencies**: None (pure Kotlin, no Android dependencies)

**Files**:
- `domain/model/Models.kt` - All domain models
- `domain/repository/Repositories.kt` - All repository interfaces
- `domain/usecase/UseCases.kt` - All use case implementations

### 💾 Data Layer
**Location**: `app/src/main/java/com/aihos/selfevolving/data/`

**Purpose**: Handle data persistence and repository implementations

**Components**:
- **Local Database**: Room database with entities and DAOs
- **Repository Implementations**: Concrete implementations of repository interfaces

**Dependencies**: Domain layer interfaces

**Files**:
- `data/local/Database.kt` - Room database, entities, and DAOs
- `data/repository/RepositoryImpl.kt` - All repository implementations

### 🎨 Presentation Layer
**Location**: `app/src/main/java/com/aihos/selfevolving/presentation/`

**Purpose**: UI and user interaction

**Components**:
- **Activities**: MainActivity
- **ViewModels**: State management and business logic coordination
- **Compose Screens**: UI components
- **Theme**: Material Design theme

**Dependencies**: Domain layer (use cases)

**Files**:
- `presentation/MainActivity.kt` - Main activity
- `presentation/home/AiHosViewModel.kt` - ViewModel
- `presentation/home/AiHosScreen.kt` - Compose UI
- `presentation/theme/` - Theme files

### 💉 Dependency Injection
**Location**: `app/src/main/java/com/aihos/selfevolving/di/`

**Purpose**: Provide dependencies using Hilt

**Components**:
- **DatabaseModule**: Provides database and DAOs
- **RepositoryModule**: Provides repository implementations

**Files**:
- `di/AppModule.kt` - All Hilt modules

## Key Files Explained

### Core Application
```
📄 AiHosApplication.kt
   - Hilt initialization
   - Application-level setup
```

### Domain Models
```
📄 domain/model/Models.kt
   - Memory (5 types)
   - ReasoningContext (inference engine)
   - ReflectionEntry (self-analysis)
   - EvolutionState (growth tracking)
   - AutonomousTask (autonomous operations)
   - AiState (system status)
```

### Repository Pattern
```
📄 domain/repository/Repositories.kt (Interfaces)
   ↓ Implemented by
📄 data/repository/RepositoryImpl.kt
   ↓ Uses
📄 data/local/Database.kt (Room)
```

### Use Cases
```
📄 domain/usecase/UseCases.kt
   - Memory operations
   - Reasoning logic
   - Reflection algorithms
   - Evolution mechanics
   - Autonomy management
```

### UI Architecture
```
📄 presentation/MainActivity.kt
   ↓ Hosts
📄 presentation/home/AiHosScreen.kt
   ↑ Observes
📄 presentation/home/AiHosViewModel.kt
   ↓ Calls
📄 domain/usecase/UseCases.kt
```

## Data Flow

### Storing a Memory
```
User Input
   ↓
AiHosScreen (Compose)
   ↓
AiHosViewModel
   ↓
StoreMemoryUseCase
   ↓
MemoryRepository (Interface)
   ↓
MemoryRepositoryImpl
   ↓
MemoryDao
   ↓
Room Database (SQLite)
```

### Performing Reasoning
```
Query
   ↓
PerformReasoningUseCase
   ├→ Retrieve Memories (MemoryRepository)
   ├→ Perform Inference (Business Logic)
   ├→ Store Context (ReasoningRepository)
   └→ Evolve Capability (EvolutionRepository)
```

## Technology Stack by Layer

### Domain Layer
- Pure Kotlin
- Coroutines (Flow, suspend functions)

### Data Layer
- Room Database
- Kotlin Coroutines
- Gson (JSON serialization)

### Presentation Layer
- Jetpack Compose
- Material 3
- ViewModel
- StateFlow

### Dependency Injection
- Hilt (Dagger)

## Configuration Files

### Gradle
```
📄 build.gradle.kts (root)
   - Plugin versions
   - Repository configuration

📄 app/build.gradle.kts
   - Dependencies
   - Build configuration
   - Compose setup

📄 settings.gradle.kts
   - Module inclusion
   - Repository configuration

📄 gradle.properties
   - Gradle JVM args
   - Android configuration
```

### Resources
```
📄 app/src/main/res/values/strings.xml
   - String resources

📄 app/src/main/res/values/themes.xml
   - Theme definitions

📄 app/src/main/AndroidManifest.xml
   - App configuration
   - Permissions
   - Activities
```

## Documentation Structure

```
📄 README.md
   - Project overview
   - Features
   - Installation
   - Usage guide

📄 ARCHITECTURE.md
   - Technical architecture
   - Design patterns
   - Implementation details

📄 EVOLUTION_EXPLAINED.md
   - Evolution mechanics
   - Learning process
   - Examples

📄 DEVELOPMENT.md
   - Setup guide
   - Contribution guidelines
   - Code examples

📄 QUICKSTART.md
   - Quick setup
   - First steps
   - Troubleshooting

📄 PROJECT_STRUCTURE.md (this file)
   - Directory structure
   - File organization
   - Data flow
```

## Build Artifacts (Not in Git)

```
📁 .gradle/              # Gradle cache
📁 build/                # Build outputs
📁 app/build/            # App build outputs
📁 .idea/                # Android Studio settings
📄 local.properties      # Local SDK paths
```

## Adding New Features

### New Domain Model
1. Add to `domain/model/Models.kt`
2. Create repository interface in `domain/repository/Repositories.kt`
3. Create use case in `domain/usecase/UseCases.kt`

### New Database Entity
1. Add entity to `data/local/Database.kt`
2. Create DAO in same file
3. Add to `@Database` entities list
4. Increment database version
5. Implement repository in `data/repository/RepositoryImpl.kt`

### New UI Screen
1. Create ViewModel in `presentation/newscreen/`
2. Create Compose screen in same directory
3. Add to navigation (if needed)

### New Dependency
1. Add to `app/build.gradle.kts`
2. Sync Gradle
3. Add to DI module if needed

## Clean Architecture Benefits

✅ **Separation of Concerns**: Each layer has clear responsibility
✅ **Testability**: Domain layer can be tested without Android
✅ **Maintainability**: Changes localized to specific layers
✅ **Scalability**: Easy to add new features
✅ **Flexibility**: Can swap implementations (e.g., different databases)

## Next Steps

- Read [ARCHITECTURE.md](ARCHITECTURE.md) for detailed architecture
- See [DEVELOPMENT.md](DEVELOPMENT.md) for contribution guide
- Check [EVOLUTION_EXPLAINED.md](EVOLUTION_EXPLAINED.md) for AI mechanics
- Start with [QUICKSTART.md](QUICKSTART.md) to run the app

---

**Note**: This structure follows Android and Clean Architecture best practices, ensuring maintainability, testability, and scalability.
