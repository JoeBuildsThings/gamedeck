package com.example.model

enum class PerformanceMode(
    val title: String,
    val description: String,
    val shortLabel: String
) {
    BALANCED(
        title = "Balanced",
        description = "Stock system behavior, baseline refresh rate, no aggressive background interference.",
        shortLabel = "BAL"
    ),
    PERFORMANCE(
        title = "Performance",
        description = "Enables Do Not Disturb, max refresh rate, and safely trims user-selected non-system apps.",
        shortLabel = "PERF"
    ),
    BATTERY_SAVER(
        title = "Battery Saver",
        description = "Conserves power with standard refresh rate and trims background services.",
        shortLabel = "SAVE"
    );

    fun next(): PerformanceMode {
        return when (this) {
            BALANCED -> PERFORMANCE
            PERFORMANCE -> BATTERY_SAVER
            BATTERY_SAVER -> BALANCED
        }
    }
}
