/**
 * Database - SQLite database for persistent storage
 * Handles all data persistence operations
 */

const sqlite3 = require('sqlite3').verbose();
const path = require('path');
const fs = require('fs');

class Database {
    constructor() {
        const dbDir = path.join(__dirname, '../data');
        if (!fs.existsSync(dbDir)) {
            fs.mkdirSync(dbDir, { recursive: true });
        }
        this.dbPath = path.join(dbDir, 'aihos.db');
        this.db = null;
    }

    /**
     * Initialize database and create tables
     */
    async initialize() {
        return new Promise((resolve, reject) => {
            this.db = new sqlite3.Database(this.dbPath, async (err) => {
                if (err) {
                    reject(err);
                    return;
                }

                try {
                    await this.createTables();
                    console.log('✅ Database initialized');
                    resolve();
                } catch (error) {
                    reject(error);
                }
            });
        });
    }

    /**
     * Create all necessary tables
     */
    async createTables() {
        const queries = [
            // Episodic Memory Table
            `CREATE TABLE IF NOT EXISTS episodes (
                id TEXT PRIMARY KEY,
                decision TEXT,
                action TEXT,
                context TEXT,
                outcome TEXT,
                reasoning TEXT,
                reflection TEXT,
                timestamp DATETIME,
                success BOOLEAN
            )`,

            // Semantic Memory Table
            `CREATE TABLE IF NOT EXISTS semantic_facts (
                id TEXT PRIMARY KEY,
                fact TEXT,
                confidence REAL,
                sources TEXT,
                createdAt DATETIME,
                updatedAt DATETIME,
                accessCount INTEGER
            )`,

            // Procedural Memory Table (Behavioral Rules)
            `CREATE TABLE IF NOT EXISTS procedural_rules (
                id TEXT PRIMARY KEY,
                condition TEXT,
                action TEXT,
                weight REAL,
                successRate REAL,
                description TEXT,
                createdAt DATETIME,
                evolvedAt DATETIME,
                executionCount INTEGER,
                successCount INTEGER
            )`,

            // Tasks Table
            `CREATE TABLE IF NOT EXISTS tasks (
                id TEXT PRIMARY KEY,
                title TEXT,
                description TEXT,
                priority TEXT,
                status TEXT,
                createdAt DATETIME,
                completedAt DATETIME
            )`,

            // Decisions Table
            `CREATE TABLE IF NOT EXISTS decisions (
                id TEXT PRIMARY KEY,
                action TEXT,
                context TEXT,
                success BOOLEAN,
                timestamp DATETIME,
                duration INTEGER
            )`,

            // Reflections Table
            `CREATE TABLE IF NOT EXISTS reflections (
                id TEXT PRIMARY KEY,
                decisionId TEXT,
                outcome TEXT,
                analysis TEXT,
                timestamp DATETIME
            )`,

            // Evolution Events Table
            `CREATE TABLE IF NOT EXISTS evolution_events (
                id TEXT PRIMARY KEY,
                timestamp DATETIME,
                type TEXT,
                decision TEXT,
                success BOOLEAN,
                rulesAffected INTEGER,
                details TEXT
            )`,

            // Sessions Table
            `CREATE TABLE IF NOT EXISTS sessions (
                token TEXT PRIMARY KEY,
                username TEXT,
                createdAt DATETIME,
                expiresAt DATETIME
            )`,

            // Settings Table
            `CREATE TABLE IF NOT EXISTS settings (
                key TEXT PRIMARY KEY,
                value TEXT,
                updatedAt DATETIME
            )`
        ];

        for (const query of queries) {
            await this.run(query);
        }

        console.log('✅ All tables created');
    }

    /**
     * Execute a database query
     */
    async run(sql, params = []) {
        return new Promise((resolve, reject) => {
            this.db.run(sql, params, function(err) {
                if (err) reject(err);
                else resolve({ id: this.lastID, changes: this.changes });
            });
        });
    }

    /**
     * Get a single row
     */
    async get(sql, params = []) {
        return new Promise((resolve, reject) => {
            this.db.get(sql, params, (err, row) => {
                if (err) reject(err);
                else resolve(row);
            });
        });
    }

    /**
     * Get multiple rows
     */
    async all(sql, params = []) {
        return new Promise((resolve, reject) => {
            this.db.all(sql, params, (err, rows) => {
                if (err) reject(err);
                else resolve(rows || []);
            });
        });
    }

    // ========================================================================
    // EPISODE OPERATIONS
    // ========================================================================

    async storeEpisode(episode) {
        const query = `
            INSERT INTO episodes (id, decision, action, context, outcome, reasoning, reflection, timestamp, success)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        `;
        return await this.run(query, [
            episode.id,
            episode.decision,
            episode.action,
            JSON.stringify(episode.context),
            episode.outcome,
            episode.reasoning,
            episode.reflection,
            episode.timestamp.toISOString(),
            episode.success ? 1 : 0
        ]);
    }

