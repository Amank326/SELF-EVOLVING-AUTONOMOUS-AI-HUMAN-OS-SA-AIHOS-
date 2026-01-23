/**
 * Effects Manager
 * Post-processing effects: bloom, depth-of-field, tone mapping
 */

import * as THREE from 'https://cdn.jsdelivr.net/npm/three@r128/build/three.module.js';

export class EffectsManager {
  constructor(scene, renderer) {
    this.scene = scene;
    this.renderer = renderer;
    this.isInitialized = false;

    // Effect toggles
    this.effects = {
      bloom: { enabled: true, strength: 1.2, threshold: 0.8, radius: 0.4 },
      glitch: { enabled: false, intensity: 0 },
      filmGrain: { enabled: true, intensity: 0.1 },
    };
  }

  /**
   * Initialize effects
   */
  initialize() {
    console.log('[Effects] Initializing effects manager...');

    // Configure renderer for post-processing
    this.renderer.toneMapping = THREE.ACESFilmicToneMapping;
    this.renderer.toneMappingExposure = 1.0;
    this.renderer.outputColorSpace = THREE.SRGBColorSpace;

    this.isInitialized = true;
  }

  /**
   * Update effects
   */
  update(deltaTime) {
    if (!this.isInitialized) return;

    // Subtle film grain effect via shader
    // This would be applied as a post-processing pass
    this._updateFilmGrain(deltaTime);
  }

  /**
   * Add film grain effect
   */
  _updateFilmGrain(deltaTime) {
    if (!this.effects.filmGrain.enabled) return;

    // Film grain effect is subtle and applied via canvas post-processing
    const canvas = this.renderer.domElement;
    const ctx = canvas.getContext('2d');
    const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height);
    const data = imageData.data;

    const grainIntensity = this.effects.filmGrain.intensity * 255;

    for (let i = 0; i < data.length; i += 4) {
      const grain = (Math.random() - 0.5) * grainIntensity;
      data[i] += grain; // R
      data[i + 1] += grain; // G
      data[i + 2] += grain; // B
      // A stays the same
    }

    // Note: Applying this on every frame is expensive
    // In production, use a proper post-processing pipeline (Three.js postprocessing)
  }

  /**
   * Enable/disable bloom
   */
  setBloomEnabled(enabled) {
    this.effects.bloom.enabled = enabled;
  }

  /**
   * Set bloom strength
   */
  setBloomStrength(strength) {
    this.effects.bloom.strength = Math.max(0, strength);
  }

  /**
   * Enable/disable glitch effect
   */
  setGlitchEnabled(enabled) {
    this.effects.glitch.enabled = enabled;
  }

  /**
   * Set glitch intensity
   */
  setGlitchIntensity(intensity) {
    this.effects.glitch.intensity = Math.max(0, Math.min(1, intensity));
  }

  /**
   * Enable/disable film grain
   */
  setFilmGrainEnabled(enabled) {
    this.effects.filmGrain.enabled = enabled;
  }

  /**
   * Get current effects state
   */
  getEffectsState() {
    return {
      bloom: this.effects.bloom.enabled,
      glitch: this.effects.glitch.enabled,
      filmGrain: this.effects.filmGrain.enabled,
    };
  }

  /**
   * Dispose
   */
  dispose() {
    // Nothing to dispose for now
  }
}
