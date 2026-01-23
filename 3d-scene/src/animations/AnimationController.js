/**
 * Animation Controller
 * Manages all micro-animations: breathing, rotation, pulse, color cycling
 * Supports animation composition and intensity control
 */

export class AnimationController {
  constructor() {
    this.animatables = [];
    this.animations = {
      breathing: { enabled: true, frequency: 2.0, amplitude: 1.0 },
      rotation: { enabled: true, speed: { x: 0.3, y: 0.5, z: 0.1 } },
      pulse: { enabled: true, frequency: 1.5, amplitude: 1.0 },
      colorCycle: { enabled: true, frequency: 0.5, amplitude: 1.0 },
    };

    this.state = {
      time: 0,
      intensity: 1.0,
    };

    // Pre-calculated animation values
    this.currentAnimationState = {
      rotationX: 0,
      rotationY: 0,
      rotationZ: 0,
      breathIntensity: 0,
      pulseIntensity: 0,
      colorPhase: 0,
    };
  }

  /**
   * Register an animatable object
   */
  registerAnimatable(object) {
    if (object && typeof object.updateAnimation === 'function') {
      this.animatables.push(object);
    }
  }

  /**
   * Unregister an animatable object
   */
  unregisterAnimatable(object) {
    const index = this.animatables.indexOf(object);
    if (index !== -1) {
      this.animatables.splice(index, 1);
    }
  }

  /**
   * Update all animations
   */
  update(deltaTime) {
    this.state.time += deltaTime;

    // Calculate current animation values
    this._updateAnimationState();

    // Update all registered animatables
    for (const animatable of this.animatables) {
      animatable.updateAnimation(deltaTime, this.currentAnimationState);
    }
  }

  /**
   * Calculate current animation state
   */
  _updateAnimationState() {
    const t = this.state.time;
    const intensity = this.state.intensity;

    // Breathing: smooth scale oscillation
    if (this.animations.breathing.enabled) {
      this.currentAnimationState.breathIntensity =
        Math.sin(t * this.animations.breathing.frequency * Math.PI) *
        this.animations.breathing.amplitude *
        intensity;
    }

    // Rotation: continuous rotation with different speeds per axis
    if (this.animations.rotation.enabled) {
      const rot = this.animations.rotation.speed;
      this.currentAnimationState.rotationX = (rot.x * deltaTime * intensity) || 0.001;
      this.currentAnimationState.rotationY = (rot.y * deltaTime * intensity) || 0.001;
      this.currentAnimationState.rotationZ = (rot.z * deltaTime * intensity) || 0.001;
    }

    // Pulse: intensity oscillation
    if (this.animations.pulse.enabled) {
      this.currentAnimationState.pulseIntensity =
        0.5 +
        0.5 * Math.sin(t * this.animations.pulse.frequency * Math.PI) *
        this.animations.pulse.amplitude *
        intensity;
    }

    // Color cycle: phase for shader color transitions
    if (this.animations.colorCycle.enabled) {
      this.currentAnimationState.colorPhase =
        t * this.animations.colorCycle.frequency * intensity;
    }
  }

  /**
   * Set overall animation intensity (0.0 - 1.0)
   */
  setIntensity(intensity) {
    this.state.intensity = Math.max(0, Math.min(1, intensity));
  }

  /**
   * Enable/disable specific animation
   */
  setAnimationEnabled(animationName, enabled) {
    if (this.animations[animationName]) {
      this.animations[animationName].enabled = enabled;
    }
  }

  /**
   * Adjust animation frequency (for breathing, pulse, color)
   */
  setAnimationFrequency(animationName, frequency) {
    if (this.animations[animationName]) {
      this.animations[animationName].frequency = frequency;
    }
  }

  /**
   * Adjust rotation speed per axis
   */
  setRotationSpeed(x, y, z) {
    this.animations.rotation.speed = { x, y, z };
  }

  /**
   * Get current animation state
   */
  getAnimationState() {
    return { ...this.currentAnimationState };
  }

  /**
   * Dispose
   */
  dispose() {
    this.animatables = [];
  }
}
