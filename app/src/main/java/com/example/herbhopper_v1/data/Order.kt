package com.example.herbhopper_v1.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

@Entity(tableName = "orders")
data class Order(
    @PrimaryKey
    val orderId: String,
    val userId: String,
    val userName: String,
    val itemsJson: String, // Lista de productos en formato JSON simplificado
    val totalAmount: Double,
    val status: String, // PENDING, ACCEPTED, CANCELLED, DELIVERED
    val timestamp: Long = System.currentTimeMillis(),
    val address: String? = null,
    val paymentMethod: String? = null
)

// Clase para representar los items internamente
data class OrderItem(
    val productId: Int,
    val productName: String,
    val quantity: Int,
    val price: Double
)
