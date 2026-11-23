package com.example.appmovil.ui.home

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.LinearEasing
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import com.example.appmovil.data.remote.SolicitudItem
import com.example.appmovil.data.remote.RetrofitClient
import com.example.appmovil.ui.theme.*
import java.time.LocalDateTime
import java.time.Duration
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Función auxiliar para obtener la URL completa de la imagen.
 * Si el path es relativo (ej: api/uploads/...), concatena la BASE_URL.
 */
fun getImageUrl(path: String?): String? {
    if (path.isNullOrBlank() || path == "null") return null
    
    // Corrección para el marcador de posición del servidor (tudominio.com)
    if (path.contains("tudominio.com")) {
        return path.replace("https://tudominio.com/", RetrofitClient.BASE_URL)
                   .replace("http://tudominio.com/", RetrofitClient.BASE_URL)
    }
    
    // Si ya es una URL completa y válida
    if (path.startsWith("http")) return path
    
    // Si es una ruta relativa, concatenar la BASE_URL asegurando que no haya doble barra
    val cleanPath = path.removePrefix("/")
    return "${RetrofitClient.BASE_URL}$cleanPath"
}

@Composable
fun SolicitudItemCard(
    solicitud: SolicitudItem,
    isSatelite: Boolean = false,
    isAdmin: Boolean = false,
    userZona: String? = null,
    onStatusUpdate: (Int, String, String?) -> Unit = { _, _, _ -> },
    onEdit: (SolicitudItem) -> Unit = {},
    onAssign: (SolicitudItem) -> Unit = {},
    onTakePhoto: (Int) -> Unit = {},
    onPickPhoto: (Int) -> Unit = {},
    onDeletePhoto: (Int, String) -> Unit = { _, _ -> },
    onTrackTicket: (Int, String, String) -> Unit = { _, _, _ -> }, // Nuevo parámetro de seguimiento
    isLocked: Boolean = false
) {
    if (isSatelite) {
        SateliteSolicitudCard(
            solicitud = solicitud,
            onStatusUpdate = onStatusUpdate,
            onTakePhoto = onTakePhoto,
            onPickPhoto = onPickPhoto,
            onDeletePhoto = onDeletePhoto,
            isLocked = isLocked,
            userRol = "satelite",
            userZona = userZona
        )
    } else {
        SupervisorSolicitudCard(
            solicitud = solicitud, 
            isAdmin = isAdmin,
            onEdit = onEdit,
            onAssign = onAssign,
            onStatusUpdate = onStatusUpdate,
            onTrackTicket = onTrackTicket // Pasar al card del supervisor
        )
    }
}

