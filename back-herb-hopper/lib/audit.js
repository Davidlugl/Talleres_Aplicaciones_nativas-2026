const pool = require('../db');

/**
 * Registra un evento en la bitácora de auditoría (accesos, tokens, pagos, cambios de estado).
 * Nunca se guardan contraseñas, números de tarjeta ni el contenido de las fórmulas médicas.
 */
async function logEvent(type, message, { userId = null, level = 'INFO' } = {}) {
    try {
        await pool.query(
            'INSERT INTO audit_logs (type, message, "userId", level, timestamp) VALUES ($1, $2, $3, $4, $5)',
            [type, String(message).slice(0, 500), userId, level, Date.now()]
        );
    } catch (error) {
        console.error('No se pudo registrar el evento de auditoría:', error.message);
    }
}

module.exports = { logEvent };
