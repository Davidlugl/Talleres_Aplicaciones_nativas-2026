const express = require('express');
const router = express.Router();
const pool = require('../db');

// Obtener usuarios
router.get('/', async (req, res) => {
    try {
        const [rows] = await pool.query('SELECT * FROM users');
        res.json(rows);
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// Crear usuario
router.post('/', async (req, res) => {
    const { id, name, email, password, role } = req.body;
    try {
        const [result] = await pool.query(
            'INSERT INTO users (id, name, email, password, role) VALUES (?, ?, ?, ?, ?)',
            [id, name, email, password, role || 'PATIENT']
        );
        res.status(201).json({ message: 'Usuario creado exitosamente' });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

module.exports = router;
