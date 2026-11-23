package com.example.appmovil.ui.login

import androidx.compose.animation.*
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.appmovil.ui.theme.*
import com.example.appmovil.ui.components.ElegantTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onBackToLogin: () -> Unit,
    onRegisterSuccess: () -> Unit,
    viewModel: RegisterViewModel = viewModel()
) {
    val name by viewModel.name.collectAsState()
    val phone by viewModel.phone.collectAsState()
    val password by viewModel.password.collectAsState()
    val confirmPassword by viewModel.confirmPassword.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    
    val scrollState = rememberScrollState()

    LaunchedEffect(uiState) {
        if (uiState is RegisterUiState.Success) {
            onRegisterSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SoftWhite),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Logo o Icono de Registro
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(DeepAzure.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.PersonAdd,
                    contentDescription = "Register Icon",
                    modifier = Modifier.size(40.dp),
                    tint = DeepAzure
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Crea una cuenta",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
                    letterSpacing = 0.5.sp
                )
            )

            Text(
                text = "Únete a nosotros en este viaje",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = TextLight,
                    letterSpacing = 0.2.sp
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Card del Formulario
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, LightAzure)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth()
                ) {
                    // Campo Nombre
                    ElegantTextField(
                        value = name,
                        onValueChange = viewModel::onNameChange,
                        label = "Nombre Completo",
                        icon = Icons.Outlined.Badge,
                        enabled = uiState !is RegisterUiState.Loading
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Campo DNI (Nuevo)
                    val dni by viewModel.dni.collectAsState()
                    ElegantTextField(
                        value = dni,
                        onValueChange = viewModel::onDniChange,
                        label = "DNI / Documento de Identidad",
                        icon = Icons.Outlined.PermIdentity,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        enabled = uiState !is RegisterUiState.Loading
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Campo Número de Celular
                    ElegantTextField(
                        value = phone,
                        onValueChange = viewModel::onPhoneChange,
                        label = "Número de Celular",
                        icon = Icons.Outlined.PhoneAndroid,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        enabled = uiState !is RegisterUiState.Loading
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Campo Password
                    ElegantTextField(
                        value = password,
                        onValueChange = viewModel::onPasswordChange,
                        label = "Contraseña",
                        icon = Icons.Outlined.Lock,
                        isPassword = true,
                        passwordVisible = passwordVisible,
                        onPasswordToggle = { passwordVisible = !passwordVisible },
                        enabled = uiState !is RegisterUiState.Loading
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Confirmar Password
                    ElegantTextField(
                        value = confirmPassword,
                        onValueChange = viewModel::onConfirmPasswordChange,
                        label = "Confirmar Contraseña",
                        icon = Icons.Outlined.LockClock,
                        isPassword = true,
                        passwordVisible = confirmPasswordVisible,
                        onPasswordToggle = { confirmPasswordVisible = !confirmPasswordVisible },
                        enabled = uiState !is RegisterUiState.Loading
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Selector de Rol (Nuevo)
                    Text(
                        text = "Rol del Usuario",
                        style = MaterialTheme.typography.labelLarge.copy(color = TextLight),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    var expanded by remember { mutableStateOf(false) }
                    val roles = listOf("Usuario", "Supervisor", "Satelite", "Auxiliar", "Tecnico")
                    val currentRol by viewModel.rol.collectAsState()

                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { if (uiState !is RegisterUiState.Loading) expanded = !expanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = currentRol.replaceFirstChar { it.uppercase() },
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SoftAzure,
                                unfocusedBorderColor = LightAzure,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = SoftWhite
                            ),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Groups,
                                    contentDescription = null,
                                    tint = SoftAzure
                                )
                            }
                        )

                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            roles.forEach { roleOption ->
                                DropdownMenuItem(
                                    text = { Text(roleOption, color = TextDark) },
                                    onClick = {
                                        viewModel.onRolChange(roleOption)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Selector de Zona (Nuevo)
                    AnimatedVisibility(visible = currentRol.lowercase() in listOf("satelite", "auxiliar", "tecnico")) {
                        Column {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Zona de Trabajo (Opcional)",
                                style = MaterialTheme.typography.labelLarge.copy(color = TextLight),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            var zonaExpanded by remember { mutableStateOf(false) }
                            val zonas by viewModel.zones.collectAsState()
                            val currentZona by viewModel.zona.collectAsState()

                            ExposedDropdownMenuBox(
                                expanded = zonaExpanded,
                                onExpandedChange = { if (uiState !is RegisterUiState.Loading) zonaExpanded = !zonaExpanded },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = currentZona,
                                    onValueChange = {},
                                    readOnly = true,
                                    placeholder = { Text("Selecciona un distrito") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = zonaExpanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = SoftAzure,
                                        unfocusedBorderColor = LightAzure,
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = SoftWhite
                                    ),
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = SoftAzure
                                        )
                                    }
                                )

                                ExposedDropdownMenu(
                                    expanded = zonaExpanded,
                                    onDismissRequest = { zonaExpanded = false },
                                    modifier = Modifier.background(Color.White)
                                ) {
                                    zonas.forEach { zonaOption ->
                                        DropdownMenuItem(
                                            text = { Text(zonaOption, color = TextDark) },
                                            onClick = {
                                                viewModel.onZonaChange(zonaOption)
                                                zonaExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Boton Registro
                    Button(
                        onClick = viewModel::register,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = uiState !is RegisterUiState.Loading,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SoftAzure,
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        if (uiState is RegisterUiState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                "Crear Cuenta",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }

            // Mensajes de Error
            AnimatedVisibility(
                visible = uiState is RegisterUiState.Error,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                if (uiState is RegisterUiState.Error) {
                    Card(
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = (uiState as RegisterUiState.Error).message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Footer
            Row(
                modifier = Modifier.padding(bottom = 32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("¿Ya tienes una cuenta?", color = TextLight, style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = onBackToLogin) {
                    Text("Inicia Sesión", color = SoftAzure, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        // Boton Flotante para Volver al Login (Opcional pero elegante)
        IconButton(
            onClick = onBackToLogin,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 48.dp, start = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = TextDark
            )
        }
    }
}

@Preview(showBackground = true, device = "id:pixel_7")
@Composable
fun RegisterScreenPreview() {
    APPMOVILTheme {
        RegisterScreen(onBackToLogin = {}, onRegisterSuccess = {})
    }
}
