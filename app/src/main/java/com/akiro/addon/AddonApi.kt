package com.akiro.addon

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Minimal Retrofit API for an addon implementing a /stream endpoint.
 * The exact params depend on the addon. Torrentio typically exposes a /stream endpoint that accepts addon-specific params.
 * We'll keep this generic and parse the response into StreamDto objects.
 */
interface AddonApi {
    @GET("/stream")
    suspend fun getStreams(
        @Query("type") type: String,
        @Query("id") id: String
    ): List<StreamDto>
}
