package com.example.system.collectors

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.display.DisplayManager
import android.os.BatteryManager
import android.os.Build
import android.view.Display
import android.view.WindowManager
import com.example.domain.SurfaceFlingerParser
import com.example.model.TelemetryMetrics
import com.example.system.shizuku.ShizukuBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.RandomAccessFile

interface MetricsCollector<T> {
    suspend fun collect(): T
}

class CpuCollector : MetricsCollector<Float> {
    private var lastTotalTime: Long = 0L
    private var lastIdleTime: Long = 0L

    override suspend fun collect(): Float {
        return try {
            val reader = RandomAccessFile("/proc/stat", "r")
            val load = reader.readLine()
            reader.close()

            if (load != null && load.startsWith("cpu ")) {
                val toks = load.split("\\s+".toRegex())
                // toks[0] is "cpu"
                val user = toks.getOrNull(1)?.toLongOrNull() ?: 0L
                val nice = toks.getOrNull(2)?.toLongOrNull() ?: 0L
                val system = toks.getOrNull(3)?.toLongOrNull() ?: 0L
                val idle = toks.getOrNull(4)?.toLongOrNull() ?: 0L
                val iowait = toks.getOrNull(5)?.toLongOrNull() ?: 0L
                val irq = toks.getOrNull(6)?.toLongOrNull() ?: 0L
                val softirq = toks.getOrNull(7)?.toLongOrNull() ?: 0L
                val steal = toks.getOrNull(8)?.toLongOrNull() ?: 0L

                val total = user + nice + system + idle + iowait + irq + softirq + steal
                val idleTotal = idle + iowait

                if (lastTotalTime != 0L) {
                    val totalDiff = total - lastTotalTime
                    val idleDiff = idleTotal - lastIdleTime
                    lastTotalTime = total
                    lastIdleTime = idleTotal
                    if (totalDiff > 0) {
                        val usage = ((totalDiff - idleDiff).toFloat() / totalDiff.toFloat()) * 100f
                        return usage.coerceIn(0f, 100f)
                    }
                } else {
                    lastTotalTime = total
                    lastIdleTime = idleTotal
                }
            }
            0f
        } catch (_: Throwable) {
            // Fallback for strict SELinux without /proc/stat read permission
            0f
        }
    }
}

class RamCollector(private val context: Context) : MetricsCollector<Pair<Long, Long>> {
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    /**
     * Returns Pair(usedRamMb, totalRamMb)
     */
    override suspend fun collect(): Pair<Long, Long> {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        val totalMb = memInfo.totalMem / (1024 * 1024)
        val availMb = memInfo.availMem / (1024 * 1024)
        val usedMb = (totalMb - availMb).coerceAtLeast(0L)
        return Pair(usedMb, totalMb)
    }
}

class ThermalBatteryCollector(private val context: Context) : MetricsCollector<Pair<Float, Int>> {
    /**
     * Returns Pair(batteryTempC, batteryPercent)
     */
    override suspend fun collect(): Pair<Float, Int> {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = context.registerReceiver(null, filter)

        val rawTemp = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        val tempC = if (rawTemp > 0) rawTemp / 10f else 0f

        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = if (level >= 0 && scale > 0) {
            ((level.toFloat() / scale.toFloat()) * 100).toInt()
        } else {
            100
        }

        return Pair(tempC, batteryPct)
    }
}

