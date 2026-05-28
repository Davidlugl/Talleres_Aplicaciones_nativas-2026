const express = require('express');
const router = express.Router();
const pool = require('../db');
const bcrypt = require('bcryptjs');

// Asegurar tabla de restablecimiento de contraseña
pool.query(`
    CREATE TABLE IF NOT EXISTS password_resets (
        id SERIAL PRIMARY KEY,
        email VARCHAR(255) NOT NULL,
        timestamp BIGINT NOT NULL,
        status VARCHAR(50) DEFAULT 'PENDING'
    )
`).catch(err => console.error("Error al crear tabla password_resets:", err));

// Obtener usuarios
router.get('/', async (req, res) => {
    try {
        const result = await pool.query('SELECT id, name, email, role FROM users');
        res.json(result.rows);
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// Registrar petición de restablecimiento de contraseña
router.post('/forgot-password', async (req, res) => {
    const { email } = req.body;
    if (!email) return res.status(400).json({ error: 'El email es obligatorio' });
    try {
        await pool.query(
            'INSERT INTO password_resets (email, timestamp, status) VALUES ($1, $2, $3)',
            [email, Date.now(), 'PENDING']
        );
        res.json({ message: 'Solicitud de restablecimiento enviada exitosamente' });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// Obtener todas las peticiones de restablecimiento de contraseña (Admin)
router.get('/password-resets', async (req, res) => {
    try {
        const result = await pool.query('SELECT * FROM password_resets ORDER BY timestamp DESC');
        res.json(result.rows);
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// Resolver/marcar como resuelta una petición de restablecimiento (Admin)
router.post('/password-resets/:id/resolve', async (req, res) => {
    const { id } = req.params;
    try {
        await pool.query("UPDATE password_resets SET status = 'RESOLVED' WHERE id = $1", [id]);
        res.json({ message: 'Solicitud resuelta exitosamente' });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// Modificar/Actualizar usuario (Admin CRUD)
router.put('/:id', async (req, res) => {
    const { id } = req.params;
    const { name, email, password, role } = req.body;
    try {
        if (password && password.trim() !== '') {
            const salt = await bcrypt.genSalt(10);
            const hashedPassword = await bcrypt.hash(password, salt);
            await pool.query(
                'UPDATE users SET name = $1, email = $2, password = $3, role = $4 WHERE id = $5',
                [name, email, hashedPassword, role, id]
            );
        } else {
            await pool.query(
                'UPDATE users SET name = $1, email = $2, role = $3 WHERE id = $4',
                [name, email, role, id]
            );
        }
        res.json({ message: 'Usuario actualizado exitosamente' });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// Eliminar usuario (Admin CRUD)
router.delete('/:id', async (req, res) => {
    const { id } = req.params;
    try {
        await pool.query('DELETE FROM users WHERE id = $1', [id]);
        res.json({ message: 'Usuario eliminado exitosamente' });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// Crear usuario (Registro)
router.post('/', async (req, res) => {
    const { id, name, email, password, role } = req.body;

    if (!email || !password) {
        return res.status(400).json({ error: 'Email y contraseña son obligatorios' });
    }

    try {
        // Verificar si el usuario ya existe
        const userExists = await pool.query('SELECT * FROM users WHERE email = $1', [email]);
        if (userExists.rows.length > 0) {
            return res.status(400).json({ error: 'El correo electrónico ya está registrado' });
        }

        const salt = await bcrypt.genSalt(10);
        const hashedPassword = await bcrypt.hash(password, salt);
        const uid = id || require('crypto').randomUUID();

        await pool.query(
            'INSERT INTO users (id, name, email, password, role) VALUES ($1, $2, $3, $4, $5)',
            [uid, name, email, hashedPassword, role || 'PATIENT']
        );
        res.status(201).json({ message: 'Usuario creado exitosamente', user: { id: uid, name, email, role: role || 'PATIENT' } });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// Inicio de sesión (Login)
router.post('/login', async (req, res) => {
    const { email, password } = req.body;

    if (!email || !password) {
        return res.status(400).json({ error: 'Email y contraseña son obligatorios' });
    }

    try {
        const result = await pool.query('SELECT * FROM users WHERE email = $1', [email]);
        if (result.rows.length === 0) {
            return res.status(401).json({ error: 'Credenciales incorrectas (Usuario no encontrado)' });
        }

        const user = result.rows[0];
        
        // Soporte retroactivo para contraseñas en texto plano, en caso de que ya hubieran
        const isMatch = password === user.password || await bcrypt.compare(password, user.password);

        if (!isMatch) {
            return res.status(401).json({ error: 'Credenciales incorrectas (Contraseña inválida)' });
        }

        res.json({ message: 'Inicio de sesión exitoso', user: { id: user.id, name: user.name, email: user.email, role: user.role } });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

module.exports = router;