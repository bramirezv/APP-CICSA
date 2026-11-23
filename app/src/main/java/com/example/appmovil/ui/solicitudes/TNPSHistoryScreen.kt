package com.example.appmovil.ui.solicitudes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.appmovil.data.remote.TNPSEvidenciaItem
import com.example.appmovil.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TNPSHistoryScreen(
    idTecnico: Int,
    viewModel: SolicitudViewModel
) {
    val evidencias by viewModel.tnpsEvidencias.collectAsState()
    val isLoading by viewModel.tnpsLoading.collectAsState()
    var selectedEvidencia by remember { mutableStateOf<TNPSEvidenciaItem?>(null) }

    LaunchedEffect(Unit) {
        viewModel.fetchTNPSEvidencias(idTecnico = idTecnico)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Historial TNPS", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.fetchTNPSEvidencias(idTecnico = idTecnico) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refrescar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = DeepAzure
                )
            )
        },
        containerColor = BackgroundLight
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isLoading && evidencias.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = SoftAzure)
            } else if (evidencias.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.History, null, modifier = Modifier.size(64.dp), tint = SoftGray)
                    Text("No has enviado ninguna evidencia", color = SoftGray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(evidencias) { evidencia ->
                        TNPSHistoryCard(evidencia, onClick = { selectedEvidencia = evidencia })
                    }
                }
            }
        }
    }

    if (selectedEvidencia != null) {
        TNPSHistoryDialog(
            evidencia = selectedEvidencia!!,
            onDismiss = { selectedEvidencia = null }
        )
    }
}

@Composable
fun TNPSHistoryCard(evidencia: TNPSEvidenciaItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = evidencia.fotoUrl,
                contentDescription = null,
                modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "SOT: ${evidencia.sot ?: "N/A"}",
                    fontWeight = FontWeight.Bold,
                    color = DeepAzure,
                    fontSize = 16.sp
                )
                Text(text = "Enviado a: ${evidencia.nombreSupervisor ?: "Admin"}", fontSize = 14.sp, color = TextLight)
                val statusColor = when (evidencia.estadoValidacion?.lowercase()) {
                    "validado" -> SuccessGreen
                    "rechazado" -> ErrorRed
                    else -> WarningOrange
                }
                Text(
                    text = "Estado: ${evidencia.estadoValidacion?.uppercase() ?: "PENDIENTE"}",
                    fontSize = 12.sp,
                    color = statusColor,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = evidencia.fechaRegistro ?: "",
                    fontSize = 11.sp,
                    color = SoftGray
                )
            }
            Icon(Icons.Default.ChevronRight, null, tint = SoftGray)
        }
    }
}

@Composable
fun TNPSHistoryDialog(
    evidencia: TNPSEvidenciaItem,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Detalle de Evidencia", fontWeight = FontWeight.Bold, color = DeepAzure)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                AsyncImage(
                    model = evidencia.fotoUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit
                )
                Spacer(Modifier.height(16.dp))
                InfoRow(label = "SOT:", value = evidencia.sot ?: "N/A")
                InfoRow(label = "Enviado a:", value = evidencia.nombreSupervisor ?: "Administración")
                InfoRow(label = "Comentario tuyo:", value = evidencia.comentario ?: "Ninguno")

                Spacer(Modifier.height(16.dp))
                Divider(color = LightAzure)
                Spacer(Modifier.height(8.dp))
                
                Text("Respuesta del Supervisor", fontWeight = FontWeight.Bold, color = DeepAzure, fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                
                val statusColor = when (evidencia.estadoValidacion?.lowercase()) {
                    "validado" -> SuccessGreen
                    "rechazado" -> ErrorRed
                    else -> WarningOrange
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Estado: ", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextLight, modifier = Modifier.width(80.dp))
                    Text(text = evidencia.estadoValidacion?.uppercase() ?: "PENDIENTE", fontSize = 14.sp, color = statusColor, fontWeight = FontWeight.Bold)
                }
                
                if (evidencia.comentarioSupervisor != null && evidencia.comentarioSupervisor.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text("Comentario:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextLight)
                    Text(
                        text = evidencia.comentarioSupervisor,
                        fontSize = 13.sp,
                        color = TextDark,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}
