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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TNPSValidationScreen(
    idSupervisor: Int,
    viewModel: SolicitudViewModel
) {
    val evidencias by viewModel.tnpsEvidencias.collectAsState()
    val isLoading by viewModel.tnpsLoading.collectAsState()
    var selectedEvidencia by remember { mutableStateOf<TNPSEvidenciaItem?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.fetchTNPSEvidencias(idSupervisor = idSupervisor)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Validación TNPS", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.fetchTNPSEvidencias(idSupervisor = idSupervisor) }) {
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
                    Text("No hay evidencias pendientes", color = SoftGray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(evidencias) { evidencia ->
                        TNPSValidationCard(evidencia, onClick = { selectedEvidencia = evidencia })
                    }
                }
            }
        }
    }

    if (selectedEvidencia != null) {
        TNPSValidationDialog(
            evidencia = selectedEvidencia!!,
            viewModel = viewModel,
            onDismiss = { selectedEvidencia = null },
            onResult = { success, message ->
                selectedEvidencia = null
                // Launch coroutine to show snackbar handled by the UI scope
            }
        )
    }
}

@Composable
fun TNPSValidationCard(evidencia: TNPSEvidenciaItem, onClick: () -> Unit) {
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
                val statusColor = when (evidencia.estadoValidacion) {
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
            }
            Icon(Icons.Default.ChevronRight, null, tint = SoftGray)
        }
    }
}

@Composable
fun TNPSValidationDialog(
    evidencia: TNPSEvidenciaItem,
    viewModel: SolicitudViewModel,
    onDismiss: () -> Unit,
    onResult: (Boolean, String) -> Unit
) {
    var comentario by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        title = {
            Text("Validar Evidencia", fontWeight = FontWeight.Bold, color = DeepAzure)
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
                InfoRow(label = "Técnico:", value = evidencia.nombreTecnico)
                InfoRow(label = "SOT:", value = evidencia.sot ?: "N/A")
                InfoRow(label = "Comentario:", value = evidencia.comentario ?: "Ninguno")

                Spacer(Modifier.height(16.dp))
                if (evidencia.estadoValidacion == "pendiente") {
                    OutlinedTextField(
                        value = comentario,
                        onValueChange = { comentario = it },
                        label = { Text("Comentario (Opcional)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                } else {
                    InfoRow(label = "Estado:", value = evidencia.estadoValidacion ?: "N/A")
                    InfoRow(label = "Comentario Supervisor:", value = evidencia.comentarioSupervisor ?: "Ninguno")
                }
            }
        },
        confirmButton = {
            if (evidencia.estadoValidacion == "pendiente") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            isProcessing = true
                            viewModel.validateTNPSEvidencia(evidencia.id, "rechazado", comentario) { success ->
                                isProcessing = false
                                if (success) onResult(true, "Rechazado") else onResult(false, "Error")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                        enabled = !isProcessing
                    ) {
                        Text("Rechazar")
                    }
                    Button(
                        onClick = {
                            isProcessing = true
                            viewModel.validateTNPSEvidencia(evidencia.id, "validado", comentario) { success ->
                                isProcessing = false
                                if (success) onResult(true, "Validado") else onResult(false, "Error")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                        enabled = !isProcessing
                    ) {
                        Text("Aprobar")
                    }
                }
            } else {
                TextButton(onClick = onDismiss) { Text("Cerrar") }
            }
        },
        dismissButton = {
            if (evidencia.estadoValidacion == "pendiente") {
                TextButton(onClick = onDismiss, enabled = !isProcessing) { Text("Cancelar") }
            }
        }
    )
}
