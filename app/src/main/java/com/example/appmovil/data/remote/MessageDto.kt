package com.example.appmovil.data.remote

import com.google.gson.annotations.SerializedName

// Modelo para un Mensaje Individual
data class MessageItem(
    @SerializedName("id") val id: Int,
    @SerializedName("emisor_id") val emisorId: Int,
    @SerializedName("receptor_id") val receptorId: Int,
    @SerializedName("mensaje") val mensaje: String,
    @SerializedName("fecha") val fecha: String,
    @SerializedName("leido") val leido: Int,
    @SerializedName("esMio") val esMio: Boolean = false // Calculado en el PHP
)

// Modelo para un Contacto en la Lista
data class UserContact(
    @SerializedName("id") val id: Int,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("rol") val rol: String,
    @SerializedName("estado") val estado: String? = null,
    @SerializedName("ultima_conexion") val ultimaConexion: String? = null,
    @SerializedName("unreadCount") val unreadCount: Int = 0 // Opcional para futuras mejoras
)

// DTO para Enviar Mensaje
data class SendMessageRequest(
    @SerializedName("emisor_id") val emisorId: Int,
    @SerializedName("receptor_id") val receptorId: Int,
    @SerializedName("mensaje") val mensaje: String
)
