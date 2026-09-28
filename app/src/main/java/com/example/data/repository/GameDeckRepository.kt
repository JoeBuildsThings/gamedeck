package com.example.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.example.data.local.ActivityLogEntity
import com.example.data.local.AppDatabase
import com.example.data.local.GameProfileEntity
import com.example.data.local.SessionEntity
import com.example.data.preferences.GameDeckPreferences
import com.example.model.GameProfile
import com.example.model.OverlayConfig
import com.example.model.PerformanceMode
import com.example.model.TelemetryMetrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

data class InstalledAppInfo(
    val packageName: String,
    val appName: String,
    val isGameCategory: Boolean,
    val isSystemApp: Boolean
)

class GameDeckRepository(
    private val context: Context,
    private val database: AppDatabase,
    private val preferences: GameDeckPreferences
) {
    val profilesFlow: Flow<List<GameProfile>> = database.gameProfileDao().getAllProfiles().map { entities ->
        entities.map { it.toDomain() }
    }

    val sessionsFlow: Flow<List<SessionEntity>> = database.sessionDao().getAllSessions()

    val activityLogsFlow: Flow<List<ActivityLogEntity>> = database.activityLogDao().getAllLogs()

    val currentModeFlow: Flow<PerformanceMode> = preferences.currentModeFlow
    val isOverlayEnabledFlow: Flow<Boolean> = preferences.isOverlayEnabledFlow
    val overlayConfigFlow: Flow<OverlayConfig> = preferences.overlayConfigFlow
    val isAutoDetectEnabledFlow: Flow<Boolean> = preferences.isAutoDetectEnabledFlow
    val killWhitelistFlow: Flow<Set<String>> = preferences.killWhitelistFlow

    // Session recording state
    private var currentSessionStartTime: Long = 0L
    private var currentSessionPackage: String = ""
    private var currentSessionAppName: String = ""
    private var currentSessionMode: PerformanceMode = PerformanceMode.BALANCED
    private val currentSessionSamples = mutableListOf<TelemetryMetrics>()

    suspend fun setMode(mode: PerformanceMode) {
        preferences.setPerformanceMode(mode)
    }

    suspend fun setOverlayEnabled(enabled: Boolean) {
        preferences.setOverlayEnabled(enabled)
    }

    suspend fun saveOverlayPosition(x: Int, y: Int) {
        preferences.saveOverlayPosition(x, y)
    }

    suspend fun updateOverlayConfig(config: OverlayConfig) {
        preferences.updateOverlayConfig(config)
    }

    suspend fun setAutoDetectEnabled(enabled: Boolean) {
        preferences.setAutoDetectEnabled(enabled)
    }

    suspend fun setKillWhitelist(packages: Set<String>) {
        preferences.setKillWhitelist(packages)
    }

    suspend fun getProfile(packageName: String): GameProfile? {
        return database.gameProfileDao().getProfile(packageName)?.toDomain()
    }

    suspend fun saveProfile(profile: GameProfile) {
        database.gameProfileDao().insertProfile(profile.toEntity())
    }

    suspend fun deleteProfile(packageName: String) {
        database.gameProfileDao().deleteProfileByPackage(packageName)
    }

    suspend fun logAction(actionType: String, details: String, isReversible: Boolean = true): Long {
        return database.activityLogDao().insertLog(
            ActivityLogEntity(
                timestamp = System.currentTimeMillis(),
                actionType = actionType,
                details = details,
                isReversible = isReversible,
                reverted = false
            )
        )
    }

    suspend fun markAllLogsReverted() {
        database.activityLogDao().markAllReverted()
    }

    suspend fun clearLogs() {
        database.activityLogDao().clearLogs()
    }

    suspend fun deleteSession(session: SessionEntity) {
        database.sessionDao().deleteSession(session)
    }

    suspend fun clearAllSessions() {
        database.sessionDao().clearAllSessions()
    }

    suspend fun getSessionById(id: Long): SessionEntity? {
        return database.sessionDao().getSessionById(id)
    }

    // Session Recording Lifecycle
    fun startSession(packageName: String, appName: String, mode: PerformanceMode) {
        synchronized(currentSessionSamples) {
            currentSessionStartTime = System.currentTimeMillis()
            currentSessionPackage = packageName
            currentSessionAppName = appName
            currentSessionMode = mode
            currentSessionSamples.clear()
        }
    }

    fun recordSessionSample(metric: TelemetryMetrics) {
        synchronized(currentSessionSamples) {
            if (currentSessionStartTime > 0L) {
                currentSessionSamples.add(metric)
            }
        }
    }

    suspend fun endSession(): SessionEntity? = withContext(Dispatchers.IO) {
        val samples: List<TelemetryMetrics>
        val pkg: String
        val name: String
        val startTime: Long
        val mode: PerformanceMode
        synchronized(currentSessionSamples) {
            if (currentSessionStartTime == 0L || currentSessionSamples.isEmpty()) {
                currentSessionStartTime = 0L
                return@withContext null
            }
            samples = currentSessionSamples.toList()
            pkg = currentSessionPackage
            name = currentSessionAppName
            startTime = currentSessionStartTime
            mode = currentSessionMode
            currentSessionStartTime = 0L
            currentSessionSamples.clear()
        }

        val endTime = System.currentTimeMillis()
        val durationSeconds = ((endTime - startTime) / 1000L).coerceAtLeast(1L)
        val validFpsSamples = samples.map { it.fps }.filter { it > 0f }
        val avgFps = if (validFpsSamples.isNotEmpty()) validFpsSamples.average().toFloat() else 0f
        val sortedFps = validFpsSamples.sorted()
        val onePercentLow = if (sortedFps.isNotEmpty()) {
            val index = (sortedFps.size * 0.01f).toInt().coerceIn(0, sortedFps.size - 1)
            sortedFps[index]
        } else 0f

        val totalJankPercent = if (samples.isNotEmpty()) {
            samples.map { it.jankPercent }.average().toFloat()
        } else 0f

        val maxTemp = samples.maxOfOrNull { it.batteryTempC } ?: 0f
        val avgCpu = if (samples.isNotEmpty()) samples.map { it.cpuUsagePercent }.average().toFloat() else 0f
        val avgRam = if (samples.isNotEmpty()) (samples.map { it.ramUsedMb }.average()).toLong() else 0L

        // Generate CSV rows for chart and export: timestamp,fps,frameTime,cpu,ram,temp
        val csvBuilder = StringBuilder("timestamp,fps,frame_time_ms,cpu_percent,ram_used_mb,battery_temp_c\n")
        samples.forEach { s ->
            csvBuilder.append("${s.timestamp},${"%.1f".format(s.fps)},${"%.2f".format(s.frameTimeMs)},${"%.1f".format(s.cpuUsagePercent)},${s.ramUsedMb},${"%.1f".format(s.batteryTempC)}\n")
        }

        val sessionEntity = SessionEntity(
            packageName = pkg,
            appName = name.ifBlank { pkg },
            startTime = startTime,
            endTime = endTime,
            durationSeconds = durationSeconds,
            modeUsed = mode.title,
            avgFps = avgFps,
            onePercentLowFps = onePercentLow,
            jankPercent = totalJankPercent,
            maxTempC = maxTemp,
            avgCpuUsage = avgCpu,
            avgRamUsedMb = avgRam,
            samplesCsv = csvBuilder.toString()
        )

        val id = database.sessionDao().insertSession(sessionEntity)
        sessionEntity.copy(id = id)
    }

    suspend fun getInstalledApps(): List<InstalledAppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val installed = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        installed.mapNotNull { appInfo ->
            try {
                val label = pm.getApplicationLabel(appInfo).toString()
                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val isGame = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    appInfo.category == ApplicationInfo.CATEGORY_GAME
                } else false

                InstalledAppInfo(
                    packageName = appInfo.packageName,
                    appName = label,
                    isGameCategory = isGame,
                    isSystemApp = isSystem
                )
            } catch (_: Exception) {
                null
            }
        }.sortedWith(compareBy({ !it.isGameCategory }, { it.isSystemApp }, { it.appName.lowercase() }))
    }

    suspend fun getBaselineSnapshot(): Triple<Int, String, Int> {
        val dnd = preferences.baselineDndFlow.firstOrNull() ?: 0
        val rr = preferences.baselineRefreshRateFlow.firstOrNull() ?: ""
        val bm = preferences.baselineBrightnessModeFlow.firstOrNull() ?: 1
        return Triple(dnd, rr, bm)
    }

    suspend fun saveBaselineSnapshot(dnd: Int, rr: String, bm: Int) {
        preferences.saveBaselineSnapshot(dnd, rr, bm)
    }

    private fun GameProfileEntity.toDomain(): GameProfile {
        val mode = try {
            PerformanceMode.valueOf(preferredMode)
        } catch (_: Exception) {
            PerformanceMode.PERFORMANCE
        }
        return GameProfile(
            packageName = packageName,
            appName = appName,
            preferredMode = mode,
            autoLaunchOverlay = autoLaunchOverlay,
            targetFps = targetFps,
            customNotes = customNotes,
            customOverlayEnabled = customOverlayEnabled,
            layoutPreset = layoutPreset,
            metricOrder = metricOrder,
            scalePercent = scalePercent,
            opacityPercent = opacityPercent,
            showFps = showFps,
            showFrameTime = showFrameTime,
            showCpu = showCpu,
            showRam = showRam,
            showTemp = showTemp,
            showBattery = showBattery,
            showGraph = showGraph
        )
    }

    private fun GameProfile.toEntity(): GameProfileEntity {
        return GameProfileEntity(
            packageName = packageName,
            appName = appName,
            preferredMode = preferredMode.name,
            autoLaunchOverlay = autoLaunchOverlay,
            targetFps = targetFps,
            customNotes = customNotes,
            customOverlayEnabled = customOverlayEnabled,
            layoutPreset = layoutPreset,
            metricOrder = metricOrder,
            scalePercent = scalePercent,
            opacityPercent = opacityPercent,
            showFps = showFps,
            showFrameTime = showFrameTime,
            showCpu = showCpu,
            showRam = showRam,
            showTemp = showTemp,
            showBattery = showBattery,
            showGraph = showGraph
        )
    }
}
