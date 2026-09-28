package com.example.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MetricItem
import com.example.model.OverlayConfig
import com.example.model.PerformanceMode
import com.example.model.TelemetryMetrics
import com.example.ui.overlay.FrameTimeCanvasGraph
import com.example.ui.theme.BackgroundDark
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
fun OverlayLayoutEditor(
    config: OverlayConfig,
    onConfigChange: (OverlayConfig) -> Unit,
    sampleMetrics: TelemetryMetrics = TelemetryMetrics(
        fps = 60f,
        frameTimeMs = 16.6f,
        cpuUsagePercent = 42f,
        ramUsedMb = 2380,
        ramTotalMb = 4096,
        batteryTempC = 36.5f,
        batteryPercent = 88,
        isRealFps = true,
        frameHistory = listOf(16.5f, 16.7f, 16.6f, 17.2f, 16.4f, 16.8f, 16.6f, 26.5f, 16.5f, 16.7f)
    )
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Live HUD Pill Preview Box
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = BackgroundDark),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorderDark))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "LIVE OVERLAY PREVIEW",
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val previewScale = (config.scalePercent / 100f).coerceIn(0.7f, 1.4f)
                    Surface(
                        modifier = Modifier
                            .scale(previewScale)
                            .clip(RoundedCornerShape(20.dp))
                            .border(1.dp, NeonCyan, RoundedCornerShape(20.dp)),
                        color = BackgroundDark.copy(alpha = (config.opacityPercent / 100f).coerceIn(0.4f, 1f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(NeonCyan.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("PERF", color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }

                            config.metricOrder.forEach { metricKey ->
                                when (metricKey) {
                                    "FPS" -> if (config.showFps) {
                                        Text("60 FPS", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
                                    }
                                    "FRAME_TIME" -> if (config.showFrameTime) {
                                        Text("16.6ms", color = TextPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                    }
                                    "CPU" -> if (config.showCpu) {
                                        Text("CPU 42%", color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                    }
                                    "RAM" -> if (config.showRam) {
                                        Text("2380M", color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                    }
                                    "TEMP" -> if (config.showTemp) {
                                        Text("36°C", color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                    }
                                    "BATTERY" -> if (config.showBattery) {
                                        Text("88%", color = NeonGreen, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                    }
                                    "GRAPH" -> if (config.showGraph) {
                                        FrameTimeCanvasGraph(
                                            frameTimes = sampleMetrics.frameHistory,
                                            modifier = Modifier.width(45.dp).height(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Layout Preset Selector
        Text(
            text = "PRESET LAYOUTS",
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PresetChip("Default", isSelected = config.layoutPreset == "DEFAULT") {
                onConfigChange(
                    config.copy(
                        layoutPreset = "DEFAULT",
                        scalePercent = 100,
                        opacityPercent = 90,
                        metricOrder = listOf("FPS", "FRAME_TIME", "CPU", "RAM", "TEMP", "GRAPH"),
                        showFps = true,
                        showFrameTime = true,
                        showCpu = true,
                        showRam = true,
                        showTemp = true,
                        showBattery = false,
                        showGraph = true
                    )
                )
            }
            PresetChip("Minimal", isSelected = config.layoutPreset == "MINIMALIST") {
                onConfigChange(OverlayConfig.presetMinimalist().copy(posX = config.posX, posY = config.posY))
            }
            PresetChip("Full", isSelected = config.layoutPreset == "FULL") {
                onConfigChange(OverlayConfig.presetFull().copy(posX = config.posX, posY = config.posY))
            }
            PresetChip("Compact", isSelected = config.layoutPreset == "COMPACT_GAMER") {
                onConfigChange(OverlayConfig.presetCompactGamer().copy(posX = config.posX, posY = config.posY))
            }
            PresetChip("Thermal", isSelected = config.layoutPreset == "THERMAL_SAVER") {
                onConfigChange(OverlayConfig.presetThermalBattery().copy(posX = config.posX, posY = config.posY))
            }
        }

        // Sliders: Scale & Opacity
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("HUD Scale", color = TextSecondary, fontSize = 12.sp)
                Text("${config.scalePercent}%", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = config.scalePercent.toFloat(),
                onValueChange = { onConfigChange(config.copy(scalePercent = it.toInt(), layoutPreset = "CUSTOM")) },
                valueRange = 70f..150f,
                colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan, inactiveTrackColor = SurfaceVariantDark)
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("HUD Opacity", color = TextSecondary, fontSize = 12.sp)
                Text("${config.opacityPercent}%", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = config.opacityPercent.toFloat(),
                onValueChange = { onConfigChange(config.copy(opacityPercent = it.toInt(), layoutPreset = "CUSTOM")) },
                valueRange = 40f..100f,
                colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan, inactiveTrackColor = SurfaceVariantDark)
            )
        }

        // Metrics Reordering & Visibility Manager
        Text(
            text = "REARRANGE & TOGGLE METRICS",
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )

        val allMetricItems = listOf(
            MetricItem.FPS,
            MetricItem.FRAME_TIME,
            MetricItem.CPU,
            MetricItem.RAM,
            MetricItem.TEMP,
            MetricItem.BATTERY,
            MetricItem.GRAPH
        )

        // Sort items by current order
        val orderedKeys = config.metricOrder.toMutableList()
        allMetricItems.forEach { item ->
            if (!orderedKeys.contains(item.id)) orderedKeys.add(item.id)
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            orderedKeys.forEachIndexed { index, metricKey ->
                val item = allMetricItems.firstOrNull { it.id == metricKey } ?: return@forEachIndexed
                val isVisible = when (item) {
                    MetricItem.FPS -> config.showFps
                    MetricItem.FRAME_TIME -> config.showFrameTime
                    MetricItem.CPU -> config.showCpu
                    MetricItem.RAM -> config.showRam
                    MetricItem.TEMP -> config.showTemp
                    MetricItem.BATTERY -> config.showBattery
                    MetricItem.GRAPH -> config.showGraph
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceVariantDark)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = isVisible,
                            onCheckedChange = { checked ->
                                val updated = when (item) {
                                    MetricItem.FPS -> config.copy(showFps = checked, layoutPreset = "CUSTOM")
                                    MetricItem.FRAME_TIME -> config.copy(showFrameTime = checked, layoutPreset = "CUSTOM")
                                    MetricItem.CPU -> config.copy(showCpu = checked, layoutPreset = "CUSTOM")
                                    MetricItem.RAM -> config.copy(showRam = checked, layoutPreset = "CUSTOM")
                                    MetricItem.TEMP -> config.copy(showTemp = checked, layoutPreset = "CUSTOM")
                                    MetricItem.BATTERY -> config.copy(showBattery = checked, layoutPreset = "CUSTOM")
                                    MetricItem.GRAPH -> config.copy(showGraph = checked, layoutPreset = "CUSTOM")
                                }
                                onConfigChange(updated)
                            },
                            colors = CheckboxDefaults.colors(checkedColor = NeonCyan, checkmarkColor = BackgroundDark)
                        )
                        Column {
                            Text(item.displayName, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(item.shortDesc, color = TextMuted, fontSize = 10.sp)
                        }
                    }

                    // Move Up / Move Down buttons
                    Row {
                        IconButton(
                            onClick = {
                                if (index > 0) {
                                    val newOrder = orderedKeys.toMutableList()
                                    val temp = newOrder[index]
                                    newOrder[index] = newOrder[index - 1]
                                    newOrder[index - 1] = temp
                                    onConfigChange(config.copy(metricOrder = newOrder, layoutPreset = "CUSTOM"))
                                }
                            },
                            enabled = index > 0,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.ArrowUpward,
                                contentDescription = "Move Up",
                                tint = if (index > 0) TextSecondary else TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                if (index < orderedKeys.size - 1) {
                                    val newOrder = orderedKeys.toMutableList()
                                    val temp = newOrder[index]
                                    newOrder[index] = newOrder[index + 1]
                                    newOrder[index + 1] = temp
                                    onConfigChange(config.copy(metricOrder = newOrder, layoutPreset = "CUSTOM"))
                                }
                            },
                            enabled = index < orderedKeys.size - 1,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.ArrowDownward,
                                contentDescription = "Move Down",
                                tint = if (index < orderedKeys.size - 1) TextSecondary else TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PresetChip(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) NeonCyan.copy(alpha = 0.25f) else SurfaceVariantDark)
            .border(1.dp, if (isSelected) NeonCyan else SurfaceBorderDark, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(
            text = name,
            color = if (isSelected) NeonCyan else TextSecondary,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
