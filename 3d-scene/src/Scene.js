/**
 * SA-AIHOS 3D Scene Manager
 * Core Three.js scene initialization, rendering loop, and lifecycle management
 * 
 * Architecture:
 * - Scene: Canvas container and coordinate system
 * - Camera: Orbital camera with dynamic positioning
 * - Renderer: WebGL with optimized settings
 * - Components: AI-Core, Particles, Effects
 * - Animations: Continuous micro-animations
 * - Lighting: Multi-light system with dynamic properties
 */

import * as THREE from 'https://cdn.jsdelivr.net/npm/three@r128/build/three.module.js';
import { AICore } from './components/AICore.js';
import { AnimationController } from './animations/AnimationController.js';
import { ProceduralAnimationController } from './animations/ProceduralAnimationController.js';
import { AIResponsiveComponentManager } from './components/AIResponsiveComponentManager.js';
import { InteractionResponsiveController } from './animations/InteractionResponsiveController.js';
import { GestureAnimationEngine } from './animations/GestureAnimationEngine.js';
import { LightingSystem } from './lighting/LightingSystem.js';
import { EffectsManager } from './effects/EffectsManager.js';
import { AndroidBridge } from './bridge/AndroidBridge.js';

export class SAIHOSScene {
  constructor(containerSelector = '#canvas-container') {
    // Container & DOM
    this.container = document.querySelector(containerSelector);
    if (!this.container) throw new Error(`Container not found: ${containerSelector}`);

    // Scene essentials
    this.scene = null;
    this.camera = null;
    this.renderer = null;
    this.canvas = null;

    // Components
    this.aiCore = null;
    this.animationController = null;
    this.proceduralAnimationController = null;
    this.componentManager = null;
    this.interactionResponsiveController = null;
    this.gestureAnimationEngine = null;
    this.lightingSystem = null;
    this.effectsManager = null;

    // State
    this.isAnimating = true;
    this.isInitialized = false;
    this.frameCount = 0;
    this.deltaTime = 0;
    this.lastFrameTime = Date.now();

    // Configuration
    this.config = {
      camera: {
        fov: 75,
        near: 0.1,
        far: 10000,
        initialDistance: 3,
        orbitRadius: 3.5,
      },
      renderer: {
        antialias: true,
        alpha: true,
        pixelRatio: window.devicePixelRatio || 1,
        shadowMap: true,
        outputEncoding: THREE.sRGBEncoding,
      },
      scene: {
        background: new THREE.Color(0x0a0e27), // Deep space blue
        fog: new THREE.Fog(0x0a0e27, 5, 20),
      },
    };

    // Bind methods
    this.render = this.render.bind(this);
    this.onWindowResize = this.onWindowResize.bind(this);

    // Android Bridge
    this.bridge = new AndroidBridge(this);
  }

  /**
   * Initialize the entire 3D scene
   */
  async initialize() {
    try {
      console.log('[SA-AIHOS 3D] Initializing scene...');

      // Setup basic Three.js elements
      this._setupScene();
      this._setupCamera();
      this._setupRenderer();

      // Initialize subsystems
      this.lightingSystem = new LightingSystem(this.scene);
      this.lightingSystem.setupLights();

      this.aiCore = new AICore(this.scene);
      await this.aiCore.initialize();

      this.effectsManager = new EffectsManager(this.scene, this.renderer);
      this.effectsManager.initialize();

      this.animationController = new AnimationController();
      this.animationController.registerAnimatable(this.aiCore);
      this.animationController.registerAnimatable(this.lightingSystem);

      // Initialize AI-driven procedural animation system
      this.proceduralAnimationController = new ProceduralAnimationController();
      
      // Initialize interaction system
      this.interactionResponsiveController = new InteractionResponsiveController(this);
      this.gestureAnimationEngine = new GestureAnimationEngine();
      
      // Initialize component manager to apply animations to 3D objects
      this.componentManager = new AIResponsiveComponentManager(
        this.scene,
        this.aiCore,
        this.lightingSystem,
        this.effectsManager
      );
      
      // Wire procedural controller to component manager
      this.proceduralAnimationController.registerAnimatable({
        updateAnimation: (frame) => this.componentManager.applyAnimationFrame(frame)
      });

      // Setup event listeners
      this._setupEventListeners();

      // Start rendering
      this.start();
      this.isInitialized = true;

      console.log('[SA-AIHOS 3D] Scene initialized successfully');
      this.bridge.notifyInitialized();

      return true;
    } catch (error) {
      console.error('[SA-AIHOS 3D] Initialization failed:', error);
      this.bridge.notifyError('initialization_failed', error.message);
      throw error;
    }
  }

