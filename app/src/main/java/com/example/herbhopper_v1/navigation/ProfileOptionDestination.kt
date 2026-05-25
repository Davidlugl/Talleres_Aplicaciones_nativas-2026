package com.example.herbhopper_v1.navigation

enum class ProfileOptionDestination(val key: String) {
    CONFIGURATION("Configuración"),
    PAYMENT_METHODS("Métodos de Pago"),
    ADDRESSES("Direcciones"),
    NOTIFICATIONS("Notificaciones");

    companion object {
        fun fromKey(key: String): ProfileOptionDestination {
            return values().firstOrNull { it.key.lowercase() == key.lowercase() } ?: CONFIGURATION
        }
    }
}
