package com.example.herbhopper_v1.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.herbhopper_v1.data.AppDatabase
import com.example.herbhopper_v1.data.UserProfile
import com.example.herbhopper_v1.data.UserDao
import com.example.herbhopper_v1.data.SessionManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val userDao: UserDao = AppDatabase.getDatabase(application).userDao()
    private val firestore = try { FirebaseFirestore.getInstance() } catch (e: Exception) { null }

    fun observeProfile(uid: String): Flow<UserProfile?> {
        // Primero intentamos sincronizar desde la nube
        syncFromCloud(uid)
        return userDao.observeUserProfile(uid)
    }

    private fun syncFromCloud(uid: String) {
        if (uid.isEmpty()) return
        
        viewModelScope.launch {
            try {
                firestore?.collection("users")?.document(uid)?.get()?.await()?.let { document ->
                    if (document.exists()) {
                        val remoteProfile = UserProfile(
                            uid = uid,
                            name = document.getString("name") ?: "",
                            email = document.getString("email") ?: "",
                            phone = document.getString("phone"),
                            role = document.getString("role") ?: "PATIENT",
                            address = document.getString("address"),
                            paymentMethod = document.getString("paymentMethod")
                        )
                        userDao.insertProfile(remoteProfile)
                    }
                }
            } catch (e: Exception) {
                // Error de red o permisos, fallamos en silencio y usamos Room
            }
        }
    }

    fun saveProfile(profile: UserProfile) {
        viewModelScope.launch {
            android.util.Log.d("ProfileVM", "Iniciando proceso de guardado para UID: ${profile.uid}")
            // 1. Guardar localmente siempre (rápido)
            try {
                userDao.insertProfile(profile)
                android.util.Log.d("ProfileVM", "✅ Guardado LOCAL (Room) exitoso")
            } catch (e: Exception) {
                android.util.Log.e("ProfileVM", "❌ Error en guardado LOCAL", e)
            }
            
            // 2. Guardar en la nube (Firestore) si no es dummy
            if (profile.uid.isNotEmpty()) {
                try {
                    if (firestore == null) {
                        android.util.Log.e("ProfileVM", "⚠️ Firestore es NULL, no se puede guardar en nube")
                        return@launch
                    }

                    val profileMap = mutableMapOf<String, Any?>(
                        "uid" to profile.uid,
                        "name" to profile.name,
                        "email" to profile.email,
                        "phone" to profile.phone,
                        "role" to profile.role,
                        "address" to profile.address,
                        "paymentMethod" to profile.paymentMethod,
                        "profileImageUrl" to profile.profileImageUrl
                    )

                    firestore.collection("users")
                        .document(profile.uid)
                        .set(profileMap)
                        .await()
                    
                    android.util.Log.d("ProfileVM", "✅ Guardado en NUBE (Firestore) exitoso")
                } catch (e: Exception) {
                    android.util.Log.e("ProfileVM", "❌ Error en guardado en NUBE", e)
                }
            } else {
                android.util.Log.d("ProfileVM", "ℹ️ Saltando guardado en nube (Usuario Dummy o UID vacío)")
            }
        }
    }

    suspend fun getProfile(uid: String): UserProfile? {
        return userDao.getUserProfile(uid)
    }

    suspend fun getProfileByEmail(email: String): UserProfile? {
        return userDao.getUserProfileByEmail(email)
    }

    suspend fun loginToBackend(email: String, password: String): Result<UserProfile> {
        return try {
            val response = com.example.herbhopper_v1.data.network.ApiService.instance.login(
                com.example.herbhopper_v1.data.network.LoginRequest(email.trim().lowercase(), password)
            )
            val user = response.user
            val profile = UserProfile(
                uid = user.id,
                name = user.name,
                email = user.email,
                role = user.role
            )
            userDao.insertProfile(profile)
            SessionManager.login(profile.uid, profile.email, profile.name, profile.role)
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun registerToBackend(name: String, email: String, password: String, role: String = "PATIENT"): Result<UserProfile> {
        return try {
            val response = com.example.herbhopper_v1.data.network.ApiService.instance.register(
                com.example.herbhopper_v1.data.network.RegisterRequest(
                    name = name,
                    email = email.trim().lowercase(),
                    password = password,
                    role = role
                )
            )
            val user = response.user
            val profile = UserProfile(
                uid = user.id,
                name = user.name,
                email = user.email,
                role = user.role
            )
            userDao.insertProfile(profile)
            SessionManager.login(profile.uid, profile.email, profile.name, profile.role)
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}