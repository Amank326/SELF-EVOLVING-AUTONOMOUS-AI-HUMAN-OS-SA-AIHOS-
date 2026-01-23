# Extension Guide - SA-AIHOS

This guide explains how to extend SA-AIHOS with custom implementations and domain-specific logic.

---

## 🎯 Extension Points

### 1. Custom Reasoning Strategies

Implement specialized reasoning for your domain.

#### Example: Fitness-Focused Reasoning

```kotlin
package com.aihos.ai.reasoning.custom

import com.aihos.ai.reasoning.*

/**
 * Fitness-specific reasoning engine
 * Makes decisions about exercise, nutrition, rest
 */
class FitnessReasoningEngine : ReasoningEngine {
    
    override suspend fun generateOptions(context: ReasoningContext): List<Option> {
        val options = mutableListOf<Option>()
        
        // Check if it's workout time
        if (context.currentTime in "06:00".."08:00") {
            options.add(Option(
                id = "fit_1",
                action = "suggest_morning_workout",
                expectedOutcome = "User starts fitness routine",
                riskLevel = RiskLevel.LOW
            ))
        }
        
        // Check if user needs hydration reminder
        if (context.appUsageDurationMinutes > 60 && !context.isCharging) {
            options.add(Option(
                id = "fit_2",
                action = "send_hydration_reminder",
                expectedOutcome = "User drinks water",
                riskLevel = RiskLevel.LOW
            ))
        }
        
        // Always include neutral option
        options.add(Option(
            id = "fit_3",
            action = "do_nothing",
            expectedOutcome = "Observe without intervening",
            riskLevel = RiskLevel.LOW
        ))
        
        return options
    }
    
    override suspend fun scoreOption(option: Option, context: ReasoningContext): Float {
        return when (option.action) {
            "suggest_morning_workout" -> {
                // Score high if it's morning and user usually exercises then
                if (context.currentTime in "06:00".."08:00") 0.85f else 0.2f
            }
            "send_hydration_reminder" -> {
                // Score based on usage duration and battery
                val usageScore = (context.appUsageDurationMinutes / 120f).coerceIn(0f, 1f)
                val batteryScore = (context.batteryPercent / 100f)
                (usageScore * 0.6f + batteryScore * 0.4f) * 0.8f
            }
            "do_nothing" -> 0.3f
            else -> 0.5f
        }
    }
    
    override suspend fun explainDecision(
        chosen: Option,
        alternatives: List<Option>,
        context: ReasoningContext
    ): String {
        return """
            Fitness Decision: ${chosen.action}
            
            Context:
            - Time: ${context.currentTime}
            - Duration: ${context.appUsageDurationMinutes} min
            - Battery: ${context.batteryPercent}%
            
            Reasoning:
            This option chosen because it aligns with fitness goals at this time.
            
            Alternative options considered:
            ${alternatives.joinToString("\n") { "- ${it.action} (risk: ${it.riskLevel})" }}
        """.trimIndent()
    }
}
```

#### Usage in DI:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object CustomReasoningModule {
    
    @Provides
    @Singleton
    fun provideReasoningEngine(): ReasoningEngine {
        return FitnessReasoningEngine() // Swap in custom engine
    }
}
```

---

### 2. Custom LLM Providers

Integrate different language models.

#### Example: Phi 2.7B Integration

```kotlin
package com.aihos.ai.reasoning.llm

import com.aihos.ai.reasoning.*
import com.microsoft.onnxruntime.OrtSession
import com.microsoft.onnxruntime.OrtEnvironment

/**
 * Phi 2.7B model provider using ONNX Runtime
 */
