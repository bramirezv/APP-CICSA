package com.example.appmovil.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import com.example.appmovil.data.remote.AttendanceItem
import com.example.appmovil.ui.solicitudes.AttendanceUiState
import com.example.appmovil.ui.solicitudes.SolicitudViewModel
import com.example.appmovil.ui.theme.*
import com.example.appmovil.utils.ReportExporter
import java.text.SimpleDateFormat
import java.util.*

/**
 * Modelo auxiliar para agrupar asistencia por usuario
 */
private data class UserAttendanceSummary(
    val userId: Int,
    val nombre: String,
    val horaIngreso: String?,   // Primera hora de INGRESO del día
    val horaSalida: String?,    // Última hora de SALIDA del día
    val totalEventos: Int
)

@Composable
fun AttendanceScreen(
    viewModel: SolicitudViewModel
) {
    val context = LocalContext.current
    val attendanceState by viewModel.attendanceState.collectAsState()
    
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    LaunchedEffect(Unit) {
        viewModel.fetchAttendance(today)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SoftWhite)
            .padding(16.dp)
    ) {
        // Cabecera
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.EventAvailable, null, tint = SoftAzure, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Control de Asistencia", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = DeepAzure)
                    }
                    IconButton(onClick = { viewModel.fetchAttendance(today) }) {
                        Icon(Icons.Default.Refresh, "Refrescar", tint = SoftAzure)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Registros de inicio y fin de ruta para el día: $today", color = TextLight, fontSize = 14.sp)
                
                Spacer(Modifier.height(20.dp))

                // Estadísticas Rápidas
                if (attendanceState is AttendanceUiState.Success) {
                    val list = (attendanceState as AttendanceUiState.Success).attendance
                    val uniqueSats = list.map { it.userId }.distinct().size
                    val ingresos = list.count { it.evento == "INGRESO" }
                    val salidas = list.count { it.evento == "SALIDA" }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Card(
                            Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = SoftAzure.copy(alpha = 0.05f)),
                            border = BorderStroke(1.dp, SoftAzure.copy(alpha = 0.1f))
                        ) {
                            Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(uniqueSats.toString(), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = SoftAzure)
                                Text("PERSONAL", fontSize = 10.sp, color = TextLight, fontWeight = FontWeight.Bold)
                            }
                        }
                        Card(
                            Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.05f)),
                            border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.1f))
                        ) {
                            Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(ingresos.toString(), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                                Text("INGRESOS", fontSize = 10.sp, color = TextLight, fontWeight = FontWeight.Bold)
                            }
                        }
                        Card(
                            Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.05f)),
                            border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.1f))
                        ) {
                            Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(salidas.toString(), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = ErrorRed)
                                Text("SALIDAS", fontSize = 10.sp, color = TextLight, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
                
                Button(
                    onClick = { 
                        if (attendanceState is AttendanceUiState.Success) {
                            ReportExporter.exportAttendance(context, (attendanceState as AttendanceUiState.Success).attendance)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                    enabled = attendanceState is AttendanceUiState.Success
                ) {
                    Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("EXCEL", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        when (val state = attendanceState) {
            is AttendanceUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SoftAzure)
                }
            }
            is AttendanceUiState.Success -> {
                if (state.attendance.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No hay registros de asistencia para hoy", color = TextLight)
                    }
                } else {
                    // Agrupar por usuario
                    val summaries = remember(state.attendance) {
                        buildUserSummaries(state.attendance)
                    }

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Sección: Resumen por usuario (hora de activar ruta y cerrar ruta)
                        item {
                            Text(
                                "Resumen por Satélite",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = DeepAzure,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }

                        items(summaries) { summary ->
                            UserAttendanceCard(summary)
                        }

                        // Separador visual
                        item {
                            Spacer(Modifier.height(16.dp))
                            HorizontalDivider(color = SoftAzure.copy(alpha = 0.2f))
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Historial de Eventos",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = DeepAzure,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }

                        // Sección: Lista individual de eventos
                        items(state.attendance.sortedByDescending { it.hora }) { item ->
                            AttendanceRow(item)
                        }
                    }
                }
            }
            is AttendanceUiState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Error: ${state.message}", color = ErrorRed)
                }
            }
            else -> {}
        }
    }
}

