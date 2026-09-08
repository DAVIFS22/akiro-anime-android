package com.akiro.anime.data.storage

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "akiro_anime_prefs")

data class FavoriteItem(
    val animeId: String,
    val title: String,
    val poster: String,
    val addedAt: Long = System.currentTimeMillis(),
)

data class WatchHistoryItem(
    val animeId: String,
    val title: String,
    val poster: String,
    val season: Int,
    val episode: Int,
    val positionSeconds: Long,
    val durationSeconds: Long,
    val updatedAt: Long = System.currentTimeMillis(),
)

// Mirrors StorageService from the web version (src/storage/localStorage.ts),
// but backed by DataStore instead of localStorage.
class StorageService(private val context: Context) {
    private val gson = Gson()

    private val favoritesKey = stringPreferencesKey("favorites")
    private val historyKey = stringPreferencesKey("history")

    val favorites: Flow<List<FavoriteItem>> = context.dataStore.data.map { prefs ->
        val json = prefs[favoritesKey] ?: "[]"
        val type = object : TypeToken<List<FavoriteItem>>() {}.type
        gson.fromJson(json, type) ?: emptyList()
    }

    val history: Flow<List<WatchHistoryItem>> = context.dataStore.data.map { prefs ->
        val json = prefs[historyKey] ?: "[]"
        val type = object : TypeToken<List<WatchHistoryItem>>() {}.type
        gson.fromJson(json, type) ?: emptyList()
    }

    suspend fun toggleFavorite(item: FavoriteItem): Boolean {
        var nowFavorite = false
        context.dataStore.edit { prefs ->
            val type = object : TypeToken<List<FavoriteItem>>() {}.type
            val current: List<FavoriteItem> = gson.fromJson(prefs[favoritesKey] ?: "[]", type) ?: emptyList()
            val exists = current.any { it.animeId == item.animeId }
            val updated = if (exists) {
                current.filter { it.animeId != item.animeId }
            } else {
                nowFavorite = true
                listOf(item) + current
            }
            prefs[favoritesKey] = gson.toJson(updated)
        }
        return nowFavorite
    }

    suspend fun isFavorite(animeId: String): Boolean {
        var result = false
        context.dataStore.data.map { prefs ->
            val type = object : TypeToken<List<FavoriteItem>>() {}.type
            val current: List<FavoriteItem> = gson.fromJson(prefs[favoritesKey] ?: "[]", type) ?: emptyList()
            result = current.any { it.animeId == animeId }
        }
        return result
    }

    suspend fun saveWatchProgress(item: WatchHistoryItem) {
        context.dataStore.edit { prefs ->
            val type = object : TypeToken<List<WatchHistoryItem>>() {}.type
            val current: List<WatchHistoryItem> = gson.fromJson(prefs[historyKey] ?: "[]", type) ?: emptyList()
            val filtered = current.filterNot {
                it.animeId == item.animeId && it.season == item.season && it.episode == item.episode
            }
            val updated = (listOf(item) + filtered).take(50)
            prefs[historyKey] = gson.toJson(updated)
        }
    }

    suspend fun clearHistory() {
        context.dataStore.edit { prefs -> prefs[historyKey] = "[]" }
    }
}
