/**
 * Utility Functions
 * Helper functions for animations, formatting, and general utilities
 */

const Utils = {
    /**
     * Generate random number between min and max
     */
    random(min, max) {
        return Math.random() * (max - min) + min;
    },

    /**
     * Format large numbers with suffixes
     */
    formatNumber(num) {
        if (num >= 1000000) {
            return (num / 1000000).toFixed(1) + 'M';
        } else if (num >= 1000) {
            return (num / 1000).toFixed(1) + 'K';
        }
        return num.toString();
    },

    /**
     * Generate random color
     */
    randomColor() {
        const colors = ['#00d9ff', '#ff006e', '#8f00ff', '#00ff88', '#ffaa00'];
        return colors[Math.floor(Math.random() * colors.length)];
    },

    /**
     * Convert hex color to RGB
     */
    hexToRgb(hex) {
        const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
        return result ? {
            r: parseInt(result[1], 16),
            g: parseInt(result[2], 16),
            b: parseInt(result[3], 16)
        } : null;
    },

    /**
     * Validate email
     */
    validateEmail(email) {
        const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return re.test(email);
    },

    /**
     * Validate password strength
     */
    validatePassword(password) {
        const strength = { weak: false, medium: false, strong: false, veryStrong: false };
        if (password.length < 8) return strength;
        strength.weak = true;
        if (/[A-Z]/.test(password) && /[0-9]/.test(password)) strength.medium = true;
        if (/[a-z]/.test(password) && /[A-Z]/.test(password) && /[0-9]/.test(password)) strength.strong = true;
        if (/[a-z]/.test(password) && /[A-Z]/.test(password) && /[0-9]/.test(password) && /[^a-zA-Z0-9]/.test(password)) strength.veryStrong = true;
        return strength;
    },

    /**
     * Format time from seconds
     */
    formatTime(seconds) {
        const hours = Math.floor(seconds / 3600);
        const minutes = Math.floor((seconds % 3600) / 60);
        const secs = Math.floor(seconds % 60);
        let result = '';
        if (hours > 0) result += hours + 'h ';
        if (minutes > 0) result += minutes + 'm ';
        if (secs > 0 || result === '') result += secs + 's';
        return result;
    },

    /**
     * Format date
     */
    formatDate(date) {
        const options = { year: 'numeric', month: 'short', day: 'numeric' };
        return new Date(date).toLocaleDateString('en-US', options);
    },

    /**
     * Format time of day
     */
    formatTimeOfDay(date) {
        const options = { hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false };
        return new Date(date).toLocaleTimeString('en-US', options);
    },

    /**
     * Check if element is in viewport
     */
    isInViewport(element) {
        const rect = element.getBoundingClientRect();
        return (
            rect.top >= 0 &&
            rect.left >= 0 &&
            rect.bottom <= (window.innerHeight || document.documentElement.clientHeight) &&
            rect.right <= (window.innerWidth || document.documentElement.clientWidth)
        );
    },

    /**
     * Debounce function calls
     */
    debounce(func, delay) {
        let timeoutId;
        return function (...args) {
            clearTimeout(timeoutId);
            timeoutId = setTimeout(() => func.apply(this, args), delay);
        };
    },

    /**
     * Throttle function calls
     */
    throttle(func, limit) {
        let inThrottle;
        return function (...args) {
            if (!inThrottle) {
                func.apply(this, args);
                inThrottle = true;
                setTimeout(() => inThrottle = false, limit);
            }
        };
    },

    /**
     * Deep clone object
     */
    deepClone(obj) {
        if (obj === null || typeof obj !== 'object') return obj;
        if (obj instanceof Date) return new Date(obj.getTime());
        if (obj instanceof Array) return obj.map(item => Utils.deepClone(item));
        const cloned = {};
        for (const key in obj) {
            if (obj.hasOwnProperty(key)) {
                cloned[key] = Utils.deepClone(obj[key]);
            }
        }
        return cloned;
    },

    /**
     * Merge objects
     */
    merge(target, source) {
        for (const key in source) {
            if (source.hasOwnProperty(key)) {
                if (typeof source[key] === 'object' && source[key] !== null) {
                    target[key] = Utils.merge(target[key] || {}, source[key]);
                } else {
                    target[key] = source[key];
                }
            }
        }
        return target;
    },

    /**
     * Wait for condition to be true
     */
    async waitFor(condition, timeout = 5000) {
        const startTime = Date.now();
        while (!condition()) {
            if (Date.now() - startTime > timeout) {
                throw new Error('Timeout waiting for condition');
            }
            await new Promise(resolve => setTimeout(resolve, 50));
        }
    },

    /**
     * Get device type
     */
    getDeviceType() {
        const ua = navigator.userAgent;
        if (/mobile/i.test(ua)) return 'mobile';
        if (/tablet/i.test(ua)) return 'tablet';
        return 'desktop';
    },

    /**
     * Check if device supports touch
     */
    isTouchDevice() {
        return (('ontouchstart' in window) ||
                (navigator.maxTouchPoints > 0) ||
                (navigator.msMaxTouchPoints > 0));
    },

    /**
     * Local storage helpers
     */
    storage: {
        set(key, value) {
            try {
                localStorage.setItem(key, JSON.stringify(value));
                return true;
            } catch (e) {
                console.error('Storage error:', e);
                return false;
            }
        },
        get(key, defaultValue = null) {
            try {
                const value = localStorage.getItem(key);
                return value ? JSON.parse(value) : defaultValue;
            } catch (e) {
                console.error('Storage error:', e);
                return defaultValue;
            }
        },
        remove(key) {
            try {
                localStorage.removeItem(key);
                return true;
            } catch (e) {
                console.error('Storage error:', e);
                return false;
            }
        },
        clear() {
            try {
                localStorage.clear();
                return true;
            } catch (e) {
                console.error('Storage error:', e);
                return false;
            }
        }
    },

    /**
     * Session storage helpers
     */
    session: {
        set(key, value) {
            try {
                sessionStorage.setItem(key, JSON.stringify(value));
                return true;
            } catch (e) {
                console.error('Session error:', e);
                return false;
            }
        },
        get(key, defaultValue = null) {
            try {
                const value = sessionStorage.getItem(key);
                return value ? JSON.parse(value) : defaultValue;
            } catch (e) {
                console.error('Session error:', e);
                return defaultValue;
            }
        },
        remove(key) {
            try {
                sessionStorage.removeItem(key);
                return true;
            } catch (e) {
                console.error('Session error:', e);
                return false;
            }
        }
    },

    /**
     * Logger utility
     */
    log: {
        info(message, data = null) {
            console.log(`%c[INFO] ${message}`, 'color: #00d9ff; font-weight: bold;', data || '');
        },
        warn(message, data = null) {
            console.warn(`%c[WARN] ${message}`, 'color: #ffaa00; font-weight: bold;', data || '');
        },
        error(message, data = null) {
            console.error(`%c[ERROR] ${message}`, 'color: #ff0055; font-weight: bold;', data || '');
        },
        success(message, data = null) {
            console.log(`%c[SUCCESS] ${message}`, 'color: #00ff88; font-weight: bold;', data || '');
        }
    }
};

// Export
window.Utils = Utils;
