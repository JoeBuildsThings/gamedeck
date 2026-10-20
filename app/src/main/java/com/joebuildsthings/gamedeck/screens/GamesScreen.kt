package com.joebuildsthings.gamedeck.screens

import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.joebuildsthings.gamedeck.*
import com.joebuildsthings.gamedeck.ui.theme.*

data class GameProfile(
    val name: String,
    val packageName: String,
    val category: String,
    val targetFps: Int,
    val defaultMode: EngineMode
)

@Composable
fun GamesScreen(onLaunchOverlayAndGame: (String) -> Unit) {
    val context = LocalContext.current
    val activeMode by GameDeckEngine.mode.collectAsState()

    val games = remember {
        listOf(
            GameProfile("Free Fire", "com.dts.freefireth", "BATTLE ROYALE", 90, EngineMode.DIABLO),
            GameProfile("Call of Duty: Mobile", "com.activision.callofduty.shooter", "FPS TACTICAL", 60, EngineMode.TURBO),
            GameProfile("PUBG Mobile", "com.tencent.ig", "BATTLE ROYALE", 60, EngineMode.TURBO),
            GameProfile("Subway Surfers", "com.kiloo.subwaysurfer", "ARCADE", 60, EngineMode.ECO)
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(BgDark)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        MechaFrame(
            accentColor = activeMode.accent,
            bgColor = PanelDark,
            cutDp = 10.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "XARENA // GAME ARMORY",
                    color = activeMode.accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "Select a game to deploy tactical HUD overlay and auto-boost CPU governor.",
                    color = MutedGrey,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        games.forEach { game ->
            val isInstalled = remember {
                runCatching {
                    context.packageManager.getPackageInfo(game.packageName, 0)
                    true
                }.getOrDefault(false)
            }

            MechaFrame(
                accentColor = if (isInstalled) activeMode.accent else LineDark,
                bgColor = PanelDark,
                cutDp = 10.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = game.name,
                                color = InkLight,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                            if (isInstalled) {
                                Box(
                                    Modifier
                                        .background(SuccessGreenDim)
                                        .border(1.dp, SuccessGreen)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "INSTALLED",
                                        color = SuccessGreen,
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Text(
                            text = "${game.category} // TARGET: ${game.targetFps}FPS",
                            color = MutedGrey,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    MechaFrame(
                        accentColor = activeMode.accent,
                        bgColor = activeMode.dimBg,
                        cutDp = 6.dp,
                        modifier = Modifier.clickable {
                            GameDeckEngine.setMode(game.defaultMode)
                            onLaunchOverlayAndGame(game.packageName)
                        }
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "LAUNCH",
                                color = activeMode.accent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
