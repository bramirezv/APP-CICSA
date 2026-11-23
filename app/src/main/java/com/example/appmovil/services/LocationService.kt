package com.example.appmovil.services

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.example.appmovil.MainActivity
import com.example.appmovil.data.remote.RetrofitClient
import com.google.android.gms.location.*
import kotlinx.coroutines.*

/**
 * Servicio de primer plano que rastrea la ubicación del usuario (Satelite/Supervisor)
 * y la reporta al servidor incluso en segundo plano.
 */
class LocationService : Service() {
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    private var userId: Int = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        userId = intent?.getIntExtra("user_id", 0) ?: 0
        
        createNotificationChannel()
        val notification = createNotification()
        startForeground(100, notification)

        if (userId != 0) {
            startLocationUpdates()
        }

        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                "location_channel_v1",
                "Rastreo de Ubicación En Vivo",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Mantiene el rastreo activo para el panel de administración"
                setShowBadge(false)
            }
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 10, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, "location_channel_v1")
            .setContentTitle("CICSA: Rastreo en Tiempo Real")
            .setContentText("Tu ubicación se está compartiendo con el centro de control.")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000L).apply {
            setMinUpdateIntervalMillis(5000L) // Mínimo 5 segundos para máxima exactitud en movimiento
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    sendLocationToServer(location)
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback!!,
            Looper.getMainLooper()
        )
    }

    private fun sendLocationToServer(location: Location) {
        serviceScope.launch {
            try {
                // Usamos retrofit para enviar la ubicación
                val response = RetrofitClient.authApi.updateLocation(
                    userId,
                    location.latitude,
                    location.longitude
                )
                if (response.isSuccessful) {
                    android.util.Log.d("LocationService", "Ubicación enviada: ${location.latitude}, ${location.longitude}")
                }
            } catch (e: Exception) {
                android.util.Log.e("LocationService", "Error enviando ubicación", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        serviceJob.cancel()
    }
}
