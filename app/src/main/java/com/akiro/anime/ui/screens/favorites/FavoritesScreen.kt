package com.akiro.anime.ui.screens.favorites

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.akiro.anime.data.model.*
import com.akiro.anime.data.storage.WatchHistoryItem
import com.akiro.anime.ui.components.AnimeCard

@Composable
fun FavoritesScreen(onAnimeClick: (String) -> Unit, viewModel: FavoritesViewModel = viewModel()) {
    val favorites by viewModel.favorites.collectAsState()
    val history by viewModel.history.collectAsState()
    var tab by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(16.dp))
        Text("Minha lista", style = MaterialTheme.typography.headlineMedium)
        Text("Seus favoritos e o que você estava assistindo", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(14.dp))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            SegmentedButton(selected = tab == 0, onClick = { tab = 0 }, shape = SegmentedButtonDefaults.itemShape(0, 2)) { Text("Favoritos") }
            SegmentedButton(selected = tab == 1, onClick = { tab = 1 }, shape = SegmentedButtonDefaults.itemShape(1, 2)) { Text("Continuar") }
        }
        Spacer(Modifier.height(12.dp))
        if (tab == 0) {
            if (favorites.isEmpty()) EmptyState("Você ainda não favoritou nenhum anime.") else LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 135.dp),
                contentPadding = PaddingValues(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) { items(favorites, key = { it.animeId }) { fav ->
                AnimeCard(Anime(fav.animeId, title = fav.title, synopsis = "", poster = fav.poster, banner = fav.poster, year = 0, status = AnimeStatus.EM_EXIBICAO, type = AnimeType.TV, rating = 0.0, ageRating = "", totalEpisodes = 0), { onAnimeClick(fav.animeId) }, Modifier.fillMaxWidth())
            } }
        } else {
            if (history.isEmpty()) EmptyState("Nenhum episódio assistido ainda.") else LazyColumn(contentPadding = PaddingValues(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { items(history) { item -> HistoryRow(item, { onAnimeClick(item.animeId) }) } }
        }
    }
}

@Composable private fun EmptyState(message: String) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant) } }

@Composable private fun HistoryRow(item: WatchHistoryItem, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = item.poster, contentDescription = item.title, contentScale = ContentScale.Crop, modifier = Modifier.size(width = 110.dp, height = 68.dp).clip(RoundedCornerShape(10.dp)))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleSmall)
                Text("T${item.season} • Episódio ${item.episode}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                if (item.durationSeconds > 0) LinearProgressIndicator({ (item.positionSeconds.toFloat() / item.durationSeconds).coerceIn(0f, 1f) }, Modifier.fillMaxWidth().padding(top = 7.dp))
            }
            Icon(Icons.Filled.PlayArrow, null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}
