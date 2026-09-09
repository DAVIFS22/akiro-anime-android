package com.akiro.anime.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    var autoPlay by remember { mutableStateOf(true) }
    var p2p by remember { mutableStateOf(true) }
    var progress by remember { mutableStateOf(true) }
    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Configurações") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Voltar") } })
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            SettingsSection("Player")
            SettingRow(Icons.Filled.PlayCircle, "Player padrão", "ExoPlayer (Interno)")
            SettingRow(Icons.Filled.HighQuality, "Qualidade de vídeo", "Auto (Recomendado)")
            SwitchRow(Icons.Filled.PlayArrow, "Reprodução automática", "Próximo episódio", autoPlay) { autoPlay = it }
            SettingRow(Icons.Filled.Subtitles, "Legendas", "Português")
            SettingsSection("Downloads e P2P")
            SwitchRow(Icons.Filled.Download, "Usar P2P (Torrentio)", "Torrent integrado", p2p) { p2p = it }
            SwitchRow(Icons.Filled.DataUsage, "Mostrar progresso do download", "Exibir status no player", progress) { progress = it }
            SettingRow(Icons.Filled.Folder, "Pasta de downloads", "/storage/emulated/0/Akiro")
            SettingsSection("Aparência")
            SettingRow(Icons.Filled.DarkMode, "Tema", "Escuro")
            SettingRow(Icons.Filled.Language, "Idioma", "Português")
            SwitchRow(Icons.Filled.Notifications, "Notificações", "Novos episódios", true) { }
        }
    }
}

@Composable private fun SettingsSection(title: String) { Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 14.dp, bottom = 6.dp)) }
@Composable private fun SettingRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, value: String) { Card(Modifier.fillMaxWidth().padding(vertical = 3.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(13.dp)) { Row(Modifier.padding(13.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) { Icon(icon, null); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(title); Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall) }; Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) } } }
@Composable private fun SwitchRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, value: String, checked: Boolean, onChecked: (Boolean) -> Unit) { Card(Modifier.fillMaxWidth().padding(vertical = 3.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(13.dp)) { Row(Modifier.padding(13.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) { Icon(icon, null); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(title); Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall) }; Switch(checked, onChecked) } } }
