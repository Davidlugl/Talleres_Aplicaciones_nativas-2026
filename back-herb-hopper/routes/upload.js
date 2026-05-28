const express = require('express');
const router = express.Router();
const multer = require('multer');
const cloudinary = require('../cloudinary');

// Configurar multer para almacenar temporalmente en memoria para evitar archivos residuales
const storage = multer.memoryStorage();
const upload = multer({
    storage: storage,
    limits: { fileSize: 5 * 1024 * 1024 } // límite de 5MB
});

// Endpoint para subir imagen a Cloudinary en la carpeta por defecto
router.post('/', upload.single('image'), (req, res) => {
    if (!req.file) {
        return res.status(400).json({ error: 'No se proporcionó ningún archivo de imagen' });
    }

    // Subir a Cloudinary utilizando un flujo de bytes (buffer stream)
    const uploadStream = cloudinary.uploader.upload_stream(
        {
            folder: 'ecomers-products',
            resource_type: 'image'
        },
        (error, result) => {
            if (error) {
                console.error('❌ Error al subir a Cloudinary:', error);
                return res.status(500).json({ error: 'Fallo al subir la imagen al servicio en la nube' });
            }
            res.json({
                message: 'Imagen subida exitosamente',
                url: result.secure_url,
                public_id: result.public_id
            });
        }
    );

    uploadStream.end(req.file.buffer);
});

module.exports = router;
