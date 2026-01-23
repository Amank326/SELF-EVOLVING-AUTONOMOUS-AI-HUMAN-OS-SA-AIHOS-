/**
 * Quality Manager
 * Adaptive quality scaling for mobile and desktop
 * 
 * Features:
 * - Automatic detection of device capability
 * - Real-time FPS monitoring and quality adjustment
 * - Profile-based quality levels (LOW, MEDIUM, HIGH)
 * - Configurable thresholds and performance targets
 * 
 * Quality affects:
 * - Particle counts and emission rates
 * - Shader complexity (detail level)
 * - Lighting quality (shadow maps, effects)
 * - Update frequencies
 * - Texture resolution
 * 
 * Philosophy: Maintain premium feel at all quality levels
 * - LOW: Optimized for 30-40 FPS on budget devices
 * - MEDIUM: Target 50-60 FPS on mid-range devices
 * - HIGH: Target 60 FPS on flagship devices
 */
export class QualityManager {
  constructor(renderer, scene) {
    this.renderer = renderer;
    this.scene = scene;

    // Quality level (AUTO, LOW, MEDIUM, HIGH)
    this.qualityLevel = 'AUTO';
    this.autoQualityEnabled = true;

    // FPS tracking
    this.fpsHistory = [];
    this.maxFpsHistorySize = 60;  // Track last 60 frames
    this.lastFpsUpdate = Date.now();
    this.fpsUpdateInterval = 500;  // Update every 500ms
    this.currentFps = 60;

    // Quality settings per level
    this.qualityProfiles = {
      LOW: {
        particleEmissionMultiplier: 0.3,
        particleMaxCount: 1000,
        shadowMapEnabled: false,
        lightCount: 2,
        effectsQuality: 'low',
        updateFrequency: 0.5,  // Update every other frame
        textureResolution: 0.5,
        reflectionProbeEnabled: false,
        antialiasLevel: 1,
        description: 'Budget devices: 30-40 FPS',
      },
      MEDIUM: {
        particleEmissionMultiplier: 0.7,
        particleMaxCount: 3000,
        shadowMapEnabled: true,
        lightCount: 4,
        effectsQuality: 'medium',
        updateFrequency: 1.0,  // Every frame
        textureResolution: 0.8,
        reflectionProbeEnabled: true,
        antialiasLevel: 1,
        description: 'Mid-range: 50-60 FPS',
      },
      HIGH: {
        particleEmissionMultiplier: 1.0,
        particleMaxCount: 5000,
        shadowMapEnabled: true,
        lightCount: 6,
        effectsQuality: 'high',
        updateFrequency: 1.0,
        textureResolution: 1.0,
        reflectionProbeEnabled: true,
        antialiasLevel: 2,
        description: 'Flagship: 60 FPS',
      },
    };

    // Current active profile
    this.activeProfile = this.qualityProfiles.MEDIUM;

    // Performance targets
    this.performanceTargets = {
      targetFps: 60,
      minFps: 30,
      maxFps: 60,
      qualityDowngradeThreshold: 40,  // Drop quality if FPS < 40
      qualityUpgradeThreshold: 55,    // Upgrade quality if FPS > 55
      evaluationInterval: 2000,        // Evaluate every 2 seconds
    };

    // Device detection
    this.deviceTier = this._detectDeviceTier();

    // Change listeners
    this.onQualityChange = null;

    console.log(`[QualityManager] Initialized: Device tier = ${this.deviceTier}`);
  }

  /**
   * Initialize quality manager
   * Set initial quality level based on device
   */
  initialize() {
    if (this.autoQualityEnabled) {
      const suggestedQuality = this._suggestQualityForDevice();
      this.setQuality(suggestedQuality);
      console.log(`[QualityManager] Auto-detected quality: ${suggestedQuality}`);
    }
  }

  /**
   * Detect device capability tier
   */
  _detectDeviceTier() {
    // Get GPU info
    const canvas = this.renderer.domElement;
    const gl = canvas.getContext('webgl') || canvas.getContext('experimental-webgl');
    const debugInfo = gl.getExtension('WEBGL_debug_renderer_info');
    
    let gpuInfo = 'unknown';
    if (debugInfo) {
      gpuInfo = gl.getParameter(debugInfo.UNMASKED_RENDERER_WEBGL);
    }

    // Check device memory
    const deviceMemory = navigator.deviceMemory || 8;
    
    // Check GPU cores (approximate)
    const cores = navigator.hardwareConcurrency || 4;

    // Classify tier
    if (deviceMemory <= 4 || gpuInfo.includes('Adreno 505') || gpuInfo.includes('Mali-G71')) {
      return 'budget';
    } else if (deviceMemory >= 8 && cores >= 8) {
      return 'flagship';
    }
    return 'midrange';
  }

  /**
   * Suggest quality level based on device tier
   */
  _suggestQualityForDevice() {
    switch (this.deviceTier) {
      case 'budget':
        return 'LOW';
      case 'flagship':
        return 'HIGH';
      default:
        return 'MEDIUM';
    }
  }

  /**
   * Set quality level (LOW, MEDIUM, HIGH, AUTO)
   */
  setQuality(level) {
    if (level === 'AUTO') {
      this.autoQualityEnabled = true;
      level = this._suggestQualityForDevice();
    } else {
      this.autoQualityEnabled = false;
    }

    if (!this.qualityProfiles[level]) {
      console.warn(`[QualityManager] Unknown quality level: ${level}`);
      return;
    }

    if (this.qualityLevel === level) return;  // No change

    this.qualityLevel = level;
    this.activeProfile = this.qualityProfiles[level];

    this._applyQualityProfile();

    console.log(`[QualityManager] Quality changed to: ${level}`);
    console.log(`[QualityManager] ${this.activeProfile.description}`);

    if (this.onQualityChange) {
      this.onQualityChange(level, this.activeProfile);
    }
  }

