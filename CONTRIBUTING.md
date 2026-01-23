# Contributing to SA-AIHOS

**Thank you for considering contributing to SA-AIHOS!**

This document provides guidelines for contributing to the Self-Evolving Autonomous AI Human OS project. We welcome contributions from researchers, engineers, designers, and maintainers worldwide.

---

## 🎯 What We're Looking For

### Researchers
- Investigation into cognitive interfaces, transparent AI, autonomous learning
- User studies validating our research claims
- Novel applications of self-evolving systems

### Engineers
- Mobile optimization (Kotlin, performance)
- 3D visualization improvements (Three.js, WebGL)
- Android-JavaScript bridge optimization
- Bug fixes and architectural improvements

### Designers & Product
- User experience improvements
- 3D visualization refinement
- Gesture interaction design
- Accessibility improvements

### Documentation & Community
- Clarity improvements to existing documentation
- Tutorials and guides for new contributors
- Examples and use-case demonstrations
- Translation and localization

---

## 🚀 Getting Started

### 1. Development Environment Setup

#### Requirements
- **Java 11+** (for Kotlin compilation)
- **Android SDK** (API 28+, target API 35+)
- **Android Studio** (latest stable, recommended) or command-line tools
- **Node.js 16+** (for JavaScript tooling)
- **Gradle 8.5+** (build automation)
- **Git** (version control)

#### Step-by-Step Setup

```bash
# Clone the repository
git clone https://github.com/Amank326/SELF-EVOLVING-AUTONOMOUS-AI-HUMAN-OS-SA-AIHOS-.git
cd "SELF-EVOLVING-AUTONOMOUS-AI-HUMAN-OS-SA-AIHOS-"

# Set up Android development environment
# Install Android Studio from: https://developer.android.com/studio
# Or use command-line tools: https://developer.android.com/tools/releases/cmdline-tools

# Verify Gradle installation
gradle --version

# Verify Android SDK setup
# Ensure ANDROID_SDK_ROOT environment variable is set

# Build the project (on Windows)
./build.bat clean build

# Build the project (on macOS/Linux)
./gradlew clean build

# Run tests
./gradlew test

# Start development
# Open in Android Studio or use emulator
./gradlew build -x test  # Build without tests (for faster iteration)
```

#### Environment Variables
```bash
# Required for builds
ANDROID_SDK_ROOT = /path/to/android/sdk
ANDROID_HOME = /path/to/android/sdk

# Optional (for CI/CD)
GRADLE_USER_HOME = ~/.gradle
```

#### IDE Setup (Android Studio)
1. Open project: File → Open → select project root
2. Wait for Gradle sync (first time: 5-10 minutes)
3. Run app: Click "Run" button or `Shift + F10`
4. Target device: Android emulator (API 28+) or physical device (API 28+)

---

### 2. Project Structure

```
SA-AIHOS/
├── app/                          # Main Android application
│   ├── src/main/
│   │   ├── AndroidManifest.xml   # App metadata
│   │   ├── kotlin/com/aihos/     # Kotlin source code
│   │   │   ├── SAIHOSApplication.kt
│   │   │   ├── ai/               # AI reasoning layer
│   │   │   ├── ui/               # UI and Compose screens
│   │   │   ├── data/             # Data persistence
│   │   │   └── di/               # Dependency injection
│   │   └── res/                  # Android resources
│   └── build.gradle.kts          # Gradle configuration
│
├── 3d-scene/                     # 3D visualization system
│   ├── src/
│   │   ├── bridge/               # Android-JavaScript bridge
│   │   ├── animation/            # Procedural animation
│   │   ├── rendering/            # Three.js rendering
│   │   └── interaction/          # Gesture handling
│   └── package.json              # Node.js dependencies
│
├── docs/                         # Documentation
├── CONTRIBUTING.md               # This file
├── CODE_OF_CONDUCT.md            # Community standards
├── ROADMAP.md                    # Project roadmap
├── README.md                     # Project overview
└── build.gradle.kts              # Root Gradle config
```

---

## 💻 Coding Standards

### Kotlin (Android)

