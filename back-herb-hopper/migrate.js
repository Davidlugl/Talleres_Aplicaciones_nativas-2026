// Reinicia la base de datos con el esquema actual y datos de demostración.
// ¡Borra todos los datos! Uso: DATABASE_URL=... JWT_SECRET=... DEMO_PASSWORD=... node migrate.js
const crypto = require('crypto');
const bcrypt = require('bcryptjs');
const pool = require('./db');
const { ensureSchema } = require('./lib/schema');
const { isStrongPassword } = require('./lib/validators');

// Ficha técnica por categoría (concentración, beneficios, modo de uso, certificaciones)
const CATEGORY_SHEETS = {
    'Líquidos y Aceites': {
        cbd: 'CBD 1000 mg / 30 ml (3,3 %)',
        benefits: 'Apoyo en dolor crónico, ansiedad e insomnio.',
        usage: 'Vía sublingual: 5 a 10 gotas, mantener 60 s bajo la lengua. Máximo 3 veces al día.',
        certs: 'Registro sanitario INVIMA · BPM · Análisis de laboratorio por lote'
    },
    'Comestibles': {
        cbd: 'CBD 10–25 mg por unidad',
        benefits: 'Dosificación exacta y discreta para manejo del estrés y el sueño.',
        usage: 'Vía oral: 1 unidad al día; efecto entre 30 y 90 minutos.',
        certs: 'Registro sanitario INVIMA · BPM alimentos'
    },
    'Tópicos': {
        cbd: 'CBD 500 mg por envase',
        benefits: 'Alivio local de dolor muscular y articular e inflamación cutánea.',
        usage: 'Uso externo: aplicar sobre piel limpia 2 a 3 veces al día. No ingerir.',
        certs: 'Notificación sanitaria obligatoria (NSO) INVIMA · Cosmético'
    },
    'Bienestar y Cuidado': {
        cbd: 'CBD 100–200 mg por unidad',
        benefits: 'Relajación y apoyo en terapias preventivas.',
        usage: 'Uso externo según indicaciones del empaque.',
        certs: 'NSO INVIMA · Cosmético'
    },
    'Concentrados y Otros': {
        cbd: 'CBD de alta pureza (≥ 80 %)',
        benefits: 'Fórmulas magistrales para dosificación personalizada bajo control médico.',
        usage: 'Únicamente según la dosis indicada en la fórmula médica.',
        certs: 'Registro sanitario INVIMA · Licencia de fabricación de derivados de cannabis'
    },
    'Bebidas y Suplementos': {
        cbd: 'CBD 10–20 mg por porción',
        benefits: 'Relajación sin los efectos del alcohol; apoyo digestivo.',
        usage: 'Vía oral: 1 porción al día.',
        certs: 'Registro sanitario INVIMA · BPM alimentos'
    },
    'Comestibles Especializados': {
        cbd: 'CBD 5–15 mg por porción',
        benefits: 'Suplemento para dietas medicinales de consumo constante.',
        usage: 'Vía oral, integrado en preparaciones; no exceder 2 porciones al día.',
        certs: 'Registro sanitario INVIMA · BPM alimentos'
    }
};