    async getAllEpisodes() {
        const rows = await this.all('SELECT * FROM episodes ORDER BY timestamp DESC LIMIT 500');
        return rows.map(row => ({
            ...row,
            context: JSON.parse(row.context || '{}'),
            success: !!row.success
        }));
    }

    async getRecentEpisodes(limit = 10) {
        const rows = await this.all(
            'SELECT * FROM episodes ORDER BY timestamp DESC LIMIT ?',
            [limit]
        );
        return rows.map(row => ({
            ...row,
            context: JSON.parse(row.context || '{}'),
            success: !!row.success
        }));
    }

    // ========================================================================
    // SEMANTIC MEMORY OPERATIONS
    // ========================================================================

    async storeSemanticFact(fact) {
        const query = `
            INSERT OR REPLACE INTO semantic_facts (id, fact, confidence, sources, createdAt, updatedAt, accessCount)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        `;
        return await this.run(query, [
            fact.id,
            fact.fact,
            fact.confidence,
            JSON.stringify(fact.sources),
            fact.createdAt.toISOString(),
            fact.updatedAt.toISOString(),
            fact.accessCount || 0
        ]);
    }

    async getAllSemanticFacts() {
        const rows = await this.all('SELECT * FROM semantic_facts');
        return rows.map(row => ({
            ...row,
            sources: JSON.parse(row.sources || '[]')
        }));
    }

    async updateSemanticFact(fact) {
        const query = `
            UPDATE semantic_facts
            SET confidence = ?, updatedAt = ?, accessCount = accessCount + 1
            WHERE id = ?
        `;
        return await this.run(query, [
            fact.confidence,
            fact.updatedAt.toISOString(),
            fact.id
        ]);
    }

    // ========================================================================
    // PROCEDURAL MEMORY OPERATIONS
    // ========================================================================

    async storeProceduralRule(rule) {
        const query = `
            INSERT OR REPLACE INTO procedural_rules 
            (id, condition, action, weight, successRate, description, createdAt, evolvedAt, executionCount, successCount)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        `;
        return await this.run(query, [
            rule.id,
            rule.condition,
            rule.action,
            rule.weight,
            rule.successRate || 0.5,
            rule.description,
            rule.createdAt.toISOString(),
            rule.evolvedAt.toISOString(),
            rule.executionCount || 0,
            rule.successCount || 0
        ]);
    }

    async getAllProceduralRules() {
        return await this.all('SELECT * FROM procedural_rules');
    }

    async updateProceduralRule(rule) {
        const query = `
            UPDATE procedural_rules
            SET weight = ?, successRate = ?, evolvedAt = ?
            WHERE id = ?
        `;
        return await this.run(query, [
            rule.weight,
            rule.successRate,
            rule.evolvedAt.toISOString(),
            rule.id
        ]);
    }

    // ========================================================================
    // TASK OPERATIONS
    // ========================================================================

    async createTask(task) {
        const id = `task_${Date.now()}`;
        const query = `
            INSERT INTO tasks (id, title, description, priority, status, createdAt)
            VALUES (?, ?, ?, ?, ?, ?)
        `;
        await this.run(query, [
            id,
            task.title,
            task.description,
            task.priority,
            task.status,
            task.createdAt.toISOString()
        ]);
        return id;
    }

    async getTasks(status = 'all') {
        let query = 'SELECT * FROM tasks';
        const params = [];

        if (status !== 'all') {
            query += ' WHERE status = ?';
            params.push(status);
        }

        query += ' ORDER BY createdAt DESC';
        return await this.all(query, params);
    }

    async updateTaskStatus(taskId, status) {
        const query = 'UPDATE tasks SET status = ?, completedAt = ? WHERE id = ?';
        return await this.run(query, [
            status,
            status === 'COMPLETED' ? new Date().toISOString() : null,
            taskId
        ]);
    }

    async getTaskCount(status = 'all') {
        let query = 'SELECT COUNT(*) as count FROM tasks';
        const params = [];

        if (status !== 'all') {
            query += ' WHERE status = ?';
            params.push(status);
        }

        const result = await this.get(query, params);
        return result?.count || 0;
    }

    // ========================================================================
    // DECISION OPERATIONS
    // ========================================================================

    async storeDecision(decision) {
        const id = `decision_${Date.now()}`;
        const query = `
            INSERT INTO decisions (id, action, context, success, timestamp, duration)
            VALUES (?, ?, ?, ?, ?, ?)
        `;
        return await this.run(query, [
            id,
            decision.action,
            JSON.stringify(decision.context),
            decision.success ? 1 : 0,
            decision.timestamp.toISOString(),
            decision.duration || 0
        ]);
    }

    async getRecentDecisions(limit = 5) {
        const rows = await this.all(
            'SELECT * FROM decisions ORDER BY timestamp DESC LIMIT ?',
            [limit]
        );
        return rows.map(row => ({
            ...row,
            context: JSON.parse(row.context || '{}'),
            success: !!row.success
        }));
    }

    async getDecisionCount() {
        const today = new Date().toDateString();
        const query = `
            SELECT COUNT(*) as count FROM decisions
            WHERE DATE(timestamp) = DATE(?)
        `;
        const result = await this.get(query, [today]);
        return result?.count || 0;
    }

