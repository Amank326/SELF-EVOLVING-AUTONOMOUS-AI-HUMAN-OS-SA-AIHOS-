/**
 * Gesture Animation Engine
 * Implements procedural animations for different gesture types
 * Applies gesture effects on top of base AI-driven animations
 * 
 * Effects:
 * - TAP: Quick pulse/spark
 * - LONG_PRESS: Deep reflection mode (dimmed, slow)
 * - SWIPE: Flowing particles
 * - PINCH: Breathing rate control
 * - DOUBLE_TAP: Burst explosion
 * - TWO_FINGER_ROTATE: Manual core rotation
 * 
 * All effects are procedural (not keyframe-based)
 */
export class GestureAnimationEngine {
  constructor() {
    this.activeEffects = [];
    this.reflectionMode = false;
    this.reflectionIntensity = 0;
    
    console.log('[GestureAnimationEngine] Initialized');
  }

  /**
   * Apply tap effect
   * Creates quick energy pulse with bright flash
   */
  applyTapEffect(location, targetMesh) {
    const effect = {
      type: 'tap',
      startTime: Date.now(),
      duration: 300,
      location: { ...location },
      
      compute: (elapsed) => {
        const progress = elapsed / this.duration;  // 0 to 1
        const easeOut = 1 - (progress * progress);
        
        return {
          // Create brief glow spike
          glowIntensity: easeOut * 0.5,
          
          // Quick pulse outward
          pulseScale: 1 + easeOut * 0.1,
          
          // Particle burst
          particleEmissionBurst: Math.max(0, (1 - progress) * 3),
          
          // Flash color (white → original color)
          colorInfluence: progress,
        };
      },
    };

    this.activeEffects.push(effect);
    console.log('[GestureEngine] TAP effect at', location);
  }

  /**
   * Apply long-press reflection mode
   * Deep, contemplative animation state
   * - Slower breathing
   * - Dimmed lights
   * - Inward-focused particles
   * - Blue/purple coloration
   */
  activateReflectionMode(duration = 2000) {
    this.reflectionMode = true;
    
    const effect = {
      type: 'reflection',
      startTime: Date.now(),
      duration: duration,
      
      compute: (elapsed) => {
        const progress = Math.min(elapsed / duration, 1);
        const pulse = Math.sin(elapsed * Math.PI / duration) * 0.5 + 0.5;  // Sinewave pulse
        
        return {
          // Slow breathing pulse
          breathingModifier: 0.3,  // 0.3x normal rate
          breathingIntensity: 0.8,
          
          // Deep blue/purple tones
          colorShift: {
            r: 0.3,  // Reduce red
            g: 0.5,  // Moderate green
            b: 1.0,  // Boost blue
          },
          
          // Dimmed lighting for introspection
          lightingDimFactor: 0.6,
          primaryLightIntensity: 0.4,
          
          // Inward-converging particles
          particlePattern: 'converging',
          particleEmissionRate: 0.5,
          
          // Slow rotation
          rotationVelocityModifier: 0.2,
          
          // Pulsing glow
          glowIntensity: 0.6 + pulse * 0.3,
        };
      },
    };

    this.activeEffects.push(effect);
    this.reflectionIntensity = 1.0;
    console.log('[GestureEngine] REFLECTION mode activated');
  }

  /**
   * Deactivate reflection mode
   */
  deactivateReflectionMode() {
    this.reflectionMode = false;
    this.reflectionIntensity = 0;
    
    // Remove reflection effect
    this.activeEffects = this.activeEffects.filter(e => e.type !== 'reflection');
    console.log('[GestureEngine] REFLECTION mode deactivated');
  }

  /**
   * Apply swipe effect
   * Creates flowing particle stream in swipe direction
   */
  applySweepEffect(direction, intensity) {
    const effect = {
      type: 'swipe',
      startTime: Date.now(),
      duration: 800,
      direction: { ...direction },
      initialIntensity: intensity,
      
      compute: (elapsed) => {
        const progress = elapsed / this.duration;
        const easeOut = 1 - (progress * progress);
        
        return {
          // Particle flow in gesture direction
          particlePattern: 'streaming',
          particleDirection: direction,
          particleEmissionRate: 2 * easeOut,
          particleSpeed: 2,
          
          // Trailing glow
          glowIntensity: 0.3 + easeOut * 0.3,
          
          // Light trails
          lightTrailColor: { r: 0.5, g: 0.8, b: 1.0 },
          lightTrailIntensity: easeOut * 0.5,
        };
      },
    };

    this.activeEffects.push(effect);
    console.log('[GestureEngine] SWIPE effect in direction', direction);
  }

