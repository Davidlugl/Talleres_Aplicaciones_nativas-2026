const bcrypt = require('bcryptjs');
const pool = require('../db');

/**
 * Crea o actualiza el esquema de forma idempotente al arrancar el servidor.
 * Así la base existente en Render recibe las columnas nuevas sin perder datos.
 */
async function ensureSchema() {
    await pool.query(`
        CREATE TABLE IF NOT EXISTS users (
            id VARCHAR(255) PRIMARY KEY,
            name VARCHAR(255) NOT NULL,
            email VARCHAR(255) NOT NULL UNIQUE,
            password VARCHAR(255) NOT NULL,
            role VARCHAR(50) DEFAULT 'PATIENT'
        )`);
    await pool.query('ALTER TABLE users ADD COLUMN IF NOT EXISTS phone VARCHAR(20)');
    await pool.query('ALTER TABLE users ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE');

    await pool.query(`
        CREATE TABLE IF NOT EXISTS products (
            id SERIAL PRIMARY KEY,
            name VARCHAR(255) NOT NULL,
            description TEXT,
            price DOUBLE PRECISION NOT NULL,
            category VARCHAR(100) NOT NULL,
            "imageUrl" VARCHAR(255)
        )`);
    const productColumns = [
        'stock INTEGER NOT NULL DEFAULT 0',
        '"minStock" INTEGER NOT NULL DEFAULT 5',
        '"cbdConcentration" VARCHAR(100)',
        '"thcPercent" DOUBLE PRECISION NOT NULL DEFAULT 0',
        'benefits TEXT',
        '"usageMode" TEXT',
        'certifications TEXT',
        '"requiresPrescription" BOOLEAN NOT NULL DEFAULT FALSE'
    ];
    for (const column of productColumns) {
        await pool.query(`ALTER TABLE products ADD COLUMN IF NOT EXISTS ${column}`);
    }

    await pool.query(`
        CREATE TABLE IF NOT EXISTS prescriptions (
            id SERIAL PRIMARY KEY,
            "userId" VARCHAR(255) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
            "doctorName" VARCHAR(255) NOT NULL,
            "doctorRegistry" VARCHAR(100) NOT NULL,
            "publicId" VARCHAR(255) NOT NULL,
            "resourceType" VARCHAR(20) NOT NULL,
            "fileFormat" VARCHAR(20),
            status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
            "reviewNote" TEXT,
            "reviewedBy" VARCHAR(255),
            "createdAt" BIGINT NOT NULL,
            "reviewedAt" BIGINT
        )`);

    await pool.query(`
        CREATE TABLE IF NOT EXISTS orders (
            "orderId" VARCHAR(255) PRIMARY KEY,
            "userId" VARCHAR(255) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
            "userName" VARCHAR(255) NOT NULL,
            "itemsJson" TEXT NOT NULL,
            "totalAmount" DOUBLE PRECISION NOT NULL,
            status VARCHAR(50) DEFAULT 'PENDING',
            timestamp BIGINT NOT NULL,
            address TEXT,
            "paymentMethod" VARCHAR(100)
        )`);
    const orderColumns = [
        'latitude DOUBLE PRECISION',
        'longitude DOUBLE PRECISION',
        `"paymentStatus" VARCHAR(20) NOT NULL DEFAULT 'PENDING'`,
        '"paymentRef" VARCHAR(100)',
        '"prescriptionId" INTEGER REFERENCES prescriptions(id) ON DELETE SET NULL',
        '"updatedAt" BIGINT'
    ];
    for (const column of orderColumns) {
        await pool.query(`ALTER TABLE orders ADD COLUMN IF NOT EXISTS ${column}`);
    }

    await pool.query(`
        CREATE TABLE IF NOT EXISTS password_resets (
            id SERIAL PRIMARY KEY,
            email VARCHAR(255) NOT NULL,
            timestamp BIGINT NOT NULL,
            status VARCHAR(50) DEFAULT 'PENDING'
        )`);

    await pool.query(`
        CREATE TABLE IF NOT EXISTS refresh_tokens (
            id SERIAL PRIMARY KEY,
            "userId" VARCHAR(255) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
            "tokenHash" VARCHAR(128) NOT NULL UNIQUE,
            "expiresAt" BIGINT NOT NULL,
            revoked BOOLEAN NOT NULL DEFAULT FALSE
        )`);

    await pool.query(`
        CREATE TABLE IF NOT EXISTS audit_logs (
            id SERIAL PRIMARY KEY,
            type VARCHAR(50) NOT NULL,
            message TEXT NOT NULL,
            "userId" VARCHAR(255),
            level VARCHAR(20) NOT NULL DEFAULT 'INFO',
            timestamp BIGINT NOT NULL
        )`);

    await pool.query(`
        CREATE TABLE IF NOT EXISTS stores (
            id SERIAL PRIMARY KEY,
            name VARCHAR(255) NOT NULL,
            location VARCHAR(255) NOT NULL,
            "licenseNumber" VARCHAR(100) NOT NULL,
            status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        )`);

    await pool.query(`
        CREATE TABLE IF NOT EXISTS restock_requests (
            id SERIAL PRIMARY KEY,
            "productId" INTEGER NOT NULL REFERENCES products(id) ON DELETE CASCADE,
            "requestedBy" VARCHAR(255) NOT NULL,
            quantity INTEGER NOT NULL,
            status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
            timestamp BIGINT NOT NULL
        )`);

    await pool.query(`
        CREATE TABLE IF NOT EXISTS support_messages (
            id SERIAL PRIMARY KEY,
            "userId" VARCHAR(255) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
            text TEXT NOT NULL,
            "fromSupport" BOOLEAN NOT NULL DEFAULT FALSE,
            timestamp BIGINT NOT NULL
        )`);

    await hashLegacyPasswords();
}

/**
 * Las versiones anteriores guardaban algunas contraseñas en texto plano.
 * Se convierten una sola vez a hash bcrypt; el login ya no compara texto plano.
 */
async function hashLegacyPasswords() {
    const result = await pool.query("SELECT id, password FROM users WHERE password NOT LIKE '$2%'");
    for (const user of result.rows) {
        const hashed = await bcrypt.hash(user.password, 10);
        await pool.query('UPDATE users SET password = $1 WHERE id = $2', [hashed, user.id]);
    }
    if (result.rows.length > 0) {
        console.log(`🔐 ${result.rows.length} contraseñas heredadas convertidas a bcrypt`);
    }
}

module.exports = { ensureSchema };