    async getLastDecision() {
        const row = await this.get('SELECT * FROM decisions ORDER BY timestamp DESC LIMIT 1');
        if (row) {
            return {
                ...row,
                context: JSON.parse(row.context || '{}'),
                success: !!row.success
            };
        }
        return null;
    }

    // ========================================================================
    // REFLECTION OPERATIONS
    // ========================================================================

    async storeReflection(reflection) {
        const id = `reflection_${Date.now()}`;
        const query = `
            INSERT INTO reflections (id, decisionId, outcome, analysis, timestamp)
            VALUES (?, ?, ?, ?, ?)
        `;
        return await this.run(query, [
            id,
            reflection.decisionId,
            reflection.outcome,
            JSON.stringify(reflection.analysis),
            reflection.timestamp.toISOString()
        ]);
    }

    // ========================================================================
    // EVOLUTION OPERATIONS
    // ========================================================================

    async storeEvolutionEvent(event) {
        const id = `evolution_${Date.now()}`;
        const query = `
            INSERT INTO evolution_events (id, timestamp, type, decision, success, rulesAffected, details)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        `;
        return await this.run(query, [
            id,
            event.timestamp.toISOString(),
            event.type,
            event.decision,
            event.success ? 1 : 0,
            event.rulesAffected || 0,
            JSON.stringify(event)
        ]);
    }

    async getEvolutionHistory(limit = 50) {
        const rows = await this.all(
            'SELECT * FROM evolution_events ORDER BY timestamp DESC LIMIT ?',
            [limit]
        );
        return rows.map(row => ({
            ...row,
            success: !!row.success,
            details: JSON.parse(row.details || '{}')
        }));
    }

    // ========================================================================
    // SESSION OPERATIONS
    // ========================================================================

    async storeSession(token, username, expiresAt) {
        const query = `
            INSERT INTO sessions (token, username, createdAt, expiresAt)
            VALUES (?, ?, ?, ?)
        `;
        return await this.run(query, [
            token,
            username,
            new Date().toISOString(),
            expiresAt.toISOString()
        ]);
    }

    // ========================================================================
    // SETTINGS OPERATIONS
    // ========================================================================

    async getSettings() {
        return await this.all('SELECT * FROM settings');
    }

    async getSetting(key) {
        const row = await this.get('SELECT * FROM settings WHERE key = ?', [key]);
        return row?.value;
    }

    async updateSetting(key, value) {
        const query = `
            INSERT OR REPLACE INTO settings (key, value, updatedAt)
            VALUES (?, ?, ?)
        `;
        return await this.run(query, [key, value, new Date().toISOString()]);
    }

    // ========================================================================
    // ANALYTICS OPERATIONS
    // ========================================================================

    async getSuccessRate() {
        const query = `
            SELECT 
                COUNT(*) as total,
                SUM(CASE WHEN success = 1 THEN 1 ELSE 0 END) as successful
            FROM decisions
        `;
        const result = await this.get(query);
        if (result?.total === 0) return 0;
        return ((result?.successful / result?.total) * 100).toFixed(1);
    }

    async getAverageDecisionTime() {
        const query = 'SELECT AVG(duration) as avg FROM decisions';
        const result = await this.get(query);
        return result?.avg || 0;
    }

    async getDecisionAccuracy() {
        const query = `
            SELECT 
                COUNT(*) as total,
                SUM(CASE WHEN success = 1 THEN 1 ELSE 0 END) as correct
            FROM decisions
            WHERE DATE(timestamp) = DATE('now')
        `;
        const result = await this.get(query);
        if (result?.total === 0) return 0;
        return ((result?.correct / result?.total) * 100).toFixed(1);
    }

    async getDecisionTimeline(days = 7) {
        const query = `
            SELECT 
                DATE(timestamp) as date,
                COUNT(*) as count,
                SUM(CASE WHEN success = 1 THEN 1 ELSE 0 END) as successful
            FROM decisions
            WHERE timestamp >= datetime('now', '-' || ? || ' days')
            GROUP BY DATE(timestamp)
            ORDER BY date ASC
        `;
        return await this.all(query, [days]);
    }

    // ========================================================================
    // MEMORY MANAGEMENT
    // ========================================================================

    async consolidateMemory(episodes, facts) {
        // Delete old episodes (keep only recent)
        await this.run('DELETE FROM episodes WHERE id NOT IN (SELECT id FROM episodes ORDER BY timestamp DESC LIMIT 500)');
        
        // Delete low-confidence facts
        await this.run('DELETE FROM semantic_facts WHERE confidence < 0.3');
        
        console.log('✅ Memory consolidation complete');
    }

    // ========================================================================
    // CLOSE DATABASE
    // ========================================================================

    close() {
        return new Promise((resolve, reject) => {
            if (this.db) {
                this.db.close((err) => {
                    if (err) reject(err);
                    else resolve();
                });
            } else {
                resolve();
            }
        });
    }
}

module.exports = Database;
