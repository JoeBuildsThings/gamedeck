package com.example.ui

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.GameDeckApp
import com.example.data.local.ActivityLogEntity
import com.example.data.local.SessionEntity
import com.example.data.repository.InstalledAppInfo
import com.example.domain.TrimBackgroundCacheAction
import com.example.model.GameProfile
import com.example.model.OverlayConfig
import com.example.model.PerformanceMode
import com.example.model.TelemetryMetrics
import com.example.system.OverlayService
import com.example.system.UsageDetector
import com.example.system.shizuku.ShizukuBridge
import com.example.system.shizuku.ShizukuStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PermissionItemState(
    val id: String,
    val title: String,
    val description: String,
    val isGranted: Boolean,
    val isCritical: Boolean = true
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as GameDeckApp
    private val repo = app.repository
    private val shizukuBridge = app.shizukuBridge

    val currentMode: StateFlow<PerformanceMode> = repo.currentModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PerformanceMode.BALANCED)

    val isOverlayEnabled: StateFlow<Boolean> = repo.isOverlayEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val overlayConfig: StateFlow<OverlayConfig> = repo.overlayConfigFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), OverlayConfig())

    val gameProfiles: StateFlow<List<GameProfile>> = repo.profilesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sessions: StateFlow<List<SessionEntity>> = repo.sessionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activityLogs: StateFlow<List<ActivityLogEntity>> = repo.activityLogsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val killWhitelist: StateFlow<Set<String>> = repo.killWhitelistFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val shizukuStatus: StateFlow<ShizukuStatus> = shizukuBridge.status

    val liveTelemetry: StateFlow<TelemetryMetrics> = app.telemetryManager.telemetryFlow

    private val _permissionsState = MutableStateFlow<List<PermissionItemState>>(emptyList())
    val permissionsState: StateFlow<List<PermissionItemState>> = _permissionsState.asStateFlow()

    private val _installedApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppInfo>> = _installedApps.asStateFlow()

    private val _selectedSession = MutableStateFlow<SessionEntity?>(null)
    val selectedSession: StateFlow<SessionEntity?> = _selectedSession.asStateFlow()

    private val _ramCleanResult = MutableStateFlow<String?>(null)
    val ramCleanResult: StateFlow<String?> = _ramCleanResult.asStateFlow()

    init {
        refreshPermissions()
        loadInstalledApps()
        UsageDetector.startMonitoring(app, viewModelScope)
        app.telemetryManager.start(viewModelScope)
    }

    fun refreshPermissions() {
        val context = getApplication<Application>()
        shizukuBridge.updateStatus()

        val hasOverlay = Settings.canDrawOverlays(context)
        val hasUsage = UsageDetector.isUsagePermissionGranted(context)
        val hasNotification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val hasBatteryOptimizationExemption = powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val hasDndAccess = notificationManager?.isNotificationPolicyAccessGranted ?: false

        _permissionsState.value = listOf(
            PermissionItemState(
                id = "overlay",
                title = "Display Over Other Apps",
                description = "Required to render the floating telemetry HUD during gameplay.",
                isGranted = hasOverlay,
                isCritical = true
            ),
            PermissionItemState(
                id = "usage",
                title = "Usage Access",
                description = "Enables automatic game detection to launch profiles seamlessly.",
                isGranted = hasUsage,
                isCritical = true
            ),
            PermissionItemState(
                id = "notification",
                title = "Notification Permission",
                description = "Keeps overlay service running reliably without being killed.",
                isGranted = hasNotification,
                isCritical = false
            ),
            PermissionItemState(
                id = "battery",
                title = "Battery Optimization Exemption",
                description = "Prevents aggressive Android OEM battery killers from stopping HUD.",
                isGranted = hasBatteryOptimizationExemption,
                isCritical = false
            ),
            PermissionItemState(
                id = "dnd",
                title = "Do Not Disturb Access",
                description = "Allows Performance Mode to silence notifications while gaming.",
                isGranted = hasDndAccess,
                isCritical = false
            )
        )
    }

    fun loadInstalledApps() {
        viewModelScope.launch {
            _installedApps.value = repo.getInstalledApps()
        }
    }

    fun toggleOverlay() {
        viewModelScope.launch {
            val nextState = !isOverlayEnabled.value
            repo.setOverlayEnabled(nextState)
            if (nextState) {
                OverlayService.startService(getApplication())
            } else {
                OverlayService.stopService(getApplication())
            }
        }
    }

    fun setMode(mode: PerformanceMode) {
        viewModelScope.launch {
            repo.setMode(mode)
            val (dnd, rr, _) = repo.getBaselineSnapshot()
            val plan = app.modeEngine.buildActionPlan(
                targetMode = mode,
                allowedThirdPartyPackages = killWhitelist.value,
                baselineDnd = dnd,
                baselineRefreshRate = rr
            )
            for (action in plan) {
                val res = action.apply(getApplication(), shizukuBridge)
                repo.logAction(action.id, "${action.description}: ${res.summary}", action.isReversible)
            }
        }
    }

    fun requestShizukuPermission() {
        shizukuBridge.requestPermission()
    }

    fun addGameProfile(appInfo: InstalledAppInfo) {
        viewModelScope.launch {
            val newProfile = GameProfile(
                packageName = appInfo.packageName,
                appName = appInfo.appName,
                preferredMode = PerformanceMode.PERFORMANCE,
                autoLaunchOverlay = true
            )
            repo.saveProfile(newProfile)
            repo.logAction("Profile Added", "Configured auto profile for ${appInfo.appName}")
        }
    }

    fun updateGameProfile(profile: GameProfile) {
        viewModelScope.launch {
            repo.saveProfile(profile)
        }
    }

    fun deleteGameProfile(packageName: String) {
        viewModelScope.launch {
            repo.deleteProfile(packageName)
        }
    }

    fun updateOverlayConfig(config: OverlayConfig) {
        viewModelScope.launch {
            repo.updateOverlayConfig(config)
            app.telemetryManager.setUpdateInterval(config.updateIntervalMs)
        }
    }

    fun toggleKillWhitelistPackage(packageName: String) {
        viewModelScope.launch {
            val current = killWhitelist.value.toMutableSet()
            if (current.contains(packageName)) {
                current.remove(packageName)
            } else {
                if (com.example.domain.ModeEngine.isSafeThirdPartyPackage(packageName)) {
                    current.add(packageName)
                }
            }
            repo.setKillWhitelist(current)
        }
    }

    fun performRamClean() {
        viewModelScope.launch {
            val action = TrimBackgroundCacheAction()
            val res = action.apply(getApplication(), shizukuBridge)
            _ramCleanResult.value = res.summary
            repo.logAction("RAM Clean", res.summary)
        }
    }

    fun clearRamCleanResult() {
        _ramCleanResult.value = null
    }

    fun panicRestore() {
        viewModelScope.launch {
            val (dnd, rr, _) = repo.getBaselineSnapshot()
            val plan = app.modeEngine.buildActionPlan(
                targetMode = PerformanceMode.BALANCED,
                allowedThirdPartyPackages = emptySet(),
                baselineDnd = dnd,
                baselineRefreshRate = rr
            )
            for (action in plan) {
                action.apply(getApplication(), shizukuBridge)
            }
            repo.setMode(PerformanceMode.BALANCED)
            repo.markAllLogsReverted()
            repo.logAction("Panic Restore", "All display & DND settings restored to safe baseline")
        }
    }

    fun selectSession(session: SessionEntity?) {
        _selectedSession.value = session
    }

    fun deleteSession(session: SessionEntity) {
        viewModelScope.launch {
            repo.deleteSession(session)
            if (_selectedSession.value?.id == session.id) {
                _selectedSession.value = null
            }
        }
    }

    fun clearAllSessions() {
        viewModelScope.launch {
            repo.clearAllSessions()
            _selectedSession.value = null
        }
    }

    fun clearActivityLogs() {
        viewModelScope.launch {
            repo.clearLogs()
        }
    }

    fun exportSessionCsv(session: SessionEntity, context: Context) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, "GameDeck Session Telemetry - ${session.appName}")
            putExtra(Intent.EXTRA_TEXT, session.samplesCsv)
        }
        val chooser = Intent.createChooser(shareIntent, "Export Session CSV")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