// [nombre, descripción, precio, categoría, imagen, stock, % THC]
const PRODUCTS = [
    ['Aceite de CBD Full Spectrum', 'El formato más común para administrar gotas sublinguales.', 65000, 'Líquidos y Aceites', 'prod_1', 40, 0.2],
    ['Nano-emulsiones de agua', 'Líquidos solubles en agua que se pueden mezclar con cualquier bebida.', 75000, 'Líquidos y Aceites', 'prod_2', 25, 0.1],
    ['Jarabe de CBD/THC', 'Utilizado a menudo para ayudar a conciliar el sueño.', 85000, 'Líquidos y Aceites', 'prod_3', 12, 5.0],
    ['Sprays Sublinguales', 'Rocíos que se aplican debajo de la lengua para una absorción rápida.', 58000, 'Líquidos y Aceites', 'prod_4', 30, 0.2],
    ['Gomitas (Gummies)', 'De sabores frutales, con dosis exactas de 10 mg o 25 mg.', 42000, 'Comestibles', 'prod_5', 60, 0],
    ['Chocolates Artesanales', 'Tabletas de chocolate con infusiones de cannabinoides.', 48000, 'Comestibles', 'prod_6', 35, 0],
    ['Cápsulas de Gel (Softgels)', 'Ideales para quienes no quieren sentir el sabor de la planta.', 55000, 'Comestibles', 'prod_7', 45, 0.2],
    ['Miel de Cannabis', 'Miel natural infusionada, usada como endulzante medicinal.', 62000, 'Comestibles', 'prod_8', 20, 0],
    ['Mentas Medicinales', 'Pequeñas pastillas para microdosis discretas durante el día.', 35000, 'Comestibles', 'prod_9', 50, 0],
    ['Bálsamos para Dolor Muscular', 'Cremas con efecto frío/calor para atletas o personas con artritis.', 59000, 'Tópicos', 'prod_10', 28, 0],
    ['Parches Transdérmicos', 'Liberan el compuesto de forma constante durante 12-24 horas.', 68000, 'Tópicos', 'prod_11', 18, 0],
    ['Aceites para Masaje', 'Enriquecidos con CBD para relajación profunda sin efectos psicoactivos.', 72000, 'Tópicos', 'prod_12', 22, 0],
    ['Lubricantes Medicinales', 'Diseñados para reducir el dolor o aumentar la sensibilidad.', 80000, 'Tópicos', 'prod_13', 15, 0],
    ['Sales de Baño (Epsom)', 'Para baños de inmersión relajantes y desinflamatorios.', 45000, 'Bienestar y Cuidado', 'prod_14', 30, 0],
    ['Bombas de Baño', 'Al contacto con el agua liberan aceites esenciales y cannabinoides.', 38000, 'Bienestar y Cuidado', 'prod_15', 40, 0],
    ['Mascarillas Faciales', 'Usadas en dermatología para reducir la inflamación cutánea.', 32000, 'Bienestar y Cuidado', 'prod_16', 3, 0],
    ['Destilado en Jeringas', 'Un aceite muy puro que se puede comer directamente o usar en recetas.', 110000, 'Concentrados y Otros', 'prod_17', 8, 2.0],
    ['Inhaladores', 'Entregan una dosis precisa de vapor sin combustión.', 95000, 'Concentrados y Otros', 'prod_18', 4, 1.5],
    ['Supositorios', 'Para pacientes con problemas gastrointestinales graves o dolores pélvicos.', 88000, 'Concentrados y Otros', 'prod_19', 10, 1.0],
    ['Polvos Hidrosolubles', 'Sobres de polvo que se disuelven en agua, ideales para llevar.', 60000, 'Concentrados y Otros', 'prod_20', 25, 0.2],
    ['Cerveza sin alcohol con CBD', 'Bebida refrescante con efecto relajante sin resaca.', 25000, 'Bebidas y Suplementos', 'prod_21', 48, 0],
    ['Kombucha infusionada', 'Mezcla de probióticos y cannabis para la salud digestiva.', 22000, 'Bebidas y Suplementos', 'prod_22', 36, 0],
    ['Café en grano con CBD', 'La alerta del café reduciendo la ansiedad de la cafeína.', 45000, 'Bebidas y Suplementos', 'prod_23', 30, 0],
    ['Té de hierbas (Tisanas)', 'Bolsitas de té con flores de cáñamo y plantas medicinales.', 30000, 'Bebidas y Suplementos', 'prod_24', 40, 0],
    ['Shots de energía', 'Dosis líquidas que combinan CBD con vitamina B12 y cafeína.', 18000, 'Bebidas y Suplementos', 'prod_25', 2, 0],
    ['Mantequilla (Cannabutter)', 'Base lista para cocinar o untar en dietas medicinales.', 78000, 'Comestibles Especializados', 'prod_26', 14, 0.2],
    ['Caramelos macizos', 'Ideales para una absorción lenta a través de la mucosa bucal.', 28000, 'Comestibles Especializados', 'prod_27', 55, 0],
    ['Aceite de Oliva infusionado', 'Para uso directo en ensaladas como suplemento nutricional.', 85000, 'Comestibles Especializados', 'prod_28', 16, 0.2],
    ['Harina de Cáñamo con CBD', 'Utilizada en repostería funcional para pacientes.', 40000, 'Comestibles Especializados', 'prod_29', 20, 0],
    ['Chicles medicinales', 'Liberación rápida de los compuestos al masticar.', 32000, 'Comestibles Especializados', 'prod_30', 45, 0]
];

