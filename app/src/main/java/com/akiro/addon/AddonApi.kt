package com.akiro.addon

import retrofit2.http.GET
import retrofit2.http.Query

data class StreamsResponse(
    val streams: List<StreamDto> = emptyList()
)

interface AddonApi {
    @GET("stream")
    suspend fun getStreams(
        @Query("type") type: String,
        @Query("id") id: String
    ): StreamsResponse
}