  /**
   * Setup Three.js scene
   */
  _setupScene() {
    this.scene = new THREE.Scene();
    this.scene.background = this.config.scene.background;
    this.scene.fog = this.config.scene.fog;

    // Add a subtle grid helper for reference (remove in production)
    // const gridHelper = new THREE.GridHelper(10, 20, 0x444444, 0x222222);
    // gridHelper.position.y = -2;
    // this.scene.add(gridHelper);
  }

  /**
   * Setup camera with orbital positioning
   */
  _setupCamera() {
    const width = this.container.clientWidth;
    const height = this.container.clientHeight;
    const aspect = width / height;

    this.camera = new THREE.PerspectiveCamera(
      this.config.camera.fov,
      aspect,
      this.config.camera.near,
      this.config.camera.far
    );

    this.camera.position.set(0, 0, this.config.camera.initialDistance);
    this.camera.lookAt(0, 0, 0);
  }

  /**
   * Setup WebGL renderer with optimizations
   */
  _setupRenderer() {
    this.renderer = new THREE.WebGLRenderer({
      antialias: this.config.renderer.antialias,
      alpha: this.config.renderer.alpha,
      powerPreference: 'high-performance',
    });

    this.renderer.setPixelRatio(this.config.renderer.pixelRatio);
    this.renderer.setSize(this.container.clientWidth, this.container.clientHeight);
    this.renderer.outputColorSpace = THREE.SRGBColorSpace;
    this.renderer.shadowMap.enabled = this.config.renderer.shadowMap;
    this.renderer.shadowMap.type = THREE.PCFShadowShadowMap;
    this.renderer.shadowMap.autoUpdate = true;

    this.canvas = this.renderer.domElement;
    this.canvas.style.display = 'block';
    this.container.appendChild(this.canvas);
  }

  /**
   * Setup event listeners
   */
  _setupEventListeners() {
    window.addEventListener('resize', this.onWindowResize);
    document.addEventListener('visibilitychange', () => {
      if (document.hidden) {
        this.pause();
      } else {
        this.resume();
      }
    });

    // Mouse/Touch interaction - forward to interaction system
    this.container.addEventListener('mousemove', (e) => {
      const rect = this.container.getBoundingClientRect();
      const x = (e.clientX - rect.left) / rect.width;
      const y = (e.clientY - rect.top) / rect.height;
      this.bridge.notifyMouseMove(x, y);
    });

    this.container.addEventListener('click', () => {
      this.bridge.notifyClick();
    });

    // Touch events for mobile
    this.container.addEventListener('touchstart', (e) => this._handleTouchEvent(e));
    this.container.addEventListener('touchmove', (e) => this._handleTouchEvent(e));
    this.container.addEventListener('touchend', (e) => this._handleTouchEvent(e));
  }

  /**
   * Handle touch events and forward to Android bridge
   */
  _handleTouchEvent(event) {
    if (event.touches.length > 0) {
      const touch = event.touches[0];
      const rect = this.container.getBoundingClientRect();
      const x = (touch.clientX - rect.left) / rect.width;
      const y = (touch.clientY - rect.top) / rect.height;
      
      // Forward to Android if available
      if (this.bridge && this.bridge.sendToAndroid) {
        this.bridge.sendToAndroid('touchEvent', {
          x: x,
          y: y,
          pressure: touch.force || 0.5,
          type: event.type,
        });
      }
    }
  }

