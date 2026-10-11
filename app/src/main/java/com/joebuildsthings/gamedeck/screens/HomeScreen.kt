package com.joebuildsthings.gamedeck.screens

import android.content.Context
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.joebuildsthings.gamedeck.ui.theme.*

private data class Mode(
    val name: String,
    val blurb: String,
    val color: androidx.compose.ui.graphics.Color,
)

private val modes = listOf(
    Mode("Balanced", "Everyday", WarnAmber),
    Mode("Performance", "Max power", AccentRed),
    Mode("Battery", "Save juice", SuccessGreen),
)

@Composable
fun HomeScreen(
    onRequestOverlay: () -> Unit,
    onToggleOverlay: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    var selected by remember { mutableIntStateOf(0) }
    var overlayOn by remember { mutableStateOf(false) }
    var hasPermission by remember { mutableStateOf(Settings.canDrawOverlays(context)) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        SectionLabel("Active mode")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            modes.forEachIndexed { i, mode ->
                ModeCard(
                    mode = mode,
                    selected = selected == i,
                    modifier = Modifier.weight(1f),
                    onClick = { selected = i }
                )
            }
        }

        SectionLabel("Overlay")
        Surface(
            color = PanelDark,
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, LineDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Floating HUD", color = InkLight, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text(
                            if (hasPermission) "Permission granted" else "Permission required",
                            color = if (hasPermission) SuccessGreen else WarnAmber,
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = overlayOn,
                        onCheckedChange = { on ->
                            if (!hasPermission) {
                                onRequestOverlay()
                            } else {
                                overlayOn = on
                                onToggleOverlay(on)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AccentRed,
                            checkedTrackColor = AccentRedDim,
                            uncheckedThumbColor = MutedGrey,
                            uncheckedTrackColor = LineDark,
                        )
                    )
                }

                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(BgDark)
                        .border(1.dp, LineDark, RoundedCornerShape(6.dp))
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Preview", color = MutedGrey, fontSize = 11.sp)
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(AccentRedDim)
                            .border(1.dp, AccentRed, RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("-- FPS", color = AccentRedBright, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        SectionLabel("Live telemetry")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard("FPS", "--", Modifier.weight(1f))
            StatCard("CPU", "--", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard("RAM", "--", Modifier.weight(1f))
            StatCard("TEMP", "--", Modifier.weight(1f))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        color = MutedGrey,
        fontSize = 11.sp,
        letterSpacing = 2.sp,
        fontWeight = FontWeight.Medium,
    )
}

@Composable
private fun ModeCard(
    mode: Mode,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) mode.color else LineDark
    val bg = if (selected) mode.color.copy(alpha = 0.08f) else PanelDark
    Box(
        modifier
            .height(84.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxHeight()) {
            Box(
                Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (selected) mode.color else LineDark)
            )
            Column {
                Text(
                    mode.name,
                    color = if (selected) InkLight else MutedGrey,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(mode.blurb, color = MutedGrey, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        color = PanelDark,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, LineDark),
        modifier = modifier.height(74.dp),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = MutedGrey, fontSize = 10.sp, letterSpacing = 1.5.sp)
            Text(value, color = AccentRedBright, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
