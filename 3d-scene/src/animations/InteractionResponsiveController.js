/**
 * Interaction Responsive Controller
 * Translates InteractionState to 3D animation targets
 * 
 * Maps:
 * - Touch position (X,Y) → 3D rotation
 * - Touch pressure → energy flow intensity
 * - Idle duration → animation decay
 * - Gesture type → animation behavior
 * - Context score → overall animation intensity
 * 
 * Updates animation targets in real-time (10-60 Hz)
 */
export class InteractionResponsiveController {
  constructor(scene) {
    this.scene = scene;
    this.currentInteractionState = null;
    
    // Animation targets (smooth toward these values)
    this.animationTargets = {
      rotationFromTouch: { x: 0, y: 0, z: 0 },
      touchPressureIntensity: 0,
      idleDecayInfluence: 1,
      gestureAnimationIntensity: 0,
      contextInfluence: 0.5,
      interactionEnergy: 0,
    };

    // Smoothing factors for different parameters
    this.smoothing = {
      touchRotation: 0.15,      // Responsive to touch
      pressure: 0.2,             // Medium responsiveness
      idle: 0.1,                 // Slow decay
      context: 0.05,             // Very slow change
      gesture: 0.25,             // Quick response
    };

    // Gesture-specific state
    this.currentGestureAnimation = null;
    this.reflectionModeActive = false;
    
    console.log('[InteractionResponsiveController] Initialized');
  }

  /**
   * Update interaction state
   * Called by AndroidBridge with new InteractionState
   */
  setInteractionState(state) {
    try {
      this.currentInteractionState = state;
      this.updateAnimationTargets(state);
      this.applyGestureAnimation(state);
    } catch (e) {
      console.error('[InteractionResponsiveController] Error setting state:', e);
    }
  }

  /**
   * Update animation targets based on interaction state
   */
  updateAnimationTargets(state) {
    // Map touch position to rotation
    if (state.multiTouchCount === 0 || state.isIdling) {
      // Idle: gentle auto-rotation
      this.animationTargets.rotationFromTouch.x = Math.sin(Date.now() * 0.0005) * 0.3;
      this.animationTargets.rotationFromTouch.y = Math.cos(Date.now() * 0.0003) * 0.3;
    } else {
      // Touch: rotate based on position
      // Normalize to -1 to 1
      const x = (state.touchX - 0.5) * 2;
      const y = (state.touchY - 0.5) * 2;
      
      this.animationTargets.rotationFromTouch.x = -y * 0.5;  // Vertical touch → X rotation
      this.animationTargets.rotationFromTouch.y = x * 0.5;   // Horizontal touch → Y rotation
      this.animationTargets.rotationFromTouch.z = (x * y) * 0.2;  // Cross-product → Z rotation
    }

    // Map touch pressure to energy/glow intensity
    this.animationTargets.touchPressureIntensity = state.touchPressure;

    // Map idle duration to animation decay
    // At 0s idle: 1.0, at 10s: 0.0
    this.animationTargets.idleDecayInfluence = state.idleDecayFactor;

    // Map context score to overall intensity
    this.animationTargets.contextInfluence = state.contextScore;

    // Compute interaction energy (0-1)
    this.animationTargets.interactionEnergy = state.getInteractionEnergy();

    // Gesture intensity based on type and duration
    const gestureIntensity = this.computeGestureIntensity(state);
    this.animationTargets.gestureAnimationIntensity = gestureIntensity;

    // Handle reflection mode
    this.reflectionModeActive = state.isInReflectionMode;
  }

  /**
   * Compute gesture-specific animation
   * Different gesture types trigger different visual effects
   */
  applyGestureAnimation(state) {
    switch (state.gestureType) {
      case 'TAP':
        this.createTapPulse(state);
        break;
      case 'LONG_PRESS':
        this.activateReflectionAnimation(state);
        break;
      case 'SWIPE':
        this.createSwipeFlow(state);
        break;
      case 'PINCH':
        this.modifyBreathingFromPinch(state);
        break;
      case 'TWO_FINGER_ROTATE':
        this.handleTwoFingerRotate(state);
        break;
      case 'DOUBLE_TAP':
        this.createBurstEffect(state);
        break;
      case 'DRAG':
        // Continuous rotation - handled by updateAnimationTargets
        break;
      case 'IDLE':
        this.currentGestureAnimation = null;
        break;
    }
  }

  /**
   * Create tap pulse effect
   * Quick energy burst at touch location
   */
  createTapPulse(state) {
    this.currentGestureAnimation = {
      type: 'tap_pulse',
      location: { x: state.touchX, y: state.touchY },
      intensity: 1.0,
      duration: 300,  // milliseconds
      startTime: Date.now(),
    };

    console.log('[Gesture] TAP pulse at', { x: state.touchX, y: state.touchY });
  }

  /**
   * Activate reflection animation (long-press)
   * Deep pulsing, dimmed lights, slowed breathing
   */
  activateReflectionAnimation(state) {
    this.currentGestureAnimation = {
      type: 'reflection_pulse',
      intensity: 1.0,
      duration: 2000,  // 2 seconds minimum
      startTime: Date.now(),
      pulseRate: 1.0,  // Hz - slow pulse
    };

    console.log('[Gesture] LONG_PRESS - entering reflection mode');
  }

