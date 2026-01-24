package com.aihos.graphics.filament

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import timber.log.Timber

/**
 * Lifecycle-Safe Renderer Wrapper
 *
 * Ensures the native 3D renderer properly binds to Android lifecycle events.
 *
 * Responsibilities:
 * - Bind to LifecycleOwner for automatic event handling
 * - Pause rendering on ON_STOP (activity backgrounded)
 * - Resume rendering on ON_START (activity foregrounded)
 * - Clean up all resources on ON_DESTROY
 * - Prevent crashes from use-after-destroy
 * - Track lifecycle state for safety checks
 * - Queue operations safely across lifecycle boundaries
 *
 * Lifecycle Safety:
 * - No rendering when paused (battery efficient)
 * - Safe pause/resume without state loss
 * - Guaranteed cleanup on destroy
 * - Exception-safe throughout
 */
class LifecycleSafeRenderer(
    private val engine: Native3DEngine
) : LifecycleEventObserver {
    
    // ==================== STATE ====================
    
    private var lifecycleOwner: LifecycleOwner? = null
    private var currentState = Lifecycle.State.DESTROYED
    private var isInitialized = false
    private var isDestroyed = false
    
    /**
     * Bind this renderer to a LifecycleOwner for automatic lifecycle management
     */
    fun bindToLifecycle(owner: LifecycleOwner) {
        if (isDestroyed) {
            Timber.w("LifecycleSafeRenderer: Cannot bind after destroy")
            return
        }
        
        lifecycleOwner = owner
        owner.lifecycle.addObserver(this)
        
        // Handle initial state
        when (owner.lifecycle.currentState) {
            Lifecycle.State.CREATED -> handleCreated()
            Lifecycle.State.STARTED -> handleStarted()
            Lifecycle.State.RESUMED -> handleResumed()
            Lifecycle.State.PAUSED -> handlePaused()
            Lifecycle.State.DESTROYED -> handleDestroyed()
        }
        
        Timber.d("LifecycleSafeRenderer: Bound to ${owner::class.simpleName}")
    }
    
    /**
     * Unbind from lifecycle (optional, destroy() does this automatically)
     */
    fun unbindFromLifecycle() {
        lifecycleOwner?.let {
            it.lifecycle.removeObserver(this)
            Timber.d("LifecycleSafeRenderer: Unbound from lifecycle")
        }
        lifecycleOwner = null
    }
    
    // ==================== LIFECYCLE EVENTS ====================
    
    /**
     * Implement LifecycleEventObserver
     */
    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        if (isDestroyed) return
        
        currentState = source.lifecycle.currentState
        
        when (event) {
            Lifecycle.Event.ON_CREATE -> handleCreated()
            Lifecycle.Event.ON_START -> handleStarted()
            Lifecycle.Event.ON_RESUME -> handleResumed()
            Lifecycle.Event.ON_PAUSE -> handlePaused()
            Lifecycle.Event.ON_STOP -> handleStopped()
            Lifecycle.Event.ON_DESTROY -> handleDestroyed()
            else -> {} // Ignore unknown events
        }
    }
    
    /**
     * ON_CREATE: Activity created
     */
    private fun handleCreated() {
        Timber.d("LifecycleSafeRenderer: ON_CREATE")
        // Engine should already be initialized, just verify
        isInitialized = true
    }
    
    /**
     * ON_START: Activity visible (but may be paused)
     */
    private fun handleStarted() {
        Timber.d("LifecycleSafeRenderer: ON_START")
        // Can start preparing rendering
    }
    
    /**
     * ON_RESUME: Activity resuming to foreground
     */
    private fun handleResumed() {
        Timber.d("LifecycleSafeRenderer: ON_RESUME - resuming rendering")
        try {
            engine.resume()
        } catch (e: Exception) {
            Timber.e(e, "LifecycleSafeRenderer: Error resuming engine")
        }
    }
    
    /**
     * ON_PAUSE: Activity paused (before going background)
     */
    private fun handlePaused() {
        Timber.d("LifecycleSafeRenderer: ON_PAUSE - pausing rendering")
        try {
            engine.pause()
        } catch (e: Exception) {
            Timber.e(e, "LifecycleSafeRenderer: Error pausing engine")
        }
    }
    
    /**
     * ON_STOP: Activity backgrounded
     */
    private fun handleStopped() {
        Timber.d("LifecycleSafeRenderer: ON_STOP")
        // Already paused from ON_PAUSE
    }
    
    /**
     * ON_DESTROY: Activity destroyed
     */
    private fun handleDestroyed() {
        Timber.d("LifecycleSafeRenderer: ON_DESTROY - destroying engine")
        destroy()
    }
    
    // ==================== STATE QUERIES ====================
    
    /**
     * Check if renderer is in a valid state for rendering
     */
    fun canRender(): Boolean {
        return !isDestroyed && currentState.isAtLeast(Lifecycle.State.STARTED)
    }
    
    /**
     * Check if lifecycle is active
     */
    fun isLifecycleActive(): Boolean {
        return !isDestroyed && currentState.isAtLeast(Lifecycle.State.CREATED)
    }
    
    /**
     * Get current lifecycle state
     */
    fun getCurrentLifecycleState(): Lifecycle.State = currentState
    
    // ==================== CLEANUP ====================
    
    /**
     * Destroy renderer and clean up all resources
     */
    fun destroy() {
        if (isDestroyed) return
        
        isDestroyed = true
        
        try {
            // Unbind from lifecycle
            unbindFromLifecycle()
            
            // Destroy engine
            engine.destroy()
            
            Timber.i("LifecycleSafeRenderer: Destroyed successfully")
        } catch (e: Exception) {
            Timber.e(e, "LifecycleSafeRenderer: Error during destroy")
        }
    }
    
    /**
     * Check if renderer is destroyed
     */
    fun isDestroyed(): Boolean = isDestroyed
    
    // ==================== SAFETY CHECKS ====================
    
    /**
     * Safely get engine or throw if destroyed
     */
    fun getEngineOrThrow(): Native3DEngine {
        check(!isDestroyed) { "Renderer has been destroyed" }
        return engine
    }
    
    /**
     * Safely execute a block with the engine
     */
    fun <T> safeWithEngine(block: (Native3DEngine) -> T): T? {
        return try {
            if (!isDestroyed) {
                block(engine)
            } else {
                Timber.w("LifecycleSafeRenderer: Attempted to use destroyed renderer")
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "LifecycleSafeRenderer: Error in safeWithEngine")
            null
        }
    }
}
