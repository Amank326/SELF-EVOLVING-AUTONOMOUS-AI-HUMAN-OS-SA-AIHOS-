/**
 * AI-Core Visual Component
 * Futuristic crystal/neural/energy orb with multiple layers
 * 
 * Composition:
 * - Core Crystal: Main geometric form (icosahedron)
 * - Neural Network: Animated particle connections
 * - Energy Field: Pulsing aura layer
 * - Particle System: Floating energy particles
 */

import * as THREE from 'https://cdn.jsdelivr.net/npm/three@r128/build/three.module.js';

export class AICore {
  constructor(scene) {
    this.scene = scene;
    this.group = new THREE.Group();
    this.scene.add(this.group);

    // Sub-components
    this.coreCrystal = null;
    this.neuralNetwork = null;
    this.energyField = null;
    this.particleSystem = null;

    // Animation state
    this.animationState = {
      rotationX: 0,
      rotationY: 0,
      rotationZ: 0,
      breathIntensity: 0,
      pulseIntensity: 0,
      colorPhase: 0,
    };

    // Configuration
    this.config = {
      coreScale: 1.0,
      colors: {
        primary: new THREE.Color(0x00ff88), // Cyan green
        secondary: new THREE.Color(0xff0088), // Magenta
        tertiary: new THREE.Color(0x0088ff), // Cyan blue
        glow: new THREE.Color(0x00ffff), // Bright cyan
      },
      materialIntensity: 1.0,
    };
  }

  /**
   * Initialize AI-Core geometry and materials
   */
  async initialize() {
    console.log('[AI-Core] Initializing...');

    this._createCoreCrystal();
    this._createNeuralNetwork();
    this._createEnergyField();
    this._createParticleSystem();

    return true;
  }

  /**
   * Create main crystal core geometry
   */
  _createCoreCrystal() {
    const geometry = new THREE.IcosahedronGeometry(0.8, 5);

    // Create shader material for crystal
    const material = new THREE.ShaderMaterial({
      uniforms: {
        time: { value: 0 },
        baseColor: { value: this.config.colors.primary },
        glowIntensity: { value: 1.5 },
      },
      vertexShader: `
        varying vec3 vNormal;
        varying vec3 vPosition;
        varying float vDepth;

        void main() {
          vNormal = normalize(normalMatrix * normal);
          vPosition = (modelMatrix * vec4(position, 1.0)).xyz;
          vDepth = gl_Position.z;
          
          gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
        }
      `,
      fragmentShader: `
        uniform vec3 baseColor;
        uniform float glowIntensity;
        uniform float time;

        varying vec3 vNormal;
        varying vec3 vPosition;
        varying float vDepth;

        void main() {
          // Fresnel effect
          vec3 viewDir = normalize(cameraPosition - vPosition);
          float fresnel = pow(1.0 - dot(viewDir, vNormal), 3.0);
          
          // Pulsing brightness
          float pulse = 0.5 + 0.5 * sin(time * 2.0);
          
          // Base color with fresnel
          vec3 color = mix(baseColor * 0.3, baseColor, fresnel);
          
          // Add glow
          color += baseColor * glowIntensity * fresnel * pulse;
          
          gl_FragColor = vec4(color, 0.9 + fresnel * 0.1);
        }
      `,
      transparent: true,
      side: THREE.DoubleSide,
      wireframe: false,
    });

    this.coreCrystal = new THREE.Mesh(geometry, material);
    this.coreCrystal.castShadow = true;
    this.coreCrystal.receiveShadow = true;
    this.group.add(this.coreCrystal);
  }

  /**
   * Create neural network connections (particles + lines)
   */
  _createNeuralNetwork() {
    const particleCount = 40;
    const positions = new Float32Array(particleCount * 3);
    const indices = new Uint16Array(particleCount * 2);

    // Create particle positions in spherical distribution
    for (let i = 0; i < particleCount; i++) {
      const theta = Math.random() * Math.PI * 2;
      const phi = Math.random() * Math.PI;
      const radius = 1.2 + Math.random() * 0.3;

      positions[i * 3] = Math.sin(phi) * Math.cos(theta) * radius;
      positions[i * 3 + 1] = Math.cos(phi) * radius;
      positions[i * 3 + 2] = Math.sin(phi) * Math.sin(theta) * radius;
    }

    // Create line geometry for neural connections
    const geometry = new THREE.BufferGeometry();
    geometry.setAttribute('position', new THREE.BufferAttribute(positions, 3));

    // Material for neural lines
    const material = new THREE.LineBasicMaterial({
      color: this.config.colors.secondary,
      transparent: true,
      opacity: 0.4,
      linewidth: 2,
    });

    this.neuralNetwork = new THREE.LineSegments(geometry, material);
    this.group.add(this.neuralNetwork);

    // Store positions for animation
    this.neuralNetwork._originalPositions = new Float32Array(positions);
    this.neuralNetwork._particleCount = particleCount;
  }

