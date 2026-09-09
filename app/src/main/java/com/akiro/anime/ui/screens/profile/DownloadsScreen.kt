package com.akiro.anime.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DownloadsScreen() {
    Column(Modifier.fillMaxSize().padding(18.dp)) {
        Spacer(Modifier.height(12.dp))
        Text("Downloads", style = MaterialTheme.typography.headlineMedium)
        Text("Conteúdo salvo e transferências P2P", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.Download, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(46.dp))
                Spacer(Modifier.height(12.dp))
                Text("Nenhum download ainda", style = MaterialTheme.typography.titleMedium)
                Text("Quando um torrent estiver baixando, o progresso aparecerá aqui.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
