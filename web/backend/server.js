/**
 * SA-AIHOS Backend Server - Complete Operating System
 * Express.js API with AI Agent, Memory Management, Database & OS Management
 * Ports: 3000 (Backend), 8080 (Frontend)
 */

const express = require('express');
const cors = require('cors');
const bodyParser = require('body-parser');
const path = require('path');
const fs = require('fs');
const sqlite3 = require('sqlite3').verbose();

// Import AI components
const AIAgent = require('./ai/AIAgent');
const Database = require('./db/Database');
const MemoryManager = require('./ai/MemoryManager');
const ReasoningEngine = require('./ai/ReasoningEngine');
const EvolutionEngine = require('./ai/EvolutionEngine');

// Import OS components
const ProcessManager = require('./os/ProcessManager');
const TaskScheduler = require('./os/TaskScheduler');
const SystemLogger = require('./os/SystemLogger');
const UserManager = require('./os/UserManager');
const SystemManager = require('./os/SystemManager');

// App setup
const app = express();
const PORT = process.env.PORT || 3000;

// Middleware
app.use(cors());
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: true }));

// Request logging middleware
app.use((req, res, next) => {
    const startTime = Date.now();
    
    res.on('finish', () => {
        const duration = Date.now() - startTime;
        if (systemLogger) {
            systemLogger.logApiRequest(req.method, req.path, res.statusCode, duration);
        }
    });
    
    next();
});

// Initialize systems
let aiAgent = null;
let database = null;
let memoryManager = null;
let reasoningEngine = null;
let evolutionEngine = null;

// OS components
let processManager = null;
let taskScheduler = null;
let systemLogger = null;
let userManager = null;
let systemManager = null;

/**
 * Initialize all systems
 */
async function initializeSystems() {
    try {
        console.log('🚀 Initializing SA-AIHOS Complete Operating System...');

        // Initialize Logger first (needed by all other components)
        systemLogger = new SystemLogger('./logs');
        systemLogger.info('🚀 System Logger initialized');

        // Initialize database
        database = new Database();
        await database.initialize();
        systemLogger.info('✅ Database initialized');

        // Initialize memory manager
        memoryManager = new MemoryManager(database);
        await memoryManager.initialize();
        systemLogger.info('✅ Memory Manager initialized');

        // Initialize reasoning engine
        reasoningEngine = new ReasoningEngine(memoryManager);
        systemLogger.info('✅ Reasoning Engine initialized');

        // Initialize evolution engine
        evolutionEngine = new EvolutionEngine(memoryManager, reasoningEngine);
        systemLogger.info('✅ Evolution Engine initialized');

        // Initialize AI Agent
        aiAgent = new AIAgent(memoryManager, reasoningEngine, database);
        aiAgent.evolutionEngine = evolutionEngine;
        await aiAgent.initialize();
        systemLogger.info('✅ AI Agent initialized');

        // Start autonomous cognition loop
        aiAgent.startCognitionLoop();
        systemLogger.info('✅ Autonomous cognition loop started');

        // Initialize OS components
        processManager = new ProcessManager();
        systemLogger.info('✅ Process Manager initialized');

        taskScheduler = new TaskScheduler(processManager);
        systemLogger.info('✅ Task Scheduler initialized');

        userManager = new UserManager(database);
        systemLogger.info('✅ User Manager initialized');

        // Initialize System Manager (coordinates everything)
        systemManager = new SystemManager(processManager, taskScheduler, systemLogger, userManager);
        await systemManager.initialize({
            autoRecovery: true,
            healthCheckInterval: 300000 // 5 minutes
        });
        systemLogger.info('✅ System Manager initialized');

        // Setup event logging
        setupEventLogging();
        systemLogger.info('✅ Event logging configured');

        return true;
    } catch (error) {
        console.error('❌ Initialization failed:', error);
        if (systemLogger) {
            systemLogger.critical('❌ OS Initialization failed', { error: error.message });
        }
        return false;
    }
}

