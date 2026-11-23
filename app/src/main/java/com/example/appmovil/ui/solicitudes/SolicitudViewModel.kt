package com.example.appmovil.ui.solicitudes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appmovil.data.remote.*
import com.example.appmovil.data.repository.SolicitudRepository
import com.example.appmovil.data.repository.SolicitudResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import java.util.Locale
import android.net.Uri
import java.io.File
import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

sealed class SolicitudUiState {
    object Idle : SolicitudUiState()
    object Loading : SolicitudUiState()
    data class Success(val solicitudes: List<SolicitudItem>) : SolicitudUiState()
    data class Error(val message: String) : SolicitudUiState()
}

sealed class CreateSolicitudUiState {
    object Idle : CreateSolicitudUiState()
    object Loading : CreateSolicitudUiState()
    data class Success(val message: String) : CreateSolicitudUiState()
    data class Error(val message: String) : CreateSolicitudUiState()
}

sealed class AttendanceUiState {
    object Idle : AttendanceUiState()
    object Loading : AttendanceUiState()
    data class Success(val attendance: List<AttendanceItem>) : AttendanceUiState()
    data class Error(val message: String) : AttendanceUiState()
}

class SolicitudViewModel(
    private val repository: SolicitudRepository = SolicitudRepository(),
    private val context: Context? = null
) : ViewModel() {

    private val prefs = context?.getSharedPreferences("supervision_prefs", Context.MODE_PRIVATE)
    private val sessionManager = context?.let { com.example.appmovil.data.SessionManager(it) }
    private val gson = Gson()

    private val _solicitudState = MutableStateFlow<SolicitudUiState>(SolicitudUiState.Idle)
    val solicitudState = _solicitudState.asStateFlow()

    private val _createState = MutableStateFlow<CreateSolicitudUiState>(CreateSolicitudUiState.Idle)
    val createState = _createState.asStateFlow()

    /** Datos del último ticket creado para navegar directamente al tracking */
    private val _lastCreatedTicketInfo = MutableStateFlow<Triple<Int, String, String>?>(null)
    val lastCreatedTicketInfo = _lastCreatedTicketInfo.asStateFlow()
    fun clearLastCreatedTicket() { _lastCreatedTicketInfo.value = null }

    // Alias para compatibilidad con HomeScreen
    val uiState = solicitudState

    private val _availableZones = MutableStateFlow<List<String>>(emptyList())
    val availableZones = _availableZones.asStateFlow()

    private val _sessionExpired = MutableStateFlow(false)
    val sessionExpired = _sessionExpired.asStateFlow()

    fun resetSessionExpired() { _sessionExpired.value = false }

    // Record de supervisión diaria
    private val _supervisionesHoy = MutableStateFlow(0)
    val supervisionesHoy = _supervisionesHoy.asStateFlow()

    // --- ESTADO DE SUPERVISIÓN PERSISTENTE ---
    private val _isSupervisionStarted = MutableStateFlow(false)
    val isSupervisionStarted = _isSupervisionStarted.asStateFlow()

    private val _supervisionElapsedTime = MutableStateFlow(0L)
    val supervisionElapsedTime = _supervisionElapsedTime.asStateFlow()

    private val _selectedSupervisionTecnico = MutableStateFlow<TecnicoItem?>(null)
    val selectedSupervisionTecnico = _selectedSupervisionTecnico.asStateFlow()

    private val _supervisionSot = MutableStateFlow("")
    val supervisionSot = _supervisionSot.asStateFlow()

    private val _capturedSupervisionPhotos = MutableStateFlow<List<Pair<Uri, File>>>(emptyList())
    val capturedSupervisionPhotos = _capturedSupervisionPhotos.asStateFlow()

    // --- NUEVOS ESTADOS DETALLADOS DE SUPERVISIÓN ---
    private val _supervisionRespuestas = MutableStateFlow<Map<String, String>>(emptyMap())
    val supervisionRespuestas = _supervisionRespuestas.asStateFlow()

    private val _supervisionDatosSot = MutableStateFlow<Map<String, String>>(emptyMap())
    val supervisionDatosSot = _supervisionDatosSot.asStateFlow()

    private val _supervisionPenalidades = MutableStateFlow<List<String>>(emptyList())
    val supervisionPenalidades = _supervisionPenalidades.asStateFlow()

    private val _supervisionRequierePenalidad = MutableStateFlow(false)
    val supervisionRequierePenalidad = _supervisionRequierePenalidad.asStateFlow()

    private val _supervisionComentarioPenalidad = MutableStateFlow("")
    val supervisionComentarioPenalidad = _supervisionComentarioPenalidad.asStateFlow()

    private val _supervisionComentarios = MutableStateFlow("")
    val supervisionComentarios = _supervisionComentarios.asStateFlow()

    private var _tempPhotoUri: Uri? = null
    private var _tempPhotoFile: File? = null

    private var timerJob: Job? = null

    init {
        restoreSupervisionState()
        loadZones()
    }

    fun loadZones() {
        viewModelScope.launch {
            val zones = repository.getZones()
            if (zones.isNotEmpty()) {
                _availableZones.value = zones
            }
        }
    }

    private fun restoreSupervisionState() {
        prefs?.let { p ->
            val started = p.getBoolean("started", false)
            if (started) {
                val tecnicoJson = p.getString("tecnico", null)
                val sotValue = p.getString("sot", "") ?: ""
                val startTime = p.getLong("start_time", 0L)
                val photosJson = p.getString("photos", "[]") ?: "[]"

                if (tecnicoJson != null) {
                    val tecnico = gson.fromJson(tecnicoJson, TecnicoItem::class.java)
                    _selectedSupervisionTecnico.value = tecnico
                    _supervisionSot.value = sotValue
                    _isSupervisionStarted.value = true
                    
                    // Restaurar fotos
                    val type = object : TypeToken<List<String>>() {}.type
                    val photoPaths: List<String> = gson.fromJson(photosJson, type)
                    _capturedSupervisionPhotos.value = photoPaths.map { path ->
                        val file = File(path)
                        Pair(Uri.fromFile(file), file)
                    }

                    // Reiniciar timer basado en la diferencia de tiempo
                    val now = System.currentTimeMillis()
                    val elapsed = (now - startTime) / 1000
                    _supervisionElapsedTime.value = if (elapsed > 0) elapsed else 0L

                    startTimerJob()
                }
            }
        }
    }

    private fun startTimerJob() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _supervisionElapsedTime.value++
            }
        }
    }

    // --- ESTADOS DE CREACIÓN/EDICIÓN ---
    private val _tecnico = MutableStateFlow("")
    val tecnico = _tecnico.asStateFlow()
    private val _sot = MutableStateFlow("")
    val sot = _sot.asStateFlow()
    private val _motivo = MutableStateFlow("")
    val motivo = _motivo.asStateFlow()
    private val _material = MutableStateFlow("")
    val material = _material.asStateFlow()
    private val _coordenadas = MutableStateFlow("")
    val coordenadas = _coordenadas.asStateFlow()
    private val _zona = MutableStateFlow("")
    val zona = _zona.asStateFlow()
    private val _editSolicitudId = MutableStateFlow<Int?>(null)
    val editSolicitudId = _editSolicitudId.asStateFlow()

    private val _selectedSupervisorId = MutableStateFlow<Int?>(null)
    val selectedSupervisorId = _selectedSupervisorId.asStateFlow()
    private val _selectedSupervisorName = MutableStateFlow("")
    val selectedSupervisorName = _selectedSupervisorName.asStateFlow()

    fun onTecnicoChange(value: String) { _tecnico.value = value }
    fun onSotChange(value: String) { _sot.value = value }
    fun onMotivoChange(value: String) { _motivo.value = value }
    fun onMaterialChange(value: String) { _material.value = value }
    fun onCoordenadasChange(value: String) { _coordenadas.value = value }
    fun onZonaChange(value: String) { _zona.value = value }
    fun onSupervisorSelectionChange(id: Int?, name: String) {
        _selectedSupervisorId.value = id
        _selectedSupervisorName.value = name
    }

    // --- 1. SUPERVISIÓN EN TIEMPO REAL ---
    fun startSupervision(idSupervisor: Int, tecnico: TecnicoItem, sot: String, onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            _createState.value = CreateSolicitudUiState.Loading
            val token = sessionManager?.getSession()?.sessionToken
            when (val res = repository.startSupervision(idSupervisor, tecnico.id, sot, token)) {
                is SolicitudResult.Success -> {
                    val startTime = System.currentTimeMillis()
                    _selectedSupervisionTecnico.value = tecnico
                    _supervisionSot.value = sot
                    _isSupervisionStarted.value = true
                    _supervisionElapsedTime.value = 0L
                    _capturedSupervisionPhotos.value = emptyList()
                    
                    // Guardar en Prefs
                    prefs?.edit()?.apply {
                        putBoolean("started", true)
                        putString("tecnico", gson.toJson(tecnico))
                        putString("sot", sot)
                        putLong("start_time", startTime)
                        putString("photos", "[]")
                        apply()
                    }

                    startTimerJob()
                    _createState.value = CreateSolicitudUiState.Idle
                    onResult(true, res.data)
                }
                is SolicitudResult.SessionExpired -> {
                    _sessionExpired.value = true
                    _createState.value = CreateSolicitudUiState.Error("Sesión expirada")
                    onResult(false, "Sesión expirada")
                }
                is SolicitudResult.Error -> {
                    _createState.value = CreateSolicitudUiState.Error(res.message)
                    onResult(false, res.message)
                }
            }
        }
    }

    fun stopSupervision() {
        timerJob?.cancel()
        _isSupervisionStarted.value = false
        prefs?.edit()?.putBoolean("started", false)?.apply()
    }

    fun setSupervisionTecnico(tecnico: TecnicoItem?) {
        _selectedSupervisionTecnico.value = tecnico
    }

    fun setSupervisionSot(sot: String) {
        _supervisionSot.value = sot
    }

    fun cancelSupervision(idSupervisor: Int) {
        val tecnicoId = _selectedSupervisionTecnico.value?.id ?: 0
        if (tecnicoId > 0) {
            viewModelScope.launch {
                repository.cancelSupervision(idSupervisor, tecnicoId)
                resetSupervision()
            }
        } else {
            resetSupervision()
        }
    }

    fun resetSupervision() {
        stopSupervision()
        _selectedSupervisionTecnico.value = null
        _supervisionSot.value = ""
        _supervisionElapsedTime.value = 0L
        _capturedSupervisionPhotos.value = emptyList()
        _supervisionRespuestas.value = emptyMap()
        _supervisionDatosSot.value = emptyMap()
        _supervisionPenalidades.value = emptyList()
        _supervisionRequierePenalidad.value = false
        _supervisionComentarioPenalidad.value = ""
        
        prefs?.edit()?.clear()?.apply()
    }

    fun setSupervisionAnswer(question: String, answer: String) {
        val current = _supervisionRespuestas.value.toMutableMap()
        current[question] = answer
        _supervisionRespuestas.value = current
        saveChecklistToPrefs()
        syncSupervisionProgress()
    }

    fun setSupervisionComentarios(value: String) {
        _supervisionComentarios.value = value
        syncSupervisionProgress()
    }

    fun updateSupervisionDatoSot(field: String, value: String) {
        val current = _supervisionDatosSot.value.toMutableMap()
        current[field] = value
        _supervisionDatosSot.value = current
        syncSupervisionProgress()
    }

    fun toggleSupervisionPenalidad(penalidad: String) {
        val current = _supervisionPenalidades.value.toMutableList()
        if (current.contains(penalidad)) {
            current.remove(penalidad)
        } else {
            current.add(penalidad)
        }
        _supervisionPenalidades.value = current
    }

    fun setSupervisionRequierePenalidad(value: Boolean) {
        _supervisionRequierePenalidad.value = value
    }

    fun setSupervisionComentarioPenalidad(value: String) {
        _supervisionComentarioPenalidad.value = value
    }

    private fun restoreFromBackend(active: ActiveSupervision) {
        val tecnico = TecnicoItem(id = active.tecnicoId, nombre = active.tecnicoNombre)
        _selectedSupervisionTecnico.value = tecnico
        _supervisionSot.value = active.sot
        _isSupervisionStarted.value = true
        
        // --- RESTAURAR CHECKLIST Y DATOS SOT ---
        if (!active.checklist.isNullOrEmpty()) {
            try {
                val type = object : TypeToken<Map<String, String>>() {}.type
                val map: Map<String, String> = gson.fromJson(active.checklist, type)
                
                // Dividir entre respuestas y datos SOT (ej. productividad, calificacion)
                val respuestas = mutableMapOf<String, String>()
                val datosSot = mutableMapOf<String, String>()
                
                map.forEach { (k, v) ->
                    if (k == "productividad" || k == "calificacion_tecnico" || k == "sugerencia") {
                        datosSot[k] = v
                    } else {
                        respuestas[k] = v
                    }
                }
                
                _supervisionRespuestas.value = respuestas
                _supervisionDatosSot.value = datosSot
            } catch (e: Exception) {}
        }
        
        // --- RESTAURAR COMENTARIOS ---
        _supervisionComentarios.value = active.comentarios ?: ""
        
        // --- RESTAURAR FOTOS DESDE EL SERVIDOR ---
        if (!active.fotos.isNullOrEmpty()) {
            val filenames = active.fotos.split(",")
            val baseUrl = "https://tecsolperu.com/api/uploads/supervisiones_temp/" // Ajustar si el dominio cambia
            val currentPhotos = _capturedSupervisionPhotos.value.toMutableList()
            
            filenames.forEach { name ->
                if (name.isNotEmpty()) {
                    val uri = Uri.parse(baseUrl + name)
                    // Para fotos remotas, el File es null
                    currentPhotos.add(uri to File("")) 
                }
            }
            _capturedSupervisionPhotos.value = currentPhotos
        }
        
        // Calcular tiempo transcurrido desde el inicio en el servidor
        try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            val startDate = sdf.parse(active.inicio)
            if (startDate != null) {
                val elapsed = (System.currentTimeMillis() - startDate.time) / 1000
                _supervisionElapsedTime.value = if (elapsed > 0) elapsed else 0L
            }
        } catch (e: Exception) {
            _supervisionElapsedTime.value = 0L
        }

        // Persistir localmente también
        prefs?.edit()?.apply {
            putBoolean("started", true)
            putString("tecnico", gson.toJson(tecnico))
            putString("sot", active.sot)
            // Usamos el tiempo calculado para el pref local (aproximado)
            putLong("start_time", System.currentTimeMillis() - (_supervisionElapsedTime.value * 1000))
            apply()
        }

        startTimerJob()
    }

    private fun savePhotosToPrefs() {
        val paths = _capturedSupervisionPhotos.value.map { it.second.absolutePath }
        prefs?.edit()?.putString("photos", gson.toJson(paths))?.apply()
    }

    private fun saveChecklistToPrefs() {
        val json = gson.toJson(_supervisionRespuestas.value)
        prefs?.edit()?.putString("checklist", json)?.apply()
    }

    private fun syncSupervisionProgress() {
        val session = sessionManager?.getSession() ?: return
        val supervisorId = session.id ?: 0
        val tecnicoId = _selectedSupervisionTecnico.value?.id ?: return
        val sessionToken = session.sessionToken
        
        viewModelScope.launch(Dispatchers.IO) {
            val checklistMap = _supervisionRespuestas.value.toMutableMap()
            // También incluimos los datos de SOT en el checklist para que se guarden
            checklistMap.putAll(_supervisionDatosSot.value)
            
            val checklistJson = gson.toJson(checklistMap)
            val comentarios = _supervisionComentarios.value
            
            repository.updateSupervisionProgress(
                idSupervisor = supervisorId,
                idTecnico = tecnicoId,
                checklist = checklistJson,
                comentarios = comentarios,
                fotos = null,
                sessionToken = sessionToken
            )
        }
    }

    fun setTempPhoto(uri: Uri, file: File) {
        _tempPhotoUri = uri
        _tempPhotoFile = file
    }

    fun confirmTempPhoto() {
        if (_tempPhotoUri != null && _tempPhotoFile != null) {
            captureSupervisionPhoto(_tempPhotoUri!!, _tempPhotoFile!!)
            _tempPhotoUri = null
            _tempPhotoFile = null
        }
    }

    fun captureSupervisionPhoto(uri: Uri, file: File) {
        val current = _capturedSupervisionPhotos.value.toMutableList()
        current.add(uri to file)
        _capturedSupervisionPhotos.value = current
        savePhotosToPrefs()
        uploadTempPhoto(file)
    }

    private fun uploadTempPhoto(file: File) {
        val session = sessionManager?.getSession() ?: return
        val supervisorId = session.id ?: 0
        val tecnicoId = _selectedSupervisionTecnico.value?.id ?: return
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val bytes = file.readBytes()
                repository.uploadSupervisionTempPhoto(supervisorId, tecnicoId, bytes)
            } catch (e: Exception) {}
        }
    }

    fun removeSupervisionPhoto(photo: Pair<Uri, File>) {
        val current = _capturedSupervisionPhotos.value.toMutableList()
        current.remove(photo)
        _capturedSupervisionPhotos.value = current
        savePhotosToPrefs()
    }

    fun removeSupervisionPhoto(index: Int) {
        val current = _capturedSupervisionPhotos.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _capturedSupervisionPhotos.value = current
        }
    }

    // --- 2. GESTIÓN DE SOLICITUDES ---
    fun fetchSolicitudes(zona: String? = null, userId: Int? = null, supervisor: String? = null, tipo: String = "normal", rol: String? = null) {
        viewModelScope.launch {
            // Solo mostramos Loading si no tenemos datos previos (evita el parpadeo)
            if (_solicitudState.value !is SolicitudUiState.Success) {
                _solicitudState.value = SolicitudUiState.Loading
            }
            
            when (val result = repository.getSolicitudes(zona = zona, userId = userId, supervisor = supervisor, tipo = tipo, rol = rol)) {
                is SolicitudResult.Success -> {
                    // Solo actualizamos si el estado no es el mismo o si queremos forzar el refresco
                    _solicitudState.value = SolicitudUiState.Success(result.data)
                }
                is SolicitudResult.SessionExpired -> {
                    _sessionExpired.value = true
                }
                is SolicitudResult.Error -> {
                    // Si ya teníamos datos, no mostramos el error de red para no interrumpir
                    if (_solicitudState.value !is SolicitudUiState.Success) {
                        _solicitudState.value = SolicitudUiState.Error(result.message)
                    }
                }
            }
        }
    }

    fun createSolicitud(request: CreateSolicitudRequest) {
        viewModelScope.launch {
            _createState.value = CreateSolicitudUiState.Loading
            when (val result = repository.createSolicitud(request)) {
                is SolicitudResult.Success -> {
                    _createState.value = CreateSolicitudUiState.Success(result.data.message)
                    // Guardar info del ticket creado para navegar al tracking
                    val ticketId = result.data.id
                    if (ticketId != null && ticketId > 0) {
                        _lastCreatedTicketInfo.value = Triple(
                            ticketId,
                            request.sot,
                            request.tecnico
                        )
                    }
                }
                is SolicitudResult.SessionExpired -> {
                    _sessionExpired.value = true
                    _createState.value = CreateSolicitudUiState.Error("Sesión expirada")
                }
                is SolicitudResult.Error -> _createState.value = CreateSolicitudUiState.Error(result.message)
            }
        }
    }

    fun createSolicitud(userName: String, tipo: String = "normal") {
        val request = CreateSolicitudRequest(
            sot = _sot.value,
            tecnico = _tecnico.value,
            motivo = _motivo.value,
            material = _material.value,
            coordenadas = _coordenadas.value,
            nombreSupervisor = userName,
            zona = _zona.value,
            tipo = tipo,
            idSupervisor = _selectedSupervisorId.value,
            nombreSupervisorAsignado = _selectedSupervisorName.value
        )
        createSolicitud(request)
    }

    fun uploadEvidencia(id: Int, bytes: ByteArray, onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            when (val result = repository.uploadEvidencia(id, bytes)) {
                is SolicitudResult.Success -> onResult(true, result.data)
                is SolicitudResult.SessionExpired -> {
                    _sessionExpired.value = true
                    onResult(false, "Sesión expirada")
                }
                is SolicitudResult.Error -> onResult(false, result.message)
            }
        }
    }

    fun deleteEvidencia(id: Int, fotoUrl: String = "", onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val result = repository.deleteEvidencia(id, fotoUrl)
            onResult(result is SolicitudResult.Success)
        }
    }

    fun actualizarEstado(id: Int, estado: String, idUsuario: Int? = null, comentario: String? = null, rol: String? = null, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val result = repository.actualizarEstado(id, estado, idUsuario, comentario)
            onResult(result is SolicitudResult.Success)
        }
    }

    fun resetCreateState() {
        _createState.value = CreateSolicitudUiState.Idle
        _tecnico.value = ""
        _sot.value = ""
        _motivo.value = ""
        _material.value = ""
        _coordenadas.value = ""
        _zona.value = ""
        _editSolicitudId.value = null
        _selectedSupervisorId.value = null
        _selectedSupervisorName.value = ""
    }

    fun prepararEdicion(solicitud: SolicitudItem) {
        _editSolicitudId.value = solicitud.id
        _sot.value = solicitud.sot ?: ""
        _tecnico.value = solicitud.tecnico ?: ""
        _motivo.value = solicitud.motivo ?: ""
        _material.value = solicitud.material ?: ""
        _coordenadas.value = solicitud.coordenadas ?: ""
        _zona.value = solicitud.zona ?: ""
        _selectedSupervisorId.value = solicitud.idSatelite
        _selectedSupervisorName.value = solicitud.nombreSatelite ?: ""
    }

    // --- 3. GESTIÓN DE SATÉLITES Y ZONIFICACIÓN ---
    private val _satelliteStatuses = MutableStateFlow<List<SatelliteStatusItem>>(emptyList())
    val satelliteStatuses = _satelliteStatuses.asStateFlow()

    private val _myStatus = MutableStateFlow<SatelliteStatusItem?>(null)
    val myStatus = _myStatus.asStateFlow()

    private val _zoningState = MutableStateFlow<CreateSolicitudUiState>(CreateSolicitudUiState.Idle)
    val zoningState = _zoningState.asStateFlow()

    private val _supervisionReports = MutableStateFlow<List<SupervisionReportItem>>(emptyList())
    val supervisionReports = _supervisionReports.asStateFlow()

    private val _currentUserZone = MutableStateFlow("SIN ZONA")
    val currentUserZone = _currentUserZone.asStateFlow()

    private val _satelliteAvailability = MutableStateFlow<String?>(null)
    val satelliteAvailability = _satelliteAvailability.asStateFlow()

    fun setCurrentUserZone(zone: String) { _currentUserZone.value = zone }

    fun fetchSatelliteStatuses(zona: String? = null) {
        viewModelScope.launch {
            when (val res = repository.getSatelliteStatuses(zona)) {
                is SolicitudResult.Success -> _satelliteStatuses.value = res.data
                else -> {}
            }
        }
    }

    fun updateMyStatus(idUsuario: Int, lat: String = "0.0", lng: String = "0.0", estado: String = "Disponible") {
        viewModelScope.launch {
            val token = sessionManager?.getSession()?.sessionToken
            val res = repository.updateSatelliteStatus(idUsuario, lat, lng, estado, token)
            when (res) {
                is SolicitudResult.Success -> {
                    // Actualización local inmediata
                    _myStatus.value = _myStatus.value?.copy(estado = estado) 
                        ?: SatelliteStatusItem(id = idUsuario, estado = estado, nombre = "Cargando...")
                }
                is SolicitudResult.SessionExpired -> {
                    _sessionExpired.value = true
                }
                else -> {}
            }
            fetchUserInfo(idUsuario)
        }
    }

    fun fetchUserInfo(userId: Int) {
        viewModelScope.launch {
            val token = sessionManager?.getSession()?.sessionToken
            when (val res = repository.getSatelliteStatus(userId, token)) {
                is SolicitudResult.Success -> {
                    _myStatus.value = res.data
                    _almuerzoUsado.value = res.data.almuerzoUsado == 1
                    _supervisionesHoy.value = res.data.supervisionesHoy ?: 0
                    
                    // Restaurar supervisión si el backend indica que hay una en curso para este supervisor
                    if (res.data.supervisionActiva != null && !_isSupervisionStarted.value) {
                        restoreFromBackend(res.data.supervisionActiva)
                    }
                    
                    if (res.data.estado == "Almorzando") {
                        var startTime = prefs?.getLong("lunch_start_time_$userId", 0L) ?: 0L
                        if (startTime == 0L) {
                            startTime = System.currentTimeMillis()
                            prefs?.edit()?.putLong("lunch_start_time_$userId", startTime)?.apply()
                        }
                        startLunchCountdown(startTime)
                    } else if (res.data.estado != "Almorzando") {
                        stopLunchCountdown()
                        prefs?.edit()?.remove("lunch_start_time_$userId")?.apply()
                    }
                }
                is SolicitudResult.SessionExpired -> {
                    _sessionExpired.value = true
                }
                else -> {}
            }
        }
    }

    fun updateUserZone(userId: Int, zona: String) {
        viewModelScope.launch {
            _zoningState.value = CreateSolicitudUiState.Loading
            repository.updateUserZone(userId, zona)
            _zoningState.value = CreateSolicitudUiState.Success("Zona actualizada")
            fetchSatelliteStatuses()
        }
    }

    fun resetZoningState() { _zoningState.value = CreateSolicitudUiState.Idle }
    fun resetSatelliteAvailability() { _satelliteAvailability.value = null }

    fun fetchAllSupervisiones() {
        viewModelScope.launch {
            _solicitudState.value = SolicitudUiState.Loading
            when (val res = repository.getAllSupervisiones()) {
                is SolicitudResult.Success -> {
                    _supervisionReports.value = res.data
                    _solicitudState.value = SolicitudUiState.Idle // Reset loading
                }
                is SolicitudResult.SessionExpired -> {
                    _sessionExpired.value = true
                }
                is SolicitudResult.Error -> {
                    _solicitudState.value = SolicitudUiState.Error(res.message)
                }
            }
        }
    }

    // --- 4. ASISTENCIA Y ALMUERZO ---
    private val _attendanceState = MutableStateFlow<AttendanceUiState>(AttendanceUiState.Idle)
    val attendanceState = _attendanceState.asStateFlow()

    private val _almuerzoUsado = MutableStateFlow(false)
    val almuerzoUsado = _almuerzoUsado.asStateFlow()

    private val _lunchTimeRemaining = MutableStateFlow(0L)
    val lunchTimeRemaining = _lunchTimeRemaining.asStateFlow()

    private var lunchJob: Job? = null

    private fun startLunchCountdown(startTimeMillis: Long) {
        lunchJob?.cancel()
        lunchJob = viewModelScope.launch {
            try {
                while (true) {
                    val passed = (System.currentTimeMillis() - startTimeMillis) / 1000
                    val remaining = 3600L - passed
                    if (remaining > 0) {
                        _lunchTimeRemaining.value = remaining
                    } else {
                        _lunchTimeRemaining.value = 0
                        // Opcional: auto-stop lunch if time runs out
                    }
                    kotlinx.coroutines.delay(1000)
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun stopLunchCountdown() {
        lunchJob?.cancel()
        lunchJob = null
        _lunchTimeRemaining.value = 0L
    }

    fun fetchAttendance(fecha: String) {
        viewModelScope.launch {
            _attendanceState.value = AttendanceUiState.Loading
            when (val res = repository.getAttendance(fecha)) {
                is SolicitudResult.Success -> _attendanceState.value = AttendanceUiState.Success(res.data)
                is SolicitudResult.SessionExpired -> {
                    _sessionExpired.value = true
                }
                is SolicitudResult.Error -> _attendanceState.value = AttendanceUiState.Error(res.message)
            }
        }
    }

    fun startLunch(userId: Int, context: android.content.Context, lat: String = "0.0", lng: String = "0.0") {
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            prefs?.edit()?.putLong("lunch_start_time_$userId", startTime)?.apply()
            
            repository.manageLunch(userId, "start")
            _almuerzoUsado.value = true
            updateMyStatus(userId, estado = "Almorzando")
            startLunchCountdown(startTime)
        }
    }

    fun stopLunch(userId: Int, context: android.content.Context, lat: String = "0.0", lng: String = "0.0") {
        viewModelScope.launch {
            prefs?.edit()?.remove("lunch_start_time_$userId")?.apply()
            repository.manageLunch(userId, "stop")
            updateMyStatus(userId, estado = "Disponible")
            stopLunchCountdown()
        }
    }

    fun resetLunch(userId: Int, context: android.content.Context, lat: String = "0.0", lng: String = "0.0") {
        viewModelScope.launch {
            repository.manageLunch(userId, "reset")
            _almuerzoUsado.value = false
            fetchSatelliteStatuses()
        }
    }

    fun checkLunchSavedState(userId: Int, context: android.content.Context, lat: String = "0.0", lng: String = "0.0") {
        // Implementación simplificada
    }

    // --- 5. REPORTES DE AUXILIARES ---
    private val _auxiliarReports = MutableStateFlow<List<SolicitudItem>>(emptyList())
    val auxiliarReports = _auxiliarReports.asStateFlow()

    fun fetchAuxiliarReports(userId: Int? = null, supervisor: String? = null, tipo: String? = null, zona: String? = null) {
        viewModelScope.launch {
            when (val res = repository.getSolicitudes(userId = userId, supervisor = supervisor, tipo = tipo ?: "auxiliar", zona = zona, rol = "auxiliar")) {
                is SolicitudResult.Success -> _auxiliarReports.value = res.data
                else -> {}
            }
        }
    }

    // --- 6. TNPS Y EQUIPO (SUPERVISORES) ---
    private val _myTeam = MutableStateFlow<List<TecnicoItem>>(emptyList())
    val myTeam = _myTeam.asStateFlow()

    private val _searchResult = MutableStateFlow<List<TecnicoItem>>(emptyList())
    val searchResult = _searchResult.asStateFlow()

    private val _isSearchingTecnicos = MutableStateFlow(false)
    val isSearchingTecnicos = _isSearchingTecnicos.asStateFlow()

    private val _tnpsEvidencias = MutableStateFlow<List<TNPSEvidenciaItem>>(emptyList())
    val tnpsEvidencias = _tnpsEvidencias.asStateFlow()

    private val _tnpsLoading = MutableStateFlow(false)
    val tnpsLoading = _tnpsLoading.asStateFlow()

    private val _supervisoresList = MutableStateFlow<List<UserProfile>>(emptyList())
    val supervisoresList = _supervisoresList.asStateFlow()

    fun fetchSupervisores() {
        viewModelScope.launch {
            when (val res = repository.getAllUsers()) {
                is SolicitudResult.Success -> {
                    _supervisoresList.value = res.data.filter { it.rol?.lowercase() == "supervisor" }
                }
                else -> {}
            }
        }
    }

    fun fetchMyTeam(idSupervisor: Int) {
        viewModelScope.launch {
            when (val res = repository.getMyTeam(idSupervisor)) {
                is SolicitudResult.Success -> _myTeam.value = res.data
                else -> {}
            }
        }
    }

    fun searchTecnicos(query: String) {
        if (query.isEmpty()) {
            _searchResult.value = emptyList()
            _isSearchingTecnicos.value = false
            return
        }
        viewModelScope.launch {
            _isSearchingTecnicos.value = true
            try {
                when (val res = repository.searchTecnicos(query)) {
                    is SolicitudResult.Success -> {
                        _searchResult.value = res.data
                    }
                    is SolicitudResult.SessionExpired -> {
                        _sessionExpired.value = true
                    }
                    is SolicitudResult.Error -> {
                        _searchResult.value = emptyList()
                        // Aquí podríamos disparar un evento de error si fuera necesario
                    }
                }
            } catch (e: Exception) {
                _searchResult.value = emptyList()
            } finally {
                _isSearchingTecnicos.value = false
            }
        }
    }

    fun manageTeam(idSupervisor: Int, idTecnico: Int, action: String) {
        viewModelScope.launch {
            repository.manageTeam(idSupervisor, idTecnico, action)
            fetchMyTeam(idSupervisor)
        }
    }

    fun submitSupervision(
        idSupervisor: Int,
        idTecnico: Int,
        sot: String,
        comentarios: String,
        lat: String,
        lng: String,
        puntuacion: Int,
        checklist: String,
        imagesList: List<ByteArray>?,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            val token = sessionManager?.getSession()?.sessionToken
            when (val res = repository.submitSupervision(idSupervisor, idTecnico, sot, comentarios, lat, lng, puntuacion, checklist, imagesList, token)) {
                is SolicitudResult.Success -> {
                    fetchUserInfo(idSupervisor)
                    onResult(true, res.data)
                }
                is SolicitudResult.SessionExpired -> {
                    _sessionExpired.value = true
                    onResult(false, "Sesión expirada")
                }
                is SolicitudResult.Error -> onResult(false, res.message)
            }
        }
    }

    fun submitSupervisionBackground(
        idSupervisor: Int,
        idTecnico: Int,
        sot: String,
        comentarios: String,
        lat: String,
        lng: String,
        puntuacion: Int,
        checklist: String,
        photos: List<File>
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val imagesBytesList = photos.map { file ->
                    val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                    val stream = java.io.ByteArrayOutputStream()
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 60, stream)
                    stream.toByteArray()
                }
                val token = sessionManager?.getSession()?.sessionToken
                val res = repository.submitSupervision(idSupervisor, idTecnico, sot, comentarios, lat, lng, puntuacion, checklist, imagesBytesList, token)
                
                if (res is SolicitudResult.Success) {
                    // Limpiar estado local solo si tuvo éxito
                    resetSupervision()
                    fetchUserInfo(idSupervisor)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun fetchTNPSEvidencias(idTecnico: Int? = null, idSupervisor: Int? = null) {
        viewModelScope.launch {
            _tnpsLoading.value = true
            when (val res = repository.getTNPSEvidencias(idTecnico, idSupervisor)) {
                is SolicitudResult.Success -> _tnpsEvidencias.value = res.data
                else -> {}
            }
            _tnpsLoading.value = false
        }
    }

    fun uploadTNPSEvidencia(idTecnico: Int, nombreTecnico: String, sot: String, comentario: String, lat: String, lng: String, idSupervisor: Int?, nombreSupervisor: String?, bytes: ByteArray, onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            when (val res = repository.uploadTNPSEvidencia(idTecnico, nombreTecnico, sot, comentario, lat, lng, idSupervisor, nombreSupervisor, bytes)) {
                is SolicitudResult.Success -> onResult(true, res.data)
                is SolicitudResult.SessionExpired -> {
                    _sessionExpired.value = true
                    onResult(false, "Sesión expirada")
                }
                is SolicitudResult.Error -> onResult(false, res.message)
            }
        }
    }

    fun validateTNPSEvidencia(id: Int, estadoValidacion: String, comentarioSupervisor: String?, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            when (val res = repository.validateTNPSEvidencia(id, estadoValidacion, comentarioSupervisor)) {
                is SolicitudResult.Success -> {
                    fetchTNPSEvidencias()
                    onResult(true)
                }
                is SolicitudResult.SessionExpired -> {
                    _sessionExpired.value = true
                    onResult(false)
                }
                is SolicitudResult.Error -> onResult(false)
            }
        }
    }
    private val _supervisorProductivity = MutableStateFlow<List<SupervisorProductivityItem>>(emptyList())
    val supervisorProductivity = _supervisorProductivity.asStateFlow()

    fun fetchSupervisorProductivity() {
        viewModelScope.launch {
            when (val res = repository.getSupervisorProductivity()) {
                is SolicitudResult.Success -> _supervisorProductivity.value = res.data
                else -> {}
            }
        }
    }

    // --- 7. SEGUIMIENTO DE TICKET EN TIEMPO REAL (Estilo Uber) ---

    sealed class TrackingUiState {
        object Idle : TrackingUiState()
        object Loading : TrackingUiState()
        /** Ticket enviado, esperando que un satélite lo acepte */
        data class SearchingSatelite(val solicitudId: Int, val sot: String, val tecnico: String) : TrackingUiState()
        /** Satélite aceptó — mostramos mapa con su ubicación en tiempo real */
        data class SateliteEnCamino(val data: TrackingResponse) : TrackingUiState()
        /** Satélite llegó al destino */
        data class Llegado(val sateliteNombre: String) : TrackingUiState()
        data class Error(val message: String) : TrackingUiState()
    }

    private val _trackingState = MutableStateFlow<TrackingUiState>(TrackingUiState.Idle)
    val trackingState = _trackingState.asStateFlow()

    /** ID del ticket actualmente en seguimiento */
    private val _activeSolicitudId = MutableStateFlow<Int?>(null)
    val activeSolicitudId = _activeSolicitudId.asStateFlow()

    private var trackingJob: Job? = null

    /**
     * Inicia el modo "Buscando Satélite" inmediatamente después de crear un ticket.
     * El polling cambiará automáticamente a "Satélite en Camino" cuando alguien acepte.
     */
    fun startTicketTracking(solicitudId: Int, sot: String, tecnico: String, isAlreadyAccepted: Boolean = false) {
        _activeSolicitudId.value = solicitudId
        if (isAlreadyAccepted) {
            _trackingState.value = TrackingUiState.Loading
        } else {
            _trackingState.value = TrackingUiState.SearchingSatelite(solicitudId, sot, tecnico)
        }
        trackingJob?.cancel()
        trackingJob = viewModelScope.launch(Dispatchers.IO) {
            try {
            while (true) {
                try {
                    val response = com.example.appmovil.data.remote.RetrofitClient.solicitudApi
                        .getTicketTracking(solicitudId)
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body?.status == "success" && body.ticket != null) {
                            val estado = body.ticket.estado?.lowercase() ?: ""
                            when {
                                estado == "llegado" || estado == "completada" || estado == "finalizada" -> {
                                    _trackingState.value = TrackingUiState.Llegado(
                                        body.satelite?.nombre ?: "Satélite"
                                    )
                                    return@launch
                                }
                                (estado == "aceptada" || estado == "en_camino" || estado == "camino") && body.satelite != null -> {
                                    _trackingState.value = TrackingUiState.SateliteEnCamino(body)
                                }
                                estado == "pendiente" -> {
                                    // Seguimos esperando — mantener estado SearchingSatelite
                                }
                                estado == "cancelada" || estado == "rechazada" -> {
                                    _trackingState.value = TrackingUiState.Idle
                                    return@launch
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Ignorar errores de red temporales durante el tracking
                }
                delay(4000L) // Poll cada 4 segundos
            }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Job fue cancelado normalmente
            }
        }
    }

    /** Detiene el seguimiento y regresa al estado Idle */
    fun stopTicketTracking() {
        trackingJob?.cancel()
        trackingJob = null
        _trackingState.value = TrackingUiState.Idle
        _activeSolicitudId.value = null
    }

    /** Llamar cuando el supervisor cancela el ticket en pantalla de tracking */
    fun cancelTicketFromTracking(solicitudId: Int, idUsuario: Int? = null, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.actualizarEstado(solicitudId, "cancelada", idUsuario, "Cancelado por supervisor")
            stopTicketTracking()
            onDone()
        }
    }
}

