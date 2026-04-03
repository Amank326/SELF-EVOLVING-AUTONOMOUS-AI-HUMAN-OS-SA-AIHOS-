/**
 * Database Configuration
 */

const { Pool } = require('pg');
const path = require('path');
require('dotenv').config({ path: path.join(__dirname, '..', '..', '.env') });

// Create connection pool
const pool = new Pool({
  user: process.env.DB_USER || 'sa_aihos',
  host: process.env.DB_HOST || 'localhost',
  database: process.env.DB_NAME || 'sa_aihos_db',
  password: process.env.DB_PASSWORD || 'aihos_secure_password',
  port: process.env.DB_PORT || 5432,
  max: 20,
  idleTimeoutMillis: 30000,
  connectionTimeoutMillis: 2000,
});

pool.on('error', (err) => {
  console.error('Unexpected error on idle client', err);
});

// Test connection
pool.query('SELECT NOW()', (err, res) => {
  if (err) {
    console.error('❌ Database connection failed:', err);
  } else {
    console.log('✅ Database connected:', res.rows[0]);
  }
});

module.exports = pool;

