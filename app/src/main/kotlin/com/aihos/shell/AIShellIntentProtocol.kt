package com.aihos.shell

import android.content.Context
import android.content.Intent
import android.os.Bundle
import kotlinx.serialization.Serializable
import timber.log.Timber

/**
 * AIShellIntentProtocol: Inter-App Communication Standard
 *
 * Defines the Intent-based protocol for other apps to communicate with the AI shell.
 * This allows third-party apps to:
 * - Ask the AI questions
 * - Request AI assistance
 * - Get AI status/insights
 * - Provide feedback for learning
 *
 * Design Philosophy:
 * - Standard Android Intent mechanism (no custom protocols)
 * - Asynchronous communication (no blocking calls)
 * - Broadcast results via callback Intent
 * - Safe and permission-controlled
 *
 * Example Usage (from another app):
 *
 *   val intent = Intent("com.aihos.shell.ACTION_ASK_AI")
 *   intent.putExtra("question", "What's the weather?")
 *   intent.putExtra("callbackIntent", callbackIntent)
 *   startService(intent)
 *
 * Protocol Actions:
 * 1. ASK_AI - Ask the AI a question
 * 2. GET_STATUS - Get current AI shell status
 * 3. REQUEST_ACTION - Request AI to perform action
 * 4. LEARN_FEEDBACK - Provide feedback for AI learning
 * 5. QUERY_CAPABILITY - Check if AI can do something
 * 6. SUBSCRIBE_UPDATES - Get AI updates via broadcast
 */

/**
 * Standard Intent actions for AI Shell protocol
 */
object AIShellIntentActions {
    const val ACTION_ASK_AI = "com.aihos.shell.ACTION_ASK_AI"
    const val ACTION_GET_STATUS = "com.aihos.shell.ACTION_GET_STATUS"
    const val ACTION_REQUEST_ACTION = "com.aihos.shell.ACTION_REQUEST_ACTION"
    const val ACTION_LEARN_FEEDBACK = "com.aihos.shell.ACTION_LEARN_FEEDBACK"
    const val ACTION_QUERY_CAPABILITY = "com.aihos.shell.ACTION_QUERY_CAPABILITY"
    const val ACTION_SUBSCRIBE_UPDATES = "com.aihos.shell.ACTION_SUBSCRIBE_UPDATES"
}

/**
 * Extra keys for Intent communication
 */
object AIShellIntentExtras {
    // Input extras
    const val EXTRA_QUESTION = "question"
    const val EXTRA_ACTION_TYPE = "actionType"
    const val EXTRA_ACTION_DESCRIPTION = "actionDescription"
    const val EXTRA_FEEDBACK = "feedback"
    const val EXTRA_FEEDBACK_TYPE = "feedbackType"
    const val EXTRA_CAPABILITY_NAME = "capabilityName"
    const val EXTRA_CALLBACK_INTENT = "callbackIntent"

    // Output extras
    const val EXTRA_RESPONSE = "response"
    const val EXTRA_CONFIDENCE = "confidence"
    const val EXTRA_STATUS = "status"
    const val EXTRA_AI_STATE = "aiState"
    const val EXTRA_PROCESSING_TIME_MS = "processingTimeMs"
    const val EXTRA_SHOULD_NOTIFY = "shouldNotify"
    const val EXTRA_SUCCESS = "success"
    const val EXTRA_ERROR_MESSAGE = "errorMessage"
}

/**
 * Helper class for building Intent requests
 */
class AIShellIntentBuilder(private val context: Context) {
    /**
     * Build an ASK_AI intent
     */
    fun buildAskAIIntent(
        question: String,
        callbackIntent: Intent? = null
    ): Intent {
        return Intent(AIShellIntentActions.ACTION_ASK_AI).apply {
            setPackage(context.packageName)
            putExtra(AIShellIntentExtras.EXTRA_QUESTION, question)
            callbackIntent?.let {
                putExtra(AIShellIntentExtras.EXTRA_CALLBACK_INTENT, it)
            }
        }
    }

