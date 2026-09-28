package com.example.ui.screens

import android.os.Build
import android.os.Environment
import android.os.StatFs
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TelemetryMetrics
import com.example.ui.MainViewModel
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
fun ToolsScreen(
    viewModel: MainViewModel,
    telemetry: TelemetryMetrics,
    ramCleanResult: String?
) {
    val storageStats = remember {
        try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availableBlocks = stat.availableBlocksLong
            val totalGb = (totalBlocks * blockSize) / (1024 * 1024 * 1024)
            val freeGb = (availableBlocks * blockSize) / (1024 * 1024 * 1024)
            val usedGb = totalGb - freeGb
            Triple(usedGb, freeGb, totalGb)
        } catch (_: Exception) {
            Triple(0L, 0L, 0L)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "SYSTEM TOOLS",
                color = NeonCyan,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
            Text(
                text = "Safe hardware diagnostics & RAM cache optimization",
                color = TextSecondary,
                fontSize = 13.sp
            )
        }

        // Safe RAM Cleaner Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorderDark))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(NeonCyan.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CleaningServices, contentDescription = null, tint = NeonCyan)
                            }
                            Column {
                                Text(
                                    text = "Safe RAM Trimmer",
                                    color = TextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Trims non-essential background cache",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    // RAM Meter
                    val ramFraction = if (telemetry.ramTotalMb > 0) telemetry.ramUsedMb.toFloat() / telemetry.ramTotalMb.toFloat() else 0f
                    MetricProgressBar(
                        label = "RAM LOAD",
                        valueText = "${telemetry.ramUsedMb} MB used / ${(telemetry.ramTotalMb - telemetry.ramUsedMb).coerceAtLeast(0)} MB free",
                        fraction = ramFraction.coerceIn(0f, 1f),
                        color = NeonPurple
                    )

                    Button(
                        onClick = { viewModel.performRamClean() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCyan,
                            contentColor = BackgroundDark
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Trim Background Memory", fontWeight = FontWeight.Bold)
                    }

                    if (ramCleanResult != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceVariantDark)
                                .padding(10.dp)
                        ) {
                            Text(
                                text = ramCleanResult,
                                color = NeonGreen,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // Thermal & Battery Health
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorderDark))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (telemetry.batteryTempC > 40f) PerformanceRed.copy(alpha = 0.2f) else NeonGreen.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Thermostat,
                                contentDescription = null,
                                tint = if (telemetry.batteryTempC > 40f) PerformanceRed else NeonGreen
                            )
                        }
                        Column {
                            Text(
                                text = "Thermal & Battery Monitor",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = when {
                                    telemetry.batteryTempC > 43f -> "Thermal Throttling Likely (High)"
                                    telemetry.batteryTempC > 38f -> "Warm (Normal Gaming)"
                                    else -> "Optimal Temperature"
                                },
                                color = when {
                                    telemetry.batteryTempC > 43f -> PerformanceRed
                                    telemetry.batteryTempC > 38f -> WarningAmber
                                    else -> NeonGreen
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ToolStatItem(label = "BATTERY TEMP", value = "${"%.1f".format(telemetry.batteryTempC)}°C")
                        ToolStatItem(label = "CHARGE LEVEL", value = "${telemetry.batteryPercent}%")
                        ToolStatItem(label = "REFRESH PERIOD", value = "${"%.1f".format(telemetry.refreshPeriodNs / 1_000_000f)} ms")
                    }
                }
            }
        }

        // Hardware & SoC Specifications
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorderDark))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(SurfaceVariantDark),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Memory, contentDescription = null, tint = NeonCyan)
                        }
                        Column {
                            Text(
                                text = "Device & Platform Info",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${Build.MANUFACTURER.uppercase()} • ${Build.MODEL}",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Android OS Version", color = TextSecondary, fontSize = 13.sp)
                        Text(text = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "CPU Cores", color = TextSecondary, fontSize = 13.sp)
                        Text(text = "${Runtime.getRuntime().availableProcessors()} Cores", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Architecture", color = TextSecondary, fontSize = 13.sp)
                        Text(text = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    if (storageStats.third > 0L) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Internal Storage", color = TextSecondary, fontSize = 13.sp)
                            Text(text = "${storageStats.first} GB used / ${storageStats.third} GB total", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun ToolStatItem(label: String, value: String) {
    Column {
        Text(text = label, color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        Text(text = value, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
    }
}
