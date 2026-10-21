package com.venice.pkgmerger

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Build

class MergeService : Service() {

    companion object {
        var currentProgress: String = "Fusion en cours..."
        var instance: MergeService? = null
        const val CHANNEL_ID = "pkg_merge_channel"
        const val NOTIF_ID = 101
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createChannel()
        val notification = buildNotification("Préparation de la fusion...")
        startForeground(NOTIF_ID, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Fusion de PKG en arrière-plan",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Affiche la progression du fusionneur PKG"
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    fun updateNotification(text: String) {
        currentProgress = text
        val notification = buildNotification(text)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIF_ID, notification)
    }

    private fun buildNotification(text: String): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        return builder
            .setContentTitle("PS5 PKG Merger")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_save)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }
}
