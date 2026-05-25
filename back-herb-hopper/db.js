const { Pool } = require('pg');

// Conexión a la base de datos PostgreSQL en Render
const pool = new Pool({
    connectionString: 'postgresql://bdlugo_user:s4sH00M9oYqLpHtlkRrPo5KUERyttn3r@dpg-d8acb78js32c739ur78g-a.oregon-postgres.render.com/bdlugo',
    ssl: {
        rejectUnauthorized: false
    }
});

module.exports = pool;