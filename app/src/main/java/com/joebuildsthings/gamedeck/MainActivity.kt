package com.joebuildsthings.gamedeck

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.joebuildsthings.gamedeck.screens.GamesScreen
import com.joebuildsthings.gamedeck.screens.HomeScreen
import com.joebuildsthings.gamedeck.screens.SettingsScreen
import com.joebuildsthings.gamedeck.ui.theme.AccentRed
import com.joebuildsthings.gamedeck.ui.theme.BgDark
import com.joebuildsthings.gamedeck.ui.theme.GameDeckTheme
import com.joebuildsthings.gamedeck.ui.theme.InkLight
import com.joebuildsthings.gamedeck.ui.theme.LineDark
import com.joebuildsthings.gamedeck.ui.theme.MutedGrey

data class Tab(val label: String, val icon: ImageVector)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GameDeckTheme {
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
        if (enable) startForegroundService(intent) else startService(intent)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameDeckApp(
    onRequestOverlay: () -> Unit,
    onToggleOverlay: (Boolean) -> Unit,
) {
    val tabs = listOf(
        Tab("Home", Icons.Filled.Home),
        Tab("Games", Icons.Filled.SportsEsports),
        Tab("Settings", Icons.Filled.Settings),
    )
    var current by remember { mutableIntStateOf(0) }

    Scaffold(
        containerColor = BgDark,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text(
                            "GameDeck",
                            color = InkLight,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp,
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "v0.1",
                            color = MutedGrey,
                            fontSize = 11.sp,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BgDark,
                )
            )
        },
        bottomBar = {
            NavigationBar(containerColor = BgDark) {
                tabs.forEachIndexed { i, tab ->
                    NavigationBarItem(
                        selected = current == i,
                        onClick = { current = i },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AccentRed,
                            selectedTextColor = AccentRed,
                            unselectedIconColor = MutedGrey,
                            unselectedTextColor = MutedGrey,
                            indicatorColor = LineDark,
                        )
                    )
                }
            }
        }
    ) { pad ->
        Box(
            Modifier
                .fillMaxSize()
                .background(BgDark)
                .padding(pad)
        ) {
            when (current) {
                0 -> HomeScreen(
                    onRequestOverlay = onRequestOverlay,
                    onToggleOverlay = onToggleOverlay,
                )
                1 -> GamesScreen()
                2 -> SettingsScreen(onRequestOverlay = onRequestOverlay)
            }
        }
    }
}