class Phi27BProvider(
    private val modelPath: String
) : LocalLLMProvider {
    
    private val env = OrtEnvironment.getEnvironment()
    private var session: OrtSession? = null
    
    init {
        try {
            session = env.createSession(modelPath)
            Timber.i("Phi 2.7B model loaded from $modelPath")
        } catch (e: Exception) {
            Timber.e(e, "Failed to load Phi 2.7B model")
        }
    }
    
    override suspend fun generate(
        prompt: String,
        context: Map<String, String>,
        maxTokens: Int,
        temperature: Float
    ): LLMResponse {
        if (session == null) {
            return LLMResponse(
                text = "Model not loaded",
                tokensUsed = 0,
                processingTimeMs = 0,
                confidence = 0f
            )
        }
        
        return try {
            val startTime = System.currentTimeMillis()
            
            // Tokenize input
            val tokens = tokenize(prompt)
            
            // Run inference
            val outputTokens = mutableListOf<Int>()
            var tokenCount = 0
            
            while (tokenCount < maxTokens) {
                // Run ONNX model (simplified)
                val output = session!!.run(
                    mapOf("input" to tokens)
                )
                
                val logits = output[0].value as FloatArray
                val nextToken = selectToken(logits, temperature)
                
                outputTokens.add(nextToken)
                tokenCount++
                
                if (nextToken == 2) break  // EOS token
            }
            
            val generatedText = detokenize(outputTokens)
            val processingTime = System.currentTimeMillis() - startTime
            
            LLMResponse(
                text = generatedText,
                tokensUsed = tokenCount,
                processingTimeMs = processingTime,
                confidence = 0.75f
            )
        } catch (e: Exception) {
            Timber.e(e, "Phi 2.7B generation failed")
            LLMResponse(
                text = "Generation error",
                tokensUsed = 0,
                processingTimeMs = 0,
                confidence = 0f
            )
        }
    }
    
    override fun isAvailable(): Boolean = session != null
    
    override fun getCapabilities(): LLMCapabilities {
        return LLMCapabilities(
            maxContextLength = 2048,
            maxOutputTokens = 512,
            supportsStreaming = false,
            supportsFunctionCalling = false,
            approxLatencyMs = 500
        )
    }
    
    override fun getModelName(): String = "Phi-2.7B"
    
    private fun tokenize(text: String): FloatArray {
        // TODO: Implement actual tokenization
        return FloatArray(256) { 0f }
    }
    
    private fun detokenize(tokens: List<Int>): String {
        // TODO: Implement actual detokenization
        return tokens.joinToString(" ")
    }
    
    private fun selectToken(logits: FloatArray, temperature: Float): Int {
        // Sample from logits distribution
        val probabilities = softmax(logits, temperature)
        return sampleFromDistribution(probabilities)
    }
    
    private fun softmax(logits: FloatArray, temperature: Float): FloatArray {
        val scaled = logits.map { it / temperature }.toFloatArray()
        val maxLogit = scaled.maxOrNull() ?: 0f
        val exp = scaled.map { kotlin.math.exp(it - maxLogit) }.toFloatArray()
        val sum = exp.sum()
        return exp.map { it / sum }.toFloatArray()
    }
    
    private fun sampleFromDistribution(probs: FloatArray): Int {
        val random = kotlin.random.Random.nextFloat()
        var cumulative = 0f
        probs.forEachIndexed { index, prob ->
            cumulative += prob
            if (random <= cumulative) return index
        }
        return probs.size - 1
    }
}
```

#### Usage:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object LLMModule {
    
    @Provides
    @Singleton
    fun provideLLMProvider(): LocalLLMProvider {
        return Phi27BProvider("/sdcard/models/phi-2.7b.onnx")
    }
}
```

---

### 3. Custom Reflection Analyzers

Implement domain-specific learning logic.

#### Example: Academic Productivity Reflection