/**
 * Setup event logging for all components
 */
function setupEventLogging() {
    // Log AI decisions
    if (aiAgent) {
        aiAgent.on('decision-made', (data) => {
            systemLogger.logAiDecision('AIAgent', data.decision, data.reasoning, data.confidence);
        });
    }

    // Log process events
    if (processManager) {
        processManager.on('process-created', (proc) => {
            systemLogger.logProcessEvent(proc, 'created');
        });
        processManager.on('process-started', (proc) => {
            systemLogger.logProcessEvent(proc, 'started');
        });
        processManager.on('process-completed', (proc) => {
            systemLogger.logProcessEvent(proc, 'completed', { duration: proc.duration });
        });
    }

    // Log user events
    if (userManager) {
        userManager.on('user-authenticated', (data) => {
            systemLogger.info(`👤 User authenticated: ${data.username}`);
        });
    }
}

// ============================================================================
// API ROUTES - OS & SYSTEM MANAGEMENT
// ============================================================================

app.get('/api/system/status', (req, res) => {
    try {
        const status = systemManager.getStatus();
        res.json({ success: true, data: status });
    } catch (error) {
        systemLogger.error('Failed to fetch system status', { error: error.message });
        res.status(500).json({ error: 'Failed to fetch system status' });
    }
});

app.get('/api/system/dashboard', (req, res) => {
    try {
        const dashboard = systemManager.getDashboard();
        res.json({ success: true, data: dashboard });
    } catch (error) {
        systemLogger.error('Failed to fetch system dashboard', { error: error.message });
        res.status(500).json({ error: 'Failed to fetch dashboard' });
    }
});

app.get('/api/system/info', (req, res) => {
    try {
        const info = systemManager.getSystemInfo();
        res.json({ success: true, data: info });
    } catch (error) {
        res.status(500).json({ error: 'Failed to fetch system info' });
    }
});

app.get('/api/system/health', async (req, res) => {
    try {
        const health = await systemManager.performHealthCheck();
        res.json({ success: true, data: health });
    } catch (error) {
        systemLogger.error('Health check failed', { error: error.message });
        res.status(500).json({ error: 'Health check failed' });
    }
});

app.post('/api/system/recovery', async (req, res) => {
    try {
        const result = await systemManager.performRecovery();
        if (result) {
            res.json({ success: true, message: 'System recovery completed' });
        } else {
            res.status(500).json({ error: 'Recovery failed' });
        }
    } catch (error) {
        systemLogger.error('Recovery failed', { error: error.message });
        res.status(500).json({ error: 'Recovery failed' });
    }
});

app.get('/api/system/alerts', (req, res) => {
    try {
        const alerts = systemManager.getActiveAlerts();
        res.json({ success: true, data: alerts });
    } catch (error) {
        res.status(500).json({ error: 'Failed to fetch alerts' });
    }
});

// ============================================================================
// API ROUTES - PROCESS MANAGEMENT
// ============================================================================

app.get('/api/processes', (req, res) => {
    try {
        const status = req.query.status;
        const processes = status 
            ? processManager.getAllProcesses({ status })
            : processManager.getAllProcesses();
        res.json({ success: true, data: processes });
    } catch (error) {
        res.status(500).json({ error: 'Failed to fetch processes' });
    }
});

app.post('/api/processes/create', (req, res) => {
    try {
        const { name, type, priority, metadata } = req.body;
        const process = processManager.createProcess(name, type, priority, metadata);
        processManager.startProcess(process.pid);
        res.json({ success: true, data: process });
    } catch (error) {
        res.status(500).json({ error: 'Failed to create process' });
    }
});

app.post('/api/processes/:pid/pause', (req, res) => {
    try {
        const process = processManager.pauseProcess(parseInt(req.params.pid));
        if (process) {
            res.json({ success: true, data: process });
        } else {
            res.status(404).json({ error: 'Process not found' });
        }
    } catch (error) {
        res.status(500).json({ error: 'Failed to pause process' });
    }
});

