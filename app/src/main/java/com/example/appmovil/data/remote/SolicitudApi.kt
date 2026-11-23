package com.example.appmovil.data.remote

import retrofit2.Response
import retrofit2.http.*
import okhttp3.RequestBody
import okhttp3.MultipartBody

/**
 * Interfaz para la API de solicitudes
 */
interface SolicitudApi {
    /**
     * Obtener todas las solicitudes registradas
     */
    @GET("api/get_solicitudes.php")
    suspend fun getSolicitudes(
        @Query("zona") zona: String? = null,
        @Query("userId") userId: Int? = null,
        @Query("supervisor") supervisor: String? = null,
        @Query("tipo") tipo: String? = null,
        @Query("rol") rol: String? = null
    ): Response<List<SolicitudItem>>

    /**
     * Crear una nueva solicitud
     */
    @POST("api/create_solicitud.php")
    suspend fun createSolicitud(@Body request: CreateSolicitudRequest): Response<CreateSolicitudResponse>

    /**
     * Actualizar el estado de una solicitud (Aceptar, Rechazar, Corregir)
     */
    @POST("api/update_solicitud_status.php")
    suspend fun updateSolicitudStatus(@Body request: UpdateStatusRequest): Response<UpdateStatusResponse>

    /**
     * Actualizar los datos de una solicitud (Editar)
     */
    @POST("api/update_solicitud_data.php")
    suspend fun updateSolicitud(@Body request: EditSolicitudRequest): Response<CreateSolicitudResponse>

    /**
     * Subir foto de evidencia
     */
    @Multipart
    @POST("api/upload_evidencia.php")
    suspend fun uploadEvidencia(
        @Part("id") id: RequestBody,
        @Part foto: MultipartBody.Part
    ): Response<UpdateStatusResponse>

    /**
     * Eliminar una foto de evidencia
     */
    @POST("api/delete_evidencia.php")
    suspend fun deleteEvidencia(@Body request: DeleteEvidenciaRequest): Response<UpdateStatusResponse>

    /**
     * Verificar si existe una nueva versión de la App
     */
    @GET("api/check_update.php")
    suspend fun checkUpdate(): Response<UpdateInfo>

    /**
     * Obtener todas las evidencias de TNPS (Admin)
     */
    @GET("api/get_tnps_evidencias.php")
    suspend fun getTNPSEvidencias(
        @Query("id_tecnico") idTecnico: Int? = null,
        @Query("id_supervisor") idSupervisor: Int? = null,
        @Query("fecha") fecha: String? = null
    ): Response<TNPSEvidenciaListResponse>

    @POST("api/validate_tnps_evidencia.php")
    suspend fun validateTNPSEvidencia(@Body request: ValidateTNPSRequest): Response<GenericResponse>

    @GET("api/admin_get_all_users.php")
    suspend fun getAllUsers(): Response<List<UserProfile>>

    // --- MÓDULO SATÉLITES Y ZONIFICACIÓN ---
    @GET("api/get_satellites_status.php")
    suspend fun getSatelliteStatuses(@Query("zona") zona: String? = null): Response<List<SatelliteStatusItem>>

    @GET("api/get_user_info.php")
    suspend fun getSatelliteStatus(
        @Query("userId") userId: Int,
        @Query("session_token") sessionToken: String? = null
    ): Response<UserProfileResponse>

    @POST("api/update_status.php")
    suspend fun updateSatelliteStatus(@Body request: UpdateUserStatusRequest): Response<GenericResponse>

    @POST("api/update_user_zone.php")
    suspend fun updateUserZone(@Body request: UpdateUserZoneRequest): Response<GenericResponse>

    // --- ASISTENCIA Y ALMUERZO ---
    @GET("api/get_attendance.php")
    suspend fun getAttendance(@Query("fecha") fecha: String): Response<List<AttendanceItem>>

    @FormUrlEncoded
    @POST("api/attendance.php")
    suspend fun recordAttendance(@Field("userId") userId: Int, @Field("evento") evento: String): Response<GenericResponse>

    @POST("api/reset_lunch_status.php")
    suspend fun resetLunchStatus(@Body request: ResetLunchRequest): Response<GenericResponse>

    // --- MÓDULO SUPERVISIÓN ---
    @GET("api/get_tecnicos.php")
    suspend fun searchTecnicos(@Query("query") query: String): Response<TecnicoListResponse>

