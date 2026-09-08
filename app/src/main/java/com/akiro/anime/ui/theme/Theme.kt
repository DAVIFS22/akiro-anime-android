package com.akiro.anime.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Dark, cinema-style palette matching an anime streaming app aesthetic.
private val AkiroDarkColors = darkColorScheme(
    primary = Color(0xFFE50914),
    onPrimary = Color.White,
    background = Color(0xFF0B0B0F),
    onBackground = Color(0xFFF2F2F2),
    surface = Color(0xFF16161C),
    onSurface = Color(0xFFF2F2F2),
    secondary = Color(0xFF7C4DFF),
)

@Composable
fun AkiroAnimeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AkiroDarkColors,
        typography = MaterialTheme.typography,
        content = content
    )
}
