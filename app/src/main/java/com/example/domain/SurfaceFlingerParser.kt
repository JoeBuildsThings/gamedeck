package com.example.domain

data class FpsParseResult(
    val fps: Float,
    val frameTimeMs: Float,
    val refreshPeriodNs: Long,
    val jankPercent: Float,
    val onePercentLowFps: Float,
    val recentFrameDeltasMs: List<Float>
)

object SurfaceFlingerParser {

    private const val ONE_SECOND_NS = 1_000_000_000L
    private const val DEFAULT_REFRESH_PERIOD_NS = 16_666_667L // ~60 Hz

    /**
     * Parses the output of `dumpsys SurfaceFlinger --list` to find the most relevant
     * layer name for the given package name.
     */
    fun findLayerForPackage(surfaceFlingerListOutput: String, packageName: String): String? {
        if (packageName.isBlank() || surfaceFlingerListOutput.isBlank()) return null

        val lines = surfaceFlingerListOutput.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && it.contains(packageName, ignoreCase = true) }
            .toList()

        if (lines.isEmpty()) return null

        // 1. Prefer active SurfaceView layers
        val surfaceViewLayer = lines.firstOrNull { line ->
            line.contains("SurfaceView", ignoreCase = true) &&
                    !line.startsWith("Background for", ignoreCase = true)
        }
        if (surfaceViewLayer != null) return surfaceViewLayer

        // 2. Prefer main Activity / Window layer (exclude Dimmer, Background, Snapshot)
        val mainLayer = lines.firstOrNull { line ->
            !line.contains("Background for", ignoreCase = true) &&
                    !line.contains("Dimmer", ignoreCase = true) &&
                    !line.contains("Snapshot", ignoreCase = true) &&
                    !line.contains("BoundsAnimation", ignoreCase = true)
        }
        if (mainLayer != null) return mainLayer

        // 3. Fallback to any matching layer
        return lines.firstOrNull()
    }

    /**
     * Parses the output of `dumpsys SurfaceFlinger --latency <layer>`.
     *
     * Output structure:
     * Line 0: refresh_period_ns
     * Remaining lines (up to 128): <app_sample_time_ns> <ready_time_ns> <frame_present_time_ns>
     */
    fun parseLatencyOutput(rawOutput: String): FpsParseResult? {
        if (rawOutput.isBlank()) return null

        val lines = rawOutput.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (lines.isEmpty()) return null

        // First line is the display refresh period in nanoseconds
        val refreshPeriodNs = lines[0].toLongOrNull()?.takeIf { it > 0 } ?: DEFAULT_REFRESH_PERIOD_NS

        if (lines.size < 2) {
            return FpsParseResult(
                fps = 0f,
                frameTimeMs = refreshPeriodNs / 1_000_000f,
                refreshPeriodNs = refreshPeriodNs,
                jankPercent = 0f,
                onePercentLowFps = 0f,
                recentFrameDeltasMs = emptyList()
            )
        }

        // Parse present times
        val presentTimesNs = ArrayList<Long>(128)
        for (i in 1 until lines.size) {
            val parts = lines[i].split(Regex("\\s+"))
            if (parts.size >= 3) {
                // Column 2 is actual frame present time
                val presentTime = parts[2].toLongOrNull()
                // Valid presented frame: > 0 and not INT64_MAX (pending/dropped)
                if (presentTime != null && presentTime > 0L && presentTime != Long.MAX_VALUE && presentTime != 0x7fffffffffffffffL) {
                    presentTimesNs.add(presentTime)
                }
            }
        }

        if (presentTimesNs.isEmpty()) {
            return FpsParseResult(
                fps = 0f,
                frameTimeMs = refreshPeriodNs / 1_000_000f,
                refreshPeriodNs = refreshPeriodNs,
                jankPercent = 0f,
                onePercentLowFps = 0f,
                recentFrameDeltasMs = emptyList()
            )
        }

        // Sort ascending to guarantee chronological ordering
        presentTimesNs.sort()

        val latestPresentTime = presentTimesNs.last()
        val oneSecondAgo = latestPresentTime - ONE_SECOND_NS

        // Frames presented within the last second
        val framesInWindow = presentTimesNs.filter { it >= oneSecondAgo }
        val fps = framesInWindow.size.toFloat()

        // Calculate delta between successive presented frames
        val deltasMs = ArrayList<Float>()
        var jankCount = 0
        val jankThresholdNs = (refreshPeriodNs * 1.5).toLong()

        for (i in 1 until presentTimesNs.size) {
            val deltaNs = presentTimesNs[i] - presentTimesNs[i - 1]
            if (deltaNs in 1..250_000_000L) { // Filter unrealistic gaps (e.g. paused game)
                val ms = deltaNs / 1_000_000f
                deltasMs.add(ms)
                if (deltaNs > jankThresholdNs) {
                    jankCount++
                }
            }
        }

        val jankPercent = if (deltasMs.isNotEmpty()) {
            (jankCount.toFloat() / deltasMs.size) * 100f
        } else 0f

        val currentFrameTimeMs = if (deltasMs.isNotEmpty()) {
            deltasMs.last()
        } else {
            refreshPeriodNs / 1_000_000f
        }

        // Calculate 1% Low FPS: sort frame times ascending, find 99th percentile longest frame time
        val onePercentLowFps = if (deltasMs.isNotEmpty()) {
            val sortedDeltas = deltasMs.sorted()
            val p99Index = ((sortedDeltas.size - 1) * 0.99f).toInt().coerceIn(0, sortedDeltas.size - 1)
            val p99Ms = sortedDeltas[p99Index]
            if (p99Ms > 0.1f) (1000f / p99Ms).coerceAtMost(fps) else fps
        } else fps

        return FpsParseResult(
            fps = fps,
            frameTimeMs = currentFrameTimeMs,
            refreshPeriodNs = refreshPeriodNs,
            jankPercent = jankPercent,
            onePercentLowFps = onePercentLowFps,
            recentFrameDeltasMs = deltasMs.takeLast(120)
        )
    }
}
