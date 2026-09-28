package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val packageName: String,
    val appName: String,
    val startTime: Long,
    val endTime: Long,
    val durationSeconds: Long,
    val modeUsed: String,
    val avgFps: Float,
    val onePercentLowFps: Float,
    val jankPercent: Float,
    val maxTempC: Float,
    val avgCpuUsage: Float,
    val avgRamUsedMb: Long,
    val samplesCsv: String = "" // Serialized rows: timestamp,fps,frameTime,cpu,ram,temp
)
