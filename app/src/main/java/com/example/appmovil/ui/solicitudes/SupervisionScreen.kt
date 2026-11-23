package com.example.appmovil.ui.solicitudes

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.appmovil.data.remote.TecnicoItem
import com.example.appmovil.ui.theme.*
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.io.File

@Composable
fun SupervisionScreen(
    supervisorId: Int,
    viewModel: SolicitudViewModel
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Mi Equipo", "Nueva Supervisión")
    val myTeam by viewModel.myTeam.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.fetchMyTeam(supervisorId)
        viewModel.searchTecnicos("")
    }

    Column(modifier = Modifier.fillMaxSize().background(SoftWhite)) {
        // Custom Header / TabRow
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 4.dp
        ) {
            Column {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SoftAzure.copy(0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.VerifiedUser, null, tint = SoftAzure)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Gestión de Supervisión", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = DeepAzure)
                        Text("Monitoreo en tiempo real", fontSize = 12.sp, color = TextLight)
                    }
                }
                
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.White,
                    contentColor = SoftAzure,
                    divider = {},
                    indicator = { tabPositions ->
                        if (selectedTab < tabPositions.size) {
                            Box(
                                Modifier
                                    .tabIndicatorOffset(tabPositions[selectedTab])
                                    .height(3.dp)
                                    .padding(horizontal = 40.dp)
                                    .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                    .background(SoftAzure)
                            )
                        }
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { 
                                Text(
                                    title, 
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 14.sp
                                ) 
                            }
                        )
                    }
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> MiEquipoTab(supervisorId, viewModel, myTeam)
                1 -> NuevaSupervisionTab(supervisorId, viewModel, myTeam)
            }
        }
    }
}

