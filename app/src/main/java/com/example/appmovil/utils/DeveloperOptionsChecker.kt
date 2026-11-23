package com.example.appmovil.utils

import android.content.Context
import android.provider.Settings

/**
 * Utilidad para detectar si el dispositivo tiene las Opciones de Desarrollador activas.
 * Se usa como medida de seguridad para bloquear el uso de la app en entornos no permitidos.
 */
object DeveloperOptionsChecker {

    /**
     * Devuelve true si las Opciones de Desarrollador están habilitadas en el dispositivo.
     */
    fun isDeveloperOptionsEnabled(context: Context): Boolean {
        return try {
            Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
                0
            ) != 0
        } catch (e: Exception) {
            // Si no se puede leer el setting, asumimos que está habilitado por seguridad
            true
        }
    }
}
