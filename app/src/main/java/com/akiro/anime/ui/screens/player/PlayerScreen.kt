package com.akiro.anime.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.akiro.anime.data.repository.TorrentManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun PlayerScreen(
    streamUrl: String?,
    title: String,
    onBack: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val torrentManager = remember { TorrentManager(context) }

    var playableUrl by remember(streamUrl) {
        mutableStateOf(if (streamUrl?.startsWith("magnet:") == true) null else streamUrl)
    }
    var error by remember(streamUrl) { mutableStateOf<String?>(null) }
    var loading by remember(streamUrl) { mutableStateOf(streamUrl?.startsWith("magnet:") == true) }

    LaunchedEffect(streamUrl) {
        if (streamUrl.isNullOrBlank() || !streamUrl.startsWith("magnet:")) {
            loading = false
            return@LaunchedEffect
        }

        loading = true
        error = null
        torrentManager.streamFromMagnet(
            magnetLink = streamUrl,
            onReady = { url ->
                scope.launch(Dispatchers.Main.immediate) {
                    playableUrl = url
                    loading = false
                }
            },
            onError = { throwable ->
                scope.launch(Dispatchers.Main.immediate) {
                    error = throwable.message ?: "Falha ao iniciar o torrent."
                    loading = false
                }
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose { torrentManager.release() }
    }

    if (playableUrl.isNullOrBlank()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(title.ifBlank { "Reprodutor" }, maxLines = 1) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar")
                        }
                    }
                )
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                when {
                    error != null -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text("Não foi possível iniciar o vídeo", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            Text(error.orEmpty(), style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(16.dp))
                            OutlinedButton(onClick = onBack) { Text("Voltar") }
                        }
                    }
                    loading -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(12.dp))
                            Text("Preparando vídeo…")
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Obtendo metadados e conectando às fontes",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    else -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Nenhuma fonte de vídeo disponível.")
                            Spacer(Modifier.height(12.dp))
                            OutlinedButton(onClick = onBack) { Text("Voltar") }
                        }
                    }
                }
            }
        }
        return
    }

    val url = playableUrl!!
    val exoPlayer = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer.release() }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext ->
                PlayerView(viewContext).apply {
                    player = exoPlayer
                    keepScreenOn = true
                }
            }
        )
        Surface(
            modifier = Modifier.statusBarsPadding().padding(8.dp),
            color = Color.Black.copy(alpha = 0.55f),
            shape = MaterialTheme.shapes.large
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar", tint = Color.White)
            }
        }
    }
}
