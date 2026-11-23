package com.example.appmovil.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appmovil.data.remote.RegisterRequest
import com.example.appmovil.data.repository.AuthResult
import com.example.appmovil.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class RegisterUiState {
    object Idle : RegisterUiState()
    object Loading : RegisterUiState()
    object Success : RegisterUiState()
    data class Error(val message: String) : RegisterUiState()
}

class RegisterViewModel : ViewModel() {

    private val repository = AuthRepository()

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _dni = MutableStateFlow("")
    val dni: StateFlow<String> = _dni.asStateFlow()

    private val _phone = MutableStateFlow("")
    val phone: StateFlow<String> = _phone.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _confirmPassword = MutableStateFlow("")
    val confirmPassword: StateFlow<String> = _confirmPassword.asStateFlow()

    private val _rol = MutableStateFlow("usuario")
    val rol: StateFlow<String> = _rol.asStateFlow()

    private val _zona = MutableStateFlow("")
    val zona: StateFlow<String> = _zona.asStateFlow()

    private val _zones = MutableStateFlow<List<String>>(emptyList())
    val zones: StateFlow<List<String>> = _zones.asStateFlow()

    private val _uiState = MutableStateFlow<RegisterUiState>(RegisterUiState.Idle)
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    init {
        loadZones()
    }

    fun loadZones() {
        viewModelScope.launch {
            _zones.value = repository.getZones()
        }
    }

    fun onNameChange(newValue: String) { _name.value = newValue }
    fun onDniChange(newValue: String) { _dni.value = newValue }
    fun onPhoneChange(newValue: String) { _phone.value = newValue }
    fun onPasswordChange(newValue: String) { _password.value = newValue }
    fun onConfirmPasswordChange(newValue: String) { _confirmPassword.value = newValue }
    fun onRolChange(newValue: String) { _rol.value = newValue.lowercase() }
    fun onZonaChange(newValue: String) { _zona.value = newValue }

    fun register() {
        if (_name.value.isBlank() || _dni.value.isBlank() || _phone.value.isBlank() || 
            _password.value.isBlank() || _confirmPassword.value.isBlank()) {
            _uiState.value = RegisterUiState.Error("Por favor, completa todos los campos (Nombre, DNI, Celular y Contraseña).")
            return
        }

        if (_password.value != _confirmPassword.value) {
            _uiState.value = RegisterUiState.Error("Las contraseñas no coinciden.")
            return
        }

        viewModelScope.launch {
            _uiState.value = RegisterUiState.Loading
            
            val result = repository.register(
                RegisterRequest(
                    name = _name.value,
                    dni = _dni.value,
                    phone = _phone.value,
                    password = _password.value,
                    rol = _rol.value,
                    zona = if (_rol.value in listOf("satelite", "auxiliar", "tecnico")) {
                        if (_zona.value.isBlank()) "SIN ZONA" else _zona.value
                    } else null
                )
            )

            _uiState.value = when (result) {
                is AuthResult.Success -> RegisterUiState.Success
                is AuthResult.Error -> RegisterUiState.Error(result.message)
            }
        }
    }

    fun resetState() {
        _uiState.value = RegisterUiState.Idle
    }
}
