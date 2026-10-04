package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.FlowGlassBorder
import com.example.ui.theme.FlowGlassSurface
import com.example.ui.theme.LocalVmaTheme

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape? = null,
    backgroundColor: Color? = null,
    borderColor: Color? = null,
    borderWidth: Dp = 1.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val vmaTheme = LocalVmaTheme.current
    val actualShape = shape ?: vmaTheme.cardShape
    val actualBg = backgroundColor ?: vmaTheme.cardBackground
    val actualBorder = borderColor ?: vmaTheme.cardBorder

    Box(
        modifier = modifier
            .clip(actualShape)
            .background(actualBg)
            .border(BorderStroke(borderWidth, actualBorder), actualShape),
        content = content
    )
}

@Composable
fun GlassCardClickable(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape? = null,
    backgroundColor: Color? = null,
    borderColor: Color? = null,
    borderWidth: Dp = 1.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val vmaTheme = LocalVmaTheme.current
    val actualShape = shape ?: vmaTheme.cardShape
    val actualBg = backgroundColor ?: vmaTheme.cardBackground
    val actualBorder = borderColor ?: vmaTheme.cardBorder

    Surface(
        onClick = onClick,
        modifier = modifier.clip(actualShape),
        shape = actualShape,
        color = actualBg,
        border = BorderStroke(borderWidth, actualBorder)
    ) {
        Box(content = content)
    }
}
