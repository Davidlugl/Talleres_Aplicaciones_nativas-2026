const express = require('express');
const router = express.Router();
const pool = require('../db');
const { authenticate, requireRole } = require('../middleware/auth');
const { logEvent } = require('../lib/audit');
const { sanitizeText } = require('../lib/validators');

// Decreto 811 de 2021: por encima de 1 % de THC el derivado se considera psicoactivo
// y solo se dispensa con fórmula médica.
const PSYCHOACTIVE_THC_THRESHOLD = 1.0;

/** Valida y normaliza el cuerpo de un producto. Devuelve { error } o { product }. */
function parseProduct(body) {
    const price = Number(body.price);
    const thcPercent = body.thcPercent == null ? 0 : Number(body.thcPercent);
    const stock = body.stock == null ? 0 : parseInt(body.stock, 10);
    const minStock = body.minStock == null ? 5 : parseInt(body.minStock, 10);

    if (!body.name || !body.category) return { error: 'Nombre y categoría son obligatorios' };
    if (!Number.isFinite(price) || price <= 0) return { error: 'El precio debe ser mayor a cero' };
    if (!Number.isFinite(thcPercent) || thcPercent < 0 || thcPercent > 100) return { error: 'El % de THC debe estar entre 0 y 100' };
    if (!Number.isInteger(stock) || stock < 0) return { error: 'El stock no puede ser negativo' };
    if (!Number.isInteger(minStock) || minStock < 0) return { error: 'El stock mínimo no puede ser negativo' };

    return {
        product: {
            name: sanitizeText(body.name, 255),
            description: sanitizeText(body.description, 2000),
            price,
            category: sanitizeText(body.category, 100),
            imageUrl: body.imageUrl || null,
            stock,
            minStock,
            cbdConcentration: sanitizeText(body.cbdConcentration, 100),
            thcPercent,
            benefits: sanitizeText(body.benefits, 2000),
            usageMode: sanitizeText(body.usageMode, 2000),
            certifications: sanitizeText(body.certifications, 500),
            requiresPrescription: Boolean(body.requiresPrescription) || thcPercent >= PSYCHOACTIVE_THC_THRESHOLD
        }
    };
}

// Catálogo público
router.get('/', async (req, res) => {
    try {
        const result = await pool.query('SELECT * FROM products ORDER BY id');
        res.json(result.rows);
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

router.get('/:id', async (req, res) => {
    try {
        const result = await pool.query('SELECT * FROM products WHERE id = $1', [req.params.id]);
        if (result.rows.length === 0) return res.status(404).json({ error: 'Producto no encontrado' });
        res.json(result.rows[0]);
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// Gestión de inventario: solo Vendedor y Administrador
router.use(authenticate, requireRole('SELLER', 'ADMIN'));

router.post('/', async (req, res) => {
    const { error, product } = parseProduct(req.body);
    if (error) return res.status(400).json({ error });
    try {
        const result = await pool.query(
            `INSERT INTO products (name, description, price, category, "imageUrl", stock, "minStock",
                "cbdConcentration", "thcPercent", benefits, "usageMode", certifications, "requiresPrescription")
             VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13) RETURNING *`,
            [product.name, product.description, product.price, product.category, product.imageUrl, product.stock,
                product.minStock, product.cbdConcentration, product.thcPercent, product.benefits, product.usageMode,
                product.certifications, product.requiresPrescription]
        );
        await logEvent('INVENTORY', `Producto creado: ${product.name}`, { userId: req.user.id });
        res.status(201).json(result.rows[0]);
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

router.put('/:id', async (req, res) => {
    const { error, product } = parseProduct(req.body);
    if (error) return res.status(400).json({ error });
    try {
        const result = await pool.query(
            `UPDATE products SET name=$1, description=$2, price=$3, category=$4, "imageUrl"=$5, stock=$6, "minStock"=$7,
                "cbdConcentration"=$8, "thcPercent"=$9, benefits=$10, "usageMode"=$11, certifications=$12,
                "requiresPrescription"=$13
             WHERE id=$14 RETURNING *`,
            [product.name, product.description, product.price, product.category, product.imageUrl, product.stock,
                product.minStock, product.cbdConcentration, product.thcPercent, product.benefits, product.usageMode,
                product.certifications, product.requiresPrescription, req.params.id]
        );
        if (result.rows.length === 0) return res.status(404).json({ error: 'Producto no encontrado' });
        await logEvent('INVENTORY', `Producto actualizado: ${product.name}`, { userId: req.user.id });
        res.json(result.rows[0]);
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

router.delete('/:id', async (req, res) => {
    try {
        const result = await pool.query('DELETE FROM products WHERE id = $1 RETURNING name', [req.params.id]);
        if (result.rows.length === 0) return res.status(404).json({ error: 'Producto no encontrado' });
        await logEvent('INVENTORY', `Producto eliminado: ${result.rows[0].name}`, { userId: req.user.id, level: 'WARNING' });
        res.json({ message: 'Producto eliminado' });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// Botón "Reordenar": registra la solicitud de reabastecimiento para el administrador
router.post('/:id/reorder', async (req, res) => {
    const quantity = parseInt(req.body.quantity, 10);
    if (!Number.isInteger(quantity) || quantity <= 0) {
        return res.status(400).json({ error: 'La cantidad debe ser mayor a cero' });
    }
    try {
        const product = await pool.query('SELECT name FROM products WHERE id = $1', [req.params.id]);
        if (product.rows.length === 0) return res.status(404).json({ error: 'Producto no encontrado' });
        await pool.query(
            'INSERT INTO restock_requests ("productId", "requestedBy", quantity, timestamp) VALUES ($1, $2, $3, $4)',
            [req.params.id, req.user.id, quantity, Date.now()]
        );
        await logEvent('INVENTORY', `Reabastecimiento solicitado: ${quantity} u. de ${product.rows[0].name}`, { userId: req.user.id, level: 'WARNING' });
        res.status(201).json({ message: 'Solicitud de reabastecimiento enviada al administrador' });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

module.exports = router;
