package com.example.nfpet.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.nfpet.MainActivity
import com.example.nfpet.R

class NotificationHelper(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "nfpet_alerts_channel"
        private const val CHANNEL_NAME = "Alertas de Mascotas Perdidas"
        private const val CHANNEL_DESC = "Notificaciones urgentes cuando se pierde una mascota cerca de ti"
        private const val NOTIFICATION_ID_BASE = 1000
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
            }
            
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Triggers a highly visible heads-up notification in the system.
     */
    fun showLostPetNotification(petName: String, cityName: String, petDescription: String) {
        if (!hasNotificationPermission()) return

        // Intent to open the MainActivity when clicking the notification
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Premium styling: BigTextStyle to show full description
        val bigTextStyle = NotificationCompat.BigTextStyle()
            .bigText("Se ha reportado la pérdida de **$petName** en **$cityName**.\n\nDescripción:\n$petDescription")
            .setBigContentTitle("🐾 ¡Alerta! Mascota perdida cerca de ti")
            .setSummaryText("Mascota Perdida")

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher) // Guaranteed to exist in Android templates
            .setContentTitle("🐾 ¡Mascota perdida en $cityName!")
            .setContentText("Buscan a $petName en tu zona. ¡Ayuda a encontrarle!")
            .setStyle(bigTextStyle)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Heads-up banner
            .setDefaults(NotificationCompat.DEFAULT_ALL)   // Sound, vibration, lights
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            val notificationId = NOTIFICATION_ID_BASE + (System.currentTimeMillis() % 1000).toInt()
            with(NotificationManagerCompat.from(context)) {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    notify(notificationId, builder.build())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
