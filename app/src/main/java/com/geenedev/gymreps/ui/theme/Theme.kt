package com.geenedev.gymreps.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * One fixed dark look (no wallpaper colours): black canvas, Prussian Blue surfaces,
 * Orange for action, Alabaster Grey for calm/secondary, White text.
 */
private val GymRepsColors = darkColorScheme(
    primary = Orange,
    onPrimary = OnOrange,
    primaryContainer = OrangeDeep,
    onPrimaryContainer = Color(0xFFFFDDB0),
    inversePrimary = Color(0xFF855400),
    secondary = AlabasterGrey,
    onSecondary = PrussianBlue,
    secondaryContainer = PrussianBlue,
    onSecondaryContainer = AlabasterGrey,
    tertiary = AlabasterGrey,
    onTertiary = PrussianBlue,
    tertiaryContainer = NavyHighest,
    onTertiaryContainer = White,
    background = Black,
    onBackground = White,
    surface = Black,
    onSurface = White,
    surfaceVariant = NavyHighest,
    onSurfaceVariant = TextMuted,
    surfaceTint = Orange,
    surfaceBright = NavyHighest,
    surfaceDim = Black,
    surfaceContainerLowest = Black,
    surfaceContainerLow = NavyLow,
    surfaceContainer = NavyMid,
    surfaceContainerHigh = NavyHigh,
    surfaceContainerHighest = NavyHighest,
    inverseSurface = AlabasterGrey,
    inverseOnSurface = PrussianBlue,
    outline = Line,
    outlineVariant = LineSoft,
    error = Danger,
    onError = Black,
    scrim = Black,
)

@Composable
fun GymRepsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = GymRepsColors,
        typography = Typography,
        content = content,
    )
}
