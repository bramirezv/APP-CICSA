package com.example.appmovil.ui.solicitudes

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.appmovil.data.remote.TrackingResponse
import kotlin.math.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.maps.android.compose.*
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.RoundCap

// ─── Colores internos ────────────────────────────────────────────────────────
private val RadarBg      = Color(0xFF0A0E1A)
private val RadarGreen   = Color(0xFF00E676)
private val RadarBlue    = Color(0xFF2979FF)
private val MapBg        = Color(0xFF1A2332)
private val MapGrid      = Color(0xFF243044)
private val SatBlue      = Color(0xFF2979FF)
private val DestRed      = Color(0xFFFF5252)
private val PathColor    = Color(0xFF64B5F6)

/** Genera un marcador premium circular para el mapa con la inicial y borde coloreado */
private fun createCustomMarkerBitmap(
    name: String,
    role: String
): BitmapDescriptor {
    val size = 110 // píxeles
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = android.graphics.Color.WHITE
    }

    val ringColor = if (role.lowercase().contains("satelite") || role.lowercase().contains("satellite")) {
        android.graphics.Color.parseColor("#2979FF") // Azul Satélite
    } else {
        android.graphics.Color.parseColor("#FF5252") // Rojo Destino
    }

    val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        color = ringColor
    }

    val initialPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#1E293B")
        textSize = 36f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    val radius = size / 2.8f
    val centerX = size / 2f
    val centerY = size / 2.8f + 4f

    canvas.drawCircle(centerX, centerY, radius, bgPaint)
    canvas.drawCircle(centerX, centerY, radius, ringPaint)

    val initial = name.firstOrNull()?.toString()?.uppercase() ?: "?"
    val textBounds = Rect()
    initialPaint.getTextBounds(initial, 0, initial.length, textBounds)
    val textHeight = textBounds.height()
    canvas.drawText(initial, centerX, centerY + textHeight / 2f, initialPaint)

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

/**
 * Pantalla de seguimiento de ticket en tiempo real — Estilo Uber.
 * Tiene dos estados animados:
 *  1. SearchingSatelite → Radar pulsante buscando.
 *  2. SateliteEnCamino  → Mini-mapa con marcadores animados.
 */
