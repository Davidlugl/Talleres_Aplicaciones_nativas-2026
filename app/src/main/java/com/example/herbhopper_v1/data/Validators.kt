package com.example.herbhopper_v1.data

/**
 * Reglas de validación de formularios. Son las mismas que aplica el backend
 * (back-herb-hopper/lib/validators.js), que vuelve a validar todo lo que recibe.
 */
object Validators {
    /** Proveedores de correo admitidos por el manual de usuario. */
    val ALLOWED_EMAIL_DOMAINS = listOf(
        "gmail.com", "outlook.com", "outlook.es", "hotmail.com", "hotmail.es",
        "yahoo.com", "yahoo.es", "icloud.com"
    )

    private val EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()

    /** Mínimo 8 caracteres, una mayúscula, una minúscula, un número y un carácter especial. */
    private val PASSWORD_REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$".toRegex()

    fun isValidEmail(email: String): Boolean {
        val normalized = email.trim().lowercase()
        if (!normalized.matches(EMAIL_REGEX)) return false
        return normalized.substringAfter("@") in ALLOWED_EMAIL_DOMAINS
    }

    fun isStrongPassword(password: String): Boolean = password.matches(PASSWORD_REGEX)

    /** Lista de requisitos que la contraseña aún no cumple (para mostrar ayuda en el formulario). */
    fun missingPasswordRules(password: String): List<String> = buildList {
        if (password.length < 8) add("8 caracteres")
        if (!password.any { it.isUpperCase() }) add("una mayúscula")
        if (!password.any { it.isLowerCase() }) add("una minúscula")
        if (!password.any { it.isDigit() }) add("un número")
        if (password.all { it.isLetterOrDigit() }) add("un carácter especial")
    }

    /** Celular colombiano: 10 dígitos que empiezan por 3. */
    fun isValidColombianMobile(digits: String): Boolean = digits.length == 10 && digits.startsWith("3") && digits.all { it.isDigit() }
}
