package com.akiro.anime.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

private val AkiroDarkColors = darkColorScheme(
    primary = Color(0xFFE900FF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF5B0870),
    onPrimaryContainer = Color(0xFFFFD6FF),
    secondary = Color(0xFF7B61FF),
    onSecondary = Color.White,
    tertiary = Color(0xFF4DD6FF),
    background = Color(0xFF03050B),
    onBackground = Color(0xFFF7F5FF),
    surface = Color(0xFF080B14),
    onSurface = Color(0xFFF7F5FF),
    surfaceVariant = Color(0xFF111827),
    onSurfaceVariant = Color(0xFFB5B9C9),
    outline = Color(0xFF2C3448),
)

private val AkiroTypography = Typography().run {
    copy(
        headlineLarge = headlineLarge.copy(fontWeight = FontWeight.Black),
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
    )
}

@Composable
fun AkiroAnimeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AkiroDarkColors,
        typography = AkiroTypography,
        content = content,
    )
}
