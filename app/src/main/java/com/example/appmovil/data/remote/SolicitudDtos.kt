package com.example.appmovil.data.remote

import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep

/**
 * Representa una solicitud individual en el sistema
 */
data class SolicitudItem(
    @SerializedName("id") val id: Int,
    @SerializedName("sot") val sot: String?,
    @SerializedName("tecnico") val tecnico: String?,
    @SerializedName("motivo") val motivo: String?,
    @SerializedName("material") val material: String?,
    @SerializedName("coordenadas") val coordenadas: String?,
    @SerializedName("nombre_supervisor") val nombreSupervisor: String?,
    @SerializedName("zona") val zona: String?,
    @SerializedName("estado") val estado: String?,
    @SerializedName("foto_evidencia") val fotoEvidencia: String?,
    @SerializedName("id_satelite") val idSatelite: Int? = 0,
    @SerializedName("nombre_satelite") val nombreSatelite: String? = null,
    @SerializedName("comentario_satelite") val comentarioSatelite: String? = null,
    @SerializedName("fecha_creacion") val fechaCreacion: String?,
    @SerializedName("fecha_aceptacion") val fechaAceptacion: String?,
    @SerializedName("fecha_finalizacion") val fechaFinalizacion: String?,
    @SerializedName("tipo") val tipo: String? = "normal",
    @SerializedName("id_supervisor") val idSupervisorAsignado: Int? = null, // Alias para compatibilidad
    @SerializedName("nombre_supervisor_asignado") val nombreSupervisorAsignado: String? = null
)

/**
 * Request para crear una nueva solicitud
 */
data class CreateSolicitudRequest(
    @SerializedName("sot") val sot: String,
    @SerializedName("tecnico") val tecnico: String,
    @SerializedName("motivo") val motivo: String,
    @SerializedName("material") val material: String,
    @SerializedName("coordenadas") val coordenadas: String,
    @SerializedName("nombre_supervisor") val nombreSupervisor: String,
    @SerializedName("zona") val zona: String,
    @SerializedName("tipo") val tipo: String = "normal",
    @SerializedName("id_supervisor") val idSupervisor: Int? = null,
    @SerializedName("nombre_supervisor_asignado") val nombreSupervisorAsignado: String? = null
)

/**
 * Request para editar una solicitud existente
 */
data class EditSolicitudRequest(
    @SerializedName("id") val id: Int,
    @SerializedName("sot") val sot: String,
    @SerializedName("tecnico") val tecnico: String,
    @SerializedName("motivo") val motivo: String,
    @SerializedName("material") val material: String,
    @SerializedName("coordenadas") val coordenadas: String,
    @SerializedName("zona") val zona: String
)

/**
 * Respuesta para la lista de solicitudes
 */
data class SolicitudListResponse(
    @SerializedName("status") val status: String,
    @SerializedName("solicitudes") val solicitudes: List<SolicitudItem>
)

/**
 * Respuesta para creación/edición de solicitudes
 */
data class CreateSolicitudResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String,
    @SerializedName("id") val id: Int? = null,
    @SerializedName("satellite_status") val satelliteStatus: String? = null,
    @SerializedName("total_satelites") val totalSatelites: Int? = null,
    @SerializedName("disponibles") val disponibles: Int? = null
)

/**
 * Request para actualizar el estado de una solicitud
 */
data class UpdateStatusRequest(
    @SerializedName("id") val id: Int,
    @SerializedName("nuevo_estado") val nuevoEstado: String,
    @SerializedName("id_usuario") val idUsuario: Int? = null,
    @SerializedName("comentario") val comentario: String? = null
)

/**
 * Respuesta para actualizar el estado de una solicitud
 */
data class UpdateStatusResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String
)

/**
 * Información de actualización desde el servidor
 */
data class UpdateInfo(
    @SerializedName("versionCode") val versionCode: Int,
    @SerializedName("versionName") val versionName: String,
    @SerializedName("apkUrl") val apkUrl: String,
    @SerializedName("notes") val notes: String? = null
)

/**
 * Request para eliminar una foto de evidencia
 */
data class DeleteEvidenciaRequest(
    @SerializedName("id") val id: Int,
    @SerializedName("foto_url") val fotoUrl: String
)

/**
 * Representa una evidencia de TNPS enviada por un técnico
 */
data class TNPSEvidenciaItem(
    @SerializedName("id") val id: Int,
    @SerializedName("id_tecnico") val idTecnico: Int,
    @SerializedName("nombre_tecnico") val nombreTecnico: String,
    @SerializedName("sot") val sot: String?,
    @SerializedName("foto_url") val fotoUrl: String,
    @SerializedName("comentario") val comentario: String?,
    @SerializedName("fecha_registro") val fechaRegistro: String?,
    @SerializedName("latitud") val latitud: Double?,
    @SerializedName("longitud") val longitud: Double?,
    @SerializedName("id_supervisor") val idSupervisor: Int?,
    @SerializedName("nombre_supervisor") val nombreSupervisor: String?,
    @SerializedName("estado_validacion") val estadoValidacion: String?,
    @SerializedName("comentario_supervisor") val comentarioSupervisor: String?
)

