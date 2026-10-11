package com.joebuildsthings.gamedeck

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.joebuildsthings.gamedeck.ui.theme.AccentRed
import com.joebuildsthings.gamedeck.ui.theme.AccentRedBright
import com.joebuildsthings.gamedeck.ui.theme.AccentRedDim
import com.joebuildsthings.gamedeck.ui.theme.GameDeckTheme

class OverlayService : Service() {

    companion object {
        const val ACTION_START = "com.joebuildsthings.gamedeck.START"
        const val ACTION_STOP = "com.joebuildsthings.gamedeck.STOP"
        private const val CHANNEL_ID = "gamedeck_overlay"
        private const val NOTIFICATION_ID = 1
        private const val TAG = "GameDeck"
    }

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                try {
                    // Phase 1: Immediately start foreground with a minimal notification
                    // to satisfy Android's 5-second rule.
                    val minimalNotification = NotificationCompat.Builder(this, CHANNEL_ID)
                        .setContentTitle("GameDeck")
                        .setContentText("Initializing...")
                        .setSmallIcon(android.R.drawable.ic_menu_info_details)
                        .setOngoing(true)
                        .setSilent(true)
                        .build()

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        ServiceCompat.startForeground(
                            this,
                            NOTIFICATION_ID,
                            minimalNotification,
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                        )
                    } else {
                        startForeground(NOTIFICATION_ID, minimalNotification)
                    }
                    Log.i(TAG, "Foreground service started with minimal notification.")

                    // Phase 2: Now build the full notification and update it.
                    val fullNotification = buildNotification()
                    val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    mgr.notify(NOTIFICATION_ID, fullNotification)
                    Log.i(TAG, "Full notification updated.")

                    if (overlayView == null) {
                        try {
                            showOverlay()
                            Log.i(TAG, "Overlay view added successfully.")
                        } catch (t: Throwable) {
                            Log.e(TAG, "Failed to add overlay view.", t)
                        }
                    }
                } catch (t: Throwable) {
                    Log.e(TAG, "Failed to start foreground service.", t)
                    stopSelf()
                }
            }
            ACTION_STOP -> {
                removeOverlay()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        removeOverlay()
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
                val ch = NotificationChannel(
                    CHANNEL_ID,
                    "GameDeck Overlay",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    setShowBadge(false)
                    enableVibration(false)
                }
                mgr.createNotificationChannel(ch)
            }
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GameDeck")
            .setContentText("Overlay running")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun showOverlay() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40
            y = 200
        }
        layoutParams = params
        val view = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent { GameDeckTheme { Pill() } }
        }
        var initialX = 0
        var initialY = 0
        var touchX = 0f
        var touchY = 0f
        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    touchX = event.rawX
                    touchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - touchX).toInt()
                    params.y = initialY + (event.rawY - touchY).toInt()
                    windowManager?.updateViewLayout(view, params)
                    true
                }
                else -> false
            }
        }
        windowManager?.addView(view, params)
        overlayView = view
    }

    private fun removeOverlay() {
        overlayView?.let { runCatching { windowManager?.removeView(it) } }
        overlayView = null
    }
}

@Composable
private fun Pill() {
    Box(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(AccentRedDim)
            .border(1.dp, AccentRed, RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(
            "-- FPS",
            color = AccentRedBright,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp
        )
    }
}
