package com.joebuildsthings.gamedeck.screens

import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.joebuildsthings.gamedeck.ui.theme.*

private data class Mode(
    val name: String,
    val blurb: String,
    val color: Color,
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
            .padding(horizontal = 18.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "PERFORMANCE MODE",
                color = MutedGrey,
                fontSize = 10.sp,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                modes[selected].name.uppercase(),
                color = modes[selected].color,
                fontSize = 10.sp,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold,
            )
        }

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

        Spacer(Modifier.height(4.dp))

        Text(
            "FLOATING HUD",
            color = MutedGrey,
            fontSize = 10.sp,
            letterSpacing = 2.sp,
            fontWeight = FontWeight.SemiBold,
        )

        Surface(
            color = PanelDark,
            shape = RoundedCornerShape(12.dp),
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
                        Text("Overlay HUD", color = InkLight, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            if (hasPermission) "Ready" else "Grant permission",
                            color = if (hasPermission) SuccessGreen else WarnAmber,
                            fontSize = 11.sp
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
                        .height(42.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(BgDark)
                        .border(1.dp, LineDark, RoundedCornerShape(8.dp))
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("LIVE PREVIEW", color = MutedGrey, fontSize = 9.sp, letterSpacing = 2.sp)
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(AccentRedDim)
                            .border(1.dp, AccentRed, RoundedCornerShape(20.dp))
                            .padding(horizontal = 14.dp, vertical = 5.dp)
                    ) {
                        Text(
                            "-- FPS",
                            color = AccentRedBright,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        Text(
            "LIVE TELEMETRY",
            color = MutedGrey,
            fontSize = 10.sp,
            letterSpacing = 2.sp,
            fontWeight = FontWeight.SemiBold,
        )

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
private fun ModeCard(
    mode: Mode,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) mode.color else LineDark
    val bg = if (selected) mode.color.copy(alpha = 0.10f) else PanelDark
    Box(
        modifier
            .height(94.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxHeight()
        ) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (selected) mode.color else LineDark)
            )
            Column {
                Text(
                    mode.name,
                    color = if (selected) InkLight else MutedGrey,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    mode.blurb,
                    color = if (selected) mode.color.copy(alpha = 0.9f) else MutedGrey,
                    fontSize = 10.sp,
                )
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        color = PanelDark,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, LineDark),
        modifier = modifier.height(78.dp),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                label,
                color = MutedGrey,
                fontSize = 9.sp,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                value,
                color = AccentRedBright,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
            )
        }
    }
}
