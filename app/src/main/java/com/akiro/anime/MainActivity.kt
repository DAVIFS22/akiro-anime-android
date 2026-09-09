package com.akiro.anime

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.akiro.anime.ui.screens.detail.AnimeDetailScreen
import com.akiro.anime.ui.screens.favorites.FavoritesScreen
import com.akiro.anime.ui.screens.home.HomeScreen
import com.akiro.anime.ui.screens.player.PlayerScreen
import com.akiro.anime.ui.screens.profile.DownloadsScreen
import com.akiro.anime.ui.screens.profile.ProfileScreen
import com.akiro.anime.ui.screens.profile.SettingsScreen
import com.akiro.anime.ui.screens.search.SearchScreen
import com.akiro.anime.ui.theme.AkiroAnimeTheme

private sealed class BottomDestination(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    data object Home : BottomDestination("home", "Início", Icons.Filled.Home)
    data object Search : BottomDestination("search", "Explorar", Icons.Filled.Search)
    data object Favorites : BottomDestination("favorites", "Favoritos", Icons.Filled.Favorite)
    data object Downloads : BottomDestination("downloads", "Downloads", Icons.Filled.Download)
    data object Profile : BottomDestination("profile", "Perfil", Icons.Filled.Person)
}
private val bottomDestinations = listOf(BottomDestination.Home, BottomDestination.Search, BottomDestination.Favorites, BottomDestination.Downloads, BottomDestination.Profile)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AkiroAnimeTheme { Surface(Modifier.fillMaxSize()) { AkiroApp() } } }
    }
}

@Composable
fun AkiroApp() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val showBottom = bottomDestinations.any { it.route == backStack?.destination?.route }
    Scaffold(bottomBar = { if (showBottom) AkiroBottomBar(navController) }) { padding ->
        NavHost(navController, startDestination = "home", modifier = Modifier.padding(padding)) {
            composable("home") { HomeScreen(onAnimeClick = { id -> navController.navigate("detail/$id") }, onSearch = { navController.navigate("search") }) }
            composable("search") { SearchScreen(onAnimeClick = { id -> navController.navigate("detail/$id") }) }
            composable("favorites") { FavoritesScreen(onAnimeClick = { id -> navController.navigate("detail/$id") }) }
            composable("downloads") { DownloadsScreen() }
            composable("profile") { ProfileScreen { navController.navigate("settings") } }
            composable("settings") { SettingsScreen { navController.popBackStack() } }
            composable("detail/{animeId}", arguments = listOf(navArgument("animeId") { type = NavType.StringType })) { entry ->
                AnimeDetailScreen(
                    animeId = entry.arguments?.getString("animeId") ?: "",
                    onBack = { navController.popBackStack() },
                    onEpisodeClick = { episode, streamUrl ->
                        navController.currentBackStackEntry?.savedStateHandle?.set("playerTitle", episode.title)
                        navController.currentBackStackEntry?.savedStateHandle?.set("playerUrl", streamUrl)
                        navController.navigate("player")
                    }
                )
            }
            composable("player") { PlayerScreen(
                streamUrl = navController.previousBackStackEntry?.savedStateHandle?.get<String>("playerUrl"),
                title = navController.previousBackStackEntry?.savedStateHandle?.get<String>("playerTitle").orEmpty(),
                onBack = { navController.popBackStack() }
            ) }
        }
    }
}

@Composable
private fun AkiroBottomBar(navController: NavHostController) {
    val current by navController.currentBackStackEntryAsState()
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .98f)) {
        bottomDestinations.forEach { dest ->
            val selected = current?.destination?.route == dest.route
            NavigationBarItem(selected = selected, onClick = {
                navController.navigate(dest.route) { launchSingleTop = true; restoreState = true }
            }, icon = { Icon(dest.icon, dest.label) }, label = { Text(dest.label) })
        }
    }
}
