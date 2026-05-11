-- Script SQL para importar en phpMyAdmin
-- Base de datos: `herb-hopper-nativas`

CREATE DATABASE IF NOT EXISTS `herb-hopper-nativas` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `herb-hopper-nativas`;

-- Estructura de tabla para `products`
CREATE TABLE IF NOT EXISTS `products` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `description` text,
  `price` double NOT NULL,
  `category` varchar(100) NOT NULL,
  `imageUrl` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Estructura de tabla para `users`
CREATE TABLE IF NOT EXISTS `users` (
  `id` varchar(255) NOT NULL,
  `name` varchar(255) NOT NULL,
  `email` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL,
  `role` varchar(50) DEFAULT 'PATIENT',
  PRIMARY KEY (`id`),
  UNIQUE KEY `email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Estructura de tabla para `orders`
CREATE TABLE IF NOT EXISTS `orders` (
  `orderId` varchar(255) NOT NULL,
  `userId` varchar(255) NOT NULL,
  `userName` varchar(255) NOT NULL,
  `itemsJson` text NOT NULL,
  `totalAmount` double NOT NULL,
  `status` varchar(50) DEFAULT 'PENDING',
  `timestamp` bigint(20) NOT NULL,
  `address` text,
  `paymentMethod` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`orderId`),
  KEY `userId` (`userId`),
  CONSTRAINT `orders_ibfk_1` FOREIGN KEY (`userId`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Insertar datos semilla (opcional)
INSERT INTO `products` (`name`, `description`, `price`, `category`, `imageUrl`) VALUES
('Aceite Aura Blend 15%', 'Aceite CBD de espectro completo 1500mg.', 64500, 'Aceite', 'url_aceite'),
('Flor Silver Haze Premium', 'Flor curada de grado medicinal.', 45000, 'Flor', 'url_flor'),
('Cápsulas Night Cap', 'Cápsulas para el descanso nocturno.', 32000, 'Cápsulas', 'url_capsulas');
