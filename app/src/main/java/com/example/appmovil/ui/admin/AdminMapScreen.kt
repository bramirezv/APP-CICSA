package com.example.appmovil.ui.admin

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.appmovil.data.remote.RetrofitClient
import com.example.appmovil.data.remote.UserLocationItem
import com.example.appmovil.ui.theme.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.maps.android.compose.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Genera un marcador circular tipo Canvas con la inicial del usuario y un anillo de color
 * correspondiente a su estado actual (Disponible, Almorzando, En Servicio, Offline).
 */
fun createCustomMarkerBitmap(
    name: String,
    status: String
): BitmapDescriptor {
    val size = 110 // tamaño en pixeles
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Círculo de fondo blanco
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = android.graphics.Color.WHITE
    }

    // Anillo exterior según estado
    val ringColor = when (status.lowercase()) {
        "disponible" -> android.graphics.Color.parseColor("#10B981") // Verde Esmeralda
        "almorzando" -> android.graphics.Color.parseColor("#F59E0B") // Ámbar
        "no disponible", "fuera de línea" -> android.graphics.Color.parseColor("#6B7280") // Gris Pizarra
        else -> android.graphics.Color.parseColor("#1E40AF") // Azul Real (Ocupado/En Servicio)
    }

    val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        color = ringColor
    }

    // Estilo para la Inicial del nombre
    val initialPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#1E293B") // Gris Oscuro Slate
        textSize = 36f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    val radius = size / 2.8f
    val centerX = size / 2f
    val centerY = size / 2.8f + 4f

    // Dibujar base del marcador circular
    canvas.drawCircle(centerX, centerY, radius, bgPaint)
    canvas.drawCircle(centerX, centerY, radius, ringPaint)

    // Obtener y dibujar inicial
    val initial = name.firstOrNull()?.toString()?.uppercase() ?: "?"
    val textBounds = Rect()
    initialPaint.getTextBounds(initial, 0, initial.length, textBounds)
    val textHeight = textBounds.height()
    canvas.drawText(initial, centerX, centerY + textHeight / 2f, initialPaint)

    // Puntero triangular en la parte inferior del círculo
    val pointerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ringColor
    }
    val path = android.graphics.Path().apply {
        moveTo(centerX - 12f, centerY + radius - 1f)
        lineTo(centerX + 12f, centerY + radius - 1f)
        lineTo(centerX, centerY + radius + 12f)
        close()
    }
    canvas.drawPath(path, pointerPaint)

    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMapScreen(userRol: String = "admin", onBack: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    
    var userLocations by remember { mutableStateOf<List<UserLocationItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var hasInitiallyCentered by remember { mutableStateOf(false) }
    var isUserListExpanded by remember { mutableStateOf(false) }
    
    // Estados de filtros
    var selectedRoleFilter by remember { mutableStateOf("Todos") }
    var selectedStatusFilter by remember { mutableStateOf("Todos") }
    var selectedZoneFilter by remember { mutableStateOf("Todos") }
    
    // Usuario seleccionado para Telemetría
    var selectedUser by remember { mutableStateOf<UserLocationItem?>(null) }
    
    // Temporizador de actualización
    var secondsRemaining by remember { mutableStateOf(10) }

    val cleanRol = userRol.lowercase()
    val isSupervisor = cleanRol.contains("supervisor")

    // Cámara inicial en Lima por defecto
    val defaultPos = LatLng(-12.046374, -77.042793) 
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultPos, 12f)
    }

    // Carga de ubicaciones desde el API
    suspend fun loadLocations() {
        try {
            val response = RetrofitClient.authApi.getUsersLocations()
            if (response.isSuccessful) {
                val allLocations = response.body() ?: emptyList()
                userLocations = if (isSupervisor) {
                    allLocations.filter { 
                        val r = it.rol.lowercase()
                        r == "satelite" || r == "auxiliar"
                    }
                } else {
                    allLocations
                }
                errorMessage = null
            } else {
                errorMessage = "Error del servidor: ${response.code()}"
            }
        } catch (e: Exception) {
            errorMessage = "Error de conexión: ${e.localizedMessage}"
        } finally {
            isLoading = false
        }
    }

    // Polling centralizado con temporizador visual
    LaunchedEffect(Unit) {
        while (true) {
            loadLocations()
            for (i in 10 downTo 1) {
                secondsRemaining = i
                delay(1000)
            }
        }
    }

    // Centrado automático al cargar datos inicialmente
    LaunchedEffect(userLocations) {
        if (userLocations.isNotEmpty() && !hasInitiallyCentered) {
            val firstUser = userLocations.first()
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(firstUser.lat, firstUser.lng),
                    14f
                )
            )
            hasInitiallyCentered = true
        }
    }

    // Lista de zonas detectadas dinámicamente
    val zones = remember(userLocations) {
        listOf("Todos") + userLocations.mapNotNull { it.zona }.distinct().sorted()
    }

    // Filtrar la lista de ubicaciones a mostrar en el mapa
    val filteredLocations = remember(userLocations, selectedRoleFilter, selectedStatusFilter, selectedZoneFilter) {
        userLocations.filter { user ->
            val matchesRole = when (selectedRoleFilter) {
                "Todos" -> true
                "Satélites" -> user.rol.lowercase() == "satelite"
                "Supervisores" -> user.rol.lowercase() == "supervisor"
                "Auxiliares" -> user.rol.lowercase() == "auxiliar"
                else -> true
            }
            val matchesStatus = when (selectedStatusFilter) {
                "Todos" -> true
                "Disponible" -> user.estado?.lowercase() == "disponible"
                "En Servicio" -> user.estado?.lowercase() == "ocupado" || user.estado?.lowercase() == "en servicio" || user.rutaActiva == 1
                "Almorzando" -> user.estado?.lowercase() == "almorzando"
                "No Disponible" -> user.estado?.lowercase() == "no disponible" || user.estado?.lowercase() == "fuera de línea"
                else -> true
            }
            val matchesZone = when (selectedZoneFilter) {
                "Todos" -> true
                else -> user.zona == selectedZoneFilter
            }
            matchesRole && matchesStatus && matchesZone
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Monitoreo de Personal", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Volver", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        isLoading = true
                        hasInitiallyCentered = false
                        coroutineScope.launch {
                            loadLocations()
                            secondsRemaining = 10
                        }
                    }) {
                        Icon(Icons.Default.Refresh, "Refrescar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepAzure)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            
            // MAPA DE GOOGLE
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(myLocationButtonEnabled = true),
                properties = MapProperties(isMyLocationEnabled = true)
            ) {
                filteredLocations.forEach { user ->
                    key(user.id) {
                        val pos = LatLng(user.lat, user.lng)
                        
                        // Icono personalizado con Canvas dinámico aislado por usuario
                        val markerIcon = remember(user.nombre, user.estado) {
                            createCustomMarkerBitmap(user.nombre, user.estado ?: "Disponible")
                        }

                        val markerState = rememberMarkerState(position = pos)

                        // Actualizar la posición de forma dinámica en tiempo real sin recrear el estado ni parpadear
                        LaunchedEffect(pos) {
                            markerState.position = pos
                        }

                        Marker(
                            state = markerState,
                            title = "${user.nombre} (${user.rol.uppercase()})",
                            snippet = "Zona: ${user.zona ?: "---"} - Estado: ${user.estado ?: "Disponible"}",
                            icon = markerIcon,
                            onClick = {
                                selectedUser = user
                                true // Previene el comportamiento por defecto de centrar automáticamente y popup básico
                            }
                        )
                    }
                }
            }

            // BARRA SUPERIOR DE FILTROS FLOTANTES
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.93f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Filtros de Búsqueda", 
                            fontWeight = FontWeight.Bold, 
                            fontSize = 12.sp, 
                            color = TextDark
                        )
                        if (selectedRoleFilter != "Todos" || selectedStatusFilter != "Todos" || selectedZoneFilter != "Todos") {
                            Text(
                                "Limpiar filtros",
                                modifier = Modifier.clickable {
                                    selectedRoleFilter = "Todos"
                                    selectedStatusFilter = "Todos"
                                    selectedZoneFilter = "Todos"
                                },
                                color = SoftAzure,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Filtro de Rol
                        var showRoleMenu by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { showRoleMenu = true },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().height(34.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (selectedRoleFilter != "Todos") SoftAzure.copy(alpha = 0.1f) else Color.Transparent
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp, 
                                    if (selectedRoleFilter != "Todos") SoftAzure else SoftGray.copy(alpha = 0.5f)
                                )
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = if (selectedRoleFilter == "Todos") "Rol: Todos" else selectedRoleFilter,
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (selectedRoleFilter != "Todos") DeepAzure else TextLight
                                    )
                                    Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(14.dp), tint = TextLight)
                                }
                            }
                            DropdownMenu(expanded = showRoleMenu, onDismissRequest = { showRoleMenu = false },
                                modifier = Modifier.background(Color.White)) {
                                listOf("Todos", "Satélites", "Supervisores", "Auxiliares").forEach { role ->
                                    DropdownMenuItem(
                                        text = { Text(role, fontSize = 12.sp, color = TextDark) },
                                        onClick = {
                                            selectedRoleFilter = role
                                            showRoleMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        // Filtro de Estado
                        var showStatusMenu by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { showStatusMenu = true },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().height(34.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (selectedStatusFilter != "Todos") SoftAzure.copy(alpha = 0.1f) else Color.Transparent
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp, 
                                    if (selectedStatusFilter != "Todos") SoftAzure else SoftGray.copy(alpha = 0.5f)
                                )
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = if (selectedStatusFilter == "Todos") "Estado: Todos" else selectedStatusFilter,
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (selectedStatusFilter != "Todos") DeepAzure else TextLight
                                    )
                                    Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(14.dp), tint = TextLight)
                                }
                            }
                            DropdownMenu(expanded = showStatusMenu, onDismissRequest = { showStatusMenu = false },
                                modifier = Modifier.background(Color.White)) {
                                listOf("Todos", "Disponible", "En Servicio", "Almorzando", "No Disponible").forEach { status ->
                                    DropdownMenuItem(
                                        text = { Text(status, fontSize = 12.sp, color = TextDark) },
                                        onClick = {
                                            selectedStatusFilter = status
                                            showStatusMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        // Filtro de Zona
                        var showZoneMenu by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { showZoneMenu = true },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().height(34.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (selectedZoneFilter != "Todos") SoftAzure.copy(alpha = 0.1f) else Color.Transparent
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp, 
                                    if (selectedZoneFilter != "Todos") SoftAzure else SoftGray.copy(alpha = 0.5f)
                                )
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = if (selectedZoneFilter == "Todos") "Zona: Todas" else selectedZoneFilter,
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (selectedZoneFilter != "Todos") DeepAzure else TextLight
                                    )
                                    Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(14.dp), tint = TextLight)
                                }
                            }
                            DropdownMenu(expanded = showZoneMenu, onDismissRequest = { showZoneMenu = false },
                                modifier = Modifier.background(Color.White)) {
                                zones.forEach { zone ->
                                    DropdownMenuItem(
                                        text = { Text(if (zone == "Todos") "Todas" else zone, fontSize = 12.sp, color = TextDark) },
                                        onClick = {
                                            selectedZoneFilter = zone
                                            showZoneMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // WIDGET TEMPORIZADOR DE ACTUALIZACIÓN (ARRIBA A LA DERECHA)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 95.dp, end = 16.dp)
            ) {
                Surface(
                    color = Color.White.copy(alpha = 0.88f),
                    shape = RoundedCornerShape(20.dp),
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(20.dp)) {
                            CircularProgressIndicator(
                                progress = secondsRemaining / 10f,
                                color = SoftAzure,
                                strokeWidth = 2.dp,
                                trackColor = SoftGray.copy(alpha = 0.2f),
                            )
                            Text(
                                text = secondsRemaining.toString(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        Text("Live", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFF10B981))
                    }
                }
            }

            // BANNER DE ERROR DE RED
            if (errorMessage != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 95.dp).align(Alignment.TopCenter),
                    color = ErrorRed.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(8.dp),
                    shadowElevation = 4.dp
                ) {
                    Text(
                        text = errorMessage!!,
                        modifier = Modifier.padding(12.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // BOTÓN FLOTANTE Y LISTA DE PERSONAL ACTIVO (Flotante izquierda)
            AnimatedVisibility(
                visible = selectedUser == null,
                enter = fadeIn() + slideInHorizontally(),
                exit = fadeOut() + slideOutHorizontally(),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 95.dp, start = 16.dp)
            ) {
                Column {
                    SmallFloatingActionButton(
                        onClick = { isUserListExpanded = !isUserListExpanded },
                        containerColor = Color.White,
                        contentColor = DeepAzure,
                        shape = CircleShape
                    ) {
                        Icon(if(isUserListExpanded) Icons.Default.Close else Icons.Default.People, "Usuarios")
                    }

                    Spacer(Modifier.height(8.dp))

                    AnimatedVisibility(
                        visible = isUserListExpanded,
                        enter = slideInHorizontally() + fadeIn(),
                        exit = slideOutHorizontally() + fadeOut()
                    ) {
                        Surface(
                            modifier = Modifier
                                .width(220.dp)
                                .heightIn(max = 350.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White.copy(alpha = 0.95f),
                            shadowElevation = 8.dp,
                            border = androidx.compose.foundation.BorderStroke(1.dp, SoftAzure.copy(alpha = 0.1f))
                        ) {
                            Column(Modifier.padding(8.dp)) {
                                Text(
                                    "PERSONAL ACTIVO (${filteredLocations.size})",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = TextLight,
                                    modifier = Modifier.padding(8.dp)
                                )
                                
                                HorizontalDivider(color = SoftGray.copy(alpha = 0.3f))
                                
                                LazyColumn {
                                    items(
                                        items = filteredLocations.sortedBy { it.nombre },
                                        key = { it.id }
                                    ) { user ->
                                        ListItem(
                                            headlineContent = { Text(user.nombre.uppercase(), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = TextDark) },
                                            supportingContent = { Text(user.rol.uppercase(), fontSize = 9.sp, color = when(user.rol.lowercase()) { "supervisor" -> SoftAzure; "auxiliar" -> Color(0xFFFF9800); else -> TextLight }) },
                                            leadingContent = {
                                                val stateColor = when (user.estado?.lowercase()) {
                                                    "disponible" -> Color(0xFF10B981)
                                                    "almorzando" -> Color(0xFFF59E0B)
                                                    "no disponible", "fuera de línea" -> Color(0xFF6B7280)
                                                    else -> Color(0xFF1E40AF)
                                                }
                                                Box(
                                                    Modifier.size(8.dp).background(stateColor, CircleShape)
                                                )
                                            },
                                            modifier = Modifier.clickable {
                                                isUserListExpanded = false
                                                selectedUser = user
                                                coroutineScope.launch {
                                                    cameraPositionState.animate(
                                                        CameraUpdateFactory.newLatLngZoom(LatLng(user.lat, user.lng), 17f)
                                                    )
                                                }
                                            },
                                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                        )
                                        HorizontalDivider(color = SoftGray.copy(alpha = 0.1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // CARGA O MENSAJE VACÍO
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = SoftAzure
                )
            } else if (filteredLocations.isEmpty() && errorMessage == null) {
                Surface(
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                    color = Color.White.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 8.dp
                ) {
                    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.LocationOff, null, tint = SoftAzure, modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("Sin coincidencias", fontWeight = FontWeight.Bold, color = TextDark, fontSize = 14.sp)
                        Text("No se detectó personal con los filtros seleccionados.", style = MaterialTheme.typography.bodySmall, color = TextLight, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
            }
            
            // LEYENDA INFERIOR COMPACTA (Oculta al abrir la telemetría)
            AnimatedVisibility(
                visible = selectedUser == null && !isUserListExpanded,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Surface(
                    color = Color.White.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 4.dp
                ) {
                    Column(Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).background(Color(0xFF10B981), CircleShape))
                            Spacer(Modifier.width(6.dp))
                            Text("Disponible", style = MaterialTheme.typography.labelSmall, color = TextDark)
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).background(Color(0xFF1E40AF), CircleShape))
                            Spacer(Modifier.width(6.dp))
                            Text("En Servicio (Ocupado)", style = MaterialTheme.typography.labelSmall, color = TextDark)
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).background(Color(0xFFF59E0B), CircleShape))
                            Spacer(Modifier.width(6.dp))
                            Text("Almorzando", style = MaterialTheme.typography.labelSmall, color = TextDark)
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).background(Color(0xFF6B7280), CircleShape))
                            Spacer(Modifier.width(6.dp))
                            Text("Fuera de Línea", style = MaterialTheme.typography.labelSmall, color = TextDark)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Activos: ${filteredLocations.size} / Total: ${userLocations.size}", 
                            fontWeight = FontWeight.Bold, 
                            style = MaterialTheme.typography.labelSmall, 
                            color = DeepAzure
                        )
                    }
                }
            }

            // HOJA DE TELEMETRÍA DETALLADA (Bottom Drawer)
            AnimatedVisibility(
                visible = selectedUser != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                selectedUser?.let { user ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Fila Cabecera: Inicial + Datos + Botón de Cierre
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val stateRingColor = when (user.estado?.lowercase()) {
                                        "disponible" -> Color(0xFF10B981)
                                        "almorzando" -> Color(0xFFF59E0B)
                                        "no disponible", "fuera de línea" -> Color(0xFF6B7280)
                                        else -> Color(0xFF1E40AF)
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(Color.White, CircleShape)
                                            .border(3.dp, stateRingColor, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = user.nombre.firstOrNull()?.toString()?.uppercase() ?: "?",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 18.sp,
                                            color = TextDark
                                        )
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = user.nombre.uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = TextDark
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                color = when (user.rol.lowercase()) {
                                                    "supervisor" -> SoftAzure.copy(alpha = 0.15f)
                                                    "auxiliar" -> Color(0xFFFF9800).copy(alpha = 0.15f)
                                                    else -> DeepAzure.copy(alpha = 0.15f)
                                                },
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = user.rol.uppercase(),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 8.sp,
                                                    color = when (user.rol.lowercase()) {
                                                        "supervisor" -> SoftAzure
                                                        "auxiliar" -> Color(0xFFFF9800)
                                                        else -> DeepAzure
                                                    }
                                                )
                                            }
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                text = "Zona: ${user.zona ?: "---"}",
                                                fontSize = 11.sp,
                                                color = TextLight,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                                IconButton(onClick = { selectedUser = null }) {
                                    Icon(Icons.Default.Close, "Cerrar", tint = TextLight)
                                }
                            }

                            Spacer(Modifier.height(12.dp))
                            HorizontalDivider(color = SoftGray.copy(alpha = 0.2f))
                            Spacer(Modifier.height(12.dp))

                            // Grid de telemetría de personal
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("ESTADO ACTUAL", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = TextLight)
                                    Spacer(Modifier.height(3.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val dotColor = when (user.estado?.lowercase()) {
                                            "disponible" -> Color(0xFF10B981)
                                            "almorzando" -> Color(0xFFF59E0B)
                                            "no disponible", "fuera de línea" -> Color(0xFF6B7280)
                                            else -> Color(0xFF1E40AF)
                                        }
                                        Box(Modifier.size(8.dp).background(dotColor, CircleShape))
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = user.estado?.uppercase() ?: "DISPONIBLE",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextDark
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text("RUTA / PLAN", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = TextLight)
                                    Spacer(Modifier.height(3.dp))
                                    Text(
                                        text = if (user.rutaActiva == 1) "Activa (En Ruta)" else "Inactiva / Fin",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (user.rutaActiva == 1) Color(0xFF10B981) else TextLight
                                    )
                                }

                                Column(modifier = Modifier.weight(1.1f)) {
                                    Text("ÚLTIMO REPORTE", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = TextLight)
                                    Spacer(Modifier.height(3.dp))
                                    Text(
                                        text = user.ultimaConexion?.substringAfter(" ") ?: "Sin conexión",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextDark
                                    )
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            // Acciones: Llamar y Cómo Llegar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (!user.telefono.isNullOrEmpty()) {
                                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${user.telefono}"))
                                            context.startActivity(intent)
                                        } else {
                                            Toast.makeText(context, "El usuario no tiene teléfono registrado", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SoftAzure),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Call, null, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Llamar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        val intent = Intent(
                                            Intent.ACTION_VIEW, 
                                            Uri.parse("geo:${user.lat},${user.lng}?q=${user.lat},${user.lng}(${user.nombre})")
                                        )
                                        try {
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "No se encontró aplicación de mapas instalada", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = DeepAzure),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Navigation, null, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Cómo Llegar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
