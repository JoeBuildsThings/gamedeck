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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
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
import com.example.data.repository.InstalledAppInfo
import com.example.model.GameProfile
import com.example.model.OverlayConfig
import com.example.model.PerformanceMode
import com.example.ui.MainViewModel
import com.example.ui.components.OverlayLayoutEditor
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.PerformanceRed
import com.example.ui.theme.SurfaceBorderDark
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber

@Composable
fun GamesScreen(
    viewModel: MainViewModel,
    profiles: List<GameProfile>,
    installedApps: List<InstalledAppInfo>
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingProfile by remember { mutableStateOf<GameProfile?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "GAME PROFILES",
                    color = NeonCyan,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Assign performance modes & custom overlay HUD layouts per game",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }

            if (profiles.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorderDark))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Gamepad,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "No Games Registered Yet",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Tap the + button to select installed games. GameDeck detects when you launch them, applies your preferred mode and custom HUD layout, and reverts to Balanced mode when closed.",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(profiles, key = { it.packageName }) { profile ->
                    GameProfileCard(
                        profile = profile,
                        onUpdate = { updated -> viewModel.updateGameProfile(updated) },
                        onCustomizeLayout = { editingProfile = profile },
                        onDelete = { viewModel.deleteGameProfile(profile.packageName) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            containerColor = NeonCyan,
            contentColor = BackgroundDark,
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Game Profile")
        }
    }

    if (showAddDialog) {
        AddGameDialog(
            installedApps = installedApps,
            existingProfiles = profiles,
            onAdd = { appInfo ->
                viewModel.addGameProfile(appInfo)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    if (editingProfile != null) {
        val currentEditing = editingProfile!!
        val profileConfig = OverlayConfig(
            layoutPreset = currentEditing.layoutPreset,
            metricOrder = currentEditing.metricOrder.split(",").map { it.trim() }.filter { it.isNotEmpty() },
            scalePercent = currentEditing.scalePercent,
            opacityPercent = currentEditing.opacityPercent,
            showFps = currentEditing.showFps,
            showFrameTime = currentEditing.showFrameTime,
            showCpu = currentEditing.showCpu,
            showRam = currentEditing.showRam,
            showTemp = currentEditing.showTemp,
            showBattery = currentEditing.showBattery,
            showGraph = currentEditing.showGraph
        )

        AlertDialog(
            onDismissRequest = { editingProfile = null },
            title = {
                Column {
                    Text(
                        text = "Customize HUD Layout",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = currentEditing.appName,
                        color = NeonCyan,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(440.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Enable Game-Specific Layout", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Switch(
                            checked = currentEditing.customOverlayEnabled,
                            onCheckedChange = { checked ->
                                val updated = currentEditing.copy(customOverlayEnabled = checked)
                                editingProfile = updated
                                viewModel.updateGameProfile(updated)
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = BackgroundDark, checkedTrackColor = NeonCyan)
                        )
                    }

                    if (currentEditing.customOverlayEnabled) {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            item {
                                OverlayLayoutEditor(
                                    config = profileConfig,
                                    onConfigChange = { newCfg ->
                                        val updated = currentEditing.copy(
                                            layoutPreset = newCfg.layoutPreset,
                                            metricOrder = newCfg.metricOrder.joinToString(","),
                                            scalePercent = newCfg.scalePercent,
                                            opacityPercent = newCfg.opacityPercent,
                                            showFps = newCfg.showFps,
                                            showFrameTime = newCfg.showFrameTime,
                                            showCpu = newCfg.showCpu,
                                            showRam = newCfg.showRam,
                                            showTemp = newCfg.showTemp,
                                            showBattery = newCfg.showBattery,
                                            showGraph = newCfg.showGraph
                                        )
                                        editingProfile = updated
                                        viewModel.updateGameProfile(updated)
                                    }
                                )
                            }
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "Using global default layout.\nToggle on to customize metrics for this game.",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { editingProfile = null },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = BackgroundDark),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Save & Done", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingProfile = null }) {
                    Text("Close", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun GameProfileCard(
    profile: GameProfile,
    onUpdate: (GameProfile) -> Unit,
    onCustomizeLayout: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                when (profile.preferredMode) {
                    PerformanceMode.PERFORMANCE -> PerformanceRed
                    PerformanceMode.BATTERY_SAVER -> NeonGreen
                    PerformanceMode.BALANCED -> NeonCyan
                }
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceVariantDark),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Gamepad,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = profile.appName,
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = profile.packageName,
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onCustomizeLayout) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Customize HUD Layout",
                            tint = if (profile.customOverlayEnabled) NeonCyan else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove Profile",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Mode Selector Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PerformanceMode.values().forEach { mode ->
                    val isSelected = profile.preferredMode == mode
                    val color = when (mode) {
                        PerformanceMode.BALANCED -> NeonCyan
                        PerformanceMode.PERFORMANCE -> PerformanceRed
                        PerformanceMode.BATTERY_SAVER -> NeonGreen
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) color.copy(alpha = 0.2f) else SurfaceVariantDark)
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) color else SurfaceBorderDark,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                onUpdate(profile.copy(preferredMode = mode))
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mode.title,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) color else TextSecondary
                        )
                    }
                }
            }

            // Layout summary badge & auto-launch switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Auto-launch HUD on open",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (profile.customOverlayEnabled) "Custom Layout (${profile.layoutPreset})" else "Global HUD layout",
                        color = if (profile.customOverlayEnabled) NeonCyan else TextSecondary,
                        fontSize = 11.sp
                    )
                }

                Switch(
                    checked = profile.autoLaunchOverlay,
                    onCheckedChange = { checked ->
                        onUpdate(profile.copy(autoLaunchOverlay = checked))
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = BackgroundDark,
                        checkedTrackColor = NeonCyan,
                        uncheckedThumbColor = TextSecondary,
                        uncheckedTrackColor = SurfaceVariantDark
                    )
                )
            }
        }
    }
}

@Composable
fun AddGameDialog(
    installedApps: List<InstalledAppInfo>,
    existingProfiles: List<GameProfile>,
    onAdd: (InstalledAppInfo) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val existingPackages = remember(existingProfiles) { existingProfiles.map { it.packageName }.toSet() }

    val filteredApps = remember(installedApps, searchQuery, existingPackages) {
        installedApps
            .filter { !existingPackages.contains(it.packageName) }
            .filter {
                if (searchQuery.isBlank()) true
                else it.appName.contains(searchQuery, ignoreCase = true) || it.packageName.contains(searchQuery, ignoreCase = true)
            }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Select Game or App",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search installed games...", color = TextSecondary) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = SurfaceVariantDark,
                        unfocusedContainerColor = SurfaceVariantDark,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedIndicatorColor = NeonCyan,
                        unfocusedIndicatorColor = SurfaceBorderDark
                    ),
                    singleLine = true
                )

                if (filteredApps.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No matching apps found", color = TextSecondary, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredApps) { appInfo ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SurfaceVariantDark)
                                    .clickable { onAdd(appInfo) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = appInfo.appName,
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = appInfo.packageName,
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                if (appInfo.isGameCategory) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(NeonPurple.copy(alpha = 0.2f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("GAME", color = NeonPurple, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(16.dp)
    )
}