```kotlin
package com.aihos.ai.reflection.custom

import com.aihos.ai.reflection.*
import com.aihos.ai.reasoning.DecisionRecord
import com.aihos.ai.memory.Outcome

/**
 * Reflection analyzer for academic/study scenarios
 */
class AcademicReflectionEngine : ReflectionEngine {
    
    override suspend fun analyzeOutcome(
        decision: DecisionRecord,
        actualOutcome: Outcome,
        outcomeFeedback: String
    ): ReflectionResult {
        
        val insights = mutableListOf<Insight>()
        
        // Check if decision was about study timing
        if (decision.chosenOption.action.contains("study")) {
            when (actualOutcome) {
                Outcome.SUCCESS -> {
                    insights.add(Insight(
                        id = "acad_1",
                        type = InsightType.SUCCESS_PATTERN,
                        description = "Study suggestion worked at ${decision.context.currentTime}",
                        importance = 0.8f
                    ))
                }
                Outcome.FAILURE -> {
                    insights.add(Insight(
                        id = "acad_2",
                        type = InsightType.TIMING_ISSUE,
                        description = "Study suggestion failed at ${decision.context.currentTime}. " +
                                    "Check if exam pressure or energy levels different.",
                        importance = 0.7f
                    ))
                }
                else -> {}
            }
        }
        
        // Analyze user feedback for explicit preferences
        if (outcomeFeedback.contains("too early", ignoreCase = true)) {
            insights.add(Insight(
                id = "acad_3",
                type = InsightType.USER_PREFERENCE,
                description = "User prefers later study sessions",
                importance = 0.9f
            ))
        }
        
        if (outcomeFeedback.contains("perfect timing", ignoreCase = true)) {
            insights.add(Insight(
                id = "acad_4",
                type = InsightType.SUCCESS_PATTERN,
                description = "Perfect timing for user. Repeat this pattern.",
                importance = 0.95f
            ))
        }
        
        return ReflectionResult(
            decisionId = decision.id,
            expectedOutcome = decision.chosenOption.expectedOutcome,
            actualOutcome = actualOutcome,
            outcomeCorrect = actualOutcome == Outcome.SUCCESS,
            confidenceInAnalysis = 0.8f,
            insights = insights,
            recommendation = when (actualOutcome) {
                Outcome.SUCCESS -> "Reinforce this decision pattern"
                Outcome.FAILURE -> "Adjust timing or approach"
                else -> "Monitor for more data"
            }
        )
    }
    
    override suspend fun identifyPatterns(decisions: List<DecisionRecord>): List<Pattern> {
        // Group by time of day
        val morningDecisions = decisions.filter { 
            it.context.currentTime < "12:00"
        }
        
        val eveningDecisions = decisions.filter { 
            it.context.currentTime >= "18:00"
        }
        
        val patterns = mutableListOf<Pattern>()
        
        if (morningDecisions.isNotEmpty()) {
            patterns.add(Pattern(
                id = "pat_morning",
                condition = "time < 12:00 AND type=study",
                action = "suggest_study_session",
                outcomeFrequency = countOutcomes(morningDecisions),
                confidenceLevel = 0.7f
            ))
        }
        
        if (eveningDecisions.isNotEmpty()) {
            patterns.add(Pattern(
                id = "pat_evening",
                condition = "time >= 18:00 AND type=study",
                action = "suggest_review_session",
                outcomeFrequency = countOutcomes(eveningDecisions),
                confidenceLevel = 0.65f
            ))
        }
        
        return patterns
    }
    
    override suspend fun validateAssumptions(reflection: ReflectionResult): List<AssumptionValidation> {
        return listOf(
            AssumptionValidation(
                assumption = "Study suggestions improve learning",
                wasCorrect = reflection.outcomeCorrect,
                evidence = when (reflection.actualOutcome) {
                    Outcome.SUCCESS -> "User started studying"
                    Outcome.FAILURE -> "User ignored suggestion"
                    else -> "Unknown"
                }
            ),
            AssumptionValidation(
                assumption = "Morning is better for complex topics",
                wasCorrect = true, // This would be calculated from patterns
                evidence = "Historical data shows higher success in morning",
                suggestionIfWrong = "Try suggesting complex topics at user's peak energy time"
            )
        )
    }
    
    private fun countOutcomes(decisions: List<DecisionRecord>): Map<Outcome, Int> {
        // This would be enhanced with actual tracking
        return mapOf(
            Outcome.SUCCESS to 5,
            Outcome.PARTIAL to 2,
            Outcome.FAILURE to 1
        )
    }
}
```

---

### 4. Custom Action Executors

Implement domain-specific actions.

#### Example: Wellness Actions

