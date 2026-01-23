/**
 * Performance Monitor
 * Real-time performance tracking and metrics
 * 
 * Monitors:
 * - Frame time and FPS
 * - Memory usage
 * - GPU utilization (if available)
 * - Draw calls and geometry
 * - Network/bridge communication
 * 
 * Features:
 * - Real-time metric logging
 * - Performance alerts
 * - Bottleneck detection
 * - Debug overlay support
 */
export class PerformanceMonitor {
  constructor(renderer) {
    this.renderer = renderer;
    this.enabled = true;

    // Timing metrics
    this.frameMetrics = {
      count: 0,
      totalTime: 0,
      avgFrameTime: 0,
      minFrameTime: Infinity,
      maxFrameTime: 0,
      fps: 60,
    };

    // Memory metrics
    this.memoryMetrics = {
      heapUsed: 0,
      heapLimit: 0,
      heapRatio: 0,
      lastUpdate: 0,
    };

    // Renderer metrics
    this.rendererMetrics = {
      drawCalls: 0,
      vertices: 0,
      triangles: 0,
      textures: 0,
      programs: 0,
    };

    // Frame time history for graph
    this.frameTimeHistory = [];
    this.maxHistorySize = 120;  // Keep 2 seconds at 60 FPS

    // Sample intervals
    this.memoryUpdateInterval = 1000;  // Update every 1 second
    this.lastMemoryUpdate = Date.now();

    // Alert thresholds
    this.alerts = {
      fpsCritical: 25,
      fpsWarn: 40,
      memoryWarn: 0.85,  // 85% of heap
    };

    console.log('[PerformanceMonitor] Initialized');
  }

  /**
   * Record frame time
   * Call once per frame
   */
  recordFrame(deltaTime) {
    if (!this.enabled) return;

    // Track frame metrics
    this.frameMetrics.count++;
    this.frameMetrics.totalTime += deltaTime;
    this.frameMetrics.avgFrameTime = this.frameMetrics.totalTime / this.frameMetrics.count;
    this.frameMetrics.minFrameTime = Math.min(this.frameMetrics.minFrameTime, deltaTime);
    this.frameMetrics.maxFrameTime = Math.max(this.frameMetrics.maxFrameTime, deltaTime);
    
    if (deltaTime > 0) {
      this.frameMetrics.fps = 1 / deltaTime;
    }

    // Record frame time history
    this.frameTimeHistory.push(deltaTime * 1000);  // Convert to ms
    if (this.frameTimeHistory.length > this.maxHistorySize) {
      this.frameTimeHistory.shift();
    }

    // Check for alerts
    this._checkAlerts();

    // Update memory periodically
    const now = Date.now();
    if (now - this.lastMemoryUpdate > this.memoryUpdateInterval) {
      this._updateMemoryMetrics();
      this._updateRendererMetrics();
      this.lastMemoryUpdate = now;
    }
  }

  /**
   * Update memory metrics
   */
  _updateMemoryMetrics() {
    if (performance && performance.memory) {
      const memory = performance.memory;
      this.memoryMetrics.heapUsed = memory.usedJSHeapSize;
      this.memoryMetrics.heapLimit = memory.jsHeapSizeLimit;
      this.memoryMetrics.heapRatio = memory.usedJSHeapSize / memory.jsHeapSizeLimit;
    }
  }

  /**
   * Update renderer metrics
   */
  _updateRendererMetrics() {
    if (this.renderer.info) {
      const info = this.renderer.info.render;
      this.rendererMetrics.drawCalls = info.calls || 0;
      this.rendererMetrics.triangles = info.triangles || 0;
      this.rendererMetrics.vertices = info.vertices || 0;
      
      if (this.renderer.info.programs) {
        this.rendererMetrics.programs = this.renderer.info.programs.length;
      }
    }

    // Count textures in scene (approximate)
    this.rendererMetrics.textures = this._countTextures();
  }

  /**
   * Count textures in use
   */
  _countTextures() {
    let count = 0;
    if (this.renderer.properties && this.renderer.properties.__webglTextures) {
      count = Object.keys(this.renderer.properties.__webglTextures).length;
    }
    return count;
  }

