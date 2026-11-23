package com.example.appmovil.data.repository

import com.example.appmovil.data.remote.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class AuthResult {
    data class Success(
        val message: String, 
        val userId: Int?, 
        val userName: String?, 
        val rol: String? = null,
        val zona: String? = null,
        val sessionToken: String? = null
    ) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

class AuthRepository(private val api: AuthApi = RetrofitClient.authApi) {

    suspend fun login(request: LoginRequest): AuthResult = withContext(Dispatchers.IO) {
        try {
            val response = api.login(request)
            if (response.isSuccessful && response.body()?.status == "success") {
                // Capturamos el campo 'id' y 'nombre' desde la respuesta del servidor
                AuthResult.Success(
                    message = response.body()?.message ?: "Éxito", 
                    userId = response.body()?.id,
                    userName = response.body()?.nombre,
                    rol = response.body()?.rol,
                    zona = response.body()?.zona,
                    sessionToken = response.body()?.sessionToken
                )
            } else {
                val errorMsg = response.body()?.message ?: "Usuario o contraseña incorrectos"
                AuthResult.Error(errorMsg)
            }
        } catch (e: Exception) {
            AuthResult.Error("Error de red: ${e.localizedMessage}")
        }
    }

    suspend fun register(request: RegisterRequest): AuthResult = withContext(Dispatchers.IO) {
        try {
            val response = api.register(request)
            if (response.isSuccessful) {
                AuthResult.Success(response.body()?.message ?: "Registro exitoso", null, null)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Error desconocido"
                AuthResult.Error("Servidor (${response.code()}): $errorMsg")
            }
        } catch (e: Exception) {
            AuthResult.Error("Error de red: ${e.localizedMessage}")
        }
    }

    suspend fun updateSatelliteStatus(userId: Int, estado: String): AuthResult = withContext(Dispatchers.IO) {
        try {
            val response = api.updateStatus(UpdateUserStatusRequest(userId, estado))
            if (response.isSuccessful) {
                AuthResult.Success(response.body()?.message ?: "Estado actualizado", null, null)
            } else {
                AuthResult.Error(response.errorBody()?.string() ?: "Error")
            }
        } catch (e: Exception) {
            AuthResult.Error(e.localizedMessage ?: "Error de red")
        }
    }

    suspend fun resetLunchStatus(userId: Int): AuthResult = withContext(Dispatchers.IO) {
        try {
            val response = api.resetLunchStatus(mapOf("userId" to userId))
            if (response.isSuccessful) {
                AuthResult.Success(response.body()?.message ?: "Almuerzo habilitado de nuevo", null, null)
            } else {
                AuthResult.Error(response.errorBody()?.string() ?: "Error")
            }
        } catch (e: Exception) {
            AuthResult.Error(e.localizedMessage ?: "Error de red")
        }
    }

    suspend fun getSatellitesStatus(): List<SatelliteStatusItem> = withContext(Dispatchers.IO) {
        try {
            val response = api.getSatellitesStatus()
            if (response.isSuccessful) response.body() ?: emptyList() else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // --- NUEVOS MÉTODOS DE CHAT ---
    suspend fun getContacts(miId: Int, rol: String): List<UserContact> = withContext(Dispatchers.IO) {
        try {
            val response = api.getContacts(miId, rol)
            if (response.isSuccessful) response.body() ?: emptyList() else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun markMessagesRead(miId: Int, otroId: Int) = withContext(Dispatchers.IO) {
        try {
            api.markMessagesRead(miId, otroId)
        } catch (e: Exception) { }
    }

    suspend fun getMessages(miId: Int, otroId: Int): List<MessageItem> = withContext(Dispatchers.IO) {
        try {
            val response = api.getMessages(miId, otroId)
            if (response.isSuccessful) response.body() ?: emptyList() else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun sendPrivateMessage(emisor: Int, receptor: Int, texto: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = api.sendMessage(emisor, receptor, texto)
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    suspend fun updateUserZone(userId: Int, zona: String): AuthResult = withContext(Dispatchers.IO) {
        try {
            val response = api.updateUserZone(UpdateUserZoneRequest(userId, zona))
            if (response.isSuccessful) {
                AuthResult.Success(response.body()?.message ?: "Zona actualizada", null, null)
            } else {
                AuthResult.Error(response.errorBody()?.string() ?: "Error")
            }
        } catch (e: Exception) {
            AuthResult.Error(e.localizedMessage ?: "Error de red")
        }
    }

    suspend fun getUserInfo(userId: Int): UserProfile? = withContext(Dispatchers.IO) {
        try {
            val response = api.getUserInfo(userId)
            if (response.isSuccessful) response.body()?.data else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun updateProfile(profile: UserProfile): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = api.updateProfile(profile)
            response.isSuccessful && response.body()?.status == "success"
        } catch (e: Exception) { false }
    }

    suspend fun changePassword(id: Int, current: String, new: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val request = ChangePasswordRequest(id, current, new)
            val response = api.changePassword(request)
            if (response.isSuccessful) {
                val ok = response.body()?.status == "success"
                val msg = response.body()?.message ?: (if(ok) "Éxito" else "Error desconocido")
                Pair(ok, msg)
            } else Pair(false, "Error de red")
        } catch (e: Exception) { Pair(false, e.message ?: "Error critico") }
    }

    suspend fun markAttendance(userId: Int, evento: String) = withContext(Dispatchers.IO) {
        try {
            api.markAttendance(userId, evento)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getZones(): List<String> = withContext(Dispatchers.IO) {
        try {
            val response = api.getZones()
            if (response.isSuccessful) response.body() ?: emptyList() else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getAttendance(fecha: String? = null): List<AttendanceItem> = withContext(Dispatchers.IO) {
        try {
            val response = api.getAttendance(fecha)
            if (response.isSuccessful) response.body() ?: emptyList() else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
