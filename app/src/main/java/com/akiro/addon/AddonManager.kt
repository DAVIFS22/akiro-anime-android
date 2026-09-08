package com.akiro.addon

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

/**
 * Simple AddonManager: downloads a manifest.json for an addon and exposes a Retrofit API for the addon endpoints.
 * This is intentionally minimal — the manifest JSON can contain more metadata (resources, id, name).
 */
object AddonManager {
    private val client = OkHttpClient()

    suspend fun fetchManifest(manifestUrl: String): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url(manifestUrl).build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    Log.e("AddonManager", "fetchManifest failed: ${resp.code}")
                    return@withContext null
                }
                val body = resp.body?.string() ?: return@withContext null
                return@withContext JSONObject(body)
            }
        } catch (e: Exception) {
            Log.e("AddonManager", "fetchManifest exception", e)
            null
        }
    }

    /**
     * Create an AddonApi bound to the given baseUrl (must end with /)
     */
    fun createAddonApi(baseUrl: String): AddonApi {
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
        return retrofit.create(AddonApi::class.java)
    }
}
