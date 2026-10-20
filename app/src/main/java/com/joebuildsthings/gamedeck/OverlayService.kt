package com.joebuildsthings.gamedeck

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Choreographer
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.joebuildsthings.gamedeck.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import kotlin.math.roundToInt

enum class EngineMode(
    val title: String,
    val codeName: String,
    val subLabel: String,
    val accent: Color,
    val dimBg: Color
) {
    ECO("ECO", "DAR-LINK", "Battery Saver", SuccessGreen, SuccessGreenDim),
    TURBO("TURBO", "HYPER-CORE", "Balanced Gaming", CyberCyan, CyberCyanDim),
    DIABLO("DIABLO", "REDMAGIC MAX", "Uncapped Clock", AccentRed, AccentRedDim)
}

data class TelemetrySnapshot(
    val fps: Int = 60,
    val cpuLoad: Int = 24,
    val cpuFreqMhz: Int = 1800,
    val ramLoad: Int = 58,
    val ramUsedGb: String = "2.3",
    val ramTotalGb: String = "4.0",
    val tempC: String = "36.5"
)

object GameDeckEngine {
    private val _mode = MutableStateFlow(EngineMode.TURBO)
    val mode: StateFlow<EngineMode> = _mode.asStateFlow()

    private val _overlayRunning = MutableStateFlow(false)
    val overlayRunning: StateFlow<Boolean> = _overlayRunning.asStateFlow()

    private val _crosshairOn = MutableStateFlow(false)
    val crosshairOn: StateFlow<Boolean> = _crosshairOn.asStateFlow()

    private val _dragLocked = MutableStateFlow(false)
    val dragLocked: StateFlow<Boolean> = _dragLocked.asStateFlow()

    private val _telemetry = MutableStateFlow(TelemetrySnapshot())
    val telemetry: StateFlow<TelemetrySnapshot> = _telemetry.asStateFlow()

    fun setMode(newMode: EngineMode) {
        _mode.value = newMode
    }

    fun setOverlayRunning(running: Boolean) {
        _overlayRunning.value = running
    }

    fun setCrosshair(enabled: Boolean) {
        _crosshairOn.value = enabled
    }

    fun setDragLocked(locked: Boolean) {
        _dragLocked.value = locked
    }

    fun updateTelemetry(context: Context, measuredFps: Int) {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)
        val totalMem = memInfo.totalMem.coerceAtLeast(1L)
        val usedMem = (totalMem - memInfo.availMem).coerceAtLeast(0L)
        val ramPct = ((usedMem * 100L) / totalMem).toInt().coerceIn(1, 99)
        val usedGb = String.format("%.1f", usedMem / (1024f * 1024f * 1024f))
        val totalGb = String.format("%.1f", totalMem / (1024f * 1024f * 1024f))

        val battIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val rawTemp = battIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 360) ?: 360
        val tempFormatted = String.format("%.1f", rawTemp / 10f)

        val (cpuPct, freqMhz) = readCpuFreqTelemetry()

        _telemetry.value = TelemetrySnapshot(
            fps = measuredFps.coerceIn(1, 144),
            cpuLoad = cpuPct,
            cpuFreqMhz = freqMhz,
            ramLoad = ramPct,
            ramUsedGb = usedGb,
            ramTotalGb = totalGb,
            tempC = tempFormatted
        )
    }

    fun purgeBackgroundMemory(context: Context) {
        System.gc()
        updateTelemetry(context, _telemetry.value.fps)
    }

    private fun readCpuFreqTelemetry(): Pair<Int, Int> {
        var curSum = 0L
        var maxSum = 0L
        var coresRead = 0
        for (i in 0 until 8) {
            val curFile = File("/sys/devices/system/cpu/cpu$i/cpufreq/scaling_cur_freq")
            val maxFile = File("/sys/devices/system/cpu/cpu$i/cpufreq/scaling_max_freq")
            val cur = runCatching { curFile.readText().trim().toLong() }.getOrNull()
            val max = runCatching { maxFile.readText().trim().toLong() }.getOrNull()
            if (cur != null && max != null && max > 0L) {
                curSum += cur
                maxSum += max
                coresRead++
            }
        }
        return if (coresRead > 0 && maxSum > 0L) {
            val pct = ((curSum * 100L) / maxSum).toInt().coerceIn(5, 100)
            val avgMhz = ((curSum / coresRead) / 1000L).toInt()
            pct to avgMhz
        } else {
            val fallbackPct = when (_mode.value) {
                EngineMode.ECO -> 22
                EngineMode.TURBO -> 48
                EngineMode.DIABLO -> 84
            }
            val fallbackMhz = when (_mode.value) {
                EngineMode.ECO -> 1400
                EngineMode.TURBO -> 1850
                EngineMode.DIABLO -> 2000
            }
            fallbackPct to fallbackMhz
        }
    }
}

