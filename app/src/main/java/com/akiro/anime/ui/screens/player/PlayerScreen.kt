@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.akiro.anime.ui.screens.player

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.akiro.anime.data.repository.TorrentManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private enum class PlayerEngine(val label: String, val description: String) {
    EXO("ExoPlayer (Interno)", "Reprodutor integrado do Akiro"),
    VLC("VLC", "Abrir no VLC instalado"),
    MPV("MPV", "Abrir no MPV Android"),
    EXTERNAL("Reprodutor Externo", "Escolher outro player")
}

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(streamUrl: String?, title: String, onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val torrentManager = remember { TorrentManager(context) }
    val isMagnet = streamUrl?.startsWith("magnet:") == true
    var playableUrl by remember(streamUrl) { mutableStateOf(if (isMagnet) null else streamUrl) }
    var error by remember(streamUrl) { mutableStateOf<String?>(null) }
    var loading by remember(streamUrl) { mutableStateOf(isMagnet) }
    var engine by remember { mutableStateOf(PlayerEngine.EXO) }
    var playerSheet by remember { mutableStateOf(false) }

    LaunchedEffect(streamUrl) {
        if (!isMagnet) { loading = false; return@LaunchedEffect }
        torrentManager.streamFromMagnet(streamUrl!!, { url -> scope.launch(Dispatchers.Main.immediate) { playableUrl = url; loading = false } }, { e -> scope.launch(Dispatchers.Main.immediate) { error = e.message ?: "Falha ao iniciar o P2P."; loading = false } })
    }
    DisposableEffect(Unit) { onDispose { torrentManager.release() } }

    val url = playableUrl
    if (url.isNullOrBlank()) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Column(Modifier.fillMaxSize().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.fillMaxWidth().statusBarsPadding(), verticalAlignment = Alignment.CenterVertically) { IconButton(onBack) { Icon(Icons.Filled.ArrowBack, "Voltar") }; Text(title.ifBlank { "Reprodutor" }, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), maxLines = 1) }
                Spacer(Modifier.weight(1f))
                if (loading) {
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(); Spacer(Modifier.height(16.dp)); Text("Buscando fontes", style = MaterialTheme.typography.titleLarge); Spacer(Modifier.height(7.dp)); Text("Consultando o Torrentio e preparando o P2P…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    Text("Não foi possível iniciar o vídeo", style = MaterialTheme.typography.titleLarge); Spacer(Modifier.height(8.dp)); Text(error ?: "Nenhuma fonte disponível.", color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.height(16.dp)); OutlinedButton(onBack) { Text("Voltar") }
                }
                Spacer(Modifier.weight(1f))
            }
        }
        return
    }

    if (engine != PlayerEngine.EXO) {
        LaunchedEffect(engine, url) {
            when (engine) {
                PlayerEngine.VLC -> openExternalPlayer(context, url, "org.videolan.vlc")
                PlayerEngine.MPV -> openMpV(context, url)
                PlayerEngine.EXTERNAL -> openChooser(context, url)
                PlayerEngine.EXO -> Unit
            }
            engine = PlayerEngine.EXO
        }
    }

    val exoPlayer = remember(url) { ExoPlayer.Builder(context).build().apply { setMediaItem(MediaItem.fromUri(url)); prepare(); playWhenReady = true } }
    DisposableEffect(exoPlayer) { onDispose { exoPlayer.release() } }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(factory = { c -> PlayerView(c).apply { player = exoPlayer; keepScreenOn = true; useController = true; controllerAutoShow = true; setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING); resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT } }, modifier = Modifier.fillMaxSize())
        Box(Modifier.fillMaxWidth().align(Alignment.TopCenter).background(Brush.verticalGradient(listOf(Color.Black.copy(.72f), Color.Transparent))) .statusBarsPadding().padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = Color.Black.copy(.45f), shape = RoundedCornerShape(14.dp)) { IconButton(onBack) { Icon(Icons.Filled.ArrowBack, "Voltar", tint = Color.White) } }
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) { Text(title.ifBlank { "Akiro Player" }, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1); Text("ExoPlayer • ${if (isMagnet) "Torrentio P2P" else "Fonte direta"}", color = Color.White.copy(.68f), style = MaterialTheme.typography.labelSmall) }
                Surface(color = Color.Black.copy(.45f), shape = RoundedCornerShape(14.dp)) { IconButton({ playerSheet = true }) { Icon(Icons.Filled.MoreVert, "Opções", tint = Color.White) } }
            }
        }
        if (isMagnet) Surface(Modifier.align(Alignment.BottomCenter).padding(bottom = 82.dp), color = Color.Black.copy(.70f), shape = RoundedCornerShape(12.dp)) { Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.CloudDownload, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(7.dp)); Text("Fonte: Torrentio (P2P)", color = Color.White, style = MaterialTheme.typography.labelSmall) } }
    }

    if (playerSheet) ModalBottomSheet(onDismissRequest = { playerSheet = false }) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 8.dp)) {
            Text("Escolha o player", style = MaterialTheme.typography.headlineSmall)
            Text("Selecione como deseja assistir este episódio", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            PlayerEngine.entries.forEach { option ->
                Card(
                    onClick = { playerSheet = false; if (option != PlayerEngine.EXO) engine = option },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (option == PlayerEngine.EXTERNAL) Icons.Filled.OpenInNew else Icons.Filled.PlayCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(13.dp)); Column(Modifier.weight(1f)) { Text(option.label, fontWeight = FontWeight.SemiBold); Text(option.description, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall) }
                        if (option == PlayerEngine.EXO) Text("Recomendado", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
        }
    }
}

private fun openExternalPlayer(context: Context, url: String, packageName: String) { val i = Intent(Intent.ACTION_VIEW).apply { setDataAndType(Uri.parse(url), "video/*"); setPackage(packageName); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }; try { context.startActivity(i) } catch (_: ActivityNotFoundException) { openChooser(context, url) } }
private fun openMpV(context: Context, url: String) { for (pkg in listOf("is.xyz.mpv", "org.mpv.android")) { val i = Intent(Intent.ACTION_VIEW).apply { setDataAndType(Uri.parse(url), "video/*"); setPackage(pkg); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }; try { context.startActivity(i); return } catch (_: ActivityNotFoundException) {} }; openChooser(context, url) }
private fun openChooser(context: Context, url: String) { val i = Intent(Intent.ACTION_VIEW).apply { setDataAndType(Uri.parse(url), "video/*"); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }; try { context.startActivity(Intent.createChooser(i, "Abrir vídeo com…").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: ActivityNotFoundException) {} }
