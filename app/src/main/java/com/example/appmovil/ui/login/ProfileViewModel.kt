package com.example.appmovil.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appmovil.data.remote.UserProfile
import com.example.appmovil.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ProfileUiState {
    object Loading : ProfileUiState()
    data class Success(val profile: UserProfile) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}

class ProfileViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun loadProfile(userId: Int) {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            println("ProfileViewModel: Cargando perfil para ID: $userId")
            val profile = repository.getUserInfo(userId)
            if (profile != null) {
                _uiState.value = ProfileUiState.Success(profile)
            } else {
                _uiState.value = ProfileUiState.Error("Hubo un problema al conectar con el servidor. Verifica que 'get_user_info.php' esté en la carpeta api y que el ID $userId exista.")
            }
        }
    }

    fun updateProfile(profile: UserProfile, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.updateProfile(profile)
            if (success) {
                // Recargar perfil tras éxito
                loadProfile(profile.id)
            }
            onResult(success)
        }
    }

    fun changePassword(userId: Int, current: String, new: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val (success, message) = repository.changePassword(userId, current, new)
            onResult(success, message)
        }
    }
}