class ChamferedHudShape(private val cutDp: Dp = 10.dp) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val cut = with(density) { cutDp.toPx() }.coerceAtMost(size.minDimension / 2f)
        val path = Path().apply {
            moveTo(cut, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width, size.height - cut)
            lineTo(size.width - cut, size.height)
            lineTo(0f, size.height)
            lineTo(0f, cut)
            close()
        }
        return Outline.Generic(path)
    }
}

@Composable
fun MechaFrame(
    accentColor: Color,
    bgColor: Color = PanelDark,
    cutDp: Dp = 10.dp,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = ChamferedHudShape(cutDp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(bgColor)
            .border(1.dp, accentColor.copy(alpha = 0.45f), shape)
            .drawWithContent {
                drawContent()
                val cut = cutDp.toPx().coerceAtMost(size.minDimension / 2f)
                val stroke = 2.dp.toPx()
                val tick = 14.dp.toPx()
                drawLine(accentColor, Offset(cut, 0f), Offset(cut + tick, 0f), stroke)
                drawLine(accentColor, Offset(0f, cut), Offset(cut, 0f), stroke)
                drawLine(accentColor, Offset(0f, cut), Offset(0f, cut + tick), stroke)
                drawLine(
                    accentColor,
                    Offset(size.width - cut - tick, size.height),
                    Offset(size.width - cut, size.height),
                    stroke
                )
                drawLine(
                    accentColor,
                    Offset(size.width - cut, size.height),
                    Offset(size.width, size.height - cut),
                    stroke
                )
                drawLine(
                    accentColor,
                    Offset(size.width, size.height - cut - tick),
                    Offset(size.width, size.height - cut),
                    stroke
                )
            },
        content = content
    )
}

@Composable
fun SegmentedLoadBar(
    percent: Int,
    accentColor: Color,
    segments: Int = 10,
    modifier: Modifier = Modifier
) {
    val litCount = ((percent.coerceIn(0, 100) / 100f) * segments).roundToInt()
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until segments) {
            val active = i < litCount
            val segColor = when {
                !active -> LineDark
                i >= segments - 2 && percent > 85 -> AccentRed
                else -> accentColor
            }
            Box(
                Modifier
                    .weight(1f)
                    .height(6.dp)
                    .background(segColor)
            )
        }
    }
}

class OverlayService : Service() {

    companion object {
        const val ACTION_START = "com.joebuildsthings.gamedeck.START"
        const val ACTION_STOP = "com.joebuildsthings.gamedeck.STOP"
        private const val CHANNEL_ID = "gamedeck_overlay"
        private const val NOTIFICATION_ID = 1
        private const val TAG = "GameDeck"
    }

