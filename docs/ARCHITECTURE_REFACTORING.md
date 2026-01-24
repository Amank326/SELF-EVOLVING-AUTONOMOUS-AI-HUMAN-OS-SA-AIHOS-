# SA-AIHOS Clean Architecture Refactoring

## Overview

This document describes the refactored architecture of SA-AIHOS with emphasis on clean separation of concerns, clear layer boundaries, and improved testability.

---

## Architecture Layers

### 1. Domain Layer (`com.aihos.domain`)
**Purpose**: Pure business logic with NO Android dependencies.

**Packages**:
- `model/`: Data models representing domain concepts
  - `DecisionRecord`: A decision made by the AI
  - `DecisionOutcome`: The result of a decision
  - `LearnedRule`: A rule learned by the AI
  - `CognitiveState`: Current AI cognitive state
  - `ReflectionData`: Insights from reflection
  - `ExecutionPhase`, `ThermalState`: Enums

- `use_case/`: High-level business operations
  - `AIBrainUseCase`: Orchestrate the THINK-ACT-REFLECT-EVOLVE cycle
  - `DecisionGenerationUseCase`: Generate decision options
  - `ReflectionUseCase`: Reflect on outcomes
  - `EvolutionUseCase`: Learn and evolve rules
  - `MemoryUseCase`: Query and store memory

**Benefits**:
- Can be unit tested without Android
- No coupling to UI or system APIs
- Reusable across different presentation layers (Android, web, CLI, etc.)
- Clear business contracts via interfaces

---

### 2. System Layer (`com.aihos.system`)
**Purpose**: Device integration and constraint management.

**Packages**:
- `signals/`: Observable device signals
  - `Signal<T>`: Base interface for any observable signal
  - `BatterySignal`, `TemperatureSignal`, etc.: Specific signals
  - `DeviceContext`: Aggregated device state
  - `SignalCollector`: Collects and aggregates all signals

- `energy/`: Energy and thermal management
  - `EnergyManager`: Monitors battery, provides constraints
  - `ThermalManager`: Monitors temperature, provides constraints
  - `ConstraintManager`: Aggregates constraints and provides recommendations
  - `EnergyState`, `ThermalState`: State enums

**Benefits**:
- Signals are decoupled from concrete Android implementations
- Easy to mock for testing
- Clear responsibility boundaries
- Can swap signal implementations without affecting AI logic

---

### 3. Graphics Layer (`com.aihos.graphics`)
**Purpose**: Native 3D/AR rendering and visualization.

**Packages**:
- `rendering/`: Graphics interfaces
  - `RenderingEngine`: Low-level rendering (Filament, native)
  - `VisualizationMapper`: Maps cognitive state to visual parameters
  - `RenderingParameters`: What and how to render
  - `GraphicsAndroidBridge`: Android↔Graphics bridge
  - `RenderingQuality`: Enum for quality levels

**Benefits**:
- Rendering can be mocked or replaced
- Cognitive state is mapped to visual parameters consistently
- Quality adapts to device constraints
- No rendering logic in UI layer

---

### 4. Data Layer (`com.aihos.data`)
**Purpose**: Persistence and data access.

**Packages**:
- `db/`: Room database
  - `SAIHOSDatabase`: Main database
  - `*Dao`: Data access objects
  - `*Entity`: Database entities

- `repository/`: Data access abstraction
  - `MemoryRepository`: Persists decisions and learning
  - Other repositories as needed

**Benefits**:
- Single source of truth for persistent data
- Easy to swap database implementations
- Repository pattern provides clean abstraction

---

### 5. UI Layer (`com.aihos.ui`)
**Purpose**: Android-specific presentation logic.

**Packages**:
- `viewmodel/`: View state management
  - `SAIHOSViewModel`: UI state ONLY (no AI logic)
  - Routes user actions to use cases
  - Exposes domain state as UI flows

- `screens/`: Jetpack Compose screens
  - `DashboardScreen`: Main dashboard
  - `MemoryScreen`: Memory visualization
  - Other screens
  - **Constraint**: No AI logic, no direct system access

- `components/`: Reusable UI components
  - Depend on ViewModels, not on domain

**Benefits**:
- Clean separation: UI renders, doesn't think
- ViewModels are thin and testable
- Easy to replace UI framework
- Clear data flow: Model → ViewModel → Screen

---

### 6. DI & Configuration (`com.aihos.di`)
**Purpose**: Dependency injection and wiring.

**Files**:
- `Module.kt`: Main Hilt module
  - Provides all use cases
  - Provides all system managers
  - Provides repositories
  - Single source of truth for object graph

**Benefits**:
- All wiring in one place
- Easy to swap implementations
- Clear object creation strategy

---

## Data Flow

### Typical User Interaction Flow

```
User Action (UI)
  ↓
Screen Component
  ↓
ViewModel.startAI() (routes action)
  ↓
AIBrainUseCase.start() (executes logic)
  ↓
DecisionGenerationUseCase.generateOptions() (domain logic)
  ↓
SignalCollector.deviceContext (gets system signals)
  ↓
Decision is made
  ↓
Rendered via VisualizationMapper → RenderingEngine
  ↓
UI observes updated CognitiveState
  ↓
Screen recomposes with new state
```

