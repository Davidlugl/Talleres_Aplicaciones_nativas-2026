const pool = require('./db');

async function migrate() {
    console.log('🔄 Iniciando migración a PostgreSQL en Render...');
    
    // Conexión inicial para validar
    try {
        const testConn = await pool.query('SELECT NOW()');
        console.log('✅ Conexión exitosa a Render PostgreSQL a las:', testConn.rows[0].now);
    } catch (err) {
        console.error('❌ Error de conexión inicial:', err.message);
        process.exit(1);
    }

    try {
        // 1. Limpieza de tablas antiguas (en orden inverso de claves foráneas)
        console.log('🧹 Limpiando tablas antiguas si existen...');
        await pool.query('DROP TABLE IF EXISTS orders CASCADE');
        await pool.query('DROP TABLE IF EXISTS products CASCADE');
        await pool.query('DROP TABLE IF EXISTS users CASCADE');
        console.log('✅ Tablas antiguas eliminadas.');

        // 2. Crear tabla de usuarios
        console.log('📋 Creando tabla "users"...');
        await pool.query(`
            CREATE TABLE users (
                id VARCHAR(255) PRIMARY KEY,
                name VARCHAR(255) NOT NULL,
                email VARCHAR(255) NOT NULL UNIQUE,
                password VARCHAR(255) NOT NULL,
                role VARCHAR(50) DEFAULT 'PATIENT'
            )
        `);

        // 3. Crear tabla de productos
        console.log('📋 Creando tabla "products"...');
        await pool.query(`
            CREATE TABLE products (
                id SERIAL PRIMARY KEY,
                name VARCHAR(255) NOT NULL,
                description TEXT,
                price DOUBLE PRECISION NOT NULL,
                category VARCHAR(100) NOT NULL,
                "imageUrl" VARCHAR(255)
            )
        `);

        // 4. Crear tabla de órdenes
        console.log('📋 Creando tabla "orders"...');
        await pool.query(`
            CREATE TABLE orders (
                "orderId" VARCHAR(255) PRIMARY KEY,
                "userId" VARCHAR(255) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                "userName" VARCHAR(255) NOT NULL,
                "itemsJson" TEXT NOT NULL,
                "totalAmount" DOUBLE PRECISION NOT NULL,
                status VARCHAR(50) DEFAULT 'PENDING',
                timestamp BIGINT NOT NULL,
                address TEXT,
                "paymentMethod" VARCHAR(100)
            )
        `);
        console.log('✅ Esquemas de tablas creados en PostgreSQL.');

        // 5. Sembrar productos (Seed Data)
        console.log('🌱 Sembrando datos de productos...');
        const productsList = [
            [1, 'Aceite de CBD Full Spectrum', 'El formato más común para administrar gotas sublinguales.', 120000.0, 'Aceites', '1.png'],
            [2, 'Nano-emulsiones de agua', 'Líquidos solubles en agua que se pueden mezclar con cualquier bebida.', 85000.0, 'Aceites', '2.png'],
            [3, 'Jarabe de CBD/THC', 'Utilizado a menudo para ayudar a conciliar el sueño (estilo "lean" medicinal).', 95000.0, 'Aceites', '3.png'],
            [4, 'Sprays Sublinguales', 'Rocíos que se aplican debajo de la lengua para una absorción rápida.', 70000.0, 'Aceites', '4.png'],
            [5, 'Gomitas (Gummies)', 'De sabores frutales, generalmente con dosis exactas de 10mg o 25mg.', 45000.0, 'Comestibles', '5.png'],
            [6, 'Chocolates Artesanales', 'Tabletas de chocolate negro o con leche con infusiones de cannabinoides.', 35000.0, 'Comestibles', '6.png'],
            [7, 'Cápsulas de Gel (Softgels)', 'Ideales para quienes no quieren sentir el sabor de la planta.', 65000.0, 'Comestibles', '7.png'],
            [8, 'Miel de Cannabis', 'Miel natural infusionada, usada como endulzante medicinal.', 55000.0, 'Comestibles', '8.png'],
            [9, 'Mentas Medicinales', 'Pequeñas pastillas para microdosis discretas durante el día.', 25000.0, 'Comestibles', '9.png'],
            [10, 'Bálsamos para Dolor Muscular', 'Cremas con efecto frío/calor para atletas o personas con artritis.', 48000.0, 'Tópicos', '10.png'],
            [11, 'Parches Transdérmicos', 'Liberan el medicamento de forma constante durante 12-24 horas.', 32000.0, 'Tópicos', '11.png'],
            [12, 'Aceites para Masaje', 'Enriquecidos con CBD para relajación profunda sin efectos psicoactivos.', 75000.0, 'Tópicos', '12.png'],
            [13, 'Lubricantes Medicinales', 'Diseñados para reducir el dolor o aumentar la sensibilidad.', 60000.0, 'Tópicos', '13.png'],
            [14, 'Sales de Baño (Epsom)', 'Para baños de inmersión relajantes y desinflamatorios.', 40000.0, 'Bienestar', '14.png'],
            [15, 'Bombas de Baño', 'Al contacto con el agua liberan aceites esenciales y cannabinoides.', 22000.0, 'Bienestar', '15.png'],
            [16, 'Mascarillas Faciales', 'Utilizadas en dermatología medicinal para reducir la inflamación cutánea.', 18000.0, 'Bienestar', '16.png'],
            [17, 'Destilado en Jeringas', 'Un aceite muy puro que se puede comer directamente o usar en recetas.', 150000.0, 'Concentrados', '17.png'],
            [18, 'Inhaladores', 'Entregan una dosis precisa de vapor sin combustión.', 110000.0, 'Concentrados', '18.png'],
            [19, 'Supositorios', 'Para pacientes con problemas gastrointestinales graves o dolores pélvicos.', 80000.0, 'Concentrados', '19.png'],
            [20, 'Polvos Hidrosolubles', 'Sobres de polvo que se disuelven en agua, ideales para llevar.', 30000.0, 'Concentrados', '20.png'],
            [21, 'Cerveza sin alcohol con CBD', 'Bebidas refrescantes que buscan el efecto relajante.', 15000.0, 'Bebidas', '21.png'],
            [22, 'Kombucha infusionada', 'Mezcla de probióticos y cannabis para la salud digestiva.', 18000.0, 'Bebidas', '22.png'],
            [23, 'Café en grano con CBD', 'Diseñado para obtener la alerta del café sin la ansiedad.', 42000.0, 'Bebidas', '23.png'],
            [24, 'Té de hierbas (Tisanas)', 'Bolsitas de té con flores de cáñamo y plantas medicinales.', 28000.0, 'Bebidas', '24.png'],
            [25, 'Shots de energía', 'Pequeñas dosis líquidas con CBD, vitamina B12 y cafeína.', 12000.0, 'Bebidas', '25.png'],
            [26, 'Mantequilla (Cannabutter)', 'Base lista para cocinar o untar en dietas medicinales.', 65000.0, 'Especializados', '26.png'],
            [27, 'Caramelos macizos', 'Ideales para una absorción lenta a través de la mucosa bucal.', 15000.0, 'Especializados', '27.png'],
            [28, 'Aceite de Oliva infusionado', 'Para uso directo en ensaladas como suplemento nutricional.', 78000.0, 'Especializados', '28.png'],
            [29, 'Harina de Cáñamo con CBD', 'Utilizada en repostería funcional para pacientes.', 38000.0, 'Especializados', '29.png'],
            [30, 'Chicles medicinales', 'Permiten una liberación rápida de los compuestos al masticar.', 20000.0, 'Especializados', '30.png']
        ];

        for (const prod of productsList) {
            await pool.query(
                'INSERT INTO products (id, name, description, price, category, "imageUrl") VALUES ($1, $2, $3, $4, $5, $6)',
                prod
            );
        }
        
        // Ajustar secuencia de ID de productos en Postgres ya que forzamos los IDs manuales inicialmente
        await pool.query("SELECT setval('products_id_seq', COALESCE((SELECT MAX(id)+1 FROM products), 1), false)");
        console.log('✅ 30 productos sembrados.');

        // 6. Sembrar usuarios de prueba
        console.log('🌱 Sembrando usuarios demo...');
        await pool.query(`
            INSERT INTO users (id, name, email, password, role) VALUES 
            ('user_demo_patient', 'Paciente Demo', 'paciente@demo.com', 'password123', 'PATIENT'),
            ('user_demo_admin', 'Administrador Demo', 'admin@demo.com', 'password123', 'ADMIN')
            ON CONFLICT (id) DO NOTHING
        `);
        console.log('✅ Usuarios de prueba sembrados.');

        console.log('🎉 ¡Migración completada exitosamente a Render PostgreSQL!');
        process.exit(0);
    } catch (err) {
        console.error('❌ Error durante la migración:', err.message);
        process.exit(1);
    }
}

migrate();