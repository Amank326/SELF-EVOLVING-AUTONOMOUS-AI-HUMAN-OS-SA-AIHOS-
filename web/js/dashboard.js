/**
 * Dashboard Logic
 * Manages dashboard functionality, real-time updates, and interactions
 */

class DashboardManager {
    constructor(animationController) {
        this.animationController = animationController;
        this.userData = {
            email: '',
            systemHealth: 85,
            activeConnections: 1247,
            evolutionProgress: 56,
            memoryUsage: 72,
            autonomyLevel: 91,
            reflectionDepth: 68,
            reasoningAccuracy: 94
        };

        this.updateIntervals = [];
        this.charts = {};
        this.initialized = false;

        this.init();
    }

    /**
     * Initialize dashboard
     */
    init() {
        window.addEventListener('loginSuccess', (e) => {
            this.userData.email = e.detail.email;
            this.onDashboardLoad();
        });

        this.attachEventListeners();
    }

    /**
     * Attach event listeners to dashboard elements
     */
    attachEventListeners() {
        const logoutBtn = document.getElementById('logoutBtn');
        if (logoutBtn) {
            logoutBtn.addEventListener('click', () => this.logout());
        }

        // Card interactions
        const cards = document.querySelectorAll('.card');
        cards.forEach(card => {
            card.addEventListener('mouseenter', () => {
                this.animationController.animateCardHover(card);
            });

            card.addEventListener('mouseleave', () => {
                this.animationController.animateCardHoverOut(card);
            });

            card.addEventListener('click', (e) => {
                const cardType = card.getAttribute('data-card');
                this.onCardClick(cardType);
            });
        });

        // Control buttons
        const controlBtns = document.querySelectorAll('.control-btn');
        controlBtns.forEach(btn => {
            btn.addEventListener('click', () => {
                controlBtns.forEach(b => b.classList.remove('active'));
                btn.classList.add('active');

                const view = btn.getAttribute('data-view');
                this.switchView(view);
            });
        });
    }

    /**
     * Handle dashboard load
     */
    onDashboardLoad() {
        if (this.initialized) return;

        this.initialized = true;

        Utils.log.success('Dashboard loaded for:', this.userData.email);

        // Create dashboard animation
        this.animationController.createDashboardAnimation();

        // Initialize updates
        this.startRealtimeUpdates();

        // Initialize charts
        this.initializeCharts();
    }

    /**
     * Start real-time data updates
     */
    startRealtimeUpdates() {
        // Update metrics every 2 seconds
        const metricsInterval = setInterval(() => {
            this.updateMetrics();
        }, 2000);

        this.updateIntervals.push(metricsInterval);

        // Update stats every 3 seconds
        const statsInterval = setInterval(() => {
            this.updateStats();
        }, 3000);

        this.updateIntervals.push(statsInterval);

        // Trigger chart updates
        this.updateCharts();
    }

    /**
     * Update dashboard metrics
     */
    updateMetrics() {
        const cards = document.querySelectorAll('.card');

        cards.forEach((card, index) => {
            const statusFills = card.querySelectorAll('.status-fill');
            const statusValues = card.querySelectorAll('.status-value');

            statusFills.forEach((fill, i) => {
                const currentWidth = parseInt(fill.style.width) || 0;
                const change = Utils.random(-10, 10);
                const newValue = Math.max(0, Math.min(100, currentWidth + change));

                gsap.to(fill, {
                    width: newValue + '%',
                    duration: 1,
                    ease: 'power1.inOut'
                });

                // Update value text
                if (statusValues[i]) {
                    gsap.to(statusValues[i], {
                        innerText: Math.round(newValue),
                        snap: { innerText: 1 },
                        duration: 1
                    });
                }
            });
        });
    }

    /**
     * Update dashboard statistics
     */
    updateStats() {
        // Simulate active connections changing
        const activeConnVal = document.querySelector('[data-stat="activeConnections"]');
        if (activeConnVal) {
            const change = Utils.random(-50, 50);
            const newValue = Math.max(1000, this.userData.activeConnections + change);
            this.userData.activeConnections = Math.round(newValue);

            gsap.to(activeConnVal, {
                innerText: Utils.formatNumber(this.userData.activeConnections),
                duration: 0.8,
                snap: { innerText: 1 }
            });
        }

        // Simulate evolution progress
        const evolutionVal = document.querySelector('[data-stat="evolutionProgress"]');
        if (evolutionVal) {
            const change = Utils.random(0, 3);
            const newValue = Math.min(100, this.userData.evolutionProgress + change);
            this.userData.evolutionProgress = Math.round(newValue);

            gsap.to(evolutionVal, {
                innerText: this.userData.evolutionProgress + '%',
                duration: 0.8,
                snap: { innerText: 1 }
            });
        }
    }