  /**
   * Apply quality profile to scene and renderer
   */
  _applyQualityProfile() {
    const profile = this.activeProfile;

    // Renderer settings
    this.renderer.shadowMap.enabled = profile.shadowMapEnabled;
    
    // If available, set shadow map type based on quality
    if (profile.shadowMapEnabled) {
      // VSM shadows for better quality
      if (profile.effectsQuality === 'high') {
        this.renderer.shadowMap.type = THREE.VSMShadowMap;
      } else {
        this.renderer.shadowMap.type = THREE.PCFShadowMap;
      }
    }

    // Update all lights in scene
    this._configureLighting();

    // Update particle emission settings (if available)
    this._configureParticles();

    // Notify attached systems of quality change
    this._notifyQualityChange();
  }

  /**
   * Configure lighting based on quality
   */
  _configureLighting() {
    const profile = this.activeProfile;
    const lights = this.scene.children.filter(obj => obj.isLight);

    // Disable excess lights for lower quality
    lights.forEach((light, index) => {
      if (index < profile.lightCount) {
        light.visible = true;
      } else {
        light.visible = false;
      }
    });

    // Adjust shadow properties for lower quality
    lights.forEach(light => {
      if (light.shadow) {
        if (profile.effectsQuality === 'low') {
          light.shadow.mapSize.width = 512;
          light.shadow.mapSize.height = 512;
          light.shadow.bias = 0.001;
        } else {
          light.shadow.mapSize.width = 1024;
          light.shadow.mapSize.height = 1024;
          light.shadow.bias = 0.0001;
        }
      }
    });
  }

  /**
   * Configure particle systems based on quality
   */
  _configureParticles() {
    const profile = this.activeProfile;
    // This is called after particle systems are initialized
    // Particle systems should listen to onQualityChange
  }

  /**
   * Notify quality change to all registered listeners
   */
  _notifyQualityChange() {
    // Dispatch event or callback for systems to adapt
    const event = new CustomEvent('qualityChange', {
      detail: {
        level: this.qualityLevel,
        profile: this.activeProfile,
      },
    });
    window.dispatchEvent(event);
  }

  /**
   * Update FPS tracking
   * Called every frame from render loop
   */
  recordFrameTime(deltaTime) {
    this.fpsHistory.push(deltaTime);
    if (this.fpsHistory.length > this.maxFpsHistorySize) {
      this.fpsHistory.shift();
    }

    // Update FPS every interval
    const now = Date.now();
    if (now - this.lastFpsUpdate > this.fpsUpdateInterval) {
      this.currentFps = this._calculateAverageFps();
      this.lastFpsUpdate = now;

      // Auto-adjust quality if enabled
      if (this.autoQualityEnabled) {
        this._evaluateQuality();
      }
    }
  }

  /**
   * Calculate average FPS from history
   */
  _calculateAverageFps() {
    if (this.fpsHistory.length === 0) return 60;

    const avgDeltaTime = this.fpsHistory.reduce((a, b) => a + b, 0) / this.fpsHistory.length;
    return avgDeltaTime > 0 ? 1 / avgDeltaTime : 60;
  }

  /**
   * Evaluate quality and auto-adjust if needed
   */
  _evaluateQuality() {
    const fps = this.currentFps;
    const targets = this.performanceTargets;

    // Downgrade quality if FPS too low
    if (fps < targets.qualityDowngradeThreshold) {
      const currentIndex = this._getQualityIndex(this.qualityLevel);
      if (currentIndex > 0) {  // Can go lower
        const lowerQuality = ['LOW', 'MEDIUM', 'HIGH'][currentIndex - 1];
        console.warn(`[QualityManager] FPS dropped to ${fps.toFixed(1)}, downgrading to ${lowerQuality}`);
        this.setQuality(lowerQuality);
      }
    }
    // Upgrade quality if FPS stable and high
    else if (fps > targets.qualityUpgradeThreshold) {
      const currentIndex = this._getQualityIndex(this.qualityLevel);
      if (currentIndex < 2) {  // Can go higher
        const higherQuality = ['LOW', 'MEDIUM', 'HIGH'][currentIndex + 1];
        console.log(`[QualityManager] FPS stable at ${fps.toFixed(1)}, upgrading to ${higherQuality}`);
        this.setQuality(higherQuality);
      }
    }
  }

  /**
   * Get quality level index
   */
  _getQualityIndex(level) {
    const levels = ['LOW', 'MEDIUM', 'HIGH'];
    return levels.indexOf(level);
  }

  /**
   * Get current metrics
   */
  getMetrics() {
    return {
      quality: this.qualityLevel,
      fps: this.currentFps.toFixed(1),
      deviceTier: this.deviceTier,
      autoEnabled: this.autoQualityEnabled,
      profile: this.activeProfile,
      fpsHistory: this.fpsHistory.slice(-10),  // Last 10 frames
    };
  }

  /**
   * Get quality recommendation for UI display
   */
  getQualityRecommendation() {
    const fps = this.currentFps;
    
    if (fps >= 55) {
      return 'Excellent';
    } else if (fps >= 45) {
      return 'Good';
    } else if (fps >= 30) {
      return 'Acceptable';
    } else {
      return 'Poor';
    }
  }
}