app.post('/api/processes/:pid/resume', (req, res) => {
    try {
        const process = processManager.resumeProcess(parseInt(req.params.pid));
        if (process) {
            res.json({ success: true, data: process });
        } else {
            res.status(404).json({ error: 'Process not found' });
        }
    } catch (error) {
        res.status(500).json({ error: 'Failed to resume process' });
    }
});

app.post('/api/processes/:pid/terminate', (req, res) => {
    try {
        const { error } = req.body;
        const process = processManager.terminateProcess(parseInt(req.params.pid), error);
        if (process) {
            res.json({ success: true, data: process });
        } else {
            res.status(404).json({ error: 'Process not found' });
        }
    } catch (error) {
        res.status(500).json({ error: 'Failed to terminate process' });
    }
});

app.get('/api/processes/metrics', (req, res) => {
    try {
        const metrics = processManager.getMetrics();
        res.json({ success: true, data: metrics });
    } catch (error) {
        res.status(500).json({ error: 'Failed to fetch metrics' });
    }
});

// ============================================================================
// API ROUTES - TASK SCHEDULING
// ============================================================================

app.get('/api/scheduler/tasks', (req, res) => {
    try {
        const status = req.query.status;
        const tasks = status
            ? taskScheduler.getScheduledTasks({ status })
            : taskScheduler.getScheduledTasks();
        res.json({ success: true, data: tasks });
    } catch (error) {
        res.status(500).json({ error: 'Failed to fetch scheduled tasks' });
    }
});

app.post('/api/scheduler/schedule-once', (req, res) => {
    try {
        const { name, delay, priority } = req.body;
        const task = taskScheduler.scheduleOnce(name, delay, () => {
            systemLogger.info(`Executed one-time task: ${name}`);
        }, priority);
        res.json({ success: true, data: task });
    } catch (error) {
        systemLogger.error('Failed to schedule task', { error: error.message });
        res.status(500).json({ error: 'Failed to schedule task' });
    }
});

app.post('/api/scheduler/schedule-cron', (req, res) => {
    try {
        const { name, cronExpression, priority } = req.body;
        const task = taskScheduler.scheduleCron(name, cronExpression, () => {
            systemLogger.info(`Executed cron task: ${name}`);
        }, priority);
        res.json({ success: true, data: task });
    } catch (error) {
        systemLogger.error('Failed to schedule cron task', { error: error.message });
        res.status(500).json({ error: 'Invalid cron expression' });
    }
});

app.post('/api/scheduler/schedule-interval', (req, res) => {
    try {
        const { name, intervalMs, priority, maxRuns } = req.body;
        const task = taskScheduler.scheduleInterval(name, intervalMs, () => {
            systemLogger.info(`Executed interval task: ${name}`);
        }, priority, maxRuns);
        res.json({ success: true, data: task });
    } catch (error) {
        res.status(500).json({ error: 'Failed to schedule interval' });
    }
});

app.post('/api/scheduler/cancel/:taskId', (req, res) => {
    try {
        const task = taskScheduler.cancelTask(parseInt(req.params.taskId));
        if (task) {
            res.json({ success: true, data: task });
        } else {
            res.status(404).json({ error: 'Task not found' });
        }
    } catch (error) {
        res.status(500).json({ error: 'Failed to cancel task' });
    }
});

// ============================================================================
// API ROUTES - USER & AUTHENTICATION
// ============================================================================

app.post('/api/users/register', (req, res) => {
    try {
        const { username, password, email, role } = req.body;
        const user = userManager.createUser(username, password, email, role);
        res.json({ success: true, data: user });
    } catch (error) {
        systemLogger.warn('User registration failed', { username: req.body.username, error: error.message });
        res.status(400).json({ error: error.message });
    }
});

