const { Pool } = require('pg');
const config = require('./config');

// Conexión a PostgreSQL (Render). La cadena de conexión llega por DATABASE_URL.
function createPool() {
    return new Pool({
        connectionString: config.databaseUrl,
        ssl: process.env.PGSSL === 'disable' ? false : { rejectUnauthorized: false }
    });
}

let pool = createPool();

module.exports = {
    query: (...args) => pool.query(...args),
    connect: () => pool.connect(),
    stats: () => ({ total: pool.totalCount, idle: pool.idleCount, waiting: pool.waitingCount }),
    /**
     * Recicla el pool: las consultas nuevas usan conexiones nuevas y el pool anterior
     * se cierra cuando terminan las consultas que tenía en curso.
     */
    reset: async () => {
        const previous = pool;
        pool = createPool();
        await previous.end();
    }
};
