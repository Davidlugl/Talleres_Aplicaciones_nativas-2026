package com.example.herbhopper_v1.model

/**
 * Solución al Issue 5: Entidad de dominio AuditLog extraída de la capa de presentación
 * y ubicada en la capa de modelo/dominio correspondiente para evitar inversión de dependencias incorrecta.
 */
data class AuditLog(
    val title: String,
    val description: String,
    val time: String,
    val isWarning: Boolean
)
