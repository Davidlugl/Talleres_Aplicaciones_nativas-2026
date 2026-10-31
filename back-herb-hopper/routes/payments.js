const express = require('express');
const crypto = require('crypto');
const router = express.Router();
const pool = require('../db');
const config = require('../config');
const { authenticate } = require('../middleware/auth');
const { logEvent } = require('../lib/audit');
const { cancelOrder } = require('./orders');

// x_cod_response de ePayco: 1 Aceptada, 2 Rechazada, 3 Pendiente, 4 Fallida, 6 Reversada...
function mapPaymentStatus(code) {
    switch (String(code)) {
        case '1': return 'APPROVED';
        case '3': return 'PENDING';
        case '2':
        case '4':
        case '6':
        case '9':
        case '10':
        case '11':
            return 'REJECTED';
        default: return 'PENDING';
    }
}

/** Firma de ePayco: sha256(p_cust_id_cliente^p_key^x_ref_payco^x_transaction_id^x_amount^x_currency_code) */
function isValidSignature(data) {
    if (!config.epayco.custId || !config.epayco.pKey) return false;
    const raw = [config.epayco.custId, config.epayco.pKey, data.x_ref_payco, data.x_transaction_id, data.x_amount, data.x_currency_code].join('^');
    const expected = crypto.createHash('sha256').update(raw).digest('hex');
    const received = String(data.x_signature || '');
    return received.length === expected.length && crypto.timingSafeEqual(Buffer.from(expected), Buffer.from(received));
}

/**
 * URL de confirmación (servidor a servidor). ePayco la llama con el resultado real del cobro.
 * Es la única fuente de verdad del estado de pago: la app nunca marca un pedido como pagado por sí sola.
 */
async function handleConfirmation(req, res) {
    const data = { ...req.query, ...req.body };
    if (!isValidSignature(data)) {
        await logEvent('EPAYCO', `Confirmación con firma inválida (ref ${data.x_ref_payco || '?'})`, { level: 'WARNING' });
        return res.status(400).send('Firma inválida');
    }
    const orderId = data.x_id_invoice;
    const paymentStatus = mapPaymentStatus(data.x_cod_response);
    const client = await pool.connect();
    try {
        await client.query('BEGIN');
        const result = await client.query('SELECT * FROM orders WHERE "orderId" = $1 FOR UPDATE', [orderId]);
        const order = result.rows[0];
        if (!order) {
            await client.query('ROLLBACK');
            await logEvent('EPAYCO', `Confirmación para pedido inexistente ${orderId}`, { level: 'WARNING' });
            return res.status(404).send('Pedido no encontrado');
        }
        if (Math.round(Number(order.totalAmount)) !== Math.round(Number(data.x_amount))) {
            await client.query('ROLLBACK');
            await logEvent('EPAYCO', `Monto no coincide en pedido ${orderId}`, { level: 'WARNING' });
            return res.status(400).send('Monto no coincide');
        }
        await client.query(
            'UPDATE orders SET "paymentStatus" = $1, "paymentRef" = $2, "updatedAt" = $3 WHERE "orderId" = $4',
            [paymentStatus, data.x_ref_payco, Date.now(), orderId]
        );
        // Pago rechazado: el pedido se anula y las unidades vuelven al inventario
        if (paymentStatus === 'REJECTED' && order.status === 'PENDING') {
            await cancelOrder(client, order);
        }
        await client.query('COMMIT');
        await logEvent('EPAYCO', `Pedido ${orderId}: ${data.x_response || paymentStatus} (ref ${data.x_ref_payco})`,
            { level: paymentStatus === 'REJECTED' ? 'WARNING' : 'INFO' });
        res.send('OK');
    } catch (error) {
        await client.query('ROLLBACK');
        res.status(500).send(error.message);
    } finally {
        client.release();
    }
}

// ePayco puede enviar la confirmación por POST o por GET
router.post('/confirmation', handleConfirmation);
router.get('/confirmation', handleConfirmation);

// URL de respuesta: la ve el usuario al terminar en el checkout. La app la intercepta en el WebView.
router.get('/response', (req, res) => {
    res.send(`<!doctype html><html lang="es"><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>HerbHopper</title>
<body style="font-family:sans-serif;text-align:center;padding:40px">
<h2>Transacción procesada</h2><p>Puede volver a la aplicación HerbHopper para ver el estado de su pedido.</p>
</body></html>`);
});

// La app consulta aquí el estado real del pago de un pedido
router.get('/status/:orderId', authenticate, async (req, res) => {
    try {
        const result = await pool.query(
            'SELECT "orderId", "userId", "paymentStatus", "paymentRef", "paymentMethod" FROM orders WHERE "orderId" = $1',
            [req.params.orderId]
        );
        const order = result.rows[0];
        if (!order) return res.status(404).json({ error: 'Pedido no encontrado' });
        if (order.userId !== req.user.id && !['SELLER', 'ADMIN'].includes(req.user.role)) {
            return res.status(403).json({ error: 'No tiene acceso a este pedido' });
        }
        res.json({ orderId: order.orderId, paymentStatus: order.paymentStatus, paymentRef: order.paymentRef, paymentMethod: order.paymentMethod });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

module.exports = router;
