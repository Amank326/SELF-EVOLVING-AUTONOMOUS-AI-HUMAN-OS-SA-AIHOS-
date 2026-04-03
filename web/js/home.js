/**
 * Home Page - Main Application Logic
 * Manages UI, API communication, and real-time updates
 */

class HomePage {
    constructor() {
        this.apiBase = 'http://localhost:3000/api';
        this.wsConnection = null;
        this.refreshInterval = 5000;
        this.currentSection = 'dashboard';
        
        this.init();
    }

    /**
     * Initialize the home page
     */
    async init() {
        Utils.log.info('Initializing Home Page...');

        try {
            // Setup WebSocket connection
            this.setupWebSocket();

            // Setup event listeners
            this.setupEventListeners();

            // Load initial data
            await this.loadAllData();

            // Setup refresh intervals
            this.setupRefreshIntervals();

            // Hide loading screen
            this.hideLoadingScreen();

            // Show dashboard
            this.showSection('dashboard');

            Utils.log.success('Home Page initialized');

        } catch (error) {
            Utils.log.error('Initialization failed:', error);
        }
    }

    /**
     * Setup WebSocket for real-time updates
     */
    setupWebSocket() {
        try {
            this.wsConnection = new WebSocket('ws://localhost:3000');

            this.wsConnection.onopen = () => {
                Utils.log.info('WebSocket connected');
            };

            this.wsConnection.onmessage = (event) => {
                const data = JSON.parse(event.data);
                this.handleWebSocketMessage(data);
            };

            this.wsConnection.onerror = (error) => {
                Utils.log.error('WebSocket error:', error);
            };

            this.wsConnection.onclose = () => {
                Utils.log.info('WebSocket disconnected');
                // Attempt to reconnect in 5 seconds
                setTimeout(() => this.setupWebSocket(), 5000);
            };

        } catch (error) {
            Utils.log.error('WebSocket setup failed:', error);
        }
    }

    /**
     * Handle WebSocket messages
     */
    handleWebSocketMessage(data) {
        console.log('WebSocket message:', data);

        if (data.type === 'agent_decision') {
            this.updateDecisionInfo(data);
            this.refreshDashboard();
        } else if (data.type === 'memory_update') {
            this.refreshMemory();
        }
    }

    /**
     * Setup event listeners
     */
    setupEventListeners() {
        // Autonomy slider
        const slider = document.getElementById('autonomySlider');
        if (slider) {
            slider.addEventListener('input', (e) => {
                document.getElementById('autonomyValue').textContent = e.target.value + '%';
                this.updateAutonomyLevel(e.target.value);
            });
        }
    }

    /**
     * Setup refresh intervals
     */
    setupRefreshIntervals() {
        setInterval(async () => {
            await this.loadDashboardData();
        }, this.refreshInterval);

        setInterval(async () => {
            if (this.currentSection === 'agent') {
                await this.loadAgentData();
            }
        }, this.refreshInterval);

        setInterval(async () => {
            if (this.currentSection === 'memory') {
                await this.loadMemoryData();
            }
        }, this.refreshInterval * 2);
    }

    /**
     * Load all data
     */
    async loadAllData() {
        try {
            await Promise.all([
                this.loadDashboardData(),
                this.loadAgentData(),
                this.loadMemoryData(),
                this.loadTasksData()
            ]);
        } catch (error) {
            Utils.log.error('Data loading failed:', error);
        }
    }

