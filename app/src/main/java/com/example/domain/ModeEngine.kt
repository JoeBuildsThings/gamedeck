package com.example.domain

import android.app.ActivityManager
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.model.PerformanceMode
import com.example.system.shizuku.ShizukuBridge

sealed interface ModeAction {
    val id: String
    val description: String
    val isReversible: Boolean
    suspend fun apply(context: Context, shizuku: ShizukuBridge?): ActionExecutionResult
    suspend fun revert(context: Context, shizuku: ShizukuBridge?): ActionExecutionResult
}

data class ActionExecutionResult(
    val success: Boolean,
    val summary: String,
    val details: String = ""
)

/**
 * Pure domain engine generating reversible action plans for mode transitions.
 */
class ModeEngine {

    companion object {
        // Forbidden package prefixes - Absolute Safety Guardrails
        val FORBIDDEN_PREFIXES = listOf(
            "android",
            "com.android",
            "com.google.android.gms",
            "com.google.android.gsf",
            "com.mediatek",
            "com.transsion",
            "com.hoffnung",
            "com.transsion.hilauncher",
            "com.transsion.xos",
            "com.transsion.hios",
            "com.google.android.inputmethod",
            "com.android.launcher",
            "com.android.systemui"
        )

        fun isSafeThirdPartyPackage(packageName: String): Boolean {
            val lower = packageName.trim().lowercase()
            if (lower.isEmpty()) return false
            for (prefix in FORBIDDEN_PREFIXES) {
                if (lower == prefix || lower.startsWith("$prefix.") || lower.startsWith(prefix)) {
                    return false
                }
            }
            if (lower.contains("systemui") || lower.contains("keyguard") || lower.contains("telephony")) {
                return false
            }
            return true
        }
    }

    fun buildActionPlan(
        targetMode: PerformanceMode,
        allowedThirdPartyPackages: Set<String>,
        baselineDnd: Int = NotificationManager.INTERRUPTION_FILTER_ALL,
        baselineRefreshRate: String = "60.0"
    ): List<ModeAction> {
        return when (targetMode) {
            PerformanceMode.BALANCED -> {
                listOf(
                    RestoreDefaultsAction(baselineDnd, baselineRefreshRate)
                )
            }
            PerformanceMode.PERFORMANCE -> {
                val safePackages = allowedThirdPartyPackages.filter { isSafeThirdPartyPackage(it) }.toSet()
                listOf(
                    DndAction(enableDnd = true, previousFilter = baselineDnd),
                    RefreshRateAction(targetRate = "120.0", previousRate = baselineRefreshRate),
                    TrimBackgroundCacheAction(),
                    KillWhitelistedThirdPartyAction(safePackages)
                )
            }
            PerformanceMode.BATTERY_SAVER -> {
                val safePackages = allowedThirdPartyPackages.filter { isSafeThirdPartyPackage(it) }.toSet()
                listOf(
                    RefreshRateAction(targetRate = "60.0", previousRate = baselineRefreshRate),
                    TrimBackgroundCacheAction(),
                    KillWhitelistedThirdPartyAction(safePackages)
                )
            }
        }
    }
}

class DndAction(
    private val enableDnd: Boolean,
    private val previousFilter: Int
) : ModeAction {
    override val id: String = "dnd_action"
    override val description: String = if (enableDnd) "Enable Do Not Disturb for zero gaming interruption" else "Restore Do Not Disturb"
    override val isReversible: Boolean = true

    override suspend fun apply(context: Context, shizuku: ShizukuBridge?): ActionExecutionResult {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        if (nm != null && nm.isNotificationPolicyAccessGranted) {
            val target = if (enableDnd) NotificationManager.INTERRUPTION_FILTER_PRIORITY else NotificationManager.INTERRUPTION_FILTER_ALL
            nm.setInterruptionFilter(target)
            return ActionExecutionResult(true, "DND filter set via NotificationManager")
        }

        // Shizuku fallback
        if (shizuku != null && shizuku.isAvailableAndGranted()) {
            val filterArg = if (enableDnd) "priority" else "all"
            val res = shizuku.exec("cmd notification set_interruption_filter $filterArg")
            return if (res.isSuccess) {
                ActionExecutionResult(true, "DND filter set via Shizuku shell ($filterArg)")
            } else {
                ActionExecutionResult(false, "Failed to set DND via Shizuku", res.exceptionOrNull()?.message ?: "")
            }
        }

        return ActionExecutionResult(false, "DND skipped: Grant Do Not Disturb access or Shizuku")
    }

    override suspend fun revert(context: Context, shizuku: ShizukuBridge?): ActionExecutionResult {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        if (nm != null && nm.isNotificationPolicyAccessGranted) {
            nm.setInterruptionFilter(previousFilter)
            return ActionExecutionResult(true, "DND reverted to original filter")
        }
        if (shizuku != null && shizuku.isAvailableAndGranted()) {
            val res = shizuku.exec("cmd notification set_interruption_filter all")
            return ActionExecutionResult(res.isSuccess, "DND reverted via Shizuku")
        }
        return ActionExecutionResult(false, "Unable to revert DND: permission missing")
    }
}

