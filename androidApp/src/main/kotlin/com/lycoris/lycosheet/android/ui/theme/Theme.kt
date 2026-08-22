package com.lycoris.lycosheet.android.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Primary,
    primaryContainer = PrimaryContainer,
    secondary = Secondary,
    secondaryContainer = SecondaryContainer,
    surface = Surface,
    surfaceVariant = SurfaceVariant,
    background = Background,
    onBackground = OnBackground,
    onSurface = OnSurface,
    onSurfaceVariant = OnSurfaceVariant,
    error = Error
)

private val DarkColors = darkColorScheme(
    primary = PrimaryDark,
    primaryContainer = PrimaryContainerDark,
    secondary = Secondary,
    secondaryContainer = PrimaryContainerDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    onSurface = OnSurfaceDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    error = Error
)

@Composable
fun LycoSheetTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content
    )
}
