package com.example.herbhopper_v1.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad Room que representa una dirección de entrega del usuario.
 * Soporta múltiples direcciones por usuario (uid), con coordenadas GPS opcionales
 * obtenidas desde el dispositivo para pre-rellenar la dirección automáticamente.
 *
 * @param id Identificador único autogenerado.
 * @param uid UID del usuario propietario de la dirección.
 * @param label Etiqueta descriptiva: "Casa", "Trabajo", etc.
 * @param fullAddress Dirección completa en texto (calle, número, ciudad).
 * @param latitude Latitud GPS opcional (null si se ingresó manualmente).
 * @param longitude Longitud GPS opcional (null si se ingresó manualmente).
 * @param isDefault Indica si es la dirección predeterminada de entrega.
 * @param createdAt Timestamp de creación en milisegundos.
 */
@Entity(tableName = "delivery_addresses")
data class DeliveryAddress(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val uid: String,
    val label: String,
    val fullAddress: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