    /**
     * Load dashboard data
     */
    async loadDashboardData() {
        try {
            const response = await fetch(`${this.apiBase}/dashboard/overview`);
            const data = await response.json();

            if (data.success) {
                const stats = data.data;
                document.getElementById('systemStatus').textContent = stats.systemStatus;
                document.getElementById('aiMode').textContent = stats.aiMode;
                document.getElementById('uptime').textContent = stats.uptimeHours + 'h';
                document.getElementById('decisionsToday').textContent = stats.decisionsToday;
                document.getElementById('episodeCount').textContent = stats.memoryUtilization.episodicMemory;
                document.getElementById('factCount').textContent = stats.memoryUtilization.semanticMemory;
                document.getElementById('ruleCount').textContent = stats.memoryUtilization.proceduralMemory;
            }

            const metricsResponse = await fetch(`${this.apiBase}/dashboard/metrics`);
            const metrics = await metricsResponse.json();

            if (metrics.success) {
                const m = metrics.data;
                document.getElementById('successRate').textContent = m.taskSuccessRate + '%';
                document.getElementById('avgTime').textContent = m.averageDecisionTime;
                document.getElementById('cpuUsage').textContent = m.cpuUsage.toFixed(1) + '%';
                document.getElementById('cpuProgress').style.width = m.cpuUsage + '%';
            }

        } catch (error) {
            Utils.log.error('Dashboard data loading failed:', error);
        }
    }

    /**
     * Load agent data
     */
    async loadAgentData() {
        try {
            const response = await fetch(`${this.apiBase}/agent/status`);
            const data = await response.json();

            if (data.success) {
                const status = data.data;
                const running = document.getElementById('agentRunning');
                if (running) {
                    running.textContent = status.isRunning ? 'YES' : 'NO';
                    running.className = status.isRunning ? 'status-badge active' : 'status-badge offline';
                }

                const nextDecision = document.getElementById('nextDecision');
                if (nextDecision) {
                    nextDecision.textContent = status.nextDecisionIn;
                }

                const lastThought = document.getElementById('lastThought');
                if (lastThought) {
                    lastThought.textContent = status.lastThought || 'None yet';
                }

                // Load recent decisions
                const decisionsResponse = await fetch(`${this.apiBase}/dashboard/metrics`);
                const metricsData = await decisionsResponse.json();
                if (metricsData.success && metricsData.data.recentDecisions) {
                    this.populateDecisionsList(metricsData.data.recentDecisions);
                }
            }

        } catch (error) {
            Utils.log.error('Agent data loading failed:', error);
        }
    }

    /**
     * Load memory data
     */
    async loadMemoryData() {
        try {
            const response = await fetch(`${this.apiBase}/memory/overview`);
            const data = await response.json();

            if (data.success) {
                const overview = data.data;

                // Populate episodic memory
                this.populateMemoryList('episodicList', overview.recentEpisodes, 'episodic');

                // Populate semantic facts
                this.populateMemoryList('semanticList', overview.topSemanticFacts, 'semantic');

                // Populate procedural rules
                this.populateMemoryList('proceduralList', overview.topBehavioralRules, 'procedural');
            }

        } catch (error) {
            Utils.log.error('Memory data loading failed:', error);
        }
    }

    /**
     * Load tasks data
     */
    async loadTasksData() {
        try {
            const response = await fetch(`${this.apiBase}/tasks`);
            const data = await response.json();

            if (data.success) {
                this.populateTasksList(data.data);
            }

        } catch (error) {
            Utils.log.error('Tasks data loading failed:', error);
        }
    }

    /**
     * Populate decisions list
     */
    populateDecisionsList(decisions) {
        const container = document.getElementById('decisionsList');
        if (!container) return;

        if (!decisions || decisions.length === 0) {
            container.innerHTML = '<p class="empty-state">No recent decisions</p>';
            return;
        }

        container.innerHTML = decisions.map(decision => `
            <div class="list-item">
                <div style="display: flex; justify-content: space-between; align-items: center;">
                    <span style="color: var(--primary); font-weight: bold;">${decision.action || 'Action'}</span>
                    <span class="status-badge ${decision.success ? 'success' : 'error'}">
                        ${decision.success ? 'SUCCESS' : 'FAILED'}
                    </span>
                </div>
                <p style="font-size: 0.85em; color: var(--text-secondary); margin-top: 5px;">
                    ${new Date(decision.timestamp).toLocaleString()}
                </p>
            </div>
        `).join('');
    }

