/**
 * Memory Routes
 */

const express = require('express');
const router = express.Router();
const pool = require('../database/pool');

// Store memory
router.post('/store', async (req, res) => {
  try {
    const userId = req.user.id;
    const { type, content, metadata, importance_score } = req.body;

    const result = await pool.query(
      'INSERT INTO memory (user_id, type, content, metadata, importance_score) VALUES ($1, $2, $3, $4, $5) RETURNING *',
      [userId, type, content, metadata || {}, importance_score || 0]
    );

    res.status(201).json({
      message: 'Memory stored successfully',
      memory: result.rows[0]
    });
  } catch (error) {
    res.status(500).json({
      error: 'Internal Server Error',
      message: error.message
    });
  }
});

// Retrieve memories
router.get('/retrieve', async (req, res) => {
  try {
    const userId = req.user.id;
    const { type, limit = 10, offset = 0 } = req.query;

    let query = 'SELECT * FROM memory WHERE user_id = $1 AND is_archived = FALSE';
    const params = [userId];

    if (type) {
      query += ' AND type = $2';
      params.push(type);
    }

    query += ' ORDER BY importance_score DESC, created_at DESC LIMIT $' + (params.length + 1) + ' OFFSET $' + (params.length + 2);
    params.push(limit, offset);

    const result = await pool.query(query, params);

    res.json({
      memories: result.rows,
      count: result.rows.length
    });
  } catch (error) {
    res.status(500).json({
      error: 'Internal Server Error',
      message: error.message
    });
  }
});

// Delete memory
router.delete('/:id', async (req, res) => {
  try {
    const userId = req.user.id;
    const { id } = req.params;

    const result = await pool.query(
      'UPDATE memory SET is_archived = TRUE WHERE id = $1 AND user_id = $2 RETURNING *',
      [id, userId]
    );

    if (result.rows.length === 0) {
      return res.status(404).json({
        error: 'Not Found',
        message: 'Memory not found'
      });
    }

    res.json({
      message: 'Memory archived successfully',
      memory: result.rows[0]
    });
  } catch (error) {
    res.status(500).json({
      error: 'Internal Server Error',
      message: error.message
    });
  }
});

module.exports = router;

