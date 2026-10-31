package com.example.herbhopper_v1.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Pedido. Espejo local (Room) de la tabla orders del backend, que es la fuente de verdad.
 */
@Entity(tableName = "orders")
data class Order(
    @PrimaryKey
    val orderId: String,
    val userId: String,
    val userName: String,
    val itemsJson: String, // Lista de OrderItem en JSON
    val totalAmount: Double,
    val status: String, // PENDING, ACCEPTED, CANCELLED, DELIVERED
    val timestamp: Long = System.currentTimeMillis(),
    val address: String? = null,
    val paymentMethod: String? = null, // CREDIT_CARD, PSE, CASH
    val latitude: Double? = null,
    val longitude: Double? = null,
    /** Estado real del pago informado por ePayco al backend: PENDING, APPROVED, REJECTED. */
    val paymentStatus: String? = null,
    val paymentRef: String? = null,
    val prescriptionId: Int? = null,
    /** Estado de la fórmula adjunta: PENDING, APPROVED, REJECTED (null si no aplica). */
    val prescriptionStatus: String? = null,
    val updatedAt: Long? = null
)

// Clase para representar los items internamente
data class OrderItem(
    val productId: Int,
    val productName: String,
    val quantity: Int,
    val price: Double
)