class FpsCollector(
    private val context: Context,
    private val shizukuBridge: ShizukuBridge
) {
    private var cachedLayerName: String? = null
    private var cachedPackage: String? = null
    private var lastLayerCheckTime: Long = 0L

    suspend fun collectFps(targetPackage: String?): TelemetryMetrics {
        val displayRefreshRate = getDisplayRefreshRate()
        val defaultRefreshPeriodNs = (1_000_000_000L / displayRefreshRate.coerceAtLeast(30f)).toLong()

        // Fallback if Shizuku is not available or targetPackage is blank
        if (!shizukuBridge.isAvailableAndGranted() || targetPackage.isNullOrBlank()) {
            val frameTimeMs = 1000f / displayRefreshRate.coerceAtLeast(30f)
            return TelemetryMetrics(
                fps = displayRefreshRate,
                frameTimeMs = frameTimeMs,
                refreshPeriodNs = defaultRefreshPeriodNs,
                isRealFps = false,
                jankPercent = 0f,
                onePercentLowFps = displayRefreshRate,
                frameHistory = emptyList()
            )
        }

        // Shizuku is active: Resolve layer name for targetPackage
        val now = System.currentTimeMillis()
        if (cachedPackage != targetPackage || cachedLayerName == null || now - lastLayerCheckTime > 8000L) {
            val listRes = shizukuBridge.exec("dumpsys SurfaceFlinger --list", timeoutMs = 1500L)
            if (listRes.isSuccess) {
                cachedLayerName = SurfaceFlingerParser.findLayerForPackage(listRes.getOrNull() ?: "", targetPackage)
                cachedPackage = targetPackage
                lastLayerCheckTime = now
            }
        }

        val layer = cachedLayerName
        if (layer == null) {
            return TelemetryMetrics(
                fps = displayRefreshRate,
                frameTimeMs = 1000f / displayRefreshRate,
                refreshPeriodNs = defaultRefreshPeriodNs,
                isRealFps = false,
                jankPercent = 0f,
                onePercentLowFps = displayRefreshRate
            )
        }

        // Query latency
        val latencyRes = shizukuBridge.exec("dumpsys SurfaceFlinger --latency " + shellQuote(layer), timeoutMs = 1500L)
        if (latencyRes.isSuccess) {
            val parseResult = SurfaceFlingerParser.parseLatencyOutput(latencyRes.getOrNull() ?: "")
            if (parseResult != null) {
                return TelemetryMetrics(
                    fps = parseResult.fps,
                    frameTimeMs = parseResult.frameTimeMs,
                    refreshPeriodNs = parseResult.refreshPeriodNs,
                    isRealFps = true,
                    jankPercent = parseResult.jankPercent,
                    onePercentLowFps = parseResult.onePercentLowFps,
                    frameHistory = parseResult.recentFrameDeltasMs
                )
            }
        }

        // Fallback on error
        return TelemetryMetrics(
            fps = displayRefreshRate,
            frameTimeMs = 1000f / displayRefreshRate,
            refreshPeriodNs = defaultRefreshPeriodNs,
            isRealFps = false
        )
    }

    private fun shellQuote(s: String): String = "'" + s.replace("'", "'\\''") + "'"

    private fun getDisplayRefreshRate(): Float {
        return try {
            val dm = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
            val display = dm?.getDisplay(Display.DEFAULT_DISPLAY)
            display?.refreshRate ?: 60f
        } catch (_: Throwable) {
            60f
        }
    }
}

class TelemetryManager(
    private val context: Context,
    private val shizukuBridge: ShizukuBridge
) {
    private val cpuCollector = CpuCollector()
    private val ramCollector = RamCollector(context)
    private val thermalCollector = ThermalBatteryCollector(context)
    private val fpsCollector = FpsCollector(context, shizukuBridge)

    private val _telemetryFlow = MutableStateFlow(TelemetryMetrics())
    val telemetryFlow: StateFlow<TelemetryMetrics> = _telemetryFlow.asStateFlow()

    private val rollingFrameDeltas = ArrayList<Float>(120)
    private var collectionJob: Job? = null
    private var currentForegroundPackage: String? = null
    private var updateIntervalMs: Long = 1000L // 1 Hz (or 500 ms = 2 Hz max)

    fun setForegroundPackage(packageName: String?) {
        currentForegroundPackage = packageName
    }

    fun setUpdateInterval(intervalMs: Long) {
        updateIntervalMs = intervalMs.coerceIn(500L, 2000L)
    }

    fun start(scope: CoroutineScope) {
        if (collectionJob?.isActive == true) return

        collectionJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                try {
                    val fpsMetric = fpsCollector.collectFps(currentForegroundPackage)
                    val cpuUsage = cpuCollector.collect()
                    val (ramUsed, ramTotal) = ramCollector.collect()
                    val (tempC, batteryPct) = thermalCollector.collect()

                    // Update rolling frame history for the canvas graph
                    synchronized(rollingFrameDeltas) {
                        if (fpsMetric.frameHistory.isNotEmpty()) {
                            rollingFrameDeltas.clear()
                            rollingFrameDeltas.addAll(fpsMetric.frameHistory)
                        } else {
                            if (rollingFrameDeltas.size >= 120) {
                                rollingFrameDeltas.removeAt(0)
                            }
                            rollingFrameDeltas.add(fpsMetric.frameTimeMs)
                        }
                    }

                    _telemetryFlow.value = fpsMetric.copy(
                        cpuUsagePercent = cpuUsage,
                        ramUsedMb = ramUsed,
                        ramTotalMb = ramTotal,
                        batteryTempC = tempC,
                        batteryPercent = batteryPct,
                        frameHistory = synchronized(rollingFrameDeltas) { rollingFrameDeltas.toList() },
                        timestamp = System.currentTimeMillis()
                    )
                } catch (_: Throwable) {}

                delay(updateIntervalMs)
            }
        }
    }

    fun stop() {
        collectionJob?.cancel()
        collectionJob = null
    }
}