async function migrate() {
    const demoPassword = process.env.DEMO_PASSWORD || `Demo#${crypto.randomBytes(4).toString('hex')}A1`;
    if (!isStrongPassword(demoPassword)) {
        console.error('❌ DEMO_PASSWORD no cumple la política (8+ caracteres, mayúscula, minúscula, número y especial)');
        process.exit(1);
    }

    try {
        console.log('🧹 Eliminando tablas...');
        for (const table of ['support_messages', 'restock_requests', 'stores', 'audit_logs', 'refresh_tokens',
            'password_resets', 'orders', 'prescriptions', 'products', 'users']) {
            await pool.query(`DROP TABLE IF EXISTS ${table} CASCADE`);
        }

        console.log('📋 Creando esquema...');
        await ensureSchema();

        console.log('🌱 Sembrando productos...');
        for (const [name, description, price, category, imageUrl, stock, thc] of PRODUCTS) {
            const sheet = CATEGORY_SHEETS[category];
            await pool.query(
                `INSERT INTO products (name, description, price, category, "imageUrl", stock, "minStock", "cbdConcentration",
                    "thcPercent", benefits, "usageMode", certifications, "requiresPrescription")
                 VALUES ($1,$2,$3,$4,$5,$6,5,$7,$8,$9,$10,$11,$12)`,
                [name, description, price, category, imageUrl, stock, sheet.cbd, thc, sheet.benefits, sheet.usage, sheet.certs, thc >= 1.0]
            );
        }

        console.log('🌱 Sembrando usuarios demo...');
        const hash = await bcrypt.hash(demoPassword, 10);
        const users = [
            ['user_demo_patient', 'Paciente Demo', 'paciente.herbhopper@gmail.com', 'PATIENT', '3001234567'],
            ['user_demo_seller', 'Botanical Precision', 'vendedor.herbhopper@gmail.com', 'SELLER', '3007654321'],
            ['user_demo_admin', 'Administrador Demo', 'admin.herbhopper@gmail.com', 'ADMIN', null]
        ];
        for (const [id, name, email, role, phone] of users) {
            await pool.query(
                'INSERT INTO users (id, name, email, password, role, phone) VALUES ($1,$2,$3,$4,$5,$6)',
                [id, name, email, hash, role, phone]
            );
        }

        await pool.query(`INSERT INTO stores (name, location, "licenseNumber", status) VALUES
            ('Botanical Precision', 'Bogotá, Calle 100', 'LIC-9823', 'VERIFIED'),
            ('Green Relief', 'Medellín, El Poblado', 'LIC-4512', 'PENDING'),
            ('Nature''s Pharmacy', 'Cali, Av. Sexta', 'LIC-7731', 'VERIFIED')`);

        console.log('🎉 Migración completada.');
        console.log(`🔑 Contraseña de las cuentas demo: ${demoPassword}`);
        process.exit(0);
    } catch (err) {
        console.error('❌ Error durante la migración:', err.message);
        process.exit(1);
    }
}

migrate();
