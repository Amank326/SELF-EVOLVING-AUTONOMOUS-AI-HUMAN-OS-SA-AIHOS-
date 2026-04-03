/**
 * Task Scheduler - OS Task Scheduling & Automation
 * Handles scheduled tasks, cron jobs, event triggers, and workflow automation
 */

const EventEmitter = require('events');
const cron = require('node-cron');

class TaskScheduler extends EventEmitter {
    constructor(processManager) {
        super();
        this.processManager = processManager;
        this.scheduledTasks = new Map();
        this.taskId = 5000;
        this.cronJobs = new Map();
    }

    /**
     * Schedule a one-time task
     */
    scheduleOnce(name, delay, callback, priority = 'normal') {
        const taskId = this.taskId++;
        
        const timeout = setTimeout(() => {
            const process = this.processManager.createProcess(name, 'task', priority);
            this.processManager.startProcess(process.pid);

            try {
                callback(process);
                this.processManager.completeProcess(process.pid);
            } catch (error) {
                this.processManager.terminateProcess(process.pid, error);
            }

            this.scheduledTasks.delete(taskId);
            this.emit('task-executed', { taskId, name, time: new Date() });
        }, delay);

        const task = {
            taskId,
            name,
            type: 'once',
            delay,
            createdAt: new Date(),
            timeout,
            status: 'scheduled'
        };

        this.scheduledTasks.set(taskId, task);
        return task;
    }

    /**
     * Schedule recurring task with cron expression
     */
    scheduleCron(name, cronExpression, callback, priority = 'normal', metadata = {}) {
        const taskId = this.taskId++;
        
        try {
            const job = cron.schedule(cronExpression, () => {
                const process = this.processManager.createProcess(name, 'scheduled-task', priority, metadata);
                this.processManager.startProcess(process.pid);

                try {
                    callback(process);
                    this.processManager.completeProcess(process.pid);
                } catch (error) {
                    this.processManager.terminateProcess(process.pid, error);
                }

                this.emit('cron-task-executed', { taskId, name, time: new Date() });
            });

            const task = {
                taskId,
                name,
                type: 'cron',
                cronExpression,
                createdAt: new Date(),
                lastRun: null,
                nextRun: new Date(),
                executions: 0,
                status: 'active',
                priority,
                metadata
            };

            this.scheduledTasks.set(taskId, task);
            this.cronJobs.set(taskId, job);
            
            return task;
        } catch (error) {
            throw new Error(`Invalid cron expression: ${cronExpression}`);
        }
    }

    /**
     * Schedule interval task
     */
    scheduleInterval(name, intervalMs, callback, priority = 'normal', maxRuns = null) {
        const taskId = this.taskId++;
        let executionCount = 0;
        
        const interval = setInterval(() => {
            if (maxRuns && executionCount >= maxRuns) {
                clearInterval(interval);
                this.scheduledTasks.delete(taskId);
                return;
            }

            const process = this.processManager.createProcess(name, 'interval-task', priority);
            this.processManager.startProcess(process.pid);

            try {
                callback(process);
                this.processManager.completeProcess(process.pid);
                executionCount++;
            } catch (error) {
                this.processManager.terminateProcess(process.pid, error);
            }

            this.emit('interval-task-executed', { taskId, name, count: executionCount });
        }, intervalMs);

        const task = {
            taskId,
            name,
            type: 'interval',
            intervalMs,
            maxRuns,
            executionCount: 0,
            createdAt: new Date(),
            interval,
            status: 'active'
        };

        this.scheduledTasks.set(taskId, task);
        return task;
    }

    /**
     * Get scheduled tasks
     */
    getScheduledTasks(filter = {}) {
        let tasks = Array.from(this.scheduledTasks.values());

        if (filter.type) {
            tasks = tasks.filter(t => t.type === filter.type);
        }
        if (filter.status) {
            tasks = tasks.filter(t => t.status === filter.status);
        }

        return tasks;
    }

    /**
     * Cancel a scheduled task
     */
    cancelTask(taskId) {
        const task = this.scheduledTasks.get(taskId);
        if (!task) return null;

        if (task.timeout) clearTimeout(task.timeout);
        if (task.interval) clearInterval(task.interval);
        
        const cronJob = this.cronJobs.get(taskId);
        if (cronJob) {
            cronJob.stop();
            this.cronJobs.delete(taskId);
        }

        task.status = 'cancelled';
        this.emit('task-cancelled', task);
        
        return task;
    }

    /**
     * Cancel all tasks
     */
    cancelAll() {
        const tasks = Array.from(this.scheduledTasks.values());
        tasks.forEach(task => this.cancelTask(task.taskId));
        return tasks.length;
    }

    /**
     * Setup common system tasks
     */
    setupSystemTasks() {
        // Cleanup zombie processes every hour
        this.scheduleCron('Cleanup Zombies', '0 * * * *', (proc) => {
            const cleaned = this.processManager.cleanupZombieProcesses();
            console.log(`🧹 Cleaned ${cleaned} zombie processes`);
        }, 'normal', { system: true });

        // System health check every 5 minutes
        this.scheduleCron('System Health Check', '*/5 * * * *', (proc) => {
            const metrics = this.processManager.getMetrics();
            console.log(`📊 System Health: CPU ${metrics.systemCpuUsage}%, Memory ${metrics.systemMemoryUsage}%`);
        }, 'high', { system: true });

        // Memory optimization every 30 minutes
        this.scheduleCron('Memory Optimization', '*/30 * * * *', (proc) => {
            console.log('🔧 Running memory optimization');
        }, 'normal', { system: true });

        console.log('✅ System tasks scheduled');
    }

    /**
     * Get task statistics
     */
    getStatistics() {
        const allTasks = Array.from(this.scheduledTasks.values());
        const activeTasks = allTasks.filter(t => t.status === 'active');
        const cancelledTasks = allTasks.filter(t => t.status === 'cancelled');

        return {
            total: allTasks.length,
            active: activeTasks.length,
            cancelled: cancelledTasks.length,
            byType: {
                once: allTasks.filter(t => t.type === 'once').length,
                cron: allTasks.filter(t => t.type === 'cron').length,
                interval: allTasks.filter(t => t.type === 'interval').length
            }
        };
    }
}

module.exports = TaskScheduler;
