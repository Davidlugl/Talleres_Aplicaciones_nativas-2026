const express = require('express');
const router = express.Router();
const pool = require('../db');
const { authenticate, requireRole } = require('../middleware/auth');
const { sanitizeText } = require('../lib/validators');

// Chat de soporte: el usuario escribe desde la app y el equipo de soporte (ADMIN) responde.
router.use(authenticate);

function present(row) {
    return { id: row.id, userId: row.userId, text: row.text, fromSupport: row.fromSupport, timestamp: Number(row.timestamp) };
}

// Conversación del usuario autenticado
router.get('/messages', async (req, res) => {
    try {
        const result = await pool.query('SELECT * FROM support_messages WHERE "userId" = $1 ORDER BY timestamp', [req.user.id]);
        res.json(result.rows.map(present));
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

router.post('/messages', async (req, res) => {
    const text = sanitizeText(req.body.text, 2000);
    if (!text) return res.status(400).json({ error: 'El mensaje no puede estar vacío' });
    try {
        const result = await pool.query(
            'INSERT INTO support_messages ("userId", text, "fromSupport", timestamp) VALUES ($1, $2, FALSE, $3) RETURNING *',
            [req.user.id, text, Date.now()]
        );
        res.status(201).json(present(result.rows[0]));
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// Bandeja del equipo de soporte: última actividad por usuario
router.get('/conversations', requireRole('ADMIN'), async (req, res) => {
    try {
        const result = await pool.query(`
            SELECT DISTINCT ON (m."userId") m."userId", u.name, u.email, m.text, m."fromSupport", m.timestamp
            FROM support_messages m JOIN users u ON u.id = m."userId"
            ORDER BY m."userId", m.timestamp DESC`);
        res.json(result.rows
            .map(r => ({ ...r, timestamp: Number(r.timestamp), awaitingReply: !r.fromSupport }))
            .sort((a, b) => b.timestamp - a.timestamp));
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

router.get('/conversations/:userId', requireRole('ADMIN'), async (req, res) => {
    try {
        const result = await pool.query('SELECT * FROM support_messages WHERE "userId" = $1 ORDER BY timestamp', [req.params.userId]);
        res.json(result.rows.map(present));
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

router.post('/conversations/:userId/reply', requireRole('ADMIN'), async (req, res) => {
    const text = sanitizeText(req.body.text, 2000);
    if (!text) return res.status(400).json({ error: 'La respuesta no puede estar vacía' });
    try {
        const result = await pool.query(
            'INSERT INTO support_messages ("userId", text, "fromSupport", timestamp) VALUES ($1, $2, TRUE, $3) RETURNING *',
            [req.params.userId, text, Date.now()]
        );
        res.status(201).json(present(result.rows[0]));
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

module.exports = router;
