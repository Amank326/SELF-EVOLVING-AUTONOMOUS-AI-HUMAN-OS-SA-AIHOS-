package com.aihos.ai.perception

import com.aihos.ai.autonomy.ContextProvider
import com.aihos.ai.reasoning.ReasoningContext
import kotlinx.coroutines.CoroutineScope
import timber.log.Timber

/**
 * EnvironmentAwareContextProvider: Integrates system signals into reasoning context
 * 
 * This decorator pattern enhances the standard ContextProvider with environmental
 * awareness, making AI reasoning context-aware of device state, battery, network,
 * time of day, and user activity level.
 * 
 * The AI can then adjust its behavior based on environmental constraints and
 * opportunities, allowing for intelligent resource management and state-dependent
 * reasoning.
 */
interface EnvironmentAwareContextProvider : ContextProvider {
    /**
     * Get reasoning context enriched with environmental awareness
     */
    suspend fun getEnvironmentAwareContext(): EnvironmentAwareReasoningContext
    
    /**
     * Get just the environment context
     */
    suspend fun getEnvironment(): EnvironmentContext
}

/**
 * Default implementation of EnvironmentAwareContextProvider
 */
class DefaultEnvironmentAwareContextProvider(
    private val baseContextProvider: ContextProvider,
    private val systemSignalsManager: SystemSignalsManager,
    private val scope: CoroutineScope
) : EnvironmentAwareContextProvider {
    
    init {
        Timber.d("EnvironmentAwareContextProvider: Initialized with system signals integration")
    }
    
    /**
     * Get base reasoning context (delegates to original provider)
     */
    override suspend fun getCurrentContext(): ReasoningContext {
        return baseContextProvider.getCurrentContext()
    }
    
    /**
     * Get reasoning context with full environmental awareness
     */
    override suspend fun getEnvironmentAwareContext(): EnvironmentAwareReasoningContext {
        val baseContext = baseContextProvider.getCurrentContext()
        val environment = systemSignalsManager.getEnvironmentContext()
        
        val contextWithEnvironment = EnvironmentAwareReasoningContext.from(
            baseContext,
            environment
        )
        
        Timber.d(
            "EnvironmentAwareContextProvider: Created context - " +
            "pressure=${if (contextWithEnvironment.isHighPressureEnvironment) "HIGH" else "NORMAL"}, " +
            "reflection=${if (contextWithEnvironment.isReflectionTime) "YES" else "NO"}, " +
            "energy=${if (contextWithEnvironment.shouldConserveEnergy) "CONSERVE" else "NORMAL"}"
        )
        
        return contextWithEnvironment
    }
    
    /**
     * Get current environment context
     */
    override suspend fun getEnvironment(): EnvironmentContext {
        return systemSignalsManager.getEnvironmentContext()
    }
}
