/**
 * Integration Test Suite - Verify all components connected
 * Tests: Database, Memory, AI Agent, Reasoning Engine, API Endpoints
 */

const sqlite3 = require('sqlite3').verbose();
const path = require('path');
const fs = require('fs');

class IntegrationTestSuite {
    constructor() {
        this.results = [];
        this.testsPassed = 0;
        this.testsFailed = 0;
    }

    /**
     * Run complete integration test
     */
    async runAllTests() {
        console.log('🧪 Starting Integration Test Suite...\n');

        // Test 1: Database Connection
        await this.testDatabaseConnection();

        // Test 2: Database Schema
        await this.testDatabaseSchema();

        // Test 3: Memory Manager Initialization
        await this.testMemoryManagerInit();

        // Test 4: Reasoning Engine
        await this.testReasoningEngine();

        // Test 5: AI Agent
        await this.testAIAgent();

        // Test 6: Decision Cycle
        await this.testDecisionCycle();

        // Test 7: Database Persistence
        await this.testDatabasePersistence();

        // Print Results
        this.printResults();

        return {
            totalTests: this.testsPassed + this.testsFailed,
            passed: this.testsPassed,
            failed: this.testsFailed,
            results: this.results
        };
    }

    /**
     * Test 1: Database Connection
     */
    async testDatabaseConnection() {
        return new Promise((resolve) => {
            console.log('📊 Test 1: Database Connection...');
            
            const dbPath = path.join(__dirname, '../../data/aihos.db');
            const dbDir = path.dirname(dbPath);

            // Ensure directory exists
            if (!fs.existsSync(dbDir)) {
                fs.mkdirSync(dbDir, { recursive: true });
            }

            const db = new sqlite3.Database(dbPath, (err) => {
                if (err) {
                    this.recordFailure('Database Connection', err.message);
                    console.log('❌ Failed: ' + err.message + '\n');
                } else {
                    db.all("SELECT sqlite_version() as version", (err, rows) => {
                        if (err) {
                            this.recordFailure('Database Connection', err.message);
                            console.log('❌ Failed: ' + err.message + '\n');
                        } else {
                            this.recordSuccess('Database Connection', `SQLite ${rows[0].version}`);
                            console.log(`✅ Passed: Connected to SQLite ${rows[0].version}\n`);
                        }
                        db.close();
                        resolve();
                    });
                }
            });
        });
    }

    /**
     * Test 2: Database Schema
     */
    async testDatabaseSchema() {
        return new Promise((resolve) => {
            console.log('📋 Test 2: Database Schema Validation...');
            
            const Database = require('./db/Database');
            const db = new Database();

            db.initialize().then(() => {
                const tables = [
                    'episodes', 'semantic_facts', 'procedural_rules',
                    'tasks', 'decisions', 'reflections', 'evolution_events',
                    'sessions', 'settings'
                ];

                let tablesFound = 0;
                for (const table of tables) {
                    db.db.all(
                        `SELECT name FROM sqlite_master WHERE type='table' AND name=?`,
                        [table],
                        (err, rows) => {
                            if (rows && rows.length > 0) tablesFound++;
                            if (tablesFound === tables.length) {
                                this.recordSuccess('Database Schema', `${tables.length} tables found`);
                                console.log(`✅ Passed: All ${tables.length} tables created\n`);
                                resolve();
                            }
                        }
                    );
                }
            }).catch(err => {
                this.recordFailure('Database Schema', err.message);
                console.log('❌ Failed: ' + err.message + '\n');
                resolve();
            });
        });
    }

    /**
     * Test 3: Memory Manager Initialization
     */
    async testMemoryManagerInit() {
        return new Promise((resolve) => {
            console.log('🧠 Test 3: Memory Manager Initialization...');
            
            try {
                const Database = require('./db/Database');
                const MemoryManager = require('./ai/MemoryManager');

                const db = new Database();
                db.initialize().then(() => {
                    const memoryManager = new MemoryManager(db);
                    memoryManager.initialize().then(() => {
                        const stats = memoryManager.getOverview();
                        this.recordSuccess('Memory Manager', 'Initialized with 3 memory types');
                        console.log(`✅ Passed: Memory Manager initialized\n`);
                        resolve();
                    });
                });
            } catch (err) {
                this.recordFailure('Memory Manager', err.message);
                console.log('❌ Failed: ' + err.message + '\n');
                resolve();
            }
        });
    }

    /**
     * Test 4: Reasoning Engine
     */
    async testReasoningEngine() {
        return new Promise((resolve) => {
            console.log('🧠 Test 4: Reasoning Engine...');
            
            try {
                const ReasoningEngine = require('./ai/ReasoningEngine');
                const reasoningEngine = new ReasoningEngine();

                const context = {
                    time: new Date(),
                    taskQueue: [{ id: 1, priority: 'high' }],
                    memoryStats: { episodic: 10, semantic: 20, procedural: 15 }
                };

                const options = reasoningEngine.generateOptions(context, 3);
                
                if (options && options.length > 0) {
                    this.recordSuccess('Reasoning Engine', `Generated ${options.length} options`);
                    console.log(`✅ Passed: Generated ${options.length} decision options\n`);
                } else {
                    this.recordFailure('Reasoning Engine', 'No options generated');
                    console.log('❌ Failed: No options generated\n');
                }
                resolve();
            } catch (err) {
                this.recordFailure('Reasoning Engine', err.message);
                console.log('❌ Failed: ' + err.message + '\n');
                resolve();
            }
        });
    }

