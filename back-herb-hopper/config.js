// Configuración centralizada. Todos los secretos llegan por variables de entorno
// (Render → Environment). Nunca se escriben credenciales en el código fuente.
function required(name) {
    const value = process.env[name];
    if (!value) {
        throw new Error(`Falta la variable de entorno obligatoria ${name}`);
    }
    return value;
}

module.exports = {
    port: process.env.PORT || 3000,
    databaseUrl: required('DATABASE_URL'),
    jwtSecret: required('JWT_SECRET'),
    // Vigencia corta del token de acceso (Ley 1581: minimizar exposición de sesiones)
    accessTokenTtl: process.env.ACCESS_TOKEN_TTL || '1h',
    // Vigencia del token de renovación que usa el acceso con huella
    refreshTokenDays: parseInt(process.env.REFRESH_TOKEN_DAYS || '30', 10),
    cloudinary: {
        cloud_name: process.env.CLOUDINARY_CLOUD_NAME,
        api_key: process.env.CLOUDINARY_API_KEY,
        api_secret: process.env.CLOUDINARY_API_SECRET
    },
    epayco: {
        // P_CUST_ID_CLIENTE y P_KEY del panel de ePayco: solo viven en el servidor
        custId: process.env.EPAYCO_P_CUST_ID_CLIENTE || '',
        pKey: process.env.EPAYCO_P_KEY || ''
    },
    publicBaseUrl: process.env.PUBLIC_BASE_URL || 'https://herb-hopper-v2.onrender.com'
};
