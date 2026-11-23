package com.example.appmovil.ui.home

import android.media.RingtoneManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.layout.ContentScale
import com.example.appmovil.ui.theme.*
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.*
import java.io.File
import androidx.core.content.FileProvider
import androidx.compose.ui.window.Dialog
import android.os.Build
import android.content.Intent
import android.Manifest
import com.example.appmovil.services.SolicitudPollingService
import com.example.appmovil.services.LocationService
import com.example.appmovil.ui.admin.AdminMapScreen
import com.example.appmovil.ui.login.ProfileScreen
import com.example.appmovil.ui.solicitudes.TNPSAdminScreen
import com.example.appmovil.ui.solicitudes.TNPSEvidenceScreen
import com.example.appmovil.ui.solicitudes.SupervisionScreen
import com.example.appmovil.ui.login.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    userId: Int,
    userName: String,
    userRol: String = "usuario",
    userZona: String = "SIN ZONA",
    onLogout: () -> Unit,
    onRegisterUser: () -> Unit,
    onCreateSolicitud: () -> Unit,
    onEditSolicitud: (com.example.appmovil.data.remote.SolicitudItem) -> Unit = {},
    onTrackTicket: (solicitudId: Int, sot: String, tecnico: String) -> Unit = { _, _, _ -> },
    solicitudViewModel: com.example.appmovil.ui.solicitudes.SolicitudViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            val ctx = androidx.compose.ui.platform.LocalContext.current.applicationContext
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return com.example.appmovil.ui.solicitudes.SolicitudViewModel(context = ctx) as T
            }
        }
    ),
    chatViewModel: com.example.appmovil.ui.chat.ChatViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return com.example.appmovil.ui.chat.ChatViewModel(com.example.appmovil.data.repository.AuthRepository(com.example.appmovil.data.remote.RetrofitClient.authApi)) as T
            }
        }
    )
) {
    val cleanRol = userRol.trim().lowercase()
    val isAdmin = cleanRol.contains("admin")
    val isSupervisor = cleanRol.contains("supervisor")
    val isAuxiliar = cleanRol.contains("auxiliar")
    val isTecnico = cleanRol.contains("tecnico")
    val isSatelite = !isAdmin && !isSupervisor && !isAuxiliar && !isTecnico
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val solicitudState by solicitudViewModel.uiState.collectAsState()
    
    val sessionExpired by solicitudViewModel.sessionExpired.collectAsState()
    
    LaunchedEffect(sessionExpired) {
        if (sessionExpired) {
            android.widget.Toast.makeText(context, "Sesión expirada (abierta en otro dispositivo)", android.widget.Toast.LENGTH_LONG).show()
            solicitudViewModel.resetSessionExpired()
            onLogout()
        }
    }

    var currentSection by remember { mutableStateOf(if(isTecnico) "tnps" else "inicio") }
    var selectedFilter by remember { mutableStateOf("TODAS") }
    var previousSolicitudes by remember { mutableStateOf<List<com.example.appmovil.data.remote.SolicitudItem>>(emptyList()) }
    var previousPendingCount by remember { mutableIntStateOf(0) }
    var pendingPhotoId by remember { mutableStateOf<Int?>(null) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }

    val satelliteStatuses by solicitudViewModel.satelliteStatuses.collectAsState()
    val myStatus by solicitudViewModel.myStatus.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val zoningState by solicitudViewModel.zoningState.collectAsState()

    // Estados para Zonificación (Admin)
    var showZoningDialog by remember { mutableStateOf(false) }
    var selectedSatForZoning by remember { mutableStateOf<com.example.appmovil.data.remote.SatelliteStatusItem?>(null) }

    // --- NUEVO: ASIGNACION MANUAL ---
    var showAssignDialog by remember { mutableStateOf(false) }
    var selectedSolicitudForAssign by remember { mutableStateOf<com.example.appmovil.data.remote.SolicitudItem?>(null) }
    var showExitRouteDialog by remember { mutableStateOf(false) }
    var showLunchConfirmDialog by remember { mutableStateOf(false) }

    // --- ALERTA: TICKET ACEPTADO POR SATÉLITE ---
    var acceptedTicketAlert by remember { mutableStateOf<com.example.appmovil.data.remote.AcceptedTicketItem?>(null) }
    val notifiedAcceptedInApp = remember { mutableSetOf<Int>() }

    // 0. Gestión de Permisos (Ubicación y Notificaciones)
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        if (!locationGranted) {
            // Podríamos mostrar un diálogo explicando por qué es necesario
        }
    }

    LaunchedEffect(Unit) {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    // Iniciar Servicio de Fondo para Vigilancia de Tareas y Chat (Todos los roles activos)
    LaunchedEffect(userId) {
        if (userId != 0 && (isSatelite || isSupervisor || isAdmin || isAuxiliar)) {
            // Inicializar la zona y recuperar estado del usuario de forma inmediata
            solicitudViewModel.setCurrentUserZone(userZona)
            if (isSatelite || isSupervisor || isAdmin) {
                solicitudViewModel.fetchUserInfo(userId)
            }

            // 1. Servicio de Polling (Tareas/Chat)
            val pollingIntent = Intent(context, SolicitudPollingService::class.java).apply {
                putExtra("user_id", userId)
                putExtra("user_rol", cleanRol)
                putExtra("user_zona", userZona)
                putExtra("user_name", userName)  // Necesario para polling de tickets aceptados
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(pollingIntent)
            } else {
                context.startService(pollingIntent)
            }

            // 2. Servicio de Ubicación (Satélites, Supervisores, Auxiliares, Tecnicos y Admin)
            if (isSatelite || isSupervisor || isAdmin || isAuxiliar || isTecnico) {
                val locationIntent = Intent(context, LocationService::class.java).apply {
                    putExtra("user_id", userId)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(locationIntent)
                } else {
                    context.startService(locationIntent)
                }
            }
        }
    }

    // Iniciar polling y recuperar estado de almuerzo
    LaunchedEffect(userId) {
        if (userId != 0) {
            solicitudViewModel.checkLunchSavedState(userId, context)
            while(true) {
                try {
                    solicitudViewModel.fetchSolicitudes(
                        userId = if(isSatelite) userId else null,
                        zona = if(isSatelite) solicitudViewModel.currentUserZone.value else if(isAdmin) "ADMIN" else null,
                        supervisor = if(isSupervisor || isAuxiliar || isTecnico) userName else null,
                        tipo = if(isAuxiliar) "auxiliar" else "normal",
                        rol = cleanRol
                    )
                    if (isSupervisor || isAdmin) {
                        solicitudViewModel.fetchSatelliteStatuses()
                        solicitudViewModel.fetchAuxiliarReports(userId, userName, cleanRol)
                    }
                    if (isSatelite || isSupervisor || isAdmin) {
                        solicitudViewModel.fetchUserInfo(userId)
                    }
                    // Refrescar contactos y UNREAD COUNT de forma constante cada 5s
                    chatViewModel.fetchContacts(userId, cleanRol)
                } catch (e: Exception) { e.printStackTrace() }
                
                kotlinx.coroutines.delay(5000) 
            }
        }
    }

    // 2. Sistema de Notificaciones Inteligente
    LaunchedEffect(solicitudState) {
        if (solicitudState is com.example.appmovil.ui.solicitudes.SolicitudUiState.Success) {
            val currentList = (solicitudState as com.example.appmovil.ui.solicitudes.SolicitudUiState.Success).solicitudes
            val currentPendingIds = currentList.filter { it.estado?.lowercase() == "pendiente" }.map { it.id }.toSet()
            val previousPendingIds = previousSolicitudes.filter { it.estado?.lowercase() == "pendiente" }.map { it.id }.toSet()

            // Solo disparar si detectamos IDs nuevos que no estaban en la carga anterior
            val newIds = currentPendingIds - previousPendingIds
            
            if (newIds.isNotEmpty() && isSatelite && previousSolicitudes.isNotEmpty()) {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = "¡Nueva solicitud pendiente de atención!",
                        duration = SnackbarDuration.Long
                    )
                }
                try {
                    val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    val r = RingtoneManager.getRingtone(context, notification)
                    r.play()
                } catch (e: Exception) { e.printStackTrace() }
            }
            previousPendingCount = currentPendingIds.size
            previousSolicitudes = currentList
        }
    }

    // --- POLLING EN PRIMER PLANO: Tickets aceptados por satélite (solo supervisores) ---
    LaunchedEffect(userId, isSupervisor) {
        if (isSupervisor && userId != 0 && userName.isNotEmpty()) {
            while (true) {
                try {
                    val response = com.example.appmovil.data.remote.RetrofitClient.solicitudApi
                        .checkAcceptedTickets(supervisor = userName, sinceSeconds = 15)
                    if (response.isSuccessful) {
                        val newlyAccepted = response.body()?.accepted
                            ?.filter { it.id !in notifiedAcceptedInApp } ?: emptyList()
                        if (newlyAccepted.isNotEmpty()) {
                            // Mostrar solo la primera (la más reciente)
                            acceptedTicketAlert = newlyAccepted.first()
                            newlyAccepted.forEach { notifiedAcceptedInApp.add(it.id) }
                            // Sonido de alarma por exactamente 2 segundos
                            scope.launch {
                                try {
                                    val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                                    val ringtone = RingtoneManager.getRingtone(context, alarmUri)
                                    ringtone.play()
                                    kotlinx.coroutines.delay(2000L) // Dejar sonar 2 segundos
                                    ringtone.stop()
                                } catch (e: Exception) { e.printStackTrace() }
                            }
                        }
                    }
                } catch (e: Exception) { e.printStackTrace() }
                kotlinx.coroutines.delay(10_000) // Verificar cada 10 segundos
            }
        }
    }

    // AUTO-LOGOUT a las 11:00 PM para Satélites
    LaunchedEffect(userId, isSatelite) {
        if (isSatelite && userId != 0) {
            while (true) {
                val now = java.time.LocalTime.now()
                if (now.hour >= 23) {
                    // Registrar salida y cerrar sesión
                    solicitudViewModel.updateMyStatus(userId, "No Disponible")
                    context.stopService(Intent(context, SolicitudPollingService::class.java))
                    context.stopService(Intent(context, LocationService::class.java))
                    snackbarHostState.showSnackbar("Sesión cerrada automáticamente (23:00)")
                    onLogout()
                    break
                }
                kotlinx.coroutines.delay(30_000) // Verificar cada 30 segundos
            }
        }
    }

    // 3. Captura de Foto
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            val uri = photoUri ?: return@rememberLauncherForActivityResult
            val id = pendingPhotoId ?: return@rememberLauncherForActivityResult
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    solicitudViewModel.uploadEvidencia(id, stream.readBytes())
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }
    
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
        val id = pendingPhotoId ?: return@rememberLauncherForActivityResult
        uris.forEach { uri ->
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    solicitudViewModel.uploadEvidencia(id, stream.readBytes())
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    // --- DIÁLOGO DE DISPONIBILIDAD DE SATÉLITES (Después de crear solicitud) ---
    val satelliteAvailability by solicitudViewModel.satelliteAvailability.collectAsState()
    
    if (satelliteAvailability != null) {
        val satStatus = satelliteAvailability!!
        val isBusy = satStatus == "all_busy"
        val isNone = satStatus == "no_satellites"
        val isAvailable = satStatus == "available"
        
        val dialogColor = when {
            isAvailable -> com.example.appmovil.ui.theme.SuccessGreen
            isBusy -> com.example.appmovil.ui.theme.WarningOrange
            else -> com.example.appmovil.ui.theme.ErrorRed
        }
        val dialogIcon = when {
            isAvailable -> Icons.Default.CheckCircle
            isBusy -> Icons.Default.Warning
            else -> Icons.Default.ErrorOutline
        }
        val dialogTitle = when {
            isAvailable -> "Satélites Disponibles"
            isBusy -> "Satélites Ocupados"
            else -> "Sin Satélites en Zona"
        }
        val dialogMsg = when {
            isAvailable -> "Tu solicitud fue enviada exitosamente.\n\nHay satélites disponibles en esta zona que serán notificados de inmediato."
            isBusy -> "Todos los satélites de esta zona están atendiendo otras solicitudes.\n\nTu solicitud será notificada automáticamente al primer satélite que se libere."
            else -> "No hay satélites asignados a esta zona.\n\nLa solicitud quedará en espera hasta que se asigne un satélite."
        }
        
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { solicitudViewModel.resetSatelliteAvailability() },
            icon = {
                Icon(dialogIcon, null, tint = dialogColor, modifier = Modifier.size(48.dp))
            },
            title = {
                Text(dialogTitle, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            },
            text = {
                Text(dialogMsg, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
            },
            confirmButton = {
                Button(
                    onClick = { solicitudViewModel.resetSatelliteAvailability() },
                    colors = ButtonDefaults.buttonColors(containerColor = dialogColor)
                ) {
                    Text("Entendido", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    val contacts by chatViewModel.contacts.collectAsState()
    val totalUnread by remember { 
        derivedStateOf { contacts.sumOf { it.unreadCount } } 
    }

    // 4. Notificaciones de Chat (Sonido en primer plano)
    // 4. Notificaciones de Chat (Sonido en primer plano)
    LaunchedEffect(Unit) {
        chatViewModel.incomingMessageEvent.collect {
            try {
                val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val r = RingtoneManager.getRingtone(context, notification)
                r.play()
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    // 5. Monitoreo de resultado de Zonificación
    LaunchedEffect(zoningState) {
        if (zoningState is com.example.appmovil.ui.solicitudes.CreateSolicitudUiState.Success) {
            snackbarHostState.showSnackbar((zoningState as com.example.appmovil.ui.solicitudes.CreateSolicitudUiState.Success).message)
            solicitudViewModel.resetZoningState()
        } else if (zoningState is com.example.appmovil.ui.solicitudes.CreateSolicitudUiState.Error) {
            snackbarHostState.showSnackbar("Error: ${(zoningState as com.example.appmovil.ui.solicitudes.CreateSolicitudUiState.Error).message}")
            solicitudViewModel.resetZoningState()
        }
    }

    val isGesturesEnabled = currentSection != "admin_map" && currentSection != "attendance" && currentSection != "statistics"

    // --- ALERTA EN PANTALLA: Ticket aceptado por satélite ---
    acceptedTicketAlert?.let { ticket ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { acceptedTicketAlert = null },
            icon = {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(com.example.appmovil.ui.theme.SuccessGreen.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = com.example.appmovil.ui.theme.SuccessGreen,
                        modifier = Modifier.size(40.dp)
                    )
                }
            },
            title = {
                Text(
                    "¡Ticket Aceptado!",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = com.example.appmovil.ui.theme.DeepAzure,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        color = com.example.appmovil.ui.theme.SuccessGreen.copy(alpha = 0.08f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Person, null, tint = com.example.appmovil.ui.theme.SoftAzure, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    ticket.nombreSatelite ?: "Satélite",
                                    fontWeight = FontWeight.Bold,
                                    color = com.example.appmovil.ui.theme.TextDark,
                                    fontSize = 16.sp
                                )
                            }
                            if (!ticket.sot.isNullOrEmpty()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.ConfirmationNumber, null, tint = com.example.appmovil.ui.theme.TextLight, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("SOT: ${ticket.sot}", color = com.example.appmovil.ui.theme.TextLight, fontSize = 13.sp)
                                }
                            }
                            if (!ticket.tecnico.isNullOrEmpty()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Build, null, tint = com.example.appmovil.ui.theme.TextLight, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Técnico: ${ticket.tecnico}", color = com.example.appmovil.ui.theme.TextLight, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "El satélite ya está en camino a atender tu solicitud.",
                        style = MaterialTheme.typography.bodySmall,
                        color = com.example.appmovil.ui.theme.TextLight,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { acceptedTicketAlert = null },
                    colors = ButtonDefaults.buttonColors(containerColor = com.example.appmovil.ui.theme.SuccessGreen),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Done, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Entendido", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color.White,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = isGesturesEnabled,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color.White,
                modifier = Modifier.width(300.dp),
                drawerShape = RoundedCornerShape(0.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    // Header Drawer
                    Box(modifier = Modifier.fillMaxWidth().background(Color.White).padding(24.dp)) {
                        Column {
                            Box(modifier = Modifier.size(64.dp).clip(CircleShape).background(SoftAzure.copy(alpha = 0.1f)).border(1.dp, SoftAzure.copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) {
                                Text(userName.take(1).uppercase(), color = SoftAzure, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineMedium)
                            }
                            Spacer(Modifier.height(16.dp))
                            Text(userName, color = DeepAzure, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                            Text(userRol.uppercase(), color = TextLight, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                        }
                    }
                    HorizontalDivider(color = LightAzure, thickness = 1.dp)
                    Spacer(Modifier.height(16.dp))
                
                // SECCIÓN PERFIL (VISIBLE PARA TODOS) 
                NavigationDrawerItem(
                    label = { Text("Mi Perfil", fontWeight = FontWeight.Bold) },
                    selected = currentSection == "profile",
                    onClick = { 
                        currentSection = "profile"
                        scope.launch { drawerState.close() } 
                    },
                    icon = { Icon(Icons.Outlined.Person, null) },
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent,
                        selectedContainerColor = SoftAzure.copy(alpha = 0.08f),
                        unselectedIconColor = SoftGray,
                        selectedIconColor = SoftAzure,
                        unselectedTextColor = TextLight,
                        selectedTextColor = SoftAzure
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp), color = LightAzure.copy(alpha = 0.3f))
                
                if (!isTecnico) {
                    // ITEMS COMUNES (PANEL Y HISTORIAL)
                    NavigationDrawerItem(
                        label = { Text("Panel de Control") },
                        selected = currentSection == "inicio",
                        onClick = { currentSection = "inicio"; scope.launch { drawerState.close() } },
                        icon = { Icon(Icons.Outlined.Dashboard, null) },
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedContainerColor = Color.Transparent,
                            selectedContainerColor = SoftAzure.copy(alpha = 0.08f),
                            unselectedIconColor = SoftGray,
                            selectedIconColor = SoftAzure,
                            unselectedTextColor = TextLight,
                            selectedTextColor = SoftAzure
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    NavigationDrawerItem(
                        label = { Text("Historial") },
                        selected = currentSection == "historial",
                        onClick = { currentSection = "historial"; scope.launch { drawerState.close() } },
                        icon = { Icon(Icons.Outlined.History, null) },
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedContainerColor = Color.Transparent,
                            selectedContainerColor = SoftAzure.copy(alpha = 0.08f),
                            unselectedIconColor = SoftGray,
                            selectedIconColor = SoftAzure,
                            unselectedTextColor = TextLight,
                            selectedTextColor = SoftAzure
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    if (isSupervisor || isAdmin) {
                        Divider(Modifier.padding(horizontal = 24.dp, vertical = 12.dp), color = LightAzure.copy(alpha = 0.5f))
                        Text("OPERACIONES", modifier = Modifier.padding(24.dp, 8.dp), style = MaterialTheme.typography.labelSmall, color = TextLight)
                        
                        NavigationDrawerItem(
                            label = { Text("Monitoreo Satélites", fontWeight = FontWeight.Bold) },
                            selected = currentSection == "monitoreo",
                            onClick = { 
                                currentSection = "monitoreo"
                                solicitudViewModel.fetchSatelliteStatuses()
                                scope.launch { drawerState.close() } 
                            },
                            icon = { Icon(Icons.Outlined.Dvr, null) },
                            colors = NavigationDrawerItemDefaults.colors(
                                unselectedContainerColor = Color.Transparent,
                                selectedContainerColor = SoftAzure.copy(alpha = 0.08f),
                                unselectedIconColor = SoftGray,
                                selectedIconColor = SoftAzure,
                                unselectedTextColor = TextLight,
                                selectedTextColor = SoftAzure
                            ),
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )

                        NavigationDrawerItem(
                            label = { Text("Reportes Auxiliar", fontWeight = FontWeight.Bold) },
                            selected = currentSection == "reportes_auxiliar",
                            onClick = { 
                                currentSection = "reportes_auxiliar"
                                solicitudViewModel.fetchAuxiliarReports(userId, userName, cleanRol)
                                scope.launch { drawerState.close() } 
                            },
                            icon = { Icon(Icons.Outlined.AssignmentTurnedIn, null) },
                            colors = NavigationDrawerItemDefaults.colors(
                                unselectedContainerColor = Color.Transparent,
                                selectedContainerColor = Color(0xFFFF9800).copy(alpha = 0.08f),
                                unselectedIconColor = SoftGray,
                                selectedIconColor = Color(0xFFFF9800),
                                unselectedTextColor = TextLight,
                                selectedTextColor = Color(0xFFFF9800)
                            ),
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )

                        if (isSupervisor) {
                            NavigationDrawerItem(
                                label = { Text("Validación TNPS", fontWeight = FontWeight.Bold) },
                                selected = currentSection == "tnps_supervisor",
                                onClick = { 
                                    currentSection = "tnps_supervisor"
                                    scope.launch { drawerState.close() } 
                                },
                                icon = { Icon(Icons.Outlined.CheckCircle, null) },
                                colors = NavigationDrawerItemDefaults.colors(
                                    unselectedContainerColor = Color.Transparent,
                                    selectedContainerColor = Color(0xFF9C27B0).copy(alpha = 0.08f),
                                    unselectedIconColor = SoftGray,
                                    selectedIconColor = Color(0xFF9C27B0),
                                    unselectedTextColor = TextLight,
                                    selectedTextColor = Color(0xFF9C27B0)
                                ),
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }

                        if (isAdmin) {
                            NavigationDrawerItem(
                                label = { Text("Reportes Supervisión", fontWeight = FontWeight.Bold) },
                                selected = currentSection == "admin_supervisiones",
                                onClick = { 
                                    currentSection = "admin_supervisiones"
                                    solicitudViewModel.fetchAllSupervisiones()
                                    scope.launch { drawerState.close() } 
                                },
                                icon = { Icon(Icons.Outlined.AssignmentInd, null) },
                                colors = NavigationDrawerItemDefaults.colors(
                                    unselectedContainerColor = Color.Transparent,
                                    selectedContainerColor = Color(0xFF4CAF50).copy(alpha = 0.1f),
                                    unselectedIconColor = TextLight,
                                    selectedIconColor = Color(0xFF4CAF50),
                                    unselectedTextColor = TextLight,
                                    selectedTextColor = Color(0xFF4CAF50)
                                ),
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )

                            NavigationDrawerItem(
                                label = { Text("Verificación TNPS", fontWeight = FontWeight.Bold) },
                                selected = currentSection == "tnps_admin",
                                onClick = { 
                                    currentSection = "tnps_admin"
                                    scope.launch { drawerState.close() } 
                                },
                                icon = { Icon(Icons.Outlined.PhotoLibrary, null) },
                                colors = NavigationDrawerItemDefaults.colors(
                                    unselectedContainerColor = Color.Transparent,
                                    selectedContainerColor = SoftAzure.copy(alpha = 0.1f),
                                    unselectedIconColor = TextLight,
                                    selectedIconColor = SoftAzure,
                                    unselectedTextColor = TextLight,
                                    selectedTextColor = SoftAzure
                                ),
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }

                        if (isSupervisor) {
                            NavigationDrawerItem(
                                label = { Text("Supervisar Equipo", fontWeight = FontWeight.Bold) },
                                selected = currentSection == "supervisar",
                                onClick = { 
                                    currentSection = "supervisar"
                                    scope.launch { drawerState.close() } 
                                },
                                icon = { Icon(Icons.Outlined.FactCheck, null) },
                                colors = NavigationDrawerItemDefaults.colors(
                                    unselectedContainerColor = Color.Transparent,
                                    selectedContainerColor = SoftAzure.copy(alpha = 0.08f),
                                    unselectedIconColor = SoftGray,
                                    selectedIconColor = SoftAzure,
                                    unselectedTextColor = TextLight,
                                    selectedTextColor = SoftAzure
                                ),
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }

                    }

                    if (isAdmin) {
                        Divider(Modifier.padding(horizontal = 24.dp, vertical = 12.dp), color = LightAzure.copy(alpha = 0.5f))
                        Text("GESTIÓN ADMINISTRATIVA", modifier = Modifier.padding(24.dp, 8.dp), style = MaterialTheme.typography.labelSmall, color = TextLight)

                        NavigationDrawerItem(
                            label = { Text("Zonificar Satélites", fontWeight = FontWeight.Bold) },
                            selected = currentSection == "zonificacion",
                            onClick = { 
                                currentSection = "zonificacion"
                                solicitudViewModel.fetchSatelliteStatuses()
                                scope.launch { drawerState.close() } 
                            },
                            icon = { Icon(Icons.Outlined.LocationOn, null) },
                            colors = NavigationDrawerItemDefaults.colors(
                                unselectedContainerColor = Color.Transparent,
                                selectedContainerColor = SoftAzure.copy(alpha = 0.08f),
                                unselectedIconColor = SoftGray,
                                selectedIconColor = SoftAzure,
                                unselectedTextColor = TextLight,
                                selectedTextColor = SoftAzure
                            ),
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )

                        NavigationDrawerItem(
                            label = { Text("Gestión de Solicitudes", fontWeight = FontWeight.Bold) },
                            selected = currentSection == "gestion_solicitudes",
                            onClick = { 
                                currentSection = "gestion_solicitudes"
                                scope.launch { drawerState.close() } 
                            },
                            icon = { Icon(Icons.Outlined.Assignment, null) },
                            colors = NavigationDrawerItemDefaults.colors(
                                unselectedContainerColor = Color.Transparent,
                                selectedContainerColor = SoftAzure.copy(alpha = 0.1f),
                                unselectedIconColor = TextLight,
                                selectedIconColor = SoftAzure,
                                unselectedTextColor = TextLight,
                                selectedTextColor = SoftAzure
                            ),
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        
                        NavigationDrawerItem(
                            label = { Text("Registrar Usuario", fontWeight = FontWeight.Bold) },
                            selected = false,
                            onClick = { 
                                scope.launch { drawerState.close() }
                                onRegisterUser() 
                            },
                            icon = { Icon(Icons.Outlined.PersonAdd, null) },
                            colors = NavigationDrawerItemDefaults.colors(
                                unselectedContainerColor = Color.Transparent,
                                selectedContainerColor = SoftAzure.copy(alpha = 0.1f),
                                unselectedIconColor = TextLight,
                                selectedIconColor = SoftAzure,
                                unselectedTextColor = TextLight,
                                selectedTextColor = SoftAzure
                            ),
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )

                        NavigationDrawerItem(
                            label = { Text("Mapa de Personal", fontWeight = FontWeight.Bold) },
                            selected = currentSection == "admin_map",
                            onClick = { 
                                currentSection = "admin_map"
                                scope.launch { drawerState.close() } 
                            },
                            icon = { Icon(Icons.Outlined.Map, null) },
                            colors = NavigationDrawerItemDefaults.colors(
                                unselectedContainerColor = Color.Transparent,
                                selectedContainerColor = SoftAzure.copy(alpha = 0.1f),
                                unselectedIconColor = TextLight,
                                selectedIconColor = SoftAzure,
                                unselectedTextColor = TextLight,
                                selectedTextColor = SoftAzure
                            ),
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )

                        NavigationDrawerItem(
                            label = { Text("Estadísticas y Reportes", fontWeight = FontWeight.Bold) },
                            selected = currentSection == "statistics",
                            onClick = { 
                                currentSection = "statistics"
                                scope.launch { drawerState.close() } 
                            },
                            icon = { Icon(Icons.Outlined.BarChart, null) },
                            colors = NavigationDrawerItemDefaults.colors(
                                unselectedContainerColor = Color.Transparent,
                                selectedContainerColor = SoftAzure.copy(alpha = 0.1f),
                                unselectedIconColor = TextLight,
                                selectedIconColor = SoftAzure,
                                unselectedTextColor = TextLight,
                                selectedTextColor = SoftAzure
                            ),
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )

                        NavigationDrawerItem(
                            label = { Text("Control de Asistencia", fontWeight = FontWeight.Bold) },
                            selected = currentSection == "attendance",
                            onClick = { 
                                currentSection = "attendance"
                                scope.launch { drawerState.close() } 
                            },
                            icon = { Icon(Icons.Outlined.EventAvailable, null) },
                            colors = NavigationDrawerItemDefaults.colors(
                                unselectedContainerColor = Color.Transparent,
                                selectedContainerColor = SoftAzure.copy(alpha = 0.1f),
                                unselectedIconColor = TextLight,
                                selectedIconColor = SoftAzure,
                                unselectedTextColor = TextLight,
                                selectedTextColor = SoftAzure
                            ),
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )

                        NavigationDrawerItem(
                            label = { Text("Productividad Supervisores", fontWeight = FontWeight.Bold) },
                            selected = currentSection == "supervisor_stats",
                            onClick = { 
                                currentSection = "supervisor_stats"
                                scope.launch { drawerState.close() } 
                            },
                            icon = { Icon(Icons.Outlined.TrendingUp, null) },
                            colors = NavigationDrawerItemDefaults.colors(
                                unselectedContainerColor = Color.Transparent,
                                selectedContainerColor = SoftAzure.copy(alpha = 0.1f),
                                unselectedIconColor = TextLight,
                                selectedIconColor = SoftAzure,
                                unselectedTextColor = TextLight,
                                selectedTextColor = SoftAzure
                            ),
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }

                    // CHAT (Para todos los que no son técnicos)
                    NavigationDrawerItem(
                        label = { 
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Chat del Equipo", fontWeight = FontWeight.Bold)
                            }
                        },
                        selected = currentSection == "chat",
                        onClick = { 
                            currentSection = "chat"
                            chatViewModel.fetchContacts(userId, cleanRol)
                            scope.launch { drawerState.close() } 
                        },
                        icon = { Icon(Icons.Outlined.Chat, null) },
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedContainerColor = Color.Transparent,
                            selectedContainerColor = SoftAzure.copy(alpha = 0.08f),
                            unselectedIconColor = SoftGray,
                            selectedIconColor = SoftAzure,
                            unselectedTextColor = TextLight,
                            selectedTextColor = SoftAzure
                        ),
                        badge = {
                            if (totalUnread > 0) {
                                Badge(containerColor = ErrorRed) {
                                    Text(totalUnread.toString(), color = Color.White)
                                }
                            }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                } else {
                    // EVIDENCIA TNPS (Solo para Técnicos)
                    NavigationDrawerItem(
                        label = { Text("Evidencia TNPS", fontWeight = FontWeight.Bold) },
                        selected = currentSection == "tnps",
                        onClick = { currentSection = "tnps"; scope.launch { drawerState.close() } },
                        icon = { Icon(Icons.Outlined.AssignmentTurnedIn, null) },
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedContainerColor = Color.Transparent,
                            selectedContainerColor = SoftAzure.copy(alpha = 0.08f),
                            unselectedIconColor = SoftGray,
                            selectedIconColor = SoftAzure,
                            unselectedTextColor = TextLight,
                            selectedTextColor = SoftAzure
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    // HISTORIAL TNPS (Solo para Técnicos)
                    NavigationDrawerItem(
                        label = { Text("Historial TNPS", fontWeight = FontWeight.Bold) },
                        selected = currentSection == "tnps_historial",
                        onClick = { currentSection = "tnps_historial"; scope.launch { drawerState.close() } },
                        icon = { Icon(Icons.Outlined.History, null) },
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedContainerColor = Color.Transparent,
                            selectedContainerColor = SoftAzure.copy(alpha = 0.08f),
                            unselectedIconColor = SoftGray,
                            selectedIconColor = SoftAzure,
                            unselectedTextColor = TextLight,
                            selectedTextColor = SoftAzure
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }

                Spacer(Modifier.height(24.dp))
                
                NavigationDrawerItem(
                    label = { Text("Cerrar Sesión") },
                    selected = false,
                    onClick = { 
                        scope.launch { 
                            // Si es Satélite y estaba activo, marcar SALIDA antes de salir
                            if (isSatelite && myStatus?.estado != "No Disponible") {
                                solicitudViewModel.updateMyStatus(idUsuario = userId, estado = "No Disponible")
                            }
                            drawerState.close() 
                            // Detenemos los servicios de fondo al salir
                            context.stopService(Intent(context, SolicitudPollingService::class.java))
                            context.stopService(Intent(context, LocationService::class.java))
                            onLogout() 
                        } 
                    },
                    icon = { Icon(Icons.Outlined.Logout, null, tint = ErrorRed) },
                    modifier = Modifier.padding(12.dp),
                    colors = NavigationDrawerItemDefaults.colors(unselectedTextColor = ErrorRed)
                )
                
                Spacer(Modifier.height(24.dp))
                }
            }
        }
    ) {
        if (currentSection == "admin_map" && (isAdmin || isSupervisor)) {
            AdminMapScreen(userRol = cleanRol, onBack = { currentSection = "inicio" })
        } else if (currentSection == "tnps_admin" && isAdmin) {
            TNPSAdminScreen(viewModel = solicitudViewModel)
        } else if (currentSection == "tnps_supervisor" && isSupervisor) {
            com.example.appmovil.ui.solicitudes.TNPSValidationScreen(idSupervisor = userId, viewModel = solicitudViewModel)
        } else if (currentSection == "tnps_historial") {
            com.example.appmovil.ui.solicitudes.TNPSHistoryScreen(idTecnico = userId, viewModel = solicitudViewModel)
        } else if (currentSection == "profile") {
            ProfileScreen(
                userId = userId,
                onBack = { currentSection = "inicio" }
            )
        } else {
            Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (totalUnread > 0) "CI-GESTIÓN ($totalUnread)" else "CI-GESTIÓN",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = SoftAzure
                                )
                            )
                            Text("ID:$userId | ${cleanRol.uppercase()}", style = MaterialTheme.typography.labelSmall, color = TextLight)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            BadgedBox(
                                badge = {
                                    if (totalUnread > 0) {
                                        Badge(containerColor = ErrorRed, modifier = Modifier.size(18.dp)) {
                                            Text(totalUnread.toString(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Menu, null, tint = SoftAzure)
                            }
                        }
                    },
                    actions = {
                        if (isSatelite) {
                            // Mostrar estado de forma automática (Lectura)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Circle, 
                                    contentDescription = null, 
                                    modifier = Modifier.size(10.dp), 
                                    tint = when(myStatus?.estado) {
                                        "Disponible" -> SuccessGreen
                                        "Ocupado" -> ErrorRed
                                        "Almorzando" -> WarningOrange
                                        else -> Color.Gray
                                    }
                                )
                                Spacer(Modifier.width(4.dp))
                                Text((myStatus?.estado ?: "CARGANDO...").uppercase(Locale.ROOT), fontWeight = FontWeight.Bold, color = SoftAzure, fontSize = 12.sp)
                                
                                // Botón de Almuerzo (Solo si está disponible, no ha almorzado hoy y NO tiene tareas activas)
                                val almuerzoYaUsado by solicitudViewModel.almuerzoUsado.collectAsState()
                                val hasActiveTask = remember(solicitudState) {
                                    if (solicitudState is com.example.appmovil.ui.solicitudes.SolicitudUiState.Success) {
                                        (solicitudState as com.example.appmovil.ui.solicitudes.SolicitudUiState.Success).solicitudes.any {
                                            it.idSatelite == userId && it.estado?.lowercase() in listOf("aceptada", "camino", "en_camino", "llegado", "corregir")
                                        }
                                    } else false
                                }
                                
                                if (myStatus?.estado == "Disponible" && !almuerzoYaUsado && !hasActiveTask) {
                                    IconButton(onClick = { showLunchConfirmDialog = true }) {
                                        Icon(Icons.Default.Restaurant, "Almuerzo", tint = WarningOrange, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                        if (isSatelite) {
                            val currentDynamicZone by solicitudViewModel.currentUserZone.collectAsState()
                            
                            // Botón para Desactivar Ruta (Solo si ya inició)
                            if (myStatus?.estado != "No Disponible") {
                                IconButton(onClick = { 
                                    showExitRouteDialog = true
                                }) {
                                    Icon(Icons.Default.PowerSettingsNew, "Cerrar Ruta", tint = ErrorRed)
                                }
                            }

                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "DISTRITO: $currentDynamicZone",
                                style = MaterialTheme.typography.labelSmall,
                                color = SoftAzure,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(onClick = { 
                            solicitudViewModel.fetchSolicitudes(
                                userId = if(isSatelite) userId else null,
                                zona = if(isSatelite) solicitudViewModel.currentUserZone.value else if(isAdmin) "ADMIN" else null,
                                supervisor = if(isSupervisor || isAuxiliar || isTecnico) userName else null,
                                tipo = if(isAuxiliar) "auxiliar" else "normal",
                                rol = cleanRol
                            ) 
                        }) { Icon(Icons.Outlined.Refresh, null, tint = SoftAzure) }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.White,
                        titleContentColor = DeepAzure,
                        navigationIconContentColor = SoftAzure,
                        actionIconContentColor = SoftAzure
                    )
                )
                HorizontalDivider(color = LightAzure, thickness = 1.dp)
            },
            floatingActionButton = {
                if ((isSupervisor || isAdmin || isAuxiliar) && currentSection == "inicio") {
                    ExtendedFloatingActionButton(
                        onClick = onCreateSolicitud, 
                        containerColor = if (isAuxiliar) Color(0xFFFF9800) else SoftAzure, 
                        contentColor = Color.White,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (isAuxiliar) "NUEVA PLANTILLA" else "NUEVA SOLICITUD", fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            containerColor = SoftWhite
        ) { innerPadding ->
            val lunchRemaining by solicitudViewModel.lunchTimeRemaining.collectAsState()
            
            Box(Modifier.fillMaxSize().padding(innerPadding)) {
                Column(Modifier.fillMaxSize()) {
                    // Banner de Almuerzo activo
                    if (myStatus?.estado == "Almorzando") {
                        val minutes = (lunchRemaining / 60)
                        val seconds = (lunchRemaining % 60)
                        val timeStr = "%02d:%02d".format(minutes, seconds)
                        
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            colors = CardDefaults.cardColors(containerColor = WarningOrange.copy(alpha = 0.1f)),
                            border = BorderStroke(1.dp, WarningOrange)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Timer, null, tint = WarningOrange)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("TIEMPO DE ALMUERZO ACTIVO", fontWeight = FontWeight.Black, fontSize = 10.sp, color = WarningOrange)
                                    Text("Retorno en: $timeStr", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DeepAzure)
                                }
                                Button(
                                    onClick = { solicitudViewModel.stopLunch(userId, context) },
                                    colors = ButtonDefaults.buttonColors(containerColor = WarningOrange),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("RETORNAR AHORA", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                    if (currentSection == "supervisar") {
                        SupervisionScreen(
                            supervisorId = userId,
                            viewModel = solicitudViewModel
                        )
                    }
                    if (currentSection == "tnps") {
                        TNPSEvidenceScreen(
                            userId = userId,
                            userName = userName,
                            viewModel = solicitudViewModel
                        )
                    } else if (currentSection == "chat") {
                        ChatScreen(chatViewModel, userId, cleanRol)
                    } else if (currentSection == "monitoreo") {
                        val filteredSats = remember(satelliteStatuses, solicitudState) {
                            val solicitudes = (solicitudState as? com.example.appmovil.ui.solicitudes.SolicitudUiState.Success)?.solicitudes ?: emptyList()
                            val activeSatIds = solicitudes
                                .filter { it.estado?.lowercase() in listOf("aceptada", "camino", "en_camino", "llegado", "corregir") }
                                .mapNotNull { it.idSatelite }
                                .toSet()

                            satelliteStatuses
                                .filter { it.rol?.lowercase()?.trim() == "satelite" }
                                .sortedWith(compareByDescending<com.example.appmovil.data.remote.SatelliteStatusItem> { activeSatIds.contains(it.id) }
                                    .thenBy { it.nombre })
                        }
                        com.example.appmovil.ui.home.SatelliteMonitorView(
                            satellites = filteredSats,
                            allSolicitudes = when (val s = solicitudState) {
                                is com.example.appmovil.ui.solicitudes.SolicitudUiState.Success -> s.solicitudes
                                else -> emptyList()
                            },
                            isAdmin = isAdmin, // Habilitar herramientas si es Admin
                            onResetLunch = { satId -> solicitudViewModel.resetLunch(satId, context) }
                        )
                    } else if (currentSection == "statistics") {
                        com.example.appmovil.ui.admin.StatisticsScreen(
                            solicitudes = when (val s = solicitudState) {
                                is com.example.appmovil.ui.solicitudes.SolicitudUiState.Success -> s.solicitudes
                                else -> emptyList()
                            }
                        )
                    } else if (currentSection == "attendance") {
                        com.example.appmovil.ui.admin.AttendanceScreen(
                            viewModel = solicitudViewModel
                        )
                    } else if (currentSection == "zonificacion") {
                        com.example.appmovil.ui.home.SatelliteMonitorView(
                            satellites = satelliteStatuses.filter { it.rol?.lowercase()?.trim() == "satelite" },
                            allSolicitudes = when (val s = solicitudState) {
                                is com.example.appmovil.ui.solicitudes.SolicitudUiState.Success -> s.solicitudes
                                else -> emptyList()
                            },
                            isAdmin = true, // Modo edición en Zonificación
                            onZoneClick = { sat ->
                                selectedSatForZoning = sat
                                showZoningDialog = true
                            },
                            onResetLunch = { satId -> solicitudViewModel.resetLunch(satId, context) }
                        )
                    } else if (currentSection == "gestion_solicitudes") {
                        // Sección de Gestión Total de Solicitudes (Filtros Maestro)
                        when (val state = solicitudState) {
                            is com.example.appmovil.ui.solicitudes.SolicitudUiState.Loading -> {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = SoftAzure)
                                }
                            }
                            is com.example.appmovil.ui.solicitudes.SolicitudUiState.Success -> {
                                // En Gestión de solicitudes, Admin puede filtrar por TODO
                                SummaryStatsBar(solicitudes = state.solicitudes, selectedFilter = selectedFilter, onFilterSelect = { selectedFilter = it })
                                
                                val fullList = state.solicitudes.filter { 
                                    when (selectedFilter) {
                                        "PENDIENTES" -> it.estado?.lowercase() == "pendiente"
                                        "CORREGIR" -> it.estado?.lowercase() == "corregir"
                                        "EN CURSO" -> it.estado?.lowercase() in listOf("aceptada", "camino", "llegado")
                                        "LISTAS" -> it.estado?.lowercase() == "completada"
                                        "RECHAZO" -> it.estado?.lowercase() == "rechazada"
                                        "ANULADAS" -> it.estado?.lowercase() == "anulada"
                                        else -> true
                                    }
                                }
                                
                                if (fullList.isEmpty()) {
                                    EmptyStateView("gestion")
                                } else {
                                    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)) {
                                        items(fullList) { sol ->
                                            com.example.appmovil.ui.home.SolicitudItemCard(
                                                solicitud = sol,
                                                isSatelite = false,
                                                isAdmin = true,
                                                onStatusUpdate = { id, nue, com -> solicitudViewModel.actualizarEstado(id = id, estado = nue, idUsuario = null, comentario = com, rol = cleanRol) },
                                                onEdit = { onEditSolicitud(it) },
                                                onAssign = {
                                                    selectedSolicitudForAssign = it
                                                    showAssignDialog = true
                                                },
                                                onTrackTicket = onTrackTicket
                                            )
                                        }
                                    }
                                }
                            }
                            else -> {}
                        }
                    } else if (currentSection == "reportes_auxiliar") {
                        // --- SECCIÓN: Reportes de Auxiliar (Para Supervisores/Admin) ---
                        val auxiliarState by solicitudViewModel.auxiliarReports.collectAsState()

                        // Asegurar carga al entrar a la sección
                        LaunchedEffect(Unit) {
                            solicitudViewModel.fetchAuxiliarReports(userId, userName, cleanRol)
                        }

                        Column(Modifier.fillMaxSize()) {
                            // Header
                            Surface(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                color = Color(0xFFFF9800).copy(alpha = 0.08f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFFFF9800).copy(alpha = 0.3f))
                            ) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.AssignmentTurnedIn, null, tint = Color(0xFFFF9800), modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text("REPORTES DE AUXILIAR", fontWeight = FontWeight.Black, fontSize = 11.sp, color = Color(0xFFFF9800))
                                        Text("Plantillas de asistencia pendientes de verificación", style = MaterialTheme.typography.bodySmall, color = TextLight)
                                    }
                                    IconButton(onClick = { solicitudViewModel.fetchAuxiliarReports(userId, userName, cleanRol) }) {
                                        Icon(Icons.Outlined.Refresh, null, tint = Color(0xFFFF9800))
                                    }
                                }
                            }

                            when (auxiliarState) {
                                is com.example.appmovil.ui.solicitudes.SolicitudUiState.Loading -> {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(color = Color(0xFFFF9800))
                                    }
                                }
                                is com.example.appmovil.ui.solicitudes.SolicitudUiState.Success -> {
                                    val auxList = (auxiliarState as com.example.appmovil.ui.solicitudes.SolicitudUiState.Success).solicitudes
                                    if (auxList.isEmpty()) {
                                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(Icons.Outlined.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(48.dp))
                                                Spacer(Modifier.height(12.dp))
                                                Text("Sin reportes pendientes", fontWeight = FontWeight.Bold, color = TextDark)
                                                Text("No hay plantillas de auxiliar por verificar", style = MaterialTheme.typography.bodySmall, color = TextLight)
                                            }
                                        }
                                    } else {
                                        LazyColumn(
                                            Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp),
                                            contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
                                        ) {
                                            items(auxList) { sol ->
                                                AuxiliarReportCard(
                                                    solicitud = sol,
                                                    onAccept = { solicitudViewModel.actualizarEstado(id = sol.id, estado = "aceptada", idUsuario = userId); solicitudViewModel.fetchAuxiliarReports(zona = null, supervisor = userName, tipo = "auxiliar") },
                                                    onReject = { comment -> solicitudViewModel.actualizarEstado(id = sol.id, estado = "rechazada", idUsuario = userId, comentario = comment); solicitudViewModel.fetchAuxiliarReports(zona = null, supervisor = userName, tipo = "auxiliar") },
                                                    onComplete = { solicitudViewModel.actualizarEstado(id = sol.id, estado = "completada", idUsuario = userId); solicitudViewModel.fetchAuxiliarReports(zona = null, supervisor = userName, tipo = "auxiliar") }
                                                )
                                            }
                                        }
                                    }
                                }
                                is com.example.appmovil.ui.solicitudes.SolicitudUiState.Error -> {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("Error al cargar reportes", color = ErrorRed)
                                    }
                                }
                            }
                        }
                    } else if (currentSection == "admin_supervisiones") {
                        val supervisionReports by solicitudViewModel.supervisionReports.collectAsState()
                        AdminSupervisionReports(
                            reports = supervisionReports,
                            onRefresh = { solicitudViewModel.fetchAllSupervisiones() }
                        )
                    } else if (currentSection == "supervisor_stats") {
                        val prodStats by solicitudViewModel.supervisorProductivity.collectAsState()
                        
                        LaunchedEffect(Unit) {
                            solicitudViewModel.fetchSupervisorProductivity()
                        }

                        SupervisorProductivityScreen(
                            stats = prodStats,
                            onRefresh = { solicitudViewModel.fetchSupervisorProductivity() }
                        )
                    } else {
                        // --- LÓGICA DE SATÉLITE / AUXILIAR: INICIO DE RUTA ---
                        if (isSatelite && myStatus?.estado == "No Disponible") {
                            RouteNotStartedView(
                                onStartRoute = { 
                                    solicitudViewModel.updateMyStatus(idUsuario = userId, estado = "Disponible") 
                                    
                                    // Reiniciar servicios al iniciar ruta
                                    val pollingIntent = Intent(context, SolicitudPollingService::class.java).apply {
                                        putExtra("user_id", userId)
                                        putExtra("user_rol", cleanRol)
                                        putExtra("user_zona", userZona)
                                    }
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        context.startForegroundService(pollingIntent)
                                    } else {
                                        context.startService(pollingIntent)
                                    }

                                    if (isSatelite || isSupervisor || isAdmin || isAuxiliar || isTecnico) {
                                        val locationIntent = Intent(context, LocationService::class.java).apply {
                                            putExtra("user_id", userId)
                                        }
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                            context.startForegroundService(locationIntent)
                                        } else {
                                            context.startService(locationIntent)
                                        }
                                    }
                                }
                            )
                        } else {
                            when (val state = solicitudState) {
                                 is com.example.appmovil.ui.solicitudes.SolicitudUiState.Loading -> {
                                     Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                         CircularProgressIndicator(color = SoftAzure)
                                     }
                                 }
                                 is com.example.appmovil.ui.solicitudes.SolicitudUiState.Success -> {
                                    val baseList = state.solicitudes.filter { 
                                        if (currentSection == "inicio") it.estado?.lowercase() !in listOf("completada", "rechazada", "anulada")
                                        else it.estado?.lowercase() in listOf("completada", "rechazada", "anulada")
                                    }

                                    if ((isSupervisor || isAdmin) && currentSection == "inicio") {
                                        if (isSupervisor) {
                                            val currentSupCount by solicitudViewModel.supervisionesHoy.collectAsState()
                                            DailySupervisionRecord(count = currentSupCount)
                                        }
                                        SummaryStatsBar(solicitudes = state.solicitudes, selectedFilter = selectedFilter, onFilterSelect = { selectedFilter = it })
                                    }
                                    val filteredList = baseList.filter { it ->
                                        if (isSatelite) {
                                            // Siempre mostrar: mis tareas activas + TODAS las pendientes de cualquier zona
                                            it.idSatelite == userId || (
                                                (it.idSatelite ?: 0) == 0 && 
                                                it.estado?.lowercase() in listOf("pendiente", "corregir")
                                            )
                                        } else {
                                            when (selectedFilter) {
                                                "PENDIENTES" -> it.estado?.lowercase() == "pendiente"
                                                "CORREGIR" -> it.estado?.lowercase() == "corregir"
                                                "EN CURSO" -> it.estado?.lowercase() in listOf("aceptada", "camino", "en_camino", "llegado")
                                                "LISTAS" -> it.estado?.lowercase() == "completada"
                                                "RECHAZO" -> it.estado?.lowercase() == "rechazada"
                                                "ANULADAS" -> it.estado?.lowercase() == "anulada"
                                                else -> true
                                            }
                                        }
                                    }.let { list ->
                                        if (isSatelite) {
                                            val miZona = solicitudViewModel.currentUserZone.value.uppercase()
                                            list.sortedWith(compareBy<com.example.appmovil.data.remote.SolicitudItem> { 
                                                if (it.zona?.uppercase() == miZona) 0 else 1 
                                            }.thenBy { it.id })
                                        } else list
                                    }

                                    if (filteredList.isEmpty()) {
                                        EmptyStateView(currentSection)
                                    } else {
                                        // Calcular el ID de la solicitud más antigua de MI ZONA para FIFO
                                        val myZone = solicitudViewModel.currentUserZone.value.uppercase()
                                        val firstPendingId = remember(filteredList, isSatelite, myZone) {
                                            if (isSatelite) {
                                                filteredList.filter { 
                                                    it.estado?.lowercase() in listOf("pendiente", "corregir") &&
                                                    it.zona?.uppercase() == myZone
                                                }.minByOrNull { it.id }?.id
                                            } else null
                                        }

                                        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)) {
                                             items(filteredList) { sol ->
                                                 if (isAuxiliar) {
                                                     AuxiliarReportCard(
                                                         solicitud = sol,
                                                         isAuxiliarView = true,
                                                         onTakePhoto = { id -> 
                                                             pendingPhotoId = id
                                                             val file = File(context.cacheDir, "images").apply { mkdirs() }.let { File(it, "img_${System.currentTimeMillis()}.jpg") }
                                                             photoUri = FileProvider.getUriForFile(context, "com.example.appmovil.fileprovider", file)
                                                             cameraLauncher.launch(photoUri!!)
                                                         },
                                                         onPickPhoto = { id -> pendingPhotoId = id; galleryLauncher.launch("image/*") },
                                                         onDeletePhoto = { id, fotoUrl -> solicitudViewModel.deleteEvidencia(id, fotoUrl) },
                                                         onRequestValidation = { id ->
                                                             // Ahora que el supervisor se elige al CREAR, solo cambiamos el estado
                                                             solicitudViewModel.actualizarEstado(id = id, estado = "por_validar")
                                                         }
                                                     )
                                                 } else {
                                                     // Bloquear: zona ajena = siempre bloqueada | mi zona = FIFO (solo la más antigua desbloqueada)
                                                     val isMyZone = sol.zona?.uppercase() == myZone
                                                     val isLocked = isSatelite && 
                                                                  sol.estado?.lowercase() in listOf("pendiente", "corregir") &&
                                                                  (!isMyZone || (firstPendingId != null && sol.id != firstPendingId))

                                                     com.example.appmovil.ui.home.SolicitudItemCard(
                                                         solicitud = sol,
                                                         isSatelite = isSatelite,
                                                         isAdmin = isAdmin,
                                                         userZona = solicitudViewModel.currentUserZone.value,
                                                         isLocked = isLocked,
                                                         onStatusUpdate = { id, nue, com -> 
                                                             solicitudViewModel.actualizarEstado(id = id, estado = nue, idUsuario = if(isSatelite) userId else null, comentario = com, rol = cleanRol)
                                                             if (nue == "aceptada") solicitudViewModel.updateMyStatus(idUsuario = userId, estado = "Ocupado")
                                                             if (nue in listOf("completada", "rechazada")) solicitudViewModel.updateMyStatus(idUsuario = userId, estado = "Disponible")
                                                         },
                                                         onEdit = { onEditSolicitud(it) },
                                                         onAssign = {
                                                             selectedSolicitudForAssign = it
                                                             showAssignDialog = true
                                                         },
                                                         onTakePhoto = { id -> 
                                                             pendingPhotoId = id
                                                             val file = File(context.cacheDir, "images").apply { mkdirs() }.let { File(it, "img_${System.currentTimeMillis()}.jpg") }
                                                             photoUri = FileProvider.getUriForFile(context, "com.example.appmovil.fileprovider", file)
                                                             cameraLauncher.launch(photoUri!!)
                                                         },
                                                         onPickPhoto = { id -> pendingPhotoId = id; galleryLauncher.launch("image/*") },
                                                         onDeletePhoto = { id, fotoUrl -> solicitudViewModel.deleteEvidencia(id, fotoUrl) },
                                                         onTrackTicket = onTrackTicket
                                                     )
                                                 }
                                             }
                                        }
                                    }
                                }
                                is com.example.appmovil.ui.solicitudes.SolicitudUiState.Error -> {
                                    ErrorView(state.message)
                                }
                                else -> { /* Idle */ }
                            }
                        }
                    }
                }
            }
        }

        // --- DIÁLOGOS DE ADMINISTRACIÓN ---
        if (showZoningDialog && selectedSatForZoning != null) {
            val dynamicZones by solicitudViewModel.availableZones.collectAsState()
            com.example.appmovil.ui.home.ZonificacionDialog(
                satelliteName = selectedSatForZoning!!.nombre,
                currentZone = selectedSatForZoning!!.zona ?: "SIN ZONA",
                availableZones = dynamicZones,
                onDismiss = { showZoningDialog = false },
                onConfirm = { nuevaZona ->
                    solicitudViewModel.updateUserZone(selectedSatForZoning!!.id, nuevaZona)
                    showZoningDialog = false
                    // El Snackbar ahora se dispara desde el LaunchedEffect(zoningState) con el resultado REAL del servidor
                }
            )
        }

        if (showAssignDialog && selectedSolicitudForAssign != null) {
            val sList = when (val s = satelliteStatuses) {
                else -> s // es un List<SatelliteStatusItem> directamente en el ViewModel actual
            }
            AssignSatelliteDialog(
                solicitudId = selectedSolicitudForAssign!!.id,
                sot = selectedSolicitudForAssign!!.sot ?: "---",
                satellites = sList,
                onDismiss = { showAssignDialog = false },
                onConfirm = { satelliteId ->
                    solicitudViewModel.actualizarEstado(id = selectedSolicitudForAssign!!.id, estado = "aceptada", idUsuario = satelliteId, rol = cleanRol)
                    showAssignDialog = false
                })
        }

        if (showExitRouteDialog) {
            AlertDialog(
                onDismissRequest = { showExitRouteDialog = false },
                icon = { Icon(Icons.Default.PowerSettingsNew, null, tint = ErrorRed) },
                title = { Text("Finalizar Ruta") },
                text = { Text("¿Estás seguro de que deseas desactivar tu ruta de hoy? Dejarás de recibir solicitudes hasta que inicies nuevamente.") },
                confirmButton = {
                    Button(
                        onClick = {
                            solicitudViewModel.updateMyStatus(idUsuario = userId, estado = "No Disponible")
                            // Detener servicios al finalizar jornada
                            context.stopService(Intent(context, SolicitudPollingService::class.java))
                            context.stopService(Intent(context, LocationService::class.java))
                            showExitRouteDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                    ) { Text("SÍ, FINALIZAR JORNADA", color = Color.White) }
                },
                dismissButton = {
                    TextButton(onClick = { showExitRouteDialog = false }) { Text("CANCELAR") }
                },
                containerColor = Color.White,
                tonalElevation = 8.dp
            )
        }

        if (showLunchConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showLunchConfirmDialog = false },
                icon = { Icon(Icons.Default.Restaurant, null, tint = WarningOrange) },
                title = { Text("Iniciar Almuerzo") },
                text = { Text("¿Estás seguro de que deseas iniciar tu hora de almuerzo ahora? Tu estado cambiará a 'Almorzando' por 60 minutos.") },
                confirmButton = {
                    Button(
                        onClick = {
                            solicitudViewModel.startLunch(userId, context)
                            showLunchConfirmDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WarningOrange)
                    ) { Text("INICIAR DESCANSO", color = Color.White) }
                },
                dismissButton = {
                    TextButton(onClick = { showLunchConfirmDialog = false }) { Text("CANCELAR") }
                },
                containerColor = Color.White,
                tonalElevation = 8.dp
            )
        }
    }
}
}

@Composable
fun SatelliteMonitorView(
    satellites: List<com.example.appmovil.data.remote.SatelliteStatusItem>,
    allSolicitudes: List<com.example.appmovil.data.remote.SolicitudItem> = emptyList(),
    isAdmin: Boolean = false,
    onZoneClick: (com.example.appmovil.data.remote.SatelliteStatusItem) -> Unit = {},
    onResetLunch: (Int) -> Unit = {}
) {
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)) {
        items(satellites) { sat ->
            val color = when(sat.estado) { "Disponible" -> SuccessGreen; "Ocupado" -> ErrorRed; else -> WarningOrange }
            val hasZone = !sat.zona.isNullOrEmpty() && sat.zona != "SIN ZONA"
            
            // Buscar solicitudes activas de este satélite
            val activeTasks = allSolicitudes.filter { 
                it.idSatelite == sat.id && 
                it.estado?.lowercase() in listOf("aceptada", "camino", "en_camino", "llegado", "corregir")
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(BorderStroke(1.dp, if(isAdmin && !hasZone) ErrorRed.copy(0.5f) else LightAzure.copy(0.5f)), RoundedCornerShape(16.dp))
                    .let { if(isAdmin) it.clickable { onZoneClick(sat) } else it }
            ) {
                Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(48.dp).clip(CircleShape).background(color.copy(0.1f)), contentAlignment = Alignment.Center) {
                        Text(sat.nombre.take(1).uppercase(), color = color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(sat.nombre, fontWeight = FontWeight.Black, color = TextDark, style = MaterialTheme.typography.bodyLarge)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = color.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = (sat.estado ?: "Desconocido").uppercase(), 
                                    color = color, 
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text("•", color = TextLight.copy(0.5f))
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Default.LocationOn, null, tint = if(hasZone) SoftAzure else ErrorRed, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(2.dp))
                            Text(
                                text = if(hasZone) sat.zona!! else "ASIGNAR ZONA", 
                                color = if(hasZone) DeepAzure else ErrorRed, 
                                style = MaterialTheme.typography.labelSmall, 
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (isAdmin) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (sat.almuerzoUsado == 1) {
                                IconButton(onClick = { onResetLunch(sat.id) }) {
                                    Icon(Icons.Default.Restaurant, "Reset Almuerzo", tint = WarningOrange, modifier = Modifier.size(20.dp))
                                }
                            }
                            IconButton(onClick = { onZoneClick(sat) }) {
                                Icon(Icons.Default.Settings, contentDescription = "Zonificar", tint = SoftAzure)
                            }
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Última vez", style = MaterialTheme.typography.labelSmall, color = SoftGray)
                            Text(sat.ultimaConexion?.substringAfter(" ") ?: "--:--", style = MaterialTheme.typography.labelSmall, color = TextLight, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Cuadro detallado de solicitudes activas
                if (activeTasks.isNotEmpty()) {
                    HorizontalDivider(color = LightAzure.copy(0.3f), modifier = Modifier.padding(horizontal = 16.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SoftAzure.copy(alpha = 0.03f))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "TAREAS EN CURSO (${activeTasks.size})", 
                            style = MaterialTheme.typography.labelSmall, 
                            fontWeight = FontWeight.Black, 
                            color = SoftAzure,
                            letterSpacing = 1.sp
                        )
                        activeTasks.forEach { task ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, LightAzure.copy(0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(SoftAzure.copy(0.1f), RoundedCornerShape(6.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Assignment, null, tint = SoftAzure, modifier = Modifier.size(16.dp))
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text("SOT: ${task.sot ?: "---"}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextDark)
                                        Text("${task.tecnico ?: "---"}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextDark.copy(0.8f))
                                        Text("${task.motivo ?: "---"} | ${task.material ?: ""}", fontSize = 10.sp, color = TextLight)
                                    }
                                    Surface(
                                        color = when(task.estado?.lowercase()) {
                                            "camino", "en_camino" -> WarningOrange.copy(0.1f)
                                            "llegado" -> SuccessGreen.copy(0.1f)
                                            else -> SoftAzure.copy(0.1f)
                                        },
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = task.estado?.uppercase() ?: "---",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = when(task.estado?.lowercase()) {
                                                "camino", "en_camino" -> WarningOrange
                                                "llegado" -> SuccessGreen
                                                else -> SoftAzure
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else if (sat.estado == "Ocupado") {
                     // Si dice ocupado pero no hay tareas en la lista filtrada (por seguridad)
                     Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                         Text("Sincronizando tareas...", style = MaterialTheme.typography.labelSmall, color = SoftGray)
                     }
                }
            }
        }
    }
}

@Composable
fun SummaryStatsBar(solicitudes: List<com.example.appmovil.data.remote.SolicitudItem>, selectedFilter: String, onFilterSelect: (String) -> Unit) {
    val stats = listOf(
        Triple("TODAS", solicitudes.size, SoftAzure),
        Triple("PENDIENTES", solicitudes.count { it.estado?.lowercase() == "pendiente" }, SoftGray),
        Triple("CORREGIR", solicitudes.count { it.estado?.lowercase() == "corregir" }, WarningOrange),
        Triple("EN CURSO", solicitudes.count { it.estado?.lowercase() in listOf("aceptada", "camino", "en_camino", "llegado") }, SuccessGreen),
        Triple("LISTAS", solicitudes.count { it.estado?.lowercase() == "completada" }, DeepAzure),
        Triple("ANULADAS", solicitudes.count { it.estado?.lowercase() == "anulada" }, Color.DarkGray)
    )
    LazyRow(Modifier.fillMaxWidth().background(Color.White).padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(horizontal = 16.dp)) {
        items(stats) { (label, count, color) ->
            val isSelected = selectedFilter == label
            Surface(
                modifier = Modifier.width(115.dp).clickable { onFilterSelect(label) },
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) color.copy(alpha = 0.05f) else Color.White,
                border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) color else LightAzure)
            ) {
                Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(count.toString(), style = MaterialTheme.typography.titleLarge, color = if(isSelected) color else TextDark, fontWeight = FontWeight.ExtraBold)
                    Text(label, style = MaterialTheme.typography.labelSmall, color = TextLight, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
    HorizontalDivider(color = LightAzure, thickness = 1.dp)
}

@Composable
fun ChatScreen(viewModel: com.example.appmovil.ui.chat.ChatViewModel, miId: Int, miRol: String) {
    val contacts by viewModel.contacts.collectAsState()
    val selectedContact by viewModel.selectedContact.collectAsState()
    val messages by viewModel.messages.collectAsState()

    if (selectedContact == null) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text("Equipo Ci-Gestión", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = SoftAzure)
            Spacer(Modifier.height(16.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(contacts) { ContactItemCard(it) { viewModel.selectContact(it, miId) } }
            }
        }
    } else {
        ConversationView(contact = selectedContact!!, messages = messages, onBack = { viewModel.clearSelection() }, onSend = { viewModel.sendMessage(miId, selectedContact!!.id, it) })
    }
}

@Composable
fun ContactItemCard(contact: com.example.appmovil.data.remote.UserContact, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }, 
        shape = RoundedCornerShape(12.dp), 
        color = Color.White, 
        border = BorderStroke(1.dp, LightAzure),
        shadowElevation = 1.dp
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(SoftAzure.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) { 
                Text(contact.nombre.take(1).uppercase(), color = SoftAzure, fontWeight = FontWeight.Bold) 
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(contact.nombre, fontWeight = FontWeight.Bold, color = TextDark)
                Text(contact.rol.uppercase(), style = MaterialTheme.typography.labelSmall, color = TextLight, fontWeight = FontWeight.SemiBold)
            }
            if (contact.unreadCount > 0) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(ErrorRed),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = contact.unreadCount.toString(),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@Composable
fun ConversationView(contact: com.example.appmovil.data.remote.UserContact, messages: List<com.example.appmovil.data.remote.MessageItem>, onBack: () -> Unit, onSend: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    LaunchedEffect(messages.size) { if(messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1) }

    Column(Modifier.fillMaxSize().background(SoftWhite)) {
        Surface(shadowElevation = 4.dp, color = Color.White) {
            Row(Modifier.padding(8.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = SoftAzure) }
                Text(contact.nombre, fontWeight = FontWeight.Bold, color = TextDark)
            }
        }
        LazyColumn(state = listState, modifier = Modifier.weight(1f).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 16.dp)) {
            items(messages) { msg ->
                val isMio = msg.esMio
                Box(Modifier.fillMaxWidth(), contentAlignment = if(isMio) Alignment.CenterEnd else Alignment.CenterStart) {
                    Surface(color = if(isMio) SoftAzure else Color(0xFF30363D), shape = RoundedCornerShape(12.dp)) {
                        Text(msg.mensaje, modifier = Modifier.padding(12.dp), color = if(isMio) Color.Black else TextDark)
                    }
                }
            }
        }
        Surface(color = DeepAzure, modifier = Modifier.imePadding()) {
            Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                TextField(value = text, onValueChange = { text = it }, modifier = Modifier.weight(1f), placeholder = { Text("Escribe...", color = TextLight) }, shape = RoundedCornerShape(24.dp), colors = TextFieldDefaults.colors(focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, focusedContainerColor = Color(0xFF161B22), unfocusedContainerColor = Color(0xFF161B22), focusedTextColor = TextDark, unfocusedTextColor = TextDark))
                IconButton(onClick = { if(text.isNotBlank()){ onSend(text); text = "" } }) { Icon(Icons.Default.Send, null, tint = SoftAzure) }
            }
        }
    }
}

@Composable
fun EmptyStateView(sec: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(if(sec=="inicio") "Sin actividades pendientes" else "Historial vacío", color = TextLight) }
}
@Composable
fun AssignSatelliteDialog(
    solicitudId: Int,
    sot: String,
    satellites: List<com.example.appmovil.data.remote.SatelliteStatusItem>,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = Color(0xFF161B22), modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(Modifier.padding(24.dp)) {
                Text("Asignar Orden #$sot", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = SoftAzure)
                Text("Selecciona el satélite para asignar esta tarea.", style = MaterialTheme.typography.bodyMedium, color = TextLight)
                Spacer(Modifier.height(16.dp))
                
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(satellites) { sat ->
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable { onConfirm(sat.id) },
                            shape = RoundedCornerShape(12.dp),
                            color = SoftWhite,
                            border = BorderStroke(1.dp, LightAzure.copy(0.3f))
                        ) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(40.dp).background(SoftAzure.copy(0.1f), CircleShape), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Person, null, tint = SoftAzure)
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(sat.nombre, fontWeight = FontWeight.Bold)
                                    val statusText = sat.estado ?: "Sin estado"
                                    Text(statusText, style = MaterialTheme.typography.labelSmall, color = if(statusText == "Disponible") SuccessGreen else WarningOrange)
                                }
                            }
                        }
                    }
                }
                
                Spacer(Modifier.height(24.dp))
                OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Text("CANCELAR") }
            }
        }
    }
}

