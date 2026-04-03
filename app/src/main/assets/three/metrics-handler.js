/**
 * SA-AIHOS 3D Scene Metrics Handler
 * Handles real-time metric updates from Android ViewModel
 * Manages animations and visual effects based on AI engine metrics
 */

// Global scene state object
const sceneState = {
    metricsReady: false,
    autonomyLevel: 0.5,
    memoryUsage: 0.5,
    reasoningConfidence: 0.5,
    evolutionProgress: 0.5,
    systemHealth: 0.5,
    cycleCount: 0,
    fps: 60,
    totalFrames: 0,
    lastFrameTime: Date.now()
};

// Scene objects references (initialized by main Three.js scene)
let crystalCore = null;
let particleSystem = null;
let ambientLight = null;
let pointLight = null;

/**
 * Main handler for metrics updates from Android
 * Called by SceneMetricsHandler.sendToScene()
 */
function handleMetricsUpdate(json) {
    if (!sceneState.metricsReady) {
        console.log('Scene not ready, queuing update');
        return;
    }
    
    const type = json.type;
    
    switch(type) {
        case 'batchUpdate':
            handleBatchUpdate(json);
            break;
        case 'updateAutonomy':
            handleAutonomyUpdate(json);
            break;
        case 'updateMemory':
            handleMemoryUpdate(json);
            break;
        case 'updateReasoning':
            handleReasoningUpdate(json);
            break;
        case 'updateEvolution':
            handleEvolutionUpdate(json);
            break;
        case 'updateHealth':
            handleHealthUpdate(json);
            break;
        case 'updateCycles':
            handleCycleUpdate(json);
            break;
        case 'triggerEvent':
            handleEventAnimation(json);
            break;
        case 'requestFrame':
            handleFrameRequest(json);
            break;
        default:
            console.warn('Unknown update type:', type);
    }
}

/**
 * Batch update - most efficient for multiple metrics
 */
function handleBatchUpdate(json) {
    sceneState.autonomyLevel = json.autonomy;
    sceneState.memoryUsage = json.memory;
    sceneState.reasoningConfidence = json.reasoning;
    sceneState.evolutionProgress = json.evolution;
    sceneState.systemHealth = json.health;
    
    // Apply all animations
    updateCrystalCore(json.autonomy);
    updateParticleSystem(json.memory);
    updateSceneColors(json.reasoning);
    updateEvolutionVisualization(json.evolution);
    updateSystemLighting(json.health);
}

/**
 * Update crystal core animation
 * - Rotation speed based on autonomy level
 * - Glow intensity variation
 * - Pulse effects
 */
function handleAutonomyUpdate(json) {
    const level = json.level;
    sceneState.autonomyLevel = level;
    updateCrystalCore(level);
}

function updateCrystalCore(autonomyLevel) {
    if (!crystalCore) return;
    
    // Rotation animation
    const rotationSpeed = json.rotationSpeed || (0.5 + autonomyLevel * 2);
    crystalCore.rotation.x += rotationSpeed * 0.01;
    crystalCore.rotation.y += rotationSpeed * 0.015;
    crystalCore.rotation.z += rotationSpeed * 0.008;
    
    // Core intensity (glow)
    const coreIntensity = json.coreIntensity || (0.3 + autonomyLevel * 0.7);
    if (crystalCore.material && crystalCore.material.emissive) {
        const emissiveIntensity = Math.sin(Date.now() * 0.002) * 0.5 + 0.5;
        crystalCore.material.emissive.setHSL(
            240 / 360,  // Blue/purple hue
            1.0,
            coreIntensity * emissiveIntensity
        );
    }
    
    // Scale pulse
    const pulseFreq = json.pulseFrequency || (1 + autonomyLevel * 3);
    const pulseFactor = 1 + Math.sin(Date.now() * (pulseFreq / 1000)) * 0.1;
    crystalCore.scale.set(pulseFactor, pulseFactor, pulseFactor);
}

/**
 * Update particle effects based on memory usage
 * Higher memory = denser particles
 */
function handleMemoryUpdate(json) {
    sceneState.memoryUsage = json.usage;
    updateParticleSystem(json.usage);
}

