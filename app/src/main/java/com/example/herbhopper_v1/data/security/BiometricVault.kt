package com.example.herbhopper_v1.data.security

import android.content.Context

/**
 * Guarda el token de renovación para el acceso con huella. El token se cifra con una clave
 * AES-256 del Keystore que solo se desbloquea tras una autenticación biométrica fuerte
 * (BiometricPrompt). No se guarda la contraseña del usuario.
 *
 * Todas las operaciones de cifrado/descifrado deben ejecutarse dentro de los
 * [KeystoreCipher.AUTH_VALIDITY_SECONDS] segundos posteriores a la huella.
 */
class BiometricVault(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val cipher = KeystoreCipher(KEY_ALIAS, requireBiometric = true)

    fun isEnabled(): Boolean = prefs.contains(KEY_TOKEN)

    fun enabledEmail(): String? = prefs.getString(KEY_EMAIL, null)

    /** Llamar justo después de un BiometricPrompt exitoso. */
    fun store(email: String, refreshToken: String) {
        prefs.edit()
            .putString(KEY_TOKEN, cipher.encrypt(refreshToken))
            .putString(KEY_EMAIL, email)
            .apply()
    }

    /** Llamar justo después de un BiometricPrompt exitoso. Devuelve null si la clave fue invalidada. */
    fun read(): String? {
        val stored = prefs.getString(KEY_TOKEN, null) ?: return null
        return try {
            cipher.decrypt(stored)
        } catch (e: Exception) {
            // Clave invalidada (p. ej. se registró una huella nueva): hay que volver a activar
            disable()
            null
        }
    }

    fun disable() {
        prefs.edit().clear().apply()
        cipher.deleteKey()
    }

    companion object {
        private const val PREF_NAME = "herbhopper_biometric"
        private const val KEY_TOKEN = "refresh_token_bio"
        private const val KEY_EMAIL = "email"
        private const val KEY_ALIAS = "herbhopper_biometric_key"
    }
}
