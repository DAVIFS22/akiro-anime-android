package com.akiro.anime.ui.screens.player

import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.akiro.anime.data.repository.TorrentManager
import kotlinx.coroutines.launch

/**
 * Player that accepts either:
 *  - a normal HTTP(S) stream, or
 *  - a magnet URI returned by a Torrentio-compatible addon.
 *
 * Magnet streams are resolved inside the device and exposed to Media3 through
 * a localhost HTTP server with Range support.
 */
@Composable
fun PlayerScreen(streamUrl: String?, title: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var playableUrl by remember(streamUrl) {
        mutableStateOf(if (streamUrl?.startsWith("magnet:") == true) null else streamUrl)
    }
    var error by remember(streamUrl) { mutableStateOf<String?>(null) }
    var loading by remember(streamUrl) {
        mutableStateOf(streamUrl?.startsWith("magnet:") == true)
    }

    val torrentManager = remember { TorrentManager(context) }

    LaunchedEffect(streamUrl) {
        if (streamUrl.isNullOrBlank()) {
            loading = false
            return@LaunchedEffect
        }

        if (!streamUrl.startsWith("magnet:")) {
            loading = false
            return@LaunchedEffect
        }

        loading = true
        error = null
        scope.launch {
            torrentManager.streamFromMagnet(
                magnetLink = streamUrl,
                onReady = { url ->
                    playableUrl = url
                    loading = false
                },
                onError = { throwable ->
                    error = throwable.message ?: "Falha ao iniciar o torrent."
                    loading = false
                }
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose { torrentManager.release() }
    }

    if (playableUrl.isNullOrBlank()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            when {
                error != null -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            "Não foi possível iniciar \"$title\"",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(error!!, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                loading -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(12.dp))
                        Text("Conectando ao torrent…")
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Obtendo metadados e preparando o vídeo",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                else -> Text("Nenhuma fonte de vídeo disponível.")
            }
        }
        return
    }

    val exoPlayer = remember(playableUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(playableUrl!!))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer.release() }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = {
            PlayerView(it).apply {
                player = exoPlayer
                setBackgroundColor(android.graphics.Color.BLACK)
                keepScreenOn = true
            }
        }
    )
}
