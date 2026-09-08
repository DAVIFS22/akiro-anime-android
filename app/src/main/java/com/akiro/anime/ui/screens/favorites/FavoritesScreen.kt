package com.akiro.anime.ui.screens.favorites

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.akiro.anime.data.storage.WatchHistoryItem
import com.akiro.anime.ui.components.AnimeCard
import com.akiro.anime.data.model.Anime
import com.akiro.anime.data.model.AnimeStatus
import com.akiro.anime.data.model.AnimeType

@Composable
fun FavoritesScreen(
    onAnimeClick: (String) -> Unit,
    viewModel: FavoritesViewModel = viewModel(),
) {
    val favorites by viewModel.favorites.collectAsState()
    val history by viewModel.history.collectAsState()
    var tab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Favoritos") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Continuar assistindo") })
        }

        if (tab == 0) {
            if (favorites.isEmpty()) {
                EmptyState("Você ainda não favoritou nenhum anime.")
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 120.dp),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(favorites, key = { it.animeId }) { fav ->
                        AnimeCard(
                            anime = Anime(
                                id = fav.animeId,
                                title = fav.title,
                                synopsis = "",
                                poster = fav.poster,
                                banner = fav.poster,
                                year = 0,
                                status = AnimeStatus.EM_EXIBICAO,
                                type = AnimeType.TV,
                                rating = 0.0,
                                ageRating = "",
                                totalEpisodes = 0,
                            ),
                            onClick = { onAnimeClick(fav.animeId) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        } else {
            if (history.isEmpty()) {
                EmptyState("Nenhum episódio assistido ainda.")
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp)) {
                    items(history) { item -> HistoryRow(item, onClick = { onAnimeClick(item.animeId) }) }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(message, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
    }
}

@Composable
private fun HistoryRow(item: WatchHistoryItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = item.poster,
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.width(80.dp).height(110.dp).clip(RoundedCornerShape(8.dp))
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(item.title, style = MaterialTheme.typography.titleSmall)
            Text(
                "T${item.season} • Ep ${item.episode}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            if (item.durationSeconds > 0) {
                LinearProgressIndicator(
                    progress = { (item.positionSeconds.toFloat() / item.durationSeconds).coerceIn(0f, 1f) },
                    modifier = Modifier.width(140.dp).padding(top = 6.dp)
                )
            }
        }
    }
}
