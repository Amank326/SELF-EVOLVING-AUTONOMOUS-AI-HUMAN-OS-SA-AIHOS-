/**
 * SA-AIHOS Backend Server
 * Express.js + PostgreSQL
 */

const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const morgan = require('morgan');
const rateLimit = require('express-rate-limit');
const path = require('path');
require('dotenv').config({ path: path.join(__dirname, '..', '.env') });

// Import routes
const authRoutes = require('./routes/auth');
const userRoutes = require('./routes/user');
const systemRoutes = require('./routes/system');
const memoryRoutes = require('./routes/memory');
const healthRoutes = require('./routes/health');

// Import middleware
const { errorHandler } = require('./middleware/errorHandler');
const { authMiddleware } = require('./middleware/auth');

// Initialize app
const app = express();
const PORT = process.env.PORT || 3000;

// ============================================
// SECURITY MIDDLEWARE
// ============================================

app.use(helmet());

// Rate limiting
const limiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 100, // limit each IP to 100 requests per windowMs
  message: 'Too many requests from this IP, please try again later.'
});
app.use(limiter);

// ============================================
// LOGGING & PARSING MIDDLEWARE
// ============================================

app.use(morgan('combined'));
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ limit: '10mb', extended: true }));

// ============================================
// CORS CONFIGURATION
// ============================================

const corsOptions = {
  origin: process.env.CORS_ORIGIN ? process.env.CORS_ORIGIN.split(',') : ['http://localhost:8000', 'http://localhost:3000', 'http://localhost:8080'],
  credentials: true,
  methods: ['GET', 'POST', 'PUT', 'DELETE', 'PATCH', 'OPTIONS'],
  allowedHeaders: ['Content-Type', 'Authorization']
};
app.use(cors(corsOptions));

// ============================================
// HEALTH CHECK
// ============================================

app.use('/api/health', healthRoutes);

// ============================================
// PUBLIC ROUTES (No authentication required)
// ============================================

app.use('/api/auth', authRoutes);

// ============================================
// PROTECTED ROUTES (Authentication required)
// ============================================

app.use('/api/user', authMiddleware, userRoutes);
app.use('/api/system', authMiddleware, systemRoutes);
app.use('/api/memory', authMiddleware, memoryRoutes);

// ============================================
// ROOT ENDPOINT
// ============================================

app.get('/', (req, res) => {
  res.json({
    name: 'SA-AIHOS Backend',
    version: '1.0.0',
    status: 'running',
    timestamp: new Date().toISOString(),
    endpoints: {
      health: '/api/health',
      auth: '/api/auth',
      user: '/api/user',
      system: '/api/system',
      memory: '/api/memory'
    }
  });
});

// ============================================
// 404 NOT FOUND
// ============================================

app.use((req, res) => {
  res.status(404).json({
    error: 'Not Found',
    message: `Route ${req.path} does not exist`,
    method: req.method
  });
});

// ============================================
// ERROR HANDLING
// ============================================

app.use(errorHandler);

// ============================================
// SERVER STARTUP
// ============================================

const server = app.listen(PORT, () => {
  console.log('\n' + '='.repeat(60));
  console.log('🚀 SA-AIHOS Backend Server Started');
  console.log('='.repeat(60));
  console.log(`✅ Server running on http://localhost:${PORT}`);
  console.log(`✅ Environment: ${process.env.NODE_ENV || 'development'}`);
  console.log(`✅ Database: ${process.env.DATABASE_URL ? '✓' : '✗'}`);
  console.log('='.repeat(60) + '\n');
});

// Graceful shutdown
process.on('SIGTERM', () => {
  console.log('\n\n👋 SIGTERM received, shutting down gracefully...\n');
  server.close(() => {
    console.log('Server closed');
    process.exit(0);
  });
});

module.exports = app;

