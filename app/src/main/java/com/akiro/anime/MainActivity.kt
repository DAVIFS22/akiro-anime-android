package com.akiro.anime

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
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
import com.akiro.anime.ui.screens.search.SearchScreen
import com.akiro.anime.ui.theme.AkiroAnimeTheme
import java.net.URLDecoder
import java.net.URLEncoder

private sealed class BottomDestination(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Home : BottomDestination("home", "Início", Icons.Filled.Home)
    object Search : BottomDestination("search", "Buscar", Icons.Filled.Search)
    object Favorites : BottomDestination("favorites", "Favoritos", Icons.Filled.Favorite)
}

private val bottomDestinations = listOf(BottomDestination.Home, BottomDestination.Search, BottomDestination.Favorites)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AkiroAnimeTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AkiroApp()
                }
            }
        }
    }
}

@Composable
fun AkiroApp() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { AkiroBottomBar(navController) }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = BottomDestination.Home.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(BottomDestination.Home.route) {
                HomeScreen(onAnimeClick = { animeId -> navController.navigate("detail/$animeId") })
            }
            composable(BottomDestination.Search.route) {
                SearchScreen(onAnimeClick = { animeId -> navController.navigate("detail/$animeId") })
            }
            composable(BottomDestination.Favorites.route) {
                FavoritesScreen(onAnimeClick = { animeId -> navController.navigate("detail/$animeId") })
            }
            composable(
                "detail/{animeId}",
                arguments = listOf(navArgument("animeId") { type = NavType.StringType })
            ) { backStackEntry ->
                val animeId = backStackEntry.arguments?.getString("animeId") ?: ""
                AnimeDetailScreen(
                    animeId = animeId,
                    onEpisodeClick = { episode, streamUrl ->
                        val encodedTitle = URLEncoder.encode(episode.title, "UTF-8")
                        val encodedUrl = URLEncoder.encode(streamUrl ?: "", "UTF-8")
                        navController.navigate("player/$encodedTitle/$encodedUrl")
                    }
                )
            }
            composable(
                "player/{title}/{streamUrl}",
                arguments = listOf(
                    navArgument("title") { type = NavType.StringType },
                    navArgument("streamUrl") { type = NavType.StringType },
                )
            ) { backStackEntry ->
                val title = URLDecoder.decode(backStackEntry.arguments?.getString("title") ?: "", "UTF-8")
                val streamUrlRaw = URLDecoder.decode(backStackEntry.arguments?.getString("streamUrl") ?: "", "UTF-8")
                PlayerScreen(streamUrl = streamUrlRaw.ifBlank { null }, title = title)
            }
        }
    }
}

@Composable
private fun AkiroBottomBar(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination

    // Only show the bottom bar on the three main tabs, not on detail/player screens.
    val isTopLevel = bottomDestinations.any { it.route == currentRoute?.route }
    if (!isTopLevel) return

    NavigationBar {
        bottomDestinations.forEach { dest ->
            val selected = currentRoute?.hierarchy?.any { it.route == dest.route } == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(dest.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(dest.icon, contentDescription = dest.label) },
                label = { Text(dest.label) }
            )
        }
    }
}
