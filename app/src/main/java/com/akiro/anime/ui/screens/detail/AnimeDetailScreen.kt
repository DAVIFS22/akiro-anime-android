package com.akiro.anime.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.akiro.anime.data.model.Episode

@Composable
fun AnimeDetailScreen(
    animeId: String,
    onEpisodeClick: (episode: Episode, streamUrl: String?) -> Unit = { _, _ -> },
    viewModel: DetailViewModel = viewModel(),
) {
    val tmdbId = animeId.toIntOrNull() ?: 0
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(tmdbId) { viewModel.load(tmdbId) }

    when {
        state.isLoading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        }
        state.error != null && state.anime == null -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(state.error ?: "Erro")
            }
        }
        state.anime != null -> {
            val anime = state.anime!!
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(260.dp)) {
                        AsyncImage(
                            model = anime.backdrop ?: anime.banner,
                            contentDescription = anime.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.95f))
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp)
                        ) {
                            Text(
                                text = anime.title,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Text(
                                text = "${anime.year} • ${anime.type.label} • ⭐ ${"%.1f".format(anime.rating)}",
                                color = Color.White.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                val ep = state.episodes.firstOrNull()
                                onEpisodeClick(ep ?: Episode("", 1, state.selectedSeason, "Episódio 1", anime.poster, 1440), null)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Assistir")
                        }
                        OutlinedButton(onClick = { viewModel.toggleFavorite() }) {
                            Icon(
                                if (state.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = "Favoritar",
                                tint = if (state.isFavorite) MaterialTheme.colorScheme.primary else LocalContentColor.current
                            )
                        }
                    }
                }

                item {
                    Text(
                        text = anime.synopsis,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                }

                if (anime.genres.isNotEmpty()) {
                    item {
                        Text(
                            text = anime.genres.joinToString(" • "),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                }

                if (anime.seasons.size > 1) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            anime.seasons.filter { it.seasonNumber > 0 }.forEach { season ->
                                FilterChip(
                                    selected = season.seasonNumber == state.selectedSeason,
                                    onClick = { viewModel.selectSeason(tmdbId, season.seasonNumber) },
                                    label = { Text(season.name) }
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                }

                item {
                    Text(
                        text = "Episódios",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                }

                items(state.episodes) { episode ->
                    EpisodeRow(episode = episode, onClick = { onEpisodeClick(episode, null) })
                }

                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun EpisodeRow(episode: Episode, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = episode.thumbnail,
            contentDescription = episode.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(120.dp)
                .height(68.dp)
                .clip(RoundedCornerShape(8.dp))
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("${episode.number}. ${episode.title}", fontWeight = FontWeight.Medium, maxLines = 1)
            episode.description?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 2, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
        }
        IconButton(onClick = onClick) {
            Icon(Icons.Filled.PlayArrow, contentDescription = "Assistir episódio ${episode.number}")
        }
    }
}
