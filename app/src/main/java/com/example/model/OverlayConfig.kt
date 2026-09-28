package com.example.model

enum class MetricItem(val id: String, val displayName: String, val shortDesc: String) {
    FPS("FPS", "FPS Counter", "Hardware SurfaceFlinger FPS or display refresh rate"),
    FRAME_TIME("FRAME_TIME", "Frame Time (ms)", "Present timestamp interval delta"),
    CPU("CPU", "CPU Usage (%)", "Proc stat calculated utilization"),
    RAM("RAM", "RAM Usage (MB)", "Used memory from MemoryInfo"),
    TEMP("TEMP", "Battery Temp (°C)", "Tenths of degree from BatteryManager"),
    BATTERY("BATTERY", "Battery Level (%)", "Remaining device charge percentage"),
    GRAPH("GRAPH", "Rolling Frame Graph", "120-sample micro frame time canvas chart")
}

data class OverlayConfig(
    val posX: Int = 20,
    val posY: Int = 200,
    val isExpanded: Boolean = false,
    val scalePercent: Int = 100, // 70 to 150
    val opacityPercent: Int = 90, // 40 to 100
    val updateIntervalMs: Long = 1000L,
    val layoutPreset: String = "DEFAULT",
    val metricOrder: List<String> = listOf("FPS", "FRAME_TIME", "CPU", "RAM", "TEMP", "GRAPH"),
    val showFps: Boolean = true,
    val showFrameTime: Boolean = true,
    val showCpu: Boolean = true,
    val showRam: Boolean = true,
    val showTemp: Boolean = true,
    val showBattery: Boolean = false,
    val showGraph: Boolean = true
) {
    companion object {
        fun presetMinimalist() = OverlayConfig(
            layoutPreset = "MINIMALIST",
            scalePercent = 90,
            opacityPercent = 85,
            metricOrder = listOf("FPS"),
            showFps = true,
            showFrameTime = false,
            showCpu = false,
            showRam = false,
            showTemp = false,
            showBattery = false,
            showGraph = false
        )

        fun presetFull() = OverlayConfig(
            layoutPreset = "FULL",
            scalePercent = 105,
            opacityPercent = 95,
            metricOrder = listOf("FPS", "FRAME_TIME", "CPU", "RAM", "TEMP", "BATTERY", "GRAPH"),
            showFps = true,
            showFrameTime = true,
            showCpu = true,
            showRam = true,
            showTemp = true,
            showBattery = true,
            showGraph = true
        )

        fun presetCompactGamer() = OverlayConfig(
            layoutPreset = "COMPACT_GAMER",
            scalePercent = 100,
            opacityPercent = 90,
            metricOrder = listOf("FPS", "FRAME_TIME", "TEMP", "GRAPH"),
            showFps = true,
            showFrameTime = true,
            showCpu = false,
            showRam = false,
            showTemp = true,
            showBattery = false,
            showGraph = true
        )

        fun presetThermalBattery() = OverlayConfig(
            layoutPreset = "THERMAL_SAVER",
            scalePercent = 95,
            opacityPercent = 90,
            metricOrder = listOf("TEMP", "BATTERY", "CPU", "FPS"),
            showFps = true,
            showFrameTime = false,
            showCpu = true,
            showRam = false,
            showTemp = true,
            showBattery = true,
            showGraph = false
        )
    }
}