function updateParticleSystem(memoryUsage) {
    if (!particleSystem) return;
    
    const particleCount = json.particleCount || (1000 + memoryUsage * 4000);
    const particleSpeed = json.particleSpeed || (0.5 + memoryUsage * 1.5);
    const cloudDensity = json.cloudDensity || (0.2 + memoryUsage * 0.8);
    
    // Update particle positions
    const positions = particleSystem.geometry.attributes.position.array;
    for (let i = 0; i < Math.min(positions.length / 3, particleCount); i++) {
        const idx = i * 3;
        positions[idx] += (Math.random() - 0.5) * particleSpeed * 0.1;
        positions[idx + 1] += (Math.random() - 0.5) * particleSpeed * 0.1;
        positions[idx + 2] += (Math.random() - 0.5) * particleSpeed * 0.1;
    }
    particleSystem.geometry.attributes.position.needsUpdate = true;
    
    // Update opacity based on memory
    if (particleSystem.material) {
        particleSystem.material.opacity = cloudDensity;
    }
}

/**
 * Update color shifts based on reasoning confidence
 * Red → Yellow → Green gradient
 */
function handleReasoningUpdate(json) {
    sceneState.reasoningConfidence = json.confidence;
    updateSceneColors(json.confidence);
}

function updateSceneColors(reasoningConfidence) {
    // Generate color gradient
    const hue = reasoningConfidence * 120 / 360;  // 0° (red) to 120° (green)
    const saturation = 0.6 + reasoningConfidence * 0.4;
    const lightness = 0.4 + reasoningConfidence * 0.3;
    
    // Apply to main point light
    if (pointLight && pointLight.color) {
        pointLight.color.setHSL(hue, saturation, lightness);
        pointLight.intensity = 0.5 + reasoningConfidence * 0.5;
    }
    
    // Glow effect on crystal
    const glowIntensity = json.glowIntensity || (0.3 + reasoningConfidence * 0.7);
    if (crystalCore && crystalCore.children && crystalCore.children[0]) {
        const glowLayer = crystalCore.children[0];
        if (glowLayer.material) {
            glowLayer.material.emissive.setHSL(hue, saturation, glowIntensity);
        }
    }
}

/**
 * Update evolution visualization
 * Growth of secondary structures
 */
function handleEvolutionUpdate(json) {
    sceneState.evolutionProgress = json.progress;
    updateEvolutionVisualization(json.progress);
}

function updateEvolutionVisualization(evolutionProgress) {
    const structureSize = json.structureSize || (0.5 + evolutionProgress * 1.5);
    const bloomIntensity = json.bloomIntensity || (evolutionProgress * 0.6);
    
    // Scale evolution structures
    // (Assumes evolutionary geometry is in a group called 'evolutionGroup')
    if (window.scene && window.scene.getObjectByName('evolutionGroup')) {
        const evolutionGroup = window.scene.getObjectByName('evolutionGroup');
        evolutionGroup.scale.set(structureSize, structureSize, structureSize);
    }
    
    // Apply post-processing bloom
    if (window.bloomPass) {
        window.bloomPass.strength = bloomIntensity;
    }
}

/**
 * Update system health visualization
 * Ambient lighting and color based on health
 */
function handleHealthUpdate(json) {
    sceneState.systemHealth = json.health;
    updateSystemLighting(json.health);
}

function updateSystemLighting(systemHealth) {
    const ambientIntensity = json.ambientIntensity || (0.3 + systemHealth * 0.5);
    const vibrationIntensity = json.vibrationIntensity || ((1 - systemHealth) * 0.5);
    
    // Update ambient light
    if (ambientLight) {
        ambientLight.intensity = ambientIntensity;
        
        // Color based on health
        let hue = systemHealth > 0.7 ? 120 / 360 :  // Green
                 systemHealth > 0.5 ? 60 / 360 :    // Yellow
                 0;                                  // Red
        ambientLight.color.setHSL(hue, 0.6, 0.5);
    }
    
    // Camera vibration effect for low health
    if (window.camera && vibrationIntensity > 0) {
        window.camera.position.x += (Math.random() - 0.5) * vibrationIntensity * 0.1;
        window.camera.position.y += (Math.random() - 0.5) * vibrationIntensity * 0.1;
    }
}

