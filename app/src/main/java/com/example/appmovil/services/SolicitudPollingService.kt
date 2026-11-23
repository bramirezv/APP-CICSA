package com.example.appmovil.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.appmovil.MainActivity
import com.example.appmovil.data.remote.RetrofitClient
import kotlinx.coroutines.*

/**
 * Servicio de primer plano que realiza polling de nuevas solicitudes 
 * incluso cuando la aplicación está en segundo plano.
 */
class SolicitudPollingService : Service() {
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private var previousPendingIds = setOf<Int>()
    private var previousAuxiliarIds = setOf<Int>()
    private var previousTotalUnread = 0
    private var notifiedAcceptedIds = mutableSetOf<Int>() // IDs ya notificados al supervisor
    private var userId: Int = 0
    private var userRol: String = ""
    private var userZona: String = ""
    private var userName: String = ""
    private var wakeLock: android.os.PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        userId = intent?.getIntExtra("user_id", 0) ?: 0
        userRol = intent?.getStringExtra("user_rol") ?: ""
        userZona = intent?.getStringExtra("user_zona") ?: ""
        userName = intent?.getStringExtra("user_name") ?: ""

        createNotificationChannel()
        val notification = createPersistentNotification()
        
        // Adquirir WakeLock para que el CPU no se duerma con pantalla bloqueada
        acquireWakeLock()
        
        // Iniciamos como Foreground Service (Requerido para Android 8+)
        startForeground(1, notification)

        startPolling()
        
        // START_STICKY hace que el sistema intente recrear el servicio si lo mata por falta de recursos
        return START_STICKY
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        wakeLock = powerManager.newWakeLock(
            android.os.PowerManager.PARTIAL_WAKE_LOCK,
            "APPMOVIL:SolicitudPollingWakeLock"
        ).apply {
            acquire(10 * 60 * 1000L /*10 minutos de gracia*/)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Forzar limpieza de canales viejos 
            listOf("solicitud_channel", "solicitud_channel_v2", "solicitud_channel_v3", "solicitud_channel_v4", "solicitud_premium_v1", "solicitud_premium_master")
                .forEach { manager.deleteNotificationChannel(it) }

            val soundId = resources.getIdentifier("alerta_mision", "raw", packageName)
            val soundUri = android.net.Uri.parse("android.resource://$packageName/$soundId")
            
            // Canal satélite: IMPORTANCE_HIGH con sonido custom corto
            val channel = NotificationChannel(
                "solicitud_cronometro_v1",
                "Alertas Cortas (3 seg) CICSA",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Canal crítico para avisos con sonido de 'Alerta de Misión'"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
                setSound(soundUri, android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build())
            }

            // Canal SUPERVISOR — Máxima prioridad, sonido de 2 segundos
            val ringSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            val supervisorChannel = NotificationChannel(
                "supervisor_accepted_v1",
                "\uD83D\uDEA8 Ticket Aceptado — Supervisor",
                NotificationManager.IMPORTANCE_MAX
            ).apply {
                description = "Alerta de máxima urgencia cuando un satélite acepta un ticket del supervisor"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 200, 800)  // ~2 segundos total
                enableLights(true)
                lightColor = android.graphics.Color.GREEN
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setSound(ringSoundUri, android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build())
            }