    /**
     * Populate memory list
     */
    populateMemoryList(containerId, items, type) {
        const container = document.getElementById(containerId);
        if (!container || !items || items.length === 0) {
            if (container) {
                container.innerHTML = '<p class="empty-state">No items</p>';
            }
            return;
        }

        container.innerHTML = items.map(item => {
            if (type === 'episodic') {
                return `
                    <div class="memory-item">
                        <strong>${item.decision || item.action || 'Event'}</strong>
                        <p style="margin: 8px 0; font-size: 0.9em;">Outcome: ${item.outcome || 'Unknown'}</p>
                        <p style="font-size: 0.85em; color: var(--text-secondary);">
                            ${new Date(item.timestamp).toLocaleString()}
                        </p>
                    </div>
                `;
            } else if (type === 'semantic') {
                return `
                    <div class="memory-item">
                        <strong>${item.fact || 'Fact'}</strong>
                        <p style="margin: 8px 0; font-size: 0.9em;">Confidence: ${(item.confidence * 100).toFixed(0)}%</p>
                    </div>
                `;
            } else if (type === 'procedural') {
                return `
                    <div class="memory-item">
                        <strong>${item.action || 'Rule'}</strong>
                        <p style="margin: 8px 0; font-size: 0.9em;">Weight: ${(item.weight * 100).toFixed(0)}%</p>
                    </div>
                `;
            }
        }).join('');
    }

    /**
     * Populate tasks list
     */
    populateTasksList(tasks) {
        const container = document.getElementById('tasksList');
        if (!container) return;

        if (!tasks || tasks.length === 0) {
            container.innerHTML = '<p class="empty-state">No tasks</p>';
            return;
        }

        container.innerHTML = tasks.map(task => `
            <div class="list-item">
                <div style="display: flex; justify-content: space-between; align-items: center;">
                    <span style="color: var(--primary); font-weight: bold;">${task.title}</span>
                    <span class="status-badge" style="background: rgba(255, 170, 0, 0.1); color: var(--warning);">
                        ${task.priority}
                    </span>
                </div>
                <p style="font-size: 0.85em; color: var(--text-secondary); margin: 8px 0;">
                    ${task.description}
                </p>
                <p style="font-size: 0.8em; color: var(--text-secondary);">
                    Status: ${task.status}
                </p>
            </div>
        `).join('');
    }

    /**
     * Show section
     */
    showSection(sectionId) {
        // Hide all sections
        document.querySelectorAll('.content-section').forEach(section => {
            section.classList.remove('active');
        });

        // Show selected section
        const section = document.getElementById(sectionId);
        if (section) {
            section.classList.add('active');
            this.currentSection = sectionId;

            // Load data for the section
            if (sectionId === 'dashboard') {
                this.loadDashboardData();
            } else if (sectionId === 'agent') {
                this.loadAgentData();
            } else if (sectionId === 'memory') {
                this.loadMemoryData();
            } else if (sectionId === 'tasks') {
                this.loadTasksData();
            }
        }
    }

