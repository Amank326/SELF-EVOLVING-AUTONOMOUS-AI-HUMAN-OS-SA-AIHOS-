/**
 * Lifecycle Manager
 * Manages app lifecycle events (pause, resume, destroy)
 * 
 * Responsibilities:
 * - Pause rendering when app backgrounded
 * - Stop expensive computations
 * - Release temporary resources
 * - Resume cleanly without visual glitches
 * - Cleanup on app exit
 * 
 * Integration with Android Activity lifecycle:
 * - onPause: Stop rendering, pause timers
 * - onResume: Resume rendering, reinitialize state
 * - onDestroy: Clean up all resources
 */
export class LifecycleManager {
  constructor(scene) {
    this.scene = scene;
    this.isPaused = false;
    this.isDestroyed = false;

    // Registered handlers
    this.pauseHandlers = [];
    this.resumeHandlers = [];
    this.destroyHandlers = [];

    // Pending updates during pause
    this.pendingUpdates = [];

    // Page visibility listener
    this.handleVisibilityChange = this._handleVisibilityChange.bind(this);

    console.log('[LifecycleManager] Initialized');
  }

  /**
   * Initialize lifecycle monitoring
   */
  initialize() {
    // Listen to page visibility changes
    document.addEventListener('visibilitychange', this.handleVisibilityChange);

    // Listen to beforeunload for cleanup
    window.addEventListener('beforeunload', () => this.destroy());

    // Listen to app state changes (if available)
    if (document.addEventListener) {
      document.addEventListener('pause', () => this.pause());
      document.addEventListener('resume', () => this.resume());
    }

    console.log('[LifecycleManager] Monitoring initialized');
  }

  /**
   * Register pause handler
   * Called when app is paused
   */
  onPause(handler) {
    this.pauseHandlers.push(handler);
  }

  /**
   * Register resume handler
   * Called when app is resumed
   */
  onResume(handler) {
    this.resumeHandlers.push(handler);
  }

  /**
   * Register destroy handler
   * Called when app is destroyed
   */
  onDestroy(handler) {
    this.destroyHandlers.push(handler);
  }

  /**
   * Pause rendering and expensive operations
   */
  pause() {
    if (this.isPaused || this.isDestroyed) return;

    this.isPaused = true;
    console.log('[LifecycleManager] App paused');

    // Call all pause handlers
    this.pauseHandlers.forEach(handler => {
      try {
        handler();
      } catch (error) {
        console.error('[LifecycleManager] Pause handler error:', error);
      }
    });

    // Pause scene rendering
    if (this.scene.pause) {
      this.scene.pause();
    }

    // Stop animations
    this._stopExpensiveOperations();

    // Log resource state
    this._logMemoryUsage('pause');
  }

  /**
   * Resume rendering
   */
  resume() {
    if (!this.isPaused || this.isDestroyed) return;

    this.isPaused = false;
    console.log('[LifecycleManager] App resumed');

    // Resume scene rendering
    if (this.scene.resume) {
      this.scene.resume();
    }

    // Call all resume handlers
    this.resumeHandlers.forEach(handler => {
      try {
        handler();
      } catch (error) {
        console.error('[LifecycleManager] Resume handler error:', error);
      }
    });

    // Process any pending updates
    this._processPendingUpdates();

    // Reset timing to avoid delta spikes
    if (this.scene) {
      this.scene.lastFrameTime = Date.now();
    }

    // Log resource state
    this._logMemoryUsage('resume');
  }

  /**
   * Destroy app resources
   */
  destroy() {
    if (this.isDestroyed) return;

    this.isDestroyed = true;
    this.isPaused = true;

    console.log('[LifecycleManager] App destroying');

    // Call all destroy handlers
    this.destroyHandlers.forEach(handler => {
      try {
        handler();
      } catch (error) {
        console.error('[LifecycleManager] Destroy handler error:', error);
      }
    });

    // Cleanup
    this._cleanup();

    console.log('[LifecycleManager] App destroyed');
  }

  /**
   * Stop expensive operations during pause
   */
  _stopExpensiveOperations() {
    // Stop timers
    if (this.scene && this.scene.animationController) {
      // Don't stop completely, but reduce update frequency
      // This allows for graceful resume without visual jump
    }

    // Reduce particle emission
    if (this.scene && this.scene.effectsManager) {
      this.scene.effectsManager.pause?.();
    }

    // Stop audio if applicable
    // stopBackgroundAudio();

    // Notify AI system to pause processing
    if (this.scene && this.scene.aiCore) {
      this.scene.aiCore.pause?.();
    }
  }

  /**
   * Process pending updates from pause period
   */
  _processPendingUpdates() {
    console.log(`[LifecycleManager] Processing ${this.pendingUpdates.length} pending updates`);

    this.pendingUpdates.forEach(update => {
      try {
        update();
      } catch (error) {
        console.error('[LifecycleManager] Pending update error:', error);
      }
    });

    this.pendingUpdates = [];
  }

  /**
   * Queue update to run on resume
   */
  queueUpdate(update) {
    if (this.isPaused) {
      this.pendingUpdates.push(update);
    } else {
      try {
        update();
      } catch (error) {
        console.error('[LifecycleManager] Update error:', error);
      }
    }
  }

  /**
   * Handle page visibility change
   */
  _handleVisibilityChange(event) {
    if (document.hidden) {
      this.pause();
    } else {
      this.resume();
    }
  }

  /**
   * Cleanup resources
   */
  _cleanup() {
    // Remove event listeners
    document.removeEventListener('visibilitychange', this.handleVisibilityChange);
    window.removeEventListener('beforeunload', () => this.destroy());

    // Dispose scene
    if (this.scene && this.scene.dispose) {
      try {
        this.scene.dispose();
      } catch (error) {
        console.error('[LifecycleManager] Scene dispose error:', error);
      }
    }

    // Clear references
    this.pauseHandlers = [];
    this.resumeHandlers = [];
    this.destroyHandlers = [];
    this.pendingUpdates = [];
  }

  /**
   * Log memory usage for monitoring
   */
  _logMemoryUsage(context) {
    if (performance && performance.memory) {
      const memory = performance.memory;
      console.log(`[LifecycleManager] Memory (${context}):`, {
        used: (memory.usedJSHeapSize / 1048576).toFixed(2) + ' MB',
        limit: (memory.jsHeapSizeLimit / 1048576).toFixed(2) + ' MB',
        ratio: ((memory.usedJSHeapSize / memory.jsHeapSizeLimit) * 100).toFixed(1) + '%',
      });
    }
  }

  /**
   * Get lifecycle state
   */
  getState() {
    return {
      isPaused: this.isPaused,
      isDestroyed: this.isDestroyed,
      pendingUpdates: this.pendingUpdates.length,
    };
  }
}
