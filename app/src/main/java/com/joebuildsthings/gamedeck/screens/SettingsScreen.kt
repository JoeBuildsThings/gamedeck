package com.joebuildsthings.gamedeck.screens

import android.provider.Settings
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.joebuildsthings.gamedeck.ui.theme.*

@Composable
fun SettingsScreen(onRequestOverlay: () -> Unit) {
    val context = LocalContext.current
    val overlayOk = Settings.canDrawOverlays(context)

    Column(
        Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            "PERMISSIONS",
            color = MutedGrey,
            fontSize = 11.sp,
            letterSpacing = 2.sp,
            fontWeight = FontWeight.Medium,
        )
        RowItem("Floating HUD", if (overlayOk) "Granted" else "Not granted", overlayOk)

        Spacer(Modifier.height(4.dp))
        Text(
            "ABOUT",
            color = MutedGrey,
            fontSize = 11.sp,
            letterSpacing = 2.sp,
            fontWeight = FontWeight.Medium,
        )
        RowItem("Version", "0.1.0", true)
        RowItem("Package", "com.joebuildsthings.gamedeck", true)
        RowItem("Author", "JoeBuildsThings", true)
    }
}

@Composable
private fun RowItem(label: String, value: String, ok: Boolean) {
    Surface(
        color = PanelDark,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, LineDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = InkLight, fontSize = 14.sp)
            Text(
                value,
                color = if (ok) SuccessGreen else WarnAmber,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