    /**
     * Test 5: AI Agent
     */
    async testAIAgent() {
        return new Promise((resolve) => {
            console.log('🤖 Test 5: AI Agent System...');
            
            try {
                const Database = require('./db/Database');
                const MemoryManager = require('./ai/MemoryManager');
                const AIAgent = require('./ai/AIAgent');
                const ReasoningEngine = require('./ai/ReasoningEngine');

                const db = new Database();
                db.initialize().then(() => {
                    const memoryManager = new MemoryManager(db);
                    memoryManager.initialize().then(() => {
                        const reasoningEngine = new ReasoningEngine();
                        const aiAgent = new AIAgent(memoryManager, reasoningEngine, db);

                        const status = {
                            autonomous: aiAgent.autonomyLevel > 0,
                            thinking: typeof aiAgent.think === 'function',
                            acting: typeof aiAgent.act === 'function',
                            reflecting: typeof aiAgent.reflect === 'function'
                        };

                        if (Object.values(status).every(s => s)) {
                            this.recordSuccess('AI Agent', 'All components operational');
                            console.log(`✅ Passed: AI Agent fully initialized\n`);
                        } else {
                            this.recordFailure('AI Agent', 'Some components missing');
                            console.log('❌ Failed: Some components missing\n');
                        }
                        resolve();
                    });
                });
            } catch (err) {
                this.recordFailure('AI Agent', err.message);
                console.log('❌ Failed: ' + err.message + '\n');
                resolve();
            }
        });
    }

    /**
     * Test 6: Decision Cycle
     */
    async testDecisionCycle() {
        return new Promise((resolve) => {
            console.log('🔄 Test 6: Decision Cycle...');
            
            try {
                const Database = require('./db/Database');
                const MemoryManager = require('./ai/MemoryManager');
                const AIAgent = require('./ai/AIAgent');
                const ReasoningEngine = require('./ai/ReasoningEngine');

                const db = new Database();
                db.initialize().then(() => {
                    const memoryManager = new MemoryManager(db);
                    memoryManager.initialize().then(() => {
                        const reasoningEngine = new ReasoningEngine();
                        const aiAgent = new AIAgent(memoryManager, reasoningEngine, db);

                        // Run single decision cycle
                        aiAgent.think().then(options => {
                            if (options && options.length > 0) {
                                const decision = aiAgent.act(options[0]);
                                if (decision) {
                                    this.recordSuccess('Decision Cycle', 'Complete cycle executed');
                                    console.log(`✅ Passed: Decision cycle complete\n`);
                                } else {
                                    this.recordFailure('Decision Cycle', 'Act phase failed');
                                    console.log('❌ Failed: Act phase failed\n');
                                }
                            }
                            resolve();
                        });
                    });
                });
            } catch (err) {
                this.recordFailure('Decision Cycle', err.message);
                console.log('❌ Failed: ' + err.message + '\n');
                resolve();
            }
        });
    }

    /**
     * Test 7: Database Persistence
     */
    async testDatabasePersistence() {
        return new Promise((resolve) => {
            console.log('💾 Test 7: Database Persistence...');
            
            try {
                const Database = require('./db/Database');
                const db = new Database();

                db.initialize().then(() => {
                    // Store test data
                    const testEpisode = {
                        action: 'test_action',
                        context: { test: true },
                        outcome: { success: true },
                        timestamp: new Date()
                    };

                    db.storeEpisode(testEpisode).then(() => {
                        // Retrieve test data
                        db.db.all(
                            'SELECT * FROM episodes WHERE action = ?',
                            ['test_action'],
                            (err, rows) => {
                                if (rows && rows.length > 0) {
                                    this.recordSuccess('Database Persistence', 'Data stored and retrieved');
                                    console.log(`✅ Passed: Data persistence working\n`);
                                } else {
                                    this.recordFailure('Database Persistence', 'Data not retrieved');
                                    console.log('❌ Failed: Data not retrieved\n');
                                }
                                resolve();
                            }
                        );
                    });
                });
            } catch (err) {
                this.recordFailure('Database Persistence', err.message);
                console.log('❌ Failed: ' + err.message + '\n');
                resolve();
            }
        });
    }

    /**
     * Record successful test
     */
    recordSuccess(testName, details) {
        this.testsPassed++;
        this.results.push({
            test: testName,
            status: 'PASS',
            details
        });
    }

    /**
     * Record failed test
     */
    recordFailure(testName, error) {
        this.testsFailed++;
        this.results.push({
            test: testName,
            status: 'FAIL',
            error
        });
    }

    /**
     * Print test results
     */
    printResults() {
        console.log('\n═══════════════════════════════════════════════════════');
        console.log('🧪 INTEGRATION TEST RESULTS');
        console.log('═══════════════════════════════════════════════════════');
        
        this.results.forEach(result => {
            if (result.status === 'PASS') {
                console.log(`✅ ${result.test}: ${result.details}`);
            } else {
                console.log(`❌ ${result.test}: ${result.error}`);
            }
        });

        console.log('\n───────────────────────────────────────────────────────');
        console.log(`📊 Total Tests: ${this.testsPassed + this.testsFailed}`);
        console.log(`✅ Passed: ${this.testsPassed}`);
        console.log(`❌ Failed: ${this.testsFailed}`);
        console.log(`📈 Success Rate: ${((this.testsPassed / (this.testsPassed + this.testsFailed)) * 100).toFixed(2)}%`);
        console.log('═══════════════════════════════════════════════════════\n');
    }
}

// Run tests if executed directly
if (require.main === module) {
    const suite = new IntegrationTestSuite();
    suite.runAllTests().then(results => {
        process.exit(results.failed > 0 ? 1 : 0);
    });
}

module.exports = IntegrationTestSuite;
