package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.OverlayConfig
import com.example.model.PerformanceMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "gamedeck_prefs")

class GameDeckPreferences(private val context: Context) {

    companion object {
        private val KEY_CURRENT_MODE = stringPreferencesKey("current_mode")
        private val KEY_OVERLAY_ENABLED = booleanPreferencesKey("overlay_enabled")
        private val KEY_POS_X = intPreferencesKey("overlay_pos_x")
        private val KEY_POS_Y = intPreferencesKey("overlay_pos_y")
        private val KEY_SCALE = intPreferencesKey("overlay_scale")
        private val KEY_OPACITY = intPreferencesKey("overlay_opacity")
        private val KEY_UPDATE_INTERVAL = longPreferencesKey("overlay_update_interval")

        private val KEY_LAYOUT_PRESET = stringPreferencesKey("overlay_layout_preset")
        private val KEY_METRIC_ORDER = stringPreferencesKey("overlay_metric_order")

        private val KEY_SHOW_FPS = booleanPreferencesKey("show_fps")
        private val KEY_SHOW_FRAME_TIME = booleanPreferencesKey("show_frame_time")
        private val KEY_SHOW_CPU = booleanPreferencesKey("show_cpu")
        private val KEY_SHOW_RAM = booleanPreferencesKey("show_ram")
        private val KEY_SHOW_TEMP = booleanPreferencesKey("show_temp")
        private val KEY_SHOW_BATTERY = booleanPreferencesKey("show_battery")
        private val KEY_SHOW_GRAPH = booleanPreferencesKey("show_graph")

        private val KEY_AUTO_DETECT = booleanPreferencesKey("auto_detect_games")
        private val KEY_WHITELISTED_KILL_PACKAGES = stringSetPreferencesKey("kill_whitelist_packages")

        // Panic baseline snapshot
        private val KEY_BASELINE_CAPTURED = booleanPreferencesKey("baseline_captured")
        private val KEY_BASELINE_DND = intPreferencesKey("baseline_dnd_filter")
        private val KEY_BASELINE_REFRESH_RATE = stringPreferencesKey("baseline_refresh_rate")
        private val KEY_BASELINE_BRIGHTNESS_MODE = intPreferencesKey("baseline_brightness_mode")
    }

    val currentModeFlow: Flow<PerformanceMode> = context.dataStore.data.map { prefs ->
        val name = prefs[KEY_CURRENT_MODE] ?: PerformanceMode.BALANCED.name
        try {
            PerformanceMode.valueOf(name)
        } catch (_: Exception) {
            PerformanceMode.BALANCED
        }
    }

    val isOverlayEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_OVERLAY_ENABLED] ?: false
    }

    val overlayConfigFlow: Flow<OverlayConfig> = context.dataStore.data.map { prefs ->
        val orderRaw = prefs[KEY_METRIC_ORDER] ?: "FPS,FRAME_TIME,CPU,RAM,TEMP,GRAPH"
        val order = orderRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }

        OverlayConfig(
            posX = prefs[KEY_POS_X] ?: 30,
            posY = prefs[KEY_POS_Y] ?: 240,
            isExpanded = false,
            scalePercent = prefs[KEY_SCALE] ?: 100,
            opacityPercent = prefs[KEY_OPACITY] ?: 90,
            updateIntervalMs = prefs[KEY_UPDATE_INTERVAL] ?: 1000L,
            layoutPreset = prefs[KEY_LAYOUT_PRESET] ?: "DEFAULT",
            metricOrder = if (order.isNotEmpty()) order else listOf("FPS", "FRAME_TIME", "CPU", "RAM", "TEMP", "GRAPH"),
            showFps = prefs[KEY_SHOW_FPS] ?: true,
            showFrameTime = prefs[KEY_SHOW_FRAME_TIME] ?: true,
            showCpu = prefs[KEY_SHOW_CPU] ?: true,
            showRam = prefs[KEY_SHOW_RAM] ?: true,
            showTemp = prefs[KEY_SHOW_TEMP] ?: true,
            showBattery = prefs[KEY_SHOW_BATTERY] ?: false,
            showGraph = prefs[KEY_SHOW_GRAPH] ?: true
        )
    }

    val isAutoDetectEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_AUTO_DETECT] ?: true
    }

    val killWhitelistFlow: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[KEY_WHITELISTED_KILL_PACKAGES] ?: emptySet()
    }

    suspend fun setPerformanceMode(mode: PerformanceMode) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CURRENT_MODE] = mode.name
        }
    }

    suspend fun setOverlayEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_OVERLAY_ENABLED] = enabled
        }
    }

    suspend fun saveOverlayPosition(x: Int, y: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_POS_X] = x
            prefs[KEY_POS_Y] = y
        }
    }

    suspend fun updateOverlayConfig(config: OverlayConfig) {
        context.dataStore.edit { prefs ->
            prefs[KEY_POS_X] = config.posX
            prefs[KEY_POS_Y] = config.posY
            prefs[KEY_SCALE] = config.scalePercent
            prefs[KEY_OPACITY] = config.opacityPercent
            prefs[KEY_UPDATE_INTERVAL] = config.updateIntervalMs
            prefs[KEY_LAYOUT_PRESET] = config.layoutPreset
            prefs[KEY_METRIC_ORDER] = config.metricOrder.joinToString(",")
            prefs[KEY_SHOW_FPS] = config.showFps
            prefs[KEY_SHOW_FRAME_TIME] = config.showFrameTime
            prefs[KEY_SHOW_CPU] = config.showCpu
            prefs[KEY_SHOW_RAM] = config.showRam
            prefs[KEY_SHOW_TEMP] = config.showTemp
            prefs[KEY_SHOW_BATTERY] = config.showBattery
            prefs[KEY_SHOW_GRAPH] = config.showGraph
        }
    }

    suspend fun setAutoDetectEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_AUTO_DETECT] = enabled
        }
    }

    suspend fun setKillWhitelist(packages: Set<String>) {
        context.dataStore.edit { prefs ->
            prefs[KEY_WHITELISTED_KILL_PACKAGES] = packages
        }
    }

    suspend fun saveBaselineSnapshot(dndFilter: Int, refreshRate: String, brightnessMode: Int) {
        context.dataStore.edit { prefs ->
            if (prefs[KEY_BASELINE_CAPTURED] != true) {
                prefs[KEY_BASELINE_CAPTURED] = true
                prefs[KEY_BASELINE_DND] = dndFilter
                prefs[KEY_BASELINE_REFRESH_RATE] = refreshRate
                prefs[KEY_BASELINE_BRIGHTNESS_MODE] = brightnessMode
            }
        }
    }

    val baselineDndFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_BASELINE_DND] ?: 0
    }

    val baselineRefreshRateFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_BASELINE_REFRESH_RATE] ?: ""
    }

    val baselineBrightnessModeFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_BASELINE_BRIGHTNESS_MODE] ?: 1
    }
}