@Composable
fun TicketTrackingScreen(
    solicitudId: Int,
    sot: String,
    tecnico: String,
    userName: String,
    userId: Int,
    viewModel: SolicitudViewModel,
    isAlreadyAccepted: Boolean = false,
    onBack: () -> Unit
) {
    val trackingState by viewModel.trackingState.collectAsState()

    // Arrancar tracking al entrar
    LaunchedEffect(solicitudId) {
        viewModel.startTicketTracking(solicitudId, sot, tecnico, isAlreadyAccepted = isAlreadyAccepted)
    }

    // Limpiar al salir
    DisposableEffect(Unit) {
        onDispose { /* no paramos el tracking al navegar atrás */ }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RadarBg)
    ) {
        when (val state = trackingState) {
            is SolicitudViewModel.TrackingUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = RadarGreen,
                        strokeWidth = 4.dp,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            is SolicitudViewModel.TrackingUiState.SearchingSatelite -> {
                SearchingSateliteSection(
                    sot = state.sot,
                    tecnico = state.tecnico,
                    onCancel = {
                        viewModel.cancelTicketFromTracking(solicitudId, userId) { onBack() }
                    }
                )
            }

            is SolicitudViewModel.TrackingUiState.SateliteEnCamino -> {
                SateliteEnCaminoSection(
                    data = state.data,
                    onBack = onBack
                )
            }

            is SolicitudViewModel.TrackingUiState.Llegado -> {
                LlegadoSection(
                    sateliteNombre = state.sateliteNombre,
                    sot = sot,
                    onDone = {
                        viewModel.stopTicketTracking()
                        onBack()
                    }
                )
            }

            else -> {
                // Idle / Error — volver
                LaunchedEffect(Unit) { onBack() }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
//  SECCIÓN 1 — BUSCANDO SATÉLITE (Radar Animado)
// ═══════════════════════════════════════════════════════════════════
@Composable
private fun SearchingSateliteSection(
    sot: String,
    tecnico: String,
    onCancel: () -> Unit
) {
    // Animación infinita de radar
    val infiniteTransition = rememberInfiniteTransition(label = "radar")

    // 3 ondas con distintos delays
    val wave1 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
        label = "wave1"
    )
    val wave2 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, 600, easing = LinearEasing)),
        label = "wave2"
    )
    val wave3 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, 1200, easing = LinearEasing)),
        label = "wave3"
    )

    // Rotación del barrido de radar
    val sweep by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label = "sweep"
    )

    // Pulsación del ícono central
    val iconScale by infiniteTransition.animateFloat(
        initialValue = 0.9f, targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            tween(800, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "iconScale"
    )

    // Parpadeo del texto
    val textAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(1000, easing = LinearEasing),
            RepeatMode.Reverse
        ),
        label = "textAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Header
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(48.dp))
            Text(
                "BUSCANDO SATÉLITE",
                color = RadarGreen,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                letterSpacing = 4.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Ticket enviado a la red",
                color = Color.White.copy(0.5f),
                fontSize = 13.sp
            )
        }

        // Radar Canvas + ícono central
        Box(
            modifier = Modifier.size(280.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRadar(wave1, wave2, wave3, sweep, RadarGreen, RadarBlue)
            }

            // Ícono satélite central
            Box(
                modifier = Modifier
                    .size((64 * iconScale).dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(SatBlue.copy(0.9f), SatBlue.copy(0.4f))
                        )
                    )
                    .shadow(8.dp, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.SatelliteAlt,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        // Info del ticket
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1526))
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Detalles del Ticket",
                    color = Color.White.copy(0.6f),
                    fontSize = 11.sp,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold
                )
                InfoRow(icon = Icons.Default.ConfirmationNumber, label = "SOT", value = sot.ifEmpty { "—" })
                InfoRow(icon = Icons.Default.Person, label = "Técnico", value = tecnico.ifEmpty { "—" })
                HorizontalDivider(color = Color.White.copy(0.08f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Indicador parpadeante
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(RadarGreen.copy(alpha = textAlpha))
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Buscando satélite disponible en tu zona...",
                        color = RadarGreen.copy(alpha = textAlpha),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Botón cancelar
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = DestRed),
            border = androidx.compose.foundation.BorderStroke(1.dp, DestRed.copy(0.5f))
        ) {
            Icon(Icons.Default.Cancel, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Cancelar Solicitud", fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(16.dp))
    }
}

/** Dibuja el radar en Canvas */
private fun DrawScope.drawRadar(
    wave1: Float,
    wave2: Float,
    wave3: Float,
    sweep: Float,
    greenColor: Color,
    blueColor: Color
) {
    val cx = size.width / 2
    val cy = size.height / 2
    val maxR = size.minDimension / 2

    // Círculos de referencia fijos
    for (i in 1..3) {
        val r = maxR * (i / 3f)
        drawCircle(
            color = greenColor.copy(alpha = 0.15f),
            radius = r,
            center = Offset(cx, cy),
            style = Stroke(1.dp.toPx())
        )
    }

    // Líneas de cruceta
    drawLine(greenColor.copy(0.1f), Offset(0f, cy), Offset(size.width, cy), 1.dp.toPx())
    drawLine(greenColor.copy(0.1f), Offset(cx, 0f), Offset(cx, size.height), 1.dp.toPx())

    // Barrido rotatorio (tipo sonar)
    rotate(sweep, Offset(cx, cy)) {
        drawArc(
            brush = Brush.sweepGradient(
                0f to Color.Transparent,
                0.25f to greenColor.copy(0.3f),
                1f to Color.Transparent,
                center = Offset(cx, cy)
            ),
            startAngle = -90f,
            sweepAngle = 90f,
            useCenter = true,
            topLeft = Offset(cx - maxR, cy - maxR),
            size = androidx.compose.ui.geometry.Size(maxR * 2, maxR * 2)
        )
    }

    // Ondas expansivas
    listOf(wave1, wave2, wave3).forEach { wave ->
        val r = maxR * wave
        val alpha = (1f - wave) * 0.6f
        drawCircle(
            color = blueColor.copy(alpha = alpha),
            radius = r,
            center = Offset(cx, cy),
            style = Stroke(2.dp.toPx())
        )
    }
}