    /**
     * Build a GET_STATUS intent
     */
    fun buildGetStatusIntent(): Intent {
        return Intent(AIShellIntentActions.ACTION_GET_STATUS).apply {
            setPackage(context.packageName)
        }
    }

    /**
     * Build a REQUEST_ACTION intent
     */
    fun buildRequestActionIntent(
        actionType: String,
        description: String? = null,
        callbackIntent: Intent? = null
    ): Intent {
        return Intent(AIShellIntentActions.ACTION_REQUEST_ACTION).apply {
            setPackage(context.packageName)
            putExtra(AIShellIntentExtras.EXTRA_ACTION_TYPE, actionType)
            description?.let {
                putExtra(AIShellIntentExtras.EXTRA_ACTION_DESCRIPTION, it)
            }
            callbackIntent?.let {
                putExtra(AIShellIntentExtras.EXTRA_CALLBACK_INTENT, it)
            }
        }
    }

    /**
     * Build a LEARN_FEEDBACK intent
     */
    fun buildLearnFeedbackIntent(
        feedback: String,
        feedbackType: String = "general"
    ): Intent {
        return Intent(AIShellIntentActions.ACTION_LEARN_FEEDBACK).apply {
            setPackage(context.packageName)
            putExtra(AIShellIntentExtras.EXTRA_FEEDBACK, feedback)
            putExtra(AIShellIntentExtras.EXTRA_FEEDBACK_TYPE, feedbackType)
        }
    }

    /**
     * Build a QUERY_CAPABILITY intent
     */
    fun buildQueryCapabilityIntent(capabilityName: String): Intent {
        return Intent(AIShellIntentActions.ACTION_QUERY_CAPABILITY).apply {
            setPackage(context.packageName)
            putExtra(AIShellIntentExtras.EXTRA_CAPABILITY_NAME, capabilityName)
        }
    }
}

/**
 * Helper class for parsing Intent responses
 */
class AIShellIntentParser {
    /**
     * Parse response from AI Shell
     */
    fun parseResponse(resultIntent: Intent?): AIShellIntentResult {
        if (resultIntent == null) {
            return AIShellIntentResult(
                success = false,
                errorMessage = "No response received"
            )
        }

        return AIShellIntentResult(
            success = resultIntent.getBooleanExtra(AIShellIntentExtras.EXTRA_SUCCESS, false),
            response = resultIntent.getStringExtra(AIShellIntentExtras.EXTRA_RESPONSE) ?: "",
            confidence = resultIntent.getFloatExtra(AIShellIntentExtras.EXTRA_CONFIDENCE, 0f),
            processingTimeMs = resultIntent.getLongExtra(AIShellIntentExtras.EXTRA_PROCESSING_TIME_MS, 0L),
            shouldNotify = resultIntent.getBooleanExtra(AIShellIntentExtras.EXTRA_SHOULD_NOTIFY, false),
            errorMessage = resultIntent.getStringExtra(AIShellIntentExtras.EXTRA_ERROR_MESSAGE) ?: ""
        )
    }

    /**
     * Parse status from GET_STATUS response
     */
    fun parseStatus(resultIntent: Intent?): AIShellStatusInfo? {
        if (resultIntent == null) return null

        return AIShellStatusInfo(
            state = resultIntent.getStringExtra(AIShellIntentExtras.EXTRA_AI_STATE) ?: "unknown",
            statusText = resultIntent.getStringExtra(AIShellIntentExtras.EXTRA_STATUS) ?: "unknown",
            isRunning = resultIntent.getBooleanExtra(AIShellIntentExtras.EXTRA_SUCCESS, false)
        )
    }
}

/**
 * Result from AI Shell intent
 */
