package com.akiro.anime.data.model

data class Episode(
    val id: String,
    val number: Int,
    val seasonNumber: Int,
    val title: String,
    val thumbnail: String,
    val duration: Int, // seconds
    val description: String? = null,
    val airDate: String? = null,
    val streamSources: List<StreamSource> = emptyList(),
)

data class Season(
    val seasonNumber: Int,
    val name: String,
    val episodeCount: Int,
    val episodes: List<Episode> = emptyList(),
    val year: Int? = null,
)

data class AnimeCast(
    val id: Int,
    val name: String,
    val character: String,
    val profilePath: String?,
)

enum class AnimeStatus(val label: String) {
    EM_EXIBICAO("Em exibição"),
    CONCLUIDO("Concluído"),
    EM_BREVE("Em breve"),
}

enum class AnimeType(val label: String) {
    TV("TV"),
    FILME("Filme"),
    OVA("OVA"),
    ESPECIAL("Especial"),
}

data class Anime(
    val id: String,
    val tmdbId: Int? = null,
    val malId: Int? = null,
    val imdbId: String? = null,
    val kitsuId: String? = null,
    val title: String,
    val romajiTitle: String? = null,
    val nativeTitle: String? = null,
    val synopsis: String,
    val poster: String,
    val banner: String,
    val backdrop: String? = null,
    val genres: List<Genre> = emptyList(),
    val year: Int,
    val status: AnimeStatus,
    val type: AnimeType,
    val rating: Double,
    val ratingScoreCount: Int? = null,
    val ageRating: String,
    val studios: List<String> = emptyList(),
    val totalEpisodes: Int,
    val seasons: List<Season> = emptyList(),
    val cast: List<AnimeCast> = emptyList(),
    val featured: Boolean = false,
    val popular: Boolean = false,
    val recent: Boolean = false,
    val trending: Boolean = false,
    val simulcast: Boolean = false,
)

data class Subtitle(
    val id: String,
    val lang: String,
    val label: String,
    val url: String? = null,
    val format: String? = null, // "vtt" | "srt"
)

data class StreamSource(
    val id: String,
    val name: String,
    val url: String,
    val type: String? = null, // "mp4" | "hls" | "embed" | "iframe"
    val quality: String, // "1080p" | "720p" | "480p" | "Auto"
    val language: String? = null,
    val subtitles: List<Subtitle> = emptyList(),
    val addonId: String? = null,
    val playableInBrowser: Boolean? = null,
)

data class CatalogFilter(
    val query: String? = null,
    val genre: String? = null,
    val year: String? = null,
    val status: String? = null,
    val type: String? = null,
    val sortBy: String? = null, // "popular" | "rating" | "recent" | "title"
)
