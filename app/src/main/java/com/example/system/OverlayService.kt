package com.example.system

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import com.example.GameDeckApp
import com.example.MainActivity
import com.example.R
import com.example.model.OverlayConfig
import com.example.model.PerformanceMode
import com.example.model.TelemetryMetrics
import com.example.ui.overlay.OverlayViewContent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs

class OverlayService : Service() {

    companion object {
        const val ACTION_START_OVERLAY = "com.example.gamedeck.START_OVERLAY"
        const val ACTION_STOP_OVERLAY = "com.example.gamedeck.STOP_OVERLAY"
        const val ACTION_CYCLE_MODE = "com.example.gamedeck.CYCLE_MODE"

        private var activeGameOverlayConfig: OverlayConfig? = null

        fun setGameSpecificOverlayConfig(config: OverlayConfig?) {
            activeGameOverlayConfig = config
        }

        fun startService(context: Context) {
            val intent = Intent(context, OverlayService::class.java).apply {
                action = ACTION_START_OVERLAY
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, OverlayService::class.java).apply {
                action = ACTION_STOP_OVERLAY
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var overlayParams: WindowManager.LayoutParams? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null

    private val currentMetrics = mutableStateOf(TelemetryMetrics())
    private val overlayConfigState = mutableStateOf(OverlayConfig())
    private val currentModeState = mutableStateOf(PerformanceMode.BALANCED)
    private val isExpandedState = mutableStateOf(false)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startForegroundNotification()
        observePreferencesAndTelemetry()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_OVERLAY -> {
                serviceScope.launch {
                    GameDeckApp.instance.repository.setOverlayEnabled(false)
                }
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_CYCLE_MODE -> {
                serviceScope.launch {
                    val nextMode = currentModeState.value.next()
                    applyMode(nextMode)
                }
            }
            else -> {
                createOrUpdateOverlay()
            }
        }
        return START_STICKY
    }

    private fun startForegroundNotification() {
        val stopIntent = Intent(this, OverlayService::class.java).apply { action = ACTION_STOP_OVERLAY }
        val stopPendingIntent = PendingIntent.getService(this, 1, stopIntent, PendingIntent.FLAG_IMMUTABLE)

        val cycleIntent = Intent(this, OverlayService::class.java).apply { action = ACTION_CYCLE_MODE }
        val cyclePendingIntent = PendingIntent.getService(this, 2, cycleIntent, PendingIntent.FLAG_IMMUTABLE)

        val appIntent = Intent(this, MainActivity::class.java)
        val appPendingIntent = PendingIntent.getActivity(this, 0, appIntent, PendingIntent.FLAG_IMMUTABLE)

        val notification: Notification = NotificationCompat.Builder(this, GameDeckApp.OVERLAY_CHANNEL_ID)
            .setContentTitle("GameDeck Active")
            .setContentText("Mode: ${currentModeState.value.title} • Tap to open")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(appPendingIntent)
            .setOngoing(true)
            .addAction(0, "Cycle Mode", cyclePendingIntent)
            .addAction(0, "Stop", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(GameDeckApp.NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(GameDeckApp.NOTIFICATION_ID, notification)
        }
    }

    private fun observePreferencesAndTelemetry() {
        // Collect telemetry
        GameDeckApp.instance.telemetryManager.start(serviceScope)
        UsageDetector.startMonitoring(applicationContext, GameDeckApp.instance.appScope)
        serviceScope.launch {
            GameDeckApp.instance.telemetryManager.telemetryFlow.collectLatest { metrics ->
                currentMetrics.value = metrics
                // Record session sample if active
                GameDeckApp.instance.repository.recordSessionSample(metrics)
            }
        }

        // Collect preferences
        serviceScope.launch {
            GameDeckApp.instance.repository.overlayConfigFlow.collectLatest { cfg ->
                val effectiveCfg = activeGameOverlayConfig ?: cfg
                overlayConfigState.value = effectiveCfg
                GameDeckApp.instance.telemetryManager.setUpdateInterval(effectiveCfg.updateIntervalMs)
                updateOverlayWindowPosition(effectiveCfg.posX, effectiveCfg.posY)
            }
        }

        serviceScope.launch {
            GameDeckApp.instance.repository.currentModeFlow.collectLatest { mode ->
                currentModeState.value = mode
                startForegroundNotification()
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createOrUpdateOverlay() {
        if (overlayView != null) return

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = overlayConfigState.value.posX
            y = overlayConfigState.value.posY
        }
        overlayParams = layoutParams

        val owner = OverlayLifecycleOwner()
        lifecycleOwner = owner

        val composeView = ComposeView(this).apply {
            owner.attachToView(this)
            setContent {
                OverlayViewContent(
                    metrics = currentMetrics.value,
                    config = overlayConfigState.value,
                    currentMode = currentModeState.value,
                    isExpanded = isExpandedState.value,
                    onToggleExpand = {
                        isExpandedState.value = !isExpandedState.value
                    },
                    onSelectMode = { newMode ->
                        serviceScope.launch { applyMode(newMode) }
                    },
                    onRamClean = {
                        serviceScope.launch {
                            val action = com.example.domain.TrimBackgroundCacheAction()
                            val res = action.apply(this@OverlayService, GameDeckApp.instance.shizukuBridge)
                            GameDeckApp.instance.repository.logAction("RAM Clean", res.summary)
                        }
                    },
                    onPanicRestore = {
                        serviceScope.launch { panicRestore() }
                    },
                    onOpenApp = {
                        val openIntent = Intent(this@OverlayService, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        }
                        startActivity(openIntent)
                    },
                    onCloseOverlay = {
                        serviceScope.launch {
                            GameDeckApp.instance.repository.setOverlayEnabled(false)
                        }
                        stopSelf()
                    }
                )
            }
        }

        // Draggable touch handling with snap-to-edge
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false

        composeView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    false
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = (event.rawX - initialTouchX).toInt()
                    val deltaY = (event.rawY - initialTouchY).toInt()

                    if (abs(deltaX) > 10 || abs(deltaY) > 10 || isDragging) {
                        isDragging = true
                        layoutParams.x = initialX + deltaX
                        layoutParams.y = initialY + deltaY
                        try {
                            windowManager?.updateViewLayout(composeView, layoutParams)
                        } catch (_: Exception) {}
                        true
                    } else {
                        false
                    }
                }
                MotionEvent.ACTION_UP -> {
                    if (isDragging) {
                        // Snap to nearest screen edge (left or right)
                        val displayMetrics = resources.displayMetrics
                        val screenWidth = displayMetrics.widthPixels
                        val finalX = if (layoutParams.x + (composeView.width / 2) < screenWidth / 2) {
                            16 // Snap to left edge with padding
                        } else {
                            screenWidth - composeView.width - 16 // Snap to right edge
                        }
                        layoutParams.x = finalX
                        try {
                            windowManager?.updateViewLayout(composeView, layoutParams)
                        } catch (_: Exception) {}

                        // Persist position
                        serviceScope.launch {
                            GameDeckApp.instance.repository.saveOverlayPosition(layoutParams.x, layoutParams.y)
                        }
                        true
                    } else {
                        false
                    }
                }
                else -> false
            }
        }

        owner.onStart()
        try {
            windowManager?.addView(composeView, layoutParams)
            overlayView = composeView
        } catch (_: Exception) {}
    }

    private fun updateOverlayWindowPosition(x: Int, y: Int) {
        val view = overlayView ?: return
        val params = overlayParams ?: return
        params.x = x
        params.y = y
        try {
            windowManager?.updateViewLayout(view, params)
        } catch (_: Exception) {}
    }

    private suspend fun applyMode(mode: PerformanceMode) {
        GameDeckApp.instance.repository.setMode(mode)
        val (dnd, rr, _) = GameDeckApp.instance.repository.getBaselineSnapshot()
        val whitelist = GameDeckApp.instance.repository.killWhitelistFlow.first()

        val plan = GameDeckApp.instance.modeEngine.buildActionPlan(
            targetMode = mode,
            allowedThirdPartyPackages = whitelist,
            baselineDnd = dnd,
            baselineRefreshRate = rr
        )

        for (action in plan) {
            val res = action.apply(this@OverlayService, GameDeckApp.instance.shizukuBridge)
            GameDeckApp.instance.repository.logAction(action.id, "${action.description}: ${res.summary}", action.isReversible)
        }
    }

    private suspend fun panicRestore() {
        val (dnd, rr, _) = GameDeckApp.instance.repository.getBaselineSnapshot()
        val plan = GameDeckApp.instance.modeEngine.buildActionPlan(
            targetMode = PerformanceMode.BALANCED,
            allowedThirdPartyPackages = emptySet(),
            baselineDnd = dnd,
            baselineRefreshRate = rr
        )
        for (action in plan) {
            action.apply(this@OverlayService, GameDeckApp.instance.shizukuBridge)
        }
        GameDeckApp.instance.repository.setMode(PerformanceMode.BALANCED)
        GameDeckApp.instance.repository.markAllLogsReverted()
        GameDeckApp.instance.repository.logAction("Panic Restore", "All settings reverted to safe baseline")
    }

    override fun onDestroy() {
        super.onDestroy()
        activeGameOverlayConfig = null
        GameDeckApp.instance.telemetryManager.stop()
        if (overlayView != null) {
            try {
                windowManager?.removeView(overlayView)
            } catch (_: Exception) {}
            overlayView = null
        }
        lifecycleOwner?.onDestroy()
        lifecycleOwner = null
        serviceScope.cancel()
    }
}
