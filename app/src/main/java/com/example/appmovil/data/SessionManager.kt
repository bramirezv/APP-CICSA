package com.example.appmovil.data

import android.content.Context
import android.content.SharedPreferences
import com.example.appmovil.data.remote.AuthResponse

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)

    fun saveSession(userId: Int, nombre: String, rol: String, zona: String, sessionToken: String? = null) {
        prefs.edit().apply {
            putInt("userId", userId)
            putString("nombre", nombre)
            putString("rol", rol)
            putString("zona", zona)
            putString("sessionToken", sessionToken)
            putBoolean("isLoggedIn", true)
            apply()
        }
    }

    fun getSession(): AuthResponse? {
        val isLoggedIn = prefs.getBoolean("isLoggedIn", false)
        if (!isLoggedIn) return null

        return AuthResponse(
            status = "success",
            message = "Session restored",
            id = prefs.getInt("userId", 0),
            nombre = prefs.getString("nombre", ""),
            rol = prefs.getString("rol", "satelite"),
            zona = prefs.getString("zona", "SIN ZONA"),
            sessionToken = prefs.getString("sessionToken", null)
        )
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
