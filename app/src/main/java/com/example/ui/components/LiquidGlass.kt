package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalVmaTheme

/**
 * Clean Material 3 Shaders & Brushes
 */
object LiquidGlassShaders {
    fun refractiveBorderBrush(isDark: Boolean): Brush {
        return Brush.linearGradient(
            colors = listOf(
                if (isDark) Color(0x33FFFFFF) else Color(0x40000000),
                if (isDark) Color(0x1AFFFFFF) else Color(0x20000000)
            )
        )
    }

    fun glassBodyBrush(isDark: Boolean, accentTint: Color? = null): Brush {
        val base = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
        return Brush.verticalGradient(
            colors = listOf(
                accentTint?.copy(alpha = 0.2f) ?: base,
                base
            )
        )
    }
}

/**
 * Clean container modifier supporting Material 3 styling
 */
fun Modifier.liquidGlass(
    shape: Shape,
    borderWidth: Dp = 0.8.dp,
    accentTint: Color? = null,
    isDark: Boolean = true,
    isGlass: Boolean = false
): Modifier = this.then(
    Modifier
        .clip(shape)
        .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC))
        .border(
            border = BorderStroke(borderWidth, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)),
            shape = shape
        )
)

/**
 * Filter Chip / Pill shaped component with pure Material 3 styling
 */
@Composable
fun LiquidGlassPill(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    borderWidth: Dp = 1.dp,
    shape: Shape? = null,
    accentTint: Color? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val vmaTheme = LocalVmaTheme.current
    val actualShape = shape ?: vmaTheme.pillShape
    val isSelected = selected || accentTint != null

    Surface(
        onClick = onClick,
        shape = actualShape,
        color = if (isSelected) vmaTheme.primary.copy(alpha = 0.18f) else vmaTheme.cardBackground,
        border = BorderStroke(
            borderWidth,
            if (isSelected) vmaTheme.primary else vmaTheme.cardBorder
        ),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
            content = content
        )
    }
}

/**
 * Circular action bubble component with Material 3 styling
 */
@Composable
fun LiquidGlassBubble(
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    borderWidth: Dp = 1.dp,
    accentTint: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Surface(
        onClick = onClick ?: {},
        shape = shape,
        color = Color.Black.copy(alpha = 0.65f),
        border = BorderStroke(borderWidth, Color.White.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Box(contentAlignment = Alignment.Center) {
            content()
        }
    }
}

/**
 * Universal Card component that renders clean, high-performance Material 3 Card
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape? = null,
    accentTint: Color? = null,
    borderWidth: Dp = 0.8.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val vmaTheme = LocalVmaTheme.current
    val actualShape = shape ?: vmaTheme.cardShape

    val clickableMod = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(),
            onClick = onClick
        )
    } else Modifier

    Surface(
        shape = actualShape,
        color = vmaTheme.cardBackground,
        border = BorderStroke(borderWidth, vmaTheme.cardBorder),
        modifier = modifier.then(clickableMod)
    ) {
        Box(modifier = Modifier, content = content)
    }
}

/**
 * Primary root container surface applying pure Material 3 theme
 */
@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(0.dp),
    borderWidth: Dp = 1.dp,
    accentTint: Color? = null,
    isGlass: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val vmaTheme = LocalVmaTheme.current
    Surface(
        modifier = modifier,
        shape = shape,
        color = vmaTheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize(), content = content)
    }
}

/**
 * Clean container panel for floating bars, headers, and dialog wrappers
 */
@Composable
fun LiquidGlassContainer(
    modifier: Modifier = Modifier,
    shape: Shape? = null,
    borderWidth: Dp = 0.8.dp,
    accentTint: Color? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val vmaTheme = LocalVmaTheme.current
    val actualShape = shape ?: RoundedCornerShape(20.dp)

    Surface(
        modifier = modifier,
        shape = actualShape,
        color = vmaTheme.surface,
        border = BorderStroke(borderWidth, vmaTheme.cardBorder)
    ) {
        Box(modifier = Modifier, content = content)
    }
}
