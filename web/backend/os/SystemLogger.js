/**
 * System Logger - Comprehensive logging and monitoring system
 * Handles all system events, errors, and performance tracking
 */

const fs = require('fs');
const path = require('path');
const EventEmitter = require('events');

class SystemLogger extends EventEmitter {
    constructor(logDir = './logs') {
        super();
        this.logDir = logDir;
        this.logs = [];
        this.maxLogs = 10000;
        this.logLevels = {
            'DEBUG': 0,
            'INFO': 1,
            'WARN': 2,
            'ERROR': 3,
            'CRITICAL': 4
        };
        this.currentLevel = this.logLevels.INFO;
        
        // Ensure log directory exists
        if (!fs.existsSync(logDir)) {
            fs.mkdirSync(logDir, { recursive: true });
        }

        this.logFile = path.join(logDir, `system-${new Date().toISOString().split('T')[0]}.log`);
    }

    /**
     * Log a message
     */
    log(level, message, metadata = {}) {
        if (this.logLevels[level] < this.currentLevel) return;

        const logEntry = {
            timestamp: new Date(),
            level,
            message,
            metadata,
            id: this.logs.length
        };

        this.logs.push(logEntry);

        // Keep memory manageable
        if (this.logs.length > this.maxLogs) {
            this.logs.shift();
        }

        // Write to file
        this._writeToFile(logEntry);

        // Emit event
        this.emit(`log:${level.toLowerCase()}`, logEntry);
        this.emit('log', logEntry);

        // Console output with colors
        this._consoleOutput(logEntry);

        return logEntry;
    }

    debug(message, metadata) { return this.log('DEBUG', message, metadata); }
    info(message, metadata) { return this.log('INFO', message, metadata); }
    warn(message, metadata) { return this.log('WARN', message, metadata); }
    error(message, metadata) { return this.log('ERROR', message, metadata); }
    critical(message, metadata) { return this.log('CRITICAL', message, metadata); }

    /**
     * Log process event
     */
    logProcessEvent(process, eventType, details = {}) {
        return this.log('INFO', `Process ${eventType}`, {
            processId: process.pid,
            processName: process.name,
            eventType,
            ...details
        });
    }

    /**
     * Log API request
     */
    logApiRequest(method, path, status, duration, userId = null) {
        return this.log('INFO', `API Request: ${method} ${path}`, {
            method,
            path,
            status,
            duration: `${duration}ms`,
            userId,
            timestamp: new Date()
        });
    }

    /**
     * Log API error
     */
    logApiError(method, path, status, error, userId = null) {
        return this.log('ERROR', `API Error: ${method} ${path}`, {
            method,
            path,
            status,
            error: error.message,
            stack: error.stack,
            userId,
            timestamp: new Date()
        });
    }

    /**
     * Log database operation
     */
    logDatabaseOperation(operation, table, duration, success = true) {
        const level = success ? 'DEBUG' : 'ERROR';
        return this.log(level, `Database: ${operation} on ${table}`, {
            operation,
            table,
            duration: `${duration}ms`,
            success,
            timestamp: new Date()
        });
    }

    /**
     * Log AI decision
     */
    logAiDecision(agentName, decision, reasoning, confidence) {
        return this.log('INFO', `AI Decision: ${decision}`, {
            agent: agentName,
            decision,
            reasoning,
            confidence: confidence.toFixed(2),
            timestamp: new Date()
        });
    }

    /**
     * Write to file
     */
    _writeToFile(logEntry) {
        const logLine = `[${logEntry.timestamp.toISOString()}] ${logEntry.level}: ${logEntry.message} ${JSON.stringify(logEntry.metadata)}\n`;
        fs.appendFileSync(this.logFile, logLine);
    }

    /**
     * Console output with formatting
     */
    _consoleOutput(logEntry) {
        const colors = {
            DEBUG: '\x1b[36m',    // Cyan
            INFO: '\x1b[32m',     // Green
            WARN: '\x1b[33m',     // Yellow
            ERROR: '\x1b[31m',    // Red
            CRITICAL: '\x1b[41m'  // Red background
        };
        const reset = '\x1b[0m';
        const color = colors[logEntry.level] || reset;

        console.log(
            `${color}[${logEntry.timestamp.toISOString()}] ${logEntry.level}${reset}`,
            logEntry.message,
            logEntry.metadata
        );
    }

    /**
     * Get logs
     */
    getLogs(filter = {}) {
        let results = this.logs;

        if (filter.level) {
            results = results.filter(l => l.level === filter.level);
        }
        if (filter.since) {
            results = results.filter(l => l.timestamp >= filter.since);
        }
        if (filter.message) {
            results = results.filter(l => l.message.includes(filter.message));
        }

        if (filter.limit) {
            results = results.slice(-filter.limit);
        }

        return results;
    }

    /**
     * Get recent logs
     */
    getRecent(limit = 100) {
        return this.logs.slice(-limit);
    }

    /**
     * Search logs
     */
    search(query) {
        return this.logs.filter(l => 
            l.message.includes(query) || 
            JSON.stringify(l.metadata).includes(query)
        );
    }

    /**
     * Get statistics
     */
    getStatistics() {
        const stats = {
            total: this.logs.length,
            byLevel: {},
            oldestLog: this.logs.length > 0 ? this.logs[0].timestamp : null,
            newestLog: this.logs.length > 0 ? this.logs[this.logs.length - 1].timestamp : null
        };

        Object.keys(this.logLevels).forEach(level => {
            stats.byLevel[level] = this.logs.filter(l => l.level === level).length;
        });

        return stats;
    }

    /**
     * Clear logs
     */
    clearLogs() {
        const count = this.logs.length;
        this.logs = [];
        this.log('INFO', `Cleared ${count} log entries`);
        return count;
    }

    /**
     * Export logs to file
     */
    exportLogs(filename = null) {
        const exportFile = filename || path.join(this.logDir, `export-${new Date().toISOString().replace(/[:.]/g, '-')}.json`);
        fs.writeFileSync(exportFile, JSON.stringify(this.logs, null, 2));
        return exportFile;
    }

    /**
     * Set log level
     */
    setLevel(level) {
        if (!this.logLevels.hasOwnProperty(level)) {
            throw new Error(`Invalid log level: ${level}`);
        }
        this.currentLevel = this.logLevels[level];
        this.log('INFO', `Log level changed to ${level}`);
    }
}

module.exports = SystemLogger;
