package com.example.herbhopper_v1.data

import android.content.Context
import android.content.SharedPreferences
import com.example.herbhopper_v1.data.security.KeystoreCipher

/**
 * Sesión del usuario. Los datos de perfil se guardan en SharedPreferences privadas y los
 * tokens (JWT de acceso y token de renovación) cifrados con AES-256-GCM del Android Keystore.
 * Nunca se guarda la contraseña.
 */
object SessionManager {
    private const val PREF_NAME = "herbhopper_session"
    private const val KEY_UID = "uid"
    private const val KEY_EMAIL = "email"
    private const val KEY_NAME = "name"
    private const val KEY_ROLE = "role"
    private const val KEY_PHONE = "phone"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_ACCESS_TOKEN = "access_token_enc"
    private const val KEY_REFRESH_TOKEN = "refresh_token_enc"

    private var preferences: SharedPreferences? = null
    private val tokenCipher by lazy { KeystoreCipher("herbhopper_session_key") }

    fun init(context: Context) {
        if (preferences == null) {
            preferences = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        }
    }

    fun login(
        uid: String,
        email: String,
        name: String,
        role: String,
        phone: String? = null,
        accessToken: String,
        refreshToken: String
    ) {
        preferences?.edit()?.apply {
            putString(KEY_UID, uid)
            putString(KEY_EMAIL, email)
            putString(KEY_NAME, name)
            putString(KEY_ROLE, role)
            putString(KEY_PHONE, phone)
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_ACCESS_TOKEN, tokenCipher.encrypt(accessToken))
            putString(KEY_REFRESH_TOKEN, tokenCipher.encrypt(refreshToken))
            apply()
        }
    }

    fun updateTokens(accessToken: String, refreshToken: String) {
        preferences?.edit()?.apply {
            putString(KEY_ACCESS_TOKEN, tokenCipher.encrypt(accessToken))
            putString(KEY_REFRESH_TOKEN, tokenCipher.encrypt(refreshToken))
            apply()
        }
    }

    fun updateProfile(name: String, phone: String?) {
        preferences?.edit()?.apply {
            putString(KEY_NAME, name)
            putString(KEY_PHONE, phone)
            apply()
        }
    }

    fun logout() {
        preferences?.edit()?.apply {
            clear()
            apply()
        }
    }

    fun isLoggedIn(): Boolean = preferences?.getBoolean(KEY_IS_LOGGED_IN, false) ?: false

    fun getAccessToken(): String? = readEncrypted(KEY_ACCESS_TOKEN)

    fun getRefreshToken(): String? = readEncrypted(KEY_REFRESH_TOKEN)

    private fun readEncrypted(key: String): String? {
        val stored = preferences?.getString(key, null) ?: return null
        return try {
            tokenCipher.decrypt(stored)
        } catch (e: Exception) {
            null
        }
    }

    fun getUid(): String = preferences?.getString(KEY_UID, null) ?: ""

    fun getEmail(): String = preferences?.getString(KEY_EMAIL, null) ?: ""

    fun getName(): String = preferences?.getString(KEY_NAME, null) ?: ""

    fun getRole(): String = preferences?.getString(KEY_ROLE, null) ?: "PATIENT"

    fun getPhone(): String? = preferences?.getString(KEY_PHONE, null)
}
