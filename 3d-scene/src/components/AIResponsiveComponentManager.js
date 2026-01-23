/**
 * AI-Responsive Component Manager
 * 
 * Connects procedural animations to actual 3D components (AI-Core, Lighting, Particles)
 * Implements state-driven visual adaptation
 */

export class AIResponsiveComponentManager {
  constructor(scene, aiCore, lightingSystem, effectsManager) {
    this.scene = scene;
    this.aiCore = aiCore;
    this.lightingSystem = lightingSystem;
    this.effectsManager = effectsManager;
    
    this.currentAnimationFrame = null;
  }
  
  /**
   * Apply animation frame to all components
   * This is called every frame by the procedural animation controller
   */
  applyAnimationFrame(frame) {
    if (!frame) return;
    
    this.currentAnimationFrame = frame;
    
    // Apply to each component system
    this._updateAICoreAnimation(frame);
    this._updateLightingAnimation(frame);
    this._updateParticleAnimation(frame);
    this._updateGlowAndEffects(frame);
    this._updateGeometryMorphing(frame);
  }
  
  /**
   * Update AI-Core breathing, color, and rotation
   */
  _updateAICoreAnimation(frame) {
    if (!this.aiCore || !this.aiCore.group) return;
    
    // BREATHING: Scale the core based on breathing phase
    const breathingScale = 1.0 + frame.breathing.value;
    this.aiCore.group.scale.set(breathingScale, breathingScale, breathingScale);
    
    // ROTATION: Rotate based on AI cognitive load
    this.aiCore.group.rotation.x = frame.rotation.x;
    this.aiCore.group.rotation.y = frame.rotation.y;
    this.aiCore.group.rotation.z = frame.rotation.z;
    
    // COLOR: Update core crystal color
    if (this.aiCore.coreMesh) {
      const color = frame.color;
      this.aiCore.coreMesh.material.color.setRGB(color.r, color.g, color.b);
    }
    
    // GLOW: Update emissive intensity
    if (this.aiCore.coreMesh) {
      const emissiveIntensity = frame.glow.intensity;
      this.aiCore.coreMesh.material.emissiveIntensity = emissiveIntensity;
    }
  }
  
  /**
   * Update lighting based on AI state
   * Different cognitive states illuminate differently
   */
  _updateLightingAnimation(frame) {
    if (!this.lightingSystem) return;
    
    const cognitiveState = frame.state.cognitive;
    const confidence = frame.state.confidence;
    const cognitiveLoad = frame.state.cognitiveLoad;
    
    // Primary light: main illumination
    // Intensity correlates with cognitive load
    if (this.lightingSystem.primaryLight) {
      this.lightingSystem.primaryLight.intensity = 1.0 + (cognitiveLoad * 0.5);
    }
    
    // Accent light: highlights different states
    // Color changes based on AI state
    if (this.lightingSystem.accentLight) {
      const accentColor = this._getStateLightColor(cognitiveState);
      this.lightingSystem.accentLight.color.set(accentColor);
      this.lightingSystem.accentLight.intensity = 0.3 + (confidence * 0.5);
    }
    
    // Rim light: outline intensity from confidence
    if (this.lightingSystem.rimLight) {
      this.lightingSystem.rimLight.intensity = 0.2 + (confidence * 0.6);
    }
    
    // REFLECTION MODE: Dim lights for introspection
    if (cognitiveState === 'REFLECTING') {
      if (this.lightingSystem.primaryLight) {
        this.lightingSystem.primaryLight.intensity = 0.6;
      }
      if (this.lightingSystem.secondaryLight) {
        this.lightingSystem.secondaryLight.intensity = 0.3;
      }
    }
    
    // EVOLUTION MODE: Pulse lights for growth
    if (cognitiveState === 'EVOLVING') {
      const pulsePhase = frame.state.elapsedTime * 2.0; // 2 Hz pulse
      const pulseFactor = 0.5 + Math.sin(pulsePhase) * 0.5;
      
      if (this.lightingSystem.primaryLight) {
        this.lightingSystem.primaryLight.intensity = 1.2 * pulseFactor;
      }
      if (this.lightingSystem.accentLight) {
        this.lightingSystem.accentLight.intensity = 0.5 * pulseFactor;
      }
    }
    
    // ERROR MODE: Flashing red warning light
    if (cognitiveState === 'ERROR') {
      const flashPhase = frame.state.elapsedTime * 4.0; // 4 Hz flash
      const flashFactor = Math.abs(Math.sin(flashPhase));
      
      if (this.lightingSystem.accentLight) {
        this.lightingSystem.accentLight.color.set(0xff0000);
        this.lightingSystem.accentLight.intensity = 1.0 * flashFactor;
      }
    }
  }
  
