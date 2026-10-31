const express = require('express');
const router = express.Router();
const pool = require('../db');
const { authenticate, requireRole } = require('../middleware/auth');
const { logEvent } = require('../lib/audit');

router.use(authenticate, requireRole('ADMIN'));

/**
 * Verificaciones de integridad referencial y de consistencia de datos.
 * El porcentaje que ve el administrador es la proporción de verificaciones superadas.
 */
const INTEGRITY_CHECKS = [
    { name: 'Productos con stock negativo', sql: 'SELECT COUNT(*) FROM products WHERE stock < 0' },
    { name: 'Productos con precio inválido', sql: 'SELECT COUNT(*) FROM products WHERE price <= 0' },
    { name: 'Pedidos sin productos', sql: `SELECT COUNT(*) FROM orders WHERE "itemsJson" IS NULL OR "itemsJson" = '[]'` },
    { name: 'Pedidos con total inválido', sql: 'SELECT COUNT(*) FROM orders WHERE "totalAmount" <= 0' },
    { name: 'Pedidos aceptados con fórmula no aprobada', sql: `SELECT COUNT(*) FROM orders o JOIN prescriptions p ON p.id = o."prescriptionId" WHERE o.status IN ('ACCEPTED','DELIVERED') AND p.status <> 'APPROVED'` },
    { name: 'Fórmulas pendientes con más de 24 h', sql: `SELECT COUNT(*) FROM prescriptions WHERE status = 'PENDING' AND "createdAt" < $1`, params: () => [Date.now() - 24 * 60 * 60 * 1000] },
    { name: 'Usuarios con rol desconocido', sql: `SELECT COUNT(*) FROM users WHERE role NOT IN ('PATIENT','SELLER','ADMIN')` },
    { name: 'Contraseñas sin cifrar', sql: `SELECT COUNT(*) FROM users WHERE password NOT LIKE '$2%'` }
];

async function runHealthCheck() {
    const started = Date.now();
    await pool.query('SELECT 1');
    const latencyMs = Date.now() - started;

    const checks = [];
    for (const check of INTEGRITY_CHECKS) {
        const result = await pool.query(check.sql, check.params ? check.params() : []);
        const issues = Number(result.rows[0].count);
        checks.push({ name: check.name, issues, ok: issues === 0 });
    }
    const passed = checks.filter(c => c.ok).length;
    const counts = await pool.query(`SELECT
        (SELECT COUNT(*) FROM users) AS users,
        (SELECT COUNT(*) FROM products) AS products,
        (SELECT COUNT(*) FROM orders) AS orders,
        (SELECT COUNT(*) FROM prescriptions WHERE status = 'PENDING') AS "pendingPrescriptions",
        (SELECT COUNT(*) FROM restock_requests WHERE status = 'PENDING') AS "pendingRestocks"`);

    return {
        database: 'UP',
        latencyMs,
        integrityPercent: Math.round((passed / checks.length) * 100),
        checks,
        counts: Object.fromEntries(Object.entries(counts.rows[0]).map(([k, v]) => [k, Number(v)])),
        uptimeSeconds: Math.round(process.uptime()),
        memoryMb: Math.round(process.memoryUsage().rss / 1024 / 1024),
        checkedAt: Date.now()
    };
}

router.get('/health', async (req, res) => {
    try {
        res.json(await runHealthCheck());
    } catch (error) {
        res.status(503).json({ database: 'DOWN', error: error.message, checkedAt: Date.now() });
    }
});

/**
 * "Reiniciar servicios": recicla el pool de conexiones a la base de datos (descarta las
 * conexiones colgadas y abre nuevas) y vuelve a ejecutar el diagnóstico.
 * Reiniciar el contenedor completo se hace desde el panel de Render.
 */