            // Canal Silencioso para el Monitor de Fondo
            val monitorChannel = NotificationChannel(
                "service_monitor_v1",
                "Monitor de Vigilancia (Silencioso)",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Canal para el icono fijo en la barra de tareas"
                setShowBadge(false)
            }
            manager.createNotificationChannel(monitorChannel)
            manager.createNotificationChannel(channel)
            manager.createNotificationChannel(supervisorChannel)
        }
    }


    private fun startPolling() {
        serviceScope.launch {
            while (isActive) {
                // 1. Polling de Solicitudes (Aislado)
                try {
                    val isFieldWorker = !userRol.contains("admin") && !userRol.contains("supervisor")
                    
                    val responseSol = if (isFieldWorker) {
                        // Para satélites/auxiliares filtramos por su zona y su ID (para ver lo asignado + bolsa de su zona)
                        RetrofitClient.solicitudApi.getSolicitudes(zona = userZona, userId = userId, rol = userRol)
                    } else {
                        // Para admin/supervisor (Vigilancia general silenciosa)
                        RetrofitClient.solicitudApi.getSolicitudes(rol = userRol)
                    }

                    if (responseSol.isSuccessful) {
                        val currentList = responseSol.body() ?: emptyList()
                        val currentPendingIds = currentList
                            .filter { it.estado?.lowercase() == "pendiente" }
                            .map { it.id }
                            .toSet()
                        
                        val newIds = currentPendingIds - previousPendingIds
                        
                        if (newIds.isNotEmpty() && previousPendingIds.isNotEmpty() && isFieldWorker) {
                            showNewSolicitudNotification()
                        }
                        previousPendingIds = currentPendingIds
                    }
                } catch (e: Exception) {
                    // Fallo de red en tareas (no bloquea al chat)
                }

                // 1.5 Polling para Supervisores (Solicitudes de Auxiliares)
                try {
                    val isSupervisor = userRol.contains("supervisor")
                    if (isSupervisor) {
                        val responseAux = RetrofitClient.solicitudApi.getSolicitudes(tipo = "auxiliar", userId = userId, rol = userRol)
                        if (responseAux.isSuccessful) {
                            val currentList = responseAux.body() ?: emptyList()
                            val currentPendingIds = currentList
                                .filter { it.estado?.lowercase() == "pendiente" }
                                .map { it.id }
                                .toSet()
                            
                            val newIds = currentPendingIds - previousAuxiliarIds
                            if (newIds.isNotEmpty() && previousAuxiliarIds.isNotEmpty()) {
                                showNewSolicitudNotification("¡Nueva Asistencia de Auxiliar!", "Un auxiliar ha enviado su reporte para validación.")
                            }
                            previousAuxiliarIds = currentPendingIds
                        }
                    }
                } catch (e: Exception) {}

                // 1.6 Polling de tickets ACEPTADOS por satélite (Notificación al Supervisor)
                try {
                    val isSupervisor = userRol.contains("supervisor")
                    if (isSupervisor && userName.isNotEmpty()) {
                        val responseAcep = RetrofitClient.solicitudApi.checkAcceptedTickets(
                            supervisor = userName,
                            sinceSeconds = 15
                        )
                        if (responseAcep.isSuccessful) {
                            val body = responseAcep.body()
                            val newlyAccepted = body?.accepted?.filter { it.id !in notifiedAcceptedIds } ?: emptyList()
                            newlyAccepted.forEach { ticket ->
                                val sateliteName = ticket.nombreSatelite ?: "un satélite"
                                val sotText = if (!ticket.sot.isNullOrEmpty()) " • SOT: ${ticket.sot}" else ""
                                val tecText = if (!ticket.tecnico.isNullOrEmpty()) "\nTécnico: ${ticket.tecnico}" else ""
                                // Usa el canal exclusivo SUPERVISOR (más fuerte)
                                showTicketAcceptedNotification(
                                    satelite = sateliteName,
                                    extraInfo = "$sotText$tecText"
                                )
                                notifiedAcceptedIds.add(ticket.id)
                            }
                        }
                    }
                } catch (e: Exception) {}

                // 2. Polling de Chat (Aislado y con Diagnóstico)
                try {
                    if (userId != 0) {
                        val responseChat = RetrofitClient.authApi.getContacts(userId, userRol)
                        if (responseChat.isSuccessful) {
                            val contacts = responseChat.body() ?: emptyList()
                            val currentTotalUnread = contacts.sumOf { it.unreadCount }
                            
                            // Si el total de no leídos aumentó
                            if (currentTotalUnread > previousTotalUnread) {
                                showNewMessageNotification()
                            }
                            previousTotalUnread = currentTotalUnread
                            
                            // ACTUALIZAR NOTIFICACIÓN PERSISTENTE CON DIAGNÓSTICO
                            updateDiagnosticNotification(currentTotalUnread)
                        }
                    }
                } catch (e: Exception) {
                    // Fallo de red en chat
                }
                
                delay(5000) // Poll cada 5 segundos
            }
        }
    }

    private fun updateDiagnosticNotification(unreadCount: Int) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = createPersistentNotification("Vigilancia Activa [ID: $userId] - [$unreadCount MSJS]")
        manager.notify(1, notification)
    }

    private fun createPersistentNotification(subtitle: String = "Buscando nuevas actualizaciones..."): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, "service_monitor_v1")
            .setContentTitle("CI-GESTIÓN: Vigilancia Activa")
            .setContentText(subtitle)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun showNewMessageNotification() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 2, intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val soundId = resources.getIdentifier("alerta_mision", "raw", packageName)
        val soundUri = android.net.Uri.parse("android.resource://$packageName/$soundId")

        val notification = NotificationCompat.Builder(this, "solicitud_cronometro_v1")
            .setContentTitle("¡Nuevo Mensaje del Equipo!")
            .setContentText("Tienes mensajes sin leer en el chat. Toca para responder.")
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setSound(soundUri)
            .setVibrate(longArrayOf(1000, 1000, 1000))
            .setContentIntent(pendingIntent)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(3, notification)
    }

    /**
     * Notificación EXCLUSIVA del Supervisor cuando un satélite acepta su ticket.
     * Canal: supervisor_accepted_v1 (IMPORTANCE_MAX + sonido RINGTONE).
     */
    private fun showTicketAcceptedNotification(satelite: String, extraInfo: String = "") {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 4, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val ringSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

        val notification = NotificationCompat.Builder(this, "supervisor_accepted_v1")
            .setContentTitle("\uD83D\uDEA8 ¡TU TICKET FUE ACEPTADO!")
            .setContentText("$satelite aceptó tu solicitud$extraInfo")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$satelite aceptó tu solicitud$extraInfo\n\nToca para abrir la app.")
                    .setBigContentTitle("\uD83D\uDEA8 ¡TU TICKET FUE ACEPTADO!")
            )
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setSound(ringSoundUri)
            .setVibrate(longArrayOf(0, 1000, 200, 800))
            .setLights(android.graphics.Color.GREEN, 500, 200)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        serviceScope.launch {
            try {
                val ringtone = RingtoneManager.getRingtone(this@SolicitudPollingService, ringSoundUri)
                ringtone.play()
                delay(2000L)
                ringtone.stop()
            } catch (e: Exception) { /* ignorar */ }
        }

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(5, notification)
    }

    private fun showNewSolicitudNotification(title: String = "¡Nueva Solicitud Detectada!", content: String = "Tienes una tarea pendiente de atención. Toca para ver.") {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 1, intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val soundId = resources.getIdentifier("alerta_mision", "raw", packageName)
        val soundUri = android.net.Uri.parse("android.resource://$packageName/$soundId")

        val notification = NotificationCompat.Builder(this, "solicitud_cronometro_v1")
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(soundUri)
            .setVibrate(longArrayOf(1000, 1000, 1000))
            .setContentIntent(pendingIntent)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Usamos un ID distinto al de la notificación persistente
        manager.notify(2, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        // Liberar WakeLock para ahorrar batería
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
    }
}
