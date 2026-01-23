/**
 * Android Bridge
 * Bidirectional communication between 3D scene and Android native code
 * Supports: lifecycle events, interactions, metrics, configuration
 */

export class AndroidBridge {
  constructor(scene) {
    this.scene = scene;
    this.isAndroidAvailable = typeof window.SAIHOSBridge !== 'undefined';

    // Event callbacks
    this.eventCallbacks = {};

    // Message queue for offline scenarios
    this.messageQueue = [];

    console.log(
      '[AndroidBridge] ' +
      (this.isAndroidAvailable ? 'Android bridge available' : 'Running in web mode')
    );
  }

  /**
   * Send message to Android
   */
  sendToAndroid(method, data) {
    const message = {
      method,
      data,
      timestamp: Date.now(),
    };

    if (this.isAndroidAvailable && window.SAIHOSBridge) {
      try {
        window.SAIHOSBridge.handleMessage(JSON.stringify(message));
      } catch (error) {
        console.error('[AndroidBridge] Error sending to Android:', error);
        this.messageQueue.push(message);
      }
    } else {
      // Queue for later when bridge becomes available
      this.messageQueue.push(message);
      console.log('[AndroidBridge] Message queued (offline):', method);
    }
  }

  /**
   * Handle message from Android
   */
  handleAndroidMessage(messageStr) {
    try {
      const message = JSON.parse(messageStr);
      console.log('[AndroidBridge] Received from Android:', message.method);

      const { method, data } = message;

      // Route to appropriate handler
      if (method === 'setTheme') {
        this.scene.setColorTheme(data.themeId);
      } else if (method === 'setAnimationIntensity') {
        this.scene.setAnimationIntensity(data.intensity);
      } else if (method === 'setAIMotionState') {
        // AI-DRIVEN ANIMATION: Receive AI state and drive procedural animations
        this.scene.setAIMotionState(data);
      } else if (method === 'getMetrics') {
        const metrics = this.scene.getMetrics();
        this.sendToAndroid('metricsUpdate', metrics);
      } else if (method === 'pause') {
        this.scene.pause();
      } else if (method === 'resume') {
        this.scene.resume();
      } else if (this.eventCallbacks[method]) {
        this.eventCallbacks[method](data);
      }
    } catch (error) {
      console.error('[AndroidBridge] Error handling Android message:', error);
    }
  }

  /**
   * Register event callback
   */
  on(event, callback) {
    this.eventCallbacks[event] = callback;
  }

  /**
   * Notify Android of scene initialization
   */
  notifyInitialized() {
    this.sendToAndroid('sceneInitialized', {
      timestamp: Date.now(),
      version: '1.0.0',
    });
  }

  /**
   * Notify Android of errors
   */
  notifyError(errorType, errorMessage) {
    this.sendToAndroid('error', {
      type: errorType,
      message: errorMessage,
      timestamp: Date.now(),
    });
  }

  /**
   * Notify Android of mouse movement
   */
  notifyMouseMove(x, y) {
    // Send at reduced frequency to avoid overhead
    if (Math.random() > 0.95) {
      this.sendToAndroid('mouseMoved', { x, y });
    }
  }

  /**
   * Notify Android of click
   */
  notifyClick() {
    this.sendToAndroid('clicked', {
      timestamp: Date.now(),
    });
  }

  /**
   * Notify Android of window resize
   */
  notifyResize(width, height) {
    this.sendToAndroid('resized', {
      width,
      height,
    });
  }

  /**
   * Notify Android of periodic metrics
   */
  notifyMetrics(metrics) {
    this.sendToAndroid('metrics', metrics);
  }

  /**
   * Receive AI motion state from Android and apply to 3D scene
   * Called by Kotlin AIStateBroadcaster
   */
  receiveAIMotionState(aiMotionStateJson) {
    // Pass to scene if available
    if (this.scene && this.scene.setAIMotionState) {
      this.scene.setAIMotionState(aiMotionStateJson);
    }
  }

  /**
   * Request theme from Android settings
   */
  requestTheme() {
    this.sendToAndroid('requestTheme', {});
  }

  /**
   * Request animation settings from Android
   */
  requestSettings() {
    this.sendToAndroid('requestSettings', {});
  }

  /**
   * Flush message queue (called when bridge becomes available)
   */
  flushQueue() {
    while (this.messageQueue.length > 0) {
      const message = this.messageQueue.shift();
      this.sendToAndroid(message.method, message.data);
    }
  }

  /**
   * Export scene to image (screenshot)
   */
  takeScreenshot() {
    if (this.scene.renderer) {
      this.scene.renderer.render(this.scene.scene, this.scene.camera);
      const imageData = this.scene.canvas.toDataURL('image/png');
      this.sendToAndroid('screenshot', { image: imageData });
    }
  }
}

// Make bridge available globally
window.SAIHOSBridge = window.SAIHOSBridge || {};
window.SAIHOSBridge.handleMessage = function(message) {
  if (window.SAIHOSSceneInstance) {
    window.SAIHOSSceneInstance.bridge.handleAndroidMessage(message);
  }
};
