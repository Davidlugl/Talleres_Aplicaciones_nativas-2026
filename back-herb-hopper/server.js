const express = require('express');
const cors = require('cors');
const config = require('./config');
const pool = require('./db');
const { ensureSchema } = require('./lib/schema');

const app = express();
app.set('trust proxy', 1); // Render está detrás de un proxy: necesario para req.ip en el limitador
app.use(cors());
app.use(express.json({ limit: '1mb' }));
// ePayco envía la confirmación como formulario (application/x-www-form-urlencoded)
app.use(express.urlencoded({ extended: false }));

// Forzar HTTPS en producción (Render termina TLS y envía X-Forwarded-Proto)
app.use((req, res, next) => {
    if (process.env.NODE_ENV === 'production' && req.headers['x-forwarded-proto'] === 'http') {
        return res.redirect(301, `https://${req.headers.host}${req.originalUrl}`);
    }
    next();
});

// Probar conexión a BD
app.get('/api/health', async (req, res) => {
    try {
        const result = await pool.query('SELECT 1 + 1 AS solution');
        res.json({ status: 'success', message: 'Conectado a la base de datos PostgreSQL', result: result.rows[0].solution });
    } catch (error) {
        res.status(500).json({ status: 'error', message: 'Falló la conexión a la base de datos' });
    }
});

app.use('/api/products', require('./routes/products'));
app.use('/api/users', require('./routes/users'));
app.use('/api/orders', require('./routes/orders'));
app.use('/api/upload', require('./routes/upload'));
app.use('/api/prescriptions', require('./routes/prescriptions'));
app.use('/api/payment', require('./routes/payments'));
app.use('/api/admin', require('./routes/admin'));
app.use('/api/stores', require('./routes/stores'));
app.use('/api/support', require('./routes/support'));

// Errores no controlados (p. ej. archivo demasiado grande en multer)
app.use((err, req, res, next) => {
    if (err.code === 'LIMIT_FILE_SIZE') {
        return res.status(413).json({ error: 'El archivo supera el tamaño máximo permitido' });
    }
    console.error(err);
    res.status(500).json({ error: 'Error interno del servidor' });
});

ensureSchema()
    .then(() => {
        app.listen(config.port, () => {
            console.log(`🚀 Servidor backend ejecutándose en el puerto ${config.port}`);
        });
    })
    .catch(error => {
        console.error('❌ No se pudo preparar el esquema de la base de datos:', error.message);
        process.exit(1);
    });

module.exports = app;
