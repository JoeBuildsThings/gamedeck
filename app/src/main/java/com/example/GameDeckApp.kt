package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.data.local.AppDatabase
import com.example.data.preferences.GameDeckPreferences
import com.example.data.repository.GameDeckRepository
import com.example.domain.ModeEngine
import com.example.system.collectors.TelemetryManager
import com.example.system.shizuku.ShizukuBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class GameDeckApp : Application() {

    companion object {
        const val OVERLAY_CHANNEL_ID = "gamedeck_overlay_channel"
        const val NOTIFICATION_ID = 2026

        lateinit var instance: GameDeckApp
            private set
    }

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database by lazy { AppDatabase.getDatabase(this) }
    val preferences by lazy { GameDeckPreferences(this) }
    val repository by lazy { GameDeckRepository(this, database, preferences) }
    val shizukuBridge by lazy { ShizukuBridge(this) }
    val modeEngine by lazy { ModeEngine() }
    val telemetryManager by lazy { TelemetryManager(this, shizukuBridge) }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
        shizukuBridge.startContinuousMonitor(appScope)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "GameDeck Service"
            val descriptionText = "Displays real-time performance overlay and telemetry"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(OVERLAY_CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(false)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