  /**
   * Create pulsing energy field aura
   */
  _createEnergyField() {
    const geometry = new THREE.IcosahedronGeometry(1.0, 4);

    const material = new THREE.ShaderMaterial({
      uniforms: {
        time: { value: 0 },
        pulseIntensity: { value: 0 },
      },
      vertexShader: `
        varying vec3 vNormal;
        varying float vDistance;

        void main() {
          vNormal = normalize(normalMatrix * normal);
          vDistance = length(position);
          
          gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
        }
      `,
      fragmentShader: `
        uniform float time;
        uniform float pulseIntensity;

        varying vec3 vNormal;
        varying float vDistance;

        void main() {
          // Pulsing glow
          float pulse = 0.5 + 0.5 * sin(time * 3.0 + vDistance * 10.0);
          float glow = pulse * pulseIntensity;
          
          // Fresnel edge glow
          vec3 viewDir = normalize(cameraPosition - vec3(0.0));
          float fresnel = pow(1.0 - abs(dot(viewDir, vNormal)), 2.0);
          
          vec3 color = vec3(0.0, 1.0, 1.0) * (glow + fresnel * 0.5);
          gl_FragColor = vec4(color, glow * 0.3 + fresnel * 0.2);
        }
      `,
      transparent: true,
      side: THREE.BackSide,
      blending: THREE.AdditiveBlending,
    });

    this.energyField = new THREE.Mesh(geometry, material);
    this.energyField.scale.set(1.3, 1.3, 1.3);
    this.group.add(this.energyField);
  }

  /**
   * Create floating particle system
   */
  _createParticleSystem() {
    const particleCount = 100;
    const geometry = new THREE.BufferGeometry();
    const positions = new Float32Array(particleCount * 3);
    const velocities = new Float32Array(particleCount * 3);
    const sizes = new Float32Array(particleCount);
    const colors = new Float32Array(particleCount * 3);

    for (let i = 0; i < particleCount; i++) {
      // Random positions in sphere
      const theta = Math.random() * Math.PI * 2;
      const phi = Math.random() * Math.PI;
      const r = Math.random() * 2.5;

      positions[i * 3] = Math.sin(phi) * Math.cos(theta) * r;
      positions[i * 3 + 1] = Math.cos(phi) * r;
      positions[i * 3 + 2] = Math.sin(phi) * Math.sin(theta) * r;

      // Random velocities
      velocities[i * 3] = (Math.random() - 0.5) * 0.5;
      velocities[i * 3 + 1] = (Math.random() - 0.5) * 0.5;
      velocities[i * 3 + 2] = (Math.random() - 0.5) * 0.5;

      // Random sizes
      sizes[i] = Math.random() * 0.02 + 0.01;

      // Random colors from palette
      const colorChoice = Math.random();
      if (colorChoice < 0.33) {
        colors[i * 3] = 0.0; colors[i * 3 + 1] = 1.0; colors[i * 3 + 2] = 1.0; // Cyan
      } else if (colorChoice < 0.66) {
        colors[i * 3] = 0.0; colors[i * 3 + 1] = 1.0; colors[i * 3 + 2] = 0.5; // Green
      } else {
        colors[i * 3] = 0.5; colors[i * 3 + 1] = 0.8; colors[i * 3 + 2] = 1.0; // Blue
      }
    }

    geometry.setAttribute('position', new THREE.BufferAttribute(positions, 3));
    geometry.setAttribute('size', new THREE.BufferAttribute(sizes, 1));
    geometry.setAttribute('color', new THREE.BufferAttribute(colors, 3));

    const material = new THREE.PointsMaterial({
      size: 0.02,
      sizeAttenuation: true,
      transparent: true,
      opacity: 0.8,
      vertexColors: true,
      sizeAttenuation: true,
    });

    this.particleSystem = new THREE.Points(geometry, material);
    this.group.add(this.particleSystem);

    // Store data for animation
    this.particleSystem._velocities = velocities;
    this.particleSystem._initialPositions = new Float32Array(positions);
  }

