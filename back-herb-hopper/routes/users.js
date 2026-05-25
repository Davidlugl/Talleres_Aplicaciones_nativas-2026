const express = require('express');
const router = express.Router();
const pool = require('../db');
const bcrypt = require('bcryptjs');

// Obtener usuarios
router.get('/', async (req, res) => {
    try {
        const result = await pool.query('SELECT id, name, email, role FROM users');
        res.json(result.rows);
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