router.post('/services/restart', async (req, res) => {
    try {
        const before = pool.stats();
        await pool.reset();
        await logEvent('INFRA', `Reciclaje del pool de conexiones (${before.total} conexiones cerradas)`, { userId: req.user.id, level: 'WARNING' });
        const health = await runHealthCheck();
        res.json({ message: 'Servicios reiniciados', before, health });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// Analítica global: ventas aprobadas, usuarios activos, transacciones y distribución por estado y medio de pago
router.get('/analytics', async (req, res) => {
    try {
        const totals = await pool.query(`SELECT
            (SELECT COALESCE(SUM("totalAmount"), 0) FROM orders WHERE "paymentStatus" = 'APPROVED' AND status <> 'CANCELLED') AS "approvedSales",
            (SELECT COUNT(*) FROM orders) AS "totalOrders",
            (SELECT COUNT(*) FROM orders WHERE "paymentStatus" = 'APPROVED') AS "approvedPayments",
            (SELECT COUNT(*) FROM users WHERE active) AS "activeUsers"`);
        const byStatus = await pool.query('SELECT status AS key, COUNT(*) AS count FROM orders GROUP BY status');
        const byMethod = await pool.query(
            `SELECT "paymentMethod" AS key, COUNT(*) AS count, COALESCE(SUM("totalAmount"), 0) AS amount
             FROM orders WHERE "paymentStatus" = 'APPROVED' GROUP BY "paymentMethod"`
        );
        const byRole = await pool.query('SELECT role AS key, COUNT(*) AS count FROM users WHERE active GROUP BY role');
        const toNumbers = rows => rows.map(r => ({ key: r.key, count: Number(r.count), amount: r.amount != null ? Number(r.amount) : null }));
        res.json({
            approvedSales: Number(totals.rows[0].approvedSales),
            totalOrders: Number(totals.rows[0].totalOrders),
            approvedPayments: Number(totals.rows[0].approvedPayments),
            activeUsers: Number(totals.rows[0].activeUsers),
            ordersByStatus: toNumbers(byStatus.rows),
            salesByPaymentMethod: toNumbers(byMethod.rows),
            usersByRole: toNumbers(byRole.rows)
        });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// Bitácora: accesos, tokens emitidos, respuestas de ePayco, cambios de inventario y pedidos
router.get('/logs', async (req, res) => {
    const limit = Math.min(parseInt(req.query.limit || '100', 10), 500);
    const type = req.query.type;
    try {
        const result = type
            ? await pool.query('SELECT * FROM audit_logs WHERE type = $1 ORDER BY timestamp DESC LIMIT $2', [type, limit])
            : await pool.query('SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT $1', [limit]);
        res.json(result.rows.map(r => ({ ...r, timestamp: Number(r.timestamp) })));
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

router.get('/restock-requests', async (req, res) => {
    try {
        const result = await pool.query(
            `SELECT r.*, p.name AS "productName", p.stock FROM restock_requests r
             JOIN products p ON p.id = r."productId" ORDER BY r.timestamp DESC`
        );
        res.json(result.rows.map(r => ({ ...r, timestamp: Number(r.timestamp) })));
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// Atender una solicitud de reabastecimiento suma las unidades al inventario
router.post('/restock-requests/:id/fulfill', async (req, res) => {
    const client = await pool.connect();
    try {
        await client.query('BEGIN');
        const result = await client.query(
            `UPDATE restock_requests SET status = 'FULFILLED' WHERE id = $1 AND status = 'PENDING' RETURNING *`,
            [req.params.id]
        );
        if (result.rows.length === 0) {
            await client.query('ROLLBACK');
            return res.status(404).json({ error: 'Solicitud no encontrada o ya atendida' });
        }
        const request = result.rows[0];
        await client.query('UPDATE products SET stock = stock + $1 WHERE id = $2', [request.quantity, request.productId]);
        await client.query('COMMIT');
        await logEvent('INVENTORY', `Reabastecimiento #${request.id} atendido (+${request.quantity} u.)`, { userId: req.user.id });
        res.json({ message: 'Inventario actualizado' });
    } catch (error) {
        await client.query('ROLLBACK');
        res.status(500).json({ error: error.message });
    } finally {
        client.release();
    }
});

module.exports = router;
