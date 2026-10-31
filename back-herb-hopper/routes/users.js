const express = require('express');
const crypto = require('crypto');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const router = express.Router();
const pool = require('../db');
const config = require('../config');
const { authenticate, requireRole } = require('../middleware/auth');
const rateLimit = require('../middleware/rateLimit');
const { logEvent } = require('../lib/audit');
const { isValidEmail, isStrongPassword, isValidPhone, sanitizeText } = require('../lib/validators');

const ROLES = ['PATIENT', 'SELLER', 'ADMIN'];
// En el registro público solo se puede elegir Paciente o Vendedor; ADMIN lo asigna otro administrador.
const SELF_SERVICE_ROLES = ['PATIENT', 'SELLER'];

const authLimiter = rateLimit({ windowMs: 15 * 60 * 1000, max: 20 });

function publicUser(user) {
    return { id: user.id, name: user.name, email: user.email, role: user.role, phone: user.phone || null, active: user.active !== false };
}

function hashToken(token) {
    return crypto.createHash('sha256').update(token).digest('hex');
}

/** Crea y guarda (hasheado) un token de renovación opaco. */
async function createRefreshToken(userId) {
    const refreshToken = crypto.randomBytes(48).toString('hex');
    const expiresAt = Date.now() + config.refreshTokenDays * 24 * 60 * 60 * 1000;
    await pool.query(
        'INSERT INTO refresh_tokens ("userId", "tokenHash", "expiresAt") VALUES ($1, $2, $3)',
        [userId, hashToken(refreshToken), expiresAt]
    );
    return refreshToken;
}

/** Emite el token de acceso (JWT de vida corta) y un token de renovación para la sesión. */
async function issueTokens(user) {
    const accessToken = jwt.sign({ sub: user.id, role: user.role }, config.jwtSecret, { expiresIn: config.accessTokenTtl });
    const refreshToken = await createRefreshToken(user.id);
    await logEvent('TOKEN', `Tokens emitidos para ${user.email}`, { userId: user.id });
    return { token: accessToken, refreshToken };
}

