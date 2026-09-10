@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.akiro.anime.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
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
fun AnimeDetailScreen(
    animeId: String,
    onBack: () -> Unit = {},
    onEpisodeClick: (Episode, String?) -> Unit = { _, _ -> },
    viewModel: DetailViewModel = viewModel()
) {
    val tmdbId = animeId.toIntOrNull() ?: 0
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(tmdbId) {
        viewModel.load(tmdbId)
    }

    LaunchedEffect(state.resolvedStreamUrl) {
        val url = state.resolvedStreamUrl
        val ep = state.resolvedEpisode

        if (!url.isNullOrBlank() && ep != null) {
            onEpisodeClick(ep, url)
            viewModel.consumeResolvedStream()
        }
    }

    when {
        state.isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        state.anime == null -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    state.error
                        ?: "Não foi possível carregar o anime"
                )
            }
        }

        else -> {
            val anime = state.anime!!

            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {

                /*
                 * HEADER
                 */
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(370.dp)
                    ) {

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
                                        listOf(
                                            Color.Black.copy(alpha = 0.10f),
                                            Color.Transparent,
                                            MaterialTheme.colorScheme.background
                                        )
                                    )
                                )
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color.Black.copy(alpha = 0.55f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            SmallFloatingActionButton(
                                onClick = onBack,
                                containerColor =
                                    Color.Black.copy(alpha = 0.55f),
                                contentColor = Color.White
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ArrowBack,
                                    contentDescription = "Voltar"
                                )
                            }

                            Spacer(
                                modifier = Modifier.weight(1f)
                            )

                            SmallFloatingActionButton(
                                onClick = {
                                    viewModel.toggleFavorite()
                                },
                                containerColor =
                                    Color.Black.copy(alpha = 0.55f),
                                contentColor = Color.White
                            ) {
                                Icon(
                                    imageVector =
                                        if (state.isFavorite) {
                                            Icons.Filled.Favorite
                                        } else {
                                            Icons.Filled.FavoriteBorder
                                        },
                                    contentDescription = "Favoritar"
                                )
                            }
                        }

                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(18.dp)
                        ) {

                            Text(
                                text = anime.title,
                                color = Color.White,
                                style =
                                    MaterialTheme.typography.headlineLarge,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Text(
                                text =
                                    anime.nativeTitle
                                        ?: anime.romajiTitle
                                        ?: "",
                                color = Color.White.copy(alpha = 0.70f),
                                style =
                                    MaterialTheme.typography.bodySmall,
                                maxLines = 1
                            )

                            Spacer(
                                modifier = Modifier.height(8.dp)
                            )

                            Row(
                                horizontalArrangement =
                                    Arrangement.spacedBy(7.dp)
                            ) {

                                MetaPill(
                                    text =
                                        "★ ${
                                            "%.1f".format(anime.rating)
                                        }"
                                )

                                MetaPill(
                                    text = anime.year.toString()
                                )

                                MetaPill(
                                    text = anime.type.label
                                )

                                if (anime.ageRating.isNotBlank()) {
                                    MetaPill(
                                        text = anime.ageRating
                                    )
                                }
                            }
                        }
                    }
                }

                /*
                 * ACTIONS
                 */
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement =
                            Arrangement.spacedBy(10.dp)
                    ) {

                        Button(
                            onClick = {
                                state.episodes
                                    .firstOrNull()
                                    ?.let(viewModel::watchEpisode)
                            },
                            enabled = !state.streamLoading,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        ) {

                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null
                            )

                            Spacer(
                                modifier = Modifier.width(6.dp)
                            )

                            Text(
                                text =
                                    if (state.streamLoading) {
                                        "Buscando…"
                                    } else {
                                        "Assistir agora"
                                    }
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.toggleFavorite()
                            },
                            shape = RoundedCornerShape(14.dp)
                        ) {

                            Icon(
                                imageVector =
                                    if (state.isFavorite) {
                                        Icons.Filled.Favorite
                                    } else {
                                        Icons.Filled.BookmarkBorder
                                    },
                                contentDescription = "Salvar"
                            )
                        }
                    }
                }

                /*
                 * GENRES + SYNOPSIS
                 */
                item {

                    if (anime.genres.isNotEmpty()) {

                        LazyRow(
                            contentPadding =
                                PaddingValues(horizontal = 16.dp),
                            horizontalArrangement =
                                Arrangement.spacedBy(7.dp)
                        ) {

                            items(anime.genres) { genre ->

                                AssistChip(
                                    onClick = {},
                                    label = {
                                        Text(
                                            text = genre.name
                                        )
                                    }
                                )
                            }
                        }
                    }

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    Text(
                        text = anime.synopsis,
                        modifier = Modifier.padding(
                            horizontal = 16.dp
                        ),
                        style =
                            MaterialTheme.typography.bodyMedium,
                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant
                    )

                    Spacer(
                        modifier = Modifier.height(18.dp)
                    )
                }

                /*
                 * SEASONS
                 */
                if (
                    anime.seasons.any {
                        it.seasonNumber > 0
                    }
                ) {

                    item {

                        Text(
                            text = "Temporadas",
                            style =
                                MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(
                                horizontal = 16.dp
                            )
                        )

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )

                        LazyRow(
                            contentPadding =
                                PaddingValues(horizontal = 16.dp),
                            horizontalArrangement =
                                Arrangement.spacedBy(8.dp)
                        ) {

                            items(
                                anime.seasons.filter {
                                    it.seasonNumber > 0
                                }
                            ) { season ->

                                FilterChip(
                                    selected =
                                        season.seasonNumber ==
                                            state.selectedSeason,

                                    onClick = {
                                        viewModel.selectSeason(
                                            tmdbId,
                                            season.seasonNumber
                                        )
                                    },

                                    label = {
                                        Text(
                                            text = season.name
                                        )
                                    }
                                )
                            }
                        }

                        Spacer(
                            modifier = Modifier.height(16.dp)
                        )
                    }
                }

                /*
                 * EPISODES HEADER
                 */
                item {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Text(
                            text =
                                "Episódios da temporada " +
                                    state.selectedSeason,
                            style =
                                MaterialTheme.typography.titleLarge,
                            modifier =
                                Modifier.weight(1f)
                        )

                        Text(
                            text =
                                state.episodes.size.toString(),
                            color =
                                MaterialTheme.colorScheme
                                    .onSurfaceVariant
                        )
                    }

                    /*
                     * STREAM LOADING
                     */
                    if (state.streamLoading) {

                        Spacer(
                            modifier = Modifier.height(10.dp)
                        )

                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        )

                        Text(
                            text =
                                "Consultando o Torrentio e procurando " +
                                    "a melhor fonte…",
                            color =
                                MaterialTheme.colorScheme.primary,
                            style =
                                MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(
                                horizontal = 16.dp,
                                vertical = 7.dp
                            )
                        )
                    }

                    /*
                     * STREAM ERROR
                     */
                    state.streamError?.let { error ->

                        Text(
                            text = error,
                            color =
                                MaterialTheme.colorScheme.error,
                            modifier =
                                Modifier.padding(16.dp)
                        )
                    }

                    Spacer(
                        modifier = Modifier.height(6.dp)
                    )
                }

                /*
                 * EPISODES
                 */
                items(
                    items = state.episodes,
                    key = {
                        "${it.seasonNumber}-${it.number}"
                    }
                ) { episode ->

                    EpisodeRow(
                        episode = episode,
                        enabled = !state.streamLoading,
                        onClick = {
                            viewModel.watchEpisode(episode)
                        }
                    )
                }

                item {
                    Spacer(
                        modifier = Modifier.height(28.dp)
                    )
                }
            }
        }
    }
}

/*
 * META PILL
 */
@Composable
private fun MetaPill(
    text: String
) {
    Surface(
        color = Color.Black.copy(alpha = 0.45f),
        shape = RoundedCornerShape(8.dp)
    ) {

        Text(
            text = text,
            color = Color.White,
            style =
                MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(
                horizontal = 8.dp,
                vertical = 5.dp
            )
        )
    }
}

/*
 * EPISODE ROW
 */
@Composable
private fun EpisodeRow(
    episode: Episode,
    enabled: Boolean,
    onClick: () -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 16.dp,
                vertical = 5.dp
            ),
        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(14.dp)
    ) {

        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            /*
             * THUMBNAIL
             */
            Box(
                modifier = Modifier
                    .size(
                        width = 120.dp,
                        height = 68.dp
                    )
                    .clip(
                        RoundedCornerShape(10.dp)
                    )
  
