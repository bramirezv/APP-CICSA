package com.example.appmovil.ui.login

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.appmovil.ui.theme.*
import com.example.appmovil.ui.components.ElegantTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: (String, Int, String?, String?, String?, String?, Boolean) -> Unit,
    onRegisterClick: () -> Unit,
    viewModel: LoginViewModel = run {
        val context = LocalContext.current
        viewModel(factory = object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return LoginViewModel(context = context) as T
            }
        })
    }
) {
    val phone by viewModel.phone.collectAsState()
    val password by viewModel.password.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        if (uiState is LoginUiState.Success) {
            val success = uiState as LoginUiState.Success
            onLoginSuccess(
                success.message,
                success.userId ?: 0, 
                success.userName, 
                success.rol, 
                success.zona,
                success.sessionToken,
                success.rememberMe
            )
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = SoftWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(64.dp))

            // Logo Corporativo Moderno
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(SoftAzure.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Shield,
                    contentDescription = "CICSA Logo",
                    modifier = Modifier.size(40.dp),
                    tint = SoftAzure
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Título de Marca
            Text(
                text = "CICSA MÓVIL",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = DeepAzure,
                    letterSpacing = 2.sp
                )
            )
            
            Text(
                text = "GESTIÓN DE ÚLTIMA MILLA",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = TextLight,
                    letterSpacing = 1.sp
                )
            )

            Spacer(modifier = Modifier.height(56.dp))

            // Formulario Limpio
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = "Identificación de Usuario",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = DeepAzure,
                        fontWeight = FontWeight.Bold
                    )
                )

                ElegantTextField(
                    value = phone,
                    onValueChange = viewModel::onPhoneChange,
                    label = "DNI o Número de Celular",
                    icon = Icons.Outlined.Badge,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    enabled = uiState !is LoginUiState.Loading
                )

                ElegantTextField(
                    value = password,
                    onValueChange = viewModel::onPasswordChange,
                    label = "Contraseña",
                    icon = Icons.Outlined.Lock,
                    isPassword = true,
                    passwordVisible = passwordVisible,
                    onPasswordToggle = { passwordVisible = !passwordVisible },
                    enabled = uiState !is LoginUiState.Loading
                )
                
                val rememberByMe by viewModel.rememberMe.collectAsState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.onRememberMeChange(!rememberByMe) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = rememberByMe,
                        onCheckedChange = { viewModel.onRememberMeChange(it) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = SoftAzure,
                            uncheckedColor = SoftGray
                        )
                    )
                    Text(
                        text = "Recordar sesión",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextDark
                    )
                }
            }



            Spacer(modifier = Modifier.height(32.dp))

            // Botón de Acceso Principal
            Button(
                onClick = viewModel::login,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = uiState !is LoginUiState.Loading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SoftAzure,
                    disabledContainerColor = SoftAzure.copy(alpha = 0.5f)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                if (uiState is LoginUiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        "CONECTAR",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                }
            }

            // Error Message
            AnimatedVisibility(visible = uiState is LoginUiState.Error) {
                if (uiState is LoginUiState.Error) {
                    Text(
                        text = (uiState as LoginUiState.Error).message,
                        color = ErrorRed,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Footer de Marca
            Column(
                modifier = Modifier.padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "© 2026 CICSA - CONTROL LOGÍSTICO",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = SoftGray,
                        letterSpacing = 0.5.sp
                    )
                )
            }
        }
    }
}

@Preview(showBackground = true, device = "id:pixel_7")
@Composable
fun LoginScreenPreview() {
    APPMOVILTheme {
        LoginScreen(onLoginSuccess = { _, _, _, _, _, _, _ -> }, onRegisterClick = {})
    }
}
