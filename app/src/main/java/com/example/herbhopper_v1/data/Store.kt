package com.example.herbhopper_v1.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stores")
data class Store(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val location: String,
    val licenseNumber: String,
    val status: String = "PENDING" // PENDING, VERIFIED, SUSPENDED
)
