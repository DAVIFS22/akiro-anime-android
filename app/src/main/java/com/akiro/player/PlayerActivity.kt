package com.akiro.player

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.PlayerView
import com.akiro.torrent.TorrentEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * PlayerActivity: extended to support magnet links using TorrentEngine (jlibtorrent) + LocalHttpServer.
 * If the app does not include jlibtorrent / NanoHTTPD on classpath, the engine will fail gracefully and player won't start for magnet links.
 */
class PlayerActivity : AppCompatActivity() {
    private var player: ExoPlayer? = null
    private var playerView: PlayerView? = null
    private val scope = CoroutineScope(Job() + Dispatchers.Main)

    private var torrentEngine: TorrentEngine? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        playerView = PlayerView(this)
        playerView?.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        setContentView(playerView)

        val streamUrl = intent.getStringExtra("stream_url")
        val magnet = intent.getStringExtra("magnet")

        if (!streamUrl.isNullOrEmpty()) {
            initializePlayer(streamUrl)
        } else if (!magnet.isNullOrEmpty()) {
            // start torrent engine and wait for local url
            scope.launch {
                try {
                    torrentEngine = TorrentEngine(applicationContext)
                    val localUrl = torrentEngine?.startTorrentAndGetLocalUrl(magnet)
                    if (!localUrl.isNullOrEmpty()) {
                        initializePlayer(localUrl)
                    } else {
                        Log.e("PlayerActivity", "failed to get local url for magnet")
                        finish()
                    }
                } catch (e: Exception) {
                    Log.e("PlayerActivity", "error while streaming magnet", e)
                    finish()
                }
            }
        } else {
            finish()
        }
    }

    private fun initializePlayer(url: String) {
        player = ExoPlayer.Builder(this).build()
        playerView?.player = player
        val mediaItem = MediaItem.fromUri(Uri.parse(url))
        player?.setMediaItem(mediaItem)
        player?.prepare()
        player?.play()
    }

    override fun onStop() {
        super.onStop()
        player?.release()
        player = null
        scope.launch(Dispatchers.IO) {
            try {
                torrentEngine?.stop()
            } catch (e: Exception) {
                Log.e("PlayerActivity", "error stopping torrent engine", e)
            }
        }
    }
}