  /**
   * Render loop
   */
  render() {
    if (!this.isAnimating) return;

    const now = Date.now();
    this.deltaTime = (now - this.lastFrameTime) / 1000;
    this.lastFrameTime = now;

    // Update PROCEDURAL animations based on AI state
    if (this.proceduralAnimationController) {
      this.proceduralAnimationController.update(this.deltaTime);
    }

    // Update interaction-based animations
    if (this.interactionResponsiveController) {
      this.interactionResponsiveController.update(this.deltaTime);
    }

    // Update gesture effects
    if (this.gestureAnimationEngine) {
      this.gestureAnimationEngine.computeEffects(this.deltaTime);
    }

    // Update traditional animations (fallback)
    if (this.animationController) {
      this.animationController.update(this.deltaTime);
    }

    // Update lighting
    if (this.lightingSystem) {
      this.lightingSystem.update(this.deltaTime);
    }

    // Update effects
    if (this.effectsManager) {
      this.effectsManager.update(this.deltaTime);
    }

    // Update camera orbit
    this._updateCameraOrbit(this.deltaTime);

    // Render
    this.renderer.render(this.scene, this.camera);

    this.frameCount++;
    requestAnimationFrame(this.render);
  }

  /**
   * Update camera orbital motion
   */
  _updateCameraOrbit(deltaTime) {
    const orbitSpeed = 0.2; // radians per second
    const orbitRadius = this.config.camera.orbitRadius;
    const angle = (this.frameCount * deltaTime * orbitSpeed) % (Math.PI * 2);

    this.camera.position.x = Math.cos(angle) * orbitRadius;
    this.camera.position.y = Math.sin(angle * 0.5) * orbitRadius * 0.6;
    this.camera.position.z = Math.sin(angle) * orbitRadius * 1.2;

    this.camera.lookAt(0, 0, 0);
  }

  /**
   * Handle window resize
   */
  onWindowResize() {
    const width = this.container.clientWidth;
    const height = this.container.clientHeight;

    this.camera.aspect = width / height;
    this.camera.updateProjectionMatrix();
    this.renderer.setSize(width, height);

    this.bridge.notifyResize(width, height);
  }

  /**
   * Start rendering
   */
  start() {
    this.isAnimating = true;
    this.lastFrameTime = Date.now();
    this.render();
  }

  /**
   * Pause rendering
   */
  pause() {
    this.isAnimating = false;
  }

  /**
   * Resume rendering
   */
  resume() {
    if (!this.isAnimating) {
      this.isAnimating = true;
      this.lastFrameTime = Date.now();
      this.render();
    }
  }

  /**
   * Get current performance metrics
   */
  getMetrics() {
    return {
      frameCount: this.frameCount,
      deltaTime: this.deltaTime,
      isAnimating: this.isAnimating,
      cameraPosition: this.camera.position,
      cameraRotation: this.camera.rotation,
      sceneObjectCount: this.scene.children.length,
    };
  }

  /**
   * Update animation intensity (0.0 - 1.0)
   */
  setAnimationIntensity(intensity) {
    if (this.animationController) {
      this.animationController.setIntensity(intensity);
    }
  }

  /**
   * Update color theme
   */
  setColorTheme(themeId) {
    if (this.aiCore) {
      this.aiCore.setColorTheme(themeId);
    }
    if (this.lightingSystem) {
      this.lightingSystem.setColorTheme(themeId);
    }
  }

  /**
   * Dispose of all resources
   */
  /**
   * Receive interaction state from Android
   * Updates 3D animations based on user touch and device context
   */
  setInteractionState(interactionStateJson) {
    try {
      const interactionState = typeof interactionStateJson === 'string' 
        ? JSON.parse(interactionStateJson)
        : interactionStateJson;
      
      if (this.interactionResponsiveController) {
        this.interactionResponsiveController.setInteractionState(interactionState);
      }
      
      console.debug('[SA-AIHOS 3D] Interaction state:', {
        gesture: interactionState.gestureType,
        idling: interactionState.isIdling,
        context: interactionState.contextScore.toFixed(2),
        energy: interactionState.getInteractionEnergy?.() || 0,
      });
    } catch (error) {
      console.error('[SA-AIHOS 3D] Error setting interaction state:', error);
    }
  }

