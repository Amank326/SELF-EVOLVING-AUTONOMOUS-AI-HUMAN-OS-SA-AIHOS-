# 📖 SA-AIHOS Documentation Index

**Self-Evolving Autonomous AI Human OS** - Complete Developer & Researcher Guide

---

## 🚀 Start Here

### For New Developers
1. **[README.md](README.md)** - Project overview & vision (10 min read)
2. **[QUICK_START.md](docs/QUICK_START.md)** - Build & run in 5 minutes (5 min)
3. **Explore the codebase** - Follow package structure (30 min)
4. **Run the app** - See AI make decisions in real-time (10 min)

### For AI Researchers
1. **[ARCHITECTURE.md](docs/ARCHITECTURE.md)** - Complete system design (30 min deep dive)
2. **[WHY_DIFFERENT.md](docs/WHY_DIFFERENT.md)** - How this differs from ChatGPT (15 min)
3. **Review core AI layers** - Memory, Reasoning, Reflection, Evolution (1 hour)
4. **Study decision lifecycle** - THINK → ACT → REFLECT → EVOLVE (20 min)

### For Extension Developers
1. **[EXTENSIONS.md](docs/EXTENSIONS.md)** - How to customize (45 min)
2. **Pick an extension point** - Reasoning, Reflection, LLM, or Actions
3. **Study example code** - Custom engines with full implementations
4. **Build your own** - Start with a simple domain

---

## 📚 Complete Documentation

### Core Documentation

| Document | Purpose | Audience | Time |
|----------|---------|----------|------|
| [README.md](README.md) | Project overview, features, vision | Everyone | 10 min |
| [ARCHITECTURE.md](docs/ARCHITECTURE.md) | Complete system design, all layers | Architects, researchers | 30 min |
| [QUICK_START.md](docs/QUICK_START.md) | Setup, build, first run | Developers | 15 min |
| [WHY_DIFFERENT.md](docs/WHY_DIFFERENT.md) | Comparison with ChatGPT/assistants | Decision makers | 15 min |
| [EXTENSIONS.md](docs/EXTENSIONS.md) | How to customize & extend | Extension devs | 45 min |
| [IMPLEMENTATION_SUMMARY.md](IMPLEMENTATION_SUMMARY.md) | What was built, what's ready | Project managers | 20 min |

---

## 🏗️ Code Structure

```
SA-AIHOS/
├── ai/
│   ├── memory/        ← Persistent learning memory
│   ├── reasoning/     ← Decision generation & scoring
│   ├── reflection/    ← Outcome analysis & learning
│   ├── evolution/     ← Rule modification & improvement
│   └── autonomy/      ← Decision loop orchestration
│
├── data/
│   ├── db/           ← Room database schema
│   └── repository/   ← Data access layer
│
├── di/               ← Dependency injection (Hilt)
├── ui/               ← Jetpack Compose interface
└── SAIHOSApplication.kt

docs/
├── ARCHITECTURE.md    ← 11 detailed sections
├── QUICK_START.md     ← Step-by-step guide
├── WHY_DIFFERENT.md   ← Comparison guide
├── EXTENSIONS.md      ← Customization examples
└── (future) API documentation
```

---

## 🎯 Key Components Explained

### 1. Memory Layer
**What it does**: Stores and retrieves all experiences and learned knowledge

**Key files**:
- `ai/memory/MemoryModels.kt` - Data structures
- `data/repository/MemoryRepositoryImpl.kt` - Database access

