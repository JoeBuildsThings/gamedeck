package com.joebuildsthings.gamedeck.screens

import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.joebuildsthings.gamedeck.*
import com.joebuildsthings.gamedeck.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    onRequestOverlay: () -> Unit,
    onToggleOverlay: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val activeMode by GameDeckEngine.mode.collectAsState()
    val overlayRunning by GameDeckEngine.overlayRunning.collectAsState()
    val stats by GameDeckEngine.telemetry.collectAsState()

    var hasPermission by remember { mutableStateOf(Settings.canDrawOverlays(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = Settings.canDrawOverlays(context)
                GameDeckEngine.updateTelemetry(context, stats.fps)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        while (true) {
            if (!overlayRunning) {
                GameDeckEngine.updateTelemetry(context, 60)
            }
            delay(2000L)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(BgDark)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        MechaFrame(
            accentColor = activeMode.accent,
            bgColor = PanelDark,
            cutDp = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            Modifier
                                .size(8.dp)
                                .background(activeMode.accent)
                        )
                        Text(
                            text = "GAMEDECK // ARMORY CORE",
                            color = activeMode.accent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        text = "XARENA x REDMAGIC TACTICAL CONSOLE",
                        color = MutedGrey,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Box(
                    Modifier
                        .background(activeMode.dimBg)
                        .border(1.dp, activeMode.accent)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = activeMode.title,
                        color = activeMode.accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        SectionHeader(
            leftTitle = "01 // ENGINE PROFILE",
            rightStatus = activeMode.codeName,
            accentColor = activeMode.accent
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            EngineMode.values().forEach { mode ->
                MechaModeCard(
                    mode = mode,
                    selected = activeMode == mode,
                    modifier = Modifier.weight(1f),
                    onClick = { GameDeckEngine.setMode(mode) }
                )
            }
        }

        SectionHeader(
            leftTitle = "02 // IN-GAME FLOATING HUD",
            rightStatus = if (overlayRunning) "HUD ACTIVE" else if (hasPermission) "STANDBY" else "LOCKED",
            accentColor = if (overlayRunning) SuccessGreen else if (hasPermission) CyberCyan else WarnAmber
        )

        MechaFrame(
            accentColor = if (overlayRunning) activeMode.accent else LineBright,
            bgColor = PanelDark,
            cutDp = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "TACTICAL OVERLAY SERVICE",
                            color = InkLight,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = when {
                                !hasPermission -> "PERMISSION REQUIRED: TAP SWITCH TO GRANT"
                                overlayRunning -> "RUNNING: TAP PILL IN-GAME TO EXPAND WINGS"
                                else -> "READY: DEPLOY FLOATING TELEMETRY BLADE"
                            },
                            color = when {
                                !hasPermission -> WarnAmber
                                overlayRunning -> SuccessGreen
                                else -> MutedGrey
                            },
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Switch(
                        checked = overlayRunning,
                        onCheckedChange = { wantOn ->
                            if (!hasPermission) {
                                onRequestOverlay()
                            } else {
                                GameDeckEngine.setOverlayRunning(wantOn)
                                onToggleOverlay(wantOn)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = BgDark,
                            checkedTrackColor = activeMode.accent,
                            uncheckedThumbColor = MutedGrey,
                            uncheckedTrackColor = LineDark
                        )
                    )
                }

                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(BgDark)
                        .border(1.dp, LineDark)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "HUD BLADE PREVIEW",
                            color = MutedGrey,
                            fontSize = 9.sp,
                            letterSpacing = 1.5.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Tap in-game to open Reticle + Boost",
                            color = LineBright,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    MechaFrame(
                        accentColor = activeMode.accent,
                        bgColor = PanelDark,
                        cutDp = 6.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                Modifier
                                    .width(3.dp)
                                    .height(12.dp)
                                    .background(activeMode.accent)
                            )
                            Text(
                                text = "${stats.fps} FPS | ${stats.tempC}°C",
                                color = InkLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        SectionHeader(
            leftTitle = "03 // REAL-TIME TELEMETRY",
            rightStatus = "${stats.cpuFreqMhz} MHz",
            accentColor = activeMode.accent
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MechaTelemetryCard(
                label = "FRAME RATE",
                value = "${stats.fps}",
                unit = "FPS",
                subText = "TARGET: 60+ HZ",
                loadPercent = ((stats.fps / 60f) * 100f).toInt().coerceIn(10, 100),
                accent = activeMode.accent,
                modifier = Modifier.weight(1f)
            )
            MechaTelemetryCard(
                label = "CPU CORES",
                value = "${stats.cpuLoad}%",
                unit = "${stats.cpuFreqMhz}MHz",
                subText = "GOVERNOR LOAD",
                loadPercent = stats.cpuLoad,
                accent = activeMode.accent,
                modifier = Modifier.weight(1f)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MechaTelemetryCard(
                label = "MEMORY ALLOC",
                value = "${stats.ramLoad}%",
                unit = "${stats.ramUsedGb}/${stats.ramTotalGb}G",
                subText = "TAP TO PURGE RAM",
                loadPercent = stats.ramLoad,
                accent = CyberCyan,
                modifier = Modifier
                    .weight(1f)
                    .clickable { GameDeckEngine.purgeBackgroundMemory(context) }
            )
            MechaTelemetryCard(
                label = "THERMAL CORE",
                value = "${stats.tempC}°C",
                unit = "BAT",
                subText = "SAFE LIMIT: 45°C",
                loadPercent = ((stats.tempC.toFloatOrNull() ?: 36f) * 2f).toInt().coerceIn(20, 100),
                accent = WarnAmber,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SectionHeader(
    leftTitle: String,
    rightStatus: String,
    accentColor: Color
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = leftTitle,
            color = MutedGrey,
            fontSize = 10.sp,
            letterSpacing = 1.8.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = rightStatus,
            color = accentColor,
            fontSize = 10.sp,
            letterSpacing = 1.5.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun MechaModeCard(
    mode: EngineMode,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    MechaFrame(
        accentColor = if (selected) mode.accent else LineDark,
        bgColor = if (selected) mode.dimBg else PanelDark,
        cutDp = 10.dp,
        modifier = modifier
            .height(98.dp)
            .clickable { onClick() }
    ) {
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(8.dp)
                        .background(if (selected) mode.accent else LineBright)
                )
                Text(
                    text = if (selected) "ON" else "OFF",
                    color = if (selected) mode.accent else MutedGrey,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = mode.title,
                    color = if (selected) InkLight else MutedGrey,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = mode.subLabel.uppercase(),
                    color = if (selected) mode.accent else MutedGrey,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun MechaTelemetryCard(
    label: String,
    value: String,
    unit: String,
    subText: String,
    loadPercent: Int,
    accent: Color,
    modifier: Modifier = Modifier
) {
    MechaFrame(
        accentColor = accent,
        bgColor = PanelDark,
        cutDp = 10.dp,
        modifier = modifier
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    color = MutedGrey,
                    fontSize = 9.sp,
                    letterSpacing = 1.2.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = unit,
                    color = accent,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }

            Text(
                text = value,
                color = InkLight,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )

            SegmentedLoadBar(
                percent = loadPercent,
                accentColor = accent,
                segments = 12,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = subText,
                color = MutedGrey,
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
