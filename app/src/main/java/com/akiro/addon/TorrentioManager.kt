package com.akiro.addon

import android.util.Log
import com.akiro.anime.data.model.Anime
import com.akiro.anime.data.model.Episode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/** Resolves anime episodes through the public Torrentio Stremio stream endpoint. */
object TorrentioManager {
    private const val TORRENTIO_BASE = "https://torrentio.strem.fun/"
    private const val KITSU_BASE = "https://kitsu.io/api/edge/"
    private val client = OkHttpClient.Builder().build()

    suspend fun findBestStream(anime: Anime, episode: Episode): String? = withContext(Dispatchers.IO) {
        try {
            val kitsuId = anime.kitsuId?.let(::normalizeId) ?: findKitsuId(anime)
            if (!kitsuId.isNullOrBlank()) {
                // Try the season-aware anime resource first, then the Kitsu episode convention.
                val paths = listOf(
                    "stream/anime/kitsu:$kitsuId:${episode.seasonNumber}:${episode.number}.json",
                    "stream/anime/kitsu:$kitsuId:${episode.number}.json"
                )
                for (path in paths) {
                    parseResponse(path)?.let { return@withContext it }
                }
            }

            anime.imdbId?.takeIf { it.isNotBlank() }?.let { imdb ->
                parseResponse("stream/series/$imdb:${episode.seasonNumber}:${episode.number}.json")
                    ?.let { return@withContext it }
            }

            Log.w(TAG, "Nenhuma fonte para ${anime.title} S${episode.seasonNumber}E${episode.number}")
            null
        } catch (t: Throwable) {
            Log.e(TAG, "Falha ao consultar Torrentio", t)
            null
        }
    }

    private fun parseResponse(path: String): String? {
        val url = TORRENTIO_BASE.toHttpUrl().newBuilder().addPathSegments(path).build()
        val request = Request.Builder().url(url).header("Accept", "application/json").build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.w(TAG, "Torrentio HTTP ${response.code}: $path")
                return null
            }
            return parseBestStream(response.body?.string().orEmpty())
        }
    }

    private fun parseBestStream(body: String): String? {
        val streams = JSONObject(body).optJSONArray("streams") ?: return null
        val candidates = mutableListOf<Candidate>()
        for (i in 0 until streams.length()) {
            val item = streams.optJSONObject(i) ?: continue
            val url = item.optString("url").takeIf(String::isNotBlank)
            val hash = item.optString("infoHash").takeIf(String::isNotBlank)
            val playable = when {
                url?.startsWith("magnet:") == true || url?.startsWith("http://") == true || url?.startsWith("https://") == true -> url
                hash != null -> "magnet:?xt=urn:btih:$hash"
                else -> null
            } ?: continue
            val title = item.optString("title").ifBlank { item.optString("name") }
            candidates += Candidate(score(title, item), playable)
        }
        return candidates.maxByOrNull { it.score }?.url
    }

    private fun score(title: String, item: JSONObject): Int {
        val t = title.lowercase(Locale.US)
        var score = 0
        when {
            "1080p" in t -> score += 50
            "720p" in t -> score += 35
            "480p" in t -> score += 15
        }
        if ("hevc" in t || "x265" in t) score += 8
        if ("dual" in t || "multi" in t || "dual audio" in t) score += 5
        if ("cam" in t || "hdcam" in t) score -= 100
        if (item.has("behaviorHints")) score += 2
        return score
    }

    private fun findKitsuId(anime: Anime): String? {
        val queries = listOfNotNull(anime.nativeTitle, anime.romajiTitle, anime.title)
            .map(::cleanText).filter(String::isNotBlank).distinct()
        var globalBest: Match? = null

        for (query in queries) {
            val url = KITSU_BASE.toHttpUrl().newBuilder()
                .addPathSegment("anime")
                .addQueryParameter("filter[text]", query)
                .addQueryParameter("page[limit]", "20")
                .build()
            val request = Request.Builder().url(url).header("Accept", "application/vnd.api+json").build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use
                val data = JSONObject(response.body?.string().orEmpty()).optJSONArray("data") ?: return@use
                for (i in 0 until data.length()) {
                    val item = data.optJSONObject(i) ?: continue
                    val attrs = item.optJSONObject("attributes") ?: continue
                    val names = mutableListOf<String>()
                    attrs.optString("canonicalTitle").takeIf(String::isNotBlank)?.let(names::add)
                    attrs.optString("slug").takeIf(String::isNotBlank)?.let(names::add)
                    attrs.optJSONObject("titles")?.let { titles ->
                        val keys = titles.keys()
                        while (keys.hasNext()) {
                            val value = titles.optString(keys.next())
                            if (value.isNotBlank()) names += value
                        }
                    }
                    val score = names.maxOfOrNull { similarity(query, cleanText(it)) } ?: 0
                    val id = item.optString("id").takeIf(String::isNotBlank) ?: continue
                    if (globalBest == null || score > globalBest!!.score) globalBest = Match(id, score)
                }
            }
            if (globalBest?.score ?: 0 >= 100) break
        }
        return globalBest?.takeIf { it.score >= 55 }?.id
    }

    private fun similarity(query: String, candidate: String): Int {
        if (query == candidate) return 100
        if (candidate.contains(query) || query.contains(candidate)) return 85
        val q = query.split(' ').filter { it.length > 2 }.toSet()
        val c = candidate.split(' ').filter { it.length > 2 }.toSet()
        if (q.isEmpty()) return 0
        val overlap = q.intersect(c).size.toDouble() / q.size
        return (overlap * 80).toInt()
    }

    private fun cleanText(value: String): String = value
        .lowercase(Locale.US)
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        .trim()
        .replace(Regex("\\s+"), " ")

    private fun normalizeId(value: String): String = value.removePrefix("kitsu:").trim()
    private data class Candidate(val score: Int, val url: String)
    private data class Match(val id: String, val score: Int)
    private const val TAG = "TorrentioManager"
}
