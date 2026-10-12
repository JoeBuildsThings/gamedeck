package com.joebuildsthings.gamedeck.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val GlassWhite = Color(0x1AFFFFFF)
val GlassBorder = Color(0x33FFFFFF)
val GlassDark = Color(0x33000000)
val GlassAccent = Color(0x33EF4444)

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    accent: Color = GlassAccent,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(GlassWhite)
                .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                .padding(16.dp),
            content = content
        )
    }
}
