package com.example.herbhopper_v1.data

import android.content.Context
import android.content.SharedPreferences

object SessionManager {
    private const val PREF_NAME = "herbhopper_session"
    private const val KEY_UID = "uid"
    private const val KEY_EMAIL = "email"
    private const val KEY_NAME = "name"
    private const val KEY_ROLE = "role"
    private const val KEY_PHONE = "phone"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"

    private var preferences: SharedPreferences? = null

    fun init(context: Context) {
        if (preferences == null) {
            preferences = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        }
    }

    fun login(uid: String, email: String, name: String, role: String, phone: String? = null) {
        preferences?.edit()?.apply {
            putString(KEY_UID, uid)
            putString(KEY_EMAIL, email)
            putString(KEY_NAME, name)
            putString(KEY_ROLE, role)
            putString(KEY_PHONE, phone)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }

    fun logout() {
        preferences?.edit()?.apply {
            clear()
            apply()
        }
    }

    fun isLoggedIn(): Boolean {
        return preferences?.getBoolean(KEY_IS_LOGGED_IN, false) ?: false
    }

    fun getUid(): String {
        return preferences?.getString(KEY_UID, null) ?: ""
    }

    fun getEmail(): String {
        return preferences?.getString(KEY_EMAIL, null) ?: ""
    }

    fun getName(): String {
        return preferences?.getString(KEY_NAME, null) ?: ""
    }

    fun getRole(): String {
        return preferences?.getString(KEY_ROLE, null) ?: "PATIENT"
    }

    fun getPhone(): String? {
        return preferences?.getString(KEY_PHONE, null)
    }
}
