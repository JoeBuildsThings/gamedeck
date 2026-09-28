package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PerformanceMode
import com.example.model.TelemetryMetrics
import com.example.system.shizuku.ShizukuStatus
import com.example.ui.MainViewModel
import com.example.ui.PermissionItemState
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
fun HomeScreen(
    viewModel: MainViewModel,
    shizukuStatus: ShizukuStatus,
    currentMode: PerformanceMode,
    isOverlayEnabled: Boolean,
    telemetry: TelemetryMetrics,
    permissions: List<PermissionItemState>
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            // Hero Title Banner
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "GAMEDECK",
                        color = NeonCyan,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "In-Game Performance HUD & Booster",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }

                IconButton(onClick = { viewModel.refreshPermissions() }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Status",
                        tint = NeonCyan
                    )
                }
            }
        }

        // Master Overlay Control Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(
                        if (isOverlayEnabled) NeonCyan else SurfaceBorderDark
                    )
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(if (isOverlayEnabled) NeonCyan.copy(alpha = 0.2f) else SurfaceVariantDark),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = null,
                                tint = if (isOverlayEnabled) NeonCyan else TextSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Floating HUD Overlay",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isOverlayEnabled) "HUD Active on screen" else "HUD Disabled",
                                color = if (isOverlayEnabled) NeonGreen else TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Switch(
                        checked = isOverlayEnabled,
                        onCheckedChange = { viewModel.toggleOverlay() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = BackgroundDark,
                            checkedTrackColor = NeonCyan,
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = SurfaceVariantDark
                        )
                    )
                }
            }
        }

        // Shizuku Status Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when (shizukuStatus) {
                        ShizukuStatus.READY -> NeonCyan.copy(alpha = 0.08f)
                        ShizukuStatus.PERMISSION_DENIED -> WarningAmber.copy(alpha = 0.08f)
                        else -> SurfaceDark
                    }
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(
                        when (shizukuStatus) {
                            ShizukuStatus.READY -> NeonCyan
                            ShizukuStatus.PERMISSION_DENIED -> WarningAmber
                            else -> SurfaceBorderDark
                        }
                    )
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = when (shizukuStatus) {
                                    ShizukuStatus.READY -> Icons.Default.CheckCircle
                                    ShizukuStatus.PERMISSION_DENIED -> Icons.Default.Warning
                                    else -> Icons.Default.Security
                                },
                                contentDescription = null,
                                tint = when (shizukuStatus) {
                                    ShizukuStatus.READY -> NeonGreen
                                    ShizukuStatus.PERMISSION_DENIED -> WarningAmber
                                    else -> TextSecondary
                                },
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Shizuku Shell Service",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Status Tag
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    when (shizukuStatus) {
                                        ShizukuStatus.READY -> NeonGreen.copy(alpha = 0.2f)
                                        ShizukuStatus.PERMISSION_DENIED -> WarningAmber.copy(alpha = 0.2f)
                                        else -> SurfaceVariantDark
                                    }
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = when (shizukuStatus) {
                                    ShizukuStatus.READY -> "CONNECTED"
                                    ShizukuStatus.PERMISSION_DENIED -> "AUTH REQUIRED"
                                    ShizukuStatus.DEAD -> "NOT RUNNING"
                                    ShizukuStatus.NOT_INSTALLED -> "NOT INSTALLED"
                                },
                                color = when (shizukuStatus) {
                                    ShizukuStatus.READY -> NeonGreen
                                    ShizukuStatus.PERMISSION_DENIED -> WarningAmber
                                    else -> TextSecondary
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Text(
                        text = when (shizukuStatus) {
                            ShizukuStatus.READY -> "Privileged features unlocked: Hardware SurfaceFlinger latency parsing for true FPS, display refresh rate locking, and safe background process trimming."
                            ShizukuStatus.PERMISSION_DENIED -> "Shizuku is running on device, but GameDeck requires user authorization to query SurfaceFlinger and display accurate FPS."
                            ShizukuStatus.DEAD -> "Shizuku service is not running. Start it via Wireless Debugging in the Shizuku app for true FPS and performance tuning."
                            ShizukuStatus.NOT_INSTALLED -> "Shizuku is not installed. GameDeck runs safely in non-root mode, showing display refresh rates as fallback."
                        },
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )

                    when (shizukuStatus) {
                        ShizukuStatus.PERMISSION_DENIED -> {
                            Button(
                                onClick = { viewModel.requestShizukuPermission() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = WarningAmber,
                                    contentColor = BackgroundDark
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Grant Shizuku Permission", fontWeight = FontWeight.Bold)
                            }
                        }
                        ShizukuStatus.DEAD -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        (context.applicationContext as? com.example.GameDeckApp)?.shizukuBridge?.launchShizukuApp(context)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = NeonCyan,
                                        contentColor = BackgroundDark
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Start Shizuku", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                FilledTonalButton(
                                    onClick = {
                                        (context.applicationContext as? com.example.GameDeckApp)?.shizukuBridge?.openWirelessDebuggingSettings(context)
                                    },
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = SurfaceVariantDark,
                                        contentColor = NeonCyan
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Wireless Debug", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        ShizukuStatus.NOT_INSTALLED -> {
                            Button(
                                onClick = {
                                    (context.applicationContext as? com.example.GameDeckApp)?.shizukuBridge?.launchShizukuApp(context)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SurfaceVariantDark,
                                    contentColor = NeonCyan
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Get Shizuku (Google Play)", fontWeight = FontWeight.Bold)
                            }
                        }
                        ShizukuStatus.READY -> {
                            // Active status feedback
                        }
                    }
                }
            }
        }

        // Performance Mode Selector
        item {
            Text(
                text = "PERFORMANCE MODE",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PerformanceMode.values().forEach { mode ->
                    val isSelected = currentMode == mode
                    val accent = when (mode) {
                        PerformanceMode.BALANCED -> NeonCyan
                        PerformanceMode.PERFORMANCE -> PerformanceRed
                        PerformanceMode.BATTERY_SAVER -> NeonGreen
                    }

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.setMode(mode) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) accent.copy(alpha = 0.15f) else SurfaceDark
                        ),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(
                                if (isSelected) accent else SurfaceBorderDark
                            )
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = mode.shortLabel,
                                color = if (isSelected) accent else TextSecondary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = mode.title,
                                color = if (isSelected) TextPrimary else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = currentMode.description,
                color = TextMuted,
                fontSize = 12.sp
            )
        }

        // Live Telemetry Preview Card
        item {
            Text(
                text = "LIVE TELEMETRY PREVIEW",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorderDark)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${telemetry.fps.toInt()}",
                                color = if (telemetry.isRealFps) NeonCyan else WarningAmber,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "FPS",
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (telemetry.isRealFps) "Hardware Latency" else "Display Fallback",
                                    color = if (telemetry.isRealFps) NeonGreen else WarningAmber,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${"%.1f".format(telemetry.frameTimeMs)} ms",
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Frame Time",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Progress indicators
                    MetricProgressBar(
                        label = "CPU USAGE",
                        valueText = "${telemetry.cpuUsagePercent.toInt()}%",
                        fraction = (telemetry.cpuUsagePercent / 100f).coerceIn(0f, 1f),
                        color = if (telemetry.cpuUsagePercent > 80f) PerformanceRed else NeonCyan
                    )

                    val ramPct = if (telemetry.ramTotalMb > 0) telemetry.ramUsedMb.toFloat() / telemetry.ramTotalMb.toFloat() else 0f
                    MetricProgressBar(
                        label = "RAM UTILIZATION",
                        valueText = "${telemetry.ramUsedMb} / ${telemetry.ramTotalMb} MB",
                        fraction = ramPct.coerceIn(0f, 1f),
                        color = NeonPurple
                    )

                    val tempPct = (telemetry.batteryTempC / 50f).coerceIn(0f, 1f)
                    MetricProgressBar(
                        label = "BATTERY TEMPERATURE",
                        valueText = "${"%.1f".format(telemetry.batteryTempC)}°C • ${telemetry.batteryPercent}%",
                        fraction = tempPct,
                        color = if (telemetry.batteryTempC > 40f) PerformanceRed else NeonGreen
                    )
                }
            }
        }

        // Permission Checklist
        item {
            Text(
                text = "PERMISSIONS & CAPABILITIES",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        items(permissions) { perm ->
            PermissionRowCard(
                perm = perm,
                onGrant = {
                    openPermissionSetting(context, perm.id)
                }
            )
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun MetricProgressBar(
    label: String,
    valueText: String,
    fraction: Float,
    color: androidx.compose.ui.graphics.Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Text(text = valueText, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(SurfaceVariantDark)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
        }
    }
}

@Composable
fun PermissionRowCard(
    perm: PermissionItemState,
    onGrant: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorderDark)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = if (perm.isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (perm.isGranted) NeonGreen else WarningAmber,
                    modifier = Modifier
                        .size(18.dp)
                        .padding(top = 2.dp)
                )

                Column {
                    Text(
                        text = perm.title,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = perm.description,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (!perm.isGranted) {
                FilledTonalButton(
                    onClick = onGrant,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = SurfaceVariantDark,
                        contentColor = NeonCyan
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Fix", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Text(
                    text = "Granted",
                    color = NeonGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

fun openPermissionSetting(context: Context, permId: String) {
    try {
        val intent = when (permId) {
            "overlay" -> Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
            "usage" -> Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            "notification" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                }
            } else {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
            }
            "battery" -> Intent(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:${context.packageName}")
            )
            "dnd" -> Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            else -> Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (_: Exception) {
        val fallback = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}")
        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        context.startActivity(fallback)
    }
}
