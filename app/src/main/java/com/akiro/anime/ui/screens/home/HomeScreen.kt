package com.akiro.anime.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.akiro.anime.data.model.Anime
import com.akiro.anime.ui.components.AnimeCard

@Composable
fun HomeScreen(
    onAnimeClick: (String) -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Akiro Anime",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(16.dp)
        )

        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.error != null -> {
                Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Não foi possível carregar o catálogo.")
                    Text(state.error ?: "", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { viewModel.loadHome() }) { Text("Tentar novamente") }
                }
            }
            else -> {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    AnimeRow(title = "Em alta", animes = state.trending, onAnimeClick = onAnimeClick)
                    AnimeRow(title = "Populares", animes = state.popular, onAnimeClick = onAnimeClick)
                }
            }
        }
    }
}

@Composable
private fun AnimeRow(
    title: String,
    animes: List<Anime>,
    onAnimeClick: (String) -> Unit,
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(animes) { anime ->
                AnimeCard(anime = anime, onClick = { onAnimeClick(anime.id) })
            }
        }
    }
}
