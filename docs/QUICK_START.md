# Quick Start Guide - SA-AIHOS

## 📋 Prerequisites

- **Android Studio**: Iguana (2023.2.1) or newer
- **JDK**: 17+
- **Android SDK**: API 34
- **Gradle**: 8.2+

## 🚀 Setup (5 minutes)

### Step 1: Clone & Open
```bash
git clone https://github.com/yourusername/SA-AIHOS.git
cd SA-AIHOS
```

Open in Android Studio:
- File → Open → Select SA-AIHOS folder

### Step 2: Sync & Build
```bash
./gradlew clean build
```

Or in Android Studio:
- Sync Now (when prompted)
- Build → Make Project

### Step 3: Run
```bash
./gradlew installDebug
```

Or:
- Select Run Configuration: "app"
- Click Run ▶️

---

## 🎮 First Use

### Initial Launch
1. **Dashboard Tab** - See system status
2. Click **"Start"** button to activate autonomous loop
3. Status changes from Idle → Running
4. AI begins decision cycles

### Dashboard Screen
```
┌─────────────────────────────────────┐
│ System Status: Running              │ ← Click Stop to pause
├─────────────────────────────────────┤
│ Autonomy Level: CONSTRAINED         │ ← Current permission level
├─────────────────────────────────────┤
│ Recent Decisions (Last 10):         │
│ ├─ send_focus_reminder (22:45)      │
│ ├─ do_nothing (22:15)               │
│ └─ suggest_pause (21:30)            │
└─────────────────────────────────────┘
```

### Memory Tab
Shows what AI has learned:
- **Total Episodes**: Number of decisions recorded
- **Active Rules**: Behavioral patterns AI uses
- **Memory Usage**: How much storage is used

### Evolution Tab
Shows how AI is improving:
- **Rule Statistics**: Total, active, deprecated rules
- **THINK→ACT→REFLECT→EVOLVE cycle** visualization

### Settings Tab
Control AI autonomy:

```
Autonomy Level:
┌─────────────────────────────────┐
│ DISABLED                        │ ← No autonomous actions
├─────────────────────────────────┤
│ ADVISORY                        │ ← AI suggests, you decide
├─────────────────────────────────┤
│ INTERACTIVE           (Current) │ ← AI asks permission
├─────────────────────────────────┤
│ CONSTRAINED                     │ ← Limited autonomous domain
├─────────────────────────────────┤
│ FULL_AUTONOMOUS                 │ ← Maximum autonomy
└─────────────────────────────────┘
```

---

## 🔍 Understanding the AI Loop

### Every 60 seconds (by default), AI does:

**1️⃣ SENSE** (Gather Context)
```
Current time: 22:47
App usage: 120 minutes
User focus: YES
Battery: 45%
```

**2️⃣ THINK** (Generate & Score Options)
```
Option A: send_focus_reminder      → Score: 0.72 ✓ SELECTED
Option B: suggest_mindfulness      → Score: 0.65
Option C: do_nothing               → Score: 0.38
```

**3️⃣ ACT** (Execute if Permitted)
```
✓ Permission granted (CONSTRAINED level)
→ Sending notification to user...
```

**4️⃣ REFLECT** (Analyze Outcome)
```
Expected: User takes a break
Actual: User dismissed, continued working
Insight: "Reminders don't work during conversations"
```

**5️⃣ EVOLVE** (Update Rules)
```
↓ Old Rule Weight: 0.80 → New: 0.65
↑ Create New Rule: "Check conversation status first"
```

---

## 📊 Example Decision Log

```
TIME        ACTION                    OUTCOME   CONFIDENCE
─────────────────────────────────────────────────────────
22:47       send_focus_reminder       SUCCESS   72%
22:45       do_nothing                N/A       38%
22:15       suggest_pause             FAILURE   65%
21:30       send_focus_reminder       SUCCESS   79%
21:00       do_nothing                N/A       42%
```

---

## 🧪 Testing Scenarios

