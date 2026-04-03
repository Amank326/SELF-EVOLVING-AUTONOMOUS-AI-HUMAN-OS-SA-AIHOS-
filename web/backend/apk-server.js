const express = require('express');
const path = require('path');
const fs = require('fs');
const http = require('http');
const os = require('os');

const app = express();
const PORT = 8080;

// Middleware
app.use(express.static('public'));
app.use(express.json());

// Routes
app.get('/', (req, res) => {
    res.send(`
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>SA-AIHOS APK Server</title>
            <style>
                * {
                    margin: 0;
                    padding: 0;
                    box-sizing: border-box;
                }
                body {
                    font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                    min-height: 100vh;
                    display: flex;
                    justify-content: center;
                    align-items: center;
                    padding: 20px;
                }
                .container {
                    background: white;
                    border-radius: 15px;
                    box-shadow: 0 20px 60px rgba(0,0,0,0.3);
                    padding: 40px;
                    max-width: 600px;
                    width: 100%;
                }
                .header {
                    text-align: center;
                    margin-bottom: 40px;
                }
                .logo {
                    font-size: 48px;
                    margin-bottom: 10px;
                }
                h1 {
                    color: #333;
                    font-size: 28px;
                    margin-bottom: 10px;
                }
                .subtitle {
                    color: #666;
                    font-size: 14px;
                }
                .status {
                    background: #f0f4ff;
                    border-left: 4px solid #667eea;
                    padding: 15px;
                    margin-bottom: 30px;
                    border-radius: 5px;
                }
                .status-item {
                    display: flex;
                    justify-content: space-between;
                    margin: 8px 0;
                    color: #666;
                    font-size: 14px;
                }
                .status-item strong {
                    color: #333;
                }
                .apk-section {
                    margin-bottom: 30px;
                }
                .apk-section h2 {
                    font-size: 18px;
                    color: #333;
                    margin-bottom: 15px;
                    display: flex;
                    align-items: center;
                }
                .apk-icon {
                    margin-right: 10px;
                    font-size: 24px;
                }
                .apk-item {
                    background: #f8f9fa;
                    padding: 15px;
                    margin-bottom: 10px;
                    border-radius: 8px;
                    border: 1px solid #e9ecef;
                }
                .apk-name {
                    font-weight: 600;
                    color: #333;
                    margin-bottom: 8px;
                }
                .apk-info {
                    font-size: 12px;
                    color: #999;
                    margin-bottom: 12px;
                }
                .download-btn {
                    display: inline-block;
                    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                    color: white;
                    padding: 10px 20px;
                    border-radius: 5px;
                    text-decoration: none;
                    font-size: 14px;
                    font-weight: 600;
                    transition: transform 0.2s, box-shadow 0.2s;
                    border: none;
                    cursor: pointer;
                }
                .download-btn:hover {
                    transform: translateY(-2px);
                    box-shadow: 0 5px 15px rgba(102, 126, 234, 0.4);
                }
                .install-guide {
                    background: #e8f5e9;
                    border-left: 4px solid #4caf50;
                    padding: 15px;
                    margin-top: 20px;
                    border-radius: 5px;
                    font-size: 14px;
                    color: #2e7d32;
                }
                .install-guide strong {
                    color: #1b5e20;
                }
                .steps {
                    list-style: none;
                    counter-reset: step;
                    margin-top: 10px;
                }
                .steps li {
                    counter-increment: step;
                    margin: 8px 0;
                    padding-left: 25px;
                    position: relative;
                }
                .steps li:before {
                    content: counter(step);
                    position: absolute;
                    left: 0;
                    top: 0;
                    background: #4caf50;
                    color: white;
                    width: 20px;
                    height: 20px;
                    border-radius: 50%;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    font-size: 12px;
                    font-weight: bold;
                }
                .no-apk {
                    color: #d32f2f;
                    text-align: center;
                    padding: 20px;
                    background: #ffebee;
                    border-radius: 5px;
                }
                .footer {
                    text-align: center;
                    margin-top: 30px;
                    padding-top: 20px;
                    border-top: 1px solid #eee;
                    color: #999;
                    font-size: 12px;
                }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <div class="logo">📱</div>
                    <h1>SA-AIHOS</h1>
                    <p class="subtitle">Android APK Server</p>
                </div>

                <div class="status">
                    <div class="status-item">
                        <strong>Server Status:</strong>
                        <span>🟢 Running</span>
                    </div>
                    <div class="status-item">
                        <strong>Local IP:</strong>
                        <span id="ip">Loading...</span>
                    </div>
                    <div class="status-item">
                        <strong>Port:</strong>
                        <span>8080</span>
                    </div>
                    <div class="status-item">
                        <strong>Build Time:</strong>
                        <span id="buildTime">2026-02-12</span>
                    </div>
                </div>

                <div class="apk-section">
                    <h2><span class="apk-icon">📦</span>Available APKs</h2>
                    <div id="apk-list">
                        <div class="apk-item">
                            <div class="apk-name">app-debug.apk</div>
                            <div class="apk-info">
                                <span>Debug Build • API 34</span>
                            </div>
                            <a href="/api/apk" class="download-btn">⬇️ Download APK</a>
                        </div>
                    </div>
                </div>

                <div class="install-guide">
                    <strong>📲 Installation Steps:</strong>
                    <ol class="steps">
                        <li>Download the APK from above</li>
                        <li>Enable Unknown Sources in Settings</li>
                        <li>Open file and tap Install</li>
                        <li>Grant requested permissions</li>
                        <li>Launch from App Drawer</li>
                    </ol>
                </div>

                <div class="footer">
                    SA-AIHOS v1.0.0 • Built with ❤️ for Android
                </div>
            </div>

            <script>
                // Set local IP
                fetch('/api/ip')
                    .then(r => r.json())
                    .then(data => {
                        document.getElementById('ip').textContent = data.ip;
                    });

                // Set build time
                fetch('/api/buildinfo')
                    .then(r => r.json())
                    .then(data => {
                        document.getElementById('buildTime').textContent = data.buildTime;
                    });
            </script>
        </body>
        </html>
    `);
});

