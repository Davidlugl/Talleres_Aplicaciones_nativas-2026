package com.example.herbhopper_v1.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey
    val uid: String,
    val name: String,
    val email: String,
    val phone: String? = null,
    val role: String, // PATIENT, SELLER, ADMIN
    val profileImageUrl: String? = null,
    val address: String? = null,
    val paymentMethod: String? = null
)
