/**
 * Easing Functions
 * High-quality easing functions for smooth, natural-feeling animations
 * 
 * Physics-based and mathematical easings for:
 * - Motion smoothing
 * - Transition effects
 * - Gesture responses
 * 
 * All functions take t (0-1) and return output (0-1)
 * Unless otherwise specified
 */
export class EasingFunctions {
  // Linear - no easing
  static linear(t) {
    return t;
  }

  // ===== Quad =====
  static easeInQuad(t) {
    return t * t;
  }

  static easeOutQuad(t) {
    return t * (2 - t);
  }

  static easeInOutQuad(t) {
    return t < 0.5 ? 2 * t * t : -1 + (4 - 2 * t) * t;
  }

  // ===== Cubic (recommended for most UI) =====
  static easeInCubic(t) {
    return t * t * t;
  }

  static easeOutCubic(t) {
    const t1 = t - 1;
    return t1 * t1 * t1 + 1;
  }

  static easeInOutCubic(t) {
    return t < 0.5
      ? 4 * t * t * t
      : (t - 1) * (2 * (t - 2)) * (2 * (t - 2)) + 1;
  }

  // ===== Quart =====
  static easeInQuart(t) {
    return t * t * t * t;
  }

  static easeOutQuart(t) {
    const t1 = t - 1;
    return 1 - t1 * t1 * t1 * t1;
  }

  static easeInOutQuart(t) {
    const t1 = t - 1;
    return t < 0.5
      ? 8 * t * t * t * t
      : 1 - 8 * t1 * t1 * t1 * t1;
  }

  // ===== Quint =====
  static easeInQuint(t) {
    return t * t * t * t * t;
  }

  static easeOutQuint(t) {
    const t1 = t - 1;
    return 1 + t1 * t1 * t1 * t1 * t1;
  }

  static easeInOutQuint(t) {
    const t1 = t - 1;
    return t < 0.5
      ? 16 * t * t * t * t * t
      : 1 + 16 * t1 * t1 * t1 * t1 * t1;
  }

  // ===== Sine (smooth and subtle) =====
  static easeInSine(t) {
    return 1 - Math.cos((t * Math.PI) / 2);
  }

  static easeOutSine(t) {
    return Math.sin((t * Math.PI) / 2);
  }

  static easeInOutSine(t) {
    return -(Math.cos(Math.PI * t) - 1) / 2;
  }

  // ===== Expo (dramatic acceleration) =====
  static easeInExpo(t) {
    return t === 0 ? 0 : Math.pow(2, 10 * t - 10);
  }

  static easeOutExpo(t) {
    return t === 1 ? 1 : 1 - Math.pow(2, -10 * t);
  }

  static easeInOutExpo(t) {
    return t === 0
      ? 0
      : t === 1
        ? 1
        : t < 0.5
          ? Math.pow(2, 20 * t - 10) / 2
          : (2 - Math.pow(2, -20 * t + 10)) / 2;
  }

  // ===== Circ =====
  static easeInCirc(t) {
    return 1 - Math.sqrt(1 - Math.pow(t, 2));
  }

  static easeOutCirc(t) {
    return Math.sqrt(1 - Math.pow(t - 1, 2));
  }

  static easeInOutCirc(t) {
    return t < 0.5
      ? (1 - Math.sqrt(1 - Math.pow(2 * t, 2))) / 2
      : (Math.sqrt(1 - Math.pow(-2 * t + 2, 2)) + 1) / 2;
  }

  // ===== Elastic (springy) =====
  static easeInElastic(t) {
    const c4 = (2 * Math.PI) / 3;
    return t === 0
      ? 0
      : t === 1
        ? 1
        : -Math.pow(2, 10 * t - 10) * Math.sin((t * 10 - 10.75) * c4);
  }

  static easeOutElastic(t) {
    const c4 = (2 * Math.PI) / 3;
    return t === 0
      ? 0
      : t === 1
        ? 1
        : Math.pow(2, -10 * t) * Math.sin((t * 10 - 0.75) * c4) + 1;
  }

  static easeInOutElastic(t) {
    const c5 = (2 * Math.PI) / 4.5;
    return t === 0
      ? 0
      : t === 1
        ? 1
        : t < 0.5
          ? -(Math.pow(2, 20 * t - 10) * Math.sin((20 * t - 11.125) * c5)) / 2
          : (Math.pow(2, -20 * t + 10) * Math.sin((20 * t - 11.125) * c5)) / 2 + 1;
  }

  // ===== Back (anticipation effect) =====
  static easeInBack(t) {
    const c1 = 1.70158;
    const c3 = c1 + 1;
    return c3 * t * t * t - c1 * t * t;
  }

  static easeOutBack(t) {
    const c1 = 1.70158;
    const c3 = c1 + 1;
    return 1 + c3 * Math.pow(t - 1, 3) + c1 * Math.pow(t - 1, 2);
  }

