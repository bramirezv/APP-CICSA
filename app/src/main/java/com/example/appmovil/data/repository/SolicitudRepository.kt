package com.example.appmovil.data.repository

import com.example.appmovil.data.remote.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Resultado de las operaciones con solicitudes
 */
sealed class SolicitudResult<out T> {
    data class Success<T>(val data: T) : SolicitudResult<T>()
    data class Error(val message: String) : SolicitudResult<Nothing>()
    object SessionExpired : SolicitudResult<Nothing>()
}

/**
 * Repositorio para gestionar las solicitudes
 */
class SolicitudRepository(private val api: SolicitudApi = RetrofitClient.solicitudApi) {

    suspend fun getSolicitudes(zona: String? = null, userId: Int? = null, supervisor: String? = null, tipo: String? = null, rol: String? = null): SolicitudResult<List<SolicitudItem>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getSolicitudes(zona, userId, supervisor, tipo, rol)
            if (response.isSuccessful) SolicitudResult.Success(response.body() ?: emptyList())
            else SolicitudResult.Error(response.errorBody()?.string() ?: "Error")
        } catch (e: Exception) { SolicitudResult.Error(e.localizedMessage) }
    }

    suspend fun createSolicitud(request: CreateSolicitudRequest): SolicitudResult<CreateSolicitudResponse> = withContext(Dispatchers.IO) {
        try {
            val response = api.createSolicitud(request)
            if (response.isSuccessful && response.body() != null) SolicitudResult.Success(response.body()!!)
            else SolicitudResult.Error(response.errorBody()?.string() ?: "Error")
        } catch (e: Exception) { SolicitudResult.Error(e.localizedMessage) }
    }

    suspend fun actualizarEstado(id: Int, nuevoEstado: String, idUsuario: Int? = null, comentario: String? = null): SolicitudResult<String> = withContext(Dispatchers.IO) {
        try {
            val response = api.updateSolicitudStatus(UpdateStatusRequest(id, nuevoEstado, idUsuario, comentario))
            if (response.isSuccessful) SolicitudResult.Success(response.body()?.message ?: "OK")
            else SolicitudResult.Error(response.errorBody()?.string() ?: "Error")
        } catch (e: Exception) { SolicitudResult.Error(e.localizedMessage) }
    }

    suspend fun updateSolicitud(request: EditSolicitudRequest): SolicitudResult<String> = withContext(Dispatchers.IO) {
        try {
            val response = api.updateSolicitud(request)
            if (response.isSuccessful) SolicitudResult.Success(response.body()?.message ?: "OK")
            else SolicitudResult.Error(response.errorBody()?.string() ?: "Error")
        } catch (e: Exception) { SolicitudResult.Error(e.localizedMessage) }
    }

    suspend fun uploadEvidencia(id: Int, bytes: ByteArray): SolicitudResult<String> = withContext(Dispatchers.IO) {
        try {
            val idBody = id.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val requestFile = bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
            val body = okhttp3.MultipartBody.Part.createFormData("foto", "evidencia.jpg", requestFile)
            val response = api.uploadEvidencia(idBody, body)
            if (response.isSuccessful) SolicitudResult.Success(response.body()?.message ?: "OK")
            else SolicitudResult.Error(response.errorBody()?.string() ?: "Error")
        } catch (e: Exception) { SolicitudResult.Error(e.localizedMessage) }
    }

    suspend fun deleteEvidencia(id: Int, fotoUrl: String): SolicitudResult<String> = withContext(Dispatchers.IO) {
        try {
            val response = api.deleteEvidencia(DeleteEvidenciaRequest(id, fotoUrl))
            if (response.isSuccessful) SolicitudResult.Success(response.body()?.message ?: "OK")
            else SolicitudResult.Error(response.errorBody()?.string() ?: "Error")
        } catch (e: Exception) { SolicitudResult.Error(e.localizedMessage) }
    }

    // --- MÓDULO SATÉLITES ---
    suspend fun getSatelliteStatuses(zona: String? = null): SolicitudResult<List<SatelliteStatusItem>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getSatelliteStatuses(zona)
            if (response.isSuccessful) SolicitudResult.Success(response.body() ?: emptyList())
            else SolicitudResult.Error("Error")
        } catch (e: Exception) { SolicitudResult.Error(e.localizedMessage) }
    }

    suspend fun getSatelliteStatus(userId: Int, sessionToken: String? = null): SolicitudResult<SatelliteStatusItem> = withContext(Dispatchers.IO) {
        try {
            val response = api.getSatelliteStatus(userId, sessionToken)
            val body = response.body()
            if (response.isSuccessful && body?.data != null) {
                val data = body.data!!
                SolicitudResult.Success(SatelliteStatusItem(
                    id = data.id,
                    nombre = data.nombre,
                    rol = data.rol,
                    estado = data.estado,
                    ultimaConexion = data.ultimaConexion,
                    zona = data.zona,
                    almuerzoUsado = data.almuerzoUsado ?: 0,
                    inicioAlmuerzo = data.inicioAlmuerzo,
                    supervisionesHoy = data.supervisionesHoy ?: 0,
                    supervisionActiva = data.supervisionActiva
                ))
            } else if (body?.sessionError == true) {
                SolicitudResult.SessionExpired
            } else SolicitudResult.Error("Error")
        } catch (e: Exception) { SolicitudResult.Error(e.localizedMessage) }
    }

    suspend fun updateSatelliteStatus(idUsuario: Int, lat: String, lng: String, estado: String, sessionToken: String? = null): SolicitudResult<String> = withContext(Dispatchers.IO) {
        try {
            val response = api.updateSatelliteStatus(UpdateUserStatusRequest(idUsuario, estado, sessionToken))
            val body = response.body()
            if (response.isSuccessful) SolicitudResult.Success(body?.message ?: "OK")
            else if (body?.sessionError == true) SolicitudResult.SessionExpired
            else SolicitudResult.Error("Error")
        } catch (e: Exception) { SolicitudResult.Error(e.localizedMessage) }
    }

    suspend fun updateUserZone(userId: Int, zona: String): SolicitudResult<String> = withContext(Dispatchers.IO) {
        try {
            val response = api.updateUserZone(UpdateUserZoneRequest(userId, zona))
            if (response.isSuccessful) SolicitudResult.Success(response.body()?.message ?: "OK")
            else SolicitudResult.Error("Error")
        } catch (e: Exception) { SolicitudResult.Error(e.localizedMessage) }
    }

    // --- ASISTENCIA Y ALMUERZO ---
    suspend fun getAttendance(fecha: String): SolicitudResult<List<AttendanceItem>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getAttendance(fecha)
            if (response.isSuccessful) SolicitudResult.Success(response.body() ?: emptyList())
            else SolicitudResult.Error("Error")
        } catch (e: Exception) { SolicitudResult.Error(e.localizedMessage) }
    }

    suspend fun manageLunch(userId: Int, action: String): SolicitudResult<String> = withContext(Dispatchers.IO) {
        try {
            val response = when(action) {
                "start" -> api.updateSatelliteStatus(UpdateUserStatusRequest(userId, "Almorzando"))
                "stop" -> api.updateSatelliteStatus(UpdateUserStatusRequest(userId, "Disponible"))
                "reset" -> api.resetLunchStatus(ResetLunchRequest(userId))
                else -> null
            }
            if (response?.isSuccessful == true) SolicitudResult.Success(response.body()?.message ?: "OK")
            else SolicitudResult.Error("Error")
        } catch (e: Exception) { SolicitudResult.Error(e.localizedMessage) }
    }

    // --- TNPS ---
    suspend fun getTNPSEvidencias(idTecnico: Int? = null, idSupervisor: Int? = null, fecha: String? = null): SolicitudResult<List<TNPSEvidenciaItem>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getTNPSEvidencias(idTecnico, idSupervisor, fecha)
            if (response.isSuccessful) SolicitudResult.Success(response.body()?.data ?: emptyList())
            else SolicitudResult.Error("Error")
        } catch (e: Exception) { SolicitudResult.Error(e.localizedMessage) }
    }

    suspend fun uploadTNPSEvidencia(idTecnico: Int, nombreTecnico: String, sot: String, comentario: String, lat: String, lng: String, idSupervisor: Int?, nombreSupervisor: String?, bytes: ByteArray): SolicitudResult<String> = withContext(Dispatchers.IO) {
        try {
            val idBody = idTecnico.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val nameBody = nombreTecnico.toRequestBody("text/plain".toMediaTypeOrNull())
            val sotBody = sot.toRequestBody("text/plain".toMediaTypeOrNull())
            val commentBody = comentario.toRequestBody("text/plain".toMediaTypeOrNull())
            val latBody = lat.toRequestBody("text/plain".toMediaTypeOrNull())
            val lngBody = lng.toRequestBody("text/plain".toMediaTypeOrNull())
            val idSupBody = idSupervisor?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
            val nameSupBody = nombreSupervisor?.toRequestBody("text/plain".toMediaTypeOrNull())
            val requestFile = bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
            val body = okhttp3.MultipartBody.Part.createFormData("foto", "tnps.jpg", requestFile)
            val response = api.uploadTNPSEvidencia(idBody, nameBody, sotBody, commentBody, latBody, lngBody, idSupBody, nameSupBody, body)
            if (response.isSuccessful) SolicitudResult.Success(response.body()?.message ?: "OK")
            else SolicitudResult.Error("Error")
        } catch (e: Exception) { SolicitudResult.Error(e.localizedMessage) }
    }

    suspend fun validateTNPSEvidencia(id: Int, estadoValidacion: String, comentarioSupervisor: String?): SolicitudResult<String> = withContext(Dispatchers.IO) {
        try {
            val response = api.validateTNPSEvidencia(ValidateTNPSRequest(id, estadoValidacion, comentarioSupervisor))
            if (response.isSuccessful) SolicitudResult.Success(response.body()?.message ?: "OK")
            else SolicitudResult.Error("Error")
        } catch (e: Exception) { SolicitudResult.Error(e.localizedMessage) }
    }

    suspend fun getAllUsers(): SolicitudResult<List<UserProfile>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getAllUsers()
            if (response.isSuccessful) SolicitudResult.Success(response.body() ?: emptyList())
            else SolicitudResult.Error("Error al obtener usuarios")
        } catch (e: Exception) {
            SolicitudResult.Error(e.localizedMessage)
        }
    }

    // --- SUPERVISIÓN ---
    suspend fun searchTecnicos(query: String): SolicitudResult<List<TecnicoItem>> = withContext(Dispatchers.IO) {
        try {
            val response = api.searchTecnicos(query)
            if (response.isSuccessful) SolicitudResult.Success(response.body()?.data ?: emptyList())
            else SolicitudResult.Error("Error")
        } catch (e: Exception) { SolicitudResult.Error(e.localizedMessage) }
    }

    suspend fun manageTeam(idSupervisor: Int, idTecnico: Int, action: String): SolicitudResult<String> = withContext(Dispatchers.IO) {
        try {
            val response = api.manageTeam(ManageTeamRequest(idSupervisor, idTecnico, action))
            if (response.isSuccessful) SolicitudResult.Success(response.body()?.message ?: "OK")
            else SolicitudResult.Error("Error")
        } catch (e: Exception) { SolicitudResult.Error(e.localizedMessage) }
    }

    suspend fun getMyTeam(idSupervisor: Int): SolicitudResult<List<TecnicoItem>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getMyTeam(idSupervisor)
            if (response.isSuccessful) SolicitudResult.Success(response.body()?.data ?: emptyList())
            else SolicitudResult.Error("Error")
        } catch (e: Exception) { SolicitudResult.Error(e.localizedMessage) }
    }
    
    suspend fun startSupervision(idSupervisor: Int, idTecnico: Int, sot: String, sessionToken: String? = null): SolicitudResult<String> = withContext(Dispatchers.IO) {
        try {
            val response = api.startSupervision(idSupervisor, idTecnico, sot, sessionToken)
            val body = response.body()
            if (response.isSuccessful && body?.status == "success") {
                SolicitudResult.Success(body.message ?: "OK")
            } else if (body?.sessionError == true) {
                SolicitudResult.SessionExpired
            } else {
                SolicitudResult.Error(body?.message ?: "Error en el servidor o técnico ya supervisado")
            }
        } catch (e: Exception) { SolicitudResult.Error(e.localizedMessage) }
    }

    suspend fun cancelSupervision(idSupervisor: Int, idTecnico: Int): SolicitudResult<String> = withContext(Dispatchers.IO) {
        try {
            val response = api.cancelSupervision(idSupervisor, idTecnico)
            if (response.isSuccessful) SolicitudResult.Success(response.body()?.message ?: "OK")
            else SolicitudResult.Error("Error")
        } catch (e: Exception) { SolicitudResult.Error(e.localizedMessage) }
    }

    suspend fun updateSupervisionProgress(idSupervisor: Int, idTecnico: Int, checklist: String?, comentarios: String?, fotos: String?, sessionToken: String? = null): SolicitudResult<String> = withContext(Dispatchers.IO) {
        try {
            val response = api.updateSupervisionProgress(idSupervisor, idTecnico, checklist, comentarios, fotos, sessionToken)
            val body = response.body()
            if (response.isSuccessful && body?.status == "success") SolicitudResult.Success(body.message ?: "OK")
            else if (body?.sessionError == true) SolicitudResult.SessionExpired
            else SolicitudResult.Error(body?.message ?: "Error")
        } catch (e: Exception) { SolicitudResult.Error(e.localizedMessage) }
    }

    suspend fun uploadSupervisionTempPhoto(idSupervisor: Int, idTecnico: Int, bytes: ByteArray): SolicitudResult<String> = withContext(Dispatchers.IO) {
        try {
            val idSupBody = idSupervisor.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val idTecBody = idTecnico.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val requestFile = bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
            val body = okhttp3.MultipartBody.Part.createFormData("foto", "temp.jpg", requestFile)
            val response = api.uploadSupervisionTempPhoto(idSupBody, idTecBody, body)
            if (response.isSuccessful) SolicitudResult.Success(response.body()?.message ?: "OK")
            else SolicitudResult.Error("Error")
        } catch (e: Exception) { SolicitudResult.Error(e.localizedMessage) }
    }

    suspend fun submitSupervision(idSupervisor: Int, idTecnico: Int, sot: String, comentarios: String, lat: String, lng: String, puntuacion: Int, checklist: String, imagesList: List<ByteArray>?, sessionToken: String? = null): SolicitudResult<String> = withContext(Dispatchers.IO) {
        try {
            val idSupBody = idSupervisor.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val idTecBody = idTecnico.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val sotBody = sot.toRequestBody("text/plain".toMediaTypeOrNull())
            val commentBody = comentarios.toRequestBody("text/plain".toMediaTypeOrNull())
            val latBody = lat.toRequestBody("text/plain".toMediaTypeOrNull())
            val lngBody = lng.toRequestBody("text/plain".toMediaTypeOrNull())
            val scoreBody = puntuacion.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val checkBody = checklist.toRequestBody("text/plain".toMediaTypeOrNull())
            val tokenBody = sessionToken?.toRequestBody("text/plain".toMediaTypeOrNull())
            val photoParts = imagesList?.mapIndexed { i, b -> okhttp3.MultipartBody.Part.createFormData("foto$i", "sup_$i.jpg", b.toRequestBody("image/jpeg".toMediaTypeOrNull())) }
            val response = api.submitSupervision(idSupBody, idTecBody, sotBody, commentBody, latBody, lngBody, scoreBody, checkBody, tokenBody, photoParts)
            val body = response.body()
            if (response.isSuccessful) SolicitudResult.Success(body?.message ?: "OK")
            else if (body?.sessionError == true) SolicitudResult.SessionExpired
            else SolicitudResult.Error("Error")
        } catch (e: Exception) { SolicitudResult.Error(e.localizedMessage) }
    }

    suspend fun getZones(): List<String> = withContext(Dispatchers.IO) {
        try {
            val response = api.getZones()
            if (response.isSuccessful) response.body() ?: emptyList() else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getAllSupervisiones(): SolicitudResult<List<SupervisionReportItem>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getAllSupervisiones()
            if (response.isSuccessful) SolicitudResult.Success(response.body()?.data ?: emptyList())
            else SolicitudResult.Error("Error al obtener supervisiones")
        } catch (e: Exception) {
            SolicitudResult.Error(e.localizedMessage)
        }
    }
    suspend fun getSupervisorProductivity(): SolicitudResult<List<SupervisorProductivityItem>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getSupervisorProductivity()
            if (response.isSuccessful && response.body() != null) {
                SolicitudResult.Success(response.body()!!.data)
            } else {
                SolicitudResult.Error("Error al obtener productividad")
            }
        } catch (e: Exception) {
            SolicitudResult.Error(e.localizedMessage)
        }
    }
}
