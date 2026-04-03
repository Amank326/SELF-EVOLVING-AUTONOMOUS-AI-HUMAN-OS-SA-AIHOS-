/**
 * Main Application Orchestration
 * Coordinates all components and manages the overall app flow
 */

class SAIHOSApp {
    constructor() {
        this.animationController = null;
        this.loginManager = null;
        this.dashboardManager = null;
        this.threeJSScene = null;
        this.isInitialized = false;

        // Backend integration
        this.apiURL = 'http://localhost:5000/api';
        this.token = localStorage.getItem('sa_aihos_token');
        this.user = JSON.parse(localStorage.getItem('sa_aihos_user') || '{}');

        // Defer initialization to ensure all classes are loaded
        setTimeout(() => this.init(), 100);
    }

    /**
     * Initialize application
     */
    init() {
        Utils.log.info('Initializing SA-AIHOS Application...');

        try {
            // Check if AnimationController is defined
            if (typeof AnimationController === 'undefined') {
                setTimeout(() => this.init(), 100);
                return;
            }

            // Initialize animation controller
            this.animationController = new AnimationController();
            this.animationController.initAnimations();
            Utils.log.success('Animation Controller initialized');

            // Wait for DOM to be fully loaded
            if (document.readyState === 'loading') {
                document.addEventListener('DOMContentLoaded', () => this.setupUI());
            } else {
                this.setupUI();
            }

            this.isInitialized = true;
            Utils.log.success('SA-AIHOS Application initialized');

            // Setup backend integration
            this.setupBackendIntegration();

        } catch (error) {
            Utils.log.error('Initialization failed:', error);
            console.error(error);
        }
    }

    /**
     * Setup backend integration
     */
    setupBackendIntegration() {
        // Setup social login buttons
        const googleBtn = document.getElementById('googleLoginBtn');
        const githubBtn = document.getElementById('githubLoginBtn');

        if (googleBtn) {
            googleBtn.addEventListener('click', (e) => {
                e.preventDefault();
                if (this.loginManager) this.loginManager.loginWithGoogle();
            });
        }

        if (githubBtn) {
            githubBtn.addEventListener('click', (e) => {
                e.preventDefault();
                if (this.loginManager) this.loginManager.loginWithGitHub();
            });
        }

        // If already has token, auto-navigate
        if (this.token) {
            this.loadUserProfile();
        }
    }