```kotlin
package com.aihos.ai.autonomy.custom

import com.aihos.ai.autonomy.ActionExecutor
import com.aihos.ai.reasoning.DecisionRecord
import timber.log.Timber

class WellnessActionExecutor : ActionExecutor {
    
    override suspend fun execute(action: String): Boolean {
        return when (action) {
            "suggest_stretching" -> executeSuggestStretching()
            "send_water_reminder" -> executeSendWaterReminder()
            "play_calming_music" -> executePlayCalmingMusic()
            "suggest_meditation" -> executeSuggestMeditation()
            "notify_posture_check" -> executeNotifyPostureCheck()
            else -> {
                Timber.w("Unknown wellness action: $action")
                false
            }
        }
    }
    
    override suspend fun requestUserApproval(decision: DecisionRecord): Boolean {
        // Show decision request notification
        Timber.d("Requesting approval: ${decision.chosenOption.action}")
        // TODO: Show notification, wait for user response
        return true
    }
    
    private suspend fun executeSuggestStretching(): Boolean {
        Timber.i("Suggesting stretching routine")
        // TODO: Show stretching exercise guide
        // TODO: Track user engagement
        return true
    }
    
    private suspend fun executeSendWaterReminder(): Boolean {
        Timber.i("Sending hydration reminder")
        // TODO: Show notification
        // TODO: Track if user drank water
        return true
    }
    
    private suspend fun executePlayCalmingMusic(): Boolean {
        Timber.i("Playing calming music")
        // TODO: Start music playback
        return true
    }
    
    private suspend fun executeSuggestMeditation(): Boolean {
        Timber.i("Suggesting meditation session")
        // TODO: Show meditation guide
        return true
    }
    
    private suspend fun executeNotifyPostureCheck(): Boolean {
        Timber.i("Notifying posture check")
        // TODO: Show posture reminder
        return true
    }
}
```

---

## 🔧 Integration Steps

### Step 1: Create Your Custom Class

```kotlin
// In: app/src/main/kotlin/com/aihos/custom/
class MyCustomReasoningEngine : ReasoningEngine { ... }
```

### Step 2: Update Hilt Module

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object CustomModule {
    
    @Provides
    @Singleton
    fun provideReasoningEngine(): ReasoningEngine {
        return MyCustomReasoningEngine()
    }
}
```

### Step 3: Test

```kotlin
@RunWith(AndroidJUnit4::class)
class CustomReasoningTest {
    
    @get:Rule
    val hiltRule = HiltAndroidRule(this)
    
    @Inject
    lateinit var reasoningEngine: ReasoningEngine
    
    @Test
    fun testCustomReasoning() = runBlocking {
        val options = reasoningEngine.generateOptions(testContext)
        assertTrue(options.isNotEmpty())
    }
}
```

---

## 📦 Creating a Domain Plugin

Structure for a reusable domain extension:

```
MyDomainPlugin/
├── build.gradle.kts
├── src/main/kotlin/
│   └── com/aihos/domains/mydomain/
│       ├── reasoning/
│       │   └── MyDomainReasoningEngine.kt
│       ├── reflection/
│       │   └── MyDomainReflectionEngine.kt
│       ├── actions/
│       │   └── MyDomainActionExecutor.kt
│       └── MyDomainModule.kt
└── README.md
```

Add to SA-AIHOS:

```kotlin
// In settings.gradle.kts
include(":my-domain-plugin")

// In app/build.gradle.kts
dependencies {
    implementation(project(":my-domain-plugin"))
}
```

---

## 🎓 Best Practices

1. **Start with reflection**: Understand what data you have access to
2. **Test extensively**: Custom logic can have bugs
3. **Log everything**: Use Timber for debugging
4. **Handle errors gracefully**: Return sensible defaults
5. **Document assumptions**: Make your reasoning clear
6. **Version your rules**: Track evolution changes
7. **Monitor performance**: LLM calls are expensive

---

## 🚀 What's Next?

- Implement custom reasoning for your domain
- Create domain-specific reflection rules
- Integrate local LLM for advanced reasoning
- Build action executors that make sense for your use case
- Share your domain plugin!