// ─── Registro (público) ────────────────────────────────────────────────────
router.post('/', authLimiter, async (req, res) => {
    const { name, email, password, role, phone } = req.body;
    const normalizedEmail = (email || '').trim().toLowerCase();
    const requestedRole = role || 'PATIENT';

    if (!name || !normalizedEmail || !password) {
        return res.status(400).json({ error: 'Nombre, correo y contraseña son obligatorios' });
    }
    if (!isValidEmail(normalizedEmail)) {
        return res.status(400).json({ error: 'El correo debe ser de Gmail, Outlook, Hotmail, Yahoo o iCloud' });
    }
    if (!isStrongPassword(password)) {
        return res.status(400).json({ error: 'La contraseña debe tener mínimo 8 caracteres, mayúscula, minúscula, número y carácter especial' });
    }
    if (!isValidPhone(phone)) {
        return res.status(400).json({ error: 'El teléfono debe ser un celular colombiano de 10 dígitos' });
    }
    if (!SELF_SERVICE_ROLES.includes(requestedRole)) {
        return res.status(403).json({ error: 'No se puede registrar una cuenta con ese rol' });
    }

    try {
        const exists = await pool.query('SELECT 1 FROM users WHERE email = $1', [normalizedEmail]);
        if (exists.rows.length > 0) {
            return res.status(400).json({ error: 'El correo electrónico ya está registrado' });
        }
        const hashedPassword = await bcrypt.hash(password, 10);
        const uid = crypto.randomUUID();
        const cleanName = sanitizeText(name, 255);
        await pool.query(
            'INSERT INTO users (id, name, email, password, role, phone) VALUES ($1, $2, $3, $4, $5, $6)',
            [uid, cleanName, normalizedEmail, hashedPassword, requestedRole, phone || null]
        );
        const user = { id: uid, name: cleanName, email: normalizedEmail, role: requestedRole, phone };
        await logEvent('ACCESS', `Registro de cuenta ${requestedRole}: ${normalizedEmail}`, { userId: uid });
        const tokens = await issueTokens(user);
        res.status(201).json({ message: 'Usuario creado exitosamente', user: publicUser(user), ...tokens });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// ─── Inicio de sesión (público) ────────────────────────────────────────────
router.post('/login', authLimiter, async (req, res) => {
    const { email, password } = req.body;
    const normalizedEmail = (email || '').trim().toLowerCase();
    if (!normalizedEmail || !password) {
        return res.status(400).json({ error: 'Email y contraseña son obligatorios' });
    }
    try {
        const result = await pool.query('SELECT * FROM users WHERE email = $1', [normalizedEmail]);
        const user = result.rows[0];
        // Mismo mensaje para usuario inexistente o contraseña errada (no revela qué cuentas existen)
        if (!user || !(await bcrypt.compare(password, user.password))) {
            await logEvent('ACCESS', `Intento fallido de inicio de sesión: ${normalizedEmail}`, { level: 'WARNING' });
            return res.status(401).json({ error: 'Credenciales incorrectas' });
        }
        if (!user.active) {
            await logEvent('ACCESS', `Intento de acceso de cuenta inactiva: ${normalizedEmail}`, { userId: user.id, level: 'WARNING' });
            return res.status(403).json({ error: 'La cuenta está inactiva. Contacte al administrador.' });
        }
        await logEvent('ACCESS', `Inicio de sesión: ${normalizedEmail}`, { userId: user.id });
        const tokens = await issueTokens(user);
        res.json({ message: 'Inicio de sesión exitoso', user: publicUser(user), ...tokens });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// ─── Renovación de sesión (usada por el acceso con huella) ─────────────────
router.post('/refresh', authLimiter, async (req, res) => {
    const { refreshToken } = req.body;
    if (!refreshToken) {
        return res.status(400).json({ error: 'refreshToken es obligatorio' });
    }
    try {
        const result = await pool.query(
            `SELECT rt.id AS "tokenId", rt."expiresAt", rt.revoked, u.*
             FROM refresh_tokens rt JOIN users u ON u.id = rt."userId"
             WHERE rt."tokenHash" = $1`,
            [hashToken(refreshToken)]
        );
        const row = result.rows[0];
        if (!row || row.revoked || row.expiresAt < Date.now() || !row.active) {
            return res.status(401).json({ error: 'La sesión guardada venció. Inicie sesión con su contraseña.' });
        }
        // Rotación: el token usado se revoca y se emite uno nuevo
        await pool.query('UPDATE refresh_tokens SET revoked = TRUE WHERE id = $1', [row.tokenId]);
        const tokens = await issueTokens(row);
        // Acceso con huella: además del token de la sesión se entrega uno nuevo para la bóveda del dispositivo
        if (req.body.device === true) {
            tokens.deviceToken = await createRefreshToken(row.id);
            await logEvent('ACCESS', `Inicio de sesión biométrico: ${row.email}`, { userId: row.id });
        }
        res.json({ message: 'Sesión renovada', user: publicUser(row), ...tokens });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

router.post('/logout', async (req, res) => {
    const { refreshToken } = req.body;
    if (refreshToken) {
        await pool.query('UPDATE refresh_tokens SET revoked = TRUE WHERE "tokenHash" = $1', [hashToken(refreshToken)]).catch(() => {});
    }
    res.json({ message: 'Sesión cerrada' });
});

// ─── Restablecimiento de contraseña ────────────────────────────────────────
router.post('/forgot-password', authLimiter, async (req, res) => {
    const { email } = req.body;
    if (!email) return res.status(400).json({ error: 'El email es obligatorio' });
    try {
        await pool.query(
            'INSERT INTO password_resets (email, timestamp, status) VALUES ($1, $2, $3)',
            [email.trim().toLowerCase(), Date.now(), 'PENDING']
        );
        res.json({ message: 'Solicitud de restablecimiento enviada exitosamente' });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// A partir de aquí todas las rutas exigen token
router.use(authenticate);

// Activar acceso con huella: token de renovación exclusivo para la bóveda biométrica del dispositivo
router.post('/device-token', async (req, res) => {
    try {
        const deviceToken = await createRefreshToken(req.user.id);
        await logEvent('TOKEN', `Acceso biométrico activado por ${req.user.email}`, { userId: req.user.id });
        res.status(201).json({ deviceToken });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// Perfil propio (CRUD de perfil)
router.get('/me', async (req, res) => {
    const result = await pool.query('SELECT * FROM users WHERE id = $1', [req.user.id]);
    res.json(publicUser(result.rows[0]));
});

router.put('/me', async (req, res) => {
    const { name, phone, password } = req.body;
    if (phone !== undefined && !isValidPhone(phone)) {
        return res.status(400).json({ error: 'El teléfono debe ser un celular colombiano de 10 dígitos' });
    }
    if (password && !isStrongPassword(password)) {
        return res.status(400).json({ error: 'La contraseña no cumple la política de seguridad' });
    }
    try {
        if (name) await pool.query('UPDATE users SET name = $1 WHERE id = $2', [sanitizeText(name, 255), req.user.id]);
        if (phone !== undefined) await pool.query('UPDATE users SET phone = $1 WHERE id = $2', [phone || null, req.user.id]);
        if (password) {
            await pool.query('UPDATE users SET password = $1 WHERE id = $2', [await bcrypt.hash(password, 10), req.user.id]);
            await pool.query('UPDATE refresh_tokens SET revoked = TRUE WHERE "userId" = $1', [req.user.id]);
        }
        const result = await pool.query('SELECT * FROM users WHERE id = $1', [req.user.id]);
        res.json(publicUser(result.rows[0]));
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

router.delete('/me', async (req, res) => {
    // Derecho de supresión (Ley 1581 de 2012)
    await pool.query('DELETE FROM users WHERE id = $1', [req.user.id]);
    await logEvent('ACCESS', `El usuario eliminó su propia cuenta`, { userId: req.user.id });
    res.json({ message: 'Cuenta eliminada' });
});

// ─── Administración de usuarios (solo ADMIN) ───────────────────────────────
router.get('/', requireRole('ADMIN'), async (req, res) => {
    try {
        const result = await pool.query('SELECT id, name, email, role, phone, active FROM users ORDER BY name');
        res.json(result.rows.map(publicUser));
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

router.post('/admin-create', requireRole('ADMIN'), async (req, res) => {
    const { name, email, password, role } = req.body;
    const normalizedEmail = (email || '').trim().toLowerCase();
    if (!name || !normalizedEmail || !password || !ROLES.includes(role)) {
        return res.status(400).json({ error: 'Datos incompletos o rol inválido' });
    }
    if (!isStrongPassword(password)) {
        return res.status(400).json({ error: 'La contraseña no cumple la política de seguridad' });
    }
    try {
        const exists = await pool.query('SELECT 1 FROM users WHERE email = $1', [normalizedEmail]);
        if (exists.rows.length > 0) {
            return res.status(400).json({ error: 'El correo electrónico ya está registrado' });
        }
        const uid = crypto.randomUUID();
        await pool.query(
            'INSERT INTO users (id, name, email, password, role) VALUES ($1, $2, $3, $4, $5)',
            [uid, sanitizeText(name, 255), normalizedEmail, await bcrypt.hash(password, 10), role]
        );
        await logEvent('ADMIN', `Usuario creado por administrador: ${normalizedEmail} (${role})`, { userId: req.user.id });
        res.status(201).json({ message: 'Usuario creado exitosamente' });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

router.get('/password-resets', requireRole('ADMIN'), async (req, res) => {
    try {
        const result = await pool.query('SELECT * FROM password_resets ORDER BY timestamp DESC');
        res.json(result.rows);
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

router.post('/password-resets/:id/resolve', requireRole('ADMIN'), async (req, res) => {
    try {
        await pool.query("UPDATE password_resets SET status = 'RESOLVED' WHERE id = $1", [req.params.id]);
        res.json({ message: 'Solicitud resuelta exitosamente' });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

router.put('/:id', requireRole('ADMIN'), async (req, res) => {
    const { id } = req.params;
    const { name, email, password, role } = req.body;
    if (!ROLES.includes(role)) {
        return res.status(400).json({ error: 'Rol inválido' });
    }
    if (password && password.trim() !== '' && !isStrongPassword(password)) {
        return res.status(400).json({ error: 'La contraseña no cumple la política de seguridad' });
    }
    try {
        if (password && password.trim() !== '') {
            await pool.query(
                'UPDATE users SET name = $1, email = $2, password = $3, role = $4 WHERE id = $5',
                [sanitizeText(name, 255), email.trim().toLowerCase(), await bcrypt.hash(password, 10), role, id]
            );
            await pool.query('UPDATE refresh_tokens SET revoked = TRUE WHERE "userId" = $1', [id]);
        } else {
            await pool.query(
                'UPDATE users SET name = $1, email = $2, role = $3 WHERE id = $4',
                [sanitizeText(name, 255), email.trim().toLowerCase(), role, id]
            );
        }
        await logEvent('ADMIN', `Usuario ${id} actualizado (rol ${role})`, { userId: req.user.id });
        res.json({ message: 'Usuario actualizado exitosamente' });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// Inactivar / reactivar cuenta (por violaciones de seguridad o soportes falsos)
router.patch('/:id/status', requireRole('ADMIN'), async (req, res) => {
    const { active } = req.body;
    if (typeof active !== 'boolean') {
        return res.status(400).json({ error: 'El campo active debe ser true o false' });
    }
    if (req.params.id === req.user.id) {
        return res.status(400).json({ error: 'No puede inactivar su propia cuenta' });
    }
    try {
        await pool.query('UPDATE users SET active = $1 WHERE id = $2', [active, req.params.id]);
        if (!active) {
            await pool.query('UPDATE refresh_tokens SET revoked = TRUE WHERE "userId" = $1', [req.params.id]);
        }
        await logEvent('ADMIN', `Cuenta ${req.params.id} ${active ? 'reactivada' : 'inactivada'}`, { userId: req.user.id, level: active ? 'INFO' : 'WARNING' });
        res.json({ message: active ? 'Cuenta reactivada' : 'Cuenta inactivada' });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

router.delete('/:id', requireRole('ADMIN'), async (req, res) => {
    if (req.params.id === req.user.id) {
        return res.status(400).json({ error: 'No puede eliminar su propia cuenta desde aquí' });
    }
    try {
        await pool.query('DELETE FROM users WHERE id = $1', [req.params.id]);
        await logEvent('ADMIN', `Usuario ${req.params.id} eliminado`, { userId: req.user.id, level: 'WARNING' });
        res.json({ message: 'Usuario eliminado exitosamente' });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

module.exports = router;
