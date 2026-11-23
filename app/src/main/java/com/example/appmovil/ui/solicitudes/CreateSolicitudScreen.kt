package com.example.appmovil.ui.solicitudes

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.appmovil.ui.components.ElegantTextField
import com.example.appmovil.ui.theme.*

/**
 * Pantalla para crear/editar una solicitud estructurada (SOT, Técnico, etc.)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSolicitudScreen(
    userName: String,
    userRol: String = "",
    onBack: () -> Unit,
    onTrackTicket: (solicitudId: Int, sot: String, tecnico: String) -> Unit = { _, _, _ -> },
    viewModel: SolicitudViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            val ctx = androidx.compose.ui.platform.LocalContext.current.applicationContext
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return SolicitudViewModel(context = ctx) as T
            }
        }
    ),
    tipo: String = "normal"
) {
    val isTecnico = userRol.contains("tecnico")
    
    // Si es tecnico, pre-llenar su nombre una sola vez al entrar
    LaunchedEffect(Unit) {
        if (isTecnico && viewModel.tecnico.value.isBlank() && viewModel.editSolicitudId.value == null) {
            viewModel.onTecnicoChange(userName)
        }
    }
    
    val sot by viewModel.sot.collectAsState()
    val tecnico by viewModel.tecnico.collectAsState()
    val motivo by viewModel.motivo.collectAsState()
    val material by viewModel.material.collectAsState()
    val coordenadas by viewModel.coordenadas.collectAsState()
    val zona by viewModel.zona.collectAsState()
    val createState by viewModel.createState.collectAsState()
    val editSolicitudId by viewModel.editSolicitudId.collectAsState()
    val lastCreatedTicketInfo by viewModel.lastCreatedTicketInfo.collectAsState()

    // Navegar al tracking cuando el ticket normal del supervisor es creado exitosamente
    val isSupervisor = userRol.contains("supervisor") || userRol.contains("admin")
    LaunchedEffect(lastCreatedTicketInfo) {
        val info = lastCreatedTicketInfo
        if (info != null && tipo == "normal" && isSupervisor) {
            viewModel.clearLastCreatedTicket()
            kotlinx.coroutines.delay(800L) // breve pausa para mostrar el éxito
            onTrackTicket(info.first, info.second, info.third)
        }
    }

    val satelliteStatuses by viewModel.satelliteStatuses.collectAsState()
    val selectedSupId by viewModel.selectedSupervisorId.collectAsState()
    val selectedSupName by viewModel.selectedSupervisorName.collectAsState()
    
    val isEditing = editSolicitudId != null
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        viewModel.fetchSatelliteStatuses()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (isEditing) "Editar Solicitud" else if (tipo == "auxiliar") "Nueva Plantilla Auxiliar" else "Nueva Solicitud", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = TextDark
                )
            )
        }
    ) { padding ->        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Brush.verticalGradient(listOf(Color.White, Color(0xFFF1F5F9))))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Cabecera Informativa
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = if (tipo == "auxiliar") Color(0xFFFF9800) else DeepAzure),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if(isEditing) Icons.Default.EditNote else if(tipo == "auxiliar") Icons.Default.AssignmentTurnedIn else Icons.Default.AddBox, null, tint = Color.White, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(if(isEditing) "Modificando Registro" else if(tipo == "auxiliar") "Registrar Asistencia" else "Generar Nueva Orden", color = Color.White, fontWeight = FontWeight.Bold)
                            Text(if(tipo == "auxiliar") "Documenta tu apoyo al técnico" else "Diligencia los campos con precisión", color = Color.White.copy(0.7f), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // SECCIÓN 1: DATOS DEL TÉCNICO
                SectionHeader(title = if(tipo == "auxiliar") "TÉCNICO ASISTIDO" else "INFORMACIÓN DEL TÉCNICO", icon = Icons.Outlined.Person)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, Color.LightGray.copy(0.2f))
                ) {
                    Column(Modifier.padding(16.dp)) {
                        ElegantTextField(
                            value = tecnico,
                            onValueChange = viewModel::onTecnicoChange,
                            label = if(tipo == "auxiliar") "Nombre del Técnico que asistes" else "Nombre del Técnico",
                            icon = Icons.Outlined.Person,
                            enabled = createState !is CreateSolicitudUiState.Loading
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // SECCIÓN 2: DETALLES DEL TRABAJO
                SectionHeader(title = "DETALLES DEL TRABAJO", icon = Icons.Outlined.Assignment)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, Color.LightGray.copy(0.2f))
                ) {
                    Column(Modifier.padding(16.dp)) {
                        ElegantTextField(
                            value = sot,
                            onValueChange = viewModel::onSotChange,
                            label = "SOT / Orden de Trabajo",
                            icon = Icons.Outlined.ConfirmationNumber,
                            enabled = createState !is CreateSolicitudUiState.Loading
                        )
                        Spacer(Modifier.height(12.dp))
                        ElegantTextField(
                            value = motivo,
                            onValueChange = viewModel::onMotivoChange,
                            label = "Motivo",
                            icon = Icons.Outlined.ChatBubbleOutline,
                            enabled = createState !is CreateSolicitudUiState.Loading,
                            singleLine = false,
                            maxLines = 3
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // SECCIÓN 3: LOGÍSTICA Y UBICACIÓN
                SectionHeader(title = "LOGÍSTICA Y UBICACIÓN", icon = Icons.Outlined.LocationOn)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, Color.LightGray.copy(0.2f))
                ) {
                    Column(Modifier.padding(16.dp)) {
                        ElegantTextField(
                            value = material,
                            onValueChange = viewModel::onMaterialChange,
                            label = "Materiales Requeridos",
                            icon = Icons.Outlined.Inventory2,
                            enabled = createState !is CreateSolicitudUiState.Loading
                        )
                        Spacer(Modifier.height(12.dp))
                        
                        // Distrito (Selector Premium)
                        var zonaExpanded by remember { mutableStateOf(false) }
                        val zonas by viewModel.availableZones.collectAsState()

                        ExposedDropdownMenuBox(
                            expanded = zonaExpanded,
                            onExpandedChange = { if (createState !is CreateSolicitudUiState.Loading) zonaExpanded = !zonaExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = zona,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Asignar Distrito") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = zonaExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = DeepAzure,
                                    unfocusedBorderColor = SoftGray.copy(0.5f),
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color(0xFFF8FAFC)
                                ),
                                leadingIcon = { Icon(Icons.Default.Place, null, tint = DeepAzure) }
                            )

                            ExposedDropdownMenu(
                                expanded = zonaExpanded,
                                onDismissRequest = { zonaExpanded = false },
                                modifier = Modifier.background(Color.White)
                            ) {
                                for (zonaOption in zonas) {
                                    DropdownMenuItem(
                                        text = { Text(zonaOption, color = TextDark, fontWeight = if(zona == zonaOption) FontWeight.Bold else FontWeight.Normal) },
                                        onClick = {
                                            viewModel.onZonaChange(zonaOption)
                                            zonaExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        ElegantTextField(
                            value = coordenadas,
                            onValueChange = viewModel::onCoordenadasChange,
                            label = "Coordenadas Gps / Link Maps",
                            icon = Icons.Outlined.Map,
                            enabled = createState !is CreateSolicitudUiState.Loading
                        )

                        if (tipo == "auxiliar") {
                            Spacer(Modifier.height(24.dp))
                            SectionHeader(title = "SUPERVISOR QUE VALIDA", icon = Icons.Outlined.VerifiedUser)
                            val supervisors = remember(satelliteStatuses) {
                                satelliteStatuses.filter { 
                                    val r = it.rol?.lowercase() ?: ""
                                    r == "supervisor" || r == "admin" || r == "usuario"
                                }
                            }
                            var supExpanded by remember { mutableStateOf(false) }
                            
                            ExposedDropdownMenuBox(
                                expanded = supExpanded,
                                onExpandedChange = { if (createState !is CreateSolicitudUiState.Loading) supExpanded = !supExpanded },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = selectedSupName ?: "Selecciona un supervisor...",
                                    onValueChange = {},
                                    readOnly = true,
                                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    leadingIcon = { Icon(Icons.Outlined.Person, null, tint = Color(0xFF9C27B0)) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = supExpanded) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF9C27B0),
                                        unfocusedBorderColor = Color(0xFF9C27B0).copy(0.3f),
                                        focusedLabelColor = Color(0xFF9C27B0)
                                    )
                                )
                                ExposedDropdownMenu(
                                    expanded = supExpanded,
                                    onDismissRequest = { supExpanded = false },
                                    modifier = Modifier.background(Color.White)
                                ) {
                                    if (supervisors.isEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text("No hay supervisores disponibles", color = TextLight, fontSize = 12.sp) },
                                            onClick = { supExpanded = false }
                                        )
                                    } else {
                                        supervisors.forEach { sup ->
                                            DropdownMenuItem(
                                                text = { Text(sup.nombre, color = TextDark, fontWeight = if(selectedSupId == sup.id) FontWeight.Bold else FontWeight.Normal) },
                                                onClick = {
                                                    viewModel.onSupervisorSelectionChange(sup.id, sup.nombre)
                                                    supExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // BOTÓN DE ACCIÓN PRINCIPAL PREMIUM
                Button(
                    onClick = { viewModel.createSolicitud(userName, tipo) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    enabled = createState !is CreateSolicitudUiState.Loading && createState !is CreateSolicitudUiState.Success,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DeepAzure),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    if (createState is CreateSolicitudUiState.Loading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(if(isEditing) Icons.Default.Save else Icons.Default.Send, null)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = if (isEditing) "GUARDAR CAMBIOS" else if (tipo == "auxiliar") "ENVIAR PLANTILLA" else "ENVIAR SOLICITUD", 
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Feedback visual dinámico
                AnimatedVisibility(visible = createState is CreateSolicitudUiState.Success) {
                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.delay(1500)
                        onBack()
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(0.1f)),
                        border = BorderStroke(1.dp, SuccessGreen.copy(0.3f))
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                (createState as CreateSolicitudUiState.Success).message,
                                color = SuccessGreen,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = createState is CreateSolicitudUiState.Error) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(0.1f)),
                        border = BorderStroke(1.dp, ErrorRed.copy(0.3f))
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Error, null, tint = ErrorRed)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Por favor completa los campos obligatorios (SOT, Técnico, Motivo, Distrito)", color = ErrorRed)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = TextLight.copy(0.7f), modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = TextLight.copy(0.7f),
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}