### Scenario 1: See AI Make a Decision
1. Start app → Set autonomy to ADVISORY
2. Wait ~1 minute
3. Check Memory/Evolution tabs
4. Dashboard will show new decision recorded

### Scenario 2: Watch Rules Evolve
1. Run for 10+ minutes
2. Check Evolution tab
3. Note "New Rules Created This Session"
4. See which rules are improving

### Scenario 3: Override AI Decision
1. Set autonomy to INTERACTIVE
2. When AI proposes action, UI shows notification
3. Accept or Reject
4. AI learns from your feedback

---

## 🐛 Troubleshooting

### App crashes on startup
```
✗ Issue: Hilt not injecting correctly
✓ Fix: 
  - Rebuild project: ./gradlew clean build
  - Sync Gradle files
  - Invalidate caches (File → Invalidate Caches)
```

### Memory tab shows no data
```
✗ Issue: No decisions recorded yet
✓ Fix:
  - Set autonomy to ADVISORY+ level
  - Click "Start" on Dashboard
  - Wait 60+ seconds for first decision cycle
```

### Settings not persisting
```
✗ Issue: DataStore not initialized
✓ Fix:
  - Check logcat for errors
  - Clear app data: Settings → Apps → SA-AIHOS → Clear
  - Restart app
```

---

## 📚 Example Code Usage

### Manually Trigger Decision
```kotlin
val outcome = autonomyController.triggerDecisionCycle(
    context = currentContext
)
println("Decision: ${outcome.action}")
println("Executed: ${outcome.executed}")
println("Reasoning: ${outcome.reasoning}")
```

### Check Memory Stats
```kotlin
val stats = memoryRepository.getMemoryStats()
println("Episodes: ${stats.totalEpisodes}")
println("Rules: ${stats.totalRules}")
println("Memory: ${stats.memoryUsageBytes / 1_000_000}MB")
```

### Report Outcome
```kotlin
autonomyController.reportOutcome(
    decisionId = "dec_123",
    outcome = Outcome.SUCCESS,
    feedback = "User seemed happy"
)
```

### Get Evolution Report
```kotlin
val report = evolutionEngine.getEvolutionReport()
println("Active Rules: ${report.activeRulesCount}")
println("New Rules: ${report.newRulesCreatedThisSession}")
println("Top Performer: ${report.topPerformingRules.first().action}")
```

---

## 🎯 Next Steps

### Explore Code Structure
```
SA-AIHOS/
├── app/src/main/kotlin/com/aihos/
│   ├── ai/
│   │   ├── memory/          ← What AI remembers
│   │   ├── reasoning/       ← How AI thinks
│   │   ├── reflection/      ← How AI learns
│   │   ├── evolution/       ← How AI improves
│   │   └── autonomy/        ← Decision orchestration
│   ├── data/
│   │   ├── db/              ← Database (Room)
│   │   └── repository/      ← Data access layer
│   ├── ui/                  ← Jetpack Compose UI
│   └── di/                  ← Dependency injection (Hilt)
├── docs/
│   ├── ARCHITECTURE.md      ← Complete technical docs
│   ├── QUICK_START.md       ← This file
│   └── EXTENSIONS.md        ← How to extend
└── README.md
```

### Customize for Your Domain
1. Create custom reasoning strategy in `ai/reasoning/`
2. Implement custom action executor in `di/`
3. Add domain-specific reflection rules
4. Test with new scenarios

### Integrate LLM (Phase 2)
1. Download Phi 2.7B model (GGUF format)
2. Integrate ONNX Runtime
3. Implement `LocalLLMProvider` interface
4. Update `ReasoningEngine` to use LLM

---

## 📞 Getting Help

- 📖 Read [ARCHITECTURE.md](docs/ARCHITECTURE.md) for deep dive
- 💬 Check comments in source code
- 🔍 Review test cases
- 🐛 Check logcat for detailed error messages

---

**Ready to watch AI evolve? Hit "Start" on the Dashboard! 🚀**

