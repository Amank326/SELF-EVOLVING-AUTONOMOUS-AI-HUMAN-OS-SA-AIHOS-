/**
 * Gesture Recognizer Bridge
 * Translates Android touch gestures into AI state changes
 * Provides haptic feedback hints for mobile interactions
 */

export class GestureRecognizerBridge {
  constructor(bridge) {
    this.bridge = bridge;
    this.lastTouchTime = 0;
    this.touchStartX = 0;
    this.touchStartY = 0;
    this.touchStartTime = 0;
    this.isLongPress = false;
    this.longPressTimer = null;

    this.SWIPE_THRESHOLD = 50;
    this.LONG_PRESS_DURATION = 500;
  }

  /**
   * Detect and classify touch gesture
   * Returns: { type: string, intensity: number }
   */
  recognizeGesture(x, y, eventType) {
    const now = Date.now();

    if (eventType === 'touchstart') {
      this.touchStartX = x;
      this.touchStartY = y;
      this.touchStartTime = now;
      this.isLongPress = false;

      // Setup long press detection
      this.longPressTimer = setTimeout(() => {
        this.isLongPress = true;
        this._sendGesture('hold', 0.7);
      }, this.LONG_PRESS_DURATION);

      return { type: 'start', intensity: 0 };
    }

    if (eventType === 'touchmove') {
      if (this.longPressTimer && !this.isLongPress) {
        clearTimeout(this.longPressTimer);
      }

      const dx = x - this.touchStartX;
      const dy = y - this.touchStartY;
      const distance = Math.sqrt(dx * dx + dy * dy);

      if (distance > this.SWIPE_THRESHOLD) {
        // Swipe detected
        const angle = Math.atan2(dy, dx);
        const intensity = Math.min(distance / 200, 1.0);

        return {
          type: 'swipe',
          intensity: intensity,
          direction: angle,
        };
      }

      return { type: 'move', intensity: distance / 100 };
    }

    if (eventType === 'touchend') {
      clearTimeout(this.longPressTimer);

      const duration = now - this.touchStartTime;
      const dx = x - this.touchStartX;
      const dy = y - this.touchStartY;
      const distance = Math.sqrt(dx * dx + dy * dy);

      if (!this.isLongPress && duration < 200 && distance < 10) {
        // Quick tap
        return { type: 'tap', intensity: 1.0 };
      }

      return { type: 'release', intensity: 0 };
    }

    return null;
  }

  /**
   * Send recognized gesture to Android bridge
   */
  _sendGesture(type, intensity) {
    this.bridge.sendToAndroid('gesture', {
      type: type,
      intensity: intensity,
      timestamp: Date.now(),
    });

    console.log(`[GestureRecognizer] ${type} gesture (${intensity.toFixed(2)})`);
  }

  /**
   * Multitouch support (pinch, rotate)
   */
  recognizeMultitouch(touches) {
    if (touches.length === 2) {
      const t1 = touches[0];
      const t2 = touches[1];

      const dx = t2.clientX - t1.clientX;
      const dy = t2.clientY - t1.clientY;
      const distance = Math.sqrt(dx * dx + dy * dy);

      // Compare with previous distance for pinch detection
      if (!this.lastMultitouchDistance) {
        this.lastMultitouchDistance = distance;
        return { type: 'multitouch', intensity: 0 };
      }

      const delta = distance - this.lastMultitouchDistance;
      this.lastMultitouchDistance = distance;

      if (Math.abs(delta) > 5) {
        const intensity = delta / 100; // normalized
        return { type: 'pinch', intensity: intensity };
      }
    } else {
      this.lastMultitouchDistance = null;
    }

    return null;
  }
}
