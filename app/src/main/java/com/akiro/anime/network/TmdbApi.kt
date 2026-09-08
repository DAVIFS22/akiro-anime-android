package com.akiro.anime.network

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

// Native apps aren't subject to browser CORS, so unlike the web version's
// server.ts proxy, this calls api.themoviedb.org directly.
interface TmdbApi {

    @GET("trending/tv/week")
    suspend fun getTrending(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "pt-BR",
    ): TmdbListResponse

    @GET("discover/tv")
    suspend fun getPopularAnime(
        @Query("api_key") apiKey: String,
        @Query("with_genres") withGenres: String = "16",
        @Query("with_original_language") originalLanguage: String = "ja",
        @Query("sort_by") sortBy: String = "popularity.desc",
        @Query("language") language: String = "pt-BR",
    ): TmdbListResponse

    @GET("search/tv")
    suspend fun searchAnime(
        @Query("api_key") apiKey: String,
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("language") language: String = "pt-BR",
    ): TmdbListResponse

    @GET("tv/{id}")
    suspend fun getAnimeDetails(
        @Path("id") tmdbId: Int,
        @Query("api_key") apiKey: String,
        @Query("append_to_response") appendToResponse: String = "credits,images,content_ratings,external_ids",
        @Query("language") language: String = "pt-BR",
    ): TmdbTvDetails

    @GET("tv/{id}/season/{seasonNumber}")
    suspend fun getSeasonDetails(
        @Path("id") tmdbId: Int,
        @Path("seasonNumber") seasonNumber: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "pt-BR",
    ): TmdbSeasonDetails
}

data class TmdbListResponse(
    val page: Int,
    val results: List<TmdbTvResult>,
    val total_pages: Int,
    val total_results: Int,
)

data class TmdbTvResult(
    val id: Int,
    val name: String,
    val original_name: String?,
    val overview: String?,
    val poster_path: String?,
    val backdrop_path: String?,
    val first_air_date: String?,
    val vote_average: Double,
    val vote_count: Int,
    val genre_ids: List<Int> = emptyList(),
    val original_language: String?,
)

data class TmdbTvDetails(
    val id: Int,
    val name: String,
    val original_name: String?,
    val overview: String?,
    val poster_path: String?,
    val backdrop_path: String?,
    val first_air_date: String?,
    val vote_average: Double,
    val vote_count: Int,
    val status: String?,
    val number_of_episodes: Int?,
    val genres: List<TmdbGenre> = emptyList(),
    val seasons: List<TmdbSeasonSummary> = emptyList(),
)

data class TmdbGenre(val id: Int, val name: String)

data class TmdbSeasonSummary(
    val season_number: Int,
    val name: String,
    val episode_count: Int,
    val air_date: String?,
)

data class TmdbSeasonDetails(
    val season_number: Int,
    val name: String,
    val episodes: List<TmdbEpisodeResult> = emptyList(),
)

data class TmdbEpisodeResult(
    val id: Int,
    val name: String?,
    val overview: String?,
    val episode_number: Int,
    val season_number: Int,
    val still_path: String?,
    val air_date: String?,
    val runtime: Int?,
)