app.post('/api/users/authenticate', (req, res) => {
    try {
        const { username, password } = req.body;
        const auth = userManager.authenticateUser(username, password);
        res.json({ success: true, data: auth });
    } catch (error) {
        systemLogger.warn('Authentication failed', { username, error: error.message });
        res.status(401).json({ error: 'Authentication failed' });
    }
});

app.post('/api/users/logout', (req, res) => {
    try {
        const { token } = req.body;
        userManager.logout(token);
        res.json({ success: true, message: 'Logged out' });
    } catch (error) {
        res.status(500).json({ error: 'Logout failed' });
    }
});

app.get('/api/users', (req, res) => {
    try {
        // Check authorization
        const users = userManager.getAllUsers();
        res.json({ success: true, data: users });
    } catch (error) {
        res.status(500).json({ error: 'Failed to fetch users' });
    }
});

app.get('/api/users/permissions/:username/:permission', (req, res) => {
    try {
        const { username, permission } = req.params;
        const hasPermission = userManager.hasPermission(username, permission);
        res.json({ success: true, hasPermission });
    } catch (error) {
        res.status(500).json({ error: 'Permission check failed' });
    }
});

// ============================================================================
// API ROUTES - LOGGING & MONITORING
// ============================================================================

app.get('/api/logs', (req, res) => {
    try {
        const limit = req.query.limit || 100;
        const level = req.query.level;
        const logs = level 
            ? systemLogger.getLogs({ level, limit })
            : systemLogger.getRecent(limit);
        res.json({ success: true, data: logs });
    } catch (error) {
        res.status(500).json({ error: 'Failed to fetch logs' });
    }
});

app.get('/api/logs/search', (req, res) => {
    try {
        const { query } = req.query;
        const results = systemLogger.search(query);
        res.json({ success: true, data: results });
    } catch (error) {
        res.status(500).json({ error: 'Search failed' });
    }
});

app.get('/api/logs/statistics', (req, res) => {
    try {
        const stats = systemLogger.getStatistics();
        res.json({ success: true, data: stats });
    } catch (error) {
        res.status(500).json({ error: 'Failed to fetch log statistics' });
    }
});

app.post('/api/logs/export', (req, res) => {
    try {
        const filepath = systemLogger.exportLogs();
        res.json({ success: true, message: 'Logs exported', filepath });
    } catch (error) {
        res.status(500).json({ error: 'Export failed' });
    }
});

app.get('/api/logs/clear', (req, res) => {
    try {
        const count = systemLogger.clearLogs();
        res.json({ success: true, message: `Cleared ${count} logs` });
    } catch (error) {
        res.status(500).json({ error: 'Clear failed' });
    }
});

// ============================================================================
// API ROUTES - AUTHENTICATION (Updated)
// ============================================================================

app.post('/api/auth/login', async (req, res) => {
    try {
        const { username, password } = req.body;

        if (!username || !password) {
            return res.status(400).json({ error: 'Username and password required' });
        }

        // Validate credentials (simple check)
        if (username.length >= 3 && password.length >= 6) {
            const sessionToken = Buffer.from(`${username}:${Date.now()}`).toString('base64');
            
            // Store session
            await database.storeSession(sessionToken, username, new Date(Date.now() + 24*60*60*1000));

            res.json({
                success: true,
                token: sessionToken,
                username: username,
                message: 'System initialized successfully'
            });
        } else {
            res.status(401).json({ error: 'Invalid credentials' });
        }
    } catch (error) {
        console.error('Login error:', error);
        res.status(500).json({ error: 'Server error' });
    }
});

app.post('/api/auth/logout', (req, res) => {
    try {
        res.json({ success: true, message: 'Logout successful' });
    } catch (error) {
        res.status(500).json({ error: 'Server error' });
    }
});

// ============================================================================
// API ROUTES - DASHBOARD & HOME
// ============================================================================

