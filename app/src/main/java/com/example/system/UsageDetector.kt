package com.example.system

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Process
import com.example.GameDeckApp
import com.example.model.PerformanceMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object UsageDetector {

    private var monitoringJob: Job? = null
    private var lastForegroundPackage: String? = null
    private var activeGamePackage: String? = null

    fun isUsagePermissionGranted(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun startMonitoring(context: Context, scope: CoroutineScope) {
        if (monitoringJob?.isActive == true) return

        monitoringJob = scope.launch(Dispatchers.Default) {
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            while (isActive) {
                try {
                    val isAutoDetect = GameDeckApp.instance.repository.isAutoDetectEnabledFlow.first()
                    if (isAutoDetect && isUsagePermissionGranted(context) && usageStatsManager != null) {
                        val endTime = System.currentTimeMillis()
                        val startTime = endTime - 3000L
                        val events = usageStatsManager.queryEvents(startTime, endTime)
                        val event = UsageEvents.Event()
                        var latestPackage: String? = null

                        while (events.hasNextEvent()) {
                            events.getNextEvent(event)
                            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                                latestPackage = event.packageName
                            }
                        }

                        if (!latestPackage.isNullOrBlank() && latestPackage != lastForegroundPackage) {
                            handleForegroundAppChanged(context, latestPackage)
                        }
                    }
                } catch (_: Throwable) {}

                delay(1500L)
            }
        }
    }

    fun stopMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
    }

    suspend fun handleForegroundAppChanged(context: Context, newPackage: String) {
        if (newPackage == context.packageName) return
        lastForegroundPackage = newPackage

        // Update telemetry target for accurate SurfaceFlinger layer resolution
        GameDeckApp.instance.telemetryManager.setForegroundPackage(newPackage)

        val profile = GameDeckApp.instance.repository.getProfile(newPackage)
        if (profile != null && profile.autoLaunchOverlay) {
            // A registered game came into foreground!
            if (activeGamePackage != newPackage) {
                activeGamePackage = newPackage

                // Apply game-specific overlay settings
                val baseConfig = GameDeckApp.instance.repository.overlayConfigFlow.first()
                val effectiveConfig = profile.toOverlayConfig(baseConfig)
                OverlayService.setGameSpecificOverlayConfig(effectiveConfig)

                // Auto show overlay
                OverlayService.startService(context)

                // Apply preferred mode
                applyGameMode(context, profile.preferredMode)

                // Start telemetry session
                GameDeckApp.instance.repository.startSession(
                    packageName = newPackage,
                    appName = profile.appName,
                    mode = profile.preferredMode
                )
                GameDeckApp.instance.repository.logAction(
                    "Auto Profile",
                    "Applied ${profile.preferredMode.title} and custom HUD layout for ${profile.appName}"
                )
            }
        } else if (activeGamePackage != null && activeGamePackage != newPackage) {
            // Exited the game!
            val exitedGame = activeGamePackage
            activeGamePackage = null

            // Revert game-specific overlay settings
            OverlayService.setGameSpecificOverlayConfig(null)

            // End session and save report
            GameDeckApp.instance.repository.endSession()

            // Restore Balanced mode
            applyGameMode(context, PerformanceMode.BALANCED)
            GameDeckApp.instance.repository.logAction(
                "Auto Profile Exit",
                "Exited game $exitedGame, restored default Balanced mode"
            )
        }
    }

    private suspend fun applyGameMode(context: Context, mode: PerformanceMode) {
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
            action.apply(context, GameDeckApp.instance.shizukuBridge)
        }
    }
}
