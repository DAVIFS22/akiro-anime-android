package com.akiro.anime.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ProfileScreen(onSettings: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(18.dp)) {
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(58.dp), shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.primary) { Box(contentAlignment = Alignment.Center) { Text("A", style = MaterialTheme.typography.headlineMedium) } }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Akiro Anime", style = MaterialTheme.typography.titleLarge)
                Text("Anime • Filmes • Séries", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onSettings) { Icon(Icons.Filled.Settings, "Configurações") }
        }
        Spacer(Modifier.height(24.dp))
        Text("Configurações rápidas", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(10.dp))
        ProfileItem(Icons.Filled.PlayArrow, "Player padrão", "ExoPlayer (Interno)")
        ProfileItem(Icons.Filled.Download, "Downloads e P2P", "Torrent integrado")
        ProfileItem(Icons.Filled.Translate, "Legendas", "Português")
        ProfileItem(Icons.Filled.DarkMode, "Aparência", "Escuro")
        Spacer(Modifier.height(24.dp))
        Text("Akiro Anime v1.3.0", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable private fun ProfileItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, value: String) {
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(15.dp)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.bodyLarge); Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall) }
            Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
