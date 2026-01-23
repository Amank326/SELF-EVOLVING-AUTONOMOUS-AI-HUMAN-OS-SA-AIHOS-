/**
 * Procedural Animation Controller: Drives 3D animations based on AI state
 * 
 * This is NOT a keyframe animator. This computes animation values in real-time
 * based on the current AI motion state, creating emergent, procedurally-generated
 * animations that reflect actual AI cognitive activity.
 * 
 * Core concept: AI state → animation parameters → procedural animation
 * 
 * No animation loops. All animation is computed live from state.
 */

export class ProceduralAnimationController {
  constructor() {
    // Current AI motion state
    this.aiMotionState = null;
    
    // Time tracking
    this.elapsedTime = 0;
    this.deltaTime = 0;
    this.lastFrameTime = performance.now();
    
    // Animation targets (what we're animating towards)
    this.targets = {
      breathingAmplitude: 0.1,
      rotationVelocity: { x: 0, y: 0, z: 0 },
      colorTarget: { r: 0, g: 1, b: 1 },
      glowIntensity: 1.0,
      particleEmissionVelocity: 1.0,
      morphDisplacement: 0.0,
    };
    
    // Current animation state (independent of AI)
    this.animationState = {
      breathingPhase: 0,
      rotationPhase: { x: 0, y: 0, z: 0 },
      colorCurrent: { r: 0, g: 1, b: 1 },
      glowCurrent: 1.0,
      particlesCurrent: 1.0,
      morphCurrent: 0.0,
    };
    
    // Registered objects that receive animation updates
    this.animatables = [];
  }
  
  /**
   * Register an object to receive animation updates
   * Object must implement updateAnimation(animationFrame)
   */
  registerAnimatable(object) {
    this.animatables.push(object);
  }
  
  /**
   * Update AI motion state (called from Android bridge)
   * This is the KEY input that drives all procedural animation
   */
  setAIMotionState(aiState) {
    this.aiMotionState = aiState;
    
    // Convert AI state to animation targets
    this._computeAnimationTargets(aiState);
  }
  
  /**
   * Update animation based on elapsed time
   * This should be called every frame
   */
  update(deltaTime) {
    this.deltaTime = deltaTime;
    this.elapsedTime += deltaTime;
    
    if (!this.aiMotionState) return;
    
    // Compute procedural animation frame
    const frame = this._computeProceduralAnimationFrame();
    
    // Send to all registered objects
    this.animatables.forEach(obj => {
      if (obj.updateAnimation) {
        obj.updateAnimation(frame);
      }
    });
  }
  
  /**
   * Convert AI state to animation target values
   * This maps cognitive state to visual parameters
   */
  _computeAnimationTargets(aiState) {
    // BREATHING: amplitude and phase determined by AI breathing rate
    this.targets.breathingAmplitude = 0.05 + (aiState.confidence.averageConfidence * 0.15);
    
    // ROTATION: speed determined by cognitive load and decision complexity
    const rotationMagnitude = aiState.rotationSpeed * 2.0; // Radians per second
    this.targets.rotationVelocity = this._computeRotationVelocity(
      aiState.primaryState,
      rotationMagnitude
    );
    
    // COLOR: theme from AI state
    this.targets.colorTarget = this._themeToColor(aiState.colorTheme);
    
    // GLOW: intensity from confidence
    this.targets.glowIntensity = 0.5 + (aiState.glowIntensity * 1.5);
    
    // PARTICLES: emission rate from cognitive load
    this.targets.particleEmissionVelocity = aiState.particleEmissionRate;
    
    // MORPHING: geometry deformation from evolution/adaptation
    this.targets.morphDisplacement = aiState.morphingIntensity;
  }
  
