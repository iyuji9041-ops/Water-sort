package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Water Sort uses an atmospheric dark theme by default to showcase luminous liquids
private val DarkColorScheme = darkColorScheme(
    primary = LiquidBlue,
    secondary = LiquidPurple,
    tertiary = LiquidCyan,
    background = SpaceDeep,
    surface = SpaceMid,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun WaterSortTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