// API Routes
app.get('/api/ip', (req, res) => {
    const interfaces = os.networkInterfaces();
    let ip = 'localhost';

    for (const name of Object.keys(interfaces)) {
        for (const iface of interfaces[name]) {
            if (iface.family === 'IPv4' && !iface.internal) {
                ip = iface.address;
                break;
            }
        }
    }

    res.json({ ip });
});

app.get('/api/buildinfo', (req, res) => {
    res.json({
        buildTime: new Date().toISOString().split('T')[0],
        version: '1.0.0',
        package: 'com.aihos'
    });
});

// APK Download
app.get('/api/apk', (req, res) => {
    const apkPath = path.join(__dirname, '..', 'app', 'build', 'outputs', 'apk', 'debug', 'app-debug.apk');

    // Check if APK exists
    if (fs.existsSync(apkPath)) {
        res.download(apkPath, 'sa-aihos-debug.apk');
    } else {
        res.status(404).json({
            error: 'APK not found',
            message: 'Build the APK first using: ./gradlew assembleDebug',
            path: apkPath
        });
    }
});

// Health check
app.get('/health', (req, res) => {
    res.json({
        status: 'ok',
        timestamp: new Date().toISOString()
    });
});

// Start server
app.listen(PORT, '0.0.0.0', () => {
    const interfaces = os.networkInterfaces();
    let localIP = 'localhost';

    for (const name of Object.keys(interfaces)) {
        for (const iface of interfaces[name]) {
            if (iface.family === 'IPv4' && !iface.internal) {
                localIP = iface.address;
                break;
            }
        }
    }

    console.log('\n🚀 SA-AIHOS APK Server Started!');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    console.log(`✅ Server running on:`);
    console.log(`   Local:   http://localhost:${PORT}`);
    console.log(`   Network: http://${localIP}:${PORT}`);
    console.log(`   Port:    ${PORT}`);
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    console.log('📱 From your Moto G96 5G, visit:');
    console.log(`   http://${localIP}:${PORT}`);
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
});

module.exports = app;

