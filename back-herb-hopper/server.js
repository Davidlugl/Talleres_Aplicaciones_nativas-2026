const express = require('express');
const cors = require('cors');
const pool = require('./db');

const app = express();
app.use(cors());
app.use(express.json());

// Probar conexión a BD
app.get('/api/health', async (req, res) => {
    try {
        const [rows] = await pool.query('SELECT 1 + 1 AS solution');
        res.json({ status: 'success', message: 'Conectado a la base de datos herb-hopper-nativas', result: rows[0].solution });
    } catch (error) {
        res.status(500).json({ status: 'error', message: 'Fallo la conexión a la base de datos', error: error.message });
    }
});

// Rutas API (se pueden expandir creando la carpeta routes)
app.use('/api/products', require('./routes/products'));
app.use('/api/users', require('./routes/users'));
app.use('/api/orders', require('./routes/orders'));

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`🚀 Servidor backend ejecutándose en el puerto ${PORT}`);
    console.log(`📦 Conectado a phpMyAdmin: localhost - DB: herb-hopper-nativas`);
});
