package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_profiles")
data class GameProfileEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val preferredMode: String,
    val autoLaunchOverlay: Boolean,
    val targetFps: Int,
    val customNotes: String = "",
    // Custom overlay layout settings per game profile
    val customOverlayEnabled: Boolean = false,
    val layoutPreset: String = "DEFAULT",
    val metricOrder: String = "FPS,FRAME_TIME,CPU,RAM,TEMP,GRAPH",
    val scalePercent: Int = 100,
    val opacityPercent: Int = 90,
    val showFps: Boolean = true,
    val showFrameTime: Boolean = true,
    val showCpu: Boolean = true,
    val showRam: Boolean = true,
    val showTemp: Boolean = true,
    val showBattery: Boolean = false,
    val showGraph: Boolean = true
)