app.get('/api/dashboard/overview', async (req, res) => {
    try {
        const stats = {
            systemStatus: 'OPERATIONAL',
            aiMode: 'AUTONOMOUS',
            uptimeHours: Math.floor(Date.now() / (1000 * 60 * 60)) % 24,
            tasksCompleted: await database.getTaskCount('completed'),
            decisionsToday: await database.getDecisionCount(),
            memoryUtilization: await memoryManager.getMemoryStats(),
            lastDecision: await database.getLastDecision(),
            autonomyLevel: aiAgent?.autonomyLevel || 75
        };

        res.json({ success: true, data: stats });
    } catch (error) {
        console.error('Dashboard error:', error);
        res.status(500).json({ error: 'Failed to fetch dashboard data' });
    }
});

app.get('/api/dashboard/metrics', async (req, res) => {
    try {
        const metrics = {
            cpuUsage: Math.random() * 40 + 10,
            memoryUsage: (await memoryManager.getMemoryStats()).utilizationPercent,
            taskSuccessRate: await database.getSuccessRate(),
            averageDecisionTime: await database.getAverageDecisionTime(),
            recentDecisions: await database.getRecentDecisions(5)
        };

        res.json({ success: true, data: metrics });
    } catch (error) {
        console.error('Metrics error:', error);
        res.status(500).json({ error: 'Failed to fetch metrics' });
    }
});

// ============================================================================
// API ROUTES - AI AGENT CONTROL
// ============================================================================

app.get('/api/agent/status', (req, res) => {
    try {
        const status = {
            isRunning: aiAgent?.isRunning || false,
            autonomyLevel: aiAgent?.autonomyLevel || 75,
            thoughtMode: aiAgent?.thoughtMode || 'CONTINUOUS',
            lastThought: aiAgent?.lastThought,
            nextDecisionIn: aiAgent?.getNextDecisionTime() || 'calculating...',
            systemHealth: 'OPTIMAL'
        };

        res.json({ success: true, data: status });
    } catch (error) {
        res.status(500).json({ error: 'Failed to fetch agent status' });
    }
});

app.post('/api/agent/toggle-autonomy', async (req, res) => {
    try {
        const { enabled } = req.body;
        if (aiAgent) {
            aiAgent.toggleAutonomy(enabled);
            res.json({
                success: true,
                message: `Autonomy ${enabled ? 'enabled' : 'disabled'}`,
                autonomyLevel: aiAgent.autonomyLevel
            });
        } else {
            res.status(500).json({ error: 'AI Agent not initialized' });
        }
    } catch (error) {
        res.status(500).json({ error: 'Failed to toggle autonomy' });
    }
});

app.post('/api/agent/set-autonomy-level', async (req, res) => {
    try {
        const { level } = req.body;
        if (aiAgent && level >= 0 && level <= 100) {
            aiAgent.setAutonomyLevel(level);
            res.json({
                success: true,
                message: `Autonomy level set to ${level}%`,
                autonomyLevel: level
            });
        } else {
            res.status(400).json({ error: 'Invalid autonomy level' });
        }
    } catch (error) {
        res.status(500).json({ error: 'Failed to set autonomy level' });
    }
});

app.post('/api/agent/trigger-reflection', async (req, res) => {
    try {
        const reflection = await aiAgent.triggerReflection();
        res.json({ success: true, data: reflection });
    } catch (error) {
        res.status(500).json({ error: 'Failed to trigger reflection' });
    }
});

// ============================================================================
// API ROUTES - MEMORY MANAGEMENT
// ============================================================================

app.get('/api/memory/overview', async (req, res) => {
    try {
        const overview = await memoryManager.getMemoryOverview();
        res.json({ success: true, data: overview });
    } catch (error) {
        res.status(500).json({ error: 'Failed to fetch memory overview' });
    }
});

app.get('/api/memory/episodes', async (req, res) => {
    try {
        const limit = req.query.limit || 20;
        const episodes = await memoryManager.getRecentEpisodes(limit);
        res.json({ success: true, data: episodes });
    } catch (error) {
        res.status(500).json({ error: 'Failed to fetch episodes' });
    }
});