  /**
   * Get light color for cognitive state
   */
  _getStateLightColor(cognitiveState) {
    switch (cognitiveState) {
      case 'THINKING':
        return 0x00ffff; // Cyan
      case 'DELIBERATING':
        return 0xff00ff; // Magenta
      case 'REFLECTING':
        return 0x0088ff; // Blue
      case 'EVOLVING':
        return 0x00ff88; // Green
      case 'UNCERTAIN':
        return 0xffaa00; // Amber
      case 'EXECUTING':
        return 0x00ffff; // Cyan
      case 'ERROR':
        return 0xff0000; // Red
      default:
        return 0xffffff; // White
    }
  }
  
  /**
   * Update particle system emission and behavior
   */
  _updateParticleAnimation(frame) {
    if (!this.aiCore || !this.aiCore.particleSystem) return;
    
    const particles = this.aiCore.particleSystem;
    const particleFrame = frame.particles;
    
    // Update emission rate
    particles.emissionRate = particleFrame.emissionRate * 10; // Scale to particles/frame
    
    // Update particle velocity based on AI state behavior
    const behavior = particleFrame.behavior;
    
    switch (behavior.pattern) {
      case 'settling':
        // Particles drift down slowly
        particles.velocity = 0.02;
        particles.gravity = -0.08;
        break;
      
      case 'orbital':
        // Particles orbit around core
        this._applyOrbitalBehavior(particles, frame);
        particles.velocity = 0.5;
        break;
      
      case 'breathing':
        // Particles expand and contract
        const breathingPhase = frame.breathing.phase;
        const breathingForce = Math.sin(breathingPhase) * 0.5 + 0.5;
        particles.velocity = 0.3 + (breathingForce * 0.7);
        break;
      
      case 'converging':
        // Particles flow inward (reflection)
        this._applyConvergingBehavior(particles);
        break;
      
      case 'bursting':
        // Particles burst radially (evolution)
        const burstPhase = frame.state.elapsedTime % 2.0; // Pulse every 2 seconds
        const burstForce = Math.exp(-burstPhase * 2.0); // Exponential decay
        particles.velocity = 1.0 + (burstForce * 2.0);
        particles.divergence = 1.0;
        break;
      
      case 'chaotic':
        // Random turbulent motion
        particles.velocity = 0.8;
        particles.turbulence = 0.8;
        break;
      
      case 'streaming':
        // Directional flow
        particles.velocity = 0.6;
        particles.streamDirection = behavior.streamDirection;
        break;
      
      case 'scattering':
        // Chaotic scatter
        particles.velocity = 1.2;
        particles.divergence = 2.0;
        break;
    }
    
    // Update particle color to match core
    if (particles.material && particles.material.color) {
      particles.material.color.setRGB(
        frame.color.r,
        frame.color.g,
        frame.color.b
      );
    }
  }
  
  /**
   * Apply orbital particle behavior
   */
  _applyOrbitalBehavior(particles, frame) {
    const time = frame.state.elapsedTime;
    const orbitSpeed = 0.5;
    
    // Each particle orbits at a slightly different phase
    particles.customBehavior = {
      type: 'orbital',
      speed: orbitSpeed,
      radius: 1.2,
      phaseOffset: Math.PI / 4 // Different orbit heights
    };
  }
  
  /**
   * Apply converging particle behavior (for reflection mode)
   */
  _applyConvergingBehavior(particles) {
    particles.customBehavior = {
      type: 'converging',
      force: 0.7,
      damping: 0.9
    };
  }
  
  /**
   * Update glow and post-processing effects
   */
  _updateGlowAndEffects(frame) {
    if (!this.effectsManager) return;
    
    // Update glow bloom based on AI confidence
    const glowIntensity = frame.glow.intensity;
    if (this.effectsManager.bloomPass) {
      this.effectsManager.bloomPass.strength = glowIntensity * 0.5;
      this.effectsManager.bloomPass.radius = 0.5 + (glowIntensity * 0.5);
    }
    
    // Film grain effect responds to uncertainty
    const uncertainty = frame.state.confidence;
    if (this.effectsManager.filmGrainPass) {
      // High uncertainty = more grain
      this.effectsManager.filmGrainPass.amount = (1.0 - uncertainty) * 0.3;
    }
    
    // Chromatic aberration for error states
    if (frame.state.cognitive === 'ERROR') {
      if (this.effectsManager.chromaticAberrationPass) {
        this.effectsManager.chromaticAberrationPass.uniforms.amount.value = 0.005;
      }
    }
  }
  
