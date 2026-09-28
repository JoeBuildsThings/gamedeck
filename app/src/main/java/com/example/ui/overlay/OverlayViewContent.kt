package com.example.ui.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.OverlayConfig
import com.example.model.PerformanceMode
import com.example.model.TelemetryMetrics
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.GameDeckTheme
import com.example.ui.theme.GraphJankColor
import com.example.ui.theme.GraphLineColor
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.PerformanceRed
import com.example.ui.theme.SurfaceBorderDark
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber

@Composable
fun OverlayViewContent(
    metrics: TelemetryMetrics,
    config: OverlayConfig,
    currentMode: PerformanceMode,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onSelectMode: (PerformanceMode) -> Unit,
    onRamClean: () -> Unit,
    onPanicRestore: () -> Unit,
    onOpenApp: () -> Unit,
    onCloseOverlay: () -> Unit
) {
    GameDeckTheme {
        val opacity = (config.opacityPercent / 100f).coerceIn(0.4f, 1f)
        val hudScale = (config.scalePercent / 100f).coerceIn(0.7f, 1.5f)

        Column(
            modifier = Modifier
                .widthIn(max = 400.dp)
                .alpha(opacity),
            horizontalAlignment = Alignment.Start
        ) {
            // Compact Draggable HUD Pill
            Surface(
                modifier = Modifier
                    .scale(hudScale)
                    .clip(RoundedCornerShape(20.dp))
                    .border(
                        width = 1.dp,
                        color = when (currentMode) {
                            PerformanceMode.PERFORMANCE -> PerformanceRed
                            PerformanceMode.BATTERY_SAVER -> NeonGreen
                            PerformanceMode.BALANCED -> NeonCyan
                        },
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onToggleExpand
                    ),
                color = BackgroundDark.copy(alpha = 0.92f),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Mode Tag is always visible as anchor
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                when (currentMode) {
                                    PerformanceMode.PERFORMANCE -> PerformanceRed.copy(alpha = 0.2f)
                                    PerformanceMode.BATTERY_SAVER -> NeonGreen.copy(alpha = 0.2f)
                                    PerformanceMode.BALANCED -> NeonCyan.copy(alpha = 0.2f)
                                }
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = currentMode.shortLabel,
                            color = when (currentMode) {
                                PerformanceMode.PERFORMANCE -> PerformanceRed
                                PerformanceMode.BATTERY_SAVER -> NeonGreen
                                PerformanceMode.BALANCED -> NeonCyan
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Dynamically ordered metrics based on customizable layout
                    config.metricOrder.forEach { metricKey ->
                        when (metricKey) {
                            "FPS" -> {
                                if (config.showFps) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "${metrics.fps.toInt()}",
                                            color = if (metrics.isRealFps) NeonCyan else WarningAmber,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = "FPS",
                                            color = TextSecondary,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (!metrics.isRealFps) {
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .clip(CircleShape)
                                                    .background(WarningAmber)
                                            )
                                        }
                                    }
                                }
                            }
                            "FRAME_TIME" -> {
                                if (config.showFrameTime) {
                                    Text(
                                        text = "${"%.1f".format(metrics.frameTimeMs)}ms",
                                        color = TextPrimary,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                            "CPU" -> {
                                if (config.showCpu) {
                                    Text(
                                        text = "CPU ${metrics.cpuUsagePercent.toInt()}%",
                                        color = if (metrics.cpuUsagePercent > 80f) PerformanceRed else TextSecondary,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                            "RAM" -> {
                                if (config.showRam) {
                                    Text(
                                        text = "${metrics.ramUsedMb}M",
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                            "TEMP" -> {
                                if (config.showTemp && metrics.batteryTempC > 0f) {
                                    Text(
                                        text = "${metrics.batteryTempC.toInt()}°C",
                                        color = if (metrics.batteryTempC > 42f) PerformanceRed else TextSecondary,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                            "BATTERY" -> {
                                if (config.showBattery) {
                                    Text(
                                        text = "${metrics.batteryPercent}%",
                                        color = if (metrics.batteryPercent < 20) PerformanceRed else NeonGreen,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                            "GRAPH" -> {
                                if (config.showGraph && metrics.frameHistory.isNotEmpty()) {
                                    FrameTimeCanvasGraph(
                                        frameTimes = metrics.frameHistory,
                                        modifier = Modifier
                                            .width(55.dp)
                                            .height(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Expand / Collapse Chevron indicator
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand HUD",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Expanded Quick Panel
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark.copy(alpha = 0.98f)),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorderDark))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Header row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "GAMEDECK PANEL",
                                color = NeonCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = onOpenApp,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Launch,
                                        contentDescription = "Open App",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                IconButton(
                                    onClick = onCloseOverlay,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close Overlay",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        // Mode Selector Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            PerformanceMode.values().forEach { mode ->
                                val isSelected = currentMode == mode
                                val color = when (mode) {
                                    PerformanceMode.BALANCED -> NeonCyan
                                    PerformanceMode.PERFORMANCE -> PerformanceRed
                                    PerformanceMode.BATTERY_SAVER -> NeonGreen
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) color.copy(alpha = 0.25f) else SurfaceVariantDark)
                                        .border(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) color else SurfaceBorderDark,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { onSelectMode(mode) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = mode.title,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) color else TextSecondary
                                    )
                                }
                            }
                        }

                        // FPS Source status
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceVariantDark)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (metrics.isRealFps) NeonGreen else WarningAmber)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (metrics.isRealFps) "Shizuku SurfaceFlinger Latency (Real FPS)" else "FPS unavailable (Display fallback). Grant Shizuku for real FPS.",
                                color = if (metrics.isRealFps) TextSecondary else WarningAmber,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.SansSerif
                            )
                        }

                        // Detailed telemetry metrics grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TelemetryMiniStat(label = "1% LOW", value = "${"%.0f".format(metrics.onePercentLowFps)} FPS")
                            TelemetryMiniStat(label = "JANK", value = "${"%.1f".format(metrics.jankPercent)}%")
                            TelemetryMiniStat(label = "RAM FREE", value = "${(metrics.ramTotalMb - metrics.ramUsedMb).coerceAtLeast(0)} MB")
                            TelemetryMiniStat(label = "BATT TEMP", value = "${metrics.batteryTempC.toInt()}°C")
                        }

                        // Quick Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
                                onClick = onRamClean,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = SurfaceVariantDark,
                                    contentColor = NeonCyan
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Trim RAM", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            FilledTonalButton(
                                onClick = onPanicRestore,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = SurfaceVariantDark,
                                    contentColor = WarningAmber
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Panic Revert", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TelemetryMiniStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        Text(text = value, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun FrameTimeCanvasGraph(
    frameTimes: List<Float>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (frameTimes.isEmpty()) return@Canvas

        val w = size.width
        val h = size.height
        val count = frameTimes.size
        val stepX = w / (count - 1).coerceAtLeast(1)

        val targetMaxMs = 33.3f // 30 FPS boundary
        val path = Path()

        frameTimes.forEachIndexed { i, ms ->
            val clampedMs = ms.coerceIn(0f, 50f)
            val x = i * stepX
            val y = h - (clampedMs / targetMaxMs * h).coerceIn(0f, h)

            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }

            // Draw jank spikes in red
            if (ms > 25f) {
                drawCircle(
                    color = GraphJankColor,
                    radius = 1.5f,
                    center = Offset(x, y)
                )
            }
        }

        drawPath(
            path = path,
            color = GraphLineColor,
            style = Stroke(width = 1.5f)
        )
    }
}