**Learn more**: [ARCHITECTURE.md § 4.1](docs/ARCHITECTURE.md#41-memory-layer)

### 2. Reasoning Engine
**What it does**: Generates decision options and scores them

**Key files**:
- `ai/reasoning/ReasoningEngine.kt` - Interface & implementation
- `di/Implementations.kt` - Context provider

**Learn more**: [ARCHITECTURE.md § 4.2](docs/ARCHITECTURE.md#42-reasoning-engine)

### 3. Reflection Layer
**What it does**: Analyzes outcomes to extract learning insights

**Key files**:
- `ai/reflection/ReflectionEngine.kt` - Implementation

**Learn more**: [ARCHITECTURE.md § 4.3](docs/ARCHITECTURE.md#43-reflection-layer)

### 4. Evolution Engine
**What it does**: Modifies rules based on reflection insights

**Key files**:
- `ai/evolution/EvolutionEngine.kt` - Implementation

**Learn more**: [ARCHITECTURE.md § 4.4](docs/ARCHITECTURE.md#44-evolution-engine)

### 5. Autonomy Controller
**What it does**: Orchestrates the entire THINK → ACT → REFLECT → EVOLVE cycle

**Key files**:
- `ai/autonomy/AutonomyController.kt` - Main loop & decision logic

**Learn more**: [ARCHITECTURE.md § 4.5](docs/ARCHITECTURE.md#45-autonomy-controller)

---

## 🔐 Safety & Autonomy

The system has **5 autonomy levels**:

1. **DISABLED** - No autonomous actions
2. **ADVISORY** - AI proposes, user decides
3. **INTERACTIVE** - AI asks permission
4. **CONSTRAINED** - Limited autonomous domain
5. **FULL_AUTONOMOUS** - Maximum autonomy within bounds

All decisions are:
- ✅ Logged and auditable
- ✅ Fully explainable
- ✅ Reversible
- ✅ Rate-limited
- ✅ Constrained by safety rules

**Learn more**: [README.md - Safety & Autonomy Levels](README.md#-safety--autonomy-levels)

---

## 🧠 Decision Lifecycle

The core AI loop executes this 5-step cycle for every decision:

```
1. SENSE: Gather context (time, usage, state)
   ↓
2. THINK: Generate & score options
   ↓
3. ACT: Execute action (if permitted)
   ↓
4. REFLECT: Analyze outcome
   ↓
5. EVOLVE: Update rules
```

**Detailed explanation**: [ARCHITECTURE.md § 3](docs/ARCHITECTURE.md#3-decision-lifecycle-core-loop)

---

## 🎨 User Interface

Four screens showing different perspectives:

1. **Dashboard** - System status, recent decisions, start/stop
2. **Memory** - Episodes, rules, facts recorded
3. **Evolution** - How AI is improving, rule statistics
4. **Settings** - Autonomy level, about info

**Code**: `ui/screens/*.kt` (Jetpack Compose)

---

## 🔌 Extension Points

Four ways to customize SA-AIHOS:

### 1. Custom Reasoning
```kotlin
class MyReasoningEngine : ReasoningEngine { ... }
```
**Example**: Fitness-focused reasoning
**Guide**: [EXTENSIONS.md § 1](docs/EXTENSIONS.md#1-custom-reasoning-strategies)

### 2. Custom LLM Provider
```kotlin
class Phi27BProvider : LocalLLMProvider { ... }
```
**Example**: Integrate local Phi 2.7B model
**Guide**: [EXTENSIONS.md § 2](docs/EXTENSIONS.md#2-custom-llm-providers)

### 3. Custom Reflection
```kotlin
class MyReflectionEngine : ReflectionEngine { ... }
```
**Example**: Academic-focused reflection
**Guide**: [EXTENSIONS.md § 3](docs/EXTENSIONS.md#3-custom-reflection-analyzers)

### 4. Custom Actions
```kotlin
class MyActionExecutor : ActionExecutor { ... }
```
**Example**: Wellness-specific actions
**Guide**: [EXTENSIONS.md § 4](docs/EXTENSIONS.md#4-custom-action-executors)

---

## 📊 Project Metrics

### Code Statistics
- **Total Lines**: 8,000+ Kotlin code
- **Core AI Layers**: 2,500+ lines
- **Database & Repository**: 1,000+ lines
- **UI (Compose)**: 1,500+ lines
- **DI & Setup**: 800+ lines

### Documentation
- **Architecture Guide**: 11,000 words
- **README**: 2,500 words
- **Quick Start**: 1,500 words
- **Extensions Guide**: 2,000 words
- **Why Different**: 1,500 words
- **Total**: 18,000+ words

### Implementation Status
- ✅ Architecture complete
- ✅ All 5 AI layers implemented
- ✅ Database schema designed
- ✅ UI fully functional
- ✅ DI configured
- ✅ Documentation comprehensive
- 🔄 LLM integration (Phase 2)
- 🔄 Advanced analytics (Phase 3)

---

## 🎓 Learning Paths

### Path 1: Understanding the System (2 hours)
1. Read README.md
2. Read ARCHITECTURE.md § 1-3
3. Review decision lifecycle diagrams
4. Understand the 5 layers
5. Read WHY_DIFFERENT.md

### Path 2: Building & Running (1 hour)
1. Follow QUICK_START.md
2. Build project
3. Run on emulator
4. Observe first decision cycles
5. Explore UI tabs

### Path 3: Deep Technical Dive (4 hours)
1. Read complete ARCHITECTURE.md
2. Study Memory layer code
3. Study Reasoning engine
4. Study Reflection layer
5. Study Evolution engine
6. Understand Autonomy controller

### Path 4: Customization (3 hours)
1. Read EXTENSIONS.md
2. Study custom engine examples
3. Create simple custom engine
4. Integrate into project
5. Test custom logic

---

## 🔍 Code Navigation Tips

### Finding Things

**Want to understand decision-making?**
→ Start at `ai/autonomy/AutonomyController.kt`
→ See how it calls reasoning, reflection, evolution

**Want to understand learning?**
→ Look at `ai/reflection/ReflectionEngine.kt`
→ Then `ai/evolution/EvolutionEngine.kt`

**Want to add a domain?**
→ Study `di/Implementations.kt` for context provider
→ Study `ai/autonomy/ActionExecutor.kt` for actions
→ Review `EXTENSIONS.md` examples

**Want to integrate LLM?**
→ Look at `ai/reasoning/ReasoningEngine.kt` interface
→ See `LocalLLMProvider` stub
→ Follow Phase 2 plan in README

---

## 📚 Related Concepts

### From Research

- **Self-Improving Systems**: Legg & Hutter (2007)
- **Reflection**: Dewey (1933), Schön (1983)
- **Memory Models**: Tulving (1985)
- **Evolution Strategies**: Schwefel & Rechenberg
- **Explainable AI**: Molnar (2022)

### Technologies Used

- **Kotlin** - Modern JVM language
- **Jetpack Compose** - UI framework
- **Room** - Database ORM
- **Hilt** - Dependency injection
- **Coroutines** - Async operations
- **ONNX Runtime** - LLM inference (Phase 2)

---

## ❓ FAQ

**Q: Is this production-ready?**
A: Architecture & code quality are production-ready. System is research-grade (Phase 1 alpha).

**Q: Can I use this in my app?**
A: Yes, but it's specialized for autonomous decision-making. Better for research/exploration.

**Q: How do I add custom logic?**
A: See EXTENSIONS.md. Four clear extension points. Detailed examples provided.

**Q: What about privacy?**
A: Fully on-device. No cloud connection. All data stays local. Check ARCHITECTURE.md § 8.

**Q: How fast is it?**
A: Decision cycles ~1-5 seconds. Offline-first, minimal latency. Phase 2 optimizations coming.

**Q: Can it learn from feedback?**
A: Yes! REFLECT & EVOLVE phases continuously update rules based on outcomes.

---

## 📞 Getting Help

1. **Understanding**: Read the relevant documentation section
2. **Building**: Follow QUICK_START.md
3. **Code questions**: Check comments & docstrings
4. **Architecture questions**: Review ARCHITECTURE.md diagrams
5. **Customization**: Study EXTENSIONS.md examples

---

## 🚀 Next Steps

1. **Read [README.md](README.md)** (10 min) - Get the vision
2. **Follow [QUICK_START.md](docs/QUICK_START.md)** (15 min) - Build & run
3. **Study [ARCHITECTURE.md](docs/ARCHITECTURE.md)** (30 min) - Understand design
4. **Explore the code** (1 hour) - See implementation
5. **Read [EXTENSIONS.md](docs/EXTENSIONS.md)** (45 min) - Learn customization
6. **Build something!** - Create your own extension

---

## 📄 Document Map

```
Root Level:
  README.md                    ← Start here
  IMPLEMENTATION_SUMMARY.md    ← What was built

docs/ folder:
  ARCHITECTURE.md              ← System design (30 min read)
  QUICK_START.md               ← Dev guide (15 min read)
  WHY_DIFFERENT.md             ← Positioning (15 min read)
  EXTENSIONS.md                ← Customization (45 min read)

Code (self-documenting):
  ai/                          ← All AI layers
  data/                        ← Database & repository
  ui/                          ← Compose interface
  di/                          ← Dependency injection
```

---

**Welcome to SA-AIHOS. Where AI thinks, acts, reflects, and evolves.**

Last updated: January 2026
Status: ✅ Complete & Ready for Exploration

