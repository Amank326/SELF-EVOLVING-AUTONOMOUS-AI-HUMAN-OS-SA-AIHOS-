# SA-AIHOS Architecture Explained

**A clear, comprehensive guide to how every part of SA-AIHOS works together.**

---

## Table of Contents
1. [System Overview](#system-overview)
2. [The Six Architectural Layers](#the-six-architectural-layers)
3. [The Cognition Loop](#the-cognition-loop)
4. [Perception & Context](#perception--context)
5. [Visualization Engine](#visualization-engine)
6. [Energy & Thermal Management](#energy--thermal-management)
7. [OS-Shell (Persistent Service)](#os-shell-persistent-service)
8. [Data Flow & Integration](#data-flow--integration)
9. [Key Design Decisions](#key-design-decisions)

---

## System Overview

SA-AIHOS is organized into **six architectural layers**, each with clear responsibilities:

```
┌────────────────────────────────────────────────────────────────┐
│  LAYER 1: User Interaction (UI, Overlay, Gesture)              │
├────────────────────────────────────────────────────────────────┤
│  LAYER 2: OS-Shell (Ambient AI, Persistent Service)            │
├────────────────────────────────────────────────────────────────┤
│  LAYER 3: Cognition Engine (Reasoning, Learning, Decisions)    │
├────────────────────────────────────────────────────────────────┤
│  LAYER 4: Perception & Visualization                           │
│         ├─ Perception (System Signals & Context)              │
│         └─ Visualization (3D Rendering)                        │
├────────────────────────────────────────────────────────────────┤
│  LAYER 5: Resource Management (Energy, Thermal)                │
├────────────────────────────────────────────────────────────────┤
│  LAYER 6: Android Platform (Battery, CPU, GPU, Lifecycle)      │
└────────────────────────────────────────────────────────────────┘
```

---

## The Six Architectural Layers

### Layer 1: User Interaction

**What it does**: Provides all touchpoints where users interact with the AI.

**Components**:

1. **MainActivity & UI Screens**
   - Displays the 3D visualization
   - Shows AI status and metrics
   - Settings and configuration
   - Direct visualization of reasoning

2. **AIShellOverlayManager**
   - Optional floating bubble always visible
   - Tap to interact, drag to move
   - Shows AI state at a glance
   - Minimal footprint (<1% CPU when idle)

3. **AIShellLauncher**
   - Quick-access interface
   - 6 context-aware quick actions
   - Suggestions based on device state
   - Voice/text query interface

4. **Touch & Gesture Handling**
   - Tap detection triggers AI introspection
   - Swipe gestures for navigation
   - Long-press for context menu
   - Gesture influences which rules AI introspects

**Connectivity to Layer 2**: UI requests → Intent/direct calls to OS-Shell → Results returned

---

### Layer 2: OS-Shell (Ambient AI)

**What it does**: Makes AI persistent, always-available system service.

**Components**:

1. **AIShellService**
   - Foreground service (never killed by Android)
   - Persistent notification (always visible)
   - Lifecycle management
   - Intent routing from other apps

2. **AIShellController**
   - Central orchestrator
   - Manages long-lived AI state
   - Coordinates all subsystems
   - Provides high-level decision making

3. **AIShellContextAggregator**
   - Monitors device state (screen, battery, thermal)
   - Tracks foreground app and user activity
   - Detects system events
   - Provides context to cognition layer

4. **AIShellIntentProtocol**
   - Standard Intent-based communication
   - 6 defined actions (ASK_AI, GET_STATUS, REQUEST_ACTION, etc.)
   - Allows other apps to query AI
   - Safe, permissions-based access

5. **State Machine**
   - INITIALIZING → READY ↔ SLEEPING → ENERGY_SAVING → SHUTDOWN
   - Transitions based on battery, thermal, user activity
   - Graceful degradation under constraints

**Connectivity to Layer 3**: 
- Layer 2 wraps Layer 3 (CognitionLoopManager)
- Passes context from Layer 4 to Layer 3
- Adapts Layer 3 behavior based on Layer 5 constraints

---

### Layer 3: Cognition Engine

**What it does**: Autonomous reasoning, decision-making, and learning.

**The THINK-ACT-REFLECT-EVOLVE Cycle** (continuous loop):

```
┌─────────────────────────────────────────────────────────────┐
│                   Cognition Loop                             │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  THINK: Generate options                                    │
│  ┌────────────────────────────────────────────────────┐    │
│  │ ReasoningEngine reads rules                         │    │
│  │ Analyzes current context (from Layer 4)             │    │
│  │ Generates ranked options with confidence scores    │    │
│  └────────────────────────────────────────────────────┘    │
│           ↓ (pass to AutonomyController)                    │
│                                                              │
│  ACT: Execute best option                                  │
│  ┌────────────────────────────────────────────────────┐    │
│  │ AutonomyController selects top option              │    │
│  │ Executes action (internal state change)            │    │
│  │ Records action and context for reflection          │    │
│  └────────────────────────────────────────────────────┘    │
│           ↓ (time passes, observe outcome)                  │
│                                                              │
│  REFLECT: Check if action achieved goals                   │
│  ┌────────────────────────────────────────────────────┐    │
│  │ ReflectionEngine compares:                          │    │
│  │   - Expected outcome (from confidence score)        │    │
│  │   - Actual outcome (measured from context change)   │    │
│  │ Detects if action succeeded or failed              │    │
│  │ Identifies which rules were helpful                │    │
│  └────────────────────────────────────────────────────┘    │
│           ↓ (if rules need updating)                        │
│                                                              │
│  EVOLVE: Update rules based on learning                    │
│  ┌────────────────────────────────────────────────────┐    │
│  │ EvolutionEngine modifies rules                      │    │
│  │   - If action succeeded: reinforce the rule         │    │
│  │   - If failed: weaken the rule                      │    │
│  │   - Detect conflicts with other rules               │    │
│  │   - Repair contradictions automatically             │    │
│  │ Track learning history and statistics               │    │
│  └────────────────────────────────────────────────────┘    │
│           ↓ (back to THINK with updated rules)              │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

**Key Components**:

1. **MemoryModels** (Storage of knowledge)
   ```
   ├─ Rules (explicit decision logic)
   │  ├─ Condition → Action mappings
   │  ├─ Confidence scores
   │  └─ Learning history
   │
   ├─ Context History (what has happened)
   │  ├─ Past observations
   │  ├─ Previous actions and outcomes
   │  └─ Pattern detection
   │
   └─ Meta-Knowledge (learning about learning)
      ├─ Which rules work best
      ├─ Learning rate and exploration balance
      └─ Prediction confidence estimates
   ```

2. **ReasoningEngine** (THINK)
   - Takes current context + rules
   - Generates decision options
   - Ranks options by predicted likelihood of success
   - Returns top N options with confidence scores
   - **Cycle time**: 100-500ms (depends on energy state)

3. **AutonomyController** (ACT)
   - Selects best option from ReasoningEngine
   - Updates internal state
   - Records decision for later reflection
   - **Timing**: <50ms
   - **Constraint-aware**: Skips expensive actions if battery low

4. **ReflectionEngine** (REFLECT)
   - Compares expected vs actual outcomes
   - Identifies which context signals changed
   - Attributes changes to actions
   - Calculates outcome score (0-1)
   - **Runs**: After actions, at configurable interval
   - **Timing**: 200-1000ms depending on depth

5. **EvolutionEngine** (EVOLVE)
   - Analyzes reflection results
   - Updates rule confidence scores
   - Detects contradictions
   - Generates new rules from patterns
   - **Conflict Resolution**: Prevents incoherent rule sets
   - **Learning Rate**: Adapts to energy state
   - **Timing**: 500-2000ms depending on operations

6. **ContinuousCognitionLoopManager** (Orchestrator)
   - Runs the full cycle continuously
   - Respects energy and thermal constraints
   - Manages coroutines and lifecycle
   - Publishes state to visualization layer
   - **Frequency**: 2-4 cycles per second (normal state)

**Connectivity**:
- Gets context from Layer 4 (perception)
- Gets constraints from Layer 5 (energy/thermal)
- Publishes state to Layer 4 (visualization)
- Publishes decisions to Layer 2 (for user notification)

---

### Layer 4: Perception & Visualization

This layer splits into two complementary systems:

#### 4a: Perception Engine (SystemSignalsManager)

**What it does**: Continuously monitors device state and provides context to the cognition engine.

**Signals Monitored**:
```
Battery State
├─ Current level (0-100%)
├─ Charging status
├─ Health percentage
└─ Estimated time to critical

Thermal State
├─ CPU/SoC temperature
├─ Throttling status
├─ Cooling predictions
└─ Thermal zones

Network State
├─ Connected (yes/no)
├─ Connection type (WiFi/cellular/none)
├─ Signal strength
└─ Latency

Device State
├─ Screen on/off
├─ Device locked/unlocked
├─ Power saving mode
└─ Airplane mode

User Activity
├─ Foreground app
├─ Activity name
├─ Time since last interaction
└─ Predicted user attention

Temporal State
├─ Time of day
├─ Day of week
├─ Time since device boot
└─ Usage patterns
```

**How It Works**:
1. **Broadcast Receivers** listen for system intents (battery changed, screen on/off, etc.)
2. **Content Observers** monitor app usage and activity changes
3. **Polled Sensors** check battery, temperature, network at regular intervals
4. **Coroutines** aggregate signals asynchronously
5. **StateFlow** publishes updates reactively to subscribers

**Update Frequency**: 
- Broadcasts: Immediate
- Polled sensors: Every 1-2 seconds
- Aggregated context: Every 500ms

#### 4b: Visualization Engine (Filament 3D Renderer)

**What it does**: Renders the AI's reasoning as animated 3D graphics.

**How It Works**:

1. **State-to-Geometry Mapping**
   ```
   Internal State → Visual Property
   
   Confidence Score    → Shape intensity
   Option Generation   → Particle emission
   Reflection Status   → Color pulse
   Rule Contradictions → Geometric disruption
   Learning Occurring  → Transformations
   Processing Load     → Animation speed
   ```

2. **Procedural Rendering**
   - No pre-made 3D models
   - Geometry generated algorithmically from state
   - Solves the problem: "What does thinking look like?"
   - Scales from simple (low energy) to complex (high energy)

3. **GPU Acceleration**
   - Uses Filament rendering engine (Google's open-source)
   - Runs on mobile GPU
   - Achieves 60 FPS even on mid-range phones
   - Uses ~10-30% of GPU capacity

4. **Real-Time Updates**
   - Every frame, reads current cognitive state
   - Geometry updates in <100ms
   - Animation responds to both AI state and user gesture
   - Quality scales with device load (energy-aware)

5. **Quality Levels**
   ```
   High (Battery Abundant)
   ├─ Full geometric detail
   ├─ Particle effects
   ├─ Complex materials/shaders
   ├─ 60 FPS expected
   └─ ~20-30% GPU load
   
   Medium (Normal Battery)
   ├─ Simplified geometry
   ├─ Reduced particle count
   ├─ Basic materials
   ├─ 60 FPS target
   └─ ~10-15% GPU load
   
   Low (Low Battery)
   ├─ Minimal geometry
   ├─ No particles
   ├─ Single material
   ├─ 30 FPS acceptable
   └─ ~5% GPU load
   ```

**Connectivity**:
- Reads continuous state from Layer 3 (cognitive state)
- Reads current context from 4a (perception signals)
- Reads constraints from Layer 5 (energy state)
- Renders to screen every 16-33ms (60-30 FPS)

---

### Layer 5: Energy & Thermal Management

**What it does**: Ensures AI adapts to device constraints without crashing or damaging hardware.

**Two Interacting Systems**:

#### 5a: EnergyAwarenessManager

**Monitors**: Battery level, charging state, power saving mode

**Energy States**:
```
ABUNDANT (Battery > 50%, Charging)
├─ Full cognition (4 cycles/sec)
├─ Max reflection depth
├─ Full visualization quality
└─ Enable overlay
  ↓ (battery drops)

NORMAL (Battery 25-50%, Not charging but not critical)
├─ Normal cognition (2-3 cycles/sec)
├─ Moderate reflection
├─ Medium visualization
└─ Overlay enabled
  ↓ (battery drops)

LOW (Battery 15-25%, Approaching critical)
├─ Reduced cognition (1 cycle/sec)
├─ Shallow reflection (only decisions)
├─ Low visualization quality
└─ Disable overlay
  ↓ (battery critical)

CRITICAL (Battery < 15%)
├─ Pause cognition entirely
├─ Disable visualization
├─ Keep service running (notification only)
└─ Resume when battery recovers
```

**How It Works**:
1. Reads battery level via BatteryManager
2. Converts to energy state
3. Broadcasts state changes via Flow
4. Layer 3 (Cognition) subscribes and adjusts frequency/depth
5. Layer 4b (Visualization) subscribes and adjusts quality

#### 5b: ThermalManager

**Monitors**: Device temperature via thermal zones

**Thermal States**:
```
NORMAL (<35°C)
├─ Cognition fully enabled
├─ ML inference enabled
└─ All processing normal

LIGHT (35-40°C)
├─ Same as NORMAL
└─ Begin monitoring

MODERATE (40-45°C)
├─ Reduce rule evaluation frequency
├─ Skip complex reasoning
└─ Enable thermal warnings

SEVERE (45-50°C)
├─ Pause ML inference
├─ Reduce visualization quality
├─ Pause reflection (learning)
└─ User notification

CRITICAL (>50°C)
├─ Pause all non-essential processing
├─ Throttle visualization to 30 FPS
├─ Keep basic cognition running
└─ Emergency notification
```

**Constraint Enforcement**:
- Temperature checked every 2 seconds
- Thresholds prevent thermal throttling before it occurs
- Prediction: if trend suggests high temp, preemptively reduce load
- Avoids device damage while maintaining some AI capability

**MetaCognitionController**:
- Tracks energy cost of each operation (reasoning, reflection, evolution, ML)
- Learns which operations are expensive
- Teaches AI when NOT to think hard
- Builds "cognitive wisdom" (when to defer expensive reasoning)

**Connectivity**:
- Layer 3 (Cognition) reads energy/thermal state before operations
- Adjusts cycle frequency and operation depth
- Layer 4b (Visualization) reads state and adjusts rendering quality

---

### Layer 6: Android Platform

**What it does**: Provides foundational Android services.

**Key Interactions**:
```
App Lifecycle
├─ onCreate: Initialize all subsystems
├─ onStart: Resume cognition
├─ onStop: Pause expensive operations
└─ onDestroy: Cleanup

Permissions
├─ QUERY_ALL_PACKAGES: Monitor foreground app
├─ BATTERY_STATS: Monitor power state
├─ THERMAL_DATA: Monitor temperature
├─ SYSTEM_ALERT_WINDOW: Show overlay
└─ PACKAGE_USAGE_STATS: Track app usage

Resources
├─ CPU: Controlled via coroutine dispatcher
├─ GPU: Managed by Filament renderer
├─ RAM: Bounded memory for state storage
├─ Battery: Monitored continuously
└─ Display: Renders at 60/30 FPS

Services
├─ NotificationManager: Persistent notification
├─ BroadcastReceiver: System event listening
├─ AccessibilityService: App switching detection
└─ WindowManager: Overlay positioning
```

---

## The Cognition Loop (Detailed)

Let's trace a complete THINK-ACT-REFLECT-EVOLVE cycle:

### Scenario: "What should I do when battery is low?"

**State at Cycle Start**:
- Battery: 12%
- Active foreground app: Email
- Time: 8 PM
- Temperature: 38°C
- Recent context: User just received urgent notification

**THINK (ReasoningEngine)**:
```
Input: Current context + all rules
Process:
  1. Retrieve all rules from memory
  2. For each rule: evaluate if condition matches context
  3. Filter to matching rules (rules where condition = true)
  4. Rank by confidence and recency
  5. Generate reasoning:
     - Rule 1: "If battery low AND evening → reduce background tasks"
       Confidence: 0.92 (learned this helps battery last)
     - Rule 2: "If battery low AND email open → prioritize quick tasks"
       Confidence: 0.87
     - Rule 3: "If battery low → aggressive power saving"
       Confidence: 0.78
     - Rule 4: "If email open → check new messages"
       Confidence: 0.65
  6. Synthesize decision: "Reduce background, keep email responsive"

Output: Top option with reasoning
```

**ACT (AutonomyController)**:
```
Input: Best option from ReasoningEngine
Process:
  1. Select option 1: "Reduce background, keep email responsive"
  2. Execute action:
     - Reduce cognition frequency to 1 cycle/sec (save power)
     - Disable visualization overlays (save GPU)
     - Keep email neural inference running (user priority)
  3. Record decision:
     - Timestamp: T0
     - Action: "reduce_background_email_priority"
     - Confidence: 0.87
     - Context snapshot: {battery: 12%, app: email, time: 20:00}

Output: Action executed, recorded for later reflection
```

**REFLECT (ReflectionEngine)**:
```
Wait ~5 minutes, then evaluate:

Input: Action record + current context
Process:
  1. Compare expected vs actual:
     - Expected: Battery drain reduced, email still responsive
     - Actual: Battery now 11%, email loaded in 1.2 sec
  2. Evaluate success:
     - Battery drain: 1% per 5 min (normal) ✓
     - Email responsiveness: 1.2 sec (expected) ✓
     - Overall outcome: SUCCESS
  3. Identify which rule was responsible:
     - Rule 1 (battery low + evening) was condition that matched
     - Reducing background saved power without hurting email
  4. Attribute success:
     - Rule 1 was helpful
     - Rule 4 (keep email running) was also helpful

Output: Success verdict + rule attribution
```

**EVOLVE (EvolutionEngine)**:
```
Input: Reflection results
Process:
  1. Update rule confidence scores:
     - Rule 1: 0.92 → 0.94 (success, reinforce)
     - Rule 2: 0.87 → 0.88 (didn't directly apply, small boost)
     - Rule 4: 0.65 → 0.72 (email responsiveness was good, boost)
  2. Check for contradictions:
     - Rule 1: "If battery low → reduce background"
     - Rule 4: "If email open → check messages"
     - Not contradictory (can do both) ✓
  3. Learn new patterns:
     - If battery low + email open + evening → combo is effective
     - Add new micro-rule: "battery_email_evening_combo" (confidence: 0.85)
  4. Track evolution:
     - Learn rate: 0.15 (increased from default 0.1 due to success)
     - Cognitive wisdom: battery low → think less hard (save power)
  5. Update memory:
     - Save updated rule confidences
     - Save new micro-rule
     - Record learning event

Output: Rules updated, ready for next THINK cycle
```

**Back to THINK**:
- Next cycle uses updated rules
- Rule 1 now has confidence 0.94 (was 0.92)
- New combo rule available (was unknown before)
- System learned something about "battery low + email open" scenarios

This cycle repeats continuously, **thousands of times per day**, gradually improving the AI's decision-making based on actual outcomes.

---

## Perception & Visualization Integration

### How Perception Flows to Visualization

```
SystemSignalsManager (Layer 4a)
  │
  ├─ Monitors 10+ device signals
  ├─ Updates every 500ms
  └─ Publishes via StateFlow: DeviceContextSnapshot
       │
       ├─→ Layer 3 (Cognition) uses for decision-making
       │    (e.g., "what should I do given current battery?")
       │
       └─→ Layer 4b (Visualization)
            ├─ Battery state → visualizes as energy level
            ├─ Thermal state → renders as heat color
            ├─ Foreground app → context visualization
            └─ User activity → animation responsiveness
```

### How Visualization Responds to Cognition

```
ContinuousCognitionLoopManager (Layer 3)
  │
  └─ Every cycle, publishes CognitionState:
       {
         thinking: Boolean,          // Currently reasoning?
         confidence: Float,          // 0-1
         optionCount: Int,          // How many options considered?
         selectedOption: String,    // Which action chosen?
         conflictLevel: Float,      // Any rule contradictions?
         learningRate: Float        // How fast is it learning?
       }
       │
       └─→ Layer 4b (Visualization) receives
            ├─ thinking=true → animate option generation
            ├─ confidence=0.9 → render high confidence (bright, solid)
            ├─ optionCount=5 → show 5 particles/nodes
            ├─ conflictLevel=0.2 → subtle visual disruption
            └─ learningRate=0.15 → faster transformations
```

---

## Energy & Thermal Management Integration

### Energy-Aware Cognition Adaptation

```
EnergyAwarenessManager monitors battery
  │
  ├─ Battery drops to 20%
  ├─ Changes state: NORMAL → LOW
  │
  └─→ Broadcasts: EnergyStateChanged(LOW)
       │
       ├─→ Layer 3 (CognitionLoopManager) reacts:
       │    ├─ Reduce cycle frequency: 4/sec → 1/sec
       │    ├─ Reduce reflection depth: full → decisions only
       │    ├─ Reduce rule evaluation: all rules → top rules only
       │    └─ Disable expensive ML inference
       │
       ├─→ Layer 4b (Visualization) reacts:
       │    ├─ Reduce quality: high → medium → low
       │    ├─ Reduce particle effects
       │    └─ Disable shader complexity
       │
       └─→ Layer 5a (Resource Monitor) tracks:
            ├─ CPU usage drops (slower cognition)
            ├─ GPU usage drops (simpler rendering)
            └─ Battery drain rate monitors improvement
```

### Thermal-Aware Cognition Throttling

```
ThermalManager monitors temperature
  │
  ├─ Temperature reaches 45°C
  ├─ Detects: MODERATE → SEVERE thermal state
  │
  └─→ Enforces thermal constraints:
       │
       ├─→ Layer 3 response:
       │    ├─ Pause ML inference (highest CPU cost)
       │    ├─ Reduce reflection iterations
       │    └─ Slower rule evaluation
       │
       ├─→ Layer 4b response:
       │    ├─ Reduce to 30 FPS (was 60 FPS)
       │    └─ Disable particle effects
       │
       └─→ MetaCognitionController learns:
            ├─ "ML inference when hot is bad"
            ├─ Adjust policy: avoid expensive reasoning when T > 43°C
            └─ AI learns thermal wisdom
```

---

## Data Flow & Integration

### Complete Data Flow Example

**Scenario**: User taps the visualization while battery is low and device is warm.

```
1. USER TOUCHES SCREEN
   ├─ MainUI captures touch event
   ├─ Recognizes tap gesture
   └─ Triggers: showCognitiveIntrospection()

2. INTROSPECTION REQUESTED
   ├─ MainUI asks AIShellController: "Why did you make that decision?"
   └─ AIShellController asks CognitionLoopManager for last decision trace

3. COGNITION STATE RETRIEVED
   ├─ Last decision: {
   │    think: "low battery detected",
   │    rule: "if battery low AND evening → reduce_background",
   │    confidence: 0.92,
   │    timestamp: T-5sec
   │  }
   └─ Trace shows: THINK → ACT → REFLECT → EVOLVE → current state

4. INTROSPECTION DISPLAYED
   ├─ UI shows decision reasoning in natural language
   ├─ Visualization animates the rule that was applied
   ├─ Shows confidence (0.92 = high, render as bright)
   └─ Explains why (battery low + evening detected)

5. USER PROVIDES FEEDBACK
   ├─ "This decision was good" (positive feedback)
   └─ Sends feedback to EvolutionEngine

6. AI LEARNS
   ├─ EvolutionEngine receives feedback
   ├─ Rule confidence: 0.92 → 0.95 (reinforced)
   ├─ New micro-rule learned: "low_battery_evening_is_good"
   └─ Next time, this decision will be preferred even more

7. CONTINUOUS OPERATION ADJUSTS
   ├─ EnergyAwarenessManager still reports LOW energy
   ├─ CognitionLoopManager still running 1 cycle/sec
   ├─ ThermalManager still restricting expensive operations
   └─ Visualization still quality-reduced
   
8. NEXT CYCLE
   ├─ ReasoningEngine runs (with updated rules)
   ├─ New decision made with benefit of user feedback
   └─ Loop continues with improved learning
```

---

## Key Design Decisions

### Decision 1: Explicit Rules Instead of Neural Networks

**Choice**: Rule-based reasoning (if-then logic) not neural networks

**Why**:
- Rules are interpretable (users can read and understand them)
- Rules can be modified at runtime (AI can learn self-modification)
- Rules can be serialized and explained (visualization needs raw logic)
- Rules have clear semantics (reasoning is formal, not probabilistic)

**Tradeoff**:
- Rules handle explicit logic excellently, but struggle with pattern recognition
- Solution: Use neural networks ONLY for perception (image classification, sensor interpretation), not for reasoning
- This way: thinking is transparent (rules) but perception can be powerful (neural)

### Decision 2: Continuous Loops Instead of Episodic Decisions

**Choice**: Cognition runs all the time, not just when user queries

**Why**:
- AI can learn from its own actions over time
- Discovery of new patterns happens continuously
- System evolves without requiring external data
- Reflection and learning happen automatically

**Tradeoff**:
- Higher computational cost
- Solution: Energy-aware adaptation (fewer cycles at low battery)
- Result: Continuous learning that respects device constraints

### Decision 3: Gesture-Triggered Introspection Over Manual Control

**Choice**: Users ask "why?" rather than directly modifying rules

**Why**:
- Users can understand without needing AI expertise
- Feedback guides learning without breaking reasoning
- Balance between autonomy and control
- Encourages genuine collaboration

**Tradeoff**:
- AI makes decisions users might not have chosen
- Solution: Fast introspection (users can question any decision)
- Result: Trust builds through understanding, not control

### Decision 4: Visualization as Core, Not Auxiliary

**Choice**: 3D rendering is fundamental to architecture, not decorative

**Why**:
- Makes reasoning transparent
- Helps users intuitively understand AI state
- Provides real-time feedback on cognition quality
- Enables gesture-based interaction with thinking

**Tradeoff**:
- Higher GPU/CPU cost
- Solution: Procedural generation (algorithms, not art), quality scaling
- Result: Beautiful AND informative AND efficient

### Decision 5: On-Device Only (No Cloud)

**Choice**: All processing happens on the phone

**Why**:
- Privacy (data never leaves device)
- Low latency (<100ms, not 500ms+)
- No API costs
- Works offline
- No external dependencies

**Tradeoff**:
- Limited by phone resources
- Solution: Energy-aware adaptation, thermal management, algorithmic optimization
- Result: Practical AI that works everywhere

### Decision 6: Respect Platform Constraints

**Choice**: Adapt to battery, thermal, and lifecycle constraints

**Why**:
- Prevents damage to hardware
- Extends battery life
- Keeps user happy
- Shows responsible AI design

**How**:
- Energy states (4 levels) determine cognition frequency
- Thermal states (5 levels) determine which operations allowed
- Lifecycle management (onCreate/onStop) prevents resource leaks
- Visualization quality scales with available resources

---

## Summary: How It All Works Together

1. **User Interaction** (Layer 1) → touches screen or asks question
2. **OS-Shell** (Layer 2) → receives request, provides context
3. **Cognition** (Layer 3) → thinks using rules, makes decision
4. **Perception** (Layer 4a) → provides device context
5. **Visualization** (Layer 4b) → shows thinking as 3D animation
6. **Energy/Thermal** (Layer 5) → ensures constraints are respected
7. **Reflection** → measures if decision worked
8. **Evolution** → updates rules based on outcome
9. **Back to Cognition** → next decision uses improved rules
10. **User Feedback** → influences future learning

This creates a **closed loop where the AI continuously improves through experience**, while **users understand and can influence the learning** through visualization and gesture.

That's the entire SA-AIHOS architecture: **Thinking → Doing → Checking → Learning → Better Thinking**.