/**
 * Agrupa la lista de asistencias por usuario y calcula la primera hora de INGRESO
 * y la última hora de SALIDA de cada uno.
 */
private fun buildUserSummaries(attendance: List<AttendanceItem>): List<UserAttendanceSummary> {
    return attendance
        .groupBy { it.userId }
        .map { (userId, events) ->
            val nombre = events.first().nombre
            // Primer INGRESO del día (hora más temprana)
            val primerIngreso = events
                .filter { it.evento == "INGRESO" }
                .minByOrNull { it.hora }
                ?.hora
            // Última SALIDA del día (hora más tardía)
            val ultimaSalida = events
                .filter { it.evento == "SALIDA" }
                .maxByOrNull { it.hora }
                ?.hora

            UserAttendanceSummary(
                userId = userId,
                nombre = nombre,
                horaIngreso = primerIngreso,
                horaSalida = ultimaSalida,
                totalEventos = events.size
            )
        }
        .sortedBy { it.horaIngreso ?: "99:99" } // Los que no tienen ingreso van al final
}

/**
 * Card que muestra el resumen de un satélite: hora de activar ruta y hora de cerrar ruta
 */
@Composable
private fun UserAttendanceCard(summary: UserAttendanceSummary) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(Modifier.padding(16.dp)) {
            // Nombre del usuario
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SoftAzure.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        summary.nombre.take(1).uppercase(),
                        fontWeight = FontWeight.Black,
                        color = SoftAzure,
                        fontSize = 16.sp
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        summary.nombre.uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextDark
                    )
                    Text(
                        "${summary.totalEventos} evento(s) registrados",
                        fontSize = 11.sp,
                        color = TextLight
                    )
                }
                // Indicador de estado
                val hasCompleted = summary.horaIngreso != null && summary.horaSalida != null
                val statusColor = when {
                    hasCompleted -> ErrorRed   // Ya cerró ruta
                    summary.horaIngreso != null -> SuccessGreen  // Ruta activa
                    else -> TextLight
                }
                val statusText = when {
                    hasCompleted -> "Ruta Cerrada"
                    summary.horaIngreso != null -> "En Ruta"
                    else -> "Sin Actividad"
                }
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = statusColor.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.3f))
                ) {
                    Text(
                        statusText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Fila de horarios
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Activó Ruta
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = SuccessGreen.copy(alpha = 0.06f),
                    border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.15f))
                ) {
                    Column(
                        Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Login,
                                null,
                                tint = SuccessGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "Activó Ruta",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SuccessGreen
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            summary.horaIngreso ?: "— — —",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = if (summary.horaIngreso != null) DeepAzure else TextLight,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Cerró Ruta
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = ErrorRed.copy(alpha = 0.06f),
                    border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.15f))
                ) {
                    Column(
                        Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Logout,
                                null,
                                tint = ErrorRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "Cerró Ruta",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ErrorRed
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            summary.horaSalida ?: "— — —",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = if (summary.horaSalida != null) DeepAzure else TextLight,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AttendanceRow(item: AttendanceItem) {
    val isIngreso = item.evento == "INGRESO"
    val color = if (isIngreso) SuccessGreen else ErrorRed
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isIngreso) Icons.Default.Login else Icons.Default.Logout,
                    null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(Modifier.weight(1f)) {
                Text(item.nombre.uppercase(), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
                Text(if (isIngreso) "Inició Ruta" else "Finalizó Ruta", fontSize = 12.sp, color = color, fontWeight = FontWeight.Medium)
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(item.hora, fontWeight = FontWeight.Black, color = DeepAzure, fontSize = 16.sp)
                Text(item.fecha, fontSize = 10.sp, color = TextLight)
            }
        }
    }
}