  /**
   * Apply pinch effect
   * Modifies breathing rate based on pinch distance
   */
  applyPinchEffect(closeness) {
    // closeness: 0 = wide open, 1 = pinched together
    // Creates inverse relationship: wide = slow, pinched = fast
    
    const effect = {
      type: 'pinch',
      startTime: Date.now(),
      duration: 500,
      closeness: closeness,
      
      compute: (elapsed) => {
        const progress = Math.min(elapsed / this.duration, 1);
        
        // Breathing modifier: 0.5x (wide) to 2.0x (pinched)
        const breathingModifier = 0.5 + (1 - closeness) * 1.5;
        
        return {
          breathingModifier: breathingModifier,
          breathingIntensity: 1.0,
          
          // Visual feedback - glow intensity follows pinch
          glowIntensity: closeness * 0.6,
          
          // Color shift - cool when open, warm when pinched
          colorShift: {
            r: closeness * 0.5,
            g: 0.5,
            b: (1 - closeness) * 0.5,
          },
        };
      },
    };

    this.activeEffects.push(effect);
    console.log('[GestureEngine] PINCH effect with closeness:', closeness);
  }

  /**
   * Apply two-finger rotation effect
   * Manual spinning of the core
   */
  applyTwoFingerRotationEffect(rotationVelocity) {
    const effect = {
      type: 'two_finger_rotation',
      startTime: Date.now(),
      duration: 1000,
      rotationVelocity: rotationVelocity,
      
      compute: (elapsed) => {
        const progress = Math.min(elapsed / this.duration, 1);
        const easeOut = 1 - (progress * progress);
        
        return {
          // Sustained rotation
          additionalRotationVelocity: rotationVelocity * easeOut,
          
          // Visual feedback - glow follows rotation
          glowIntensity: 0.3 + (Math.abs(rotationVelocity) / 2) * 0.5,
          
          // Particle vortex effect
          particlePattern: 'orbital',
          particleSpeed: Math.abs(rotationVelocity),
        };
      },
    };

    this.activeEffects.push(effect);
    console.log('[GestureEngine] TWO_FINGER_ROTATION with velocity:', rotationVelocity);
  }

  /**
   * Apply double-tap burst effect
   * Explosive particle burst from center
   */
  applyDoubleTabEffect() {
    const effect = {
      type: 'burst',
      startTime: Date.now(),
      duration: 600,
      
      compute: (elapsed) => {
        const progress = elapsed / this.duration;
        const easeOut = 1 - (progress * progress * progress);  // Cubic ease-out
        
        return {
          // Explosive color flash
          colorFlash: {
            r: 1.0,
            g: 0.8,
            b: 0.3,
          },
          flashIntensity: easeOut,
          
          // Burst particles outward
          particlePattern: 'bursting',
          particleEmissionRate: 4 * easeOut,
          particleSpeed: 3,
          burstRadius: 1.5,
          
          // Shock wave
          meshScaleModifier: 1 + easeOut * 0.2,
          
          // Bright glow
          glowIntensity: 0.8 + easeOut * 0.5,
        };
      },
    };

    this.activeEffects.push(effect);
    console.log('[GestureEngine] DOUBLE_TAP burst effect');
  }

  /**
   * Compute combined effect from all active effects
   * Returns accumulated animation modifiers
   */
  computeEffects(deltaTime) {
    const now = Date.now();
    const accumulated = {
      breathingModifier: 1.0,
      colorShift: { r: 0, g: 0, b: 0 },
      glowIntensity: 0,
      particleModifiers: {},
      rotationModifier: 1.0,
      lightingDim: 1.0,
      effectCount: 0,
    };

    // Update and process active effects
    this.activeEffects = this.activeEffects.filter(effect => {
      const elapsed = now - effect.startTime;
      
      if (elapsed > effect.duration) {
        return false;  // Remove expired effect
      }

      const computation = effect.compute(elapsed);

      // Accumulate effect values
      if (computation.breathingModifier) {
        accumulated.breathingModifier *= computation.breathingModifier;
      }
      if (computation.colorShift) {
        accumulated.colorShift.r += computation.colorShift.r * 0.2;
        accumulated.colorShift.g += computation.colorShift.g * 0.2;
        accumulated.colorShift.b += computation.colorShift.b * 0.2;
      }
      if (computation.glowIntensity !== undefined) {
        accumulated.glowIntensity += computation.glowIntensity;
      }
      if (computation.lightingDimFactor) {
        accumulated.lightingDim *= computation.lightingDimFactor;
      }
      if (computation.additionalRotationVelocity !== undefined) {
        accumulated.rotationModifier += computation.additionalRotationVelocity;
      }

      // Store particle modifiers
      if (computation.particlePattern) {
        accumulated.particleModifiers.pattern = computation.particlePattern;
        accumulated.particleModifiers.emissionRate = computation.particleEmissionRate;
      }

      accumulated.effectCount++;
      return true;  // Keep effect
    });

    // Apply reflection mode intensity decay
    if (this.reflectionMode) {
      this.reflectionIntensity = Math.max(0, this.reflectionIntensity - deltaTime * 0.5);
    } else {
      this.reflectionIntensity = Math.max(0, this.reflectionIntensity - deltaTime * 1.0);
    }

    return accumulated;
  }

  /**
   * Get current state for debugging
   */
  getMetrics() {
    return {
      reflectionMode: this.reflectionMode,
      reflectionIntensity: this.reflectionIntensity,
      activeEffectCount: this.activeEffects.length,
      activeEffectTypes: this.activeEffects.map(e => e.type),
    };
  }
}