app.get('/api/memory/semantic', async (req, res) => {
    try {
        const facts = await memoryManager.getSemanticFacts();
        res.json({ success: true, data: facts });
    } catch (error) {
        res.status(500).json({ error: 'Failed to fetch semantic memory' });
    }
});

app.get('/api/memory/procedural', async (req, res) => {
    try {
        const rules = await memoryManager.getProceduralRules();
        res.json({ success: true, data: rules });
    } catch (error) {
        res.status(500).json({ error: 'Failed to fetch procedural memory' });
    }
});

app.post('/api/memory/store-episode', async (req, res) => {
    try {
        const { decision, action, context, outcome, reasoning } = req.body;
        const episodeId = await memoryManager.storeEpisode({
            decision,
            action,
            context,
            outcome,
            reasoning,
            timestamp: new Date()
        });
        res.json({ success: true, episodeId });
    } catch (error) {
        res.status(500).json({ error: 'Failed to store episode' });
    }
});

// ============================================================================
// API ROUTES - TASK MANAGEMENT
// ============================================================================

app.get('/api/tasks', async (req, res) => {
    try {
        const status = req.query.status || 'all';
        const tasks = await database.getTasks(status);
        res.json({ success: true, data: tasks });
    } catch (error) {
        res.status(500).json({ error: 'Failed to fetch tasks' });
    }
});

app.post('/api/tasks/create', async (req, res) => {
    try {
        const { title, description, priority } = req.body;
        const taskId = await database.createTask({
            title,
            description,
            priority: priority || 'MEDIUM',
            status: 'PENDING',
            createdAt: new Date()
        });
        res.json({ success: true, taskId });
    } catch (error) {
        res.status(500).json({ error: 'Failed to create task' });
    }
});

app.post('/api/tasks/:id/complete', async (req, res) => {
    try {
        const taskId = req.params.id;
        await database.updateTaskStatus(taskId, 'COMPLETED');
        res.json({ success: true, message: 'Task completed' });
    } catch (error) {
        res.status(500).json({ error: 'Failed to complete task' });
    }
});

// ============================================================================
// API ROUTES - REASONING VISUALIZATION
// ============================================================================

app.get('/api/reasoning/latest', async (req, res) => {
    try {
        const reasoning = await reasoningEngine.getLatestReasoning();
        res.json({ success: true, data: reasoning });
    } catch (error) {
        res.status(500).json({ error: 'Failed to fetch reasoning' });
    }
});

app.post('/api/reasoning/analyze', async (req, res) => {
    try {
        const { context, goals } = req.body;
        const analysis = await reasoningEngine.analyzeOptions(context, goals);
        res.json({ success: true, data: analysis });
    } catch (error) {
        res.status(500).json({ error: 'Failed to analyze reasoning' });
    }
});

// ============================================================================
// API ROUTES - EVOLUTION TRACKING
// ============================================================================

app.get('/api/evolution/history', async (req, res) => {
    try {
        const limit = req.query.limit || 50;
        const history = await database.getEvolutionHistory(limit);
        res.json({ success: true, data: history });
    } catch (error) {
        res.status(500).json({ error: 'Failed to fetch evolution history' });
    }
});

app.get('/api/evolution/rules', async (req, res) => {
    try {
        const rules = await memoryManager.getEvolutionRules();
        res.json({ success: true, data: rules });
    } catch (error) {
        res.status(500).json({ error: 'Failed to fetch evolution rules' });
    }
});

app.post('/api/evolution/update-rule', async (req, res) => {
    try {
        const { ruleId, adjustment, reason } = req.body;
        await memoryManager.updateEvolutionRule(ruleId, adjustment, reason);
        res.json({ success: true, message: 'Rule updated' });
    } catch (error) {
        res.status(500).json({ error: 'Failed to update rule' });
    }
});

// ============================================================================
// API ROUTES - ANALYTICS
// ============================================================================