@Composable
fun MiEquipoTab(supervisorId: Int, viewModel: SolicitudViewModel, myTeam: List<TecnicoItem>) {
    var searchQuery by remember { mutableStateOf("") }
    val searchResults by viewModel.searchResult.collectAsState()
    val isSearchingLoading by viewModel.isSearchingTecnicos.collectAsState()

    Column(modifier = Modifier.padding(20.dp)) {
        // Premium Search Bar
        Surface(
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            shadowElevation = 2.dp,
            border = BorderStroke(1.dp, LightAzure.copy(0.5f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSearchingLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = SoftAzure)
                } else {
                    Icon(Icons.Default.Search, null, tint = SoftGray, modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (searchQuery.isEmpty()) Text("Buscar técnico por nombre o DNI...", color = TextLight, fontSize = 14.sp)
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { newValue ->
                            searchQuery = newValue
                            viewModel.searchTecnicos(newValue)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(fontSize = 15.sp, color = TextDark, fontWeight = FontWeight.Medium),
                        singleLine = true
                    )
                }
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { 
                        searchQuery = ""
                        viewModel.searchTecnicos("")
                    }) {
                        Icon(Icons.Default.Cancel, null, tint = SoftGray, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        // Animated Search Results
        AnimatedVisibility(
            visible = searchQuery.isNotEmpty(),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Surface(
                modifier = Modifier.padding(top = 12.dp).fillMaxWidth().heightIn(max = 240.dp),
                color = Color.White,
                shape = RoundedCornerShape(20.dp),
                shadowElevation = 10.dp,
                border = BorderStroke(1.dp, SoftAzure.copy(0.1f))
            ) {
                if (searchResults.isEmpty() && !isSearchingLoading) {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No se encontraron coincidencias", color = TextLight, fontSize = 14.sp)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                        items(searchResults) { tecnico ->
                            TecnicoSearchItem(tecnico) {
                                viewModel.manageTeam(supervisorId, tecnico.id, "add")
                                searchQuery = ""
                                viewModel.searchTecnicos("")
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Equipo a Cargo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DeepAzure)
            Spacer(modifier = Modifier.weight(1f))
            Surface(
                color = SoftAzure.copy(0.1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "${myTeam.size} técnicos", 
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    color = SoftAzure,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (myTeam.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.GroupAdd, null, modifier = Modifier.size(64.dp), tint = SoftGray.copy(0.3f))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Agrega técnicos para supervisar", color = TextLight, textAlign = TextAlign.Center)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(myTeam) { tecnico ->
                    TecnicoTeamItem(tecnico) {
                        viewModel.manageTeam(supervisorId, tecnico.id, "remove")
                    }
                }
            }
        }
    }
}

@Composable
fun TecnicoSearchItem(tecnico: TecnicoItem, onAdd: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAdd() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(SoftAzure, SoftAzure.copy(0.6f)))),
            contentAlignment = Alignment.Center
        ) {
            Text(tecnico.nombre.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(tecnico.nombre, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
            Text("DNI: ${tecnico.dni ?: "---"}", fontSize = 12.sp, color = TextLight)
        }
        Icon(Icons.Default.AddCircle, null, tint = SoftAzure)
    }
}

@Composable
fun TecnicoTeamItem(tecnico: TecnicoItem, onRemove: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, LightAzure.copy(0.3f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SoftWhite),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, null, tint = DeepAzure)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(tecnico.nombre, fontWeight = FontWeight.Bold, color = TextDark, fontSize = 15.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(12.dp), tint = SoftGray)
                    Text(" Zona: ${tecnico.zona ?: "Sin asignar"}", fontSize = 12.sp, color = TextLight)
                }
            }
            IconButton(
                onClick = onRemove,
                colors = IconButtonDefaults.iconButtonColors(containerColor = ErrorRed.copy(0.1f))
            ) {
                Icon(Icons.Default.Delete, null, tint = ErrorRed, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NuevaSupervisionTab(supervisorId: Int, viewModel: SolicitudViewModel, myTeam: List<TecnicoItem>) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val isSupervisionStarted by viewModel.isSupervisionStarted.collectAsState()
    val elapsedTime by viewModel.supervisionElapsedTime.collectAsState()
    val selectedTecnico by viewModel.selectedSupervisionTecnico.collectAsState()
    val sot by viewModel.supervisionSot.collectAsState()
    val capturedPhotos: List<Pair<Uri, File>> by viewModel.capturedSupervisionPhotos.collectAsState()

    var comentarios by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    fun formatTime(seconds: Long): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return "%02d:%02d".format(mins, secs)
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            viewModel.confirmTempPhoto()
        }
    }

    fun launchCamera() {
        val file = File(context.externalCacheDir, "sup_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        viewModel.setTempPhoto(uri, file)
        cameraLauncher.launch(uri)
        // Note: Actual logic depends on how ViewModel handles addSupervisionPhoto. 
        // I'll update ViewModel later if needed, but for now assuming it works with a temp state.
    }

    var carPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var carPhotoFile by remember { mutableStateOf<File?>(null) }

    val carCameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (!success) {
            carPhotoUri = null
            carPhotoFile = null
        }
    }

    fun launchCarCamera() {
        val file = File(context.externalCacheDir, "sup_car_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        carPhotoFile = file
        carPhotoUri = uri
        carCameraLauncher.launch(uri)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
            
            // Premium Header with Timer
            AnimatedVisibility(visible = isSupervisionStarted) {
                Surface(
                    modifier = Modifier.padding(bottom = 20.dp).fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = DeepAzure,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Supervisión en curso", color = Color.White.copy(0.7f), fontSize = 12.sp)
                            Text(selectedTecnico?.nombre ?: "---", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Surface(
                            color = Color.White.copy(0.15f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Timer, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(formatTime(elapsedTime), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        IconButton(
                            onClick = { viewModel.cancelSupervision(supervisorId) },
                            colors = IconButtonDefaults.iconButtonColors(containerColor = ErrorRed.copy(0.2f))
                        ) {
                            Icon(Icons.Default.Stop, null, tint = ErrorRed)
                        }
                    }
                }
            }

            if (!isSupervisionStarted) {
                Text("Configuración de Inicio", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DeepAzure)
                Spacer(modifier = Modifier.height(16.dp))
                
                SectionCard(title = "Detalles de la Visita", icon = Icons.Default.Assignment) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Custom Dropdown for Technician Selection
                        var expanded by remember { mutableStateOf(false) }
                        Column {
                            Text("Técnico a supervisar", fontSize = 12.sp, color = TextLight, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                modifier = Modifier.fillMaxWidth().height(56.dp).clickable { expanded = true },
                                shape = RoundedCornerShape(16.dp),
                                color = SoftWhite,
                                border = BorderStroke(1.dp, LightAzure)
                            ) {
                                Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Person, null, tint = SoftAzure)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        selectedTecnico?.nombre ?: "Seleccionar técnico...",
                                        color = if (selectedTecnico != null) TextDark else TextLight,
                                        fontSize = 15.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(Icons.Default.ArrowDropDown, null, tint = SoftGray)
                                }
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.fillMaxWidth(0.8f).background(Color.White)
                            ) {
                                myTeam.forEach { tecnico ->
                                    DropdownMenuItem(
                                        text = { Text(tecnico.nombre, color = TextDark) },
                                        onClick = {
                                            viewModel.setSupervisionTecnico(tecnico)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Custom SOT Field
                        Column {
                            Text("SOT de referencia", fontSize = 12.sp, color = TextLight, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = sot,
                                onValueChange = { newValue ->
                                    if (newValue.all { it.isDigit() }) {
                                        viewModel.setSupervisionSot(newValue)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                placeholder = { Text("Ej: 1234567") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SoftAzure,
                                    unfocusedBorderColor = LightAzure,
                                    focusedContainerColor = Color.White
                                ),
                                textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        if (selectedTecnico == null || sot.isEmpty()) {
                            scope.launch { snackbarHostState.showSnackbar("Completa los datos iniciales") }
                            return@Button
                        }
                        viewModel.startSupervision(supervisorId, selectedTecnico!!, sot) { success, msg ->
                            if (!success) scope.launch { snackbarHostState.showSnackbar(msg) }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SoftAzure),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Icon(Icons.Default.FlashOn, null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("INICIAR SUPERVISIÓN", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                }
            } else {
                // Paso 2: El resto de los campos
                val respuestas by viewModel.supervisionRespuestas.collectAsState()
                val datosSot by viewModel.supervisionDatosSot.collectAsState()

                // 1. Datos Generales
                SectionCard(title = "Estado y Cuadrilla", icon = Icons.Default.Info) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(modifier = Modifier.weight(1f)) {
                                SupervisionDropdown(
                                    label = "Tipo Cuadrilla",
                                    options = SupervisionConstants.CUADRILLA,
                                    selected = datosSot["cuadrilla"] ?: "Simple",
                                    onSelect = { viewModel.updateSupervisionDatoSot("cuadrilla", it) }
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                Column {
                                    Text("Placa Vehículo", fontSize = 12.sp, color = TextLight, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    OutlinedTextField(
                                        value = datosSot["placa"] ?: "",
                                        onValueChange = { viewModel.updateSupervisionDatoSot("placa", it) },
                                        modifier = Modifier.fillMaxWidth().height(52.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        textStyle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SoftAzure, unfocusedBorderColor = LightAzure)
                                    )
                                }
                            }
                        }

                        // Photo Card for Vehicle
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = SoftWhite,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, LightAzure.copy(0.5f))
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White)
                                        .clickable { launchCarCamera() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (carPhotoUri != null) {
                                        AsyncImage(
                                            model = carPhotoFile, 
                                            contentDescription = null, 
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(Icons.Default.AddAPhoto, null, tint = SoftAzure)
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Evidencia Vehículo", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(if (carPhotoUri != null) "Foto capturada" else "Capturar foto frontal", fontSize = 12.sp, color = TextLight)
                                }
                                if (carPhotoUri != null) {
                                    IconButton(onClick = { carPhotoUri = null }) {
                                        Icon(Icons.Default.Refresh, null, tint = SoftGray)
                                    }
                                }
                            }
                        }

                        if (datosSot["cuadrilla"] == "Doble") {
                            OutlinedTextField(
                                value = datosSot["nombre_segundo_tecnico"] ?: "",
                                onValueChange = { viewModel.updateSupervisionDatoSot("nombre_segundo_tecnico", it) },
                                label = { Text("Nombre Apoyo / Segundo Técnico") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        HorizontalDivider(color = LightAzure.copy(0.5f))

                        SupervisionDropdown(
                            label = "Tecnología",
                            options = SupervisionConstants.TECNOLOGIA,
                            selected = datosSot["tecnologia"] ?: "",
                            onSelect = { viewModel.updateSupervisionDatoSot("tecnologia", it) }
                        )
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                             Box(modifier = Modifier.weight(1f)) {
                                SupervisionDropdown(
                                    label = "Estado Material",
                                    options = SupervisionConstants.MATERIAL_ESTADO,
                                    selected = datosSot["material_estado"] ?: "",
                                    onSelect = { viewModel.updateSupervisionDatoSot("material_estado", it) }
                                )
                             }
                             Box(modifier = Modifier.weight(1f)) {
                                SupervisionDropdown(
                                    label = "Llegada Sup.",
                                    options = SupervisionConstants.HORA_LLEGADA,
                                    selected = datosSot["hora_llegada_supervisor"] ?: "",
                                    onSelect = { viewModel.updateSupervisionDatoSot("hora_llegada_supervisor", it) }
                                )
                             }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Checklist Sections with Progressive Disclosure
                SupervisionConstants.SECCIONES.forEach { (titulo, preguntas) ->
                    SectionCard(title = titulo, icon = Icons.Default.Rule) {
                        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            preguntas.forEach { pregunta ->
                                QuestionItem(
                                    question = pregunta.text,
                                    selectedOption = respuestas[pregunta.id] ?: "",
                                    onOptionSelected = { viewModel.setSupervisionAnswer(pregunta.id, it) }
                                )
                                
                                if (pregunta.id == "rf_check" && respuestas[pregunta.id] == "no") {
                                    OutlinedTextField(
                                        value = datosSot["rf_motivo"] ?: "",
                                        onValueChange = { viewModel.updateSupervisionDatoSot("rf_motivo", it) },
                                        placeholder = { Text("Describa el motivo de falta de RF...") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Final Evaluation
                SectionCard(title = "Productividad y NPS", icon = Icons.Default.Analytics, headerColor = SuccessGreen) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        SupervisionDropdown(
                            label = "¿Es productivo?",
                            options = SupervisionConstants.PRODUCTIVIDAD,
                            selected = datosSot["productividad"] ?: "",
                            onSelect = { viewModel.updateSupervisionDatoSot("productividad", it) }
                        )
                        
                        Column {
                            Text("Evaluación NPS del Cliente", fontSize = 12.sp, color = TextLight, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(12.dp))
                            Slider(
                                value = (datosSot["calificacion_tecnico"]?.toFloatOrNull() ?: 5f),
                                onValueChange = { viewModel.updateSupervisionDatoSot("calificacion_tecnico", it.toInt().toString()) },
                                valueRange = 1f..10f,
                                steps = 8,
                                colors = SliderDefaults.colors(thumbColor = SoftAzure, activeTrackColor = SoftAzure)
                            )
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    color = SoftAzure.copy(0.1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        "Puntaje: ${datosSot["calificacion_tecnico"] ?: "5"}/10", 
                                        fontWeight = FontWeight.ExtraBold, 
                                        color = SoftAzure,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                SectionCard(title = "Sugerencias y Cierre", icon = Icons.Default.ChatBubbleOutline, headerColor = WarningOrange) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = datosSot["sugerencia"] ?: "",
                            onValueChange = { viewModel.updateSupervisionDatoSot("sugerencia", it) },
                            placeholder = { Text("Escribe sugerencias de mejora para el técnico...") },
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                            shape = RoundedCornerShape(16.dp)
                        )
                        OutlinedTextField(
                            value = comentarios,
                            onValueChange = { comentarios = it },
                            placeholder = { Text("Observaciones finales de la supervisión...") },
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Photos Grid
                Text("Evidencias Adicionales", fontWeight = FontWeight.Bold, color = DeepAzure)
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(capturedPhotos) { photo ->
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .border(1.dp, LightAzure, RoundedCornerShape(20.dp))
                        ) {
                            AsyncImage(model = photo.first, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            IconButton(
                                onClick = { viewModel.removeSupervisionPhoto(photo) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(28.dp)
                                    .background(Color.Black.copy(0.4f), CircleShape)
                            ) {
                                Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                    item {
                        Surface(
                            modifier = Modifier.size(110.dp).clickable { launchCamera() },
                            shape = RoundedCornerShape(20.dp),
                            color = SoftWhite,
                            border = BorderStroke(1.dp, LightAzure)
                        ) {
                            Column(
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.CameraAlt, null, tint = SoftAzure)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Añadir", fontSize = 12.sp, color = SoftAzure, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                // Final Button
                Button(
                    onClick = {
                        val allPhotos = capturedPhotos.toMutableList()
                        if (carPhotoUri != null && carPhotoFile != null) {
                            allPhotos.add(carPhotoUri!! to carPhotoFile!!)
                        }
                        
                        if (allPhotos.isEmpty()) {
                            scope.launch { snackbarHostState.showSnackbar("Debe capturar al menos una foto de evidencia.") }
                            return@Button
                        }
                        
                        showConfirmDialog = true
                    },
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                ) {
                    Text("FINALIZAR Y ENVIAR", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                }

                if (showConfirmDialog) {
                    AlertDialog(
                        onDismissRequest = { showConfirmDialog = false },
                        title = { Text("Finalizar Supervisión", fontWeight = FontWeight.Bold, color = DeepAzure) },
                        text = { Text("¿Está seguro de finalizar esta supervisión? Una vez enviada, no podrá ser modificada.") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showConfirmDialog = false
                                    scope.launch {
                                        try {
                                            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                                            val location = try { fusedLocationClient.lastLocation.await() } catch(e: Exception) { null }
                                            
                                            val allPhotos = capturedPhotos.toMutableList()
                                            if (carPhotoUri != null && carPhotoFile != null) {
                                                allPhotos.add(carPhotoUri!! to carPhotoFile!!)
                                            }
                                            val filesList = allPhotos.map { it.second }

                                            val checklistJson = JSONObject().apply {
                                                put("datos_sot", JSONObject(datosSot))
                                                put("respuestas", JSONObject(respuestas))
                                                put("duracion_segundos", elapsedTime)
                                            }.toString()

                                            viewModel.submitSupervisionBackground(
                                                idSupervisor = supervisorId,
                                                idTecnico = selectedTecnico!!.id,
                                                sot = sot,
                                                comentarios = comentarios,
                                                lat = location?.latitude?.toString() ?: "0.0",
                                                lng = location?.longitude?.toString() ?: "0.0",
                                                puntuacion = 0,
                                                checklist = checklistJson,
                                                photos = filesList
                                            )

                                            snackbarHostState.showSnackbar("Subiendo supervisión...")
                                            viewModel.resetSupervision()
                                            comentarios = ""
                                            carPhotoUri = null
                                            carPhotoFile = null
                                        } catch (e: Exception) {
                                            snackbarHostState.showSnackbar("Error al procesar")
                                        }
                                    }
                                }
                            ) {
                                Text("Sí, Finalizar", color = SuccessGreen, fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showConfirmDialog = false }) {
                                Text("Cancelar", color = ErrorRed)
                            }
                        },
                        containerColor = Color.White,
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(60.dp))
        }
        
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
fun SectionCard(
    title: String,
    icon: ImageVector,
    headerColor: Color = DeepAzure,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, LightAzure.copy(0.3f))
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerColor.copy(0.05f))
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(headerColor.copy(0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = headerColor, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(title, fontWeight = FontWeight.ExtraBold, color = headerColor, fontSize = 15.sp)
            }
            Box(modifier = Modifier.padding(20.dp)) {
                content()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupervisionDropdown(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, fontSize = 12.sp, color = TextLight, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = selected,
                onValueChange = {},
                readOnly = true,
                placeholder = { Text("Seleccionar...", fontSize = 13.sp) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                textStyle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SoftAzure,
                    unfocusedBorderColor = LightAzure,
                    focusedContainerColor = Color.White
                )
            )
            ExposedDropdownMenu(
                expanded = expanded, 
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(Color.White)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option, fontSize = 13.sp, color = TextDark) },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun QuestionItem(
    question: String,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(question, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDark, lineHeight = 20.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SupervisionConstants.RESPUESTAS.forEach { option ->
                val isSelected = selectedOption == option
                val color = when(option.lowercase()) {
                    "si" -> SuccessGreen
                    "no" -> ErrorRed
                    else -> WarningOrange
                }
                
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clickable { onOptionSelected(option) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) color else color.copy(0.05f),
                    border = BorderStroke(1.dp, if (isSelected) color else color.copy(0.2f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            option.uppercase(), 
                            color = if (isSelected) Color.White else color,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
