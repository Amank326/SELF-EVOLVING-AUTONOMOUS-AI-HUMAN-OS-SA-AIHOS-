/**
 * SA-AIHOS Complete OS - System Verification Test
 * Tests all components and verifies complete functionality
 */

const http = require('http');

class OSVerificationTest {
    constructor(apiBase = 'http://localhost:3000') {
        this.apiBase = apiBase;
        this.results = {
            passed: 0,
            failed: 0,
            tests: []
        };
    }

    /**
     * Helper to make HTTP requests
     */
    request(method, endpoint, body = null) {
        return new Promise((resolve, reject) => {
            const url = new URL(endpoint, this.apiBase);
            const options = {
                hostname: url.hostname,
                port: url.port || 3000,
                path: url.pathname + url.search,
                method: method,
                headers: {
                    'Content-Type': 'application/json'
                }
            };

            const req = http.request(options, (res) => {
                let data = '';
                res.on('data', (chunk) => data += chunk);
                res.on('end', () => {
                    try {
                        resolve({
                            status: res.statusCode,
                            data: data ? JSON.parse(data) : null
                        });
                    } catch (e) {
                        resolve({
                            status: res.statusCode,
                            data: data
                        });
                    }
                });
            });

            req.on('error', reject);
            if (body) req.write(JSON.stringify(body));
            req.end();
        });
    }

    /**
     * Run a test
     */
    async test(name, fn) {
        try {
            await fn();
            this.results.passed++;
            this.results.tests.push({ name, status: '✅ PASS' });
            console.log(`✅ ${name}`);
        } catch (error) {
            this.results.failed++;
            this.results.tests.push({ name, status: '❌ FAIL', error: error.message });
            console.log(`❌ ${name}: ${error.message}`);
        }
    }

    /**
     * Assert condition
     */
    assert(condition, message) {
        if (!condition) throw new Error(message);
    }

    /**
     * Run all tests
     */
    async runAll() {
        console.log(`
╔════════════════════════════════════════════════════════════╗
║  SA-AIHOS Complete OS - System Verification Test Suite     ║
╚════════════════════════════════════════════════════════════╝
        `);

        await this.testSystemEndpoints();
        await this.testProcessManagement();
        await this.testTaskScheduling();
        await this.testUserManagement();
        await this.testLogging();
        await this.testHealthAndRecovery();

        this.printResults();
    }

    /**
     * Test system endpoints
     */
    async testSystemEndpoints() {
        console.log('\n📊 Testing System Endpoints...');

        await this.test('GET /api/system/status', async () => {
            const response = await this.request('GET', '/api/system/status');
            this.assert(response.status === 200, `Expected 200, got ${response.status}`);
            this.assert(response.data.success === true, 'Response should be successful');
            this.assert(response.data.data.status === 'running', 'System should be running');
        });

        await this.test('GET /api/system/info', async () => {
            const response = await this.request('GET', '/api/system/info');
            this.assert(response.status === 200, 'Status should be 200');
            this.assert(response.data.data.os, 'Should have OS info');
            this.assert(response.data.data.cpu, 'Should have CPU info');
            this.assert(response.data.data.memory, 'Should have memory info');
        });

        await this.test('GET /api/system/dashboard', async () => {
            const response = await this.request('GET', '/api/system/dashboard');
            this.assert(response.status === 200, 'Status should be 200');
            this.assert(response.data.data.system, 'Should have system data');
            this.assert(response.data.data.processes, 'Should have processes data');
            this.assert(response.data.data.health, 'Should have health data');
        });

        await this.test('GET /api/system/alerts', async () => {
            const response = await this.request('GET', '/api/system/alerts');
            this.assert(response.status === 200, 'Status should be 200');
            this.assert(Array.isArray(response.data.data), 'Should return array');
        });
    }

    /**
     * Test process management
     */
    async testProcessManagement() {
        console.log('\n🔧 Testing Process Management...');

        let processId = null;

        await this.test('POST /api/processes/create', async () => {
            const response = await this.request('POST', '/api/processes/create', {
                name: 'TestProcess',
                type: 'task',
                priority: 'normal'
            });
            this.assert(response.status === 200, 'Status should be 200');
            this.assert(response.data.success === true, 'Should be successful');
            this.assert(response.data.data.pid, 'Should return process ID');
            processId = response.data.data.pid;
        });

        await this.test('GET /api/processes', async () => {
            const response = await this.request('GET', '/api/processes');
            this.assert(response.status === 200, 'Status should be 200');
            this.assert(Array.isArray(response.data.data), 'Should return array');
            this.assert(response.data.data.length > 0, 'Should have processes');
        });

        await this.test('GET /api/processes/metrics', async () => {
            const response = await this.request('GET', '/api/processes/metrics');
            this.assert(response.status === 200, 'Status should be 200');
            this.assert(response.data.data.currentRunning >= 0, 'Should have metrics');
        });

        if (processId) {
            await this.test(`POST /api/processes/${processId}/pause`, async () => {
                const response = await this.request('POST', `/api/processes/${processId}/pause`);
                this.assert(response.status === 200, 'Status should be 200');
                this.assert(response.data.data.status === 'paused', 'Process should be paused');
            });

            await this.test(`POST /api/processes/${processId}/resume`, async () => {
                const response = await this.request('POST', `/api/processes/${processId}/resume`);
                this.assert(response.status === 200, 'Status should be 200');
                this.assert(response.data.data.status === 'running', 'Process should be running');
            });
        }
    }

