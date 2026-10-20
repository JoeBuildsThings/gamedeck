package com.joebuildsthings.gamedeck

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.joebuildsthings.gamedeck.screens.GamesScreen
import com.joebuildsthings.gamedeck.screens.HomeScreen
import com.joebuildsthings.gamedeck.screens.SettingsScreen
import com.joebuildsthings.gamedeck.ui.theme.*

data class Tab(val label: String, val icon: ImageVector)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CrashLog.install(applicationContext)
        val lastCrash = CrashLog.consume(this)
        setContent {
            GameDeckTheme {
                var crashText by remember { mutableStateOf(lastCrash) }
                crashText?.let { text ->
                    AlertDialog(
                        onDismissRequest = { crashText = null },
                        confirmButton = {
                            TextButton(onClick = { crashText = null }) { Text("Close", fontFamily = FontFamily.Monospace) }
                        },
                        title = { Text("CRASH LOG INTERCEPTED", fontFamily = FontFamily.Monospace, color = AccentRed) },
                        text = {
                            SelectionContainer {
                                Text(
                                    text,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.verticalScroll(rememberScrollState())
                                )
                            }
                        },
                        containerColor = PanelDark
                    )
                }
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BgDark
                ) {
                    GameDeckApp(
                        onRequestOverlay = { requestOverlayPermission() },
                        onToggleOverlay = { enable -> toggleOverlay(enable) }
                    )
                }
            }
        }
    }

    private fun requestOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
        }
    }

    private fun toggleOverlay(enable: Boolean) {
        val intent = Intent(this, OverlayService::class.java).apply {
            action = if (enable) OverlayService.ACTION_START else OverlayService.ACTION_STOP
        }
        if (enable) {
            ContextCompat.startForegroundService(this, intent)
        } else {
            startService(intent)
        }
    }
}

@Composable
fun GameDeckApp(
    onRequestOverlay: () -> Unit,
    onToggleOverlay: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val activeMode by GameDeckEngine.mode.collectAsState()
    val tabs = listOf(
        Tab("ARMORY", Icons.Filled.Home),
        Tab("GAMES", Icons.Filled.SportsEsports),
        Tab("CONFIG", Icons.Filled.Settings),
    )
    var current by remember { mutableIntStateOf(0) }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(PanelDark)
                .border(1.dp, LineDark)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    Modifier
                        .size(10.dp)
                        .background(activeMode.accent)
                )
                Text(
                    text = "GAMEDECK OS",
                    color = InkLight,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.2.sp
                )
            }
            Text(
                text = "v1.0.4-MECHA",
                color = MutedGrey,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (current) {
                0 -> HomeScreen(
                    onRequestOverlay = onRequestOverlay,
                    onRequestLaunchGames = { current = 1 },
                    onToggleOverlay = onToggleOverlay
                )
                1 -> GamesScreen(onLaunchOverlayAndGame = { pkg ->
                    onToggleOverlay(true)
                    runCatching {
                        val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg)
                        if (launchIntent != null) {
                            context.startActivity(launchIntent)
                        }
                    }
                })
                2 -> SettingsScreen(onRequestOverlay = onRequestOverlay)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(PanelDark)
                .border(1.dp, LineDark)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            tabs.forEachIndexed { i, tab ->
                val selected = current == i
                MechaFrame(
                    accentColor = if (selected) activeMode.accent else LineDark,
                    bgColor = if (selected) activeMode.dimBg else BgDark,
                    cutDp = 6.dp,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { current = i }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = tab.label,
                            color = if (selected) activeMode.accent else MutedGrey,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.2.sp
                        )
                    }
                }
            }
        }
    }
}
