/**
 * Advanced AI State Manager
 * Handles bidirectional state synchronization between Android app and 3D visualization
 * Connects AI reasoning, memory updates, and motion state to 3D animations
 */

export class AIStateManager {
  constructor(scene) {
    this.scene = scene;
    this.currentState = {
      memory: {
        semantic: 0,
        behavioral: 0,
        episodic: 0,
        consolidation: 0,
      },
      reasoning: {
        active: false,
        confidence: 0,
        complexity: 'low',
      },
      motion: {
        intensity: 0,
        rotation: 0,
        oscillation: 0,
      },
      performance: {
        fps: 60,
        renderTime: 0,
        updateTime: 0,
      },
      timestamp: Date.now(),
    };

    this.stateChangeCallbacks = [];
    this.metricsCallbacks = [];
  }

  /**
   * Update AI state from Android app
   * Called when ViewModel sends new state through AndroidBridge
   */
  updateAIState(stateData) {
    const previousState = { ...this.currentState };
    
    // Update memory metrics
    if (stateData.memory) {
      this.currentState.memory = {
        ...this.currentState.memory,
        ...stateData.memory,
      };
    }

    // Update reasoning state
    if (stateData.reasoning) {
      this.currentState.reasoning = {
        ...this.currentState.reasoning,
        ...stateData.reasoning,
      };
    }

    // Update motion parameters
    if (stateData.motion) {
      this.currentState.motion = {
        ...this.currentState.motion,
        ...stateData.motion,
      };
    }

    this.currentState.timestamp = Date.now();

    // Notify listeners of state change
    this._notifyStateChange(previousState, this.currentState);

    // Apply state to 3D visualization
    this._applyStateToVisualization();

    console.log('[AIStateManager] State updated:', this.currentState);
  }

  /**
   * Apply AI state changes to 3D scene
   * Maps memory metrics → colors, reasoning confidence → glow, motion → animations
   */
  _applyStateToVisualization() {
    const { memory, reasoning, motion } = this.currentState;

    // Update AI Core colors based on memory
    if (this.scene.aiCore) {
      const semanticColor = new THREE.Color().setHSL(
        0.6 - memory.semantic * 0.1,  // Hue shifts from blue to purple
        0.7 + memory.semantic * 0.3,  // Saturation increases
        0.5 + memory.semantic * 0.2   // Lightness increases
      );
      
      this.scene.aiCore.setColor(semanticColor);
    }

    // Update glow based on reasoning confidence
    if (this.scene.effectsManager) {
      const glowIntensity = reasoning.confidence * 2;
      this.scene.effectsManager.setBloomIntensity(glowIntensity);
    }

    // Update animation intensity based on motion
    if (this.scene.animationController) {
      this.scene.animationController.setIntensity(motion.intensity);
    }

    // Update rotation speed
    if (this.scene.proceduralAnimationController) {
      this.scene.proceduralAnimationController.setRotationSpeed(motion.rotation);
    }

    // Log state visualization
    console.log('[AIStateManager] Applied to visualization', {
      memory: memory.semantic.toFixed(2),
      confidence: reasoning.confidence.toFixed(2),
      intensity: motion.intensity.toFixed(2),
    });
  }

  /**
   * Update performance metrics from rendering
   */
  updateMetrics(fps, renderTime) {
    this.currentState.performance.fps = fps;
    this.currentState.performance.renderTime = renderTime;

    this._notifyMetricsUpdate({
      fps,
      renderTime,
      timestamp: Date.now(),
    });
  }

  /**
   * Process gesture input and translate to AI state changes
   * Example: Swipe → Confidence increase, Tap → Memory consolidation
   */
  processGesture(gestureType, intensity) {
    const stateChange = {
      motion: {
        intensity: intensity,
      },
    };

    switch (gestureType.toLowerCase()) {
      case 'swipe':
        stateChange.reasoning = { active: true, confidence: intensity };
        console.log('[AIStateManager] Swipe gesture → Increase reasoning confidence');
        break;
      case 'tap':
        stateChange.memory = { consolidation: intensity };
        console.log('[AIStateManager] Tap gesture → Trigger memory consolidation');
        break;
      case 'hold':
        stateChange.motion = { rotation: intensity * 2 };
        console.log('[AIStateManager] Hold gesture → Increase rotation');
        break;
      case 'pinch':
        // Zoom-like effect
        stateChange.motion = {
          oscillation: intensity,
        };
        console.log('[AIStateManager] Pinch gesture → Trigger oscillation');
        break;
    }

    this.updateAIState(stateChange);
  }

  /**
   * Register callback for state changes
   */
  onStateChange(callback) {
    this.stateChangeCallbacks.push(callback);
  }

  /**
   * Register callback for metrics updates
   */
  onMetricsUpdate(callback) {
    this.metricsCallbacks.push(callback);
  }

  /**
   * Notify all state change listeners
   */
  _notifyStateChange(previousState, newState) {
    this.stateChangeCallbacks.forEach((callback) => {
      try {
        callback(previousState, newState);
      } catch (error) {
        console.error('[AIStateManager] Error in state change callback:', error);
      }
    });
  }

  /**
   * Notify all metrics update listeners
   */
  _notifyMetricsUpdate(metrics) {
    this.metricsCallbacks.forEach((callback) => {
      try {
        callback(metrics);
      } catch (error) {
        console.error('[AIStateManager] Error in metrics callback:', error);
      }
    });
  }

  /**
   * Get current AI state for transmission to Android
   */
  getState() {
    return {
      ...this.currentState,
      memory: { ...this.currentState.memory },
      reasoning: { ...this.currentState.reasoning },
      motion: { ...this.currentState.motion },
    };
  }

  /**
   * Reset state to defaults
   */
  reset() {
    this.updateAIState({
      memory: {
        semantic: 0,
        behavioral: 0,
        episodic: 0,
        consolidation: 0,
      },
      reasoning: {
        active: false,
        confidence: 0,
        complexity: 'low',
      },
      motion: {
        intensity: 0,
        rotation: 0,
        oscillation: 0,
      },
    });
  }
}
