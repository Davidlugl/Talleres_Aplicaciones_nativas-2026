const jwt = require('jsonwebtoken');
const pool = require('../db');
const config = require('../config');

/**
 * Verifica el JWT enviado en "Authorization: Bearer <token>" y que la cuenta siga activa.
 * Deja en req.user { id, role, name, email }.
 */
async function authenticate(req, res, next) {
    const header = req.headers.authorization || '';
    const token = header.startsWith('Bearer ') ? header.slice(7) : null;
    if (!token) {
        return res.status(401).json({ error: 'Se requiere autenticación' });
    }
    let payload;
    try {
        payload = jwt.verify(token, config.jwtSecret);
    } catch (error) {
        return res.status(401).json({ error: 'Token inválido o vencido' });
    }
    try {
        const result = await pool.query('SELECT id, name, email, role, active FROM users WHERE id = $1', [payload.sub]);
        const user = result.rows[0];
        if (!user || !user.active) {
            return res.status(401).json({ error: 'La cuenta no existe o está inactiva' });
        }
        req.user = { id: user.id, role: user.role, name: user.name, email: user.email };
        next();
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
}

/** Control de acceso basado en roles (RBAC). */
function requireRole(...roles) {
    return (req, res, next) => {
        if (!req.user || !roles.includes(req.user.role)) {
            return res.status(403).json({ error: 'No tiene permisos para esta operación' });
        }
        next();
    };
}

module.exports = { authenticate, requireRole };