    /**
     * Test task scheduling
     */
    async testTaskScheduling() {
        console.log('\n📅 Testing Task Scheduling...');

        await this.test('GET /api/scheduler/tasks', async () => {
            const response = await this.request('GET', '/api/scheduler/tasks');
            this.assert(response.status === 200, 'Status should be 200');
            this.assert(Array.isArray(response.data.data), 'Should return array');
        });

        await this.test('POST /api/scheduler/schedule-cron', async () => {
            const response = await this.request('POST', '/api/scheduler/schedule-cron', {
                name: 'TestCronJob',
                cronExpression: '*/5 * * * *',
                priority: 'normal'
            });
            this.assert(response.status === 200, 'Status should be 200');
            this.assert(response.data.success === true, 'Should be successful');
            this.assert(response.data.data.taskId, 'Should return task ID');
        });

        await this.test('POST /api/scheduler/schedule-interval', async () => {
            const response = await this.request('POST', '/api/scheduler/schedule-interval', {
                name: 'TestIntervalTask',
                intervalMs: 10000,
                priority: 'normal'
            });
            this.assert(response.status === 200, 'Status should be 200');
            this.assert(response.data.data.taskId, 'Should return task ID');
        });
    }

    /**
     * Test user management
     */
    async testUserManagement() {
        console.log('\n👥 Testing User Management...');

        await this.test('POST /api/users/register', async () => {
            const response = await this.request('POST', '/api/users/register', {
                username: 'testuser',
                password: 'password123',
                email: 'test@aihos.local',
                role: 'user'
            });
            // Might fail if user exists, that's OK for testing
            this.assert(response.status === 200, 'Status should be 200');
        });

        await this.test('POST /api/users/authenticate', async () => {
            const response = await this.request('POST', '/api/users/authenticate', {
                username: 'admin',
                password: 'admin@123'
            });
            this.assert(response.status === 200, 'Status should be 200');
            this.assert(response.data.data.token, 'Should return token');
            this.assert(response.data.data.role === 'admin', 'Should have role');
        });

        await this.test('GET /api/users', async () => {
            const response = await this.request('GET', '/api/users');
            this.assert(response.status === 200, 'Status should be 200');
            this.assert(Array.isArray(response.data.data), 'Should return array of users');
            this.assert(response.data.data.length > 0, 'Should have users');
        });
    }

    /**
     * Test logging
     */
    async testLogging() {
        console.log('\n📋 Testing Logging System...');

        await this.test('GET /api/logs', async () => {
            const response = await this.request('GET', '/api/logs?limit=10');
            this.assert(response.status === 200, 'Status should be 200');
            this.assert(Array.isArray(response.data.data), 'Should return array');
        });

        await this.test('GET /api/logs/statistics', async () => {
            const response = await this.request('GET', '/api/logs/statistics');
            this.assert(response.status === 200, 'Status should be 200');
            this.assert(response.data.data.total >= 0, 'Should have log count');
            this.assert(response.data.data.byLevel, 'Should have level breakdown');
        });

        await this.test('GET /api/logs/search', async () => {
            const response = await this.request('GET', '/api/logs/search?query=error');
            this.assert(response.status === 200, 'Status should be 200');
            this.assert(Array.isArray(response.data.data), 'Should return array');
        });
    }

    /**
     * Test health and recovery
     */
    async testHealthAndRecovery() {
        console.log('\n💊 Testing Health & Recovery...');

        await this.test('GET /api/system/health', async () => {
            const response = await this.request('GET', '/api/system/health');
            this.assert(response.status === 200, 'Status should be 200');
            this.assert(Array.isArray(response.data.data), 'Should return array of checks');
            this.assert(response.data.data.length > 0, 'Should have health checks');
        });

        // Note: Be careful with recovery test - it will perform system recovery
        // Uncomment to test
        /*
        await this.test('POST /api/system/recovery', async () => {
            const response = await this.request('POST', '/api/system/recovery');
            this.assert(response.status === 200, 'Status should be 200');
            this.assert(response.data.success === true, 'Should be successful');
        });
        */
    }

    /**
     * Print test results
     */
    printResults() {
        console.log(`
╔════════════════════════════════════════════════════════════╗
║                    TEST RESULTS SUMMARY                    ║
╚════════════════════════════════════════════════════════════╝

✅ Passed: ${this.results.passed}
❌ Failed: ${this.results.failed}
📊 Total:  ${this.results.passed + this.results.failed}

Detailed Results:
${this.results.tests.map(t => `${t.status} - ${t.name}${t.error ? ' (' + t.error + ')' : ''}`).join('\n')}

${this.results.failed === 0 ? '🎉 All tests passed!' : '⚠️ Some tests failed'}
        `);
    }
}

// Run tests
const tester = new OSVerificationTest();
tester.runAll().catch(console.error);
