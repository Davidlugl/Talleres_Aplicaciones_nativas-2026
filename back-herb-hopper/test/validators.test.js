// Ejecutar con: npm test
const test = require('node:test');
const assert = require('node:assert');
const v = require('../lib/validators');

test('solo acepta correos de los proveedores del manual', () => {
    assert.ok(v.isValidEmail('ana@gmail.com'));
    assert.ok(v.isValidEmail(' Ana@Outlook.com '));
    assert.ok(v.isValidEmail('ana@icloud.com'));
    assert.ok(!v.isValidEmail('gmail@falso.com'));
    assert.ok(!v.isValidEmail('x@gmail.com.evil.co'));
    assert.ok(!v.isValidEmail('sin-arroba'));
});

test('exige contraseña con mayúscula, minúscula, número y carácter especial', () => {
    assert.ok(v.isStrongPassword('Clave#2026'));
    assert.ok(!v.isStrongPassword('clave#2026'));
    assert.ok(!v.isStrongPassword('Clave2026'));
    assert.ok(!v.isStrongPassword('Cl#1'));
    assert.ok(!v.isStrongPassword(''));
});

test('valida celulares colombianos de 10 dígitos', () => {
    assert.ok(v.isValidPhone('3001234567'));
    assert.ok(v.isValidPhone('+57 300 123 4567'));
    assert.ok(!v.isValidPhone('123'));
    assert.ok(!v.isValidPhone('2001234567'));
});

test('elimina etiquetas HTML de los textos libres', () => {
    assert.strictEqual(v.sanitizeText('<b>Hola</b>'), 'Hola');
    assert.strictEqual(v.sanitizeText(null), null);
});
