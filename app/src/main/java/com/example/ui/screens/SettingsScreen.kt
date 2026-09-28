package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ActivityLogEntity
import com.example.data.repository.InstalledAppInfo
import com.example.domain.ModeEngine
import com.example.model.OverlayConfig
import com.example.ui.MainViewModel
import com.example.ui.components.OverlayLayoutEditor
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.SurfaceBorderDark
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    config: OverlayConfig,
    installedApps: List<InstalledAppInfo>,
    killWhitelist: Set<String>,
    activityLogs: List<ActivityLogEntity>
) {
    var showWhitelistManager by remember { mutableStateOf(false) }
    var showActivityLogs by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "SETTINGS & LOGS",
                color = NeonCyan,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
            Text(
                text = "Customizable HUD layouts, update rate, safe whitelist & panic restore",
                color = TextSecondary,
                fontSize = 13.sp
            )
        }

        // Panic Restore Button Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = WarningAmber.copy(alpha = 0.1f)),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(WarningAmber))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = WarningAmber)
                        Text(
                            text = "Panic Restore",
                            color = WarningAmber,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Instantly reverts Do Not Disturb, display refresh rate, and all applied settings to the baseline captured at initial launch.",
                        color = TextPrimary,
                        fontSize = 12.sp
                    )
                    Button(
                        onClick = { viewModel.panicRestore() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WarningAmber,
                            contentColor = BackgroundDark
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Revert to Safe Baseline", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Customizable Overlay HUD Layout Editor
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
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = NeonCyan)
                        Text(
                            text = "HUD Layout & Metrics Customizer",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Update Frequency Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Update Frequency", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text("Hard rule: < 2% CPU overhead", color = TextMuted, fontSize = 11.sp)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (config.updateIntervalMs == 1000L) NeonCyan.copy(alpha = 0.2f) else SurfaceVariantDark)
                                    .clickable { viewModel.updateOverlayConfig(config.copy(updateIntervalMs = 1000L)) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("1 Hz", color = if (config.updateIntervalMs == 1000L) NeonCyan else TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (config.updateIntervalMs == 500L) NeonCyan.copy(alpha = 0.2f) else SurfaceVariantDark)
                                    .clickable { viewModel.updateOverlayConfig(config.copy(updateIntervalMs = 500L)) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("2 Hz", color = if (config.updateIntervalMs == 500L) NeonCyan else TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Reordering, Sizing, Toggles & Live Preview
                    OverlayLayoutEditor(
                        config = config,
                        onConfigChange = { updated -> viewModel.updateOverlayConfig(updated) }
                    )
                }
            }
        }

        // Third-Party Safe Whitelist Manager Button
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showWhitelistManager = !showWhitelistManager },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorderDark))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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
                            Icon(Icons.Default.Security, contentDescription = null, tint = NeonGreen)
                            Column {
                                Text("Apps Allowed to Stop in Performance Mode", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("${killWhitelist.size} user third-party apps selected", color = TextSecondary, fontSize = 12.sp)
                            }
                        }
                        Text(if (showWhitelistManager) "Hide" else "Manage", color = NeonCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    Text(
                        text = "Safety Guardrails: System packages (Android, Google, MediaTek, Transsion/HiOS) are hard-blocked and can never be stopped.",
                        color = TextMuted,
                        fontSize = 11.sp
                    )

                    AnimatedVisibility(visible = showWhitelistManager) {
                        val safeApps = remember(installedApps) {
                            installedApps.filter { !it.isSystemApp && ModeEngine.isSafeThirdPartyPackage(it.packageName) }
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (safeApps.isEmpty()) {
                                Text("No third-party user apps found.", color = TextSecondary, fontSize = 12.sp)
                            } else {
                                safeApps.forEach { appInfo ->
                                    val isChecked = killWhitelist.contains(appInfo.packageName)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(SurfaceVariantDark)
                                            .clickable { viewModel.toggleKillWhitelistPackage(appInfo.packageName) }
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(appInfo.appName, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            Text(appInfo.packageName, color = TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                        }
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = { viewModel.toggleKillWhitelistPackage(appInfo.packageName) },
                                            colors = CheckboxDefaults.colors(checkedColor = NeonCyan, checkmarkColor = BackgroundDark)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Activity Log Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showActivityLogs = !showActivityLogs },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorderDark))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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
                            Icon(Icons.Default.FormatListBulleted, contentDescription = null, tint = NeonPurple)
                            Text("Activity Log", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (activityLogs.isNotEmpty()) {
                                IconButton(onClick = { viewModel.clearActivityLogs() }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "Clear Logs", tint = TextMuted, modifier = Modifier.size(16.dp))
                                }
                            }
                            Text(if (showActivityLogs) "Collapse" else "View (${activityLogs.size})", color = NeonCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    AnimatedVisibility(visible = showActivityLogs) {
                        val dateFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (activityLogs.isEmpty()) {
                                Text("No activity recorded yet.", color = TextSecondary, fontSize = 12.sp)
                            } else {
                                activityLogs.take(30).forEach { log ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(SurfaceVariantDark)
                                            .padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text(dateFormat.format(Date(log.timestamp)), color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                                Text(log.actionType, color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Text(log.details, color = TextPrimary, fontSize = 11.sp)
                                        }
                                        if (log.reverted) {
                                            Text("REVERTED", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // About & Safety Architecture Notice
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
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "About GameDeck",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Native rootless Android performance optimizer and telemetry HUD. Built with Kotlin & Jetpack Compose. Optimized for MediaTek, Infinix Smart 9, and all modern Android 10-14 devices.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
