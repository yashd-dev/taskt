package com.yashd.assignment2.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val cols = darkColorScheme(
    primary = Color(0xFFF5A1AD),
    onPrimary = Color(0xFF39121C),
    primaryContainer = Color(0xFF593039),
    onPrimaryContainer = Color(0xFFFFD9DF),
    inversePrimary = Color(0xFF8D4A58),
    secondary = Color(0xFFDDBBC2),
    onSecondary = Color(0xFF3E292E),
    secondaryContainer = Color(0xFF493037),
    onSecondaryContainer = Color(0xFFFFD9DF),
    tertiary = Color(0xFFE8B9A8),
    onTertiary = Color(0xFF432B22),
    tertiaryContainer = Color(0xFF5C3B30),
    onTertiaryContainer = Color(0xFFFFDBCE),
    background = Color(0xFF1C1214),
    onBackground = Color(0xFFE7DADD),
    surface = Color(0xFF1C1214),
    onSurface = Color(0xFFE7DADD),
    surfaceVariant = Color(0xFF49383D),
    onSurfaceVariant = Color(0xFFCBB7BD),
    surfaceTint = Color(0xFFF5A1AD),
    surfaceDim = Color(0xFF1C1214),
    surfaceBright = Color(0xFF453438),
    surfaceContainerLowest = Color(0xFF120A0B),
    surfaceContainerLow = Color(0xFF120A0B),
    surfaceContainer = Color(0xFF241719),
    surfaceContainerHigh = Color(0xFF2D2022),
    surfaceContainerHighest = Color(0xFF3B2B2F),
    inverseSurface = Color(0xFFE7DADD),
    inverseOnSurface = Color(0xFF332629),
    outline = Color(0xFF9C858C),
    outlineVariant = Color(0xFF514047)
)

private val rnd = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun Assignment2Theme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = cols,
        shapes = rnd,
        typography = fon,
        content = content
    )
}
