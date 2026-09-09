package com.akiro.anime.ui.screens.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.akiro.anime.ui.components.AnimeCard

@Composable
fun SearchScreen(onAnimeClick: (String) -> Unit, viewModel: SearchViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(14.dp))
        Text("Explorar", style = MaterialTheme.typography.headlineMedium)
        Text("Encontre seu próximo anime", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(14.dp))
        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Buscar anime, filmes, séries...") },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Todos", "Anime", "Filmes", "Séries").forEachIndexed { index, label ->
                FilterChip(selected = index == 0, onClick = {}, label = { Text(label) })
            }
        }
        Spacer(Modifier.height(16.dp))
        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            state.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(state.error ?: "Erro") }
            state.query.isBlank() -> {
                Text("Digite o nome de um anime para começar.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(18.dp))
                Text("Gêneros", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(10.dp))
                GenreCloud()
            }
            state.results.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Nenhum resultado encontrado.") }
            else -> {
                Text("Resultados", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(10.dp))
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 135.dp),
                    contentPadding = PaddingValues(bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) { items(state.results, key = { it.id }) { anime -> AnimeCard(anime, { onAnimeClick(anime.id) }, Modifier.fillMaxWidth()) } }
            }
        }
    }
}

@Composable
private fun GenreCloud() {
    val genres = listOf("Ação", "Aventura", "Comédia", "Drama", "Fantasia", "Terror", "Romance", "Sci-Fi")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        genres.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { row.forEach { label -> AssistChip(onClick = {}, label = { Text(label) }) } }
        }
    }
}