@Composable
fun SateliteSolicitudCard(
    solicitud: SolicitudItem,
    onStatusUpdate: (Int, String, String?) -> Unit,
    onTakePhoto: (Int) -> Unit = {},
    onPickPhoto: (Int) -> Unit = {},
    onDeletePhoto: (Int, String) -> Unit = { _, _ -> },
    userRol: String = "satelite",
    userZona: String? = null,
    isLocked: Boolean = false
) {
    val context = LocalContext.current
    val isSatelite = userRol.trim().lowercase() == "satelite"
    val estado = solicitud.estado?.lowercase() ?: "pendiente"
    var selectedImageUrl by remember { mutableStateOf<String?>(null) }
    var pendingActionStatus by remember { mutableStateOf<String?>(null) }
    
    if (pendingActionStatus != null) {
        val (title, description, color) = when(pendingActionStatus) {
            "corregir" -> Triple("Motivo de Corrección", "Explica por qué envías a corregir.", WarningOrange)
            "rechazada" -> Triple("Motivo de Rechazo", "Explica por qué rechazas este apoyo.", ErrorRed)
            else -> Triple("Comentario", "Añade una observación.", DeepAzure)
        }
        
        ActionCommentDialog(
            title = title,
            description = description,
            confirmColor = color,
            onDismiss = { pendingActionStatus = null },
            onConfirm = { comment ->
                onStatusUpdate(solicitud.id, pendingActionStatus!!, comment)
                pendingActionStatus = null
            }
        )
    }
    
    if (selectedImageUrl != null) {
        ImageZoomDialog(imageUrl = selectedImageUrl!!, onDismiss = { selectedImageUrl = null })
    }
    
    // Extraer lat,lng de cualquier formato: coordenadas puras, URL de Google Maps, short URL, etc.
    fun extractLatLng(input: String): String? {
        val trimmed = input.trim()
        // 1. Formato directo: -12.1234,-77.1234 o -12.1234, -77.1234
        val coordRegex = Regex("(-?\\d{1,3}\\.\\d+)[,\\s]+(-?\\d{1,3}\\.\\d+)")
        coordRegex.find(trimmed)?.let { return "${it.groupValues[1]},${it.groupValues[2]}" }
        // 2. URL de Google Maps con @lat,lng
        val atRegex = Regex("@(-?\\d{1,3}\\.\\d+),(-?\\d{1,3}\\.\\d+)")
        atRegex.find(trimmed)?.let { return "${it.groupValues[1]},${it.groupValues[2]}" }
        // 3. URL con query ?q=lat,lng o &q=lat,lng
        val qRegex = Regex("[?&]q=(-?\\d{1,3}\\.\\d+),(-?\\d{1,3}\\.\\d+)")
        qRegex.find(trimmed)?.let { return "${it.groupValues[1]},${it.groupValues[2]}" }
        // 4. URL con /place/ → abrir directamente
        return null
    }

    val openMap = { platform: String ->
        val rawCoords = solicitud.coordenadas ?: ""
        if (rawCoords.isNotBlank()) {
            val latLng = extractLatLng(rawCoords)
            val uri = if (latLng != null) {
                if (platform == "google") "geo:$latLng?q=$latLng"
                else "waze://?ll=$latLng&navigate=yes"
            } else {
                // Si es una URL de Google Maps completa sin coordenadas extraíbles, abrirla directo
                if (platform == "google") rawCoords else "waze://"
            }
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
            } catch (e: Exception) {
                // Fallback: abrir en navegador
                val webUrl = if (latLng != null)
                    "https://www.google.com/maps/search/?api=1&query=$latLng"
                else rawCoords
                try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(webUrl))) } catch (_: Exception) {}
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, if (isLocked) SoftGray.copy(alpha = 0.5f) else LightAzure)
    ) {
        var now by remember { mutableStateOf(LocalDateTime.now()) }
        LaunchedEffect(solicitud.id, estado) {
            if (estado == "pendiente") {
                while(true) {
                    kotlinx.coroutines.delay(10000) // 10s ticker
                    now = LocalDateTime.now()
                }
            }
        }

        Box(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                // Barra lateral de estado
                val accentColor = when(estado) {
                    "completada" -> SuccessGreen
                    "en_camino", "llegado" -> WarningOrange
                    "aceptada" -> SoftAzure
                    "rechazada" -> ErrorRed
                    else -> SoftGray
                }
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(accentColor)
                )

                Column(modifier = Modifier.padding(16.dp)) {
                // Header: Folio y Estado
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ORDEN #${solicitud.sot ?: "---"}",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isLocked) TextLight else DeepAzure
                        )
                    )
                    
                    if (isLocked) {
                        Surface(
                            color = SoftGray.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, SoftGray.copy(alpha = 0.2f))
                        ) {
                            Row(Modifier.padding(horizontal = 8.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lock, null, modifier = Modifier.size(10.dp), tint = TextLight)
                                Spacer(Modifier.width(4.dp))
                                Text("BLOQUEADA", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, color = TextLight, fontSize = 9.sp))
                            }
                        }
                    } else {
                        StatusBadge(estado)
                    }
                }

                // --- TIEMPOS ---
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Schedule, null, modifier = Modifier.size(12.dp), tint = TextLight)
                    Spacer(Modifier.width(6.dp))
                    val creationTime = try { 
                        val dt = LocalDateTime.parse(solicitud.fechaCreacion?.replace(" ", "T") ?: "")
                        dt.format(DateTimeFormatter.ofPattern("HH:mm"))
                    } catch(e: Exception) { "--:--" }
                    
                    Text("Creado: $creationTime", style = MaterialTheme.typography.labelSmall, color = TextLight)
                    
                    if (estado == "pendiente") {
                        Spacer(Modifier.width(12.dp))
                        val waitMin = try {
                            val start = LocalDateTime.parse(solicitud.fechaCreacion?.replace(" ", "T") ?: "")
                            ChronoUnit.MINUTES.between(start, now).coerceAtLeast(0)
                        } catch(e: Exception) { 0 }
                        Text("Espera: ${waitMin}m", style = MaterialTheme.typography.labelSmall, color = ErrorRed, fontWeight = FontWeight.Bold)
                    }
                    
                    if (estado == "completada" && solicitud.fechaAceptacion != null && solicitud.fechaFinalizacion != null) {
                        Spacer(Modifier.width(12.dp))
                        val durationMin = try {
                            val start = LocalDateTime.parse(solicitud.fechaAceptacion?.replace(" ", "T") ?: "")
                            val end = LocalDateTime.parse(solicitud.fechaFinalizacion?.replace(" ", "T") ?: "")
                            ChronoUnit.MINUTES.between(start, end)
                        } catch(e: Exception) { 0 }
                        Text("Duración: ${durationMin}m", style = MaterialTheme.typography.labelSmall, color = SuccessGreen, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Cuerpo: Información Técnica
                Text(
                    text = solicitud.tecnico?.uppercase() ?: "TÉCNICO NO ASIGNADO",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                )
                
                Spacer(Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoRowMinimal(Icons.Outlined.Build, solicitud.motivo ?: "Sin motivo")
                    InfoRowMinimal(Icons.Outlined.Inventory2, "Material: ${solicitud.material ?: "Sin material"}")
                    InfoRowMinimal(Icons.Outlined.LocationOn, solicitud.zona ?: "Sin zona")
                    InfoRowMinimal(Icons.Outlined.AssignmentInd, "Supervisor: ${solicitud.nombreSupervisor ?: "---"}")
                }

                // --- FOTOS DE EVIDENCIA (Multi-foto con eliminación) ---
                val rawPhotoPaths = remember(solicitud.fotoEvidencia) {
                    solicitud.fotoEvidencia
                        ?.split(",")
                        ?.map { it.trim() }
                        ?.filter { it.isNotBlank() && it != "null" }
                        ?: emptyList()
                }
                val photoUrls = remember(rawPhotoPaths) {
                    rawPhotoPaths.mapNotNull { getImageUrl(it) }
                }
                val hasPhoto = photoUrls.isNotEmpty()
                val canDelete = estado in listOf("llegado", "aceptada", "en_camino")
                
                if (hasPhoto) {
                    Spacer(Modifier.height(16.dp))
                    if (photoUrls.size == 1) {
                        // Una sola foto con botón de eliminar
                        Box(modifier = Modifier.fillMaxWidth()) {
                            AsyncImage(
                                model = photoUrls[0],
                                contentDescription = "Evidencia",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(LightAzure)
                                    .clickable { selectedImageUrl = photoUrls[0] },
                                contentScale = ContentScale.Crop
                            )
                            if (canDelete && !isLocked) {
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
                        // Múltiples fotos: carrusel con botones de eliminar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            photoUrls.forEachIndexed { index, url ->
                                Box {
                                    AsyncImage(
                                        model = url,
                                        contentDescription = "Evidencia",
                                        modifier = Modifier
                                            .size(150.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(LightAzure)
                                            .clickable { selectedImageUrl = url },
                                        contentScale = ContentScale.Crop
                                    )
                                    if (canDelete && !isLocked && index < rawPhotoPaths.size) {
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
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Verified, null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (photoUrls.size > 1) "${photoUrls.size} evidencias verificadas" else "Evidencia verificada",
                            color = SuccessGreen,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
                
                // --- ACCIONES TÉCNICAS (Solo Satélite) ---
                if (isSatelite) {
                    Spacer(Modifier.height(20.dp))
                    
                    // Navegación (visible en todos los estados)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MapButtonMinimal("Maps", Icons.Outlined.Map, SoftAzure, enabled = !isLocked, onClick = { openMap("google") }, modifier = Modifier.weight(1f))
                        MapButtonMinimal("Waze", Icons.Outlined.Navigation, Color(0xFF33CCFF), enabled = !isLocked, onClick = { openMap("waze") }, modifier = Modifier.weight(1f))
                    }

                    Spacer(Modifier.height(12.dp))

                    // Estados de Flujo
                    when (estado) {
                        "pendiente", "corregir" -> {
                            // Verificar si la solicitud es de la zona del satélite
                            val zonaCoincide = solicitud.zona?.uppercase() == userZona?.uppercase()
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ActionButtonModern(
                                    text = if (!zonaCoincide) "ZONA AJENA" else "ACEPTAR",
                                    color = if (!zonaCoincide) SoftGray else SuccessGreen,
                                    enabled = !isLocked && zonaCoincide,
                                    onClick = { if (zonaCoincide) onStatusUpdate(solicitud.id, "aceptada", null) },
                                    modifier = Modifier.weight(1f)
                                )
                                ActionButtonModern("CORREGIR", WarningOrange, enabled = !isLocked && zonaCoincide, onClick = { pendingActionStatus = "corregir" }, modifier = Modifier.weight(1f))
                                ActionButtonModern("RECHAZAR", ErrorRed, enabled = !isLocked && zonaCoincide, onClick = { pendingActionStatus = "rechazada" }, modifier = Modifier.weight(1f))
                            }
                        }
                        "aceptada" -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                ActionButtonModern("INICIAR RUTA", SoftAzure, enabled = !isLocked, onClick = { onStatusUpdate(solicitud.id, "en_camino", null) }, modifier = Modifier.fillMaxWidth())
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ActionButtonModern("CORREGIR", WarningOrange, enabled = !isLocked, onClick = { pendingActionStatus = "corregir" }, modifier = Modifier.weight(1f))
                                    ActionButtonModern("RECHAZAR", ErrorRed, enabled = !isLocked, onClick = { pendingActionStatus = "rechazada" }, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                        "en_camino" -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                ActionButtonModern("MARCAR LLEGADA", WarningOrange, textColor = Color.Black, enabled = !isLocked, onClick = { onStatusUpdate(solicitud.id, "llegado", null) }, modifier = Modifier.fillMaxWidth())
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ActionButtonModern("CORREGIR", WarningOrange, enabled = !isLocked, onClick = { pendingActionStatus = "corregir" }, modifier = Modifier.weight(1f))
                                    ActionButtonModern("RECHAZAR", ErrorRed, enabled = !isLocked, onClick = { pendingActionStatus = "rechazada" }, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                        "llegado" -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Siempre mostrar botones de foto para agregar más evidencias
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    HeavyButton("CÁMARA", Icons.Default.PhotoCamera, enabled = !isLocked, onClick = { onTakePhoto(solicitud.id) }, Modifier.weight(1f))
                                    HeavyButton("GALERÍA", Icons.Default.Collections, enabled = !isLocked, onClick = { onPickPhoto(solicitud.id) }, Modifier.weight(1f))
                                }
                                ActionButtonModern("FINALIZAR APOYO", if (hasPhoto) SuccessGreen else TextLight, enabled = !isLocked, onClick = { if (hasPhoto) onStatusUpdate(solicitud.id, "completada", null) }, modifier = Modifier.fillMaxWidth())
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ActionButtonModern("CORREGIR", WarningOrange, enabled = !isLocked, onClick = { pendingActionStatus = "corregir" }, modifier = Modifier.weight(1f))
                                    ActionButtonModern("RECHAZAR", ErrorRed, enabled = !isLocked, onClick = { pendingActionStatus = "rechazada" }, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                        "completada" -> {
                            // Completada: Sin botones de acción — la tarea ya finalizó
                        }
                    }
                }
                }
            }

            // Capa de bloqueo visual
            if (isLocked) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.White.copy(alpha = 0.5f))
                        .pointerInput(Unit) {
                            // Consumir todos los eventos de puntero para bloquear clics
                            awaitPointerEventScope {
                                while (true) {
                                    awaitPointerEvent()
                                }
                            }
                        }
                )
            }
        }
    }
}

@Composable
fun SupervisorSolicitudCard(
    solicitud: SolicitudItem,
    isAdmin: Boolean = false,
    onEdit: (SolicitudItem) -> Unit,
    onAssign: (SolicitudItem) -> Unit = {},
    onStatusUpdate: (Int, String, String?) -> Unit = { _, _, _ -> },
    onTrackTicket: (Int, String, String) -> Unit = { _, _, _ -> } // Nuevo parámetro
) {
    val estado = solicitud.estado?.lowercase() ?: "pendiente"
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(solicitud.id, estado) {
        if (estado == "pendiente") {
            while(true) {
                kotlinx.coroutines.delay(10000) // 10s ticker
                now = LocalDateTime.now()
            }
        }
    }
    
    var selectedImageUrl by remember { mutableStateOf<String?>(null) }
    var showAnnulDialog by remember { mutableStateOf(false) }

    if (selectedImageUrl != null) {
        ImageZoomDialog(imageUrl = selectedImageUrl!!, onDismiss = { selectedImageUrl = null })
    }

    if (showAnnulDialog) {
        AlertDialog(
            onDismissRequest = { showAnnulDialog = false },
            title = { Text("¿Anular Orden?") },
            text = { Text("Esta acción dará de baja la orden SOT #${solicitud.sot} y no podrá ser tomada por ningún satélite.") },
            confirmButton = {
                TextButton(
                    onClick = { 
                        onStatusUpdate(solicitud.id, "anulada", null)
                        showAnnulDialog = false 
                    }
                ) {
                    Text("SÍ, ANULAR", color = ErrorRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAnnulDialog = false }) {
                    Text("CANCELAR", color = TextLight)
                }
            },
            shape = RoundedCornerShape(12.dp),
            containerColor = Color.White
        )
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, LightAzure)
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            // Barra lateral de estado
            val accentColor = when(estado) {
                "completada" -> SuccessGreen
                "en_camino", "llegado" -> WarningOrange
                "aceptada" -> SoftAzure
                "rechazada" -> ErrorRed
                else -> SoftGray
            }
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(accentColor)
            )
            
            Column(modifier = Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text("ORDEN #${solicitud.sot}", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold, color = DeepAzure))
                    StatusBadge(estado)
                }

                // --- TIEMPOS (ADMIN/SUP) ---
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    val creationTime = try { 
                        LocalDateTime.parse(solicitud.fechaCreacion?.replace(" ", "T") ?: "").format(DateTimeFormatter.ofPattern("HH:mm"))
                    } catch(e: Exception) { "--:--" }
                    
                    Text("📩 $creationTime", style = MaterialTheme.typography.labelSmall, color = TextLight)
                    
                    if (estado == "pendiente") {
                        val waitMin = try {
                            val start = LocalDateTime.parse(solicitud.fechaCreacion?.replace(" ", "T") ?: "")
                            ChronoUnit.MINUTES.between(start, now).coerceAtLeast(0)
                        } catch(e: Exception) { 0 }
                        Spacer(Modifier.width(8.dp))
                        Text("⏳ Espera: ${waitMin}m", style = MaterialTheme.typography.labelSmall, color = ErrorRed, fontWeight = FontWeight.Bold)
                    }
                    
                    if (solicitud.fechaAceptacion != null && solicitud.fechaFinalizacion != null) {
                        val durationMin = try {
                            val start = LocalDateTime.parse(solicitud.fechaAceptacion?.replace(" ", "T") ?: "")
                            val end = LocalDateTime.parse(solicitud.fechaFinalizacion?.replace(" ", "T") ?: "")
                            ChronoUnit.MINUTES.between(start, end)
                        } catch(e: Exception) { 0 }
                        Spacer(Modifier.width(8.dp))
                        Text("✅ Duró: ${durationMin}m", style = MaterialTheme.typography.labelSmall, color = SuccessGreen, fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(Modifier.height(8.dp))
                
                Text(
                    text = solicitud.tecnico?.uppercase() ?: "TÉCNICO NO ASIGNADO",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                )

                Spacer(Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoRowMinimal(Icons.Outlined.Build, solicitud.motivo ?: "Sin motivo")
                    if (!solicitud.material.isNullOrBlank()) {
                        InfoRowMinimal(Icons.Outlined.Inventory2, "Material: ${solicitud.material}")
                    }
                    InfoRowMinimal(Icons.Outlined.LocationOn, solicitud.zona ?: "Sin zona")
                    
                    if (!solicitud.nombreSupervisor.isNullOrBlank()) {
                        InfoRowMinimal(Icons.Outlined.AssignmentInd, "Supervisor: ${solicitud.nombreSupervisor}")
                    }

                    if (!solicitud.coordenadas.isNullOrBlank()) {
                        InfoRowMinimal(Icons.Outlined.Map, "GPS: ${solicitud.coordenadas}")
                    }
                }

                // --- ANIMACIÓN DE RADAR BUSCANDO SATÉLITE ---
                if (estado == "pendiente") {
                    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "radarPulse")
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.4f,
                        targetValue = 1f,
                        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                            animation = androidx.compose.animation.core.tween(1000, easing = androidx.compose.animation.core.LinearEasing),
                            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                        ),
                        label = "alpha"
                    )
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        color = Color(0xFF10B981).copy(alpha = 0.08f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationSearching,
                                contentDescription = null,
                                tint = Color(0xFF10B981).copy(alpha = alpha),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Buscando Satélite Disponible...",
                                color = Color(0xFF10B981).copy(alpha = alpha),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
                
                val photoUrls = remember(solicitud.fotoEvidencia) {
                    solicitud.fotoEvidencia
                        ?.split(",")
                        ?.mapNotNull { getImageUrl(it.trim()) }
                        ?: emptyList()
                }
                val hasPhoto = photoUrls.isNotEmpty()
                
                if (hasPhoto) {
                    Spacer(Modifier.height(16.dp))
                    if (photoUrls.size == 1) {
                        AsyncImage(
                            model = photoUrls[0],
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SoftWhite)
                                .clickable { selectedImageUrl = photoUrls[0] },
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            photoUrls.forEach { url ->
                                AsyncImage(
                                    model = url,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(120.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(SoftWhite)
                                        .clickable { selectedImageUrl = url },
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${photoUrls.size} fotos de evidencia",
                            style = MaterialTheme.typography.labelSmall,
                            color = SuccessGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // --- COMENTARIO DEL SATÉLITE (Si existe) ---
                if (!solicitud.comentarioSatelite.isNullOrBlank()) {
                    val (label, icon, color) = when(estado) {
                        "corregir" -> Triple("MOTIVO DE CORRECCIÓN:", Icons.Default.EditNote, WarningOrange)
                        "rechazada" -> Triple("MOTIVO DE RECHAZO:", Icons.Default.Block, ErrorRed)
                        else -> Triple("OBSERVACIÓN DEL SATÉLITE:", Icons.Default.Feedback, SoftAzure)
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    androidx.compose.material3.Surface(
                        color = color.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(icon, null, Modifier.size(16.dp), tint = color)
                                Spacer(Modifier.width(8.dp))
                                Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = color)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(solicitud.comentarioSatelite, style = MaterialTheme.typography.bodySmall, color = TextDark)
                        }
                    }
                }
                
                if (solicitud.idSatelite != null && solicitud.idSatelite != 0) {
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PersonSearch, null, Modifier.size(16.dp), tint = SoftAzure)
                        Spacer(Modifier.width(8.dp))
                        Text("Asignado: ${solicitud.nombreSatelite}", style = MaterialTheme.typography.bodySmall, color = SoftAzure, fontWeight = FontWeight.Bold)
                    }
                }

                // --- BOTÓN DE SEGUIMIENTO SATELITAL EN VIVO (ESTILO UBER) ---
                if (estado in listOf("aceptada", "en_camino", "llegado")) {
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = {
                            onTrackTicket(
                                solicitud.id,
                                solicitud.sot ?: "",
                                solicitud.nombreSatelite ?: solicitud.tecnico ?: "Satélite"
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SoftAzure),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Icon(Icons.Default.Map, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("VER MAPA EN VIVO (ESTILO UBER)", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (estado == "pendiente") {
                            Text(
                                "ANULAR", 
                                Modifier.clickable { showAnnulDialog = true }, 
                                color = ErrorRed.copy(alpha = 0.7f), 
                                fontWeight = FontWeight.Bold, 
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
                            )
                        }
                        
                        if (estado == "corregir" || estado == "pendiente") {
                            Text(
                                "EDITAR", 
                                Modifier.clickable { onEdit(solicitud) }, 
                                color = SoftAzure, 
                                fontWeight = FontWeight.ExtraBold, 
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
                            )
                        }

                        if (isAdmin && estado !in listOf("completada", "rechazada", "anulada")) {
                            Text(
                                if (solicitud.idSatelite != null && solicitud.idSatelite != 0) "REASIGNAR" else "ASIGNAR",
                                Modifier.clickable { onAssign(solicitud) },
                                color = WarningOrange,
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
                            )
                        }
                    }
                }
            }
        }
    }
}


// --- SUB-COMPONENTES DE DISEÑO EMPRESARIAL ---

@Composable
fun StatusBadge(estado: String) {
    val color = when(estado) {
        "completada" -> SuccessGreen
        "en_camino", "llegado" -> WarningOrange
        "aceptada" -> SoftAzure
        "rechazada" -> ErrorRed
        else -> TextLight
    }
    
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Text(
            text = estado.uppercase(),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, color = color, fontSize = 9.sp)
        )
    }
}