  /**
   * Update animations (called from AnimationController)
   */
  updateAnimation(deltaTime, animationState) {
    // Update animation state
    this.animationState = animationState;

    // Update core crystal
    if (this.coreCrystal) {
      this.coreCrystal.rotation.x += animationState.rotationX;
      this.coreCrystal.rotation.y += animationState.rotationY;
      this.coreCrystal.rotation.z += animationState.rotationZ;

      const breathScale = 1.0 + animationState.breathIntensity * 0.1;
      this.coreCrystal.scale.set(breathScale, breathScale, breathScale);

      // Update shader time
      if (this.coreCrystal.material.uniforms) {
        this.coreCrystal.material.uniforms.time.value += deltaTime;
      }
    }

    // Update energy field
    if (this.energyField) {
      this.energyField.rotation.x += animationState.rotationX * 0.5;
      this.energyField.rotation.y += animationState.rotationY * 0.5;

      if (this.energyField.material.uniforms) {
        this.energyField.material.uniforms.time.value += deltaTime;
        this.energyField.material.uniforms.pulseIntensity.value = animationState.pulseIntensity;
      }
    }

    // Update neural network particles
    if (this.neuralNetwork) {
      const positions = this.neuralNetwork.geometry.attributes.position.array;
      const originalPositions = this.neuralNetwork._originalPositions;

      for (let i = 0; i < this.neuralNetwork._particleCount; i++) {
        const idx = i * 3;
        const wobble = Math.sin(animationState.colorPhase + i) * 0.05;

        positions[idx] = originalPositions[idx] * (1 + wobble);
        positions[idx + 1] = originalPositions[idx + 1] * (1 + wobble);
        positions[idx + 2] = originalPositions[idx + 2] * (1 + wobble);
      }

      this.neuralNetwork.geometry.attributes.position.needsUpdate = true;
      this.neuralNetwork.rotation.z += 0.0005;
    }

    // Update particle system
    if (this.particleSystem) {
      const positions = this.particleSystem.geometry.attributes.position.array;
      const velocities = this.particleSystem._velocities;
      const initialPositions = this.particleSystem._initialPositions;

      for (let i = 0; i < positions.length / 3; i++) {
        const idx = i * 3;

        // Update position with velocity
        positions[idx] += velocities[idx] * deltaTime;
        positions[idx + 1] += velocities[idx + 1] * deltaTime;
        positions[idx + 2] += velocities[idx + 2] * deltaTime;

        // Keep particles in bounds with soft boundaries
        const dist = Math.sqrt(
          positions[idx] ** 2 +
          positions[idx + 1] ** 2 +
          positions[idx + 2] ** 2
        );

        if (dist > 3.0) {
          // Reset to initial position
          positions[idx] = initialPositions[idx];
          positions[idx + 1] = initialPositions[idx + 1];
          positions[idx + 2] = initialPositions[idx + 2];
        }
      }

      this.particleSystem.geometry.attributes.position.needsUpdate = true;
    }
  }

  /**
   * Set color theme
   */
  setColorTheme(themeId) {
    const themes = {
      cyan: { primary: 0x00ff88, secondary: 0xff0088, tertiary: 0x0088ff },
      purple: { primary: 0xaa00ff, secondary: 0xff00aa, tertiary: 0x00ffff },
      red: { primary: 0xff0055, secondary: 0x00ff88, tertiary: 0xff8800 },
      blue: { primary: 0x0088ff, secondary: 0x00ffff, tertiary: 0x0055ff },
    };

    const theme = themes[themeId] || themes.cyan;

    if (this.coreCrystal && this.coreCrystal.material.uniforms) {
      this.coreCrystal.material.uniforms.baseColor.value.setHex(theme.primary);
    }

    if (this.neuralNetwork && this.neuralNetwork.material) {
      this.neuralNetwork.material.color.setHex(theme.secondary);
    }
  }

  /**
   * Dispose of resources
   */
  dispose() {
    if (this.coreCrystal) {
      this.coreCrystal.geometry.dispose();
      this.coreCrystal.material.dispose();
    }

    if (this.neuralNetwork) {
      this.neuralNetwork.geometry.dispose();
      this.neuralNetwork.material.dispose();
    }

    if (this.energyField) {
      this.energyField.geometry.dispose();
      this.energyField.material.dispose();
    }

    if (this.particleSystem) {
      this.particleSystem.geometry.dispose();
      this.particleSystem.material.dispose();
    }

    this.scene.remove(this.group);
  }
}
