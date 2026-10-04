package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

enum class AppStyle {
    MATERIAL_EXPRESSIVE,
    CLASSIC_MODERN,
    LIQUID_GLASS
}

data class VmaColorScheme(
    val primary: Color,
    val primaryLight: Color = Color(0xFFFF5252),
    val secondary: Color = GoldAccent,
    val accent: Color,
    val background: Color,
    val surface: Color,
    val card: Color,
    val cardBackground: Color = card,
    val border: Color,
    val cardBorder: Color = border,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color = Color(0xFF94A3B8),
    val isGlass: Boolean = false,
    val isDark: Boolean = true,
    val cardShape: Shape = RoundedCornerShape(16.dp),
    val pillShape: Shape = RoundedCornerShape(20.dp),
    val topBarBackground: Color = surface,
    val style: AppStyle = AppStyle.MATERIAL_EXPRESSIVE
)

val DarkVmaTheme = VmaColorScheme(
    primary = RedPrimary,
    primaryLight = Color(0xFFFF5252),
    secondary = GoldAccent,
    accent = GoldAccent,
    background = DarkBackground,
    surface = DarkSurface,
    card = DarkCard,
    cardBackground = DarkCard,
    border = DarkBorder,
    cardBorder = DarkBorder,
    textPrimary = TextPrimaryDark,
    textSecondary = TextSecondaryDark,
    textMuted = Color(0xFF94A3B8),
    isGlass = false,
    isDark = true,
    cardShape = RoundedCornerShape(16.dp),
    pillShape = RoundedCornerShape(20.dp),
    topBarBackground = DarkSurface,
    style = AppStyle.MATERIAL_EXPRESSIVE
)

val LightVmaTheme = VmaColorScheme(
    primary = RedPrimary,
    primaryLight = Color(0xFFFF5252),
    secondary = GoldAccent,
    accent = GoldAccent,
    background = LightBackground,
    surface = LightSurface,
    card = LightCard,
    cardBackground = LightCard,
    border = LightBorder,
    cardBorder = LightBorder,
    textPrimary = TextPrimaryLight,
    textSecondary = TextSecondaryLight,
    textMuted = Color(0xFF6B7280),
    isGlass = false,
    isDark = false,
    cardShape = RoundedCornerShape(16.dp),
    pillShape = RoundedCornerShape(20.dp),
    topBarBackground = LightSurface,
    style = AppStyle.MATERIAL_EXPRESSIVE
)

val LocalVmaTheme = staticCompositionLocalOf { DarkVmaTheme }

private val DarkColorScheme = darkColorScheme(
    primary = RedPrimary,
    secondary = GoldAccent,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark
)

private val LightColorScheme = lightColorScheme(
    primary = RedPrimary,
    secondary = GoldAccent,
    background = LightBackground,
    surface = LightSurface,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    style: AppStyle = AppStyle.MATERIAL_EXPRESSIVE,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val baseScheme = if (darkTheme) DarkVmaTheme else LightVmaTheme
    val vmaScheme = baseScheme.copy(
        style = style,
        isGlass = style == AppStyle.LIQUID_GLASS,
        isDark = darkTheme
    )

    CompositionLocalProvider(LocalVmaTheme provides vmaScheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