// ═══════════════════════════════════════════════════════════════════
//  SECCIÓN 2 — SATÉLITE EN CAMINO (Mini-mapa)
// ═══════════════════════════════════════════════════════════════════
@Composable
private fun SateliteEnCaminoSection(
    data: TrackingResponse,
    onBack: () -> Unit
) {
    val satelite = data.satelite
    val ticket = data.ticket
    val eta = data.etaMinutos
    var isMapLoaded by remember { mutableStateOf(false) }

    // Animación de la posición del satélite (lerp suave)
    val infiniteTransition = rememberInfiniteTransition(label = "map")

    // Determinar la ubicación de destino real o de simulación según la zona de la BD
    val destLatLng = remember(ticket?.destLat, ticket?.destLng, ticket?.zona) {
        if (ticket?.destLat != null && ticket.destLat != 0.0 && ticket.destLng != null && ticket.destLng != 0.0) {
            LatLng(ticket.destLat, ticket.destLng)
        } else {
            when (ticket?.zona?.uppercase()?.trim()) {
                "SJM", "SAN JUAN DE MIRAFLORES", "SAN JUAN DE MIRAFLOREZ" -> LatLng(-12.1622, -76.9631)
                "VES", "VILLA EL SALVADOR" -> LatLng(-12.2114, -76.9357)
                "CHORRILLOS" -> LatLng(-12.1932, -77.0145)
                "LURIN", "LURÍN" -> LatLng(-12.2800, -76.8700)
                "SAN BORJA" -> LatLng(-12.0970, -77.0076)
                "MIRAFLORES" -> LatLng(-12.1206, -77.0305)
                "SURCO", "SANTIAGO DE SURCO" -> LatLng(-12.1268, -76.9994)
                else -> LatLng(-12.1268, -76.9994) // Santiago de Surco por defecto
            }
        }
    }

    // Progreso de simulación si no hay coords reales de satélite
    var simulatedProgress by remember { mutableStateOf(0.05f) }
    val isSimulated = remember(satelite?.lat, satelite?.lng) {
        satelite?.lat == null || satelite.lat == 0.0 || satelite.lng == null || satelite.lng == 0.0
    }

    LaunchedEffect(isSimulated) {
        if (isSimulated) {
            simulatedProgress = 0.05f
            while (simulatedProgress < 1.0f) {
                kotlinx.coroutines.delay(3000L)
                simulatedProgress += 0.05f
                if (simulatedProgress > 1.0f) simulatedProgress = 1.0f
            }
        }
    }

    // Ubicación del satélite real o simulada (avanzando hacia el destino dentro del mismo distrito)
    val satLatLng = remember(satelite?.lat, satelite?.lng, simulatedProgress, destLatLng) {
        if (!isSimulated) {
            LatLng(satelite!!.lat!!, satelite.lng!!)
        } else {
            // El satélite inicia en un punto desplazado al suroeste de destLatLng y avanza en el mismo distrito
            val startLat = destLatLng.latitude - 0.012
            val startLng = destLatLng.longitude - 0.012
            val lat = startLat + (destLatLng.latitude - startLat) * simulatedProgress
            val lng = startLng + (destLatLng.longitude - startLng) * simulatedProgress
            LatLng(lat, lng)
        }
    }

    // Inicializar estado de cámara centrado en el destino
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(destLatLng, 15f)
    }

    // Ajuste dinámico de cámara para encuadrar ambos puntos
    LaunchedEffect(satLatLng, destLatLng, isMapLoaded) {
        if (isMapLoaded) {
            try {
                val builder = LatLngBounds.Builder()
                builder.include(satLatLng)
                builder.include(destLatLng)
                val bounds = builder.build()
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngBounds(bounds, 180),
                    800
                )
            } catch (e: Exception) {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(destLatLng, 14f)
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF0A0E1A), Color(0xFF1A2332)))
            )
    ) {
        // Header compacto
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .statusBarsPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(0.1f))
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.ArrowBack, null, tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("🛰️ Satélite en Camino", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text(
                    if (isSimulated) "Simulación de Recorrido Activo" else if (eta != null) "ETA aproximado: $eta min" else "En camino a tu ubicación",
                    color = RadarGreen, fontSize = 12.sp, fontWeight = FontWeight.Medium
                )
            }
        }

        // MINI MAPA — Google Maps real para visualizar la ruta y calles
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(24.dp))
        ) {
            val satMarkerIcon = remember(satelite?.nombre) {
                createCustomMarkerBitmap(satelite?.nombre ?: "S", "satelite")
            }
            val destMarkerIcon = remember(ticket?.sot) {
                createCustomMarkerBitmap(ticket?.sot ?: "T", "ticket")
            }

            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                onMapLoaded = { isMapLoaded = true },
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    myLocationButtonEnabled = false,
                    mapToolbarEnabled = false
                ),
                properties = MapProperties(
                    isMyLocationEnabled = false
                )
            ) {
                Marker(
                    state = rememberMarkerState(position = satLatLng),
                    title = satelite?.nombre ?: "Satélite",
                    snippet = if (isSimulated) "Simulando Ruta..." else "En camino",
                    icon = satMarkerIcon
                )

                Marker(
                    state = rememberMarkerState(position = destLatLng),
                    title = "Destino (SOT: ${ticket?.sot ?: "—"})",
                    snippet = ticket?.tecnico ?: "Técnico",
                    icon = destMarkerIcon
                )

                Polyline(
                    points = listOf(satLatLng, destLatLng),
                    color = PathColor,
                    width = 8f,
                    jointType = JointType.ROUND,
                    startCap = RoundCap(),
                    endCap = RoundCap()
                )
            }

            // Badge ETA arriba derecha
            if (eta != null) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = SatBlue.copy(0.9f)
                ) {
                    Text(
                        "~$eta min",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp
                    )
                }
            }

            // Leyenda inferior del mapa
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                LegendItem(color = SatBlue, label = "Satélite")
                LegendItem(color = DestRed, label = "Destino")
            }
        }

        // Card inferior con datos del satélite
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 20.dp, bottomEnd = 20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1526)),
            elevation = CardDefaults.cardElevation(12.dp)
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Avatar + nombre satélite
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(listOf(SatBlue, SatBlue.copy(0.5f)))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            (satelite?.nombre ?: "?").take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            satelite?.nombre ?: "Satélite",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(RadarGreen))
                            Spacer(Modifier.width(6.dp))
                            Text("En camino • Activo ahora", color = RadarGreen, fontSize = 12.sp)
                        }
                    }
                }

                HorizontalDivider(color = Color.White.copy(0.08f))

                // Detalles ticket
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MiniInfoChip(label = "SOT", value = ticket?.sot ?: "—")
                    MiniInfoChip(label = "Técnico", value = ticket?.tecnico?.take(12) ?: "—")
                    MiniInfoChip(label = "Zona", value = ticket?.zona ?: "—")
                }

                // Barra de progreso visual
                Text("Estado del servicio", color = Color.White.copy(0.5f), fontSize = 11.sp, letterSpacing = 1.sp)
                ProgressTrackBar(estado = ticket?.estado ?: "aceptada")
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
//  SECCIÓN 3 — LLEGADO
// ═══════════════════════════════════════════════════════════════════
@Composable
private fun LlegadoSection(
    sateliteNombre: String,
    sot: String,
    onDone: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "llegado")
    val checkScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "checkScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0A1628), Color(0xFF0D2218))))
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Ícono de check grande animado
        Box(
            modifier = Modifier
                .size((120 * checkScale).dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(listOf(RadarGreen.copy(0.3f), RadarGreen.copy(0.05f)))
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.CheckCircle,
                null,
                tint = RadarGreen,
                modifier = Modifier.size(72.dp)
            )
        }

        Spacer(Modifier.height(32.dp))

        Text(
            "¡Satélite Llegó!",
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 28.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "$sateliteNombre ya está en el punto de trabajo",
            color = Color.White.copy(0.7f),
            fontSize = 15.sp,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1526))
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                InfoRow(icon = Icons.Default.ConfirmationNumber, label = "SOT", value = sot.ifEmpty { "—" })
                InfoRow(icon = Icons.Default.Person, label = "Satélite", value = sateliteNombre)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, tint = RadarGreen, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Servicio en ejecución", color = RadarGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = RadarGreen)
        ) {
            Icon(Icons.Default.Done, null, tint = Color(0xFF0A1628))
            Spacer(Modifier.width(8.dp))
            Text("Listo", fontWeight = FontWeight.ExtraBold, color = Color(0xFF0A1628), fontSize = 16.sp)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
//  Canvas: Mapa personalizado
// ═══════════════════════════════════════════════════════════════════
private fun DrawScope.drawMap(
    satX: Float, satY: Float,
    destX: Float, destY: Float,
    dotOffset: Float,
    destPulse: Float,
    satPulse: Float
) {
    val w = size.width
    val h = size.height

    // Fondo del mapa
    drawRect(MapBg)

    // Grid de calles (fondo cuadriculado tipo mapa)
    val gridStep = 60.dp.toPx()
    var x = 0f
    while (x < w) {
        drawLine(MapGrid, Offset(x, 0f), Offset(x, h), 1.dp.toPx())
        x += gridStep
    }
    var y = 0f
    while (y < h) {
        drawLine(MapGrid, Offset(0f, y), Offset(w, y), 1.dp.toPx())
        y += gridStep
    }

    // "Calles" más gruesas aleatorias para dar realismo
    val streets = listOf(h * 0.3f, h * 0.6f)
    streets.forEach { sy ->
        drawLine(MapGrid.copy(alpha = 0.5f), Offset(0f, sy), Offset(w, sy), 3.dp.toPx())
    }
    val vStreets = listOf(w * 0.35f, w * 0.65f)
    vStreets.forEach { sx ->
        drawLine(MapGrid.copy(alpha = 0.5f), Offset(sx, 0f), Offset(sx, h), 3.dp.toPx())
    }

    val satCx = w * satX
    val satCy = h * satY
    val destCx = w * destX
    val destCy = h * destY

    // Línea punteada animada entre satélite y destino
    drawDottedLine(
        start = Offset(satCx, satCy),
        end = Offset(destCx, destCy),
        color = PathColor.copy(0.6f),
        dotProgress = dotOffset,
        strokeWidth = 3.dp.toPx()
    )

    // Pulso del destino (círculos expansivos)
    drawCircle(DestRed.copy(0.15f), destPulse * 36.dp.toPx(), Offset(destCx, destCy))
    drawCircle(DestRed.copy(0.25f), 24.dp.toPx(), Offset(destCx, destCy))

    // Marcador destino — pin
    drawCircle(DestRed, 14.dp.toPx(), Offset(destCx, destCy))
    drawCircle(Color.White, 6.dp.toPx(), Offset(destCx, destCy))

    // Halo del satélite
    drawCircle(SatBlue.copy(0.2f), satPulse * 40.dp.toPx(), Offset(satCx, satCy))

    // Marcador satélite — círculo con ícono
    drawCircle(SatBlue, 18.dp.toPx(), Offset(satCx, satCy))
    drawCircle(Color.White, 8.dp.toPx(), Offset(satCx, satCy))

    // Dirección del satélite (flecha triangular)
    val angle = atan2(destCy - satCy, destCx - satCx)
    val arrowLen = 26.dp.toPx()
    val arrowEndX = satCx + cos(angle) * arrowLen
    val arrowEndY = satCy + sin(angle) * arrowLen
    drawLine(SatBlue, Offset(satCx, satCy), Offset(arrowEndX, arrowEndY), 2.dp.toPx())

    // Puntas de la flecha
    val spread = Math.toRadians(30.0)
    val arrowW = 10.dp.toPx()
    for (sign in listOf(spread, -spread)) {
        val px = arrowEndX - cos(angle + sign).toFloat() * arrowW
        val py = arrowEndY - sin(angle + sign).toFloat() * arrowW
        drawLine(SatBlue, Offset(arrowEndX, arrowEndY), Offset(px, py), 2.dp.toPx())
    }
}

/** Línea punteada animada entre dos puntos */
private fun DrawScope.drawDottedLine(
    start: Offset,
    end: Offset,
    color: Color,
    dotProgress: Float,
    strokeWidth: Float
) {
    val dx = end.x - start.x
    val dy = end.y - start.y
    val dist = sqrt(dx * dx + dy * dy)
    if (dist < 1f) return

    val dotSpacing = 18.dp.toPx()
    val steps = (dist / dotSpacing).toInt()
    val offset = dotProgress * dotSpacing

    for (i in 0..steps) {
        val t = ((i * dotSpacing + offset) % dist) / dist
        val ox = start.x + dx * t
        val oy = start.y + dy * t
        val dotRadius = 3.dp.toPx()
        if (ox in 0f..size.width && oy in 0f..size.height) {
            drawCircle(color, dotRadius, Offset(ox, oy))
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
//  Composables auxiliares
// ═══════════════════════════════════════════════════════════════════
@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = SatBlue, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = Color.White.copy(0.5f), fontSize = 12.sp, modifier = Modifier.width(64.dp))
        Text(value, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

@Composable
private fun MiniInfoChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color.White.copy(0.4f), fontSize = 10.sp, letterSpacing = 1.sp)
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        modifier = androidx.compose.ui.Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(0.5f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ProgressTrackBar(estado: String) {
    val steps = listOf("Pendiente", "Aceptada", "En Camino", "Llegado")
    val estadoLow = estado.lowercase()
    val activeIdx = when {
        estadoLow.contains("llegado") -> 3
        estadoLow.contains("camino") -> 2
        estadoLow.contains("aceptada") -> 1
        else -> 0
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { idx, step ->
            val isActive = idx <= activeIdx
            val isDone = idx < activeIdx
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isDone -> RadarGreen
                                isActive -> SatBlue
                                else -> Color.White.copy(0.1f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isDone) {
                        Icon(Icons.Default.Check, null, tint = Color.Black, modifier = Modifier.size(14.dp))
                    } else {
                        Text("${idx + 1}", color = if (isActive) Color.White else Color.White.copy(0.3f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    step,
                    color = if (isActive) Color.White else Color.White.copy(0.3f),
                    fontSize = 9.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1
                )
            }
            if (idx < steps.lastIndex) {
                Box(
                    modifier = Modifier
                        .weight(0.5f)
                        .height(2.dp)
                        .background(
                            if (idx < activeIdx) RadarGreen else Color.White.copy(0.1f)
                        )
                )
            }
        }
    }
}
