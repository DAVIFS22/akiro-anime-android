package com.akiro.torrent

import android.content.Context
import com.akiro.anime.data.repository.TorrentManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Compatibility facade for the addon player.
 * It delegates torrent playback to the app's real libtorrent4j implementation.
 */
class TorrentEngine(context: Context) {
    private val manager = TorrentManager(context)

    suspend fun startTorrentAndGetLocalUrl(magnetUri: String): String? =
        withContext(Dispatchers.IO) {
            var result: String? = null
            var failure: Throwable? = null

            manager.streamFromMagnet(
                magnetLink = magnetUri,
                onReady = { result = it },
                onError = { failure = it }
            )

            if (result == null && failure != null) {
                throw failure as Throwable
            }

            result
        }

    suspend fun stop() {
        withContext(Dispatchers.IO) {
            manager.stop()
        }
    }
}
