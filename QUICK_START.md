# SA-AIHOS Quick Start Guide

## What is SA-AIHOS?

**SA-AIHOS** (Self-Evolving Autonomous AI-Human OS) is a research-grade Android system that demonstrates how AI can:

- **Think** through problems using reasoning layers
- **Act** autonomously within your defined constraints
- **Reflect** on its own decisions and performance
- **Evolve** by continuously improving its own rules

It's **not** a chatbot. It's an experiment in self-improving AI running locally on your Android device.

---

## Quick Setup (2 minutes)

### Prerequisites
- Android Studio Iguana+
- Android SDK 34+
- JDK 17+

### Installation

```bash
# Clone the repository
git clone https://github.com/Amank326/SELF-EVOLVING-AUTONOMOUS-AI-HUMAN-OS-SA-AIHOS-.git
cd SA-AIHOS

# Build
./gradlew build

# Run on emulator/device
./gradlew installDebug
```

### First Launch

1. App opens to **Dashboard** screen
2. You'll see:
   - **Memory Stats**: How much the AI has learned
   - **Reasoning Rules**: Decision rules currently active
   - **Autonomy Level**: How much AI can act independently
   - **Evolution Metrics**: How much the AI has improved

---

## Core Screens

### 🧠 Dashboard
- Overview of AI system state
- Current autonomy level
- Recent decisions
- System health metrics

### 💾 Memory Screen
- View stored memories (episodic, semantic, etc)
- See what the AI remembers
- Manage memory (add, edit, delete)
- Memory search

### 🔄 Evolution Screen
- Watch AI improve over time
- See learned patterns
- Track generation number
- Review evolutionary improvements

### ⚙️ Settings Screen
- Set autonomy level (0 = manual, 1 = full autonomy)
- Configure safety constraints
- Enable/disable AI features
- View/export logs

---

## Understanding Autonomy Levels

| Level | Behavior | When to Use |
|-------|----------|------------|
| **0.0** (Manual) | AI only advises, you decide | Learning phase, high risk |
| **0.25** (Advisory) | AI suggests, you approve | Getting comfortable |
| **0.5** (Assisted) | AI helps with your actions | Daily use |
| **0.75** (Semi) | AI acts with your oversight | Trusted scenarios |
| **1.0** (Full) | AI fully autonomous | Proven reliability |

---

## How the AI Learns

### The Cycle: THINK → ACT → REFLECT → EVOLVE

```
1. THINK
   The AI analyzes the situation using Memory and Reasoning layers
   It retrieves relevant memories and applies reasoning rules

2. ACT
   The Autonomy Controller decides what to do
   It executes the action (or requests your permission first)

3. REFLECT
   The Reflection Layer examines what happened
   It identifies patterns and discovers insights

4. EVOLVE
   The Evolution Engine learns from outcomes
   It updates reasoning rules and strategy
```

### Example: Learning from Experience

```
Situation: User feels anxious at 8 PM
└─ THINK: Why might this be? (Reasoning)
└─ ACT: Suggest meditation (if autonomy allows)
└─ REFLECT: Did it help? What patterns emerge?
└─ EVOLVE: Update rule "evening anxiety → suggest meditation"
```

---

## Typical Usage Patterns

### Pattern 1: Advisory Mode
```
You: What should I do about X?
AI: Based on your history, option A seems best because...
You: OK, I'll do that
AI: Great, I'll remember this
```

### Pattern 2: Assisted Mode
```
You: [doing an activity]
AI: I notice you usually need a break now - take 5 min?
You: Good call, thanks!
AI: Remembered that pattern
```

### Pattern 3: Autonomous Mode
```
AI: [takes action you've pre-approved]
[checks outcome]
AI: That worked! Updated my rule for similar situations
```

---

## Memory Types Explained

The AI stores different types of memories:

### 📝 Episodic
"Specific events and experiences"
- User did X on Tuesday at 3 PM
- Outcome was Y
- Environment was Z

### 📚 Semantic  
"Facts and knowledge"
- Exercise reduces anxiety
- Morning is best for creative work
- Breaks improve focus

### 🛠️ Procedural
"Skills and methods"
- How to do X (step by step)
- When to apply method Y
- Resources needed for Z

### 💝 Emotional
"Feelings and preferences"
- User prefers morning conversations
- Dislikes sudden interruptions
- Values privacy highly

### 🌍 Contextual
"Situations and environments"
- Working from home vs. office
- Quiet environment aids focus
- Interruptions trigger anxiety

---

## Privacy & Security

✅ **All data stays on your device**
- No cloud storage
- No data collection
- No analytics
- No tracking

✅ **You control everything**
- See all AI memories
- Delete any memory anytime
- Export your data
- Revert to manual mode anytime

✅ **No paid APIs or dependencies**
- Works completely offline
- No subscription needed
- No external API calls
- Pure local processing

---

## Architecture at a Glance

```
┌─────────────────────────────┐
│      UI (Compose)           │
├─────────────────────────────┤
│   ViewModel (MVVM)          │
├─────────────────────────────┤
│  AI Layers:                 │
│  • Memory Layer             │
│  • Reasoning Layer          │
│  • Reflection Layer         │
│  • Evolution Engine         │
│  • Autonomy Controller      │
├─────────────────────────────┤
│  Repositories (Clean Arch)  │
├─────────────────────────────┤
│  Room Database (Local)      │
└─────────────────────────────┘
```

---

## Key Features

🧠 **Self-Improving**
- Learns from every interaction
- Updates its own decision rules
- No external training required

🔍 **Explainable**
- Every decision shows reasoning
- Confidence levels visible
- Can explain its thinking

🔒 **Private**
- All processing local
- No cloud or API calls
- You own your data

⚖️ **Balanced Autonomy**
- You set the level
- Transparent decision making
- Easy to override

📊 **Observable**
- Track AI improvement
- View memory and rules
- Monitor evolution progress

---

## Troubleshooting

### AI not learning
- Check autonomy level
- Make sure feedback is provided
- Verify memories are being stored

### Memory seems incorrect
- AI might be over-generalizing
- Give corrective feedback
- Lower autonomy level temporarily

### Want to reset
- Settings → Clear All Data
- Starts fresh with default configuration

---

## Next Steps

1. **Set autonomy level** to your comfort (start at 0.5)
2. **Give the AI tasks** it can learn from
3. **Provide feedback** on outcomes (good/bad)
4. **Watch it improve** as it learns patterns
5. **Gradually increase autonomy** as you trust it more

---

## Want to Contribute?

Areas for contribution:
- [ ] Semantic similarity search
- [ ] Local embeddings (ONNX)
- [ ] Advanced reasoning
- [ ] UI improvements
- [ ] Testing & feedback

See [ARCHITECTURE_GUIDE.md](ARCHITECTURE_GUIDE.md) for technical details.

---

## Questions?

Check these resources:
- **Architecture**: [ARCHITECTURE_GUIDE.md](ARCHITECTURE_GUIDE.md)
- **Code**: Browse the repository
- **Issues**: GitHub Issues

---

**Remember**: This is research-grade software. Use at your own risk, but enjoy the journey of building a self-improving AI! 🚀
