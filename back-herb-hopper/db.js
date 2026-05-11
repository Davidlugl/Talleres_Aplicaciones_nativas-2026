const mysql = require('mysql2/promise');

// Conexión a la base de datos MySQL (phpMyAdmin localhost)
// Se asume usuario 'root' y contraseña vacía como es por defecto en XAMPP/WAMP
const pool = mysql.createPool({
    host: 'localhost',
    user: 'root', 
    password: '', 
    database: 'herb-hopper-nativas',
    waitForConnections: true,
    connectionLimit: 10,
    queueLimit: 0
});

module.exports = pool;
