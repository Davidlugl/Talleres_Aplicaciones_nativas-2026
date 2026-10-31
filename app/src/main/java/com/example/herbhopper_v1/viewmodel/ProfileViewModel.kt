package com.example.herbhopper_v1.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.herbhopper_v1.data.AppDatabase
import com.example.herbhopper_v1.data.SessionManager
import com.example.herbhopper_v1.data.UserDao
import com.example.herbhopper_v1.data.UserProfile
import com.example.herbhopper_v1.data.network.ApiService
import com.example.herbhopper_v1.data.network.AuthResponse
import com.example.herbhopper_v1.data.network.LoginRequest
import com.example.herbhopper_v1.data.network.RefreshRequest
import com.example.herbhopper_v1.data.network.RegisterRequest
import com.example.herbhopper_v1.data.network.UpdateMeRequest
import com.example.herbhopper_v1.data.network.apiErrorMessage
import com.example.herbhopper_v1.data.security.BiometricVault
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * Autenticación y CRUD del perfil propio contra el backend (JWT).
 * Room guarda una copia local del perfil para mostrarlo sin conexión.
 */
class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val userDao: UserDao = AppDatabase.getDatabase(application).userDao()
    private val api = ApiService.instance
    val biometricVault = BiometricVault(application)

    fun observeProfile(uid: String): Flow<UserProfile?> {
        syncFromBackend()
        return userDao.observeUserProfile(uid)
    }

    private fun syncFromBackend() {
        if (!SessionManager.isLoggedIn()) return
        viewModelScope.launch {
            try {
                val me = api.getMe()
                val local = userDao.getUserProfile(me.id)
                userDao.insertProfile(
                    (local ?: UserProfile(uid = me.id, name = me.name, email = me.email, role = me.role))
                        .copy(name = me.name, email = me.email, role = me.role, phone = me.phone)
                )
            } catch (e: Exception) {
                // Sin conexión: se usa la copia local
            }
        }
    }

    /**
     * Guarda nombre y teléfono en el servidor y en la copia local.
     * [onResult] recibe null si todo salió bien o el mensaje de error.
     */
    fun saveProfile(profile: UserProfile, onResult: (String?) -> Unit = {}) {
        viewModelScope.launch {
            try {
                api.updateMe(UpdateMeRequest(name = profile.name, phone = profile.phone))
                userDao.insertProfile(profile)
                SessionManager.updateProfile(profile.name, profile.phone)
                onResult(null)
            } catch (e: Exception) {
                onResult(e.apiErrorMessage())
            }
        }
    }

    /** Datos locales que no viajan al servidor (dirección preferida, tarjeta enmascarada). */
    fun saveLocalPreferences(profile: UserProfile) {
        viewModelScope.launch { userDao.insertProfile(profile) }
    }

    fun changePassword(newPassword: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                api.updateMe(UpdateMeRequest(password = newPassword))
                // El servidor revoca las sesiones guardadas: hay que volver a activar la huella
                biometricVault.disable()
                onResult(null)
            } catch (e: Exception) {
                onResult(e.apiErrorMessage())
            }
        }
    }

    /** Derecho de supresión (Ley 1581): elimina la cuenta en el servidor y los datos locales. */
    fun deleteAccount(onResult: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                api.deleteMe()
                biometricVault.disable()
                AppDatabase.getDatabase(getApplication()).clearAllTables()
                SessionManager.logout()
                onResult(null)
            } catch (e: Exception) {
                onResult(e.apiErrorMessage())
            }
        }
    }

    suspend fun getProfile(uid: String): UserProfile? = userDao.getUserProfile(uid)

    suspend fun getProfileByEmail(email: String): UserProfile? = userDao.getUserProfileByEmail(email)

    suspend fun loginToBackend(email: String, password: String): Result<UserProfile> = try {
        Result.success(startSession(api.login(LoginRequest(email.trim().lowercase(), password))))
    } catch (e: Exception) {
        Result.failure(Exception(e.apiErrorMessage("No se pudo conectar con el servidor")))
    }

    /**
     * Pide al servidor un token de renovación exclusivo para la bóveda biométrica
     * (distinto al de la sesión, para que la rotación de uno no invalide el otro).
     */
    suspend fun requestDeviceToken(): Result<String> = try {
        Result.success(api.issueDeviceToken().deviceToken)
    } catch (e: Exception) {
        Result.failure(Exception(e.apiErrorMessage()))
    }

    /**
     * Acceso con huella: el token descifrado tras la huella se cambia por una sesión nueva.
     * El servidor lo revoca y entrega otro token de dispositivo, que se guarda de nuevo en la
     * bóveda (la clave sigue desbloqueada unos segundos después de la huella).
     */
    suspend fun loginWithBiometricToken(deviceToken: String): Result<UserProfile> = try {
        val auth = api.refresh(RefreshRequest(deviceToken, device = true))
        val email = biometricVault.enabledEmail() ?: auth.user.email
        try {
            auth.deviceToken?.let { biometricVault.store(email, it) } ?: biometricVault.disable()
        } catch (e: Exception) {
            biometricVault.disable()
        }
        Result.success(startSession(auth))
    } catch (e: Exception) {
        biometricVault.disable()
        Result.failure(Exception(e.apiErrorMessage("La sesión guardada venció. Inicia sesión con tu contraseña.")))
    }

    suspend fun registerToBackend(
        name: String,
        email: String,
        password: String,
        role: String = "PATIENT",
        phone: String? = null
    ): Result<UserProfile> = try {
        Result.success(startSession(api.register(RegisterRequest(name, email.trim().lowercase(), password, role, phone))))
    } catch (e: Exception) {
        Result.failure(Exception(e.apiErrorMessage("No se pudo conectar con el servidor")))
    }

    private suspend fun startSession(auth: AuthResponse): UserProfile {
        val user = auth.user
        val profile = (userDao.getUserProfile(user.id) ?: UserProfile(uid = user.id, name = user.name, email = user.email, role = user.role))
            .copy(name = user.name, email = user.email, role = user.role, phone = user.phone)
        userDao.insertProfile(profile)
        SessionManager.login(profile.uid, profile.email, profile.name, profile.role, profile.phone, auth.token, auth.refreshToken)
        return profile
    }

    /**
     * Cierra la sesión y revoca su token en el servidor. El token de la bóveda biométrica
     * es independiente, así que el acceso con huella sigue disponible.
     */
    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            SessionManager.getRefreshToken()?.let { refreshToken ->
                try { api.logout(RefreshRequest(refreshToken)) } catch (e: Exception) { }
            }
            AppDatabase.getDatabase(getApplication()).orderDao().deleteAll()
            SessionManager.logout()
            onDone()
        }
    }
}
