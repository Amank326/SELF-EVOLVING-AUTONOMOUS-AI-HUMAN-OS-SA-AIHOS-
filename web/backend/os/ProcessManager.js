/**
 * Process Manager - Core OS Process Management
 * Handles process creation, lifecycle, monitoring, and termination
 */

const EventEmitter = require('events');
const os = require('os');

class ProcessManager extends EventEmitter {
    constructor() {
        super();
        this.processes = new Map();
        this.processId = 1000;
        this.metrics = {
            totalCreated: 0,
            totalTerminated: 0,
            currentRunning: 0,
            cpuUsage: 0,
            memoryUsage: 0
        };
    }

    /**
     * Create a new process
     */
    createProcess(name, type = 'task', priority = 'normal', metadata = {}) {
        const pid = this.processId++;
        const process = {
            pid,
            name,
            type, // 'task', 'service', 'daemon', 'background'
            priority, // 'low', 'normal', 'high', 'critical'
            status: 'created', // created, pending, running, paused, completed, failed
            createdAt: new Date(),
            startedAt: null,
            completedAt: null,
            duration: 0,
            cpu: 0,
            memory: 0,
            result: null,
            error: null,
            progress: 0,
            dependencies: [],
            metadata
        };

        this.processes.set(pid, process);
        this.metrics.totalCreated++;
        this.emit('process-created', process);
        
        return process;
    }

    /**
     * Start a process
     */
    startProcess(pid) {
        const process = this.processes.get(pid);
        if (!process) return null;

        if (process.status === 'created' || process.status === 'paused') {
            process.status = 'running';
            process.startedAt = new Date();
            this.metrics.currentRunning++;
            this.emit('process-started', process);
        }
        return process;
    }

    /**
     * Complete a process
     */
    completeProcess(pid, result = null) {
        const process = this.processes.get(pid);
        if (!process) return null;

        process.status = 'completed';
        process.completedAt = new Date();
        process.duration = process.completedAt - process.startedAt;
        process.result = result;
        this.metrics.currentRunning = Math.max(0, this.metrics.currentRunning - 1);
        this.metrics.totalTerminated++;
        this.emit('process-completed', process);
        
        return process;
    }

    /**
     * Pause a process
     */
    pauseProcess(pid) {
        const process = this.processes.get(pid);
        if (!process || process.status !== 'running') return null;

        process.status = 'paused';
        this.metrics.currentRunning = Math.max(0, this.metrics.currentRunning - 1);
        this.emit('process-paused', process);
        
        return process;
    }

    /**
     * Resume a paused process
     */
    resumeProcess(pid) {
        const process = this.processes.get(pid);
        if (!process || process.status !== 'paused') return null;

        process.status = 'running';
        this.metrics.currentRunning++;
        this.emit('process-resumed', process);
        
        return process;
    }

    /**
     * Terminate a process with error
     */
    terminateProcess(pid, error = null) {
        const process = this.processes.get(pid);
        if (!process) return null;

        process.status = 'failed';
        process.error = error;
        process.completedAt = new Date();
        process.duration = process.completedAt - process.startedAt;
        this.metrics.currentRunning = Math.max(0, this.metrics.currentRunning - 1);
        this.metrics.totalTerminated++;
        this.emit('process-terminated', process);
        
        return process;
    }

    /**
     * Update process progress
     */
    updateProgress(pid, progress, metadata = {}) {
        const process = this.processes.get(pid);
        if (!process) return null;

        process.progress = Math.min(100, Math.max(0, progress));
        process.metadata = { ...process.metadata, ...metadata };
        this.emit('process-progress', process);
        
        return process;
    }

    /**
     * Get process by ID
     */
    getProcess(pid) {
        return this.processes.get(pid);
    }

    /**
     * Get all processes
     */
    getAllProcesses(filter = {}) {
        let results = Array.from(this.processes.values());

        if (filter.status) {
            results = results.filter(p => p.status === filter.status);
        }
        if (filter.type) {
            results = results.filter(p => p.type === filter.type);
        }
        if (filter.priority) {
            results = results.filter(p => p.priority === filter.priority);
        }

        return results;
    }

    /**
     * Get running processes
     */
    getRunningProcesses() {
        return this.getAllProcesses({ status: 'running' });
    }

    /**
     * Get system metrics
     */
    getMetrics() {
        const runningProcs = this.getRunningProcesses();
        const totalCpu = runningProcs.reduce((sum, p) => sum + (p.cpu || 0), 0);
        const totalMemory = runningProcs.reduce((sum, p) => sum + (p.memory || 0), 0);

        return {
            ...this.metrics,
            systemCpuUsage: os.loadavg()[0].toFixed(2),
            systemMemoryUsage: ((1 - (os.freemem() / os.totalmem())) * 100).toFixed(2),
            totalCpuUsage: totalCpu.toFixed(2),
            totalMemoryUsage: totalMemory.toFixed(2),
            timestamp: new Date()
        };
    }

    /**
     * Kill zombie processes (cleanup)
     */
    cleanupZombieProcesses() {
        const completedProcs = this.getAllProcesses({ status: 'completed' });
        const failedProcs = this.getAllProcesses({ status: 'failed' });

        const allZombie = [...completedProcs, ...failedProcs];
        const oneHourAgo = new Date(Date.now() - 60*60*1000);

        allZombie.forEach(proc => {
            if (proc.completedAt < oneHourAgo) {
                this.processes.delete(proc.pid);
            }
        });

        return allZombie.length;
    }

    /**
     * Statistics
     */
    getStatistics() {
        const allProcs = Array.from(this.processes.values());
        const completed = allProcs.filter(p => p.status === 'completed');
        const failed = allProcs.filter(p => p.status === 'failed');
        const running = allProcs.filter(p => p.status === 'running');

        const avgDuration = completed.length > 0
            ? completed.reduce((sum, p) => sum + p.duration, 0) / completed.length
            : 0;

        const successRate = (allProcs.length > 0)
            ? ((completed.length / (completed.length + failed.length)) * 100).toFixed(2)
            : 0;

        return {
            totalProcesses: allProcs.length,
            running: running.length,
            completed: completed.length,
            failed: failed.length,
            successRate: `${successRate}%`,
            averageDuration: avgDuration.toFixed(2),
            uptime: new Date()
        };
    }
}

module.exports = ProcessManager;
