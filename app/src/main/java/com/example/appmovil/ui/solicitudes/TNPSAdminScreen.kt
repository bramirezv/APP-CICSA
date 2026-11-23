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
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TNPSAdminScreen(
    viewModel: SolicitudViewModel
) {
    val evidencias by viewModel.tnpsEvidencias.collectAsState()
    val isLoading by viewModel.tnpsLoading.collectAsState()
    var selectedEvidencia by remember { mutableStateOf<TNPSEvidenciaItem?>(null) }

    LaunchedEffect(Unit) {
        viewModel.fetchTNPSEvidencias()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Verificación TNPS", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.fetchTNPSEvidencias() }) {
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
                    Icon(Icons.Default.Inbox, null, modifier = Modifier.size(64.dp), tint = SoftGray)
                    Text("No hay evidencias registradas", color = SoftGray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(evidencias) { evidencia ->
                        TNPSEvidenciaCard(evidencia, onClick = { selectedEvidencia = evidencia })
                    }
                }
            }
        }
    }

    if (selectedEvidencia != null) {
        TNPSEvidenciaDetailDialog(
            evidencia = selectedEvidencia!!,
            onDismiss = { selectedEvidencia = null }
        )
    }
}

@Composable
fun TNPSEvidenciaCard(evidencia: TNPSEvidenciaItem, onClick: () -> Unit) {
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
                Text(text = evidencia.nombreTecnico, fontSize = 14.sp, color = TextLight)
                Text(
                    text = evidencia.fechaRegistro ?: "",
                    fontSize = 12.sp,
                    color = SoftGray
                )
            }
            Icon(Icons.Default.ChevronRight, null, tint = SoftGray)
        }
    }
}

@Composable
fun TNPSEvidenciaDetailDialog(evidencia: TNPSEvidenciaItem, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        },
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
                        .height(300.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit
                )
                Spacer(Modifier.height(16.dp))
                InfoRow(label = "Técnico:", value = evidencia.nombreTecnico)
                InfoRow(label = "SOT:", value = evidencia.sot ?: "N/A")
                InfoRow(label = "Fecha:", value = evidencia.fechaRegistro ?: "")
                InfoRow(label = "Comentario:", value = evidencia.comentario ?: "Sin comentarios")
                
                if (evidencia.latitud != null && evidencia.longitud != null) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "GPS: ${evidencia.latitud}, ${evidencia.longitud}",
                            fontSize = 12.sp,
                            color = SuccessGreen
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(text = label, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DeepAzure, modifier = Modifier.width(80.dp))
        Text(text = value, fontSize = 14.sp, color = TextLight)
    }
}
