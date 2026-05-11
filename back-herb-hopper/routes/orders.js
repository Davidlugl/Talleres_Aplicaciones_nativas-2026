const express = require('express');
const router = express.Router();
const pool = require('../db');

// Obtener todas las ordenes
router.get('/', async (req, res) => {
    try {
        const [rows] = await pool.query('SELECT * FROM orders');
        res.json(rows);
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// Crear orden
router.post('/', async (req, res) => {
    const { orderId, userId, userName, itemsJson, totalAmount, status, timestamp, address, paymentMethod } = req.body;
    try {
        await pool.query(
            'INSERT INTO orders (orderId, userId, userName, itemsJson, totalAmount, status, timestamp, address, paymentMethod) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)',
            [orderId, userId, userName, itemsJson, totalAmount, status || 'PENDING', timestamp, address, paymentMethod]
        );
        res.status(201).json({ message: 'Orden creada' });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// Actualizar estado de orden
router.put('/:orderId/status', async (req, res) => {
    const { status } = req.body;
    try {
        await pool.query('UPDATE orders SET status = ? WHERE orderId = ?', [status, req.params.orderId]);
        res.json({ message: 'Estado actualizado' });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

module.exports = router;
