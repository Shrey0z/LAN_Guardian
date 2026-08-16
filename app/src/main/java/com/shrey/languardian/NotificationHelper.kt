package com.shrey.languardian

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * Shows a system notification when a critical alert comes in - so you
 * find out about a rogue device even if the app isn't in the foreground.
 *
 * Requires the POST_NOTIFICATIONS runtime permission on Android 13+
 * (handled in MainActivity) and the CHANNEL_ID channel to be registered
 * once at app start.
 */
object NotificationHelper {
    private const val CHANNEL_ID = "lan_guardian_alerts"
    private var nextNotificationId = 1000

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "LAN Guardian Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical rogue-device and ARP spoofing alerts"
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    fun showAlert(context: Context, alert: Alert) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Rogue device detected")
            .setContentText("${alert.mac} on ${alert.ip}: ${alert.explanation}")
            .setStyle(NotificationCompat.BigTextStyle().bigText(alert.explanation))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        // Each alert gets a unique ID so multiple notifications stack
        // instead of overwriting each other.
        NotificationManagerCompat.from(context).notify(nextNotificationId++, notification)
    }
}
