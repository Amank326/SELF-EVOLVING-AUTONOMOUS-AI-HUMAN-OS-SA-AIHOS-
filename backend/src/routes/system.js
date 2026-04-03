/**
 * System Routes
 */

const express = require('express');
const router = express.Router();
const pool = require('../database/pool');

// Initialize system
router.get('/init', async (req, res) => {
  try {
    const userId = req.user.id;

    // Get user data
    const userResult = await pool.query(
      'SELECT id, email, first_name, last_name FROM users WHERE id = $1',
      [userId]
    );

    if (userResult.rows.length === 0) {
      return res.status(404).json({
        error: 'Not Found',
        message: 'User not found'
      });
    }

    const user = userResult.rows[0];

    // Get memory count
    const memoryResult = await pool.query(
      'SELECT COUNT(*) as count FROM memory WHERE user_id = $1 AND is_archived = FALSE',
      [userId]
    );

    res.json({
      status: 'initialized',
      user,
      system: {
        version: '1.0.0',
        ai_core: 'online',
        memory_units: memoryResult.rows[0].count,
        last_sync: new Date().toISOString()
      }
    });
  } catch (error) {
    res.status(500).json({
      error: 'Internal Server Error',
      message: error.message
    });
  }
});

// System status
router.get('/status', async (req, res) => {
  res.json({
    status: 'running',
    uptime: process.uptime(),
    memory: process.memoryUsage(),
    timestamp: new Date().toISOString()
  });
});

module.exports = router;

