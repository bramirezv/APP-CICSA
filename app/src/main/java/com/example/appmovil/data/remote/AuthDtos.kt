package com.example.appmovil.data.remote

import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep

@Keep
data class GenericResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String,
    @SerializedName("session_error") val sessionError: Boolean? = false
)

@Keep
data class LoginRequest(
    @SerializedName("phone", alternate = ["celular", "telefono", "identifier"]) val phone: String,
    @SerializedName("password") val password: String
)


// DTO para Registro
@Keep
data class RegisterRequest(
    @SerializedName("name") val name: String,
    @SerializedName("dni") val dni: String? = "",
    @SerializedName("phone", alternate = ["telefono", "celular"]) val phone: String,
    @SerializedName("password") val password: String,
    @SerializedName("rol") val rol: String? = "satelite",
    @SerializedName("zona") val zona: String? = null,
    @SerializedName("email") val email: String? = ""
)

// DTO para Perfil Completo
data class UserProfile(
    @SerializedName("id") val id: Int,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("telefono") val telefono: String? = null,
    @SerializedName("rol") val rol: String,
    @SerializedName("zona") val zona: String? = null,
    @SerializedName("disponible") val disponible: Int? = 0,
    @SerializedName("estado") val estado: String? = "Disponible",
    @SerializedName("en_almuerzo") val enAlmuerzo: Int? = 0,
    @SerializedName("inicio_almuerzo") val inicioAlmuerzo: String? = null,
    @SerializedName("lat") val lat: Double? = null,
    @SerializedName("lng") val lng: Double? = null,
    @SerializedName("ultima_conexion") val ultimaConexion: String? = null,
    @SerializedName("ultima_ubicacion") val ultimaUbicacion: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("distrito_id") val distritoId: Int? = null,
    @SerializedName("fecha_creacion") val fechaCreacion: String? = null,
    @SerializedName("foto_perfil") val fotoPerfil: String? = null,
    @SerializedName("ultimo_acceso") val ultimoAcceso: String? = null,
    @SerializedName("telefono_emergencia") val telefonoEmergencia: String? = null,
    @SerializedName("direccion") val direccion: String? = null,
    @SerializedName("fecha_nacimiento") val fechaNacimiento: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("ruta_activa") val rutaActiva: Int? = 0,
    @SerializedName("almuerzo_usado") val almuerzoUsado: Int? = 0,
    @SerializedName("ultima_activacion") val ultimaActivacion: String? = null,
    @SerializedName("supervisiones_hoy") val supervisionesHoy: Int? = 0,
    @SerializedName("supervision_activa") val supervisionActiva: ActiveSupervision? = null
)

data class ActiveSupervision(
    @SerializedName("tecnico_id") val tecnicoId: Int,
    @SerializedName("tecnico_nombre") val tecnicoNombre: String,
    @SerializedName("sot") val sot: String,
    @SerializedName("inicio") val inicio: String,
    @SerializedName("checklist") val checklist: String? = null,
    @SerializedName("comentarios") val comentarios: String? = null,
    @SerializedName("fotos") val fotos: String? = null
)

// Respuesta para el Perfil
data class UserProfileResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String? = null,
    @SerializedName("session_error") val sessionError: Boolean? = false,
    @SerializedName("data") val data: UserProfile? = null
)

@Keep
data class ChangePasswordRequest(
    @SerializedName("id") val id: Int,
    @SerializedName("current_password") val currentPassword: String,
    @SerializedName("new_password") val newPassword: String
)

@Keep
data class DefaultResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String? = null
)

// DTO para Respuesta General
@Keep
data class AuthResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String,
    @SerializedName("id", alternate = ["userId", "ID", "Id", "uid", "userId_old"]) val id: Int? = null,
    @SerializedName("nombre", alternate = ["userName", "NOMBRE", "Nombre", "user"]) val nombre: String? = null,
    @SerializedName("rol") val rol: String? = null,
    @SerializedName("zona") val zona: String? = null,
    @SerializedName("estado") val estado: String? = null,
    @SerializedName("almuerzo_usado") val almuerzoUsado: Int? = 0,
    @SerializedName("sessionToken") val sessionToken: String? = null
)

// DTO para el Estado de un Satélite (Individual)
data class SatelliteStatusItem(
    @SerializedName("id") val id: Int,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("rol") val rol: String? = "satelite",
    @SerializedName("estado") val estado: String? = "Disponible",
    @SerializedName("ultima_conexion") val ultimaConexion: String? = null,
    @SerializedName("zona") val zona: String? = "SIN ZONA",
    @SerializedName("almuerzo_usado") val almuerzoUsado: Int? = 0,
    @SerializedName("inicio_almuerzo") val inicioAlmuerzo: String? = null,
    @SerializedName("supervisiones_hoy") val supervisionesHoy: Int? = 0,
    @SerializedName("supervision_activa") val supervisionActiva: ActiveSupervision? = null
)

// DTO para Actualizar Zona del Usuario
data class UpdateUserZoneRequest(
    @SerializedName("userId") val userId: Int,
    @SerializedName("zona") val zona: String
)

// DTO para Actualizar Estado del Usuario
data class UpdateUserStatusRequest(
    @SerializedName("userId") val userId: Int,
    @SerializedName("estado") val estado: String,
    @SerializedName("session_token") val sessionToken: String? = null
)

// DTO para Resetear Almuerzo
data class ResetLunchRequest(
    @SerializedName("userId") val userId: Int
)

// DTO para Mapa de Ubicación (Usuario Admin)
data class UserLocationItem(
    @SerializedName("id") val id: Int,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("rol") val rol: String,
    @SerializedName("zona") val zona: String? = null,
    @SerializedName("lat") val lat: Double,
    @SerializedName("lng") val lng: Double,
    @SerializedName("ultima_conexion") val ultimaConexion: String? = null,
    @SerializedName("estado") val estado: String? = "Disponible",
    @SerializedName("ruta_activa") val rutaActiva: Int? = 0,
    @SerializedName("almuerzo_usado") val almuerzoUsado: Int? = 0,
    @SerializedName("telefono") val telefono: String? = null
)

// DTO para Control de Asistencia
data class AttendanceItem(
    @SerializedName("id") val id: Int,
    @SerializedName("userId") val userId: Int,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("evento") val evento: String, // "INGRESO" o "SALIDA"
    @SerializedName("fecha") val fecha: String,
    @SerializedName("hora") val hora: String
)
// DTO para Productividad de Supervisores
data class SupervisorProductivityItem(
    @SerializedName("supervisor_id") val supervisorId: Int,
    @SerializedName("supervisor_nombre") val supervisorNombre: String,
    @SerializedName("total_supervisiones") val totalSupervisiones: Int,
    @SerializedName("tecnicos_supervisados") val tecnicosSupervisados: String? = null
)

data class SupervisorProductivityResponse(
    @SerializedName("status") val status: String,
    @SerializedName("data") val data: List<SupervisorProductivityItem>
)
