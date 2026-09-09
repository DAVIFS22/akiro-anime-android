package com.akiro.addon

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

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
                JSONObject(body)
            }
        } catch (e: Exception) {
            Log.e("AddonManager", "fetchManifest exception", e)
            null
        }
    }

    fun createAddonApi(baseUrl: String): AddonApi {
        val normalized = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return Retrofit.Builder()
            .baseUrl(normalized)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AddonApi::class.java)
    }
}