    /**
     * Initialize charts
     */
    initializeCharts() {
        // Evolution chart
        const evolutionChart = document.getElementById('evolutionChart');
        if (evolutionChart) {
            this.createEvolutionChart(evolutionChart);
        }

        // System health chart
        const healthChart = document.getElementById('healthChart');
        if (healthChart) {
            this.createHealthChart(healthChart);
        }
    }

    /**
     * Create evolution chart
     */
    createEvolutionChart(container) {
        if (!container) return;

        // Create simple canvas-based chart
        const canvas = document.createElement('canvas');
        canvas.width = container.offsetWidth;
        canvas.height = 200;
        container.appendChild(canvas);

        const ctx = canvas.getContext('2d');

        const data = Array.from({ length: 30 }, (_, i) => Math.random() * 100);

        // Draw chart
        const drawChart = () => {
            ctx.clearRect(0, 0, canvas.width, canvas.height);

            // Draw grid
            ctx.strokeStyle = 'rgba(0, 217, 255, 0.1)';
            ctx.lineWidth = 1;

            for (let i = 0; i <= 5; i++) {
                const y = (i / 5) * canvas.height;
                ctx.beginPath();
                ctx.moveTo(0, y);
                ctx.lineTo(canvas.width, y);
                ctx.stroke();
            }

            // Draw line
            ctx.strokeStyle = '#00d9ff';
            ctx.lineWidth = 2;
            ctx.beginPath();

            const xStep = canvas.width / (data.length - 1);

            data.forEach((value, index) => {
                const x = index * xStep;
                const y = canvas.height - (value / 100) * canvas.height;

                if (index === 0) {
                    ctx.moveTo(x, y);
                } else {
                    ctx.lineTo(x, y);
                }
            });

            ctx.stroke();

            // Draw fill
            ctx.fillStyle = 'rgba(0, 217, 255, 0.1)';
            ctx.lineTo(canvas.width, canvas.height);
            ctx.lineTo(0, canvas.height);
            ctx.closePath();
            ctx.fill();
        };

        drawChart();

        // Update chart data
        setInterval(() => {
            data.shift();
            data.push(Math.random() * 100);
            drawChart();
        }, 2000);
    }

    /**
     * Create system health chart
     */
    createHealthChart(container) {
        if (!container) return;

        const canvas = document.createElement('canvas');
        canvas.width = container.offsetWidth;
        canvas.height = 200;
        container.appendChild(canvas);

        const ctx = canvas.getContext('2d');

        const metrics = {
            cpu: 45,
            memory: 72,
            network: 38,
            storage: 61
        };

        const drawChart = () => {
            ctx.clearRect(0, 0, canvas.width, canvas.height);

            const barWidth = canvas.width / 4 - 10;
            const barHeight = canvas.height - 40;
            const colors = ['#00d9ff', '#ff006e', '#8f00ff', '#00ff88'];

            const values = Object.values(metrics);
            const labels = Object.keys(metrics);

            values.forEach((value, index) => {
                const x = index * (barWidth + 15) + 10;
                const y = canvas.height - (value / 100) * barHeight;
                const height = (value / 100) * barHeight;

                // Draw bar
                ctx.fillStyle = colors[index];
                ctx.globalAlpha = 0.7;
                ctx.fillRect(x, y, barWidth, height);
                ctx.globalAlpha = 1;

                // Draw border
                ctx.strokeStyle = colors[index];
                ctx.lineWidth = 2;
                ctx.strokeRect(x, y, barWidth, height);

                // Draw label
                ctx.fillStyle = '#b0b0b0';
                ctx.font = '12px Arial';
                ctx.textAlign = 'center';
                ctx.fillText(labels[index].toUpperCase(), x + barWidth / 2, canvas.height - 15);

                // Draw value
                ctx.fillStyle = colors[index];
                ctx.font = 'bold 14px Arial';
                ctx.fillText(value + '%', x + barWidth / 2, y - 10);
            });
        };

        drawChart();

        // Update chart data
        setInterval(() => {
            Object.keys(metrics).forEach(key => {
                const change = Utils.random(-5, 5);
                metrics[key] = Math.max(0, Math.min(100, metrics[key] + change));
            });
            drawChart();
        }, 3000);
    }