@Composable
fun InfoRowMinimal(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(16.dp), tint = TextLight)
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = TextLight)
    }
}

@Composable
fun ActionButtonModern(text: String, color: Color, textColor: Color = Color.White, enabled: Boolean = true, onClick: () -> Unit, modifier: Modifier) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(44.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            disabledContainerColor = color.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(8.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
    ) {
        Text(text, fontWeight = FontWeight.Bold, fontSize = 9.sp, letterSpacing = 0.sp, color = if(enabled) textColor else textColor.copy(alpha = 0.5f))
    }
}

@Composable
fun MapButtonMinimal(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, enabled: Boolean = true, onClick: () -> Unit, modifier: Modifier) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if(enabled) color.copy(alpha = 0.3f) else color.copy(alpha = 0.1f))
    ) {
        Icon(icon, null, Modifier.size(18.dp), tint = if(enabled) color else color.copy(alpha = 0.3f))
        Spacer(Modifier.width(8.dp))
        Text(text, color = if(enabled) color else color.copy(alpha = 0.3f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
fun HeavyButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, enabled: Boolean = true, onClick: () -> Unit, modifier: Modifier) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(44.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = DeepAzure,
            disabledContainerColor = DeepAzure.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(8.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
    ) {
        Icon(icon, null, Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ImageZoomDialog(imageUrl: String, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.9f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Zoom Evidencia",
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.8f)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Fit
            )
            
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
            }
        }
    }
}
@Composable
fun ActionCommentDialog(
    title: String,
    description: String,
    placeholder: String = "Escribe aquí el motivo...",
    confirmColor: Color,
    onDismiss: () -> Unit, 
    onConfirm: (String) -> Unit
) {
    var comment by remember { mutableStateOf("") }
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        androidx.compose.material3.Surface(
            shape = RoundedCornerShape(12.dp), 
            color = Color.White, 
            modifier = Modifier.fillMaxWidth().padding(16.dp), 
            shadowElevation = 8.dp
        ) {
            Column(Modifier.padding(24.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = DeepAzure)
                Spacer(Modifier.height(8.dp))
                Text(description, style = MaterialTheme.typography.bodyMedium, color = TextLight)
                Spacer(Modifier.height(16.dp))
                androidx.compose.material3.OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    placeholder = { Text(placeholder, color = SoftGray) },
                    shape = RoundedCornerShape(8.dp),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SoftAzure,
                        unfocusedBorderColor = LightAzure,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = SoftWhite
                    )
                )
                Spacer(Modifier.height(24.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    androidx.compose.material3.OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) { Text("CANCELAR", color = TextLight) }
                    androidx.compose.material3.Button(
                        onClick = { if(comment.isNotBlank()) onConfirm(comment) }, 
                        modifier = Modifier.weight(1f), 
                        shape = RoundedCornerShape(8.dp), 
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = confirmColor),
                        enabled = comment.isNotBlank()
                    ) { Text("ENVIAR", color = Color.White, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}
