const express = require('express');
const router = express.Router();
const pool = require('../db');
const { authenticate, requireRole } = require('../middleware/auth');
const { logEvent } = require('../lib/audit');
const { sanitizeText } = require('../lib/validators');

const STORE_STATUSES = ['PENDING', 'VERIFIED', 'SUSPENDED'];

// Gobernanza de la red de sucursales: la lista vive en la base central y la app la sincroniza
router.use(authenticate);

router.get('/', requireRole('SELLER', 'ADMIN'), async (req, res) => {
    try {
        const result = await pool.query('SELECT * FROM stores ORDER BY name');
        res.json(result.rows);
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

router.post('/', requireRole('ADMIN'), async (req, res) => {
    const { name, location, licenseNumber } = req.body;
    if (!name || !location || !licenseNumber) {
        return res.status(400).json({ error: 'Nombre, ubicación y licencia son obligatorios' });
    }
    try {
        const result = await pool.query(
            'INSERT INTO stores (name, location, "licenseNumber", status) VALUES ($1, $2, $3, $4) RETURNING *',
            [sanitizeText(name, 255), sanitizeText(location, 255), sanitizeText(licenseNumber, 100), 'PENDING']
        );
        await logEvent('ADMIN', `Sucursal registrada: ${name}`, { userId: req.user.id });
        res.status(201).json(result.rows[0]);
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

router.put('/:id', requireRole('ADMIN'), async (req, res) => {
    const { name, location, licenseNumber, status } = req.body;
    if (!STORE_STATUSES.includes(status)) {
        return res.status(400).json({ error: 'Estado inválido' });
    }
    try {
        const result = await pool.query(
            'UPDATE stores SET name = $1, location = $2, "licenseNumber" = $3, status = $4 WHERE id = $5 RETURNING *',
            [sanitizeText(name, 255), sanitizeText(location, 255), sanitizeText(licenseNumber, 100), status, req.params.id]
        );
        if (result.rows.length === 0) return res.status(404).json({ error: 'Sucursal no encontrada' });
        await logEvent('ADMIN', `Sucursal ${name} → ${status}`, { userId: req.user.id });
        res.json(result.rows[0]);
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

router.delete('/:id', requireRole('ADMIN'), async (req, res) => {
    try {
        await pool.query('DELETE FROM stores WHERE id = $1', [req.params.id]);
        await logEvent('ADMIN', `Sucursal ${req.params.id} eliminada`, { userId: req.user.id, level: 'WARNING' });
        res.json({ message: 'Sucursal eliminada' });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

module.exports = router;
