package com.akiro.anime.ui.screens.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.akiro.anime.data.model.Anime
import com.akiro.anime.data.model.Episode
import com.akiro.anime.data.repository.AnimeRepository
import com.akiro.anime.data.storage.FavoriteItem
import com.akiro.anime.data.storage.StorageService
import com.akiro.anime.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DetailUiState(
    val isLoading: Boolean = true,
    val anime: Anime? = null,
    val selectedSeason: Int = 1,
    val episodes: List<Episode> = emptyList(),
    val isFavorite: Boolean = false,
    val error: String? = null,
)

class DetailViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AnimeRepository()
    private val storage = StorageService(application)

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    fun load(tmdbId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val anime = repository.getDetails(tmdbId)
                val firstSeason = anime.seasons.firstOrNull { it.seasonNumber > 0 } ?: anime.seasons.firstOrNull()
                val isFav = storage.isFavorite(anime.id)
                _uiState.value = DetailUiState(
                    isLoading = false,
                    anime = anime,
                    selectedSeason = firstSeason?.seasonNumber ?: 1,
                    isFavorite = isFav,
                )
                firstSeason?.let { loadSeasonEpisodes(tmdbId, it.seasonNumber) }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Erro ao carregar detalhes")
            }
        }
    }

    fun selectSeason(tmdbId: Int, seasonNumber: Int) {
        _uiState.value = _uiState.value.copy(selectedSeason = seasonNumber, episodes = emptyList())
        loadSeasonEpisodes(tmdbId, seasonNumber)
    }

    private fun loadSeasonEpisodes(tmdbId: Int, seasonNumber: Int) {
        viewModelScope.launch {
            try {
                val season = RetrofitClient.tmdbApi.getSeasonDetails(
                    tmdbId, seasonNumber, com.akiro.anime.BuildConfig.TMDB_API_KEY
                )
                val episodes = season.episodes.map {
                    Episode(
                        id = "${tmdbId}-s${it.season_number}-e${it.episode_number}",
                        number = it.episode_number,
                        seasonNumber = it.season_number,
                        title = it.name ?: "Episódio ${it.episode_number}",
                        thumbnail = com.akiro.anime.data.repository.backdropUrl(it.still_path, "w500"),
                        duration = (it.runtime ?: 24) * 60,
                        description = it.overview,
                        airDate = it.air_date,
                    )
                }
                _uiState.value = _uiState.value.copy(episodes = episodes)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Não foi possível carregar os episódios")
            }
        }
    }

    fun toggleFavorite() {
        val anime = _uiState.value.anime ?: return
        viewModelScope.launch {
            val nowFavorite = storage.toggleFavorite(
                FavoriteItem(animeId = anime.id, title = anime.title, poster = anime.poster)
            )
            _uiState.value = _uiState.value.copy(isFavorite = nowFavorite)
        }
    }
}