@Serializable
data class AIShellIntentResult(
    val success: Boolean,
    val response: String = "",
    val confidence: Float = 0f,
    val processingTimeMs: Long = 0L,
    val shouldNotify: Boolean = false,
    val errorMessage: String = ""
)

/**
 * Status information from GET_STATUS
 */
@Serializable
data class AIShellStatusInfo(
    val state: String,
    val statusText: String,
    val isRunning: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Intent handler for AI Shell service
 */
class AIShellIntentHandler(
    private val shellController: AIShellController?
) {
    /**
     * Handle incoming intent and return response
     */
    suspend fun handleIntent(intent: Intent): Intent {
        Timber.i("🐚 Handling intent: ${intent.action}")

        if (shellController == null) {
            Timber.e("🐚 Shell controller not available")
            return createErrorResponse(intent, "AI Shell not available")
        }

        return when (intent.action) {
            AIShellIntentActions.ACTION_ASK_AI -> {
                handleAskAI(intent, shellController)
            }
            AIShellIntentActions.ACTION_GET_STATUS -> {
                handleGetStatus(intent, shellController)
            }
            AIShellIntentActions.ACTION_REQUEST_ACTION -> {
                handleRequestAction(intent, shellController)
            }
            AIShellIntentActions.ACTION_LEARN_FEEDBACK -> {
                handleLearnFeedback(intent, shellController)
            }
            AIShellIntentActions.ACTION_QUERY_CAPABILITY -> {
                handleQueryCapability(intent, shellController)
            }
            else -> createErrorResponse(intent, "Unknown action: ${intent.action}")
        }
    }

    /**
     * Handle ASK_AI intent
     */
    private suspend fun handleAskAI(intent: Intent, controller: AIShellController): Intent {
        val question = intent.getStringExtra(AIShellIntentExtras.EXTRA_QUESTION) ?: ""
        Timber.i("🐚 Question: $question")

        val startTime = System.currentTimeMillis()
        val result = controller.requestAIAction(AIAction.AnswerQuestion(question))
        val processingTime = System.currentTimeMillis() - startTime

        return Intent().apply {
            putExtra(AIShellIntentExtras.EXTRA_SUCCESS, result.success)
            putExtra(AIShellIntentExtras.EXTRA_RESPONSE, result.result)
            putExtra(AIShellIntentExtras.EXTRA_CONFIDENCE, result.confidence)
            putExtra(AIShellIntentExtras.EXTRA_PROCESSING_TIME_MS, processingTime)
            putExtra(AIShellIntentExtras.EXTRA_SHOULD_NOTIFY, result.shouldNotify)
        }
    }

    /**
     * Handle GET_STATUS intent
     */
    private suspend fun handleGetStatus(intent: Intent, controller: AIShellController): Intent {
        val status = controller.getShellStatus()

        return Intent().apply {
            putExtra(AIShellIntentExtras.EXTRA_SUCCESS, true)
            putExtra(AIShellIntentExtras.EXTRA_AI_STATE, status.shellState.name)
            putExtra(AIShellIntentExtras.EXTRA_STATUS, "State: ${status.shellState}")
        }
    }

    /**
     * Handle REQUEST_ACTION intent
     */
    private suspend fun handleRequestAction(intent: Intent, controller: AIShellController): Intent {
        val actionType = intent.getStringExtra(AIShellIntentExtras.EXTRA_ACTION_TYPE) ?: ""
        val description = intent.getStringExtra(AIShellIntentExtras.EXTRA_ACTION_DESCRIPTION) ?: ""
        Timber.i("🐚 Requested action: $actionType - $description")

        val result = controller.requestAIAction(
            AIAction.PerformTask(description.ifEmpty { actionType })
        )

        return Intent().apply {
            putExtra(AIShellIntentExtras.EXTRA_SUCCESS, result.success)
            putExtra(AIShellIntentExtras.EXTRA_RESPONSE, result.result)
            putExtra(AIShellIntentExtras.EXTRA_CONFIDENCE, result.confidence)
        }
    }

    /**
     * Handle LEARN_FEEDBACK intent
     */
    private suspend fun handleLearnFeedback(intent: Intent, controller: AIShellController): Intent {
        val feedback = intent.getStringExtra(AIShellIntentExtras.EXTRA_FEEDBACK) ?: ""
        val feedbackType = intent.getStringExtra(AIShellIntentExtras.EXTRA_FEEDBACK_TYPE) ?: "general"
        Timber.i("🐚 Learning feedback: $feedback ($feedbackType)")

        val result = controller.requestAIAction(AIAction.LearnFromFeedback(feedback))

        return Intent().apply {
            putExtra(AIShellIntentExtras.EXTRA_SUCCESS, result.success)
            putExtra(AIShellIntentExtras.EXTRA_RESPONSE, "Feedback incorporated")
        }
    }

    /**
     * Handle QUERY_CAPABILITY intent
     */
    private suspend fun handleQueryCapability(intent: Intent, controller: AIShellController): Intent {
        val capability = intent.getStringExtra(AIShellIntentExtras.EXTRA_CAPABILITY_NAME) ?: ""
        Timber.i("🐚 Checking capability: $capability")

        // For now, assume all capabilities are available
        val isCapable = when (capability.lowercase()) {
            "answer_question" -> true
            "perform_task" -> true
            "get_insight" -> true
            "suggest_action" -> true
            "learn_feedback" -> true
            else -> false
        }

        return Intent().apply {
            putExtra(AIShellIntentExtras.EXTRA_SUCCESS, isCapable)
            putExtra(AIShellIntentExtras.EXTRA_RESPONSE, if (isCapable) "Capable" else "Not capable")
        }
    }

    /**
     * Create error response
     */
    private fun createErrorResponse(intent: Intent, message: String): Intent {
        Timber.e("🐚 Error: $message")
        return Intent().apply {
            putExtra(AIShellIntentExtras.EXTRA_SUCCESS, false)
            putExtra(AIShellIntentExtras.EXTRA_ERROR_MESSAGE, message)
        }
    }
}

/**
 * Utility class for third-party apps to use
 */
class AIShellClient(private val context: Context) {
    private val intentBuilder = AIShellIntentBuilder(context)
    private val intentParser = AIShellIntentParser()

    /**
     * Ask the AI a question
     * Results will be broadcast via callback intent
     */
    fun askAI(question: String, onResult: (AIShellIntentResult) -> Unit) {
        val intent = intentBuilder.buildAskAIIntent(question)
        Timber.i("🐚 Sending question to AI: $question")
        context.startService(intent)
    }

    /**
     * Get AI Shell status
     */
    fun getStatus(onStatus: (AIShellStatusInfo?) -> Unit) {
        val intent = intentBuilder.buildGetStatusIntent()
        Timber.i("🐚 Requesting AI status")
        context.startService(intent)
    }

    /**
     * Request AI to perform an action
     */
    fun requestAction(
        actionType: String,
        description: String? = null,
        onResult: (AIShellIntentResult) -> Unit
    ) {
        val intent = intentBuilder.buildRequestActionIntent(actionType, description)
        Timber.i("🐚 Requesting AI action: $actionType")
        context.startService(intent)
    }

    /**
     * Provide feedback to AI for learning
     */
    fun provideFeedback(feedback: String, feedbackType: String = "general") {
        val intent = intentBuilder.buildLearnFeedbackIntent(feedback, feedbackType)
        Timber.i("🐚 Sending feedback to AI: $feedback")
        context.startService(intent)
    }

    /**
     * Check if AI can do something
     */
    fun canAIDo(capability: String): Boolean {
        val intent = intentBuilder.buildQueryCapabilityIntent(capability)
        Timber.i("🐚 Querying capability: $capability")
        context.startService(intent)
        // In real implementation, would get async response
        return true
    }
}