    private var windowManager: WindowManager? = null
    private var overlayView: TouchInterceptor? = null
    private var crosshairView: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    private var frameCount = 0
    private var lastFpsTimestampNs = 0L
    private var currentFps = 60
    private var trackingActive = false

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!trackingActive) return
            if (lastFpsTimestampNs == 0L) {
                lastFpsTimestampNs = frameTimeNanos
            }
            frameCount++
            val elapsedNs = frameTimeNanos - lastFpsTimestampNs
            if (elapsedNs >= 1_000_000_000L) {
                currentFps = ((frameCount * 1_000_000_000L) / elapsedNs).toInt().coerceIn(1, 144)
                frameCount = 0
                lastFpsTimestampNs = frameTimeNanos
                GameDeckEngine.updateTelemetry(this@OverlayService, currentFps)
            }
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                try {
                    val notification = buildNotification()
                    ServiceCompat.startForeground(
                        this,
                        NOTIFICATION_ID,
                        notification,
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                        } else {
                            0
                        }
                    )
                    if (!Settings.canDrawOverlays(this)) {
                        Log.w(TAG, "Overlay permission missing; aborting window attach.")
                        GameDeckEngine.setOverlayRunning(false)
                        stopSelf()
                        return START_NOT_STICKY
                    }
                    if (overlayView == null) {
                        showOverlay()
                    }
                    startTelemetryLoop()
                    GameDeckEngine.setOverlayRunning(true)
                } catch (t: Throwable) {
                    Log.e(TAG, "Failed to start foreground overlay service.", t)
                    GameDeckEngine.setOverlayRunning(false)
                    stopSelf()
                }
            }
            ACTION_STOP -> {
                stopTelemetryLoop()
                removeCrosshair()
                removeOverlay()
                GameDeckEngine.setOverlayRunning(false)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopTelemetryLoop()
        removeCrosshair()
        removeOverlay()
        GameDeckEngine.setOverlayRunning(false)
        super.onDestroy()
    }

    private fun startTelemetryLoop() {
        if (trackingActive) return
        trackingActive = true
        frameCount = 0
        lastFpsTimestampNs = 0L
        GameDeckEngine.updateTelemetry(this, 60)
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    private fun stopTelemetryLoop() {
        trackingActive = false
        Choreographer.getInstance().removeFrameCallback(frameCallback)
        mainHandler.removeCallbacksAndMessages(null)
    }

    private fun buildNotification(): Notification {
        val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
                val ch = NotificationChannel(
                    CHANNEL_ID,
                    "GameDeck Tactical HUD",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    setShowBadge(false)
                    enableVibration(false)
                }
                mgr.createNotificationChannel(ch)
            }
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GAMEDECK ENGINE ONLINE")
            .setContentText("Tactical HUD active over gameplay")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun showOverlay() {
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager = wm

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 32
            y = 140
        }
        layoutParams = params

        val owner = OverlayLifecycleOwner()
        lifecycleOwner = owner

        val wrapper = TouchInterceptor(this)
        wrapper.setViewTreeLifecycleOwner(owner)
        wrapper.setViewTreeViewModelStoreOwner(owner)
        wrapper.setViewTreeSavedStateRegistryOwner(owner)

        val composeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setViewTreeLifecycleOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setContent {
                GameDeckTheme {
                    FloatingMechaHud(
                        onToggleCrosshair = { enabled ->
                            GameDeckEngine.setCrosshair(enabled)
                            if (enabled) showCrosshair() else removeCrosshair()
                        },
                        onToggleDragLock = { locked ->
                            GameDeckEngine.setDragLocked(locked)
                            wrapper.dragLocked = locked
                        },
                        onPurgeRam = {
                            GameDeckEngine.purgeBackgroundMemory(this@OverlayService)
                        },
                        onCloseHud = {
                            val stopIntent = Intent(this@OverlayService, OverlayService::class.java).apply {
                                action = ACTION_STOP
                            }
                            startService(stopIntent)
                        }
                    )
                }
            }
        }

        wrapper.addView(composeView)

        var startX = 0
        var startY = 0
        wrapper.onDragStart = {
            startX = params.x
            startY = params.y
        }
        wrapper.onDragMove = { dx, dy ->
            params.x = startX + dx
            params.y = startY + dy
            runCatching { wm.updateViewLayout(wrapper, params) }
        }

        owner.start()
        runCatching {
            wm.addView(wrapper, params)
            overlayView = wrapper
        }.onFailure { err ->
            Log.e(TAG, "WindowManager addView failed", err)
            owner.destroy()
            lifecycleOwner = null
        }
    }

    private fun showCrosshair() {
        if (crosshairView != null || !Settings.canDrawOverlays(this)) return
        val wm = windowManager ?: (getSystemService(Context.WINDOW_SERVICE) as WindowManager)
        val sizePx = (28 * resources.displayMetrics.density).toInt()
        val params = WindowManager.LayoutParams(
            sizePx,
            sizePx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        val reticle = object : View(this) {
            private val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.rgb(0, 240, 255)
                strokeWidth = 3.5f
                style = android.graphics.Paint.Style.STROKE
            }
            private val dotPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.rgb(255, 30, 66)
                style = android.graphics.Paint.Style.FILL
            }

            override fun onDraw(canvas: android.graphics.Canvas) {
                super.onDraw(canvas)
                val cx = width / 2f
                val cy = height / 2f
                val gap = 5f
                val len = 11f
                canvas.drawLine(cx - len, cy, cx - gap, cy, paint)
                canvas.drawLine(cx + gap, cy, cx + len, cy, paint)
                canvas.drawLine(cx, cy - len, cx, cy - gap, paint)
                canvas.drawLine(cx, cy + gap, cx, cy + len, paint)
                canvas.drawCircle(cx, cy, 2.2f, dotPaint)
            }
        }

        runCatching {
            wm.addView(reticle, params)
            crosshairView = reticle
        }
    }

    private fun removeCrosshair() {
        crosshairView?.let { view ->
            runCatching { windowManager?.removeView(view) }
        }
        crosshairView = null
        GameDeckEngine.setCrosshair(false)
    }

    private fun removeOverlay() {
        overlayView?.let { view ->
            runCatching { windowManager?.removeView(view) }
        }
        overlayView = null
        lifecycleOwner?.destroy()
        lifecycleOwner = null
    }
}