    /**
     * Toggle agent autonomy
     */
    async toggleAgentAutonomy() {
        try {
            const currentStatus = document.getElementById('agentRunning').textContent === 'YES';
            const response = await fetch(`${this.apiBase}/agent/toggle-autonomy`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ enabled: !currentStatus })
            });

            const data = await response.json();
            if (data.success) {
                await this.loadAgentData();
                Utils.log.success(data.message);
            }

        } catch (error) {
            Utils.log.error('Failed to toggle autonomy:', error);
        }
    }

    /**
     * Update autonomy level
     */
    async updateAutonomyLevel(level) {
        try {
            const response = await fetch(`${this.apiBase}/agent/set-autonomy-level`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ level: parseInt(level) })
            });

            const data = await response.json();
            if (data.success) {
                Utils.log.info('Autonomy level updated');
            }

        } catch (error) {
            Utils.log.error('Failed to update autonomy level:', error);
        }
    }

    /**
     * Trigger reflection
     */
    async triggerReflection() {
        try {
            const response = await fetch(`${this.apiBase}/agent/trigger-reflection`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' }
            });

            const data = await response.json();
            if (data.success) {
                console.log('Reflection triggered:', data.data);
                Utils.log.success('Reflection triggered');
            }

        } catch (error) {
            Utils.log.error('Failed to trigger reflection:', error);
        }
    }

    /**
     * Create task
     */
    async createTask() {
        const title = document.getElementById('taskTitle')?.value;
        const description = document.getElementById('taskDesc')?.value;
        const priority = document.getElementById('taskPriority')?.value;

        if (!title) {
            alert('Please enter a task title');
            return;
        }

        try {
            const response = await fetch(`${this.apiBase}/tasks/create`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ title, description, priority })
            });

            const data = await response.json();
            if (data.success) {
                // Clear form
                document.getElementById('taskTitle').value = '';
                document.getElementById('taskDesc').value = '';
                
                // Reload tasks
                await this.loadTasksData();
                Utils.log.success('Task created');
            }

        } catch (error) {
            Utils.log.error('Failed to create task:', error);
        }
    }

    /**
     * Save settings
     */
    async saveSettings() {
        const refreshRate = document.getElementById('refreshRate')?.value;
        const decisionInterval = document.getElementById('decisionInterval')?.value;
        const memoryThreshold = document.getElementById('memoryThreshold')?.value;

        try {
            await Promise.all([
                this.saveSetting('refresh_rate', refreshRate),
                this.saveSetting('decision_interval', decisionInterval),
                this.saveSetting('memory_threshold', memoryThreshold)
            ]);

            Utils.log.success('Settings saved');

        } catch (error) {
            Utils.log.error('Failed to save settings:', error);
        }
    }

    /**
     * Save individual setting
     */
    async saveSetting(key, value) {
        try {
            const response = await fetch(`${this.apiBase}/settings/update`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ setting: key, value })
            });

            return await response.json();

        } catch (error) {
            Utils.log.error(`Failed to save ${key}:`, error);
        }
    }

    /**
     * Update decision info
     */
    updateDecisionInfo(data) {
        const lastThought = document.getElementById('lastThought');
        if (lastThought) {
            lastThought.textContent = data.decision;
        }
    }

    /**
     * Refresh dashboard
     */
    async refreshDashboard() {
        await this.loadDashboardData();
    }

    /**
     * Refresh memory
     */
    async refreshMemory() {
        await this.loadMemoryData();
    }

    /**
     * Hide loading screen
     */
    hideLoadingScreen() {
        const loadingScreen = document.getElementById('loadingScreen');
        if (loadingScreen) {
            setTimeout(() => {
                loadingScreen.classList.add('hidden');
            }, 500);
        }
    }

    /**
     * Logout
     */
    logout() {
        if (confirm('Are you sure you want to logout?')) {
            // Clear session
            localStorage.removeItem('sessionToken');
            localStorage.removeItem('username');

            // Redirect to login
            window.location.href = 'index.html';
        }
    }
}

// Global functions for HTML onclick handlers
function showSection(sectionId) {
    if (window.homePage) {
        window.homePage.showSection(sectionId);
    }
}

async function toggleAgentAutonomy() {
    if (window.homePage) {
        await window.homePage.toggleAgentAutonomy();
    }
}

async function triggerReflection() {
    if (window.homePage) {
        await window.homePage.triggerReflection();
    }
}

function switchMemoryTab(tabName) {
    // Hide all tabs
    document.querySelectorAll('.memory-tab').forEach(tab => {
        tab.classList.remove('active');
    });

    // Show selected tab
    const tab = document.getElementById(tabName);
    if (tab) {
        tab.classList.add('active');
    }

    // Update button states
    document.querySelectorAll('.tab-button').forEach(btn => {
        btn.classList.remove('active');
    });

    event.target.classList.add('active');
}

async function createTask() {
    if (window.homePage) {
        await window.homePage.createTask();
    }
}

async function saveSettings() {
    if (window.homePage) {
        await window.homePage.saveSettings();
    }
}

function logout() {
    if (window.homePage) {
        window.homePage.logout();
    }
}

// Initialize when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
    window.homePage = new HomePage();
});
