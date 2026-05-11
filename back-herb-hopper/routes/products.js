const express = require('express');
const router = express.Router();
const pool = require('../db');

// Obtener todos los productos
router.get('/', async (req, res) => {
    try {
        const [rows] = await pool.query('SELECT * FROM products');
        res.json(rows);
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// Crear producto
router.post('/', async (req, res) => {
    const { name, description, price, category, imageUrl } = req.body;
    try {
        const [result] = await pool.query(
            'INSERT INTO products (name, description, price, category, imageUrl) VALUES (?, ?, ?, ?, ?)',
            [name, description, price, category, imageUrl]
        );
        res.status(201).json({ id: result.insertId, ...req.body });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

module.exports = router;
