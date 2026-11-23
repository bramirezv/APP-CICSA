package com.example.appmovil.ui.admin

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.appmovil.data.remote.SolicitudItem
import com.example.appmovil.ui.theme.*
import com.example.appmovil.utils.ReportExporter
import com.example.appmovil.utils.SatelliteStat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun StatisticsScreen(
    solicitudes: List<SolicitudItem>
) {
    val context = LocalContext.current
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // Estados para los filtros de fecha (Inicial: Mes actual)
    var startDate by remember { 
        mutableStateOf(sdf.format(Date(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000))) 
    }
    var endDate by remember { 
        mutableStateOf(sdf.format(Date())) 
    }

    // Función para mostrar el DatePicker
    fun showDatePicker(currentDate: String, onDateSelected: (String) -> Unit) {
        val dateParts = currentDate.split("-")
        val year = dateParts[0].toInt()
        val month = dateParts[1].toInt() - 1
        val day = dateParts[2].toInt()

        DatePickerDialog(context, { _, y, m, d ->
            val cal = Calendar.getInstance()
            cal.set(y, m, d)
            onDateSelected(sdf.format(cal.time))
        }, year, month, day).show()
    }

    // Filtrar solicitudes localmente para la vista
    val filteredSolicitudes = remember(solicitudes, startDate, endDate) {
        solicitudes.filter {
            val dateStr = it.fechaCreacion?.split(" ")?.firstOrNull() ?: ""
            dateStr >= startDate && dateStr <= endDate
        }
    }

    // Calcular estadísticas agrupadas por satélite (basado en la lista filtrada)
    val statsList = remember(filteredSolicitudes) {
        filteredSolicitudes
            .filter { it.idSatelite != null }
            .groupBy { it.idSatelite }
            .map { (id, items) ->
                val satName = items.first().nombreSatelite ?: "ID: $id"
                val pending = items.count { it.estado?.lowercase() == "pendiente" }
                val progress = items.count { it.estado?.lowercase() in listOf("aceptada", "camino", "en_camino", "llegado", "corregir") }
                val completed = items.count { it.estado?.lowercase() == "completada" }
                
                SatelliteStat(
                    name = satName,
                    pending = pending,
                    inProgress = progress,
                    completed = completed,
                    total = items.size
                )
            }
            .sortedByDescending { it.completed }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SoftWhite)
            .padding(16.dp)
    ) {
        // --- SECCIÓN DE FILTROS ---
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(Modifier.padding(12.dp)) {
                Text("Filtrar por Rango de Fechas", style = MaterialTheme.typography.labelMedium, color = TextLight, modifier = Modifier.padding(bottom = 8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DateFilterField("Desde", startDate, Modifier.weight(1f)) { 
                        showDatePicker(startDate) { startDate = it }
                    }
                    DateFilterField("Hasta", endDate, Modifier.weight(1f)) { 
                        showDatePicker(endDate) { endDate = it }
                    }
                }
            }
        }

        // Cabecera Resumen
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.BarChart, null, tint = SoftAzure, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Panel de Rendimiento", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = DeepAzure)
                }
                Spacer(Modifier.height(8.dp))
                Text("Estadísticas del periodo seleccionado.", color = TextLight, fontSize = 13.sp)
                
                Spacer(Modifier.height(20.dp))
                
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatSummaryItem("TOTAL", filteredSolicitudes.size.toString(), SoftAzure)
                    StatSummaryItem("COMPLETADAS", filteredSolicitudes.count { it.estado?.lowercase() == "completada" }.toString(), SuccessGreen)
                    StatSummaryItem("PENDIENTES", filteredSolicitudes.count { it.estado?.lowercase() == "pendiente" }.toString(), WarningOrange)
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Botones de Exportar
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = { ReportExporter.downloadServerReport(context, startDate, endDate) },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA50000)) // Rojo Corporativo
            ) {
                Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("SOLICITUDES", fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { 
                    val cal = Calendar.getInstance()
                    val endMonth = endDate.split("-")[1].toInt()
                    val endYear = endDate.split("-")[0].toInt()
                    ReportExporter.downloadAttendanceMonthlyReport(context, endMonth, endYear)
                },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)) // Verde Asistencia
            ) {
                Icon(Icons.Default.EventAvailable, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("ASISTENCIA", fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            
            OutlinedButton(
                onClick = { ReportExporter.exportDetailedReport(context, filteredSolicitudes) },
                modifier = Modifier.weight(0.8f).height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SoftAzure)
            ) {
                Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp), tint = SoftAzure)
                Spacer(Modifier.width(4.dp))
                Text("CSV", fontSize = 9.sp, color = SoftAzure)
            }
        }

        Spacer(Modifier.height(20.dp))

        Text("Ranking de Atención por Satélite", fontWeight = FontWeight.ExtraBold, color = DeepAzure, modifier = Modifier.padding(bottom = 12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(statsList) { stat ->
                SatelliteStatRow(stat)
            }
            if (statsList.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Text("Sin datos en este rango", color = TextLight, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun DateFilterField(label: String, value: String, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier) {
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextLight, modifier = Modifier.padding(start = 4.dp, bottom = 2.dp))
        Surface(
            modifier = Modifier.fillMaxWidth().clickable { onClick() },
            shape = RoundedCornerShape(8.dp),
            color = SoftGray.copy(alpha = 0.1f),
            border = androidx.compose.foundation.BorderStroke(1.dp, SoftGray.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DeepAzure)
                Icon(Icons.Outlined.CalendarMonth, null, modifier = Modifier.size(16.dp), tint = SoftAzure)
            }
        }
    }
}

@Composable
fun StatSummaryItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Black, color = color)
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextLight)
    }
}

@Composable
fun SatelliteStatRow(stat: SatelliteStat) {
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
            Icon(Icons.Default.AccountCircle, null, tint = SoftGray, modifier = Modifier.size(40.dp))
            Spacer(Modifier.width(16.dp))
            
            Column(Modifier.weight(1f)) {
                Text(stat.name.uppercase(), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Total: ${stat.total}", fontSize = 12.sp, color = TextLight)
                    Spacer(Modifier.width(8.dp))
                    Text("•", color = SoftGray)
                    Spacer(Modifier.width(8.dp))
                    Text("${stat.completed} Logradas", fontSize = 12.sp, color = SuccessGreen, fontWeight = FontWeight.Medium)
                }
            }

            Box(contentAlignment = Alignment.CenterEnd) {
                Text(
                    text = "${if(stat.total > 0) (stat.completed * 100 / stat.total) else 0}%",
                    fontWeight = FontWeight.Black,
                    color = SoftAzure,
                    fontSize = 16.sp
                )
            }
        }
    }
}
