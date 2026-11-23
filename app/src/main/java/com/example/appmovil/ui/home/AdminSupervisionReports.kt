package com.example.appmovil.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.appmovil.data.remote.SupervisionReportItem
import com.example.appmovil.ui.theme.*
import com.example.appmovil.ui.solicitudes.SupervisionConstants
import com.google.gson.Gson

@Composable
fun AdminSupervisionReports(
    reports: List<SupervisionReportItem>,
    onRefresh: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Supervisiones Realizadas",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = TextDark)
            )
            Row {
                IconButton(onClick = {
                    val url = "${com.example.appmovil.data.remote.RetrofitClient.BASE_URL}api/export_supervisiones.php"
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                    context.startActivity(intent)
                }) {
                    Icon(Icons.Default.Download, contentDescription = "Exportar a Excel", tint = SuccessGreen)
                }
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = SoftAzure)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (reports.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hay supervisiones registradas aún.", color = SoftGray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(reports) { report ->
                    SupervisionReportCard(report)
                }
            }
        }
    }
}

@Composable
fun SupervisionReportCard(report: SupervisionReportItem) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Cabecera: Supervisor y Técnico
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).background(SoftAzure.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.FactCheck, contentDescription = null, tint = SoftAzure, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "SOT: ${report.sot ?: "---"}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextDark)
                    )
                    Text(
                        text = "${report.fechaCreacion ?: ""}",
                        style = MaterialTheme.typography.bodySmall.copy(color = SoftGray)
                    )
                }
            }


            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = SoftWhite)
            Spacer(modifier = Modifier.height(12.dp))

            // Info de Participantes
            Row(modifier = Modifier.fillMaxWidth()) {
                InfoColumn(label = "Supervisor", value = report.nombreSupervisor ?: "---", modifier = Modifier.weight(1f))
                InfoColumn(label = "Técnico", value = report.nombreTecnico ?: "---", modifier = Modifier.weight(1f))
            }

            if (!report.comentarios.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("Comentarios:", style = MaterialTheme.typography.labelMedium, color = SoftGray)
                Text(report.comentarios, style = MaterialTheme.typography.bodyMedium, color = TextDark)
            }

            // Fotos
            if (report.fotosUrls.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("Evidencias Fotográficas:", style = MaterialTheme.typography.labelMedium, color = SoftGray)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    report.fotosUrls.forEach { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = "Evidencia",
                            modifier = Modifier.size(100.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            // Botón Expandir
            Button(
                onClick = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = SoftAzure.copy(alpha = 0.08f), contentColor = SoftAzure),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(8.dp)
            ) {
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
                Spacer(Modifier.width(8.dp))
                Text(if (expanded) "Ocultar Detalles" else "Ver Detalles del Checklist", fontWeight = FontWeight.Bold)
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    val checklist = report.checklist
                    if (checklist != null) {
                        // Parsear el checklist si es necesario
                        val checklistMap = try {
                            if (checklist is Map<*, *>) {
                                checklist as Map<String, Any?>
                            } else {
                                Gson().fromJson(Gson().toJson(checklist), Map::class.java) as Map<String, Any?>
                            }
                        } catch (e: Exception) {
                            null
                        }

                        if (checklistMap != null) {
                            // 1. Datos SOT
                            val datosSot = checklistMap["datos_sot"] as? Map<String, Any?>
                            if (datosSot != null) {
                                SectionHeader("Información de Actividad", Icons.Default.Assignment)
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(8.dp)) {
                                    datosSot.forEach { (k, v) ->
                                        if (v != null && v.toString().isNotBlank()) {
                                            Row(modifier = Modifier.fillMaxWidth()) {
                                                Text(
                                                    text = "${k.replace("_", " ").uppercase()}: ",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = SoftGray,
                                                    modifier = Modifier.width(120.dp)
                                                )
                                                Text(
                                                    text = v.toString(),
                                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                    color = TextDark
                                                )
                                            }
                                        }
                                    }
                                }
                                Spacer(Modifier.height(12.dp))
                            }

                            // 2. Respuestas
                            val respuestas = checklistMap["respuestas"] as? Map<String, Any?>
                            if (respuestas != null) {
                                SectionHeader("Evaluación de Calidad", Icons.Default.Checklist)
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(8.dp)) {
                                    respuestas.forEach { (qid, ans) ->
                                        val qText = SupervisionConstants.QUESTIONS_MAP[qid] ?: qid
                                        Column {
                                            Text(qText, style = MaterialTheme.typography.bodySmall, color = TextDark)
                                            Text(
                                                text = ans.toString().uppercase(),
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                                color = if (ans.toString() == "si") SuccessGreen else if (ans.toString() == "no") ErrorRed else SoftGray
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(12.dp))
                            }

                            // 3. Penalidades
                            val penalidades = checklistMap["penalidades"] as? List<String>
                            if (!penalidades.isNullOrEmpty()) {
                                SectionHeader("Penalidades Detectadas", Icons.Default.Warning)
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(8.dp)) {
                                    penalidades.forEach { p ->
                                        Row(verticalAlignment = Alignment.Top) {
                                            Text("• ", color = ErrorRed, fontWeight = FontWeight.Bold)
                                            Text(p, style = MaterialTheme.typography.bodySmall, color = TextDark)
                                        }
                                    }
                                }
                                Spacer(Modifier.height(12.dp))
                            }
                            
                            // 4. Duración
                            val duracion = checklistMap["duracion_segundos"] ?: checklistMap["duracion"]
                            if (duracion != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().background(SoftAzure.copy(alpha = 0.05f), RoundedCornerShape(8.dp)).padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Timer, null, tint = SoftAzure, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Duración de auditoría: ${duracion}s", style = MaterialTheme.typography.labelSmall, color = SoftAzure, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            Text("No se pudo cargar el detalle del checklist.", color = ErrorRed, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Icon(icon, null, tint = SoftAzure, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, color = SoftAzure)
    }
    HorizontalDivider(color = SoftAzure.copy(alpha = 0.2f), modifier = Modifier.padding(bottom = 8.dp))
}

@Composable
fun InfoColumn(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = SoftGray)
        Text(text = value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = TextDark)
    }
}