  /**
   * Check for performance alerts
   */
  _checkAlerts() {
    const fps = this.frameMetrics.fps;
    const memRatio = this.memoryMetrics.heapRatio;

    // FPS critical
    if (fps < this.alerts.fpsCritical) {
      console.warn(`[PerformanceMonitor] CRITICAL: FPS at ${fps.toFixed(1)}`);
    }
    // FPS warning
    else if (fps < this.alerts.fpsWarn) {
      console.warn(`[PerformanceMonitor] WARNING: FPS at ${fps.toFixed(1)}`);
    }

    // Memory warning
    if (memRatio > this.alerts.memoryWarn) {
      console.warn(`[PerformanceMonitor] Memory usage: ${(memRatio * 100).toFixed(1)}%`);
    }
  }

  /**
   * Get current metrics
   */
  getMetrics() {
    return {
      frame: {
        count: this.frameMetrics.count,
        fps: this.frameMetrics.fps.toFixed(1),
        avgFrameTime: this.frameMetrics.avgFrameTime.toFixed(3) + ' s',
        minFrameTime: this.frameMetrics.minFrameTime.toFixed(3) + ' s',
        maxFrameTime: this.frameMetrics.maxFrameTime.toFixed(3) + ' s',
      },
      memory: {
        heapUsed: (this.memoryMetrics.heapUsed / 1048576).toFixed(2) + ' MB',
        heapLimit: (this.memoryMetrics.heapLimit / 1048576).toFixed(2) + ' MB',
        heapRatio: (this.memoryMetrics.heapRatio * 100).toFixed(1) + '%',
      },
      renderer: this.rendererMetrics,
      frameTimeHistory: this.frameTimeHistory.slice(-60),  // Last 1 second at 60 FPS
    };
  }

  /**
   * Get performance summary
   */
  getSummary() {
    const metrics = this.getMetrics();
    return `
FPS: ${metrics.frame.fps} | 
Frame: ${(this.frameMetrics.avgFrameTime * 1000).toFixed(1)}ms | 
Memory: ${metrics.memory.heapUsed}/${metrics.memory.heapLimit} | 
Geometry: ${metrics.renderer.triangles.toLocaleString()} triangles | 
Draw calls: ${metrics.renderer.drawCalls}
    `.trim();
  }

  /**
   * Reset metrics
   */
  reset() {
    this.frameMetrics = {
      count: 0,
      totalTime: 0,
      avgFrameTime: 0,
      minFrameTime: Infinity,
      maxFrameTime: 0,
      fps: 60,
    };
    this.frameTimeHistory = [];
  }

  /**
   * Enable/disable monitoring
   */
  setEnabled(enabled) {
    this.enabled = enabled;
    if (enabled) {
      this.reset();
    }
  }

  /**
   * Create debug overlay string
   * Use this in a debug UI overlay
   */
  getDebugOverlayText() {
    const metrics = this.getMetrics();
    const fpsColor = this.frameMetrics.fps < 40 ? '#ff6b6b' : '#51cf66';
    
    return `
<div style="font-family: monospace; font-size: 11px; padding: 8px; background: rgba(0,0,0,0.7); color: #fff; border-radius: 4px;">
  <div style="color: ${fpsColor};">FPS: ${metrics.frame.fps}</div>
  <div>Frame: ${(this.frameMetrics.avgFrameTime * 1000).toFixed(1)}ms</div>
  <div>Memory: ${metrics.memory.heapRatio}</div>
  <div>Draws: ${metrics.renderer.drawCalls}</div>
  <div>Geo: ${metrics.renderer.triangles}</div>
</div>
    `;
  }

  /**
   * Export metrics for analysis
   */
  exportMetrics(name = 'performance-metrics') {
    const data = {
      timestamp: new Date().toISOString(),
      duration: this.frameMetrics.avgFrameTime * this.frameMetrics.count,
      metrics: this.getMetrics(),
      summary: this.getSummary(),
      frameTimeHistory: this.frameTimeHistory,
    };

    // Create downloadable JSON
    const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${name}-${Date.now()}.json`;
    a.click();
    URL.revokeObjectURL(url);

    console.log('[PerformanceMonitor] Metrics exported:', data);
  }
}
