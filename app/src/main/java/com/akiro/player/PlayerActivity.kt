package com.akiro.player

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.akiro.torrent.TorrentEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlayerActivity : ComponentActivity() {
    private var player: ExoPlayer? = null
    private var playerView: PlayerView? = null
    private var torrentEngine: TorrentEngine? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        playerView = PlayerView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        setContentView(playerView)

        val streamUrl = intent.getStringExtra("stream_url")
        val magnet = intent.getStringExtra("magnet")

        when {
            !streamUrl.isNullOrBlank() -> initializePlayer(streamUrl)
            !magnet.isNullOrBlank() -> startMagnet(magnet)
            else -> finish()
        }
    }

    private fun startMagnet(magnet: String) {
        lifecycleScope.launch {
            try {
                val (engine, localUrl) = withContext(Dispatchers.IO) {
                    val engine = TorrentEngine(applicationContext)
                    val url = engine.startTorrentAndGetLocalUrl(magnet)
                    engine to url
                }
                torrentEngine = engine
                if (!localUrl.isNullOrBlank()) {
                    initializePlayer(localUrl)
                } else {
                    Log.e("PlayerActivity", "Não foi possível obter URL local do torrent")
                    engine.stop()
                    finish()
                }
            } catch (t: Throwable) {
                Log.e("PlayerActivity", "Falha ao iniciar torrent", t)
                finish()
            }
        }
    }

    private fun initializePlayer(url: String) {
        player = ExoPlayer.Builder(this).build().also { exo ->
            playerView?.player = exo
            exo.setMediaItem(MediaItem.fromUri(Uri.parse(url)))
            exo.prepare()
            exo.playWhenReady = true
        }
    }

    override fun onStop() {
        playerView?.player = null
        player?.release()
        player = null
        super.onStop()
    }

    override fun onDestroy() {
        lifecycleScope.launch(Dispatchers.IO) {
            runCatching { torrentEngine?.stop() }
            torrentEngine = null
        }
        super.onDestroy()
    }
}
