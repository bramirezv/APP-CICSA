package com.example.appmovil.ui.solicitudes

import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.gms.location.LocationServices
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.appmovil.ui.components.ElegantTextField
import com.example.appmovil.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TNPSEvidenceScreen(
    userId: Int,
    userName: String,
    viewModel: SolicitudViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onSuccess: () -> Unit = {}
) {
    var sot by remember { mutableStateOf("") }
    var comentario by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var photoFile by remember { mutableStateOf<File?>(null) }
    var capturedLocation by remember { mutableStateOf<Location?>(null) }
    var capturedTime by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }
    
    val supervisores by viewModel.supervisoresList.collectAsState()
    var selectedSupervisor by remember { mutableStateOf<com.example.appmovil.data.remote.UserProfile?>(null) }
    var expandedSupervisor by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Launcher para la Cámara
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            // Capturar GPS y Hora al momento del éxito de la foto
            scope.launch {
                try {
                    val location = fusedLocationClient.lastLocation.await()
                    capturedLocation = location
                    capturedTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else {
            imageUri = null
        }
    }

    // Launcher para Permisos
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        
        if (cameraGranted && locationGranted) {
            // Re-intentar abrir cámara
            val file = createImageFile(context)
            val uri = FileProvider.getUriForFile(context, "com.example.appmovil.fileprovider", file)
            photoFile = file
            imageUri = uri
            cameraLauncher.launch(uri)
        }
    }

    fun checkAndLaunchCamera() {
        val hasCamera = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val hasLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (hasCamera && hasLocation) {
            val file = createImageFile(context)
            val uri = FileProvider.getUriForFile(context, "com.example.appmovil.fileprovider", file)
            photoFile = file
            imageUri = uri
            cameraLauncher.launch(uri)
        } else {
            permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION))
        }
    }

    LaunchedEffect(Unit) {
        checkAndLaunchCamera()
        viewModel.fetchSupervisores()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Evidencia TNPS", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = DeepAzure
                )
            )
        },
        containerColor = BackgroundLight
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Registro de Performance", style = MaterialTheme.typography.titleMedium, color = DeepAzure, fontWeight = FontWeight.Bold)
                    Text("Sube la evidencia de tu trabajo para la medición de TNPS.", fontSize = 12.sp, color = TextLight)
                    
                    Spacer(Modifier.height(20.dp))

                    ElegantTextField(
                        value = sot,
                        onValueChange = { sot = it },
                        label = "Número de SOT / Requerimiento",
                        icon = Icons.Default.Numbers
                    )

                    Spacer(Modifier.height(16.dp))

                    ElegantTextField(
                        value = comentario,
                        onValueChange = { comentario = it },
                        label = "Comentario adicional",
                        icon = Icons.Default.Description,
                        singleLine = false
                    )

                    Spacer(Modifier.height(16.dp))

                    // Dropdown de Supervisor
                    ExposedDropdownMenuBox(
                        expanded = expandedSupervisor,
                        onExpandedChange = { expandedSupervisor = !expandedSupervisor },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedSupervisor?.nombre ?: "Seleccionar Supervisor",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Supervisor Asignado") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSupervisor) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = expandedSupervisor,
                            onDismissRequest = { expandedSupervisor = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            supervisores.forEach { supervisor ->
                                DropdownMenuItem(
                                    text = { Text(supervisor.nombre ?: "Sin Nombre", color = TextDark) },
                                    onClick = {
                                        selectedSupervisor = supervisor
                                        expandedSupervisor = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // Selector de Imagen (Ahora dispara la cámara directamente)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF8F9FA))
                            .border(1.dp, SoftGray.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .clickable { checkAndLaunchCamera() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (imageUri != null) {
                            AsyncImage(
                                model = imageUri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.CameraAlt, null, tint = SoftAzure, modifier = Modifier.size(48.dp))
                                Spacer(Modifier.height(8.dp))
                                Text("Tomar Foto de Evidencia", color = SoftAzure, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Se capturará GPS y Hora automáticamente", fontSize = 11.sp, color = SoftGray)
                            }
                        }
                    }

                    if (capturedTime.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "Capturado: $capturedTime | GPS: ${capturedLocation?.latitude ?: "0.0"}, ${capturedLocation?.longitude ?: "0.0"}",
                                fontSize = 11.sp,
                                color = SuccessGreen
                            )
                        }
                    }

                    Spacer(Modifier.height(32.dp))

                    Button(
                        onClick = {
                            if (imageUri == null || sot.isBlank()) {
                                scope.launch { snackbarHostState.showSnackbar("Por favor toma la foto de evidencia y completa el SOT") }
                                return@Button
                            }
                            
                            isUploading = true
                            scope.launch {
                                try {
                                    // Comprimir y convertir imagen a bytes
                                    val bitmap = BitmapFactory.decodeFile(photoFile?.absolutePath)
                                    val stream = ByteArrayOutputStream()
                                    bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream)
                                    val bytes = stream.toByteArray()
                                    
                                    viewModel.uploadTNPSEvidencia(
                                        idTecnico = userId,
                                        nombreTecnico = userName,
                                        sot = sot,
                                        comentario = comentario,
                                        lat = capturedLocation?.latitude?.toString() ?: "0.0",
                                        lng = capturedLocation?.longitude?.toString() ?: "0.0",
                                        idSupervisor = selectedSupervisor?.id,
                                        nombreSupervisor = selectedSupervisor?.nombre,
                                        bytes = bytes
                                    ) { success, message ->
                                        isUploading = false
                                        if (success) {
                                            scope.launch { snackbarHostState.showSnackbar("Evidencia enviada: $capturedTime") }
                                            // Limpiar campos
                                            sot = ""
                                            comentario = ""
                                            imageUri = null
                                            capturedTime = ""
                                            capturedLocation = null
                                            onSuccess()
                                        } else {
                                            scope.launch { snackbarHostState.showSnackbar("Error: $message") }
                                        }
                                    }
                                } catch (e: Exception) {
                                    isUploading = false
                                    snackbarHostState.showSnackbar("Error al procesar: ${e.message}")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SoftAzure),
                        enabled = !isUploading
                    ) {
                        if (isUploading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Icon(Icons.Default.CloudUpload, null)
                            Spacer(Modifier.width(8.dp))
                            Text("ENVIAR EVIDENCIA", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

private fun createImageFile(context: Context): File {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val storageDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
    return File.createTempFile("TNPS_${timeStamp}_", ".jpg", storageDir)
}