  static easeInOutBack(t) {
    const c1 = 1.70158;
    const c2 = c1 * 1.525;
    return t < 0.5
      ? (Math.pow(2 * t, 2) * ((c2 + 1) * 2 * t - c2)) / 2
      : (Math.pow(2 * t - 2, 2) * ((c2 + 1) * (t * 2 - 2) + c2) + 2) / 2;
  }

  // ===== Bounce (elasticity) =====
  static easeOutBounce(t) {
    const n1 = 7.5625;
    const d1 = 2.75;

    if (t < 1 / d1) {
      return n1 * t * t;
    } else if (t < 2 / d1) {
      return n1 * (t -= 1.5 / d1) * t + 0.75;
    } else if (t < 2.5 / d1) {
      return n1 * (t -= 2.25 / d1) * t + 0.9375;
    } else {
      return n1 * (t -= 2.625 / d1) * t + 0.984375;
    }
  }

  static easeInBounce(t) {
    return 1 - EasingFunctions.easeOutBounce(1 - t);
  }

  static easeInOutBounce(t) {
    return t < 0.5
      ? (1 - EasingFunctions.easeOutBounce(1 - 2 * t)) / 2
      : (1 + EasingFunctions.easeOutBounce(2 * t - 1)) / 2;
  }

  // ===== Premium custom functions =====
  
  /**
   * Smooth step - very smooth transition
   */
  static smoothstep(t) {
    return t * t * (3 - 2 * t);
  }

  /**
   * Smoother step - even smoother
   */
  static smootherstep(t) {
    return t * t * t * (t * (t * 6 - 15) + 10);
  }

  /**
   * Custom curve for gesture response
   * Quick initial response, smooth finish
   */
  static gestureResponse(t) {
    // Combines expo-in (quick start) with cubic (smooth finish)
    if (t < 0.3) {
      return EasingFunctions.easeOutExpo(t / 0.3) * 0.3;
    } else {
      return 0.3 + EasingFunctions.easeInOutCubic((t - 0.3) / 0.7) * 0.7;
    }
  }

  /**
   * Custom curve for reflection mode
   * Slow, contemplative ease
   */
  static reflectionCurve(t) {
    // Very smooth, slow-in, slow-out
    return EasingFunctions.easeInOutSine(t);
  }

  /**
   * Custom curve for impact effect
   * Sharp entrance, decay
   */
  static impactCurve(t) {
    // Quick start, exponential decay
    if (t < 0.2) {
      return 1;  // Full intensity
    }
    return Math.exp(-5 * (t - 0.2));  // Exponential decay
  }

  /**
   * Bezier curve (custom cubic bezier)
   * parameters: p1x, p1y, p2x, p2y (control points)
   * Returns function that takes t and returns eased value
   */
  static bezier(p1x, p1y, p2x, p2y) {
    return function(t) {
      // Simplified cubic bezier approximation
      const mt = 1 - t;
      const mt2 = mt * mt;
      const mt3 = mt2 * mt;
      const t2 = t * t;
      const t3 = t2 * t;

      const y = mt3 * 0 + 3 * mt2 * t * p1y + 3 * mt * t2 * p2y + t3 * 1;
      return Math.max(0, Math.min(1, y));  // Clamp to 0-1
    };
  }

  /**
   * Get easing function by name
   */
  static getEasing(name) {
    const easings = {
      linear: this.linear,
      easeInQuad: this.easeInQuad,
      easeOutQuad: this.easeOutQuad,
      easeInOutQuad: this.easeInOutQuad,
      easeInCubic: this.easeInCubic,
      easeOutCubic: this.easeOutCubic,
      easeInOutCubic: this.easeInOutCubic,
      easeInSine: this.easeInSine,
      easeOutSine: this.easeOutSine,
      easeInOutSine: this.easeInOutSine,
      easeOutElastic: this.easeOutElastic,
      smoothstep: this.smoothstep,
      smootherstep: this.smootherstep,
      gestureResponse: this.gestureResponse,
      reflectionCurve: this.reflectionCurve,
      impactCurve: this.impactCurve,
    };

    return easings[name] || this.linear;
  }

  /**
   * List all available easing names
   */
  static getAvailableEasings() {
    return [
      'linear',
      'easeInQuad',
      'easeOutQuad',
      'easeInOutQuad',
      'easeInCubic',
      'easeOutCubic',
      'easeInOutCubic',
      'easeInSine',
      'easeOutSine',
      'easeInOutSine',
      'easeInExpo',
      'easeOutExpo',
      'easeInOutExpo',
      'easeInCirc',
      'easeOutCirc',
      'easeInOutCirc',
      'easeOutElastic',
      'easeOutBounce',
      'easeInBounce',
      'easeInOutBounce',
      'smoothstep',
      'smootherstep',
      'gestureResponse',
      'reflectionCurve',
      'impactCurve',
    ];
  }
}
