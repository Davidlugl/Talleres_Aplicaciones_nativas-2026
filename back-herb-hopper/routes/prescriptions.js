const express = require('express');
const multer = require('multer');
const router = express.Router();
const pool = require('../db');
const cloudinary = require('../cloudinary');
const { authenticate, requireRole } = require('../middleware/auth');
const { logEvent } = require('../lib/audit');
const { sanitizeText } = require('../lib/validators');

const REVIEW_WINDOW_MS = 24 * 60 * 60 * 1000;
const ALLOWED_TYPES = ['image/jpeg', 'image/png', 'image/webp', 'application/pdf'];

const upload = multer({
    storage: multer.memoryStorage(),
    limits: { fileSize: 10 * 1024 * 1024 },
    fileFilter: (req, file, cb) => cb(null, ALLOWED_TYPES.includes(file.mimetype))
});

/**
 * La fórmula es un dato de salud (dato sensible, Ley 1581): se guarda en Cloudinary
 * como recurso "authenticated" y solo se entrega mediante URL firmada que vence en 10 minutos.
 */
function signedUrl(row) {
    return cloudinary.url(row.publicId, {
        type: 'authenticated',
        resource_type: row.resourceType,
        format: row.fileFormat || undefined,
        sign_url: true,
        secure: true,
        expires_at: Math.floor(Date.now() / 1000) + 600
    });
}

function present(row, includeFile) {
    const result = {
        id: row.id,
        userId: row.userId,
        patientName: row.patientName,
        doctorName: row.doctorName,
        doctorRegistry: row.doctorRegistry,
        status: row.status,
        reviewNote: row.reviewNote,
        createdAt: Number(row.createdAt),
        reviewedAt: row.reviewedAt ? Number(row.reviewedAt) : null,
        reviewDeadline: Number(row.createdAt) + REVIEW_WINDOW_MS,
        fileFormat: row.fileFormat
    };
    if (includeFile) result.fileUrl = signedUrl(row);
    return result;
}

router.use(authenticate);

// Paciente: carga la fórmula (foto de cámara, imagen de galería o PDF)
router.post('/', requireRole('PATIENT'), upload.single('file'), async (req, res) => {
    const doctorName = sanitizeText(req.body.doctorName, 255);
    const doctorRegistry = sanitizeText(req.body.doctorRegistry, 100);
    if (!req.file) {
        return res.status(400).json({ error: 'Adjunte una imagen (JPG/PNG) o un PDF de máximo 10 MB' });
    }
    if (!doctorName || !doctorRegistry) {
        return res.status(400).json({ error: 'El nombre y el registro médico del profesional son obligatorios' });
    }

    const isPdf = req.file.mimetype === 'application/pdf';
    cloudinary.uploader.upload_stream(
        {
            folder: 'prescriptions',
            type: 'authenticated',
            resource_type: isPdf ? 'raw' : 'image',
            format: isPdf ? 'pdf' : undefined
        },
        async (error, result) => {
            if (error) {
                console.error('Error al subir la fórmula:', error.message);
                return res.status(500).json({ error: 'No se pudo guardar la fórmula en la nube' });
            }
            try {
                const inserted = await pool.query(
                    `INSERT INTO prescriptions ("userId", "doctorName", "doctorRegistry", "publicId", "resourceType", "fileFormat", "createdAt")
                     VALUES ($1,$2,$3,$4,$5,$6,$7) RETURNING *`,
                    [req.user.id, doctorName, doctorRegistry, result.public_id, result.resource_type, isPdf ? 'pdf' : result.format, Date.now()]
                );
                await logEvent('PRESCRIPTION', `Fórmula #${inserted.rows[0].id} cargada para revisión`, { userId: req.user.id });
                res.status(201).json(present(inserted.rows[0], false));
            } catch (dbError) {
                res.status(500).json({ error: dbError.message });
            }
        }
    ).end(req.file.buffer);
});

// Paciente: sus fórmulas. Vendedor/Administrador: todas, con enlace firmado para auditarlas.
router.get('/', async (req, res) => {
    try {
        const isStaff = ['SELLER', 'ADMIN'].includes(req.user.role);
        const query = `SELECT p.*, u.name AS "patientName" FROM prescriptions p JOIN users u ON u.id = p."userId"`;
        const result = isStaff
            ? await pool.query(`${query} ORDER BY p."createdAt" DESC`)
            : await pool.query(`${query} WHERE p."userId" = $1 ORDER BY p."createdAt" DESC`, [req.user.id]);
        res.json(result.rows.map(row => present(row, isStaff)));
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// Farmacéutico (vendedor) o administrador: aprueba o rechaza tras verificar registro, sello y firma
router.put('/:id/review', requireRole('SELLER', 'ADMIN'), async (req, res) => {
    const { status, note } = req.body;
    if (!['APPROVED', 'REJECTED'].includes(status)) {
        return res.status(400).json({ error: 'El estado debe ser APPROVED o REJECTED' });
    }
    if (status === 'REJECTED' && !note) {
        return res.status(400).json({ error: 'Indique el motivo del rechazo' });
    }
    try {
        const result = await pool.query(
            `UPDATE prescriptions SET status = $1, "reviewNote" = $2, "reviewedBy" = $3, "reviewedAt" = $4
             WHERE id = $5 RETURNING *`,
            [status, sanitizeText(note, 500), req.user.id, Date.now(), req.params.id]
        );
        if (result.rows.length === 0) return res.status(404).json({ error: 'Fórmula no encontrada' });
        await logEvent('PRESCRIPTION', `Fórmula #${req.params.id} → ${status}`, { userId: req.user.id });
        res.json(present(result.rows[0], false));
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

module.exports = router;
