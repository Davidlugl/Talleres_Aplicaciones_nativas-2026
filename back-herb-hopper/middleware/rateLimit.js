// Limitador en memoria para frenar ataques de fuerza bruta en login y registro.
function rateLimit({ windowMs, max }) {
    const hits = new Map();
    return (req, res, next) => {
        const key = req.ip;
        const now = Date.now();
        const entry = hits.get(key);
        if (!entry || now - entry.start > windowMs) {
            hits.set(key, { start: now, count: 1 });
            return next();
        }
        entry.count += 1;
        if (entry.count > max) {
            return res.status(429).json({ error: 'Demasiados intentos. Intente de nuevo en unos minutos.' });
        }
        next();
    };
}

module.exports = rateLimit;