    /**
     * Update charts
     */
    updateCharts() {
        // Charts are auto-updated by their intervals
    }

    /**
     * Handle card click
     */
    onCardClick(cardType) {
        Utils.log.info('Card clicked:', cardType);

        // Show detailed view for card
        const card = document.querySelector(`[data-card="${cardType}"]`);
        if (card) {
            gsap.timeline()
                .to(card, { scale: 1.05, duration: 0.2 })
                .to(card, { scale: 1, duration: 0.2 });
        }
    }

    /**
     * Switch dashboard view
     */
    switchView(view) {
        Utils.log.info('Switching view to:', view);

        const dashboardContent = document.querySelector('.dashboard-content');

        gsap.timeline()
            .to(dashboardContent, { opacity: 0.5, duration: 0.2 })
            .to(dashboardContent, { opacity: 1, duration: 0.2 });
    }

    /**
     * Handle logout
     */
    logout() {
        // Stop all intervals
        this.updateIntervals.forEach(interval => clearInterval(interval));
        this.updateIntervals = [];

        const dashboardScreen = document.getElementById('dashboardScreen');
        const loginScreen = document.getElementById('loginScreen');

        // Animate logout
        this.animationController.animateLogout()
            .then(() => {
                dashboardScreen.classList.remove('active');
                loginScreen.classList.add('active');

                // Reset login form
                const loginManager = window.loginManagerInstance;
                if (loginManager) {
                    loginManager.reset();
                }

                this.initialized = false;

                Utils.log.success('Logged out successfully');
            });
    }

    /**
     * Update user data
     */
    setUserData(data) {
        this.userData = { ...this.userData, ...data };
    }

    /**
     * Get user data
     */
    getUserData() {
        return this.userData;
    }

    /**
     * Show notification
     */
    showNotification(message, type = 'info') {
        const notification = document.createElement('div');
        notification.className = `notification notification-${type}`;
        notification.style.cssText = `
            position: fixed;
            top: 20px;
            right: 20px;
            background: rgba(26, 26, 46, 0.9);
            border: 1px solid ${type === 'success' ? '#00ff88' : type === 'error' ? '#ff0055' : '#00d9ff'};
            color: ${type === 'success' ? '#00ff88' : type === 'error' ? '#ff0055' : '#00d9ff'};
            padding: 16px 24px;
            border-radius: 8px;
            font-size: 12px;
            z-index: 9999;
            opacity: 0;
            animation: slideInCard 0.4s ease-out forwards;
            backdrop-filter: blur(10px);
        `;

        notification.textContent = message;
        document.body.appendChild(notification);

        gsap.timeline()
            .to(notification, { opacity: 1, duration: 0.3 })
            .to(notification, { opacity: 0, duration: 0.3 }, '+=2.5')
            .call(() => notification.remove());
    }

    /**
     * Export dashboard data
     */
    exportData() {
        const data = {
            timestamp: new Date().toISOString(),
            user: this.userData.email,
            metrics: {
                systemHealth: this.userData.systemHealth,
                activeConnections: this.userData.activeConnections,
                evolutionProgress: this.userData.evolutionProgress,
                memoryUsage: this.userData.memoryUsage,
                autonomyLevel: this.userData.autonomyLevel,
                reflectionDepth: this.userData.reflectionDepth,
                reasoningAccuracy: this.userData.reasoningAccuracy
            }
        };

        const dataStr = JSON.stringify(data, null, 2);
        const blob = new Blob([dataStr], { type: 'application/json' });
        const url = URL.createObjectURL(blob);

        const link = document.createElement('a');
        link.href = url;
        link.download = `dashboard_export_${new Date().getTime()}.json`;
        link.click();

        URL.revokeObjectURL(url);

        this.showNotification('Data exported successfully', 'success');
    }
}

// Export
window.DashboardManager = DashboardManager;
