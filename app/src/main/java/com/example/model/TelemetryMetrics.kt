package com.example.model

data class TelemetryMetrics(
    val fps: Float = 0f,
    val frameTimeMs: Float = 0f,
    val cpuUsagePercent: Float = 0f,
    val ramUsedMb: Long = 0L,
    val ramTotalMb: Long = 0L,
    val batteryTempC: Float = 0f,
    val batteryPercent: Int = 100,
    val isRealFps: Boolean = false,
    val refreshPeriodNs: Long = 16_666_667L,
    val jankPercent: Float = 0f,
    val onePercentLowFps: Float = 0f,
    val frameHistory: List<Float> = emptyList(), // Last 120 frame time samples in ms
    val timestamp: Long = System.currentTimeMillis()
)
