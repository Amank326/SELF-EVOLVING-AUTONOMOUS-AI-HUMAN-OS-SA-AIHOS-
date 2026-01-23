/**
 * Gesture Animation Engine
 * Implements procedural animations for different gesture types
 * Applies gesture effects on top of base AI-driven animations
 * 
 * OPTIMIZED FOR MOBILE:
 * - Object pooling for effect instances
 * - Capped effect count (max 6 simultaneous)
 * - Efficient effect accumulation
 * - Memory-friendly computations
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
import { EasingFunctions } from './EasingFunctions.js';

export class GestureAnimationEngine {
  constructor() {
    this.activeEffects = [];
    this.reflectionMode = false;
    this.reflectionIntensity = 0;
    
    // Object pooling for effect instances
    this.effectPool = [];
    this.maxPoolSize = 10;
    this.maxConcurrentEffects = 6;  // Limit simultaneous effects for performance
    
    // Track last effect time to prevent spam
    this.lastEffectTime = 0;
    this.effectDebounce = 50;  // Min 50ms between effect triggers
    
    console.log('[GestureAnimationEngine] Initialized (optimized for mobile)');
  }

  /**
   * Get effect from pool or create new
   */
  _getEffect() {
    if (this.effectPool.length > 0) {
      return this.effectPool.pop();
    }
    return {};
  }

  /**
   * Return effect to pool
   */
  _returnEffect(effect) {
    if (this.effectPool.length < this.maxPoolSize) {
      this.effectPool.push(effect);
    }
  }

  /**
   * Check if effect can be triggered (debounce)
   */
  _canTriggerEffect() {
    const now = Date.now();
    if (now - this.lastEffectTime < this.effectDebounce) {
      return false;
    }
    this.lastEffectTime = now;
    return true;
  }

  /**
   * Manage effect limit
   */
  _enforceEffectLimit() {
    // Remove oldest effects if exceeding limit
    while (this.activeEffects.length > this.maxConcurrentEffects) {
      const oldest = this.activeEffects.shift();
      this._returnEffect(oldest);
    }
  }

  /**
   * Apply tap effect
   * Creates quick energy pulse with bright flash
   * OPTIMIZED: Uses easing, object pooling, debouncing
   */
  applyTapEffect(location, targetMesh) {
    if (!this._canTriggerEffect()) return;

    const effect = this._getEffect();
    const duration = 300;
    
    effect.type = 'tap';
    effect.startTime = Date.now();
    effect.duration = duration;
    effect.location = { ...location };
    
    effect.compute = (elapsed) => {
      const progress = Math.min(elapsed / duration, 1);
      // Use cubic easing for smoother feel
      const easeOut = EasingFunctions.easeOutCubic(1 - progress);
      
      return {
        glowIntensity: easeOut * 0.5,
        pulseScale: 1 + easeOut * 0.1,
        particleEmissionBurst: Math.max(0, easeOut * 3),
        colorInfluence: progress,
      };
    };

    this.activeEffects.push(effect);
    this._enforceEffectLimit();
  }

  /**
   * Apply long-press reflection mode
   * Deep, contemplative animation state
   * OPTIMIZED: Smooth sine easing, efficient computation
   */
  activateReflectionMode(duration = 2000) {
    if (this.reflectionMode) return;  // Already active
    
    this.reflectionMode = true;
    const effect = this._getEffect();
    
    effect.type = 'reflection';
    effect.startTime = Date.now();
    effect.duration = duration;
    
    effect.compute = (elapsed) => {
      const progress = Math.min(elapsed / duration, 1);
      // Smooth sine pulse for contemplative feel
      const pulse = Math.sin(progress * Math.PI) * 0.5 + 0.5;
      
      return {
        breathingModifier: 0.3,
        breathingIntensity: 0.8,
        colorShift: {
          r: 0.3,
          g: 0.5,
          b: 1.0,
        },
        lightingDimFactor: 0.6,
        primaryLightIntensity: 0.4,
        particlePattern: 'converging',
        particleEmissionRate: 0.5,
        rotationVelocityModifier: 0.2,
        glowIntensity: 0.6 + pulse * 0.3,
      };
    };

    this.activeEffects.push(effect);
    this.reflectionIntensity = 1.0;
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
   * OPTIMIZED: Better easing, object pooling
   */
  applySweepEffect(direction, intensity) {
    if (!this._canTriggerEffect()) return;

    const effect = this._getEffect();
    const duration = 800;
    
    effect.type = 'swipe';
    effect.startTime = Date.now();
    effect.duration = duration;
    effect.direction = { ...direction };
    effect.initialIntensity = intensity;
    
    effect.compute = (elapsed) => {
      const progress = Math.min(elapsed / duration, 1);
      // Out-quart for flowing feel
      const easeOut = EasingFunctions.easeOutQuart(1 - progress);
      
      return {
        particlePattern: 'streaming',
        particleDirection: direction,
        particleEmissionRate: 2 * easeOut,
        particleSpeed: 2,
        glowIntensity: 0.3 + easeOut * 0.3,
        lightTrailColor: { r: 0.5, g: 0.8, b: 1.0 },
        lightTrailIntensity: easeOut * 0.5,
      };
    };

    this.activeEffects.push(effect);
    this._enforceEffectLimit();
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
   * OPTIMIZED: Cubic easing, object pooling
   */
  applyDoubleTabEffect() {
    if (!this._canTriggerEffect()) return;

    const effect = this._getEffect();
    const duration = 600;
    
    effect.type = 'burst';
    effect.startTime = Date.now();
    effect.duration = duration;
    
    effect.compute = (elapsed) => {
      const progress = Math.min(elapsed / duration, 1);
      // Cubic ease-out for impact feel
      const easeOut = EasingFunctions.easeOutCubic(1 - progress);
      
      return {
        colorFlash: {
          r: 1.0,
          g: 0.8,
          b: 0.3,
        },
        flashIntensity: easeOut,
        particlePattern: 'bursting',
        particleEmissionRate: 4 * easeOut,
        particleSpeed: 3,
        burstRadius: 1.5,
        meshScaleModifier: 1 + easeOut * 0.2,
        glowIntensity: 0.8 + easeOut * 0.5,
      };
    };

    this.activeEffects.push(effect);
    this._enforceEffectLimit();
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