  /**
   * Apply geometry morphing to AI-Core
   * Evolution and reflection cause shape changes
   */
  _updateGeometryMorphing(frame) {
    if (!this.aiCore || !this.aiCore.coreMesh) return;
    
    const morphIntensity = frame.morph.displacement;
    const cognitiveState = frame.state.cognitive;
    
    if (morphIntensity === 0) return;
    
    // Different morphing behaviors for different states
    switch (cognitiveState) {
      case 'EVOLVING':
        // Gradual shape transformation (geometry deformation)
        this._morphForEvolution(this.aiCore.coreMesh, morphIntensity, frame.state.elapsedTime);
        break;
      
      case 'REFLECTING':
        // Contraction inward
        this._morphForReflection(this.aiCore.coreMesh, morphIntensity);
        break;
      
      case 'UNCERTAIN':
        // Instability in shape
        this._morphForUncertainty(this.aiCore.coreMesh, morphIntensity, frame.state.elapsedTime);
        break;
      
      case 'THINKING':
        // Subtle expansion
        this._morphForThinking(this.aiCore.coreMesh, morphIntensity * 0.5);
        break;
    }
  }
  
  /**
   * Morph geometry for evolution state
   * Gradual shape changes reflecting rule adaptation
   */
  _morphForEvolution(mesh, intensity, time) {
    if (!mesh.geometry.userData.originalPositions) {
      mesh.geometry.userData.originalPositions = 
        mesh.geometry.attributes.position.array.slice();
    }
    
    const positions = mesh.geometry.attributes.position;
    const original = mesh.geometry.userData.originalPositions;
    
    // Sinusoidal deformation with time
    const deformPhase = time * 0.5;
    
    for (let i = 0; i < positions.count; i++) {
      const idx = i * 3;
      const origX = original[idx];
      const origY = original[idx + 1];
      const origZ = original[idx + 2];
      
      // Radial deformation
      const distance = Math.sqrt(origX * origX + origY * origY + origZ * origZ);
      const deformAmount = Math.sin(deformPhase + distance) * intensity;
      
      // Apply deformation
      const deformFactor = 1.0 + deformAmount;
      positions.setXYZ(i, 
        origX * deformFactor,
        origY * deformFactor,
        origZ * deformFactor
      );
    }
    
    positions.needsUpdate = true;
  }
  
  /**
   * Morph for reflection: inward contraction
   */
  _morphForReflection(mesh, intensity) {
    if (!mesh.geometry.userData.originalPositions) {
      mesh.geometry.userData.originalPositions = 
        mesh.geometry.attributes.position.array.slice();
    }
    
    const positions = mesh.geometry.attributes.position;
    const original = mesh.geometry.userData.originalPositions;
    
    // Contract inward
    const contractionFactor = 1.0 - (intensity * 0.3);
    
    for (let i = 0; i < positions.count; i++) {
      const idx = i * 3;
      positions.setXYZ(i,
        original[idx] * contractionFactor,
        original[idx + 1] * contractionFactor,
        original[idx + 2] * contractionFactor
      );
    }
    
    positions.needsUpdate = true;
  }
  
  /**
   * Morph for uncertainty: unstable, jittering shape
   */
  _morphForUncertainty(mesh, intensity, time) {
    if (!mesh.geometry.userData.originalPositions) {
      mesh.geometry.userData.originalPositions = 
        mesh.geometry.attributes.position.array.slice();
    }
    
    const positions = mesh.geometry.attributes.position;
    const original = mesh.geometry.userData.originalPositions;
    
    // Random perturbations
    for (let i = 0; i < positions.count; i++) {
      const idx = i * 3;
      
      // Pseudo-random noise based on position and time
      const noise = Math.sin(original[idx] * 10 + time * 5) * 
                   Math.cos(original[idx + 1] * 10 + time * 3);
      
      const jitter = noise * intensity * 0.1;
      
      positions.setXYZ(i,
        original[idx] + jitter,
        original[idx + 1] + jitter,
        original[idx + 2] + jitter
      );
    }
    
    positions.needsUpdate = true;
  }
  
  /**
   * Subtle expansion for thinking
   */
  _morphForThinking(mesh, intensity) {
    if (!mesh.geometry.userData.originalPositions) {
      mesh.geometry.userData.originalPositions = 
        mesh.geometry.attributes.position.array.slice();
    }
    
    const positions = mesh.geometry.attributes.position;
    const original = mesh.geometry.userData.originalPositions;
    
    // Expand outward
    const expansionFactor = 1.0 + (intensity * 0.2);
    
    for (let i = 0; i < positions.count; i++) {
      const idx = i * 3;
      positions.setXYZ(i,
        original[idx] * expansionFactor,
        original[idx + 1] * expansionFactor,
        original[idx + 2] * expansionFactor
      );
    }
    
    positions.needsUpdate = true;
  }
}
