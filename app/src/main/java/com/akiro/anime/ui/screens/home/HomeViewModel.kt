package com.akiro.anime.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akiro.anime.data.model.Anime
import com.akiro.anime.data.repository.AnimeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val trending: List<Anime> = emptyList(),
    val popular: List<Anime> = emptyList(),
    val error: String? = null,
)

class HomeViewModel(
    private val repository: AnimeRepository = AnimeRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHome()
    }

    fun loadHome() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val trending = repository.getTrending()
                val popular = repository.getPopular()
                _uiState.value = HomeUiState(isLoading = false, trending = trending, popular = popular)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Erro ao carregar catálogo")
            }
        }
    }
}