    @POST("api/manage_supervised_tecnicos.php")
    suspend fun manageTeam(@Body request: ManageTeamRequest): Response<GenericResponse>

    @GET("api/get_supervised_tecnicos.php")
    suspend fun getMyTeam(@Query("id_supervisor") idSupervisor: Int): Response<TecnicoListResponse>

    @FormUrlEncoded
    @POST("api/start_supervision.php")
    suspend fun startSupervision(
        @Field("id_supervisor") idSupervisor: Int,
        @Field("id_tecnico") idTecnico: Int,
        @Field("sot") sot: String,
        @Field("session_token") sessionToken: String?
    ): Response<GenericResponse>

    @FormUrlEncoded
    @POST("api/cancel_supervision.php")
    suspend fun cancelSupervision(
        @Field("id_supervisor") idSupervisor: Int,
        @Field("id_tecnico") idTecnico: Int
    ): Response<GenericResponse>

    @FormUrlEncoded
    @POST("api/update_supervision_progress.php")
    suspend fun updateSupervisionProgress(
        @Field("id_supervisor") idSupervisor: Int,
        @Field("id_tecnico") idTecnico: Int,
        @Field("checklist") checklist: String?,
        @Field("comentarios") comentarios: String?,
        @Field("fotos") fotos: String?,
        @Field("session_token") sessionToken: String?
    ): Response<GenericResponse>

    @Multipart
    @POST("api/upload_supervision_temp_photo.php")
    suspend fun uploadSupervisionTempPhoto(
        @Part("id_supervisor") idSupervisor: RequestBody,
        @Part("id_tecnico") idTecnico: RequestBody,
        @Part foto: MultipartBody.Part
    ): Response<GenericResponse>

    @Multipart
    @POST("api/submit_supervision.php")
    suspend fun submitSupervision(
        @Part("id_supervisor") idSupervisor: RequestBody,
        @Part("id_tecnico") idTecnico: RequestBody,
        @Part("sot") sot: RequestBody,
        @Part("comentarios") comentarios: RequestBody,
        @Part("lat") lat: RequestBody,
        @Part("lng") lng: RequestBody,
        @Part("puntuacion") puntuacion: RequestBody,
        @Part("checklist") checklist: RequestBody,
        @Part("session_token") sessionToken: RequestBody?,
        @Part fotos: List<MultipartBody.Part>?
    ): Response<GenericResponse>

    /**
     * Subir evidencia de TNPS (Técnico)
     */
    @Multipart
    @POST("api/upload_tnps_evidencia.php")
    suspend fun uploadTNPSEvidencia(
        @Part("id_tecnico") idTecnico: RequestBody,
        @Part("nombre_tecnico") nombreTecnico: RequestBody,
        @Part("sot") sot: RequestBody,
        @Part("comentario") comentario: RequestBody,
        @Part("lat") lat: RequestBody,
        @Part("lng") lng: RequestBody,
        @Part("id_supervisor") idSupervisor: RequestBody?,
        @Part("nombre_supervisor") nombreSupervisor: RequestBody?,
        @Part foto: MultipartBody.Part
    ): Response<UpdateStatusResponse>

    /**
     * Obtener lista de zonas desde la base de datos
     */
    @GET("api/get_zones.php")
    suspend fun getZones(): Response<List<String>>

    /**
     * Obtener todas las supervisiones realizadas (Admin)
     */
    @GET("api/get_all_supervisiones.php")
    suspend fun getAllSupervisiones(): Response<SupervisionReportListResponse>

    @GET("api/get_supervisor_productivity.php")
    suspend fun getSupervisorProductivity(): Response<SupervisorProductivityResponse>

    /**
     * Verificar si alguna solicitud del supervisor fue aceptada recientemente por un satélite
     */
    @GET("api/check_accepted_tickets.php")
    suspend fun checkAcceptedTickets(
        @Query("supervisor") supervisor: String,
        @Query("since_seconds") sinceSeconds: Int = 15
    ): Response<AcceptedTicketsResponse>

    /**
     * Obtener datos de seguimiento en tiempo real de un ticket (ubicación del satélite asignado)
     */
    @GET("api/get_ticket_tracking.php")
    suspend fun getTicketTracking(
        @Query("id_solicitud") idSolicitud: Int
    ): Response<TrackingResponse>
}
