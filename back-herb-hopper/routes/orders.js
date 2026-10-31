const express = require('express');
const router = express.Router();
const pool = require('../db');
const { authenticate, requireRole } = require('../middleware/auth');
const { logEvent } = require('../lib/audit');
const { sanitizeText } = require('../lib/validators');

const ORDER_STATUSES = ['PENDING', 'ACCEPTED', 'CANCELLED', 'DELIVERED'];
const PAYMENT_METHODS = ['CREDIT_CARD', 'PSE', 'CASH'];

// Consulta base: incluye el estado de la fórmula adjunta para el vendedor
const ORDER_SELECT = `
    SELECT o.*, p.status AS "prescriptionStatus"
    FROM orders o LEFT JOIN prescriptions p ON p.id = o."prescriptionId"`;

router.use(authenticate);

// Paciente: solo sus pedidos. Vendedor/Administrador: todos.
router.get('/', async (req, res) => {
    try {
        const isStaff = ['SELLER', 'ADMIN'].includes(req.user.role);
        const result = isStaff
            ? await pool.query(`${ORDER_SELECT} ORDER BY o.timestamp DESC`)
            : await pool.query(`${ORDER_SELECT} WHERE o."userId" = $1 ORDER BY o.timestamp DESC`, [req.user.id]);
        res.json(result.rows);
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// Métricas del dashboard del vendedor (ventas del día en COP y pedidos nuevos)
router.get('/stats/today', requireRole('SELLER', 'ADMIN'), async (req, res) => {
    try {
        const startOfDay = new Date();
        startOfDay.setHours(0, 0, 0, 0);
        const sales = await pool.query(
            `SELECT COALESCE(SUM("totalAmount"), 0) AS total, COUNT(*) AS count FROM orders
             WHERE timestamp >= $1 AND status <> 'CANCELLED' AND "paymentStatus" = 'APPROVED'`,
            [startOfDay.getTime()]
        );
        const pending = await pool.query("SELECT COUNT(*) AS count FROM orders WHERE status = 'PENDING'");
        res.json({
            salesToday: Number(sales.rows[0].total),
            paidOrdersToday: Number(sales.rows[0].count),
            newOrders: Number(pending.rows[0].count)
        });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

router.get('/:orderId', async (req, res) => {
    try {
        const result = await pool.query(`${ORDER_SELECT} WHERE o."orderId" = $1`, [req.params.orderId]);
        const order = result.rows[0];
        if (!order) return res.status(404).json({ error: 'Pedido no encontrado' });
        const isStaff = ['SELLER', 'ADMIN'].includes(req.user.role);
        if (!isStaff && order.userId !== req.user.id) {
            return res.status(403).json({ error: 'No tiene acceso a este pedido' });
        }
        res.json(order);
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

/**
 * Crear pedido. El total se recalcula en el servidor con los precios de la base de datos
 * (el cliente no puede alterar precios), se valida el stock y, si algún producto
 * requiere fórmula médica, se exige una fórmula del paciente que no esté rechazada.
 */
router.post('/', requireRole('PATIENT'), async (req, res) => {
    const { orderId, items, address, latitude, longitude, paymentMethod, prescriptionId } = req.body;

    if (!orderId || !Array.isArray(items) || items.length === 0) {
        return res.status(400).json({ error: 'El pedido debe tener un identificador y al menos un producto' });
    }
    if (!address || String(address).trim().length < 5) {
        return res.status(400).json({ error: 'La dirección de entrega es obligatoria' });
    }
    if (!PAYMENT_METHODS.includes(paymentMethod)) {
        return res.status(400).json({ error: 'Método de pago inválido' });
    }

    const client = await pool.connect();
    try {
        await client.query('BEGIN');

        const orderItems = [];
        let total = 0;
        let needsPrescription = false;
        for (const item of items) {
            const quantity = parseInt(item.quantity, 10);
            if (!Number.isInteger(quantity) || quantity <= 0) {
                throw { status: 400, message: 'Cantidad inválida' };
            }
            const productResult = await client.query('SELECT * FROM products WHERE id = $1 FOR UPDATE', [item.productId]);
            const product = productResult.rows[0];
            if (!product) throw { status: 400, message: `El producto ${item.productId} no existe` };
            if (product.stock < quantity) {
                throw { status: 409, message: `Stock insuficiente de ${product.name} (disponible: ${product.stock})` };
            }
            if (product.requiresPrescription) needsPrescription = true;
            await client.query('UPDATE products SET stock = stock - $1 WHERE id = $2', [quantity, product.id]);
            orderItems.push({ productId: product.id, productName: product.name, quantity, price: product.price });
            total += product.price * quantity;
        }

        let validPrescriptionId = null;
        if (prescriptionId != null) {
            const prescription = await client.query(
                `SELECT id FROM prescriptions WHERE id = $1 AND "userId" = $2 AND status <> 'REJECTED'`,
                [prescriptionId, req.user.id]
            );
            if (prescription.rows.length === 0) throw { status: 400, message: 'La fórmula médica indicada no es válida' };
            validPrescriptionId = prescription.rows[0].id;
        }
        if (needsPrescription && validPrescriptionId == null) {
            throw { status: 400, message: 'Este pedido incluye productos de control especial. Cargue su fórmula médica antes de pagar.' };
        }

        const now = Date.now();
        const result = await client.query(
            `INSERT INTO orders ("orderId", "userId", "userName", "itemsJson", "totalAmount", status, timestamp, address,
                "paymentMethod", latitude, longitude, "paymentStatus", "prescriptionId", "updatedAt")
             VALUES ($1,$2,$3,$4,$5,'PENDING',$6,$7,$8,$9,$10,'PENDING',$11,$6) RETURNING *`,
            [orderId, req.user.id, req.user.name, JSON.stringify(orderItems), total, now,
                sanitizeText(address, 500), paymentMethod, latitude ?? null, longitude ?? null, validPrescriptionId]
        );
        await client.query('COMMIT');
        await logEvent('ORDER', `Pedido ${orderId} creado por ${req.user.email} ($${total} COP)`, { userId: req.user.id });
        res.status(201).json(result.rows[0]);
    } catch (error) {
        await client.query('ROLLBACK');
        if (error.status) return res.status(error.status).json({ error: error.message });
        if (error.code === '23505') return res.status(409).json({ error: 'El pedido ya existe' });
        res.status(500).json({ error: error.message });
    } finally {
        client.release();
    }
});

/** Cancela un pedido y devuelve sus unidades al inventario (dentro de una transacción abierta). */
async function cancelOrder(client, order) {
    for (const item of JSON.parse(order.itemsJson)) {
        await client.query('UPDATE products SET stock = stock + $1 WHERE id = $2', [item.quantity, item.productId]);
    }
    await client.query(`UPDATE orders SET status = 'CANCELLED', "updatedAt" = $1 WHERE "orderId" = $2`, [Date.now(), order.orderId]);
}

// Paciente: cancela su propio pedido mientras no esté pagado ni aceptado (retracto, Ley 1480)
router.put('/:orderId/cancel', async (req, res) => {
    const client = await pool.connect();
    try {
        await client.query('BEGIN');
        const result = await client.query('SELECT * FROM orders WHERE "orderId" = $1 FOR UPDATE', [req.params.orderId]);
        const order = result.rows[0];
        if (!order) throw { status: 404, message: 'Pedido no encontrado' };
        if (order.userId !== req.user.id) throw { status: 403, message: 'No tiene acceso a este pedido' };
        if (order.status !== 'PENDING' || order.paymentStatus === 'APPROVED') {
            throw { status: 409, message: 'El pedido ya fue pagado o procesado. Solicite la reversión desde el Centro de Ayuda.' };
        }
        await cancelOrder(client, order);
        await client.query('COMMIT');
        await logEvent('ORDER', `Pedido ${order.orderId} cancelado por el paciente`, { userId: req.user.id });
        res.json({ message: 'Pedido cancelado' });
    } catch (error) {
        await client.query('ROLLBACK');
        if (error.status) return res.status(error.status).json({ error: error.message });
        res.status(500).json({ error: error.message });
    } finally {
        client.release();
    }
});

// Cambio de estado por el vendedor: ACEPTAR, CANCELAR, MARCAR COMO ENTREGADO
router.put('/:orderId/status', requireRole('SELLER', 'ADMIN'), async (req, res) => {
    const { status } = req.body;
    if (!ORDER_STATUSES.includes(status)) {
        return res.status(400).json({ error: 'Estado inválido' });
    }
    const client = await pool.connect();
    try {
        await client.query('BEGIN');
        const result = await client.query(`${ORDER_SELECT} WHERE o."orderId" = $1 FOR UPDATE OF o`, [req.params.orderId]);
        const order = result.rows[0];
        if (!order) throw { status: 404, message: 'Pedido no encontrado' };
        if (order.status === 'CANCELLED' || order.status === 'DELIVERED') {
            throw { status: 409, message: 'El pedido ya fue cerrado' };
        }
        if (status === 'ACCEPTED' && order.prescriptionId && order.prescriptionStatus !== 'APPROVED') {
            throw { status: 409, message: 'La fórmula médica del pedido aún no ha sido aprobada' };
        }
        if (status === 'DELIVERED' && order.status !== 'ACCEPTED') {
            throw { status: 409, message: 'Solo se puede entregar un pedido aceptado' };
        }
        if (status === 'CANCELLED') {
            await cancelOrder(client, order);
        } else {
            await client.query('UPDATE orders SET status = $1, "updatedAt" = $2 WHERE "orderId" = $3', [status, Date.now(), req.params.orderId]);
        }
        await client.query('COMMIT');
        await logEvent('ORDER', `Pedido ${req.params.orderId} → ${status}`, { userId: req.user.id });
        res.json({ message: 'Estado actualizado' });
    } catch (error) {
        await client.query('ROLLBACK');
        if (error.status) return res.status(error.status).json({ error: error.message });
        res.status(500).json({ error: error.message });
    } finally {
        client.release();
    }
});

module.exports = router;
module.exports.cancelOrder = cancelOrder;