@Composable
fun ErrorView(msg: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Error: $msg", color = ErrorRed) }
}
@Composable
fun ZonificacionDialog(
    satelliteName: String,
    currentZone: String,
    availableZones: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val zones = if (availableZones.isNotEmpty()) availableZones else listOf("SURCO", "SAN JUAN DE MIRAFLOREZ", "VILLA EL SALVADOR")
    var selectedZone by remember { mutableStateOf(currentZone) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = Color(0xFF161B22), modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.LocationOn, null, tint = SoftAzure, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(16.dp))
                Text("Zonificar Personal", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = SoftAzure)
                Text(satelliteName, style = MaterialTheme.typography.bodyMedium, color = TextLight)
                Spacer(Modifier.height(24.dp))
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    zones.forEach { zone ->
                        val isSelected = selectedZone == zone
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable { selectedZone = zone },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) SoftAzure.copy(0.1f) else Color.Transparent,
                            border = BorderStroke(1.dp, if (isSelected) SoftAzure else SoftGray.copy(0.3f))
                        ) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = isSelected, onClick = { selectedZone = zone }, colors = RadioButtonDefaults.colors(selectedColor = SoftAzure))
                                Spacer(Modifier.width(8.dp))
                                Text(zone, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }
                
                Spacer(Modifier.height(32.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("CANCELAR") }
                    Button(onClick = { onConfirm(selectedZone) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = SoftAzure)) { Text("GUARDAR") }
                }
            }
        }
    }
}