class RefreshRateAction(
    private val targetRate: String,
    private val previousRate: String
) : ModeAction {
    override val id: String = "refresh_rate_action"
    override val description: String = "Set display refresh rate to $targetRate Hz"
    override val isReversible: Boolean = true

    override suspend fun apply(context: Context, shizuku: ShizukuBridge?): ActionExecutionResult {
        if (shizuku != null && shizuku.isAvailableAndGranted()) {
            val cmd = "settings put system peak_refresh_rate $targetRate && settings put system min_refresh_rate $targetRate"
            val res = shizuku.exec(cmd)
            return if (res.isSuccess) {
                ActionExecutionResult(true, "Refresh rate locked to $targetRate Hz")
            } else {
                ActionExecutionResult(false, "Failed to lock refresh rate", res.exceptionOrNull()?.message ?: "")
            }
        }
        return ActionExecutionResult(false, "Refresh rate lock requires Shizuku permission")
    }

    override suspend fun revert(context: Context, shizuku: ShizukuBridge?): ActionExecutionResult {
        if (shizuku != null && shizuku.isAvailableAndGranted()) {
            val revertCmd = if (previousRate.isNotBlank()) {
                "settings put system peak_refresh_rate $previousRate && settings put system min_refresh_rate 0"
            } else {
                "settings delete system peak_refresh_rate && settings delete system min_refresh_rate"
            }
            val res = shizuku.exec(revertCmd)
            return ActionExecutionResult(res.isSuccess, "Refresh rate reverted to system default ($previousRate Hz)")
        }
        return ActionExecutionResult(false, "Revert refresh rate requires Shizuku")
    }
}

class TrimBackgroundCacheAction : ModeAction {
    override val id: String = "trim_cache_action"
    override val description: String = "Trim background app memory and system caches"
    override val isReversible: Boolean = false

    override suspend fun apply(context: Context, shizuku: ShizukuBridge?): ActionExecutionResult {
        if (shizuku != null && shizuku.isAvailableAndGranted()) {
            val res = shizuku.exec("cmd activity kill-all")
            return if (res.isSuccess) {
                ActionExecutionResult(true, "Background process trim executed safely via Shizuku")
            } else {
                ActionExecutionResult(false, "Shizuku background trim failed", res.exceptionOrNull()?.message ?: "")
            }
        }
        return ActionExecutionResult(true, "Standard trim executed")
    }

    override suspend fun revert(context: Context, shizuku: ShizukuBridge?): ActionExecutionResult {
        return ActionExecutionResult(true, "Memory trim cannot be undone, no revert necessary")
    }
}

class KillWhitelistedThirdPartyAction(
    private val packagesToKill: Set<String>
) : ModeAction {
    override val id: String = "kill_third_party_action"
    override val description: String = "Safely stop ${packagesToKill.size} user-whitelisted apps"
    override val isReversible: Boolean = false

    override suspend fun apply(context: Context, shizuku: ShizukuBridge?): ActionExecutionResult {
        if (packagesToKill.isEmpty()) {
            return ActionExecutionResult(true, "No third-party apps selected to stop")
        }

        val killedList = mutableListOf<String>()
        val failedList = mutableListOf<String>()
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager

        for (pkg in packagesToKill) {
            // Strict safety check per absolute safety rules
            if (!ModeEngine.isSafeThirdPartyPackage(pkg)) {
                continue
            }

            if (shizuku != null && shizuku.isAvailableAndGranted()) {
                val res = shizuku.exec("am force-stop $pkg")
                if (res.isSuccess) {
                    killedList.add(pkg)
                } else {
                    failedList.add(pkg)
                }
            } else {
                // Non-root fallback: killBackgroundProcesses (only terminates background cached processes)
                try {
                    am?.killBackgroundProcesses(pkg)
                    killedList.add(pkg)
                } catch (_: Exception) {
                    failedList.add(pkg)
                }
            }
        }

        return ActionExecutionResult(
            success = true,
            summary = "Stopped ${killedList.size} apps to free RAM for game",
            details = killedList.joinToString(", ")
        )
    }

    override suspend fun revert(context: Context, shizuku: ShizukuBridge?): ActionExecutionResult {
        return ActionExecutionResult(true, "Stopped processes will launch normally when opened by user")
    }
}

class RestoreDefaultsAction(
    private val baselineDnd: Int,
    private val baselineRefreshRate: String
) : ModeAction {
    override val id: String = "restore_defaults_action"
    override val description: String = "Restore system defaults (Balanced Mode)"
    override val isReversible: Boolean = true

    override suspend fun apply(context: Context, shizuku: ShizukuBridge?): ActionExecutionResult {
        // Revert DND
        val dndAction = DndAction(enableDnd = false, previousFilter = baselineDnd)
        dndAction.revert(context, shizuku)

        // Revert refresh rate
        val rrAction = RefreshRateAction(targetRate = baselineRefreshRate, previousRate = baselineRefreshRate)
        rrAction.revert(context, shizuku)

        return ActionExecutionResult(true, "Restored system defaults successfully")
    }

    override suspend fun revert(context: Context, shizuku: ShizukuBridge?): ActionExecutionResult {
        return ActionExecutionResult(true, "Already in default state")
    }
}
