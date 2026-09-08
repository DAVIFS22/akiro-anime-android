package com.akiro.anime.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.akiro.anime.data.model.Anime

@Composable
fun AnimeCard(
    anime: Anime,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .width(140.dp)
            .height(210.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = anime.poster,
            contentDescription = "Abrir ${anime.title}",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))))
                .padding(9.dp)
        ) {
            Column {
                Text(
                    text = anime.title,
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (anime.year > 0 || anime.rating > 0) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = buildString {
                            if (anime.year > 0) append(anime.year)
                            if (anime.year > 0 && anime.rating > 0) append(" • ")
                            if (anime.rating > 0) append("★ %.1f".format(anime.rating))
                        },
                        color = Color.White.copy(alpha = 0.78f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}
