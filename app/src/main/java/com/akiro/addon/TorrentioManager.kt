package com.akiro.addon

import android.util.Log
import com.akiro.anime.data.model.Anime
import com.akiro.anime.data.model.Episode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Torrentio resolver used by the real player flow.
 *
 * The resolver is deliberately defensive: anime IDs can come from TMDB while
 * Torrentio expects Stremio resource IDs (Kitsu/IMDb). We therefore resolve
 * Kitsu first, try both common episode-ID conventions, then IMDb as a fallback.
 */object TorrentioManager {
    private const val TORRENTIO_BASE = "https://torrentio.strem.fun/"
    private const val KITSU_BASE = "https://kitsu.io/api/edge/"
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .writeTimeout(8, TimeUnit.SECONDS)
        .callTimeout(18, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val kitsuCache = ConcurrentHashMap<String, String>()

    data class TorrentStreamSource(
        val title: String,
        val url: String,
        val quality: String,
        val size: String,
        val provider: String,
        val score: Int,
    )

    suspend fun findStreams(anime: Anime, episode: Episode): List<TorrentStreamSource> = withContext(Dispatchers.IO) {
        val result = linkedMapOf<String, TorrentStreamSource>()
        try {
            val kitsuId = anime.kitsuId?.let(::normalizeId) ?: findKitsuId(anime)
            if (!kitsuId.isNullOrBlank()) {
                val ids = linkedSetOf(
                    "kitsu:$kitsuId:${episode.seasonNumber}:${episode.number}",
                    "kitsu:$kitsuId:${episode.number}",
                )
                for (id in ids) {
                    parseResponse("stream/anime/$id.json").forEach { source -> result.putIfAbsent(source.url, source) }
                    if (result.size >= 12) break
                }
            }

            anime.imdbId?.takeIf { it.isNotBlank() }?.let { imdb ->
                parseResponse("stream/series/$imdb:${episode.seasonNumber}:${episode.number}.json")
                    .forEach { source -> result.putIfAbsent(source.url, source) }
            }

            result.values.sortedWith(compareByDescending<TorrentStreamSource> { it.score }.thenBy { it.size }).take(12)
        } catch (t: Throwable) {
            Log.e(TAG, "Falha ao consultar Torrentio", t)
            emptyList()
        }
    }

    suspend fun findBestStream(anime: Anime, episode: Episode): String? =
        findStreams(anime, episode).firstOrNull()?.url

    private fun parseResponse(path: String): List<TorrentStreamSource> {
        val url = TORRENTIO_BASE.toHttpUrl().newBuilder().addPathSegments(path).build()
        val request = Request.Builder().url(url).header("Accept", "application/json").build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.w(TAG, "Torrentio HTTP ${response.code}: $path")
                return emptyList()
            }
            return parseStreams(response.body?.string().orEmpty())
        }
    }

    private fun parseStreams(body: String): List<TorrentStreamSource> {
        val streams = JSONObject(body).optJSONArray("streams") ?: return emptyList()
        val candidates = mutableListOf<TorrentStreamSource>()
        for (i in 0 until streams.length()) {
            val item = streams.optJSONObject(i) ?: continue
            val rawUrl = item.optString("url").trim()
            val hash = item.optString("infoHash").trim()
            val playable = when {
                rawUrl.startsWith("magnet:", true) || rawUrl.startsWith("http://", true) || rawUrl.startsWith("https://", true) -> rawUrl
                hash.isNotBlank() -> "magnet:?xt=urn:btih:$hash"
                else -> null
            } ?: continue

            val title = item.optString("title").ifBlank { item.optString("name") }.ifBlank { "Fonte Torrentio" }
            val quality = extractQuality(title)
            val size = extractSize(title)
            val provider = extractProvider(title)
            candidates += TorrentStreamSource(
                title = title.replace("\n", " ").trim(),
                url = playable,
                quality = quality,
                size = size,
                provider = provider,
                score = score(title, item),
            )
        }
        return candidates.sortedByDescending { it.score }
    }

    private fun score(title: String, item: JSONObject): Int {
        val t = title.lowercase(Locale.US)
        var score = 0
        when {
            "2160p" in t || "4k" in t -> score += 65
            "1080p" in t -> score += 50
            "720p" in t -> score += 35
            "480p" in t -> score += 15
        }
        if ("hevc" in t || "x265" in t) score += 8
        if ("av1" in t) score += 5
        if ("dual" in t || "multi" in t || "dual audio" in t) score += 5
        if ("10bit" in t || "10-bit" in t) score += 2
        if ("cam" in t || "hdcam" in t || "ts" in t) score -= 100
        if ("sample" in t || "trailer" in t) score -= 80
        if (item.has("behaviorHints")) score += 2
        return score
    }

    private fun extractQuality(title: String): String {
        val t = title.lowercase(Locale.US)
        return when {
            "2160p" in t || "4k" in t -> "4K"
            "1080p" in t -> "1080p"
            "720p" in t -> "720p"
            "480p" in t -> "480p"
            else -> "Auto"
        }
    }

    private fun extractSize(title: String): String = Regex("\\b(?:\\d+(?:\\.\\d+)?)(?:gb|mb)\\b", RegexOption.IGNORE_CASE)
        .find(title)?.value ?: "Tamanho não informado"

    private fun extractProvider(title: String): String {
        val lower = title.lowercase(Locale.US)
        val known = listOf("nyaa", "eztv", "1337x", "torrentgalaxy", "thepiratebay", "anidex", "tokyotosho")
        return known.firstOrNull { lower.contains(it) }?.uppercase(Locale.US) ?: "Torrentio"
    }

    private fun findKitsuId(anime: Anime): String? {
        val queries = listOfNotNull(anime.romajiTitle, anime.nativeTitle, anime.title)
            .map(::cleanText).filter { it.length >= 2 }.distinct()

        for (query in queries) {
            kitsuCache[query]?.let { return it }
            val url = KITSU_BASE.toHttpUrl().newBuilder()
                .addPathSegment("anime")
                .addQueryParameter("filter[text]", query)
                .addQueryParameter("page[limit]", "20")
                .build()
            val request = Request.Builder().url(url).header("Accept", "application/vnd.api+json").build()
            runCatching {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use null
                    val data = JSONObject(response.body?.string().orEmpty()).optJSONArray("data") ?: return@use null
                    var best: Match? = null
                    for (i in 0 until data.length()) {
                        val item = data.optJSONObject(i) ?: continue
                        val attrs = item.optJSONObject("attributes") ?: continue
                        val names = mutableListOf<String>()
                        attrs.optString("canonicalTitle").takeIf { it.isNotBlank() }?.let(names::add)
                        attrs.optString("slug").takeIf { it.isNotBlank() }?.let(names::add)
                        attrs.optJSONObject("titles")?.let { titles ->
                            val keys = titles.keys()
                            while (keys.hasNext()) titles.optString(keys.next()).takeIf { it.isNotBlank() }?.let(names::add)
                        }
                        val id = item.optString("id").takeIf { it.isNotBlank() } ?: continue
                        val similarity = names.maxOfOrNull { similarity(query, cleanText(it)) } ?: 0
                        if (best == null || similarity > best!!.score) best = Match(id, similarity)
                    }
                    best?.takeIf { it.score >= 55 }?.let {
                        kitsuCache[query] = it.id
                        return it.id
                    }
                }
            }.onFailure { Log.w(TAG, "Kitsu search failed for '$query'", it) }
        }
        return null
    }

    private fun similarity(query: String, candidate: String): Int {
        if (query == candidate) return 100
        if (candidate.contains(query) || query.contains(candidate)) return 85
        val q = query.split(' ').filter { it.length > 2 }.toSet()
        val c = candidate.split(' ').filter { it.length > 2 }.toSet()
        if (q.isEmpty()) return 0
        return ((q.intersect(c).size.toDouble() / q.size) * 80).toInt()
    }

    private fun cleanText(value: String): String = value
        .lowercase(Locale.US)
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        .trim()
        .replace(Regex("\\s+"), " ")

    private fun normalizeId(value: String): String = value.removePrefix("kitsu:").trim()
    private data class Match(val id: String, val score: Int)
    private const val TAG = "TorrentioManager"
}
