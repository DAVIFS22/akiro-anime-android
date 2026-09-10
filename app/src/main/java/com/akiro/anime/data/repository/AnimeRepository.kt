package com.akiro.anime.data.repository

import com.akiro.anime.BuildConfig
import com.akiro.anime.data.model.Anime
import com.akiro.anime.data.model.AnimeStatus
import com.akiro.anime.data.model.AnimeType
import com.akiro.anime.data.model.Season
import com.akiro.anime.network.RetrofitClient
import com.akiro.anime.network.AkiroApiClient
import com.akiro.anime.network.TmdbTvResult

fun posterUrl(path: String?, size: String = "w500"): String =
    if (path.isNullOrEmpty()) {
        "https://image.tmdb.org/t/p/w500"
    } else {
        "https://image.tmdb.org/t/p/$size$path"
    }

fun backdropUrl(path: String?, size: String = "w1280"): String =
    if (path.isNullOrEmpty()) {
        "https://image.tmdb.org/t/p/w1280"
    } else {
        "https://image.tmdb.org/t/p/$size$path"
    }

fun TmdbTvResult.toAnime(): Anime = Anime(
    id = id.toString(),
    tmdbId = id,
    title = name,
    romajiTitle = original_name,
    synopsis = overview.orEmpty(),
    poster = posterUrl(poster_path),
    banner = backdropUrl(backdrop_path),
    backdrop = backdropUrl(backdrop_path),
    genres = emptyList(),
    year = first_air_date?.take(4)?.toIntOrNull() ?: 0,
    status = AnimeStatus.EM_EXIBICAO,
    type = AnimeType.TV,
    rating = vote_average,
    ratingScoreCount = vote_count,
    ageRating = "14+",
    totalEpisodes = 0,
    seasons = emptyList<Season>(),
)

fun com.akiro.anime.network.AnimeDto.toDomain(): Anime = Anime(
    id = id,
    tmdbId = tmdb_id,
    malId = mal_id,
    imdbId = imdb_id,
    kitsuId = kitsu_id,
    title = title,
    romajiTitle = romaji_title,
    nativeTitle = native_title,
    synopsis = synopsis,
    poster = poster,
    banner = banner,
    backdrop = backdrop,
    genres = genres,
    year = year,
    status = runCatching {
        AnimeStatus.valueOf(status)
    }.getOrDefault(AnimeStatus.EM_EXIBICAO),
    type = runCatching {
        AnimeType.valueOf(type)
    }.getOrDefault(AnimeType.TV),
    rating = rating,
    ratingScoreCount = rating_score_count,
    ageRating = age_rating,
    studios = studios,
    totalEpisodes = total_episodes,
    seasons = seasons.map {
        Season(
            it.season_number,
            it.name,
            it.episode_count,
            year = it.year
        )
    }
)

private fun com.akiro.anime.network.EpisodeDto.toDomain() =
    com.akiro.anime.data.model.Episode(
        id = id,
        number = number,
        seasonNumber = 0,
        title = title,
        thumbnail = thumbnail,
        duration = duration,
        description = description,
        airDate = air_date
    )

class AnimeRepository {

    private val api = RetrofitClient.tmdbApi

    private val apiKey
        get() = BuildConfig.TMDB_API_KEY

    suspend fun getTrending(): List<Anime> {
        if (AkiroApiClient.isConfigured) {
            return AkiroApiClient.api
                .trending(30)
                .items
                .map { it.toDomain() }
        }

        return api.getTrending(apiKey)
            .results
            .filter {
                it.genre_ids.contains(16) ||
                    it.original_language == "ja"
            }
            .map { it.toAnime() }
    }

    suspend fun getPopular(): List<Anime> {
        if (AkiroApiClient.isConfigured) {
            return AkiroApiClient.api
                .popular(30)
                .items
                .map { it.toDomain() }
        }

        return api.getPopularAnime(apiKey)
            .results
            .map { it.toAnime() }
    }

    suspend fun search(query: String): List<Anime> {
        if (AkiroApiClient.isConfigured) {
            return AkiroApiClient.api
                .search(query)
                .items
                .map { it.toDomain() }
        }

        return api.searchAnime(apiKey, query)
            .results
            .sortedByDescending {
                it.original_language == "ja"
            }
            .map { it.toAnime() }
    }

    suspend fun getDetails(tmdbId: Int): Anime {

        if (AkiroApiClient.isConfigured) {
            return AkiroApiClient.api
                .getAnime(tmdbId)
                .toDomain()
        }

        val d = api.getAnimeDetails(
            tmdbId,
            apiKey
        )

        return Anime(
            id = d.id.toString(),

            tmdbId = d.id,

            imdbId = d.external_ids?.imdb_id,

            title = d.name,

            romajiTitle = d.original_name,

            synopsis = d.overview.orEmpty(),

            poster = posterUrl(d.poster_path),

            banner = backdropUrl(d.backdrop_path),

            backdrop = backdropUrl(d.backdrop_path),

            genres = d.genres.map {
                com.akiro.anime.data.model.Genre(
                    id = it.id,
                    name = it.name
                )
            },

            year = d.first_air_date
                ?.take(4)
                ?.toIntOrNull()
                ?: 0,

            status = if (
                d.status == "Ended" ||
                d.status == "Canceled"
            ) {
                AnimeStatus.CONCLUIDO
            } else {
                AnimeStatus.EM_EXIBICAO
            },

            type = AnimeType.TV,

            rating = d.vote_average,

            ratingScoreCount = d.vote_count,

            ageRating = "14+",

            totalEpisodes = d.number_of_episodes ?: 0,

            seasons = d.seasons.map {
                Season(
                    seasonNumber = it.season_number,
                    name = it.name,
                    episodeCount = it.episode_count,
                    year = it.air_date
                        ?.take(4)
                        ?.toIntOrNull()
                )
            }
        )
    }

    suspend fun getSeasonEpisodes(
        tmdbId: Int,
        seasonNumber: Int
    ): List<com.akiro.anime.data.model.Episode> {

        if (AkiroApiClient.isConfigured) {
            return AkiroApiClient.api
                .getEpisodes(
                    tmdbId,
                    seasonNumber
                )
                .items
                .map {
                    it.toDomain()
                        .copy(
                            seasonNumber = seasonNumber
                        )
                }
        }

        val season = api.getSeasonDetails(
            tmdbId,
            seasonNumber,
            apiKey
        )

        return season.episodes.map { ep ->

            com.akiro.anime.data.model.Episode(
                id = "$tmdbId-s${ep.season_number}-e${ep.episode_number}",

                number = ep.episode_number,

                seasonNumber = ep.season_number,

                title = ep.name
                    ?: "Episódio ${ep.episode_number}",

                thumbnail = backdropUrl(
                    ep.still_path,
                    "w500"
                ),

                duration = (ep.runtime ?: 24) * 60,

                description = ep.overview,

                airDate = ep.air_date
            )
        }
    }
}