#### Style Guide
- Follow [Kotlin official style guide](https://kotlinlang.org/docs/coding-conventions.html)
- Use Kotlin idioms (extension functions, data classes, scope functions)
- Prefer immutability: `val` over `var`
- Use explicit types in public APIs, type inference in implementations

#### Example
```kotlin
// Good: Clear intent, idiomatic Kotlin
data class DecisionRecord(
    val id: String,
    val timestamp: Long,
    val decision: String,
    val outcome: Outcome
)

fun analyzeDecision(record: DecisionRecord): Insight =
    record.outcome.takeIf { it == Outcome.SUCCESS }
        ?.let { Insight("Success pattern identified") }
        ?: Insight("Failure analysis needed")

// Avoid: Verbose, un-idiomatic
class DecisionRecord {
    var id: String = ""
    var timestamp: Long = 0
    // ... getter/setter boilerplate
}
```

#### Naming Conventions
```kotlin
// Classes and interfaces: PascalCase
class ReasoningEngine { }
interface EvolutionStrategy { }

// Functions and variables: camelCase
fun analyzeOutcome() { }
val decisionWeights: Map<String, Float> = emptyMap()

// Constants: UPPER_SNAKE_CASE
const val MAX_WEIGHT_CHANGE = 0.1f
const val DEFAULT_LEARNING_RATE = 0.3f

// Private/internal: prefix with underscore (if needed)
private val _internalState = mutableMapOf<String, Any>()
```

#### Async/Coroutines
```kotlin
// Use Kotlin Coroutines for async work
viewModelScope.launch {
    val result = analyticsRepository.getMetrics()
    updateUI(result)
}

// Avoid callbacks and manual threading
// Don't use Thread() or Runnable directly
```

#### Testing
```kotlin
// Use JUnit 4 + Mockito for unit tests
@Test
fun `reasoning engine scores options correctly`() {
    val engine = ReasoningEngine()
    val score = engine.scoreOption(testOption)
    
    assertTrue(score in 0f..1f)
    assertEquals(expectedScore, score, 0.01f)
}

// Structure: Given-When-Then
@Test
fun `AI evolves weights after successful outcome`() {
    // Given: AI with initial weights
    val ai = createTestAI()
    val initialWeight = ai.getWeight("test_rule")
    
    // When: Successful outcome is reported
    ai.reportOutcome(DecisionOutcome.SUCCESS)
    
    // Then: Weight increases
    val newWeight = ai.getWeight("test_rule")
    assertTrue(newWeight > initialWeight)
}
```

### JavaScript (3D Visualization)

#### Style Guide
- Follow [Airbnb JavaScript Style Guide](https://github.com/airbnb/javascript)
- Use ES6+ syntax (arrow functions, destructuring, template literals)
- Strict mode: `'use strict'` at file top

#### Example
```javascript
// Good: Modern, readable
class ProceduralAnimationController {
  constructor(scene, aiStateMonitor) {
    this.scene = scene;
    this.aiStateMonitor = aiStateMonitor;
    this.animationParameters = new Map();
  }

  computeAnimationFrame(aiState) {
    const { breathing, rotation, color, glow } = aiState;
    return {
      targetRotation: this.computeRotation(rotation),
      targetColor: this.computeColor(color),
      targetGlow: glow * 0.8
    };
  }
}

// Avoid: Callback hell, imperative
function updateAnimation(aiState, callback) {
  setTimeout(() => {
    scene.rotation.z += 0.01;
    callback();
  }, 16);
}
```

#### Naming Conventions
```javascript
// Classes: PascalCase
class MotionIntelligenceSystem { }

// Functions, variables: camelCase
function computeBreathingRate(cpuLoad) { }
const animationState = new AnimationState();

// Constants: UPPER_SNAKE_CASE
const MAX_PARTICLE_COUNT = 1000;
const DEFAULT_FPS = 60;

// Private methods: prefix with underscore
_updateInternalState() { }
```

#### async/await
```javascript
// Good: Use async/await for clarity
async function updateVisualization(aiState) {
  try {
    const metrics = await aiStateMonitor.getMetrics();
    const animation = await animationComputer.compute(metrics);
    renderer.render(animation);
  } catch (error) {
    console.error('Visualization update failed:', error);
  }
}

// Avoid: Promises without async/await (unless for parallel work)
```

#### Testing
```javascript
// Use Jest for JavaScript testing
describe('ProceduralAnimationController', () => {
  it('computes breathing rate from cognitive load', () => {
    const controller = new ProceduralAnimationController();
    const load = 0.75; // 75% cognitive load
    
    const breathing = controller.computeBreathingRate(load);
    
    expect(breathing).toBeGreaterThan(0.5);
    expect(breathing).toBeLessThan(1.5);
  });
});
```

---

## 🔧 Making Changes

### Before You Start

1. **Check existing issues**: Does your idea already exist as an issue?
2. **Start a discussion**: For large changes, open a discussion before coding
3. **Create an issue**: Describe the problem or feature you want to address

### Step-by-Step Contribution Process

#### 1. Fork and Branch
```bash
# Fork the repo on GitHub (one time)
# Clone your fork
git clone https://github.com/YOUR_USERNAME/SA-AIHOS.git
cd SA-AIHOS

# Create a feature branch (never commit to main/master)
git checkout -b feature/your-feature-name
# or for bug fixes
git checkout -b fix/issue-description
```

#### 2. Make Your Changes

Follow the coding standards above. Keep changes:
- **Focused**: One feature or fix per branch
- **Minimal**: Don't refactor unrelated code
- **Tested**: Include unit tests for new code
- **Documented**: Update docs if behavior changes

#### 3. Test Your Changes

```bash
# Run all tests
./gradlew test                 # Kotlin/Android tests
npm test                       # JavaScript tests

# Test specific functionality
./gradlew test --tests "*ReasoningEngine*"

# Manual testing
# Open in Android Studio, run on emulator or device
# Check:
# - AI state changes are visible
# - Gestures respond immediately
# - No crashes or memory leaks
```

#### 4. Commit with Clear Messages

```bash
# Follow conventional commits format
git commit -m "feat(ai): add multi-agent reasoning capability"
git commit -m "fix(visualization): correct breathing rate calculation"
git commit -m "docs(contributing): improve setup instructions"

# Format: type(scope): description
# Types: feat, fix, docs, style, refactor, perf, test, chore
# Scope: ai, visualization, bridge, performance, etc.
```

#### 5. Push and Open a Pull Request

```bash
git push origin feature/your-feature-name
```

Then on GitHub:
1. Go to your fork
2. Click "New Pull Request"
3. Write a clear description:
   - What problem does this solve?
   - How does it solve it?
   - Any breaking changes?
   - Links to related issues

#### 6. Respond to Review

- Be responsive to feedback
- Make requested changes in additional commits
- Ask questions if feedback is unclear
- Don't take criticism personally—it's about code quality

---

## 🤖 Designing AI Changes

When modifying the AI reasoning, evolution, or memory layers:

### Design Checklist
- [ ] Explain the hypothesis: "What behavior should improve?"
- [ ] Propose metrics: "How do we measure improvement?"
- [ ] Identify constraints: "What must stay the same?"
- [ ] Test edge cases: "What could go wrong?"
- [ ] Consider performance: "Will this run on mobile?"

### Example AI Change
```kotlin
// Bad: "Let's change the learning rate"
evolutionEngine.learningRate = 0.5f

// Good: "We hypothesize faster learning with higher learning rate"
// Metric: Success rate improvement over N decisions
// Constraint: Stability—no divergence in weights
// Edge case: What if all outcomes are failures?

const val EXPERIMENTAL_LEARNING_RATE = 0.5f
const val BASELINE_LEARNING_RATE = 0.3f

// Test both, measure success rate, compare convergence stability
```

### AI Modification Guidelines

**Reasoning Engine Changes**
- Must maintain decision quality
- Should improve or maintain latency (<100ms)
- Changes must be auditable (logged)
- Must work with existing gesture system

**Evolution Engine Changes**
- Learning rate changes need justification
- Weight bounds must be validated
- Must prevent divergence
- Should show measurable improvement over baseline

**Memory Layer Changes**
- Must not break existing decision logic
- Should maintain or improve retrieval performance
- Must be compatible with persistence layer
- Consider memory constraints (<50MB total)

---

## 🎨 Designing Visualization Changes

When modifying 3D animation or cognitive interface:

### Design Checklist
- [ ] Cognitive integrity: Does this accurately represent AI state?
- [ ] Clarity: Can users understand what's shown?
- [ ] Performance: Will this run at 60 FPS?
- [ ] Consistency: Does this fit with existing aesthetics?
- [ ] Accessibility: Can colorblind users understand?

### Example Visualization Change
```javascript
// Bad: "Make it more colorful"
particleColor = new THREE.Color(Math.random(), Math.random(), Math.random());

// Good: "Use color to show decision confidence"
// Mapping: Low confidence (red) → High confidence (green)
// Constraint: Must be visible on all devices
// Testing: Verify colorblind users see distinction

const CONFIDENCE_COLORS = {
  low: 0xff0000,      // Red
  medium: 0xffff00,   // Yellow
  high: 0x00ff00      // Green
};

particleColor = CONFIDENCE_COLORS[confidence];
```

---

## 📝 Documentation

When adding features, also add documentation:

### Required Documentation
- **Code comments**: Why, not what. Code shows what; comments explain why.
- **Function/class documentation**: Parameters, return value, exceptions
- **README updates**: If user-facing changes
- **ARCHITECTURE.md updates**: If architecture changes

### Example Documentation
```kotlin
/**
 * Adapts decision weights based on outcome reflection.
 * 
 * Uses exponential moving average to weight recent successes:
 * newWeight = oldWeight * (1 - alpha) + successRate * alpha
 * 
 * @param outcome The observed outcome (SUCCESS, PARTIAL, FAILURE)
 * @param decisionId The decision to learn from
 * @throws IllegalStateException if weight bounds violated
 */
fun adaptWeights(outcome: Outcome, decisionId: String) {
    val successRate = calculateSuccessRate(outcome)
    val newWeight = updateMovingAverage(successRate)
    require(newWeight in 0f..1f) { "Weight bounds violated: $newWeight" }
}
```

---

## ⚙️ Performance Considerations

Contributions should maintain these performance targets:

### Mobile Performance (Android)
- **Memory**: <50MB total app size
- **CPU**: <100ms latency for gesture response
- **FPS**: 60 FPS target, 30 FPS minimum
- **Battery**: Adaptive quality scaling for low battery

### JavaScript/Rendering
- **Frame time**: <16ms per frame (60 FPS)
- **Memory**: <100MB for all Three.js objects
- **Bridge latency**: <40ms Android-JavaScript round trip

### AI Reasoning
- **Decision cycle**: 10-30 Hz (100-30ms per decision)
- **Memory**: Each decision record <1KB
- **History buffer**: 1000 decisions max

### Before Submitting
```bash
# Profile performance
./gradlew profileDebug

# Check memory with Android Profiler
# Verify FPS in Android Studio

# JavaScript performance
npm run profile

# Check that targets still met
```

---

## 🧪 Testing Requirements

All contributions should include tests:

### Test Coverage Targets
- **AI reasoning**: 80%+ coverage
- **Evolution engine**: 85%+ coverage
- **Visualization**: 60%+ coverage (harder to test)
- **Bridge/IPC**: 70%+ coverage

### Running Tests

```bash
# All tests
./gradlew test

# With coverage report
./gradlew test jacocoTestReport

# Specific test class
./gradlew test --tests ReasoningEngineTest

# JavaScript tests
npm test
npm test -- --coverage
```

---

## 🚀 Pull Request Checklist

Before submitting a PR:

- [ ] Tests added/updated
- [ ] Code follows style guide
- [ ] Documentation updated (if needed)
- [ ] No breaking changes (or explained clearly)
- [ ] Performance targets met
- [ ] All tests pass locally
- [ ] Commit messages clear and conventional
- [ ] No unrelated changes mixed in
- [ ] Related issues linked in description

---

## 📞 Getting Help

### Where to Ask Questions
- **GitHub Issues**: Bug reports, feature requests
- **GitHub Discussions**: Design questions, architecture discussions
- **Documentation**: Check ARCHITECTURE.md, QUICK_START.md first

### Response Time Expectations
- **Critical bugs**: 24-48 hours
- **Feature discussions**: 1-2 weeks
- **Code review**: 3-7 days for active maintainers

---

## 📜 License

By contributing to SA-AIHOS, you agree that your contributions will be licensed under the MIT License. This allows the project to remain open and freely available.

---

## 🎯 Final Notes

**What we value:**
- Quality over quantity
- Thoughtful design over quick fixes
- Clear communication
- Learning and iteration
- Respect and collaboration

**What we discourage:**
- Large, unfocused changes
- Breaking changes without discussion
- Committing without tests
- Dismissive or disrespectful communication

**We're excited to work with you. Welcome to the SA-AIHOS community!**

---

**Questions?** Open an issue or discussion, or reach out to the maintainers.

**Thank you for contributing!** 🙏