@Composable
fun RouteNotStartedView(onStartRoute: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(120.dp),
            shape = CircleShape,
            color = SoftAzure.copy(alpha = 0.1f)
        ) {
            Icon(
                imageVector = Icons.Default.Route,
                contentDescription = null,
                modifier = Modifier.padding(24.dp).size(64.dp),
                tint = SoftAzure
            )
        }
        
        Spacer(Modifier.height(32.dp))
        
        Text(
            "Ruta No Iniciada",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = DeepAzure,
            textAlign = TextAlign.Center
        )
        
        Spacer(Modifier.height(12.dp))
        
        Text(
            "Debes marcar el inicio de tu ruta para poder visualizar y recibir nuevas solicitudes de atención.",
            style = MaterialTheme.typography.bodyLarge,
            color = TextLight,
            textAlign = TextAlign.Center
        )
        
        Spacer(Modifier.height(48.dp))
        
        Button(
            onClick = onStartRoute,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SoftAzure),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Icon(Icons.Default.PlayArrow, null)
            Spacer(Modifier.width(8.dp))
            Text("INICIAR RUTA DE HOY", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun AuxiliarReportCard(
    solicitud: com.example.appmovil.data.remote.SolicitudItem,
    onAccept: () -> Unit = {},
    onReject: (String) -> Unit = {},
    onComplete: () -> Unit = {},
    onTakePhoto: (Int) -> Unit = {},
    onPickPhoto: (Int) -> Unit = {},
    onDeletePhoto: (Int, String) -> Unit = { _, _ -> },
    onRequestValidation: (Int) -> Unit = {}, // callback para abrir el diálogo de selección de supervisor
    isAuxiliarView: Boolean = false  // true cuando el auxiliar ve SUS propias plantillas
) {
    var showRejectDialog by remember { mutableStateOf(false) }
    var rejectComment by remember { mutableStateOf("") }
    var selectedImageUrl by remember { mutableStateOf<String?>(null) }

    val estado = solicitud.estado?.lowercase() ?: "pendiente"
    val estadoColor = when (estado) {
        "pendiente" -> Color(0xFFFF9800)
        "aceptada" -> SuccessGreen
        "por_validar" -> Color(0xFF9C27B0) // Púrpura para validación
        "rechazada" -> ErrorRed
        "completada" -> SoftAzure
        else -> TextLight
    }

    // Fotos de evidencia
    val rawPhotoPaths = remember(solicitud.fotoEvidencia) {
        solicitud.fotoEvidencia
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() && it != "null" }
            ?: emptyList()
    }
    val photoUrls = remember(rawPhotoPaths) {
        rawPhotoPaths.mapNotNull { com.example.appmovil.ui.home.getImageUrl(it) }
    }
    val hasPhoto = photoUrls.isNotEmpty()

    // Diálogo de zoom de imagen
    if (selectedImageUrl != null) {
        com.example.appmovil.ui.home.ImageZoomDialog(
            imageUrl = selectedImageUrl!!,
            onDismiss = { selectedImageUrl = null }
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFFF9800).copy(alpha = 0.3f)),
        shadowElevation = 2.dp
    ) {
        Column(Modifier.padding(16.dp)) {
            // Header con badge de estado
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFFF9800).copy(alpha = 0.1f)
                ) {
                    Text(
                        "PLANTILLA AUXILIAR",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFF9800)
                    )
                }
                Spacer(Modifier.weight(1f))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = estadoColor.copy(alpha = 0.1f)
                ) {
                    Text(
                        (solicitud.estado ?: "---").uppercase(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = estadoColor
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Auxiliar (Creador)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Person, null, tint = Color(0xFFFF9800), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Auxiliar: ", style = MaterialTheme.typography.labelSmall, color = TextLight)
                val auxName = solicitud.nombreSatelite?.takeIf { it.isNotBlank() } ?: "---"
                Text(auxName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextDark)
            }

            Spacer(Modifier.height(6.dp))

            // Supervisor asignado para validar
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.VerifiedUser, null, tint = Color(0xFF9C27B0), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Valida: ", style = MaterialTheme.typography.labelSmall, color = TextLight)
                Text(solicitud.nombreSupervisor ?: "---", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF9C27B0))
            }

            Spacer(Modifier.height(6.dp))

            // SOT
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Tag, null, tint = SoftAzure, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("SOT: ", style = MaterialTheme.typography.labelSmall, color = TextLight)
                Text(solicitud.sot ?: "---", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DeepAzure)
            }

            Spacer(Modifier.height(6.dp))

            // Técnico que ayuda
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Engineering, null, tint = TextLight, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Técnico asistido: ", style = MaterialTheme.typography.labelSmall, color = TextLight)
                Text(solicitud.tecnico ?: "---", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextDark)
            }

            Spacer(Modifier.height(6.dp))

            // Motivo
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Outlined.Description, null, tint = TextLight, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Motivo: ", style = MaterialTheme.typography.labelSmall, color = TextLight)
                Text(solicitud.motivo ?: "---", fontSize = 12.sp, color = TextDark)
            }

            if (!solicitud.zona.isNullOrEmpty()) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.LocationOn, null, tint = TextLight, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Zona: ", style = MaterialTheme.typography.labelSmall, color = TextLight)
                    Text(solicitud.zona, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = DeepAzure)
                }
            }

            // Fecha
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Schedule, null, tint = TextLight, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(solicitud.fechaCreacion ?: "---", fontSize = 10.sp, color = TextLight)
            }

            // ====== GALERÍA DE FOTOS DE EVIDENCIA ======
            if (hasPhoto) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFFFF9800).copy(alpha = 0.15f))
                Spacer(Modifier.height(8.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PhotoLibrary, null, tint = Color(0xFFFF9800), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (photoUrls.size > 1) "${photoUrls.size} fotos de evidencia" else "1 foto de evidencia",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF9800)
                    )
                }
                Spacer(Modifier.height(8.dp))
                
                if (photoUrls.size == 1) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        coil.compose.AsyncImage(
                            model = photoUrls[0],
                            contentDescription = "Evidencia",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(LightAzure)
                                .clickable { selectedImageUrl = photoUrls[0] },
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                        if (isAuxiliarView && (estado == "aceptada" || estado == "pendiente") && rawPhotoPaths.isNotEmpty()) {
                            IconButton(
                                onClick = { onDeletePhoto(solicitud.id, rawPhotoPaths[0]) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(28.dp)
                                    .background(ErrorRed.copy(alpha = 0.85f), CircleShape)
                            ) {
                                Icon(Icons.Default.Close, "Eliminar", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        photoUrls.forEachIndexed { index, url ->
                            Box {
                                coil.compose.AsyncImage(
                                    model = url,
                                    contentDescription = "Evidencia",
                                    modifier = Modifier
                                        .size(140.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(LightAzure)
                                        .clickable { selectedImageUrl = url },
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                                if (isAuxiliarView && (estado == "aceptada" || estado == "pendiente") && index < rawPhotoPaths.size) {
                                    IconButton(
                                        onClick = { onDeletePhoto(solicitud.id, rawPhotoPaths[index]) },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(2.dp)
                                            .size(24.dp)
                                            .background(ErrorRed.copy(alpha = 0.85f), CircleShape)
                                    ) {
                                        Icon(Icons.Default.Close, "Eliminar", tint = Color.White, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ====== BOTONES DE FOTO (AUXILIAR - cuando está aceptada o pendiente) ======
            if (isAuxiliarView && (estado == "aceptada" || estado == "pendiente")) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = SuccessGreen.copy(alpha = 0.2f))
                Spacer(Modifier.height(8.dp))
                
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = SuccessGreen.copy(alpha = 0.06f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.2f))
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CameraAlt, null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("SUBIR EVIDENCIA DE APOYO", fontWeight = FontWeight.Black, fontSize = 10.sp, color = SuccessGreen)
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onTakePhoto(solicitud.id) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                                contentPadding = PaddingValues(vertical = 10.dp)
                            ) {
                                Icon(Icons.Default.PhotoCamera, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("CÁMARA", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Button(
                                onClick = { onPickPhoto(solicitud.id) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SoftAzure),
                                contentPadding = PaddingValues(vertical = 10.dp)
                            ) {
                                Icon(Icons.Default.Collections, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("GALERÍA", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
                
                // Botón para finalizar apoyo (solo auxiliar y cuando hay fotos)
                if (isAuxiliarView && hasPhoto && (estado == "aceptada" || estado == "pendiente")) {
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { onRequestValidation(solicitud.id) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C27B0)),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Icon(Icons.Default.Send, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("FINALIZAR APOYO", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                }
            }
            
            // ====== BOTONES DE ACCIÓN (SUPERVISOR) ======
            val showSupervisorActions = (!isAuxiliarView && estado in listOf("pendiente", "aceptada", "por_validar"))
            if (showSupervisorActions) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFFFF9800).copy(alpha = 0.15f))
                Spacer(Modifier.height(12.dp))

                if (estado == "pendiente") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { showRejectDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.5f)),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Close, null, tint = ErrorRed, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("RECHAZAR", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ErrorRed)
                        }
                        Button(
                            onClick = onAccept,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("ACEPTAR", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                } else if (estado == "aceptada" || estado == "por_validar") {
                    if (estado == "aceptada") {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFFFF9800).copy(alpha = 0.06f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFFFF9800).copy(alpha = 0.2f))
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.HourglassBottom, null, tint = Color(0xFFFF9800), modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text("EN CURSO / ESPERANDO FINALIZACIÓN", fontWeight = FontWeight.Black, fontSize = 10.sp, color = Color(0xFFFF9800))
                                    Text(if(!hasPhoto) "El auxiliar aún no sube fotos" else "El auxiliar está subiendo evidencias...", style = MaterialTheme.typography.bodySmall, color = TextLight, fontSize = 10.sp)
                                }
                            }
                        }
                    } else if (estado == "por_validar") {
                        // Solo cuando ya el auxiliar dio por finalizado el apoyo
                        Column {
                            Surface(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                color = Color(0xFF9C27B0).copy(alpha = 0.06f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFF9C27B0).copy(alpha = 0.2f))
                            ) {
                                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Info, null, tint = Color(0xFF9C27B0), modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("EL AUXILIAR FINALIZÓ EL APOYO - VALIDAR AHORA", fontWeight = FontWeight.Black, fontSize = 10.sp, color = Color(0xFF9C27B0))
                                }
                            }
                            
                            Button(
                                onClick = onComplete,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                contentPadding = PaddingValues(vertical = 12.dp)
                            ) {
                                Icon(Icons.Default.Verified, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("VERIFICADO - COMPLETAR", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White)
                            }
                        }
                    }
                }
            }

            // Completada: Indicador visual
            if (estado == "completada") {
                Spacer(Modifier.height(12.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = SuccessGreen.copy(alpha = 0.06f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.2f))
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Verified, null, tint = SuccessGreen, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("TRABAJO VERIFICADO POR SUPERVISOR", fontWeight = FontWeight.Black, fontSize = 10.sp, color = SuccessGreen)
                    }
                }
            }
        }
    }

    // Diálogo de rechazo
    if (showRejectDialog) {
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            icon = { Icon(Icons.Default.Warning, null, tint = ErrorRed) },
            title = { Text("Rechazar Plantilla") },
            text = {
                Column {

                    Text("¿Por qué rechazas esta plantilla del auxiliar?")
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = rejectComment,
                        onValueChange = { rejectComment = it },
                        label = { Text("Comentario (opcional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onReject(rejectComment)
                        showRejectDialog = false
                        rejectComment = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) { Text("RECHAZAR", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showRejectDialog = false }) { Text("CANCELAR") }
            },
            containerColor = Color.White,
            tonalElevation = 8.dp
        )
    }
}

@Composable
fun SupervisorProductivityScreen(
    stats: List<com.example.appmovil.data.remote.SupervisorProductivityItem>,
    onRefresh: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            color = SoftAzure.copy(alpha = 0.08f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, SoftAzure.copy(alpha = 0.3f))
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.TrendingUp, null, tint = SoftAzure)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("PRODUCTIVIDAD SUPERVISORES", fontWeight = FontWeight.Black, fontSize = 12.sp, color = SoftAzure)
                    Text("Supervisiones realizadas hoy", style = MaterialTheme.typography.bodySmall, color = TextLight)
                }
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Outlined.Refresh, null, tint = SoftAzure)
                }
            }
        }

        if (stats.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hay datos de supervisión hoy", color = TextLight)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(stats) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier.size(40.dp).clip(CircleShape).background(SoftAzure.copy(0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(item.supervisorNombre.take(1).uppercase(), color = SoftAzure, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(item.supervisorNombre, fontWeight = FontWeight.Bold, color = TextDark)
                                    Text("Supervisor ID: ${item.supervisorId}", style = MaterialTheme.typography.labelSmall, color = TextLight)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(item.totalSupervisiones.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = SoftAzure)
                                    Text("SUPERVISIONES", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = SoftAzure)
                                }
                            }
                            
                            if (!item.tecnicosSupervisados.isNullOrEmpty()) {
                                Spacer(Modifier.height(12.dp))
                                HorizontalDivider(color = LightAzure.copy(0.3f))
                                Spacer(Modifier.height(8.dp))
                                Text("TÉCNICOS SUPERVISADOS HOY:", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = TextLight)
                                Text(
                                    item.tecnicosSupervisados!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DeepAzure,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DailySupervisionRecord(count: Int) {
    val meta = 4
    val progress = (count.toFloat() / meta.toFloat()).coerceAtMost(1.0f)
    val isMetaReached = count >= meta
    val statusColor = when {
        count >= meta -> SuccessGreen
        count >= 2 -> WarningOrange
        else -> ErrorRed
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.1f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "RECORD DE SUPERVISIÓN",
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        color = statusColor,
                        letterSpacing = 1.sp
                    )
                    Text(
                        when {
                            count >= meta -> "¡META DIARIA CUMPLIDA!"
                            count >= 2 -> "BUEN RITMO, SIGUE ASÍ"
                            else -> "PENDIENTE DE COMPLETAR"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isMetaReached) SuccessGreen else if (count >= 2) WarningOrange else TextLight,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(statusColor.copy(0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isMetaReached) {
                        Icon(Icons.Default.EmojiEvents, null, tint = SuccessGreen, modifier = Modifier.size(24.dp))
                    } else {
                        Text(
                            "$count/$meta",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = ErrorRed
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Barra de progreso
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape),
                color = statusColor,
                trackColor = statusColor.copy(0.1f)
            )

            Spacer(Modifier.height(8.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "Minimo requerido: 4 en campo",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextLight,
                    fontWeight = FontWeight.Medium
                )
                if (isMetaReached) {
                    Text(
                        "EXCELENTE TRABAJO",
                        style = MaterialTheme.typography.labelSmall,
                        color = SuccessGreen,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}



