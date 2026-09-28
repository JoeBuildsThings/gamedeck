package com.example.model

data class GameProfile(
    val packageName: String,
    val appName: String,
    val preferredMode: PerformanceMode = PerformanceMode.PERFORMANCE,
    val autoLaunchOverlay: Boolean = true,
    val targetFps: Int = 60,
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
) {
    fun toOverlayConfig(baseConfig: OverlayConfig): OverlayConfig {
        if (!customOverlayEnabled) return baseConfig
        val order = metricOrder.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        return baseConfig.copy(
            layoutPreset = layoutPreset,
            metricOrder = if (order.isNotEmpty()) order else baseConfig.metricOrder,
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
