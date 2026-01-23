/**
 * Lighting System
 * Multi-light setup with dynamic properties, shadows, and color themes
 */

import * as THREE from 'https://cdn.jsdelivr.net/npm/three@r128/build/three.module.js';

export class LightingSystem {
  constructor(scene) {
    this.scene = scene;
    this.lights = {
      primary: null,
      secondary: null,
      accent: null,
      ambient: null,
      rim: null,
    };

    this.animationState = {
      time: 0,
    };

    this.colorThemes = {
      cyan: {
        primary: 0x00ffaa,
        secondary: 0xff0055,
        accent: 0x0088ff,
        ambient: 0x0a1a2e,
      },
      purple: {
        primary: 0xaa00ff,
        secondary: 0xff00aa,
        accent: 0x00ffff,
        ambient: 0x1a0a2e,
      },
      red: {
        primary: 0xff0055,
        secondary: 0x00ffaa,
        accent: 0xff8800,
        ambient: 0x2e0a0a,
      },
      blue: {
        primary: 0x0088ff,
        secondary: 0x00ffff,
        accent: 0x0055ff,
        ambient: 0x0a0a2e,
      },
    };

    this.currentTheme = 'cyan';
  }

  /**
   * Setup all lights
   */
  setupLights() {
    console.log('[Lighting] Setting up lights...');

    // Ambient light
    this.lights.ambient = new THREE.AmbientLight(0xffffff, 0.2);
    this.scene.add(this.lights.ambient);

    // Primary light (key light)
    this.lights.primary = new THREE.PointLight(0x00ffaa, 2.0, 10);
    this.lights.primary.position.set(3, 2, 2);
    this.lights.primary.castShadow = true;
    this.lights.primary.shadow.mapSize.width = 2048;
    this.lights.primary.shadow.mapSize.height = 2048;
    this.lights.primary.shadow.camera.near = 0.1;
    this.lights.primary.shadow.camera.far = 20;
    this.lights.primary.shadow.radius = 4;
    this.scene.add(this.lights.primary);

    // Secondary light (fill light)
    this.lights.secondary = new THREE.PointLight(0xff0055, 1.5, 10);
    this.lights.secondary.position.set(-3, -1, 2);
    this.lights.secondary.castShadow = true;
    this.lights.secondary.shadow.mapSize.width = 2048;
    this.lights.secondary.shadow.mapSize.height = 2048;
    this.scene.add(this.lights.secondary);

    // Accent light (rim light)
    this.lights.accent = new THREE.PointLight(0x0088ff, 1.2, 10);
    this.lights.accent.position.set(0, 3, -3);
    this.lights.accent.castShadow = false;
    this.scene.add(this.lights.accent);

    // Directional light for overall scene illumination
    this.lights.rim = new THREE.DirectionalLight(0xffffff, 0.3);
    this.lights.rim.position.set(5, 5, 5);
    this.lights.rim.castShadow = true;
    this.scene.add(this.lights.rim);
  }

  /**
   * Update lights based on animation
   */
  updateAnimation(deltaTime, animationState) {
    this.animationState = animationState;
    this._updateLightPositions(deltaTime);
    this._updateLightIntensities();
  }

  /**
   * Update light positions for orbital motion
   */
  _updateLightPositions(deltaTime) {
    const time = this.animationState.time || 0;
    const speed = 0.5;

    // Orbital primary light
    if (this.lights.primary) {
      const angle1 = time * speed;
      this.lights.primary.position.x = Math.cos(angle1) * 3.5;
      this.lights.primary.position.y = 2 + Math.sin(angle1 * 0.3) * 1.5;
      this.lights.primary.position.z = Math.sin(angle1) * 3.5;
    }

    // Orbital secondary light
    if (this.lights.secondary) {
      const angle2 = time * speed + Math.PI;
      this.lights.secondary.position.x = Math.cos(angle2) * 3.5;
      this.lights.secondary.position.y = -1 + Math.sin(angle2 * 0.5) * 1.5;
      this.lights.secondary.position.z = Math.sin(angle2) * 3.5;
    }

    // Static accent light with vertical oscillation
    if (this.lights.accent) {
      this.lights.accent.position.y = 3 + Math.sin(time * 1.5) * 1.0;
    }
  }

  /**
   * Update light intensities based on animation state
   */
  _updateLightIntensities() {
    const pulseIntensity = this.animationState.pulseIntensity || 0.5;

    if (this.lights.primary) {
      this.lights.primary.intensity = 2.0 * (0.8 + pulseIntensity * 0.4);
    }

    if (this.lights.secondary) {
      this.lights.secondary.intensity = 1.5 * (0.8 + pulseIntensity * 0.4);
    }

    if (this.lights.accent) {
      this.lights.accent.intensity = 1.2 * (0.7 + pulseIntensity * 0.5);
    }
  }

  /**
   * Set color theme
   */
  setColorTheme(themeId) {
    const theme = this.colorThemes[themeId] || this.colorThemes.cyan;
    this.currentTheme = themeId;

    if (this.lights.primary) {
      this.lights.primary.color.setHex(theme.primary);
    }

    if (this.lights.secondary) {
      this.lights.secondary.color.setHex(theme.secondary);
    }

    if (this.lights.accent) {
      this.lights.accent.color.setHex(theme.accent);
    }

    if (this.lights.ambient) {
      this.lights.ambient.color.setHex(theme.ambient);
    }
  }

  /**
   * Set light intensity globally
   */
  setIntensity(factor) {
    const factor_clamped = Math.max(0, Math.min(2, factor));

    if (this.lights.primary) this.lights.primary.intensity *= factor_clamped;
    if (this.lights.secondary) this.lights.secondary.intensity *= factor_clamped;
    if (this.lights.accent) this.lights.accent.intensity *= factor_clamped;
    if (this.lights.ambient) this.lights.ambient.intensity *= factor_clamped;
  }

  /**
   * Get all lights
   */
  getLights() {
    return { ...this.lights };
  }

  /**
   * Dispose
   */
  dispose() {
    for (const key in this.lights) {
      if (this.lights[key]) {
        this.scene.remove(this.lights[key]);
      }
    }
  }
}