  /**
   * Compute the rotation velocity vector for current AI state
   * Different states have different rotation characteristics
   */
  _computeRotationVelocity(cognitiveState, magnitude) {
    switch (cognitiveState) {
      case 'IDLE':
        // Nearly stationary
        return { x: 0, y: magnitude * 0.2, z: 0 };
      
      case 'THINKING':
        // Gentle rotation around Y axis
        return { x: 0, y: magnitude, z: 0 };
      
      case 'DELIBERATING':
        // Vigorous multi-axis rotation
        return {
          x: magnitude * Math.sin(this.elapsedTime * 0.3),
          y: magnitude * 1.5,
          z: magnitude * Math.cos(this.elapsedTime * 0.2) * 0.5
        };
      
      case 'REFLECTING':
        // Slow, introspective rotation - mostly X axis
        return { x: magnitude * 0.5, y: magnitude * 0.3, z: 0 };
      
      case 'EVOLVING':
        // Spiraling growth rotation
        return {
          x: magnitude * Math.sin(this.elapsedTime * 0.5) * 0.7,
          y: magnitude * 1.2,
          z: magnitude * Math.cos(this.elapsedTime * 0.5) * 0.7
        };
      
      case 'UNCERTAIN':
        // Searching, somewhat erratic rotation
        return {
          x: magnitude * Math.sin(this.elapsedTime * 0.4) * 0.8,
          y: magnitude * 0.9,
          z: magnitude * Math.cos(this.elapsedTime * 0.6) * 0.6
        };
      
      case 'EXECUTING':
        // Focused, purposeful rotation
        return { x: 0, y: magnitude * 0.8, z: 0 };
      
      case 'ERROR':
        // Erratic, agitated rotation
        return {
          x: magnitude * Math.sin(this.elapsedTime * 1.5) * 1.2,
          y: magnitude * 1.5,
          z: magnitude * Math.cos(this.elapsedTime * 2.0) * 1.0
        };
      
      default:
        return { x: 0, y: magnitude, z: 0 };
    }
  }
  
  /**
   * Convert AI color theme to RGB
   */
  _themeToColor(theme) {
    switch (theme) {
      case 'CYAN':
        return { r: 0.0, g: 1.0, b: 1.0 };
      case 'PURPLE':
        return { r: 0.8, g: 0.2, b: 1.0 };
      case 'RED':
        return { r: 1.0, g: 0.1, b: 0.2 };
      case 'BLUE':
        return { r: 0.2, g: 0.5, b: 1.0 };
      case 'GREEN':
        return { r: 0.2, g: 1.0, b: 0.4 };
      case 'AMBER':
        return { r: 1.0, g: 0.7, b: 0.0 };
      default:
        return { r: 0.0, g: 1.0, b: 1.0 };
    }
  }
  
