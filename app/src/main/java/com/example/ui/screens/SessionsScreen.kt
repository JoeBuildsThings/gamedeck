package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.SessionEntity
import com.example.ui.MainViewModel
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.GraphJankColor
import com.example.ui.theme.GraphLineColor
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SessionsScreen(
    viewModel: MainViewModel,
    sessions: List<SessionEntity>,
    selectedSession: SessionEntity?
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SESSION HISTORY",
                        color = NeonCyan,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Recorded game performance benchmarks & charts",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }

                if (sessions.isNotEmpty()) {
                    IconButton(onClick = { viewModel.clearAllSessions() }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear All Sessions",
                            tint = TextSecondary
                        )
                    }
                }
            }
        }

        if (sessions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
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
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "No Game Sessions Recorded",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Play a registered game with the HUD active to record FPS, frame times, CPU, and thermals. Complete benchmark reports will appear here.",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(sessions, key = { it.id }) { session ->
                SessionCard(
                    session = session,
                    onClick = { viewModel.selectSession(session) },
                    onDelete = { viewModel.deleteSession(session) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Detailed Session Modal Dialog
    if (selectedSession != null) {
        SessionDetailDialog(
            session = selectedSession,
            onDismiss = { viewModel.selectSession(null) },
            onExportCsv = { viewModel.exportSessionCsv(selectedSession, context) },
            onDelete = {
                viewModel.deleteSession(selectedSession)
                viewModel.selectSession(null)
            }
        )
    }
}

@Composable
fun SessionCard(
    session: SessionEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }
    val formattedDate = remember(session.startTime) { dateFormat.format(Date(session.startTime)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorderDark))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = session.appName,
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$formattedDate • ${session.durationSeconds}s • ${session.modeUsed}",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Session",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Stat Cards Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SessionStatBadge(label = "AVG FPS", value = "${"%.1f".format(session.avgFps)}", accentColor = NeonCyan)
                SessionStatBadge(label = "1% LOW", value = "${"%.0f".format(session.onePercentLowFps)}", accentColor = WarningAmber)
                SessionStatBadge(label = "JANK", value = "${"%.1f".format(session.jankPercent)}%", accentColor = if (session.jankPercent > 5f) PerformanceRed else NeonGreen)
                SessionStatBadge(label = "MAX TEMP", value = "${session.maxTempC.toInt()}°C", accentColor = if (session.maxTempC > 40f) PerformanceRed else TextSecondary)
            }
        }
    }
}

@Composable
fun SessionStatBadge(label: String, value: String, accentColor: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceVariantDark)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = label, color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Text(text = value, color = accentColor, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
fun SessionDetailDialog(
    session: SessionEntity,
    onDismiss: () -> Unit,
    onExportCsv: () -> Unit,
    onDelete: () -> Unit
) {
    // Parse samples from CSV for chart
    val frameTimes = remember(session.samplesCsv) {
        val list = mutableListOf<Float>()
        session.samplesCsv.lines().drop(1).forEach { line ->
            val cols = line.split(",")
            if (cols.size >= 3) {
                cols[2].toFloatOrNull()?.let { list.add(it) }
            }
        }
        list
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = session.appName,
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Benchmark Report (${session.durationSeconds} seconds)",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Key metrics grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SessionStatBadge(label = "AVG FPS", value = "${"%.1f".format(session.avgFps)}", accentColor = NeonCyan)
                    SessionStatBadge(label = "1% LOW", value = "${"%.0f".format(session.onePercentLowFps)}", accentColor = WarningAmber)
                    SessionStatBadge(label = "JANK", value = "${"%.1f".format(session.jankPercent)}%", accentColor = PerformanceRed)
                    SessionStatBadge(label = "MAX TEMP", value = "${session.maxTempC.toInt()}°C", accentColor = NeonGreen)
                }

                // Frame Time Chart
                Text(
                    text = "FRAME TIME DURATION (MS)",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                if (frameTimes.isNotEmpty()) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceVariantDark)
                            .padding(8.dp)
                    ) {
                        val w = size.width
                        val h = size.height
                        val count = frameTimes.size
                        val stepX = w / (count - 1).coerceAtLeast(1)
                        val maxMs = 40f
                        val path = Path()

                        frameTimes.forEachIndexed { i, ms ->
                            val clamped = ms.coerceIn(0f, maxMs)
                            val x = i * stepX
                            val y = h - (clamped / maxMs * h)

                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)

                            if (ms > 25f) {
                                drawCircle(GraphJankColor, radius = 2.5f, center = Offset(x, y))
                            }
                        }

                        // Baseline at 16.6ms (60 FPS)
                        val line60Y = h - (16.6f / maxMs * h)
                        drawLine(
                            color = TextMuted.copy(alpha = 0.5f),
                            start = Offset(0f, line60Y),
                            end = Offset(w, line60Y),
                            strokeWidth = 1f
                        )

                        drawPath(path, color = GraphLineColor, style = Stroke(width = 2f))
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceVariantDark),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No granular samples recorded", color = TextSecondary, fontSize = 12.sp)
                    }
                }

                // Averages summary
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Average CPU: ${session.avgCpuUsage.toInt()}%", color = TextSecondary, fontSize = 12.sp)
                    Text(text = "Average RAM: ${session.avgRamUsedMb} MB", color = TextSecondary, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onExportCsv,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonCyan,
                        contentColor = BackgroundDark
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export CSV", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = TextSecondary)
            }
        },
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(16.dp)
    )
}
