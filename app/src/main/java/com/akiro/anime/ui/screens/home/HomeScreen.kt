@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.akiro.anime.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
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
import com.akiro.anime.data.model.Anime
import com.akiro.anime.ui.components.AnimeCard

@Composable
fun HomeScreen(
    onAnimeClick: (String) -> Unit,
    onSearch: () -> Unit = {},
    viewModel: HomeViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val featured = state.trending.firstOrNull() ?: state.popular.firstOrNull()

    when {
        state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        state.error != null -> ErrorState(state.error ?: "Erro", onRetry = { viewModel.loadHome() })
        else -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            HomeHeader(onSearch)
            featured?.let { FeaturedHero(it, onAnimeClick) }
            AnimeRow("🔥 Em alta agora", state.trending, onAnimeClick)
            AnimeRow("⭐ Mais populares", state.popular, onAnimeClick)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HomeHeader(onSearch: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Akiro", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
            Text("Anime • Filmes • Séries", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onSearch) { Icon(Icons.Filled.Search, "Explorar") }
        IconButton(onClick = {}) { Icon(Icons.Filled.NotificationsNone, "Notificações") }
    }
}

@Composable
private fun FeaturedHero(anime: Anime, onAnimeClick: (String) -> Unit) {
    Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp).height(330.dp).clip(RoundedCornerShape(24.dp))) {
        AsyncImage(model = anime.backdrop ?: anime.banner ?: anime.poster, contentDescription = anime.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent, Color.Black.copy(.94f)))))
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color.Black.copy(.55f), Color.Transparent))))
        Column(Modifier.align(Alignment.BottomStart).padding(20.dp)) {
            Surface(color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(50.dp)) {
                Text("DESTAQUE", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(anime.title, color = Color.White, style = MaterialTheme.typography.headlineMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("★ ${"%.1f".format(anime.rating)}  •  ${anime.year}  •  ${anime.type.label}", color = Color.White.copy(.82f), style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(12.dp))
            Button(onClick = { onAnimeClick(anime.id) }, shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Filled.PlayArrow, null)
                Spacer(Modifier.width(5.dp))
                Text("Assistir agora")
            }
        }
    }
}

@Composable
private fun AnimeRow(title: String, animes: List<Anime>, onAnimeClick: (String) -> Unit) {
    if (animes.isEmpty()) return
    Column(Modifier.padding(top = 12.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            Text("Ver todos ›", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
        }
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(horizontal = 18.dp)) {
            items(animes, key = { it.id }) { anime -> AnimeCard(anime, { onAnimeClick(anime.id) }) }
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Não foi possível carregar o Akiro", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp)); Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(18.dp)); Button(onClick = onRetry) { Text("Tentar novamente") }
    }
}
