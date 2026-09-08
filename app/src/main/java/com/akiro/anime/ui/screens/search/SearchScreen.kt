package com.akiro.anime.ui.screens.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.akiro.anime.ui.components.AnimeCard

@Composable
fun SearchScreen(
    onAnimeClick: (String) -> Unit,
    viewModel: SearchViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Buscar anime...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
        )

        Spacer(Modifier.height(16.dp))

        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }
            state.error != null -> {
                Text(state.error ?: "Erro")
            }
            state.query.isBlank() -> {
                Text(
                    "Digite o nome de um anime para buscar.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            state.results.isEmpty() -> {
                Text("Nenhum resultado encontrado.")
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.results, key = { it.id }) { anime ->
                        AnimeCard(anime = anime, onClick = { onAnimeClick(anime.id) }, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}