    /**
     * Handle login with backend API
     */
    async handleBackendLogin(email, password) {
        try {
            const response = await fetch(`${this.apiURL}/auth/login`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email, password })
            });

            if (!response.ok) {
                const error = await response.json();
                throw new Error(error.message || 'Login failed');
            }

            const data = await response.json();

            // Store token and user
            this.token = data.token;
            this.user = data.user;
            localStorage.setItem('sa_aihos_token', this.token);
            localStorage.setItem('sa_aihos_user', JSON.stringify(this.user));

            Utils.log.success('Login successful!');
            return data;
        } catch (error) {
            Utils.log.error('Login error:', error);
            throw error;
        }
    }

    /**
     * Handle system initialization with backend
     */
    async handleBackendInitialize() {
        if (!this.token) {
            Utils.log.error('Not authenticated');
            return;
        }

        try {
            const response = await fetch(`${this.apiURL}/system/init`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${this.token}`
                }
            });

            if (!response.ok) {
                const error = await response.json();
                throw new Error(error.message || 'Initialization failed');
            }

            const data = await response.json();

            // Update user state
            this.user.initialized = true;
            localStorage.setItem('sa_aihos_user', JSON.stringify(this.user));

            Utils.log.success('System initialized!');

            // Store initialization memory
            await this.storeMemory('system', 'System initialized successfully');

            return data;
        } catch (error) {
            Utils.log.error('Initialization error:', error);
            throw error;
        }
    }

    /**
     * Load user profile from backend
     */
    async loadUserProfile() {
        if (!this.token) return;

        try {
            const response = await fetch(`${this.apiURL}/user/profile`, {
                method: 'GET',
                headers: {
                    'Authorization': `Bearer ${this.token}`
                }
            });

            if (!response.ok) throw new Error('Failed to load profile');

            const data = await response.json();
            this.user = data.user;
            this.updateProfileUI();
        } catch (error) {
            Utils.log.error('Profile load failed:', error);
        }
    }

    /**
     * Store memory to backend
     */
    async storeMemory(type, content) {
        if (!this.token) return;

        try {
            await fetch(`${this.apiURL}/memory/store`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${this.token}`
                },
                body: JSON.stringify({
                    type,
                    content,
                    metadata: {
                        timestamp: new Date().toISOString(),
                        userAgent: navigator.userAgent
                    }
                })
            });
        } catch (error) {
            Utils.log.error('Memory storage failed:', error);
        }
    }

    /**
     * Retrieve memories from backend
     */
    async retrieveMemories(type = null) {
        if (!this.token) return [];

        try {
            let url = `${this.apiURL}/memory/retrieve`;
            if (type) url += `?type=${type}`;

            const response = await fetch(url, {
                method: 'GET',
                headers: {
                    'Authorization': `Bearer ${this.token}`
                }
            });

            if (!response.ok) throw new Error('Failed to retrieve memories');
            const data = await response.json();
            return data.memories || [];
        } catch (error) {
            Utils.log.error('Memory retrieval failed:', error);
            return [];
        }
    }

    /**
     * Navigate to dashboard
     */
    navigateToDashboard() {
        const screens = document.querySelectorAll('.screen');
        screens.forEach(s => s.classList.remove('active'));

        const dashboardScreen = document.getElementById('dashboardScreen');
        if (dashboardScreen) {
            dashboardScreen.classList.add('active');
        }
    }

    /**
     * Update profile UI
     */
    updateProfileUI() {
        const usernameEl = document.getElementById('profileUsername');
        const emailEl = document.getElementById('profileEmail');

        if (usernameEl) usernameEl.textContent = this.user.first_name || this.user.username || 'User';
        if (emailEl) emailEl.textContent = this.user.email || '';
    }

    /**
     * Setup UI components
     */
    setupUI() {
        try {
            // Initialize Three.js scene
            const container = document.getElementById('neuralNet');
            if (container && typeof ThreeJSScene !== 'undefined') {
                this.threeJSScene = new ThreeJSScene('neuralNet');
                Utils.log.success('Three.js Scene initialized');
            }

            // Initialize login manager
            if (typeof LoginManager !== 'undefined') {
                this.loginManager = new LoginManager(this.animationController);
                window.loginManagerInstance = this.loginManager;
                Utils.log.success('Login Manager initialized');
            }

            // Initialize dashboard manager
            if (typeof DashboardManager !== 'undefined') {
                this.dashboardManager = new DashboardManager(this.animationController);
                Utils.log.success('Dashboard Manager initialized');
            }

            // Setup global event listeners
            this.setupEventListeners();

            // Setup performance monitoring
            this.setupPerformanceMonitoring();

            // Hide loading screen
            this.hideLoadingScreen();

            Utils.log.success('UI Setup completed');

        } catch (error) {
            Utils.log.error('UI Setup failed:', error);
            this.showFatalError(error);
        }
    }

    /**
     * Setup event listeners
     */
    setupEventListeners() {
        // Handle window resize
        window.addEventListener('resize', Utils.debounce(() => {
            this.onWindowResize();
        }, 200));

        // Handle visibility change
        document.addEventListener('visibilitychange', () => {
            this.onVisibilityChange();
        });

        // Keyboard shortcuts
        document.addEventListener('keydown', (e) => {
            this.handleKeyboardShortcuts(e);
        });

        // Track user activity
        document.addEventListener('mousemove', Utils.throttle(() => {
            this.updateLastActivity();
        }, 5000));

        // Login success handler
        window.addEventListener('loginSuccess', (e) => {
            Utils.log.success('Login success event received');
            this.navigateToDashboard();
        });

        // Logout button
        const logoutBtn = document.getElementById('logoutBtn');
        if (logoutBtn) {
            logoutBtn.addEventListener('click', () => this.logout());
        }

        // Error handling
        window.addEventListener('error', (e) => {
            Utils.log.error('Global error:', e.error);
        });

        window.addEventListener('unhandledrejection', (e) => {
            Utils.log.error('Unhandled promise rejection:', e.reason);
        });
    }

    /**
     * Logout
     */
    logout() {
        this.token = null;
        this.user = {};
        localStorage.removeItem('sa_aihos_token');
        localStorage.removeItem('sa_aihos_user');

        const screens = document.querySelectorAll('.screen');
        screens.forEach(s => s.classList.remove('active'));

        const loginScreen = document.getElementById('loginScreen');
        if (loginScreen) loginScreen.classList.add('active');

        if (this.loginManager) this.loginManager.reset();
    }

    /**
     * Hide loading screen after animations
     */
    hideLoadingScreen() {
        const loadingScreen = document.querySelector('.loading-screen');
        if (loadingScreen) {
            gsap.to(loadingScreen, {
                opacity: 0,
                duration: 0.6,
                delay: 3,
                pointerEvents: 'none'
            });
        }
    }

    /**
     * Setup performance monitoring
     */
    setupPerformanceMonitoring() {
        let lastTime = performance.now();
        let frames = 0;

        const measureFPS = () => {
            const currentTime = performance.now();
            const deltaTime = currentTime - lastTime;
            frames++;

            if (deltaTime >= 1000) {
                const fps = Math.round(frames * 1000 / deltaTime);
                frames = 0;
                lastTime = currentTime;
                if (fps < 50) {
                    Utils.log.warn('Low FPS detected:', fps);
                }
            }
            requestAnimationFrame(measureFPS);
        };

        requestAnimationFrame(measureFPS);
    }

    /**
     * Handle window resize
     */
    onWindowResize() {
        if (this.threeJSScene && this.threeJSScene.onWindowResize) {
            this.threeJSScene.onWindowResize();
        }
    }

    /**
     * Handle visibility change
     */
    onVisibilityChange() {
        if (document.hidden) {
            gsap.globalTimeline.pause();
        } else {
            gsap.globalTimeline.resume();
        }
    }

    /**
     * Handle keyboard shortcuts
     */
    handleKeyboardShortcuts(event) {
        if ((event.ctrlKey || event.metaKey) && event.key === 'l') {
            event.preventDefault();
            this.logout();
        }
    }

    /**
     * Update last user activity
     */
    updateLastActivity() {
        Utils.session.set('lastActivity', Date.now());
    }

    /**
     * Show fatal error
     */
    showFatalError(error) {
        const errorContainer = document.createElement('div');
        errorContainer.style.cssText = `
            position: fixed; top: 0; left: 0; width: 100%; height: 100%;
            background: rgba(26, 26, 46, 0.95); display: flex; align-items: center;
            justify-content: center; z-index: 99999; backdrop-filter: blur(10px);
        `;

        const content = document.createElement('div');
        content.style.cssText = `
            text-align: center; color: #ff0055; padding: 40px;
            border: 1px solid rgba(255, 0, 85, 0.3); border-radius: 12px; max-width: 500px;
        `;

        content.innerHTML = `
            <h1 style="margin-bottom: 20px; font-size: 24px;">Error</h1>
            <p style="margin-bottom: 30px; color: #b0b0b0;">${error.message || 'An unexpected error occurred'}</p>
            <button onclick="location.reload()" style="
                padding: 12px 24px; background: #ff0055; border: none; color: white;
                border-radius: 6px; cursor: pointer; font-weight: 600;
            ">Reload Page</button>
        `;

        errorContainer.appendChild(content);
        document.body.appendChild(errorContainer);
    }

    /**
     * Get application status
     */
    getStatus() {
        return {
            initialized: this.isInitialized,
            animationController: !!this.animationController,
            loginManager: !!this.loginManager,
            dashboardManager: !!this.dashboardManager,
            threeJSScene: !!this.threeJSScene,
            timestamp: new Date().toISOString()
        };
    }

    /**
     * Clean up resources
     */
    dispose() {
        Utils.log.info('Cleaning up resources...');
        if (this.threeJSScene && this.threeJSScene.dispose) {
            this.threeJSScene.dispose();
        }
        gsap.killTweensOf('*');
        Utils.log.success('Resources cleaned up');
    }
}

// Initialize app when DOM is ready
let appInstance = null;

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
        appInstance = new SAIHOSApp();
        window.appInstance = appInstance;
    });
} else {
    appInstance = new SAIHOSApp();
    window.appInstance = appInstance;
}

// Cleanup on page unload
window.addEventListener('beforeunload', () => {
    if (appInstance) {
        appInstance.dispose();
    }
});

// Export for console access
window.SAIHOSApp = SAIHOSApp;