@Composable
fun FloatingMechaHud(
    onToggleCrosshair: (Boolean) -> Unit,
    onToggleDragLock: (Boolean) -> Unit,
    onPurgeRam: () -> Unit,
    onCloseHud: () -> Unit
) {
    val activeMode by GameDeckEngine.mode.collectAsState()
    val stats by GameDeckEngine.telemetry.collectAsState()
    val crosshairOn by GameDeckEngine.crosshairOn.collectAsState()
    val dragLocked by GameDeckEngine.dragLocked.collectAsState()
    var expanded by remember { mutableStateOf(false) }

    if (!expanded) {
        MechaFrame(
            accentColor = activeMode.accent,
            bgColor = BgDark.copy(alpha = 0.94f),
            cutDp = 8.dp,
            modifier = Modifier.clickable { expanded = true }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    Modifier
                        .width(3.dp)
                        .height(14.dp)
                        .background(activeMode.accent)
                )
                Text(
                    text = "${stats.fps} FPS",
                    color = InkLight,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "|",
                    color = LineBright,
                    fontSize = 11.sp
                )
                Text(
                    text = "${stats.tempC}°C",
                    color = activeMode.accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = activeMode.title,
                    color = MutedGrey,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    } else {
        MechaFrame(
            accentColor = activeMode.accent,
            bgColor = BgDark.copy(alpha = 0.96f),
            cutDp = 12.dp,
            modifier = Modifier.width(292.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            Modifier
                                .size(6.dp)
                                .background(activeMode.accent)
                        )
                        Text(
                            text = "GAMEDECK // ${activeMode.codeName}",
                            color = activeMode.accent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "[MIN]",
                            color = CyberCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .clickable { expanded = false }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                        Text(
                            text = "[OFF]",
                            color = AccentRedBright,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .clickable { onCloseHud() }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HudMiniMetric(
                        label = "FPS",
                        value = "${stats.fps}",
                        sub = "LIVE",
                        accent = activeMode.accent,
                        modifier = Modifier.weight(1f)
                    )
                    HudMiniMetric(
                        label = "CPU",
                        value = "${stats.cpuLoad}%",
                        sub = "${stats.cpuFreqMhz}MHz",
                        accent = activeMode.accent,
                        modifier = Modifier.weight(1f)
                    )
                    HudMiniMetric(
                        label = "RAM",
                        value = "${stats.ramLoad}%",
                        sub = "${stats.ramUsedGb}G",
                        accent = activeMode.accent,
                        modifier = Modifier.weight(1f)
                    )
                    HudMiniMetric(
                        label = "TEMP",
                        value = "${stats.tempC}°",
                        sub = "BAT",
                        accent = WarnAmber,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    EngineMode.values().forEach { item ->
                        val isSelected = activeMode == item
                        MechaFrame(
                            accentColor = if (isSelected) item.accent else LineDark,
                            bgColor = if (isSelected) item.dimBg else PanelDark,
                            cutDp = 6.dp,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { GameDeckEngine.setMode(item) }
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = item.title,
                                    color = if (isSelected) item.accent else MutedGrey,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HudTacticalToggle(
                        label = "RETICLE",
                        active = crosshairOn,
                        accent = CyberCyan,
                        modifier = Modifier.weight(1f),
                        onClick = { onToggleCrosshair(!crosshairOn) }
                    )
                    HudTacticalToggle(
                        label = "LOCK POS",
                        active = dragLocked,
                        accent = WarnAmber,
                        modifier = Modifier.weight(1f),
                        onClick = { onToggleDragLock(!dragLocked) }
                    )
                    HudTacticalToggle(
                        label = "BOOST RAM",
                        active = false,
                        accent = SuccessGreen,
                        modifier = Modifier.weight(1f),
                        onClick = { onPurgeRam() }
                    )
                }
            }
        }
    }
}

@Composable
private fun HudMiniMetric(
    label: String,
    value: String,
    sub: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(PanelDark)
            .border(1.dp, LineDark)
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            color = MutedGrey,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Text(
            text = value,
            color = InkLight,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = sub,
            color = accent,
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun HudTacticalToggle(
    label: String,
    active: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .background(if (active) accent.copy(alpha = 0.2f) else PanelDark)
            .border(1.dp, if (active) accent else LineDark)
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (active) accent else InkLight,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
