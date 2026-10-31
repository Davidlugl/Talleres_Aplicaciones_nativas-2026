// Reglas de validación compartidas (las mismas que aplica la app Android).

// Dominios de correo admitidos por el manual de usuario.
const ALLOWED_EMAIL_DOMAINS = [
    'gmail.com', 'outlook.com', 'outlook.es', 'hotmail.com', 'hotmail.es',
    'yahoo.com', 'yahoo.es', 'icloud.com'
];

const EMAIL_REGEX = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;

// Mínimo 8 caracteres, una mayúscula, una minúscula, un número y un carácter especial.
const PASSWORD_REGEX = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,}$/;

function isValidEmail(email) {
    if (typeof email !== 'string') return false;
    const normalized = email.trim().toLowerCase();
    if (!EMAIL_REGEX.test(normalized)) return false;
    const domain = normalized.split('@')[1];
    return ALLOWED_EMAIL_DOMAINS.includes(domain);
}

function isStrongPassword(password) {
    return typeof password === 'string' && PASSWORD_REGEX.test(password);
}

function isValidPhone(phone) {
    // Celular colombiano: 10 dígitos que empiezan por 3 (se admite prefijo +57)
    if (phone == null || phone === '') return true;
    const digits = String(phone).replace(/\D/g, '').replace(/^57(?=\d{10}$)/, '');
    return /^3\d{9}$/.test(digits);
}

// Elimina etiquetas HTML para evitar XSS almacenado en textos libres.
function sanitizeText(value, maxLength = 1000) {
    if (value == null) return null;
    return String(value).replace(/<[^>]*>/g, '').trim().slice(0, maxLength);
}

module.exports = {
    ALLOWED_EMAIL_DOMAINS,
    isValidEmail,
    isStrongPassword,
    isValidPhone,
    sanitizeText
};
