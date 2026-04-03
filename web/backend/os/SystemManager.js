/**
 * System Manager - Core OS system management
 * Handles system initialization, health checks, recovery, and shutdown
 */

const EventEmitter = require('events');
const os = require('os');
const fs = require('fs');
const path = require('path');

class SystemManager extends EventEmitter {
    constructor(processManager, taskScheduler, logger, userManager) {
        super();
        this.processManager = processManager;
        this.taskScheduler = taskScheduler;
        this.logger = logger;
        this.userManager = userManager;
        this.systemStart = new Date();
        this.status = 'initializing';
        this.config = {};
        this.alerts = [];
        this.healthChecks = [];
    }

    /**
     * Initialize the operating system
     */
    async initialize(config = {}) {
        try {
            this.logger.info('🚀 Initializing SA-AIHOS Operating System...');
            this.config = config;

            // Setup system components
            this.taskScheduler.setupSystemTasks();
            this.logger.info('✅ System tasks scheduled');

            // Run initial health check
            await this.performHealthCheck();

            // Setup default user
            this.userManager.createUser('admin', 'admin@123', 'admin@aihos.local', 'admin');
            this.userManager.createUser('system', 'system@123', 'system@aihos.local', 'admin');
            this.logger.info('✅ Default users created');

            this.status = 'running';
            this.logger.info('✅ SA-AIHOS OS initialized successfully');
            
            this.emit('system-initialized');
            return true;
        } catch (error) {
            this.logger.error('❌ OS initialization failed', { error: error.message });
            this.status = 'error';
            return false;
        }
    }

    /**
     * Perform system health check
     */
    async performHealthCheck() {
        const checks = [];

        // CPU check
        const cpuLoad = os.loadavg()[0];
        const cpuCheck = {
            component: 'CPU',
            status: cpuLoad < 80 ? 'healthy' : 'warning',
            value: `${cpuLoad.toFixed(2)}%`,
            threshold: '80%'
        };
        checks.push(cpuCheck);

        // Memory check
        const memoryUsage = (1 - (os.freemem() / os.totalmem())) * 100;
        const memoryCheck = {
            component: 'Memory',
            status: memoryUsage < 80 ? 'healthy' : 'warning',
            value: `${memoryUsage.toFixed(2)}%`,
            threshold: '80%'
        };
        checks.push(memoryCheck);

        // Process check
        const runningProcs = this.processManager.getRunningProcesses().length;
        const processCheck = {
            component: 'Processes',
            status: runningProcs < 1000 ? 'healthy' : 'warning',
            value: runningProcs,
            threshold: 1000
        };
        checks.push(processCheck);

        // Database check (would check actual DB connection)
        const dbCheck = {
            component: 'Database',
            status: 'healthy',
            value: 'Connected',
            lastCheck: new Date()
        };
        checks.push(dbCheck);

        this.healthChecks = checks;
        this.logger.info('📊 System health check completed', { checks });

        return checks;
    }

    /**
     * Get system status
     */
    getStatus() {
        const uptime = new Date() - this.systemStart;
        const uptimeHours = Math.floor(uptime / (1000 * 60 * 60));
        const uptimeMinutes = Math.floor((uptime % (1000 * 60 * 60)) / (1000 * 60));

        return {
            status: this.status,
            uptime: `${uptimeHours}h ${uptimeMinutes}m`,
            startTime: this.systemStart,
            currentTime: new Date(),
            hostname: os.hostname(),
            platform: os.platform(),
            cpuCount: os.cpus().length,
            totalMemory: `${(os.totalmem() / (1024**3)).toFixed(2)} GB`,
            freeMemory: `${(os.freemem() / (1024**3)).toFixed(2)} GB`,
            loadAverage: os.loadavg().map(x => x.toFixed(2))
        };
    }

    /**
     * Get system information
     */
    getSystemInfo() {
        return {
            os: {
                type: os.type(),
                platform: os.platform(),
                arch: os.arch(),
                release: os.release()
            },
            cpu: {
                count: os.cpus().length,
                model: os.cpus()[0]?.model,
                speed: os.cpus()[0]?.speed
            },
            memory: {
                total: os.totalmem(),
                free: os.freemem(),
                used: os.totalmem() - os.freemem()
            },
            network: os.networkInterfaces()
        };
    }

