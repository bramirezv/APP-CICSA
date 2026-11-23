package com.example.appmovil.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.appmovil.data.remote.UserProfile
import com.example.appmovil.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userId: Int,
    onBack: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showEditDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(userId) {
        viewModel.loadProfile(userId)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Mi Perfil", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (uiState is ProfileUiState.Success) {
                        IconButton(onClick = { showEditDialog = true }) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Editar Perfil", tint = SoftAzure)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = DeepAzure,
                    navigationIconContentColor = SoftAzure
                )
            )
        },
        containerColor = BackgroundLight
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val state = uiState) {
                is ProfileUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = SoftAzure
                    )
                }
                is ProfileUiState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Outlined.ErrorOutline, null, tint = ErrorRed, modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(16.dp))
                        Text(state.message, color = TextDark, modifier = Modifier.padding(horizontal = 32.dp))
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadProfile(userId) }, colors = ButtonDefaults.buttonColors(containerColor = SoftAzure)) {
                            Text("Reintentar")
                        }
                    }
                }
                is ProfileUiState.Success -> {
                    ProfileContent(
                        profile = state.profile,
                        onChangePassword = { showPasswordDialog = true }
                    )

                    if (showEditDialog) {
                        EditProfileDialog(
                            profile = state.profile,
                            onDismiss = { showEditDialog = false },
                            onConfirm = { updatedProfile ->
                                viewModel.updateProfile(updatedProfile) { success ->
                                    if (success) {
                                        showEditDialog = false
                                        scope.launch { snackbarHostState.showSnackbar("Perfil actualizado correctamente") }
                                    } else {
                                        scope.launch { snackbarHostState.showSnackbar("Error al actualizar perfil") }
                                    }
                                }
                            }
                        )
                    }

                    if (showPasswordDialog) {
                        ChangePasswordDialog(
                            onDismiss = { showPasswordDialog = false },
                            onConfirm = { current, new ->
                                viewModel.changePassword(userId, current, new) { success, message ->
                                    scope.launch { snackbarHostState.showSnackbar(message) }
                                    if (success) showPasswordDialog = false
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileContent(profile: UserProfile, onChangePassword: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Cabecera con Avatar
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(SoftAzure, DeepAzure))),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = profile.nombre.take(1).uppercase(),
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(profile.nombre, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextDark)
        Text(profile.rol.uppercase(), fontSize = 13.sp, color = SoftAzure, fontWeight = FontWeight.SemiBold)

        Spacer(Modifier.height(24.dp))

        // Tarjeta de Información de Contacto
        InfoSectionCard(
            title = "Información Personal",
            items = listOf(
                InfoItem("Nombre Completo", profile.nombre, Icons.Outlined.Person),
                InfoItem("Teléfono Celular", profile.telefono ?: "No registrado", Icons.Outlined.Phone),
                InfoItem("Correo Electrónico", profile.email ?: "No registrado", Icons.Outlined.Email),
                InfoItem("Dirección Actual", profile.direccion ?: "No registrada", Icons.Outlined.LocationOn)
            )
        )

        Spacer(Modifier.height(16.dp))

        // Tarjeta de Datos Adicionales
        InfoSectionCard(
            title = "Datos Adicionales",
            items = listOf(
                InfoItem("Fecha Nacimiento", profile.fechaNacimiento ?: "No registrada", Icons.Outlined.Cake),
                InfoItem("Contacto de Emergencia", profile.telefonoEmergencia ?: "No registrado", Icons.Outlined.ContactPhone),
                InfoItem("Última Conexión", profile.ultimoAcceso ?: "-", Icons.Outlined.AccessTime)
            )
        )

        Spacer(Modifier.height(24.dp))

        // Botón de Cambio de Contraseña
        OutlinedButton(
            onClick = onChangePassword,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = SoftAzure),
            border = androidx.compose.foundation.BorderStroke(1.dp, SoftAzure)
        ) {
            Icon(Icons.Outlined.Lock, null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("CAMBIAR CONTRASEÑA", fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun EditProfileDialog(
    profile: UserProfile,
    onDismiss: () -> Unit,
    onConfirm: (UserProfile) -> Unit
) {
    var nombre by remember { mutableStateOf(profile.nombre) }
    var telefono by remember { mutableStateOf(profile.telefono ?: "") }
    var email by remember { mutableStateOf(profile.email ?: "") }
    var direccion by remember { mutableStateOf(profile.direccion ?: "") }
    var emergencia by remember { mutableStateOf(profile.telefonoEmergencia ?: "") }
    var fechaNac by remember { mutableStateOf(profile.fechaNacimiento ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Información", fontWeight = FontWeight.Bold, color = DeepAzure) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = telefono, onValueChange = { telefono = it }, label = { Text("Teléfono") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = direccion, onValueChange = { direccion = it }, label = { Text("Dirección") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = emergencia, onValueChange = { emergencia = it }, label = { Text("Telf. Emergencia") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = fechaNac, onValueChange = { fechaNac = it }, label = { Text("Fecha Nac. (AAAA-MM-DD)") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(profile.copy(
                    nombre = nombre,
                    telefono = telefono,
                    email = email,
                    direccion = direccion,
                    telefonoEmergencia = emergencia,
                    fechaNacimiento = fechaNac
                ))
            }, colors = ButtonDefaults.buttonColors(containerColor = SoftAzure)) {
                Text("GUARDAR CAMBIOS")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCELAR", color = TextLight) }
        }
    )
}

@Composable
fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var currentPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cambiar Contraseña", fontWeight = FontWeight.Bold, color = DeepAzure) },
        text = {
            Column {
                OutlinedTextField(
                    value = currentPass, 
                    onValueChange = { currentPass = it }, 
                    label = { Text("Contraseña Actual") }, 
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = newPass, 
                    onValueChange = { newPass = it }, 
                    label = { Text("Nueva Contraseña") }, 
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirmPass, 
                    onValueChange = { confirmPass = it }, 
                    label = { Text("Confirmar Nueva Contraseña") }, 
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                if (newPass.isNotEmpty() && confirmPass.isNotEmpty() && newPass != confirmPass) {
                    Text("Las contraseñas no coinciden", color = ErrorRed, style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = currentPass.isNotBlank() && newPass.isNotBlank() && newPass == confirmPass,
                onClick = { onConfirm(currentPass, newPass) },
                colors = ButtonDefaults.buttonColors(containerColor = SoftAzure)
            ) {
                Text("ACTUALIZAR CLAVE")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCELAR", color = TextLight) }
        }
    )
}

@Composable
fun InfoSectionCard(title: String, items: List<InfoItem>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = DeepAzure,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))
            items.forEachIndexed { index, item ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Icon(item.icon, null, tint = SoftAzure, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(item.label, fontSize = 12.sp, color = TextLight)
                        Text(item.value, fontSize = 15.sp, color = TextDark, fontWeight = FontWeight.Medium)
                    }
                }
                if (index < items.size - 1) {
                    HorizontalDivider(color = BackgroundLight, modifier = Modifier.padding(start = 32.dp))
                }
            }
        }
    }
}

data class InfoItem(val label: String, val value: String, val icon: ImageVector)