### State Flow Architecture

```
Domain Layer (Pure)
  └─ CognitiveState StateFlow
      └─ ViewModel (read-only)
          └─ Screen (observes)
              └─ UI Recomposes
```

**Key Principle**: State flows from domain → presentation, never backwards.

---

## Separation of Concerns

| Concern | Layer | Justification |
|---------|-------|---------------|
| AI Decision Making | Domain | Pure business logic, no Android |
| System Monitoring | System | Device-specific, decoupled from AI |
| Rendering | Graphics | Specialized domain, can be mocked |
| Data Persistence | Data | Clean abstraction, easy to swap |
| User Presentation | UI | Android-specific, thin |
| Object Creation | DI | Single responsibility, easy to test |

---

## Dependency Constraints (Allowed)

- UI → ViewModel ✅
- ViewModel → Use Cases ✅
- Use Cases → System (signals, energy, constraints) ✅
- Use Cases → Data (repositories) ✅
- Use Cases → Graphics ✅
- System → (no cross-layer dependencies) ✅
- Graphics → Domain (models) ✅
- Data → Domain (models) ✅

## Dependency Constraints (Forbidden)

- UI → Domain ❌ (UI should go through ViewModel)
- UI → System ❌ (UI should go through ViewModel)
- Domain → Android APIs ❌
- Domain → UI ❌
- System → UI ❌
- Data → UI ❌

---

## Testing Strategy

### Unit Tests (No Android)
- Domain models and use cases
- Interfaces can be mocked easily
- Run on JVM, fast

### Integration Tests
- Repositories with real Room database
- System managers with mocked sensors
- ViewModel with mocked use cases

### UI Tests
- Jetpack Compose previews
- Screenshot tests (optional)
- Navigation tests

### System Tests (Android)
- End-to-end flow on device
- Actual signal collection
- Graphics rendering verification

---

## Package Naming Convention

```
com.aihos
├── domain              # Pure business logic
│   ├── model          # Data classes
│   └── use_case       # Business operations
├── system             # Device integration
│   ├── signals        # Observable signals
│   └── energy         # Constraints
├── graphics           # Rendering
│   └── rendering      # Graphics interfaces
├── data               # Persistence
│   ├── db            # Room database
│   └── repository    # Data access
├── ui                 # Presentation
│   ├── viewmodel     # State management
│   ├── screens       # Compose screens
│   └── components    # UI pieces
└── di                 # Dependency injection
```

---

## Migration Path

The refactoring happens incrementally:

1. **Phase 1**: Create new clean layer interfaces (done)
2. **Phase 2**: Implement use cases using existing AI engines
3. **Phase 3**: Create system layer implementations for signals
4. **Phase 4**: Refactor ViewModel to use use cases
5. **Phase 5**: Move graphics to rendering layer
6. **Phase 6**: Update screens to use clean ViewModels
7. **Phase 7**: Deprecate old direct dependencies
8. **Phase 8**: Full migration complete, old code removed

---

## Benefits of This Architecture

✅ **Testability**: Pure domain logic can be tested without Android
✅ **Maintainability**: Clear responsibilities, easy to find code
✅ **Scalability**: Easy to add new features in appropriate layers
✅ **Reusability**: Domain logic can be used by different UIs
✅ **Flexibility**: Easy to swap implementations (e.g., different RenderingEngine)
✅ **Clarity**: Data flow is explicit and unidirectional
✅ **Decoupling**: Layers don't depend on lower layers' implementations

---

## Examples

### Adding a New Signal Type

1. Create interface in `system/signals/Signal.kt`
2. Implement in `system/signals/impl/ConcreteSignal.kt`
3. Register in DI Module
4. Use in `SignalCollector`
5. **Result**: No changes to domain or UI

### Replacing Rendering Engine

1. Create new `RenderingEngine` implementation
2. Register in DI Module
3. **Result**: All AI logic continues working unchanged

### Testing a Use Case

```kotlin
@Test
fun testDecisionGeneration() {
    // Mock dependencies
    val mockSignalCollector = mock<SignalCollector>()
    val mockMemoryUseCase = mock<MemoryUseCase>()
    
    // Create use case with mocks
    val useCase = DecisionGenerationUseCaseImpl(
        signalCollector = mockSignalCollector,
        memoryUseCase = mockMemoryUseCase
    )
    
    // Test
    val options = useCase.generateOptions("test context")
    assertTrue(options.isNotEmpty())
}
```

---

## Glossary

- **Use Case**: A high-level business operation (e.g., "Start AI")
- **Interactor**: Synonym for Use Case
- **Domain Model**: Data class representing a business concept
- **Repository**: Data access abstraction
- **Signal**: Observable value from the device
- **Constraint**: Limitation imposed by device state (energy, thermal)
- **ViewModel**: UI state manager, routes actions to use cases
- **Bridge**: Adapter between different systems (e.g., Kotlin↔JavaScript)

---

## References

- Clean Architecture (Robert C. Martin)
- MVVM Pattern
- Repository Pattern
- Dependency Injection (Hilt)
- Kotlin Coroutines & Flow
- Jetpack Compose

---

## Revision History

- **v1.0** (2026-01-24): Initial clean architecture design
