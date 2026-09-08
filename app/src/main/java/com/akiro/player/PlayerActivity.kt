package com.akiro.player

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.PlayerView

/**
 * Simple player activity: plays an http/https/HLS/DASH url with ExoPlayer.
 * If a magnet is provided, this activity currently shows a TODO and logs the magnet. Integration with a torrent engine is required to stream magnets.
 */
class PlayerActivity : AppCompatActivity() {
    private var player: ExoPlayer? = null
    private var playerView: PlayerView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        playerView = PlayerView(this)
        setContentView(playerView)

        val streamUrl = intent.getStringExtra("stream_url")
        val magnet = intent.getStringExtra("magnet")

        if (!streamUrl.isNullOrEmpty()) {
            initializePlayer(streamUrl)
        } else if (!magnet.isNullOrEmpty()) {
            // TODO: integrate a torrent engine (jlibtorrent) + local HTTP server to stream into ExoPlayer.
            Log.i("PlayerActivity", "Received magnet: $magnet")
            // For now show a simple message
            finish()
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
    }
}
