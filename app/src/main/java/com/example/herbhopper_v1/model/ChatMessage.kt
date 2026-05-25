package com.example.herbhopper_v1.model

/**
 * Solución al Issue 5: Entidad de dominio ChatMessage extraída de la capa de presentación
 * y ubicada en la capa de modelo/dominio correspondiente para evitar inversión de dependencias incorrecta.
 */
data class ChatMessage(
    val id: String,
    val text: String,
    val isFromUser: Boolean,
    val timestamp: String
)
