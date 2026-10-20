package com.joebuildsthings.gamedeck.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.joebuildsthings.gamedeck.*
import com.joebuildsthings.gamedeck.ui.theme.*

@Composable
fun SettingsScreen(onRequestOverlay: () -> Unit) {
    val context = LocalContext.current
    val activeMode by GameDeckEngine.mode.collectAsState()
    val overlayOk = Settings.canDrawOverlays(context)

    Column(
        Modifier
            .fillMaxSize()
            .background(BgDark)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "SYSTEM CONFIG // XOS SURVIVAL",
            color = MutedGrey,
            fontSize = 10.sp,
            letterSpacing = 1.8.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )

        SettingsRow(
            label = "FLOATING OVERLAY PERMISSION",
            status = if (overlayOk) "GRANTED" else "REQUIRED",
            ok = overlayOk,
            accent = activeMode.accent,
            onClick = { onRequestOverlay() }
        )

        SettingsRow(
            label = "BATTERY OPTIMIZATION (XOS WHITELIST)",
            status = "TAP TO FIX KILL",
            ok = false,
            accent = CyberCyan,
            onClick = {
                runCatching {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                }
            }
        )

        SettingsRow(
            label = "SHIZUKU SHELL BRIDGE",
            status = "READY (SYSFS)",
            ok = true,
            accent = SuccessGreen,
            onClick = {}
        )

        Spacer(Modifier.height(4.dp))
        Text(
            text = "BUILD SPECIFICATION",
            color = MutedGrey,
            fontSize = 10.sp,
            letterSpacing = 1.8.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )

        InfoRow("VERSION", "1.0.4-MECHA", activeMode.accent)
        InfoRow("PACKAGE", "com.joebuildsthings.gamedeck", activeMode.accent)
        InfoRow("DEVELOPER", "Master Joe (JoeBuildsThings)", activeMode.accent)
        InfoRow("ARCHITECTURE", "Compose + WindowManager + Choreographer", activeMode.accent)
    }
}

@Composable
private fun SettingsRow(
    label: String,
    status: String,
    ok: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    MechaFrame(
        accentColor = if (ok) accent else WarnAmber,
        bgColor = PanelDark,
        cutDp = 10.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = InkLight,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
            Box(
                Modifier
                    .background(if (ok) SuccessGreenDim else WarnAmber.copy(alpha = 0.2f))
                    .border(1.dp, if (ok) SuccessGreen else WarnAmber)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = status,
                    color = if (ok) SuccessGreen else WarnAmber,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, accent: Color) {
    MechaFrame(
        accentColor = LineDark,
        bgColor = PanelDark,
        cutDp = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = MutedGrey,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = value,
                color = accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
