package com.example.appmovil.utils

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.appmovil.BuildConfig
import com.example.appmovil.data.remote.RetrofitClient
import com.example.appmovil.data.remote.UpdateInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Gestor de actualizaciones internas para la aplicación
 */
class AppUpdateManager(private val context: Context) {

    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private var downloadId: Long = -1

    /**
     * Verifica si existe una nueva versión
     */
    fun checkForUpdates(onUpdateAvailable: (UpdateInfo) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.solicitudApi.checkUpdate()
                if (response.isSuccessful) {
                    val info = response.body()
                    if (info != null && info.versionCode > BuildConfig.VERSION_CODE) {
                        withContext(Dispatchers.Main) {
                            onUpdateAvailable(info)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Inicia la descarga del APK
     */
    fun downloadAndInstall(apkUrl: String) {
        // Borrar archivo previo si existe para evitar conflictos/corrupción
        try {
            val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "appmovil-update.apk")
            if (file.exists()) file.delete()
        } catch (e: Exception) { e.printStackTrace() }

        val uri = Uri.parse(apkUrl)
        val request = DownloadManager.Request(uri)
            .setTitle("Actualizando APPMOVIL")
            .setDescription("Descargando nueva versión...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "appmovil-update.apk")
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            request.setRequiresCharging(false)
            request.setRequiresDeviceIdle(false)
        }

        downloadId = downloadManager.enqueue(request)
        
        // Registrar receptor para cuando termine la descarga (Compatibilidad con Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(
                onDownloadComplete,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                Context.RECEIVER_EXPORTED
            )
        } else {
            context.registerReceiver(
                onDownloadComplete,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            )
        }
        
        Toast.makeText(context, "Iniciando descarga...", Toast.LENGTH_SHORT).show()
    }

    private val onDownloadComplete = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (id == downloadId) {
                installApk()
                context.unregisterReceiver(this)
            }
        }
    }

    /**
     * Lanza el instalador de Android usando la URI proveída por DownloadManager
     */
    private fun installApk() {
        try {
            val downloadUri = downloadManager.getUriForDownloadedFile(downloadId)
            if (downloadUri != null) {
                val installIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(downloadUri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(installIntent)
            } else {
                installApkManual()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            installApkManual()
        }
    }

    private fun installApkManual() {
        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "appmovil-update.apk")
        if (file.exists()) {
            val fileUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(fileUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(installIntent)
        } else {
            Toast.makeText(context, "Error: No se pudo localizar el archivo de actualización", Toast.LENGTH_LONG).show()
        }
    }
}