data class TNPSEvidenciaListResponse(
    @SerializedName("status") val status: String,
    @SerializedName("data") val data: List<TNPSEvidenciaItem>
)

data class ValidateTNPSRequest(
    @SerializedName("id") val id: Int,
    @SerializedName("estado_validacion") val estadoValidacion: String,
    @SerializedName("comentario_supervisor") val comentarioSupervisor: String? = null
)

// DTO para Técnicos (Búsqueda y Mi Equipo)
@Keep
data class TecnicoItem(
    @SerializedName("id") val id: Int,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("dni") val dni: String? = null,
    @SerializedName("zona") val zona: String? = null,
    @SerializedName("telefono") val telefono: String? = null
)

@Keep
data class TecnicoListResponse(
    @SerializedName("status") val status: String,
    @SerializedName("data") val data: List<TecnicoItem> = emptyList()
)

@Keep
data class ManageTeamRequest(
    val id_supervisor: Int,
    val id_tecnico: Int,
    val action: String // 'add' or 'remove'
)

/**
 * Representa un reporte de supervisión para el administrador
 */
@Keep
data class SupervisionReportItem(
    @SerializedName("id") val id: Int,
    @SerializedName("id_supervisor") val idSupervisor: Int,
    @SerializedName("nombre_supervisor") val nombreSupervisor: String?,
    @SerializedName("id_tecnico") val idTecnico: Int,
    @SerializedName("nombre_tecnico") val nombreTecnico: String?,
    @SerializedName("sot") val sot: String?,
    @SerializedName("comentarios") val comentarios: String?,
    @SerializedName("latitud") val latitud: String?,
    @SerializedName("longitud") val longitud: String?,
    @SerializedName("puntuacion") val puntuacion: Int,
    @SerializedName("checklist") val checklist: Any?, // Puede ser Map o JsonElement
    @SerializedName("fotos_urls") val fotosUrls: List<String> = emptyList(),
    @SerializedName("fecha_creacion") val fechaCreacion: String?
)

@Keep
data class SupervisionReportListResponse(
    @SerializedName("status") val status: String,
    @SerializedName("data") val data: List<SupervisionReportItem> = emptyList()
)

/**
 * Representa una solicitud recientemente aceptada por un satélite (notificación al supervisor)
 */
@Keep
data class AcceptedTicketItem(
    @SerializedName("id") val id: Int,
    @SerializedName("sot") val sot: String?,
    @SerializedName("tecnico") val tecnico: String?,
    @SerializedName("motivo") val motivo: String?,
    @SerializedName("zona") val zona: String?,
    @SerializedName("nombre_satelite") val nombreSatelite: String?,
    @SerializedName("fecha_aceptacion") val fechaAceptacion: String?
)

@Keep
data class AcceptedTicketsResponse(
    @SerializedName("status") val status: String,
    @SerializedName("count") val count: Int = 0,
    @SerializedName("accepted") val accepted: List<AcceptedTicketItem> = emptyList()
)

/**
 * Datos del ticket destino para el mapa de seguimiento
 */
@Keep
data class TrackingTicket(
    @SerializedName("id") val id: Int,
    @SerializedName("sot") val sot: String?,
    @SerializedName("tecnico") val tecnico: String?,
    @SerializedName("motivo") val motivo: String?,
    @SerializedName("zona") val zona: String?,
    @SerializedName("estado") val estado: String?,
    @SerializedName("coordenadas") val coordenadas: String?,
    @SerializedName("dest_lat") val destLat: Double?,
    @SerializedName("dest_lng") val destLng: Double?
)

/**
 * Datos del satélite en tiempo real para el mapa de seguimiento
 */
@Keep
data class TrackingSatelite(
    @SerializedName("id") val id: Int,
    @SerializedName("nombre") val nombre: String?,
    @SerializedName("lat") val lat: Double?,
    @SerializedName("lng") val lng: Double?,
    @SerializedName("ultima_ubicacion") val ultimaUbicacion: String?,
    @SerializedName("estado") val estado: String?
)

/**
 * Respuesta completa del endpoint get_ticket_tracking.php
 */
@Keep
data class TrackingResponse(
    @SerializedName("status") val status: String,
    @SerializedName("ticket") val ticket: TrackingTicket?,
    @SerializedName("satelite") val satelite: TrackingSatelite?,
    @SerializedName("eta_minutos") val etaMinutos: Int?
)

