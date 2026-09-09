@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.akiro.anime.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.akiro.anime.data.model.Episode

@Composable
fun AnimeDetailScreen(animeId: String, onBack: () -> Unit = {}, onEpisodeClick: (Episode, String?) -> Unit = { _, _ -> }, viewModel: DetailViewModel = viewModel()) {
    val tmdbId = animeId.toIntOrNull() ?: 0
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(tmdbId) { viewModel.load(tmdbId) }
    LaunchedEffect(state.resolvedStreamUrl) {
        val url = state.resolvedStreamUrl
        val ep = state.resolvedEpisode
        if (!url.isNullOrBlank() && ep != null) { onEpisodeClick(ep, url); viewModel.consumeResolvedStream() }
    }
    when {
        state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        state.anime == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(state.error ?: "Não foi possível carregar o anime") }
        else -> {
            val anime = state.anime!!
            LazyColumn(Modifier.fillMaxSize()) {
                item {
                    Box(Modifier.fillMaxWidth().height(370.dp)) {
                        AsyncImage(model = anime.backdrop ?: anime.banner, contentDescription = anime.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(.10f), Color.Transparent, MaterialTheme.colorScheme.background))))
                        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color.Black.copy(.55f), Color.Transparent))))
                        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            SmallFloatingActionButton(onClick = onBack, containerColor = Color.Black.copy(.55f), contentColor = Color.White) { Icon(Icons.Filled.ArrowBack, "Voltar") }
                            Spacer(Modifier.weight(1f))
                            SmallFloatingActionButton(onClick = { viewModel.toggleFavorite() }, containerColor = Color.Black.copy(.55f), contentColor = Color.White) { Icon(if (state.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, "Favoritar") }
                        }
                        Column(Modifier.align(Alignment.BottomStart).padding(18.dp)) {
                            Text(anime.title, color = Color.White, style = MaterialTheme.typography.headlineLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(anime.nativeTitle ?: anime.romajiTitle ?: "", color = Color.White.copy(.70f), style = MaterialTheme.typography.bodySmall, maxLines = 1)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                MetaPill("★ ${"%.1f".format(anime.rating)}")
                                MetaPill(anime.year.toString())
                                MetaPill(anime.type.label)
                                if (anime.ageRating.isNotBlank()) MetaPill(anime.ageRating)
                            }
                        }
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = { state.episodes.firstOrNull()?.let(viewModel::watchEpisode) }, enabled = !state.streamLoading, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                            Icon(Icons.Filled.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text(if (state.streamLoading) "Buscando…" else "Assistir agora")
                        }
                        OutlinedButton(onClick = { viewModel.toggleFavorite() }, shape = RoundedCornerShape(14.dp)) { Icon(if (state.isFavorite) Icons.Filled.Favorite else Icons.Filled.BookmarkBorder, "Salvar") }
                    }
                }
                item {
                    if (anime.genres.isNotEmpty()) LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) { items(anime.genres) { AssistChip(onClick = {}, label = { Text(it) }) } }
                    Spacer(Modifier.height(12.dp))
                    Text(anime.synopsis, modifier = Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(18.dp))
                }
                if (anime.seasons.any { it.seasonNumber > 0 }) item {
                    Text("Temporadas", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 16.dp))
                    Spacer(Modifier.height(8.dp))
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(anime.seasons.filter { it.seasonNumber > 0 }) { season -> FilterChip(selected = season.seasonNumber == state.selectedSeason, onClick = { viewModel.selectSeason(tmdbId, season.seasonNumber) }, label = { Text(season.name) }) }
                    }
                    Spacer(Modifier.height(16.dp))
                }
                item {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Episódios da temporada ${state.selectedSeason}", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                        Text("${state.episodes.size}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (state.streamLoading) {
                        Spacer(Modifier.height(10.dp)); LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 16.dp)); Text("Consultando o Torrentio e procurando a melhor fonte…", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp))
                    }
                    state.streamError?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp)) }
                    Spacer(Modifier.height(6.dp))
                }
                items(state.episodes, key = { "${it.seasonNumber}-${it.number}" }) { episode -> EpisodeRow(episode, !state.streamLoading) { viewModel.watchEpisode(episode) } }
                item { Spacer(Modifier.height(28.dp)) }
            }
        }
    }
}

@Composable private fun MetaPill(text: String) { Surface(color = Color.Black.copy(.45f), shape = RoundedCornerShape(8.dp)) { Text(text, color = Color.White, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)) } }

@Composable private fun EpisodeRow(episode: Episode, enabled: Boolean, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(14.dp)) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(width = 120.dp, height = 68.dp).clip(RoundedCornerShape(10.dp))) {
                AsyncImage(model = episode.thumbnail, contentDescription = episode.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                Surface(Modifier.align(Alignment.Center), color = Color.Black.copy(.60f), shape = RoundedCornerShape(50.dp)) { IconButton(onClick = onClick, enabled = enabled) { Icon(Icons.Filled.PlayArrow, "Assistir", tint = Color.White) } }
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text("${episode.number}. ${episode.title}", fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(if (episode.duration > 0) "${episode.duration / 60} min" else "Episódio ${episode.number}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                episode.description?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
            IconButton(onClick = onClick, enabled = enabled) { Icon(Icons.Filled.PlayCircle, "Reproduzir") }
        }
    }
}
