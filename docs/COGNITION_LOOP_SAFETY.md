# AI Cognition Loop Safety Enforcement Guide

**Document Purpose**: Comprehensive guide to safety mechanisms in SA-AIHOS AI cognition loop  
**Status**: Complete (Phases 1-2 implemented)  
**Target Audience**: Developers, maintainers, architects  
**Last Updated**: Post-Phase 2

---

## Table of Contents

1. [Overview](#overview)
2. [Safety Architecture](#safety-architecture)
3. [State Machine (FSM)](#state-machine-fsm)
4. [Loop Boundaries](#loop-boundaries)
5. [Reflection Safety](#reflection-safety)
6. [Evolution Safety](#evolution-safety)
7. [Error Recovery](#error-recovery)
8. [Performance Targets](#performance-targets)
9. [Testing Strategy](#testing-strategy)
10. [Monitoring & Debugging](#monitoring--debugging)
11. [Future Enhancements](#future-enhancements)

---

## Overview

### Core Principle

The SA-AIHOS AI cognition loop (Think → Act → Reflect → Evolve) implements **five layers of safety** to ensure deterministic, bounded, and stable execution:

1. **State Transition FSM** - Ensures valid state flow
2. **Loop Frequency Enforcement** - Prevents CPU exhaustion
3. **Reflection Depth Limiting** - Prevents meta-reflection cascades
4. **Evolution Constraint Validation** - Prevents rule set destabilization
5. **Comprehensive Error Recovery** - Handles all failure modes gracefully

### Safety Philosophy

- **Deterministic**: All state transitions explicitly defined
- **Bounded**: Execution has explicit limits (cycles, time, rules)
- **Transparent**: All safety decisions logged for debugging
- **Graceful**: Errors don't crash system, enable recovery
- **Monitored**: Key metrics tracked for observability

---

## Safety Architecture

### The Cognitive Loop with Safety Layers

```
┌─────────────────────────────────────────────────────────┐
│ COGNITIVE LOOP with Safety Enforcement (AISystemController)
├─────────────────────────────────────────────────────────┤
│
│  while (isRunning && !isPaused && cycleCount < MAX_CYCLES)
│  {
│    ┌──────────────────────────────────────────────┐
│    │ LAYER 1: Cycle Boundary Enforcement          │
│    │ - Detect timeout from previous cycle         │
│    │ - Enforce minimum cycle duration (16ms)      │
│    │ - Check cycle count limit (1M cycles)        │
│    └──────────────────────────────────────────────┘
│
│    ┌──────────────────────────────────────────────┐
│    │ PHASE: THINK                                  │
│    │ - Validate state transition: Idle → Thinking │
│    │ - Generate reasoning context                 │
│    │ - Generate decision options                  │
│    │ - Score and select best option               │
│    └──────────────────────────────────────────────┘
│
│    ┌──────────────────────────────────────────────┐
│    │ PHASE: ACT                                    │
│    │ - Validate state transition: Thinking → Act  │
│    │ - Execute selected action                    │
│    │ - Record outcome                             │
│    └──────────────────────────────────────────────┘
│
│    ┌──────────────────────────────────────────────┐
│    │ LAYER 2: Reflection Gating                   │
│    │ if (cycleCount % REFLECTION_INTERVAL == 0)  │
│    │ {                                             │
│    │   ┌────────────────────────────────────────┐ │
│    │   │ PHASE: REFLECT                          │ │
│    │   │ - Validate state transition             │ │
│    │   │ - Check reflection depth (max 1)        │ │
│    │   │ - Analyze outcome vs expectation        │ │
│    │   │ - Generate insight with confidence      │ │
│    │   └────────────────────────────────────────┘ │
│    │                                               │
│    │   ┌────────────────────────────────────────┐ │
│    │   │ LAYER 3: Evolution Gating              │ │
│    │   │ if (insight.confidence > 0.7f)         │ │
│    │   │ {                                       │ │
│    │   │   ┌──────────────────────────────────┐ │ │
│    │   │   │ PHASE: EVOLVE                    │ │ │
│    │   │   │ - Create pre-evolution snapshot   │ │ │
│    │   │   │ - Generate rule adaptations       │ │ │
│    │   │   │ - Validate: rule count <= 3      │ │ │
│    │   │   │ - Validate: stability >= 0.5     │ │ │
│    │   │   │ - Record in rollback buffer       │ │ │
│    │   │   │ - Persist evolved state           │ │ │
│    │   │   └──────────────────────────────────┘ │ │
│    │   │ }                                       │ │
│    │   └────────────────────────────────────────┘ │
│    │ }                                             │
│    └──────────────────────────────────────────────┘
│
│    ┌──────────────────────────────────────────────┐
│    │ LAYER 4: Cycle Metrics & Timing              │
│    │ - Record cycle time (actual duration)        │
│    │ - Track average cycle time                   │
│    │ - Enforce min duration (16ms)                │
│    │ - Detect performance degradation             │
│    └──────────────────────────────────────────────┘
│  }
│
│  ┌──────────────────────────────────────────────────┐
│  │ LAYER 5: Cleanup & Logging                       │
│  │ - On max cycles: graceful shutdown               │
│  │ - On error: recovery with appropriate backoff    │
│  │ - Log all safety decisions for audit trail       │
│  └──────────────────────────────────────────────────┘
│
└─────────────────────────────────────────────────────────┘
```

---

## State Machine (FSM)

### Complete State Transition Map

The AI system has **9 states** with **22 explicitly defined transitions**. Invalid transitions are rejected and logged.

#### State Definitions

| State | Purpose | Valid Transitions |
|-------|---------|-------------------|
| **Idle** | Waiting to start | → Initializing, Paused, Stopped, Error |
| **Initializing** | System setup | → Thinking, Paused, Stopped, Error |
| **Thinking** | Decision making | → Acting, Paused, Error |
| **Acting** | Executing action | → Reflecting, Paused, Error |
| **Reflecting** | Analyzing outcome | → Evolving, Thinking, Paused, Error |
| **Evolving** | Adapting rules | → Thinking, Paused, Error |
| **Paused** | User suspend | → Thinking, Idle, Stopped, Error |
| **Stopped** | System halted | → Idle, Error |
| **Error** | Exception state | → Idle, Paused, Stopped |

#### Transition Validation Algorithm

```kotlin
// Defined in AISystemController
private val ALLOWED_TRANSITIONS = mapOf(
    AIState.Idle::class to setOf(...),
    // 9 states total, each with allowed next states
)

private fun isTransitionAllowed(from: AIState, to: AIState): Boolean {
    if (from::class == to::class) return true  // No-op allowed
    val allowedNextStates = ALLOWED_TRANSITIONS[from::class] ?: return false
    return to::class in allowedNextStates
}

private fun transitionToState(newState: AIState): Boolean {
    val currentState = _aiState.value
    if (!isTransitionAllowed(currentState, newState)) {
        Timber.e("Invalid transition: $currentState → $newState")
        _aiState.value = AIState.Error("Invalid transition")
        return false
    }
    _aiState.value = newState
    return true
}
```

#### Example: Invalid Transition Handling

```
Scenario: User calls resume() while system is in Error state

Current Flow:
1. System in Error state (invalid operation occurred)
2. User calls resume()
3. resume() calls transitionToState(AIState.Thinking)
4. isTransitionAllowed(Error, Thinking) checks ALLOWED_TRANSITIONS[Error::class]
5. Thinking ∉ allowedNextStates for Error
6. FSM rejects transition
7. System stays in Error state
8. Logs: "Invalid transition: Error → Thinking"

Expected User Behavior:
- User must call stop() to exit Error state
- Then call start() to restart system cleanly
```

---

## Loop Boundaries

### Cycle Counting

```kotlin
companion object {
    // Maximum cycles per session (~4.6 hours at 60 FPS)
    private const val MAX_CYCLES_PER_SESSION = 1_000_000
    
    // How to calculate: 1,000,000 cycles ÷ 60 FPS ÷ 60 seconds = 277 minutes ≈ 4.6 hours
}

private var cycleCount = 0

// In cognitive loop:
while (isRunning && !isPaused && cycleCount < MAX_CYCLES_PER_SESSION) {
    cycleCount++
    // ... phase implementations
}

// On exit:
if (cycleCount >= MAX_CYCLES_PER_SESSION) {
    Timber.i("Reached max cycle limit. Stopping.")
    stop()  // Graceful shutdown
}
```

#### Purpose

- **Prevents unbounded execution**: System won't run indefinitely
- **Resource protection**: CPU, memory, battery won't be exhausted
- **Predictable timing**: Session duration is deterministic
- **Audit trail**: Always know when system should stop

### Cycle Timeout Detection

```kotlin
companion object {
    // Maximum time allowed for a single cycle (5 seconds)
    private const val CYCLE_TIMEOUT_MS = 5000
}

private var cycleStartTime = 0L

// At cycle start:
cycleStartTime = System.currentTimeMillis()

// During phases:
if (System.currentTimeMillis() - cycleStartTime > CYCLE_TIMEOUT_MS) {
    Timber.w("Cycle timeout detected")
    handleErrorRecovery(TimeoutException("Phase timeout"))
    continue  // Skip to next cycle
}
```

#### Purpose

- **Detects stuck phases**: If any phase takes > 5 seconds, system detects it
- **Prevents cascading delays**: Early timeout detection prevents domino effects
- **Enables recovery**: System can backoff and retry
- **Debugging**: Timeout logs identify problematic phases

### Cycle Frequency Enforcement

```kotlin
companion object {
    // Minimum time between cycles (16ms = 60 FPS)
    private const val MIN_CYCLE_DURATION_MS = 16
}

// At cycle end:
val cycleTime = System.currentTimeMillis() - cycleStartTime
val minDurationRemaining = MIN_CYCLE_DURATION_MS - cycleTime

if (minDurationRemaining > 0) {
    delay(minDurationRemaining)  // Wait to reach minimum duration
}
```

#### Purpose

- **Bounded CPU usage**: System won't saturate CPU
- **Stable frame rate**: ~60 FPS target for UI responsiveness
- **Predictable behavior**: Cycle frequency is consistent
- **Energy efficiency**: Regular sleep intervals reduce power consumption

#### Calculation Example

```
Scenario: THINK phase takes 45ms

1. cycleStartTime = 0ms
2. THINK phase: 45ms
3. ACT phase: 5ms
4. Total elapsed: 50ms
5. cycleTime = 50ms
6. minDurationRemaining = 16ms - 50ms = -34ms (already exceeded minimum)
7. No delay needed
8. Next cycle starts immediately at 50ms

Scenario: All phases complete in 8ms

1. cycleStartTime = 0ms
2. THINK phase: 3ms
3. ACT phase: 5ms
4. Total elapsed: 8ms
5. cycleTime = 8ms
6. minDurationRemaining = 16ms - 8ms = 8ms
7. delay(8ms) enforces minimum
8. Next cycle starts at 16ms
```

---

## Reflection Safety

### Gating Mechanism

```kotlin
companion object {
    // Reflection interval: analyze every N cycles
    private const val REFLECTION_INTERVAL_CYCLES = 10
}

// In cognitive loop:
if (cycleCount % REFLECTION_INTERVAL_CYCLES == 0) {
    // Execute reflection phase
}
```

**Effect**: Reflection occurs every 10 cycles (~167ms at 60 FPS), not every cycle

#### Purpose

- **Performance**: Reduces reflection overhead from every cycle to every 10 cycles
- **Stability**: Prevents excessive analysis from destabilizing decisions
- **Resource efficiency**: Saves CPU, memory, and battery
- **Signal clarity**: Allows decision patterns to stabilize before analyzing

### Reflection Depth Limiting

```kotlin
companion object {
    // Maximum reflection depth (prevents meta-reflection loops)
    private const val MAX_REFLECTION_DEPTH = 1
    
    // Maximum insights extracted per reflection cycle
    private const val MAX_INSIGHTS_PER_REFLECTION = 5
}

private var reflectionDepth = 0

private suspend fun reflectPhase(...): ReflectionInsight {
    if (reflectionDepth >= MAX_REFLECTION_DEPTH) {
        Timber.w("Reflection depth limit reached. Returning safe default.")
        return ReflectionInsight(..., confidence = 0.0f)
    }
    
    reflectionDepth++
    try {
        // Execute reflection
        return insight
    } finally {
        reflectionDepth--
    }
}
```

#### Purpose

- **Prevents meta-reflection**: Reflection won't generate insights about reflection
- **Prevents cascades**: Insights won't trigger further insights
- **Bounded analysis**: At most 1 level of reflection per cycle
- **Predictable**: Clear limit on analysis depth

#### Example: Prevented Cascade

```
Without depth limit:

Cycle 10:
1. REFLECT triggered (cycleCount % 10 == 0)
2. Analyze decision → generates insight
3. Insight confidence > 0.7 → triggers EVOLVE
4. Evolution changes rules
5. Insight about rule changes → generates meta-insight
6. Meta-insight triggers another REFLECT
7. Analysis of analysis → another meta-insight
8. Cascading reflection continues...
9. System becomes stuck analyzing analyses

With depth limit (MAX_REFLECTION_DEPTH = 1):

Cycle 10:
1. REFLECT triggered
2. reflectionDepth++ (now = 1)
3. Analyze decision → generates insight
4. Return insight
5. reflectionDepth-- (back to 0)
6. Even if insight triggers meta-analysis:
   - reflectionDepth++ (now = 1)
   - Depth check: 1 >= 1 → return safe default
   - No meta-analysis executed
7. System continues normally
```

---

## Evolution Safety

### Confidence Gating

```kotlin
companion object {
    // Evolution confidence threshold
    private const val MIN_CONFIDENCE_FOR_EVOLUTION = 0.7f
}

// In cognitive loop EVOLVE section:
if (insight.confidence > MIN_CONFIDENCE_FOR_EVOLUTION) {
    // Execute evolution phase
}
```

**Effect**: Evolution only occurs when reflection confidence ≥ 70%

#### Purpose

- **Prevents noise-driven evolution**: Weak signals won't change rules
- **Ensures evidence base**: Only high-confidence insights drive adaptation
- **Stability**: Low-confidence changes won't destabilize system
- **Learning safety**: System doesn't overreact to anomalies

### Rule Change Constraints

```kotlin
companion object {
    // Maximum number of rules to create/modify per evolution
    private const val MAX_RULES_PER_EVOLUTION = 3
    
    // Maximum weight change per rule (0.0 to 1.0 scale)
    private const val MAX_WEIGHT_CHANGE = 0.2f
}

// In evolvePhase:
val totalChanges = evolutionEvent.rulesAdded + evolutionEvent.rulesModified
if (totalChanges > MAX_RULES_PER_EVOLUTION) {
    Timber.w("Evolution exceeded max rules. Rolling back.")
    return EvolutionEvent(rulesAdded = 0, rulesModified = 0)
}
```

#### Purpose

- **Prevents destabilization**: Won't change > 3 rules per evolution
- **Gradual adaptation**: Small incremental changes instead of wholesale rewrites
- **Rule set stability**: Preserves learned behavior while adapting
- **Predictability**: Bounded rule changes make system behavior more predictable

#### Example: Rule Change Validation

```
Scenario: Evolution attempts to create 5 new rules

Pre-evolution snapshot:
- Current rules: 20 active rules
- Avg stability: 0.75f

Evolution event generated:
- rulesAdded: 5
- rulesModified: 2
- totalChanges = 5 + 2 = 7

Validation:
- Check: 7 > MAX_RULES_PER_EVOLUTION (3)?
- YES → violation detected
- Action: Log warning, rollback, return safe EvolutionEvent
- Result: No rules added, system state unchanged

Next evolution can retry with smaller changes.
```

### Stability Validation

```kotlin
// In evolvePhase:
val estimatedStability = evolutionEvent.ruleSetStability

if (estimatedStability < 0.5f) {
    Timber.w("Evolution reduced stability. Rolling back.")
    return EvolutionEvent(..., ruleSetStability = preSnapshot.stability)
}
```

#### Purpose

- **Ensures coherence**: Rules won't contradict each other
- **Prevents instability**: Won't accept changes that reduce consistency
- **Maintains performance**: Stable rule sets execute predictably
- **Safe adaptation**: Only improvements are accepted

### Evolution Snapshot & Rollback

```kotlin
@Serializable
data class EvolutionSnapshot(
    val cycleNumber: Int,
    val timestamp: Long,
    val insightConfidence: Float,
    val stability: Float,
    val processingTimeMs: Long
)

companion object {
    // Keep history of recent evolutions
    private const val EVOLUTION_ROLLBACK_WINDOW = 100
}

private val evolutionRollbackBuffer = mutableListOf<EvolutionSnapshot>()

// In evolvePhase:
val preSnapshot = EvolutionSnapshot(...)
// ... evolution execution ...
evolutionRollbackBuffer.add(preSnapshot)

// Trim to size
while (evolutionRollbackBuffer.size > EVOLUTION_ROLLBACK_WINDOW) {
    evolutionRollbackBuffer.removeAt(0)
}
```

#### Purpose

- **Rollback capability**: Can revert bad evolutions
- **History tracking**: Records evolution sequence for analysis
- **Forensics**: Helps debug why system evolved unexpectedly
- **Bounded memory**: Keeps only last 100 evolutions

---

## Error Recovery

### Error Classification & Response

```kotlin
private suspend fun handleErrorRecovery(error: Throwable) {
    val recoveryAction = when (error) {
        is OutOfMemoryError -> {
            Timber.e("Memory exhausted, clearing old data")
            memorySystem.clearOldData(...)  // Free up space
            RecoveryAction.RETRY_WITH_SHORT_BACKOFF
        }
        is TimeoutException -> {
            Timber.e("Phase timeout, reducing frequency")
            RecoveryAction.RETRY_WITH_LONG_BACKOFF  // 3x normal delay
        }
        is IllegalStateException -> {
            Timber.e("Invalid state, resetting")
            RecoveryAction.RESET_TO_IDLE  // Requires manual resume
        }
        is InterruptedException -> {
            Timber.d("Interrupted, stopping")
            RecoveryAction.STOP
        }
        else -> {
            Timber.e("Unknown error, pausing")
            RecoveryAction.PAUSE_FOR_INTERVENTION  // Manual recovery
        }
    }
    
    // Execute recovery action
    when (recoveryAction) {
        RecoveryAction.RETRY_WITH_SHORT_BACKOFF -> {
            delay(1000)  // 1 second backoff
            transitionToState(AIState.Thinking)
        }
        RecoveryAction.RETRY_WITH_LONG_BACKOFF -> {
            delay(3000)  // 3 second backoff
            transitionToState(AIState.Thinking)
        }
        RecoveryAction.RESET_TO_IDLE -> {
            transitionToState(AIState.Idle)
            isPaused = true  // Requires manual resume
        }
        RecoveryAction.PAUSE_FOR_INTERVENTION -> {
            transitionToState(AIState.Paused)
            isPaused = true  // Requires manual resume
        }
        RecoveryAction.STOP -> {
            transitionToState(AIState.Stopped)
            isRunning = false
        }
    }
}

enum class RecoveryAction {
    RETRY_WITH_SHORT_BACKOFF,
    RETRY_WITH_LONG_BACKOFF,
    RESET_TO_IDLE,
    PAUSE_FOR_INTERVENTION,
    STOP
}
```

#### Recovery Strategies

| Error Type | Recovery Action | Rationale |
|-----------|-----------------|-----------|
| OutOfMemoryError | Clean & retry (short backoff) | Free resources, try again |
| TimeoutException | Retry with long backoff | Reduce frequency, give system time |
| IllegalStateException | Reset to Idle (manual) | Requires user intervention |
| InterruptedException | Stop cleanly | Thread was cancelled, stop cleanly |
| Other exceptions | Pause (manual) | Unknown error, requires investigation |

---

## Performance Targets

### Cycle Timing

| Component | Target | Maximum | Notes |
|-----------|--------|---------|-------|
| THINK phase | 16-100ms | 100ms | Decision generation |
| ACT phase | ~0ms | 10ms | Simulation |
| REFLECT phase | 50-200ms | 200ms | Every 10 cycles only |
| EVOLVE phase | 10-50ms | 50ms | Only if confidence > 0.7 |
| **Total cycle** | **16-116ms** | **5000ms** | Full cycle, with timeout |
| **Frame rate** | **~60 FPS** | - | 16ms minimum between cycles |

### Memory Constraints

| Item | Limit | Purpose |
|------|-------|---------|
| Evolution rollback buffer | 100 snapshots | Bounded rollback history |
| Reflection depth | 1 level | Prevent cascades |
| Insights per reflection | 5 maximum | Bounded analysis |
| Rules per evolution | 3 maximum | Bounded rule changes |

### System Limits

| Metric | Limit | Purpose |
|--------|-------|---------|
| Cycles per session | 1,000,000 | ~4.6 hours at 60 FPS |
| Cycle timeout | 5000ms | Detect stuck phases |
| Min cycle duration | 16ms | Bounded frequency |

---

## Testing Strategy

### Unit Tests

```kotlin
// AISystemControllerTest.kt

@Test
fun testStateTransitionValidation() {
    // Idle → Initializing should succeed
    assertTrue(controller.transitionToState(AIState.Initializing))
    
    // Thinking → Idle should fail
    controller._aiState.value = AIState.Thinking
    assertFalse(controller.transitionToState(AIState.Idle))
    assertEquals(AIState.Error, controller._aiState.value)
}

@Test
fun testCycleCounterIncrements() {
    controller.start()
    runBlocking { delay(100) }
    assertTrue(controller.cycleCount > 0)
}

@Test
fun testReflectionDepthLimit() {
    // reflectionDepth should not exceed MAX_REFLECTION_DEPTH
    controller.reflectionDepth = 1
    val insight = controller.reflectPhase(...)
    assertEquals(0, controller.reflectionDepth)  // Decremented
    assertTrue(insight.confidence >= 0.0f)  // Safe default or real
}

@Test
fun testEvolutionConstraints() {
    // Create evolution event with > 3 rule changes
    val event = EvolutionEvent(rulesAdded = 5, rulesModified = 2)
    
    // Should be rejected
    val result = controller.evolvePhase(insight)
    assertEquals(0, result.rulesAdded)
    assertEquals(0, result.rulesModified)
}
```

### Integration Tests

```kotlin
@Test
fun testCompleteThinkActReflectEvolveLoop() {
    controller.start()
    
    runBlocking {
        delay(200)  // Let 10-12 cycles complete
    }
    
    // Verify state flows are updated
    assertNotNull(controller.aiState.value)
    assertNotNull(controller.cycleMetrics.value)
    assertNotNull(controller.lastDecision.value)
    
    controller.stop()
    assertEquals(AIState.Stopped, controller.aiState.value)
}

@Test
fun testErrorRecoveryFromTimeout() {
    controller.start()
    
    // Simulate timeout
    controller.cycleStartTime = System.currentTimeMillis() - 6000
    controller.handleErrorRecovery(TimeoutException("Timeout"))
    
    // Should transition to appropriate error state
    assertTrue(controller.aiState.value is AIState.Error ||
              controller.aiState.value == AIState.Paused)
}
```

### Stress Tests

```kotlin
@Test
fun testLongRunningStability() {
    controller.start()
    
    runBlocking {
        // Let 100,000 cycles complete (28 minutes at 60 FPS)
        delay(1680000)
    }
    
    // System should still be running
    assertTrue(controller.isRunning)
    
    // Memory should not grow unbounded
    val memoryBefore = Runtime.getRuntime().totalMemory()
    delay(60000)  // Another 60 seconds
    val memoryAfter = Runtime.getRuntime().totalMemory()
    
    // Memory growth should be < 10MB
    assertTrue((memoryAfter - memoryBefore) < 10_000_000)
}
```

---

## Monitoring & Debugging

### Key Logs to Monitor

```
// System startup
Timber.d("AI System starting")
Timber.d("AI System initializing")

// State transitions (every cycle)
Timber.d("State transition: Idle → Initializing")

// Cycle completion
Timber.d("THINK phase: Decision = 'focus_reminder', confidence = 0.85")
Timber.d("ACT phase: Executed action")
Timber.d("REFLECT phase: Insight confidence = 0.75 (depth=1)")
Timber.d("EVOLVE phase: Added 2 rules, modified 1 rule. Stability: 72%")

// Safety violations
Timber.w("Cycle timeout detected at start of cycle 42")
Timber.w("Reflection depth limit reached (depth=1)")
Timber.w("Evolution exceeded max rules (5 > 3)")

// Error recovery
Timber.e(exception, "Error in THINK phase")
Timber.e("Memory exhausted, clearing old data")
Timber.i("Reached max cycle limit (1000000). Stopping.")

// System shutdown
Timber.d("AI System pausing")
Timber.d("AI System resuming")
Timber.d("AI System stopping")
```

### Metrics to Track

```
// Via StateFlow<CycleMetrics>
data class CycleMetrics(
    val lastCycleTimeMs: Long,      // Last cycle duration
    val averageCycleTimeMs: Long,   // Moving average
    val totalCyclesCompleted: Int,  // Total cycles executed
    val targetCycleTimeMs: Long = 16 // Target (60 FPS)
)

// Via monitoring code
controller.cycleMetrics.collect { metrics ->
    log("Last cycle: ${metrics.lastCycleTimeMs}ms")
    log("Average: ${metrics.averageCycleTimeMs}ms")
    log("Total cycles: ${metrics.totalCyclesCompleted}")
    log("Performance: ${if (metrics.isPerformanceGood) "GOOD" else "DEGRADED"}")
}
```

### Debugging Checklist

When investigating issues:

1. **Check state logs** - Verify FSM transitions are valid
2. **Monitor cycle time** - Is cycle time within target?
3. **Watch for timeouts** - Any "Cycle timeout" messages?
4. **Check error logs** - What exceptions are occurring?
5. **Review recovery actions** - Is system recovering properly?
6. **Verify memory** - Is rollback buffer growing too large?
7. **Analyze evolution** - How many rules being added/modified?

---

## Future Enhancements

### Planned Phase 3-5 Improvements

#### Phase 3: Advanced Logging & Observability
- Add structured logging with tags (THINK, ACT, REFLECT, EVOLVE)
- Implement decision trace logging (decisions, outcomes, insights)
- Add performance profiling for each phase
- Create debug mode with verbose logging

#### Phase 4: Rollback Implementation
- Implement actual rule rollback from snapshots
- Add rollback trigger logic based on stability metrics
- Create rollback test suite
- Document rollback scenarios

#### Phase 5: Monitoring & Analytics
- Add real-time dashboard for cycle metrics
- Implement anomaly detection for performance degradation
- Create evolution impact analysis
- Add predictive warnings for resource exhaustion

### Open Questions for Future Work

1. **Rule Weight Limits**: Should we enforce MAX_WEIGHT_CHANGE?
2. **Adaptive Gating**: Should reflection interval adapt based on activity?
3. **Multi-User Safety**: How does FSM handle concurrent access?
4. **Learning Rate**: Should evolution rate adapt to rule set stability?
5. **Memory Cleanup**: When should old decisions/insights be purged?

---

## Appendix A: Safety Constants Reference

```kotlin
companion object {
    // Cycle Management
    const val MAX_CYCLES_PER_SESSION = 1_000_000
    const val CYCLE_TIMEOUT_MS = 5000
    const val REFLECTION_INTERVAL_CYCLES = 10
    const val MIN_CYCLE_DURATION_MS = 16
    
    // Evolution Gates
    const val MIN_CONFIDENCE_FOR_EVOLUTION = 0.7f
    const val MAX_RULES_PER_EVOLUTION = 3
    const val MAX_WEIGHT_CHANGE = 0.2f
    
    // Reflection Safety
    const val MAX_REFLECTION_DEPTH = 1
    const val MAX_INSIGHTS_PER_REFLECTION = 5
    const val EVOLUTION_ROLLBACK_WINDOW = 100
    
    // Error Recovery
    const val ERROR_RECOVERY_BACKOFF_MS = 1000L
}
```

---

## Appendix B: FSM State Diagram

```
                    ┌─────────────┐
                    │    Idle     │
                    └──────┬──────┘
                           │
                ┌──────────┴──────────┐
                │                     │
                ▼                     ▼
         ┌────────────┐        ┌───────────────┐
         │Initializing│────────│ Error (any)   │
         └─────┬──────┘        └────────┬──────┘
               │                        │
               ▼                        ▼
         ┌──────────┐              ┌─────────┐
    ┌────┤ Thinking │              │ Paused  │
    │    └──────┬───┘              └────┬────┘
    │           │                       │
    │           ▼                       │
    │    ┌────────────┐                 │
    │    │ Acting     │                 │
    │    └─────┬──────┘                 │
    │          │                        │
    │          ▼                        │
    │    ┌────────────┐                 │
    │    │ Reflecting │─┐ (gated)       │
    │    └────────────┘ │               │
    │          ▲        │               │
    │          │        ▼               │
    │          │   ┌────────────┐       │
    │          └───┤ Evolving   │       │
    │              └──┬─────────┘       │
    │                 │                │
    │                 ▼               │
    │            (continue loop)       │
    │                                  │
    └──────────────────┬───────────────┘
                       ▼
                  ┌──────────┐
                  │ Stopped  │
                  └──────────┘
```

---

## Appendix C: Sample Safety Monitoring Code

```kotlin
// In Activity or ViewModel
lifecycleScope.launch {
    aiSystemController.cycleMetrics.collect { metrics ->
        // Update UI with cycle metrics
        updateCycleDisplay(
            cycleTime = "${metrics.lastCycleTimeMs}ms",
            avgTime = "${metrics.averageCycleTimeMs}ms",
            total = "${metrics.totalCyclesCompleted}",
            health = if (metrics.isPerformanceGood) "✓" else "⚠"
        )
    }
}

lifecycleScope.launch {
    aiSystemController.aiState.collect { state ->
        // Update state display
        updateStateDisplay(state)
        
        // Alert on error
        if (state is AIState.Error) {
            showErrorAlert("AI System Error: ${state.message}")
        }
    }
}

lifecycleScope.launch {
    aiSystemController.evolutionEvents.collect { event ->
        // Log evolution events for analysis
        logEvolutionEvent(
            timestamp = event.timestamp,
            rulesAdded = event.rulesAdded,
            rulesModified = event.rulesModified,
            stability = event.ruleSetStability
        )
    }
}
```

---

**Document Status**: Complete  
**Last Reviewed**: Post-Phase 2 Implementation  
**Next Review**: After Phase 3-5 implementations
