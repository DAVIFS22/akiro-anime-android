package com.akiro.anime.network

import com.akiro.anime.data.model.Genre
import com.google.gson.TypeAdapter
import com.google.gson.annotations.JsonAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.io.IOException
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface AkiroApi {

    @GET("v1/home")
    suspend fun home(): HomeDto

    @GET("v1/animes/trending")
    suspend fun trending(
        @Query("limit") limit: Int = 30
    ): AnimeListDto

    @GET("v1/animes/popular")
    suspend fun popular(
        @Query("limit") limit: Int = 30
    ): AnimeListDto

    @GET("v1/animes/recent")
    suspend fun recent(
        @Query("limit") limit: Int = 30
    ): AnimeListDto

    @GET("v1/animes/search")
    suspend fun search(
        @Query("q") query: String
    ): AnimeListDto

    @GET("v1/animes/{id}")
    suspend fun getAnime(
        @Path("id") id: Int
    ): AnimeDto

    @GET("v1/animes/{animeId}/seasons/{season}/episodes")
    suspend fun getEpisodes(
        @Path("animeId") animeId: Int,
        @Path("season") season: Int
    ): EpisodeListDto

    @GET("v1/episodes/{episodeId}/sources")
    suspend fun getSources(
        @Path("episodeId") episodeId: String
    ): SourceListDto

    @POST("v1/auth/login")
    suspend fun login(
        @Body body: LoginRequest
    ): AuthResponseDto

    @POST("v1/auth/register")
    suspend fun register(
        @Body body: RegisterRequest
    ): AuthResponseDto

    @GET("v1/me/favorites")
    suspend fun favorites(): AnimeListDto

    @POST("v1/me/favorites/{animeId}")
    suspend fun addFavorite(
        @Path("animeId") animeId: String
    )

    @DELETE("v1/me/favorites/{animeId}")
    suspend fun removeFavorite(
        @Path("animeId") animeId: String
    )

    @GET("v1/me/history/continue-watching")
    suspend fun continueWatching(): HistoryListDto

    @POST("v1/me/history")
    suspend fun saveHistory(
        @Body body: HistoryRequest
    )
}

data class AnimeListDto(
    val items: List<AnimeDto> = emptyList()
)

data class HomeDto(
    val featured: List<AnimeDto> = emptyList(),
    val trending: List<AnimeDto> = emptyList(),
    val popular: List<AnimeDto> = emptyList(),
    val recent: List<AnimeDto> = emptyList(),
    val simulcast: List<AnimeDto> = emptyList()
)

data class EpisodeListDto(
    val items: List<EpisodeDto> = emptyList()
)

data class SourceListDto(
    val items: List<SourceDto> = emptyList()
)

data class AnimeDto(
    val id: String,

    val tmdb_id: Int? = null,
    val mal_id: Int? = null,
    val imdb_id: String? = null,
    val kitsu_id: String? = null,

    val title: String,
    val romaji_title: String? = null,
    val native_title: String? = null,

    val synopsis: String = "",

    val poster: String = "",
    val banner: String = "",
    val backdrop: String? = null,

    // A API retorna gêneros como objetos:
    // [{"id": 1, "name": "Action"}]
    val genres: List<Genre> = emptyList(),

    val year: Int = 0,
    val status: String = "UNKNOWN",
    val type: String = "TV",

    val rating: Double = 0.0,
    val rating_score_count: Int? = null,

    val age_rating: String = "14+",

    // A API pode retornar:
    //
    // ["MAPPA", "Bones"]
    //
    // ou:
    //
    // [
    //   {"id": 1, "name": "MAPPA"},
    //   {"id": 2, "name": "Bones"}
    // ]
    //
    // O adapter aceita os dois formatos.
    @JsonAdapter(StudioListAdapter::class)
    val studios: List<String> = emptyList(),

    val total_episodes: Int = 0,

    val seasons: List<SeasonDto> = emptyList()
)

data class SeasonDto(
    val season_number: Int,
    val name: String,
    val episode_count: Int = 0,
    val year: Int? = null
)

data class EpisodeDto(
    val id: String,
    val number: Int,
    val title: String,
    val thumbnail: String = "",
    val duration: Int = 0,
    val description: String? = null,
    val air_date: String? = null,
    val season_id: String? = null
)

data class SourceDto(
    val id: String,
    val provider: String,
    val name: String,
    val url: String,
    val type: String = "hls",
    val quality: String = "Auto",
    val language: String? = null,
    val subtitles: List<SubtitleDto> = emptyList(),
    val addonId: String? = null,
    val playableInBrowser: Boolean? = null
)

data class SubtitleDto(
    val id: String? = null,
    val lang: String = "",
    val label: String = "",
    val url: String? = null,
    val format: String? = null
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val email: String,
    val password: String,
    val displayName: String? = null
)

data class UserDto(
    val id: String,
    val email: String,
    val display_name: String? = null
)

data class AuthResponseDto(
    val user: UserDto,
    val token: String
)

data class HistoryRequest(
    val episodeId: String,
    val positionSeconds: Int,
    val durationSeconds: Int,
    val completed: Boolean
)

data class HistoryItemDto(
    val episode_id: String,
    val position_seconds: Int,
    val duration_seconds: Int,
    val completed: Boolean,
    val watched_at: String
)

data class HistoryListDto(
    val items: List<HistoryItemDto> = emptyList()
)

/**
 * Converte "studios" da API para List<String>.
 *
 * Aceita:
 *
 * 1. Array de strings:
 *    ["MAPPA", "Bones"]
 *
 * 2. Array de objetos:
 *    [
 *      {"id": 1, "name": "MAPPA"},
 *      {"id": 2, "name": "Bones"}
 *    ]
 *
 * 3. Valores nulos ou formatos inesperados:
 *    são ignorados com segurança.
 */
class StudioListAdapter : TypeAdapter<List<String>>() {

    override fun read(reader: JsonReader): List<String> {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull()
            return emptyList()
        }

        if (reader.peek() != JsonToken.BEGIN_ARRAY) {
            reader.skipValue()
            return emptyList()
        }

        val result = mutableListOf<String>()

        reader.beginArray()

        while (reader.hasNext()) {

            when (reader.peek()) {

                JsonToken.STRING -> {
                    val value = reader.nextString().trim()

                    if (value.isNotEmpty()) {
                        result.add(value)
                    }
                }

                JsonToken.BEGIN_OBJECT -> {
                    var studioName: String? = null

                    reader.beginObject()

                    while (reader.hasNext()) {

                        when (reader.nextName()) {

                            "name",
                            "title" -> {
                                if (reader.peek() == JsonToken.STRING) {
                                    val value = reader.nextString().trim()

                                    if (value.isNotEmpty()) {
                                        studioName = value
                                    }
                                } else {
                                    reader.skipValue()
                                }
                            }

                            else -> {
                                reader.skipValue()
                            }
                        }
                    }

                    reader.endObject()

                    if (!studioName.isNullOrBlank()) {
                        result.add(studioName)
                    }
                }

                JsonToken.NULL -> {
                    reader.nextNull()
                }

                else -> {
                    reader.skipValue()
                }
            }
        }

        reader.endArray()

        return result
    }

    override fun write(
        writer: JsonWriter,
        value: List<String>?
    ) {
        if (value == null) {
            writer.nullValue()
            return
        }

        writer.beginArray()

        value.forEach { studio ->
            writer.value(studio)
        }

        writer.endArray()
    }
}