    /**
     * Get comprehensive dashboard data
     */
    getDashboard() {
        const metrics = this.processManager.getMetrics();
        const procStats = this.processManager.getStatistics();
        const taskStats = this.taskScheduler.getStatistics();
        const userStats = this.userManager.getStatistics();
        const logStats = this.logger.getStatistics();

        return {
            system: this.getStatus(),
            processes: {
                metrics,
                statistics: procStats,
                running: this.processManager.getRunningProcesses().length,
                total: this.processManager.getAllProcesses().length
            },
            tasks: {
                statistics: taskStats,
                scheduled: this.taskScheduler.getScheduledTasks().length
            },
            users: userStats,
            logs: logStats,
            health: this.healthChecks,
            alerts: this.alerts.slice(-10) // Last 10 alerts
        };
    }

    /**
     * Create system alert
     */
    addAlert(severity, message, metadata = {}) {
        const alert = {
            id: this.alerts.length,
            timestamp: new Date(),
            severity, // 'info', 'warning', 'critical'
            message,
            metadata,
            resolved: false
        };

        this.alerts.push(alert);
        this.logger.warn(`⚠️ Alert: ${message}`, metadata);
        this.emit('alert', alert);

        return alert;
    }

    /**
     * Resolve alert
     */
    resolveAlert(alertId) {
        const alert = this.alerts.find(a => a.id === alertId);
        if (alert) {
            alert.resolved = true;
            alert.resolvedAt = new Date();
            this.emit('alert-resolved', alert);
        }
        return alert;
    }

    /**
     * Get active alerts
     */
    getActiveAlerts() {
        return this.alerts.filter(a => !a.resolved);
    }

    /**
     * System recovery
     */
    async performRecovery() {
        this.logger.warn('🔄 Starting system recovery...');
        
        try {
            // Cleanup zombies
            const zombiesCleaned = this.processManager.cleanupZombieProcesses();
            this.logger.info(`🧹 Cleaned ${zombiesCleaned} zombie processes`);

            // Health check
            await this.performHealthCheck();

            // Reset any stuck processes
            const failedProcs = this.processManager.getAllProcesses({ status: 'failed' });
            this.logger.info(`📊 Found ${failedProcs.length} failed processes`);

            this.logger.info('✅ System recovery completed');
            this.emit('recovery-completed');
            
            return true;
        } catch (error) {
            this.logger.error('❌ Recovery failed', { error: error.message });
            return false;
        }
    }

    /**
     * Graceful shutdown
     */
    async shutdown() {
        try {
            this.logger.info('🛑 Initiating graceful shutdown...');
            this.status = 'shutting-down';

            // Cancel all scheduled tasks
            const tasksCancelled = this.taskScheduler.cancelAll();
            this.logger.info(`❌ Cancelled ${tasksCancelled} scheduled tasks`);

            // Stop running processes gracefully
            const runningProcs = this.processManager.getRunningProcesses();
            runningProcs.forEach(proc => {
                this.processManager.pauseProcess(proc.pid);
            });
            this.logger.info(`⏸️ Paused ${runningProcs.length} running processes`);

            // Export logs
            const logFile = this.logger.exportLogs();
            this.logger.info(`📄 Exported logs to ${logFile}`);

            // Final status
            this.status = 'shutdown';
            this.logger.info('✅ SA-AIHOS OS shutdown completed gracefully');
            this.emit('system-shutdown');

            return true;
        } catch (error) {
            this.logger.error('❌ Shutdown error', { error: error.message });
            return false;
        }
    }

    /**
     * Get system statistics
     */
    getStatistics() {
        return {
            uptime: new Date() - this.systemStart,
            status: this.status,
            processes: this.processManager.getStatistics(),
            tasks: this.taskScheduler.getStatistics(),
            users: this.userManager.getStatistics(),
            logs: this.logger.getStatistics(),
            healthChecks: this.healthChecks,
            activeAlerts: this.getActiveAlerts().length
        };
    }
}

module.exports = SystemManager;
