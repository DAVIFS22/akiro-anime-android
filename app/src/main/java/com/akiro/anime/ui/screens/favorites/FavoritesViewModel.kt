package com.akiro.anime.ui.screens.favorites

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.akiro.anime.data.storage.FavoriteItem
import com.akiro.anime.data.storage.StorageService
import com.akiro.anime.data.storage.WatchHistoryItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class FavoritesViewModel(application: Application) : AndroidViewModel(application) {
    private val storage = StorageService(application)

    val favorites: StateFlow<List<FavoriteItem>> = storage.favorites.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val history: StateFlow<List<WatchHistoryItem>> = storage.history.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
}
