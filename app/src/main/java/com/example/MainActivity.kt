package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainViewModel
import com.example.ui.screens.GamesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SessionsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ToolsScreen
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.GameDeckTheme
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SurfaceBorderDark
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary

enum class GameDeckTab(val title: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    GAMES("Games", Icons.Default.Gamepad),
    SESSIONS("Sessions", Icons.Default.History),
    TOOLS("Tools", Icons.Default.Build),
    SETTINGS("Settings", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            GameDeckTheme {
                val viewModel: MainViewModel = viewModel()
                GameDeckAppContent(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh permissions when returning from system settings
        try {
            val app = application as? GameDeckApp
            // Any resumed action
        } catch (_: Exception) {}
    }
}

@Composable
fun GameDeckAppContent(viewModel: MainViewModel) {
    var selectedTab by remember { mutableStateOf(GameDeckTab.HOME) }

    val shizukuStatus by viewModel.shizukuStatus.collectAsStateWithLifecycle()
    val currentMode by viewModel.currentMode.collectAsStateWithLifecycle()
    val isOverlayEnabled by viewModel.isOverlayEnabled.collectAsStateWithLifecycle()
    val overlayConfig by viewModel.overlayConfig.collectAsStateWithLifecycle()
    val liveTelemetry by viewModel.liveTelemetry.collectAsStateWithLifecycle()
    val permissions by viewModel.permissionsState.collectAsStateWithLifecycle()
    val gameProfiles by viewModel.gameProfiles.collectAsStateWithLifecycle()
    val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val selectedSession by viewModel.selectedSession.collectAsStateWithLifecycle()
    val activityLogs by viewModel.activityLogs.collectAsStateWithLifecycle()
    val killWhitelist by viewModel.killWhitelist.collectAsStateWithLifecycle()
    val ramCleanResult by viewModel.ramCleanResult.collectAsStateWithLifecycle()

    BackHandler(enabled = selectedTab != GameDeckTab.HOME) {
        selectedTab = GameDeckTab.HOME
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark),
        containerColor = BackgroundDark,
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceDark,
                contentColor = NeonCyan,
                tonalElevation = 8.dp
            ) {
                GameDeckTab.values().forEach { tab ->
                    val isSelected = selectedTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonCyan,
                            selectedTextColor = NeonCyan,
                            indicatorColor = NeonCyan.copy(alpha = 0.15f),
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                GameDeckTab.HOME -> {
                    HomeScreen(
                        viewModel = viewModel,
                        shizukuStatus = shizukuStatus,
                        currentMode = currentMode,
                        isOverlayEnabled = isOverlayEnabled,
                        telemetry = liveTelemetry,
                        permissions = permissions
                    )
                }
                GameDeckTab.GAMES -> {
                    GamesScreen(
                        viewModel = viewModel,
                        profiles = gameProfiles,
                        installedApps = installedApps
                    )
                }
                GameDeckTab.SESSIONS -> {
                    SessionsScreen(
                        viewModel = viewModel,
                        sessions = sessions,
                        selectedSession = selectedSession
                    )
                }
                GameDeckTab.TOOLS -> {
                    ToolsScreen(
                        viewModel = viewModel,
                        telemetry = liveTelemetry,
                        ramCleanResult = ramCleanResult
                    )
                }
                GameDeckTab.SETTINGS -> {
                    SettingsScreen(
                        viewModel = viewModel,
                        config = overlayConfig,
                        installedApps = installedApps,
                        killWhitelist = killWhitelist,
                        activityLogs = activityLogs
                    )
                }
            }
        }
    }
}