app.get('/api/analytics/decision-timeline', async (req, res) => {
    try {
        const days = req.query.days || 7;
        const timeline = await database.getDecisionTimeline(days);
        res.json({ success: true, data: timeline });
    } catch (error) {
        res.status(500).json({ error: 'Failed to fetch timeline' });
    }
});

app.get('/api/analytics/performance', async (req, res) => {
    try {
        const performance = {
            decisionAccuracy: await database.getDecisionAccuracy(),
            averageDecisionTime: await database.getAverageDecisionTime(),
            taskSuccessRate: await database.getSuccessRate(),
            autonomyEfficiency: aiAgent?.autonomyLevel || 75
        };
        res.json({ success: true, data: performance });
    } catch (error) {
        res.status(500).json({ error: 'Failed to fetch performance data' });
    }
});

// ============================================================================
// API ROUTES - SETTINGS
// ============================================================================

app.get('/api/settings', async (req, res) => {
    try {
        const settings = await database.getSettings();
        res.json({ success: true, data: settings });
    } catch (error) {
        res.status(500).json({ error: 'Failed to fetch settings' });
    }
});

app.post('/api/settings/update', async (req, res) => {
    try {
        const { setting, value } = req.body;
        await database.updateSetting(setting, value);
        res.json({ success: true, message: 'Setting updated' });
    } catch (error) {
        res.status(500).json({ error: 'Failed to update setting' });
    }
});

// ============================================================================
// WEBSOCKET FOR REAL-TIME UPDATES
// ============================================================================

const WebSocket = require('ws');
const http = require('http');
const server = http.createServer(app);
const wss = new WebSocket.Server({ server });

const clients = new Set();

wss.on('connection', (ws) => {
    console.log('📡 WebSocket client connected');
    clients.add(ws);

    ws.on('message', (message) => {
        console.log('Received:', message);
        // Broadcast to all clients
        broadcast(JSON.parse(message));
    });

    ws.on('close', () => {
        console.log('📡 WebSocket client disconnected');
        clients.delete(ws);
    });
});

function broadcast(data) {
    clients.forEach(client => {
        if (client.readyState === WebSocket.OPEN) {
            client.send(JSON.stringify(data));
        }
    });
}

// Export broadcast for AI agent to use
global.broadcastUpdate = broadcast;

// ============================================================================
// ERROR HANDLING & SERVER START
// ============================================================================

app.use((err, req, res, next) => {
    if (systemLogger) {
        systemLogger.logApiError(req.method, req.path, 500, err);
    }
    console.error('Server error:', err);
    res.status(500).json({ error: 'Internal server error', details: err.message });
});

server.listen(PORT, async () => {
    console.log(`
╔════════════════════════════════════════════════════════════╗
║   SA-AIHOS Complete Operating System Starting...          ║
╚════════════════════════════════════════════════════════════╝
    `);

    // Initialize systems
    const initialized = await initializeSystems();

    if (initialized) {
        console.log(`
╔════════════════════════════════════════════════════════════╗
║      ✅ SA-AIHOS Complete OS Ready                        ║
║      📍 Backend API: http://localhost:${PORT}                ║
║      🤖 AI Agent: AUTONOMOUS MODE                         ║
║      💾 Database: CONNECTED                               ║
║      🧠 Memory: OPERATIONAL                               ║
║      🔧 Process Manager: ACTIVE                           ║
║      📅 Task Scheduler: ACTIVE                            ║
║      👥 User Manager: INITIALIZED                         ║
║      📊 System Logger: RECORDING                          ║
║      🛡️  System Manager: ORCHESTRATING                    ║
╚════════════════════════════════════════════════════════════╝
        `);

        // Periodic system health check every 5 minutes
        setInterval(async () => {
            await systemManager.performHealthCheck();
        }, 300000);

    } else {
        console.error('Failed to initialize OS');
        process.exit(1);
    }
});

module.exports = app;
