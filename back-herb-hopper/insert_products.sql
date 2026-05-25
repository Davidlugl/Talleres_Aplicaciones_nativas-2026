USE `herb-hopper-nativas`;

-- Limpiar tabla de productos para insertar los nuevos
TRUNCATE TABLE `products`;

INSERT INTO `products` (`id`, `name`, `description`, `price`, `category`, `imageUrl`) VALUES
(1, 'Aceite de CBD Full Spectrum', 'El formato más común para administrar gotas sublinguales.', 120000.0, 'Aceites', '1.png'),
(2, 'Nano-emulsiones de agua', 'Líquidos solubles en agua que se pueden mezclar con cualquier bebida.', 85000.0, 'Aceites', '2.png'),
(3, 'Jarabe de CBD/THC', 'Utilizado a menudo para ayudar a conciliar el sueño (estilo "lean" medicinal).', 95000.0, 'Aceites', '3.png'),
(4, 'Sprays Sublinguales', 'Rocíos que se aplican debajo de la lengua para una absorción rápida.', 70000.0, 'Aceites', '4.png'),
(5, 'Gomitas (Gummies)', 'De sabores frutales, generalmente con dosis exactas de 10mg o 25mg.', 45000.0, 'Comestibles', '5.png'),
(6, 'Chocolates Artesanales', 'Tabletas de chocolate negro o con leche con infusiones de cannabinoides.', 35000.0, 'Comestibles', '6.png'),
(7, 'Cápsulas de Gel (Softgels)', 'Ideales para quienes no quieren sentir el sabor de la planta.', 65000.0, 'Comestibles', '7.png'),
(8, 'Miel de Cannabis', 'Miel natural infusionada, usada como endulzante medicinal.', 55000.0, 'Comestibles', '8.png'),
(9, 'Mentas Medicinales', 'Pequeñas pastillas para microdosis discretas durante el día.', 25000.0, 'Comestibles', '9.png'),
(10, 'Bálsamos para Dolor Muscular', 'Cremas con efecto frío/calor para atletas o personas con artritis.', 48000.0, 'Tópicos', '10.png'),
(11, 'Parches Transdérmicos', 'Liberan el medicamento de forma constante durante 12-24 horas.', 32000.0, 'Tópicos', '11.png'),
(12, 'Aceites para Masaje', 'Enriquecidos con CBD para relajación profunda sin efectos psicoactivos.', 75000.0, 'Tópicos', '12.png'),
(13, 'Lubricantes Medicinales', 'Diseñados para reducir el dolor o aumentar la sensibilidad.', 60000.0, 'Tópicos', '13.png'),
(14, 'Sales de Baño (Epsom)', 'Para baños de inmersión relajantes y desinflamatorios.', 40000.0, 'Bienestar', '14.png'),
(15, 'Bombas de Baño', 'Al contacto con el agua liberan aceites esenciales y cannabinoides.', 22000.0, 'Bienestar', '15.png'),
(16, 'Mascarillas Faciales', 'Utilizadas en dermatología medicinal para reducir la inflamación cutánea.', 18000.0, 'Bienestar', '16.png'),
(17, 'Destilado en Jeringas', 'Un aceite muy puro que se puede comer directamente o usar en recetas.', 150000.0, 'Concentrados', '17.png'),
(18, 'Inhaladores', 'Entregan una dosis precisa de vapor sin combustión.', 110000.0, 'Concentrados', '18.png'),
(19, 'Supositorios', 'Para pacientes con problemas gastrointestinales graves o dolores pélvicos.', 80000.0, 'Concentrados', '19.png'),
(20, 'Polvos Hidrosolubles', 'Sobres de polvo que se disuelven en agua, ideales para llevar.', 30000.0, 'Concentrados', '20.png'),
(21, 'Cerveza sin alcohol con CBD', 'Bebidas refrescantes que buscan el efecto relajante.', 15000.0, 'Bebidas', '21.png'),
(22, 'Kombucha infusionada', 'Mezcla de probióticos y cannabis para la salud digestiva.', 18000.0, 'Bebidas', '22.png'),
(23, 'Café en grano con CBD', 'Diseñado para obtener la alerta del café sin la ansiedad.', 42000.0, 'Bebidas', '23.png'),
(24, 'Té de hierbas (Tisanas)', 'Bolsitas de té con flores de cáñamo y plantas medicinales.', 28000.0, 'Bebidas', '24.png'),
(25, 'Shots de energía', 'Pequeñas dosis líquidas con CBD, vitamina B12 y cafeína.', 12000.0, 'Bebidas', '25.png'),
(26, 'Mantequilla (Cannabutter)', 'Base lista para cocinar o untar en dietas medicinales.', 65000.0, 'Especializados', '26.png'),
(27, 'Caramelos macizos', 'Ideales para una absorción lenta a través de la mucosa bucal.', 15000.0, 'Especializados', '27.png'),
(28, 'Aceite de Oliva infusionado', 'Para uso directo en ensaladas como suplemento nutricional.', 78000.0, 'Especializados', '28.png'),
(29, 'Harina de Cáñamo con CBD', 'Utilizada en repostería funcional para pacientes.', 38000.0, 'Especializados', '29.png'),
(30, 'Chicles medicinales', 'Permiten una liberación rápida de los compuestos al masticar.', 20000.0, 'Especializados', '30.png');
