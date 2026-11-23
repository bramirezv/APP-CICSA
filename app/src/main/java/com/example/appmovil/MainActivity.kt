package com.example.appmovil

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.unit.dp
import androidx.compose.material3.*
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.appmovil.ui.login.LoginScreen
import com.example.appmovil.ui.theme.APPMOVILTheme
import com.example.appmovil.data.SessionManager
import com.example.appmovil.data.remote.AuthResponse
import com.example.appmovil.data.remote.UpdateInfo
import com.example.appmovil.utils.AppUpdateManager
import com.example.appmovil.utils.DeveloperOptionsChecker
import kotlinx.coroutines.launch
import androidx.compose.runtime.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeveloperMode
import androidx.compose.material3.Icon

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            APPMOVILTheme {
                val navController = rememberNavController()
                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()
                // Instanciamos el ViewModel aquí para que sea el mismo para todas las pantallas
                val context = androidx.compose.ui.platform.LocalContext.current
                val sessionManager = remember { SessionManager(context) }
                val savedSession = remember { sessionManager.getSession() }
                val solicitudViewModel: com.example.appmovil.ui.solicitudes.SolicitudViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            return com.example.appmovil.ui.solicitudes.SolicitudViewModel(context = context) as T
                        }
                    }
                )
                
                // --- SEGURIDAD: Opciones de Desarrollador ---
                var showDevOptionsDialog by remember {
                    mutableStateOf(DeveloperOptionsChecker.isDeveloperOptionsEnabled(context))
                }

                // Lógica de Actualización
                val updateManager = remember { AppUpdateManager(context) }
                var showUpdateDialog by remember { mutableStateOf<UpdateInfo?>(null) }

                LaunchedEffect(Unit) {
                    updateManager.checkForUpdates { info ->
                        showUpdateDialog = info
                    }
                }

                // Diálogo de bloqueo por Opciones de Desarrollador
                if (showDevOptionsDialog) {
                    AlertDialog(
                        onDismissRequest = { /* No se puede cerrar */ },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.DeveloperMode,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        title = {
                            Text(
                                text = "Acceso Restringido",
                                style = MaterialTheme.typography.titleLarge
                            )
                        },
                        text = {
                            Text(
                                text = "Esta aplicación no puede ejecutarse con las Opciones de Desarrollador activadas.\n\n" +
                                       "Por seguridad, desactiva las Opciones de Desarrollador en:\n" +
                                       "Ajustes → Sistema → Opciones de Desarrollador → Desactivar",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    // Cierra la aplicación completamente
                                    (context as? android.app.Activity)?.finishAffinity()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("Cerrar Aplicación")
                            }
                        }
                    )
                }

                if (showUpdateDialog != null) {
                    AlertDialog(
                        onDismissRequest = { /* No permitir cerrar si es crítica, o simplemente cerrar */ },
                        title = { Text("¡Nueva Versión Disponible!", style = MaterialTheme.typography.titleLarge, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic) },
                        text = {
                            androidx.compose.foundation.layout.Column {
                                Text("Se ha encontrado la versión ${showUpdateDialog?.versionName}. Se recomienda actualizar para obtener las últimas mejoras.")
                                if (!showUpdateDialog?.notes.isNullOrEmpty()) {
                                    Spacer(modifier = Modifier.padding(4.dp))
                                    Text("Novedades:", style = MaterialTheme.typography.labelLarge)
                                    Text(showUpdateDialog?.notes ?: "", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showUpdateDialog?.let { updateManager.downloadAndInstall(it.apkUrl) }
                                    showUpdateDialog = null
                                }
                            ) {
                                Text("Actualizar Ahora")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showUpdateDialog = null }) {
                                Text("Más tarde")
                            }
                        }
                    )
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { innerPadding ->
                    val startDest = if (savedSession != null) {
                        val id = savedSession.id
                        val name = java.net.URLEncoder.encode(savedSession.nombre ?: "Usuario", "UTF-8")
                        val rol = java.net.URLEncoder.encode(savedSession.rol ?: "satelite", "UTF-8")
                        val zona = java.net.URLEncoder.encode(savedSession.zona ?: "SIN ZONA", "UTF-8")
                        "home/$id/$name/$rol/$zona"
                    } else "login"

                    NavHost(
                        navController = navController,
                        startDestination = startDest,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("login") {
                            LoginScreen(
                                onLoginSuccess = { msg, userId, userName, userRol, userZona, sessionToken, rememberMe ->
                                    scope.launch {
                                        val displayId = if (userId > 0) userId.toString() else "ERROR_ID"
                                        val displayName = userName ?: "NULL_NAME"
                                        val debugInfo = "MSG: $msg | ID: $displayId | NAME: $displayName"
                                        snackbarHostState.showSnackbar(debugInfo)
                                    }
                                    
                                    val idParam = userId
                                    val nameParam = java.net.URLEncoder.encode(userName ?: "Usuario", "UTF-8")
                                    val rolParam = java.net.URLEncoder.encode(userRol ?: "satelite", "UTF-8")
                                    val zonaParam = java.net.URLEncoder.encode(userZona ?: "SIN ZONA", "UTF-8")
                                    
                                    navController.navigate("home/$idParam/$nameParam/$rolParam/$zonaParam") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                    
                                    // Guardar sesión si se marcó 'Recordar'
                                    if (rememberMe) {
                                        sessionManager.saveSession(
                                            userId = userId,
                                            nombre = userName ?: "Usuario",
                                            rol = userRol ?: "satelite",
                                            zona = userZona ?: "SIN ZONA",
                                            sessionToken = sessionToken
                                        )
                                    }
                                },
                                onRegisterClick = {
                                    navController.navigate("register")
                                }
                            )
                        }
                        
                        composable("register") {
                            com.example.appmovil.ui.login.RegisterScreen(
                                onBackToLogin = {
                                    navController.popBackStack()
                                },
                                onRegisterSuccess = {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("¡Cuenta creada con éxito!")
                                    }
                                    navController.popBackStack() 
                                }
                            )
                        }

                        composable("home/{userId}/{userName}/{userRol}/{userZona}") { backStackEntry ->
                            val userId = backStackEntry.arguments?.getString("userId")?.toIntOrNull() ?: 0
                            val nameRaw = backStackEntry.arguments?.getString("userName") ?: "Usuario"
                            val rolRaw = backStackEntry.arguments?.getString("userRol") ?: "satelite"
                            val zonaRaw = backStackEntry.arguments?.getString("userZona") ?: "SIN ZONA"
                            
                            val name = java.net.URLDecoder.decode(nameRaw, "UTF-8")
                            val rol = java.net.URLDecoder.decode(rolRaw, "UTF-8")
                            val zona = java.net.URLDecoder.decode(zonaRaw, "UTF-8")
                            
                            com.example.appmovil.ui.home.HomeScreen(
                                userId = userId,
                                userName = name,
                                userRol = rol,
                                userZona = zona,
                                onLogout = {
                                    sessionManager.clearSession()
                                    navController.navigate("login") {
                                        popUpTo("home/$userId/$name/$rol/$zona") { inclusive = true }
                                    }
                                },
                                onRegisterUser = {
                                    navController.navigate("register")
                                },
                                onCreateSolicitud = {
                                    // Al crear nueva, reseteamos para que salga vacío
                                    solicitudViewModel.resetCreateState()
                                    val encodedRol = java.net.URLEncoder.encode(rol, "UTF-8")
                                    navController.navigate("create_solicitud/$name/$encodedRol")
                                },
                                onEditSolicitud = { solicitud ->
                                    // Al editar, cargamos los datos en el ViewModel primero
                                    solicitudViewModel.prepararEdicion(solicitud)
                                    val encodedRol = java.net.URLEncoder.encode(rol, "UTF-8")
                                    navController.navigate("create_solicitud/$name/$encodedRol")
                                },
                                onTrackTicket = { solicitudId, sot, tecnico ->
                                    val encSot = java.net.URLEncoder.encode(sot, "UTF-8")
                                    val encTec = java.net.URLEncoder.encode(tecnico, "UTF-8")
                                    navController.navigate("ticket_tracking/$solicitudId/$encSot/$encTec/true")
                                },
                                solicitudViewModel = solicitudViewModel
                            )
                        }

                        composable("create_solicitud/{userName}/{userRol}") { backStackEntry ->
                            val name = backStackEntry.arguments?.getString("userName") ?: "Usuario"
                            val createRol = java.net.URLDecoder.decode(
                                backStackEntry.arguments?.getString("userRol") ?: "supervisor", "UTF-8"
                            ).trim().lowercase()
                            val tipo = if (createRol.contains("auxiliar")) "auxiliar" else "normal"
                            com.example.appmovil.ui.solicitudes.CreateSolicitudScreen(
                                userName = name,
                                userRol = createRol,
                                viewModel = solicitudViewModel,
                                tipo = tipo,
                                onBack = {
                                    navController.popBackStack()
                                },
                                onTrackTicket = { solId, solSot, solTecnico ->
                                    val encSot = java.net.URLEncoder.encode(solSot, "UTF-8")
                                    val encTec = java.net.URLEncoder.encode(solTecnico, "UTF-8")
                                    navController.navigate("ticket_tracking/$solId/$encSot/$encTec/false") {
                                        // Salimos del CreateSolicitudScreen para que el back vaya a Home
                                        popUpTo("create_solicitud/$name/${java.net.URLEncoder.encode(createRol, "UTF-8")}") {
                                            inclusive = true
                                        }
                                    }
                                }
                            )
                        }

                        composable("ticket_tracking/{solicitudId}/{sot}/{tecnico}/{isAlreadyAccepted}") { backStackEntry ->
                            val solId = backStackEntry.arguments?.getString("solicitudId")?.toIntOrNull() ?: 0
                            val trackSot = java.net.URLDecoder.decode(
                                backStackEntry.arguments?.getString("sot") ?: "", "UTF-8"
                            )
                            val trackTecnico = java.net.URLDecoder.decode(
                                backStackEntry.arguments?.getString("tecnico") ?: "", "UTF-8"
                            )
                            val isAlreadyAccepted = backStackEntry.arguments?.getString("isAlreadyAccepted")?.toBoolean() ?: false
                            val backEntry = navController.previousBackStackEntry
                            val userId2 = backEntry?.arguments?.getString("userId")?.toIntOrNull() ?: 0
                            val name2 = java.net.URLDecoder.decode(
                                backEntry?.arguments?.getString("userName") ?: "Usuario", "UTF-8"
                            )
                            com.example.appmovil.ui.solicitudes.TicketTrackingScreen(
                                solicitudId = solId,
                                sot = trackSot,
                                tecnico = trackTecnico,
                                userName = name2,
                                userId = userId2,
                                viewModel = solicitudViewModel,
                                isAlreadyAccepted = isAlreadyAccepted,
                                onBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}