  /**
   * Compute the complete animation frame for this moment
   * Returns all animation values for the current frame
   */
  _computeProceduralAnimationFrame() {
    if (!this.aiMotionState) return null;
    
    const state = this.aiMotionState;
    
    // BREATHING: Sinusoidal oscillation at breathing rate
    const breathingPhase = this.elapsedTime * state.breathingRate * Math.PI * 2;
    const breathingValue = Math.sin(breathingPhase) * this.targets.breathingAmplitude;
    
    // ROTATION: Accumulate rotation based on velocity
    // Each axis rotates independently
    const rotVel = this.targets.rotationVelocity;
    this.animationState.rotationPhase.x += rotVel.x * this.deltaTime;
    this.animationState.rotationPhase.y += rotVel.y * this.deltaTime;
    this.animationState.rotationPhase.z += rotVel.z * this.deltaTime;
    
    // COLOR: Smoothly interpolate towards target color
    const colorLerp = Math.min(1.0, this.deltaTime * 2.0); // 0.5 second transition
    this.animationState.colorCurrent = {
      r: this.animationState.colorCurrent.r * (1 - colorLerp) + this.targets.colorTarget.r * colorLerp,
      g: this.animationState.colorCurrent.g * (1 - colorLerp) + this.targets.colorTarget.g * colorLerp,
      b: this.animationState.colorCurrent.b * (1 - colorLerp) + this.targets.colorTarget.b * colorLerp,
    };
    
    // GLOW: Smooth interpolation with pulsing based on uncertainty
    const glowLerp = Math.min(1.0, this.deltaTime * 1.5);
    this.animationState.glowCurrent = 
      this.animationState.glowCurrent * (1 - glowLerp) + 
      this.targets.glowIntensity * glowLerp;
    
    // Add pulsing to glow based on uncertainty level
    const uncertaintyPulse = state.processing.uncertaintyLevel * 0.3;
    const pulsePhase = this.elapsedTime * 3.0; // 3 Hz pulse
    this.animationState.glowCurrent += Math.sin(pulsePhase) * uncertaintyPulse * 0.1;
    
    // PARTICLES: Smooth transition to target emission rate
    const particleLerp = Math.min(1.0, this.deltaTime * 1.0);
    this.animationState.particlesCurrent = 
      this.animationState.particlesCurrent * (1 - particleLerp) + 
      this.targets.particleEmissionVelocity * particleLerp;
    
    // MORPHING: Smooth deformation based on AI adaptation
    const morphLerp = Math.min(1.0, this.deltaTime * 0.8);
    this.animationState.morphCurrent = 
      this.animationState.morphCurrent * (1 - morphLerp) + 
      this.targets.morphDisplacement * morphLerp;
    
    // Return complete animation frame
    return {
      // Breathing animation
      breathing: {
        amplitude: this.targets.breathingAmplitude,
        phase: breathingPhase,
        value: breathingValue,
        frequency: state.breathingRate,
      },
      
      // Rotation animation
      rotation: {
        x: this.animationState.rotationPhase.x,
        y: this.animationState.rotationPhase.y,
        z: this.animationState.rotationPhase.z,
        velocity: rotVel,
      },
      
      // Color animation
      color: this.animationState.colorCurrent,
      
      // Glow animation
      glow: {
        intensity: this.animationState.glowCurrent,
        targetIntensity: this.targets.glowIntensity,
        confidence: state.confidence.averageConfidence,
      },
      
      // Particle system
      particles: {
        emissionRate: this.animationState.particlesCurrent,
        targetRate: this.targets.particleEmissionVelocity,
        behavior: this._computeParticleBehavior(state),
      },
      
      // Geometry morphing
      morph: {
        displacement: this.animationState.morphCurrent,
        targetDisplacement: this.targets.morphDisplacement,
        evolutionRate: state.processing.adaptationIntensity,
      },
      
      // Overall state for effects
      state: {
        cognitive: state.primaryState,
        operational: state.operationalMode,
        confidence: state.confidence.averageConfidence,
        cognitiveLoad: state.processing.cognitiveLoad,
        elapsedTime: this.elapsedTime,
        deltaTime: this.deltaTime,
      },
    };
  }
  
  /**
   * Compute particle behavior based on AI state
   * Different states produce different particle motion patterns
   */
  _computeParticleBehavior(state) {
    switch (state.primaryState) {
      case 'IDLE':
        // Particles drift slowly, settling
        return {
          pattern: 'settling',
          velocity: 0.2,
          drift: { x: 0, y: -0.1, z: 0 },
          turbulence: 0.1,
        };
      
      case 'THINKING':
        // Particles orbit and swirl
        return {
          pattern: 'orbital',
          velocity: 0.5,
          orbitRadius: 1.2,
          turbulence: 0.3,
        };
      
      case 'DELIBERATING':
        // Particles burst outward then fall back
        return {
          pattern: 'breathing',
          velocity: 1.0,
          expandPhase: this.elapsedTime * state.breathingRate,
          turbulence: 0.5,
        };
      
      case 'REFLECTING':
        // Particles flow inward, introspective
        return {
          pattern: 'converging',
          velocity: 0.4,
          convergenceForce: 0.7,
          turbulence: 0.2,
        };
      
      case 'EVOLVING':
        // Particles burst radially (growth event)
        return {
          pattern: 'bursting',
          velocity: 1.5,
          burstForce: state.processing.adaptationIntensity,
          turbulence: 0.4,
        };
      
      case 'UNCERTAIN':
        // Particles bounce chaotically
        return {
          pattern: 'chaotic',
          velocity: 0.8,
          randomWalk: 0.6,
          turbulence: 0.8,
        };
      
      case 'EXECUTING':
        // Particles stream forward
        return {
          pattern: 'streaming',
          velocity: 0.6,
          streamDirection: { x: 0, y: 1, z: 0 },
          turbulence: 0.2,
        };
      
      case 'ERROR':
        // Particles scatter erratically
        return {
          pattern: 'scattering',
          velocity: 1.2,
          chaos: 1.0,
          turbulence: 1.0,
        };
      
      default:
        return {
          pattern: 'neutral',
          velocity: 0.5,
          turbulence: 0.3,
        };
    }
  }
}