/**
 * Update cycle counter display
 */
function handleCycleUpdate(json) {
    sceneState.cycleCount = json.count;
    const pulseFreq = json.pulseFrequency || 1.0;
    
    // Update HUD element if exists
    if (document.getElementById('cycleCounter')) {
        document.getElementById('cycleCounter').textContent = json.count;
    }
    
    // Pulse effect
    if (window.cycleIndicator) {
        const pulseFactor = 1 + Math.sin(Date.now() * (pulseFreq / 1000)) * 0.1;
        window.cycleIndicator.scale.set(pulseFactor, pulseFactor, pulseFactor);
    }
}

/**
 * Trigger special animations for events
 */
function handleEventAnimation(json) {
    const eventType = json.eventType;
    const intensity = json.intensity || 1.0;
    
    console.log(`Event animation triggered: ${eventType} (intensity: ${intensity})`);
    
    switch(eventType) {
        case 'autonomy_goal_executed':
            triggerGoalExecutionAnimation(intensity);
            break;
        case 'memory_consolidation_complete':
            triggerMemoryConsolidationAnimation(intensity);
            break;
        case 'reasoning_cycle_complete':
            triggerReasoningCycleAnimation(intensity);
            break;
        case 'evolution_cycle_complete':
            triggerEvolutionCycleAnimation(intensity);
            break;
        case 'system_health_degraded':
            triggerWarningAnimation(intensity);
            break;
        case 'critical_alert':
            triggerCriticalAnimation(intensity);
            break;
        default:
            triggerGenericEventAnimation(intensity);
    }
}

function triggerGoalExecutionAnimation(intensity) {
    // Bright burst from crystal core
    if (crystalCore && crystalCore.material) {
        crystalCore.material.emissive.setHSL(60 / 360, 1.0, intensity);
    }
}

function triggerMemoryConsolidationAnimation(intensity) {
    // Particle acceleration and reorganization
    if (particleSystem) {
        // Fade in/out effect
        particleSystem.material.opacity = intensity;
    }
}

function triggerReasoningCycleAnimation(intensity) {
    // Color pulse through reasoning color
    if (pointLight) {
        pointLight.intensity = 1.0 * intensity;
    }
}

function triggerEvolutionCycleAnimation(intensity) {
    // Growth burst
    if (window.scene && window.scene.getObjectByName('evolutionGroup')) {
        const group = window.scene.getObjectByName('evolutionGroup');
        const originalScale = 1.0;
        group.scale.set(
            originalScale * (1 + intensity * 0.2),
            originalScale * (1 + intensity * 0.2),
            originalScale * (1 + intensity * 0.2)
        );
    }
}

function triggerWarningAnimation(intensity) {
    // Red tint warning
    if (ambientLight) {
        ambientLight.color.setHSL(0, 1.0, 0.5);
    }
}

function triggerCriticalAnimation(intensity) {
    // Rapid flashing
    if (ambientLight) {
        ambientLight.intensity = intensity;
    }
}

function triggerGenericEventAnimation(intensity) {
    // General glow effect
    if (crystalCore && crystalCore.material) {
        crystalCore.material.emissive.setHSL(
            240 / 360,
            1.0,
            intensity * 0.5
        );
    }
}

/**
 * Handle frame request from metrics handler
 */
function handleFrameRequest(json) {
    sceneState.totalFrames++;
    
    // Calculate FPS
    const now = Date.now();
    if (now - sceneState.lastFrameTime >= 1000) {
        sceneState.fps = sceneState.totalFrames;
        sceneState.totalFrames = 0;
        sceneState.lastFrameTime = now;
        
        console.log(`FPS: ${sceneState.fps}`);
    }
}

/**
 * Export handlers for Three.js scene
 */
window.handleMetricsUpdate = handleMetricsUpdate;
window.sceneState = sceneState;

console.log('SA-AIHOS Metrics Handler loaded');