  /**
   * Handle gesture animations from Android
   */
  onGesture(gestureType, intensity) {
    try {
      if (!this.gestureAnimationEngine) return;
      
      switch (gestureType) {
        case 'TAP':
          this.gestureAnimationEngine.applyTapEffect({ x: 0, y: 0 }, null);
          break;
        case 'LONG_PRESS':
          this.gestureAnimationEngine.activateReflectionMode();
          break;
        case 'SWIPE':
          this.gestureAnimationEngine.applySweepEffect({ x: 1, y: 0 }, intensity);
          break;
        case 'PINCH':
          this.gestureAnimationEngine.applyPinchEffect(intensity);
          break;
        case 'DOUBLE_TAP':
          this.gestureAnimationEngine.applyDoubleTabEffect();
          break;
        case 'TWO_FINGER_ROTATE':
          this.gestureAnimationEngine.applyTwoFingerRotationEffect(intensity);
          break;
      }
      
      console.log('[SA-AIHOS 3D] Gesture:', gestureType, 'intensity:', intensity);
    } catch (error) {
      console.error('[SA-AIHOS 3D] Error handling gesture:', error);
    }
  }

  /**
   * Receive AI motion state from Android
   * This is the primary method that drives procedural animations
   */
  setAIMotionState(aiMotionStateJson) {
    try {
      const aiState = typeof aiMotionStateJson === 'string' 
        ? JSON.parse(aiMotionStateJson)
        : aiMotionStateJson;
      
      console.log('[SA-AIHOS 3D] Received AI motion state:', aiState.primaryState);
      
      // Pass AI state to procedural animation controller
      if (this.proceduralAnimationController) {
        this.proceduralAnimationController.setAIMotionState(aiState);
      }
      
      // Log for debugging
      console.debug('AI Metrics:', {
        state: aiState.primaryState,
        confidence: aiState.confidence.averageConfidence.toFixed(2),
        load: aiState.processing.cognitiveLoad.toFixed(2),
        breathing: aiState.breathingRate.toFixed(2),
        rotation: aiState.rotationSpeed.toFixed(2)
      });
    } catch (error) {
      console.error('[SA-AIHOS 3D] Error setting AI motion state:', error);
      this.bridge.notifyError('ai_state_error', error.message);
    }
  }

  /**
   * Get current animation metrics for display/debugging
   */
  getAnimationMetrics() {
    const interactionMetrics = this.interactionResponsiveController?.getMetrics() || {};
    const gestureMetrics = this.gestureAnimationEngine?.getMetrics() || {};
    
    return {
      elapsedTime: this.proceduralAnimationController?.elapsedTime || 0,
      frameCount: this.frameCount,
      currentAIState: this.proceduralAnimationController?.aiMotionState?.primaryState || 'IDLE',
      interaction: interactionMetrics,
      gesture: gestureMetrics,
      fps: this.calculateFPS(),
    };
  }

  dispose() {
    console.log('[SA-AIHOS 3D] Disposing scene...');

    window.removeEventListener('resize', this.onWindowResize);

    if (this.animationController) {
      this.animationController.dispose();
    }

    if (this.aiCore) {
      this.aiCore.dispose();
    }

    if (this.effectsManager) {
      this.effectsManager.dispose();
    }

    if (this.lightingSystem) {
      this.lightingSystem.dispose();
    }

    if (this.renderer) {
      this.renderer.dispose();
    }

    if (this.canvas && this.canvas.parentNode) {
      this.canvas.parentNode.removeChild(this.canvas);
    }

    this.scene = null;
    this.camera = null;
    this.renderer = null;
    this.isInitialized = false;
  }
}

// Export for global use
window.SAIHOSScene = SAIHOSScene;