  /**
   * Create swipe flow effect
   * Particles stream in swipe direction
   */
  createSwipeFlow(state) {
    const velocityX = state.gestureVelocity > 0 ? 1 : -1;
    const velocityY = state.gestureVelocity > 0 ? 1 : -1;

    this.currentGestureAnimation = {
      type: 'swipe_flow',
      direction: { x: velocityX, y: velocityY },
      intensity: Math.min(state.gestureVelocity / 500, 1),
      duration: 800,
      startTime: Date.now(),
    };

    console.log('[Gesture] SWIPE flow in direction', { x: velocityX, y: velocityY });
  }

  /**
   * Modify breathing rate based on pinch distance
   * Smaller pinch = faster breathing
   */
  modifyBreathingFromPinch(state) {
    // Gesture intensity is proportional to pinch distance
    const breathingModifier = 0.5 + (1 - state.gestureIntensity) * 1.5;  // 0.5x to 2x

    this.currentGestureAnimation = {
      type: 'pinch_breathing',
      breathingModifier: breathingModifier,
      duration: 500,
      startTime: Date.now(),
    };

    console.log('[Gesture] PINCH - breathing modifier:', breathingModifier.toFixed(2) + 'x');
  }

  /**
   * Handle two-finger rotation
   * Spin the core in response to finger movement
   */
  handleTwoFingerRotate(state) {
    this.currentGestureAnimation = {
      type: 'two_finger_rotation',
      intensity: state.gestureIntensity,
      duration: 1000,
      startTime: Date.now(),
      rotationVelocity: state.gestureVelocity * 0.5,
    };

    console.log('[Gesture] TWO_FINGER_ROTATE - velocity:', state.gestureVelocity);
  }

  /**
   * Create burst effect (double-tap)
   * Explosive particle burst from center
   */
  createBurstEffect(state) {
    this.currentGestureAnimation = {
      type: 'burst_effect',
      intensity: 1.5,
      duration: 600,
      startTime: Date.now(),
      burstRadius: 1.5,
    };

    console.log('[Gesture] DOUBLE_TAP - burst effect');
  }

  /**
   * Compute current gesture intensity (0-1)
   * Based on gesture type and duration
   */
  computeGestureIntensity(state) {
    if (state.gestureType === 'IDLE') return 0;

    let intensity = state.gestureIntensity;

    // Extend intensity if gesture animation is still active
    if (this.currentGestureAnimation) {
      const elapsed = Date.now() - this.currentGestureAnimation.startTime;
      if (elapsed < this.currentGestureAnimation.duration) {
        const progress = elapsed / this.currentGestureAnimation.duration;
        intensity = Math.max(intensity, (1 - progress) * 0.5);  // Decay over time
      } else {
        this.currentGestureAnimation = null;
      }
    }

    return intensity;
  }

  /**
   * Get smoothed animation values
   * Apply exponential smoothing to prevent sudden jumps
   */
  getSmoothedTargets() {
    const smoothed = {
      rotationFromTouch: { ...this.animationTargets.rotationFromTouch },
      touchPressureIntensity: this.animationTargets.touchPressureIntensity,
      idleDecayInfluence: this.animationTargets.idleDecayInfluence,
      gestureAnimationIntensity: this.animationTargets.gestureAnimationIntensity,
      contextInfluence: this.animationTargets.contextInfluence,
      interactionEnergy: this.animationTargets.interactionEnergy,
    };

    return smoothed;
  }

  /**
   * Update with delta time
   * Called every frame to apply smoothing
   */
  update(deltaTime) {
    // Apply exponential smoothing to targets
    // This creates smooth transitions instead of sudden jumps

    if (!this.currentInteractionState) return;

    // Gesture animation influence decays over time
    if (this.currentGestureAnimation) {
      const elapsed = Date.now() - this.currentGestureAnimation.startTime;
      if (elapsed > this.currentGestureAnimation.duration) {
        this.currentGestureAnimation = null;
        this.animationTargets.gestureAnimationIntensity = 0;
      }
    }

    // Reflection mode influence
    if (this.reflectionModeActive) {
      this.animationTargets.gestureAnimationIntensity = Math.max(
        this.animationTargets.gestureAnimationIntensity,
        0.7  // Strong influence while reflecting
      );
    }
  }

  /**
   * Get interaction metrics for debugging
   */
  getMetrics() {
    return {
      currentGestureType: this.currentInteractionState?.gestureType || 'IDLE',
      touchPosition: {
        x: this.currentInteractionState?.touchX || 0,
        y: this.currentInteractionState?.touchY || 0,
      },
      touchPressure: this.currentInteractionState?.touchPressure || 0,
      idleDuration: this.currentInteractionState?.idleDuration || 0,
      contextScore: this.currentInteractionState?.contextScore || 0,
      interactionEnergy: this.animationTargets.interactionEnergy,
      gestureAnimationActive: this.currentGestureAnimation !== null,
      reflectionMode: this.reflectionModeActive,
    };
  }
}
