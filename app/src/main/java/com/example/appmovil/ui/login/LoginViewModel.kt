package com.example.appmovil.ui.login

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appmovil.data.remote.LoginRequest
import com.example.appmovil.data.repository.AuthRepository
import com.example.appmovil.data.repository.AuthResult
import com.example.appmovil.utils.DeveloperOptionsChecker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    data class Success(
        val message: String,
        val userId: Int?, 
        val userName: String?, 
        val rol: String? = null,
        val zona: String? = null,
        val sessionToken: String? = null,
        val rememberMe: Boolean = false
    ) : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

class LoginViewModel(
    private val context: Context? = null,
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _phone = MutableStateFlow("")
    val phone: StateFlow<String> = _phone.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _rememberMe = MutableStateFlow(true)
    val rememberMe: StateFlow<Boolean> = _rememberMe.asStateFlow()

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onPhoneChange(newValue: String) {
        _phone.value = newValue
    }

    fun onPasswordChange(newValue: String) {
        _password.value = newValue
    }

    fun onRememberMeChange(newValue: Boolean) {
        _rememberMe.value = newValue
    }

    fun login() {
        // --- SEGURIDAD: Bloquear si Opciones de Desarrollador están activas ---
        if (context != null && DeveloperOptionsChecker.isDeveloperOptionsEnabled(context)) {
            _uiState.value = LoginUiState.Error(
                "⚠️ Acceso denegado: Desactiva las Opciones de Desarrollador para usar la aplicación."
            )
            return
        }

        if (_phone.value.isEmpty() || _password.value.isEmpty()) {
            _uiState.value = LoginUiState.Error("Por favor, completa todos los campos")
            return
        }

        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            val result = repository.login(LoginRequest(_phone.value, _password.value))
            
            _uiState.value = when (result) {
                is AuthResult.Success -> {
                    LoginUiState.Success(
                        message = result.message,
                        userId = result.userId, 
                        userName = result.userName, 
                        rol = result.rol,
                        zona = result.zona,
                        sessionToken = result.sessionToken,
                        rememberMe = _rememberMe.value
                    )
                }
                is AuthResult.Error -> LoginUiState.Error(result.message)
            }
        }
    }
}
