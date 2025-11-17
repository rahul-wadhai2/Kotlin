package com.example.githubrepository

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.githubrepository.domain.model.Repository
import com.example.githubrepository.presentation.ui.DetailScreen
import com.example.githubrepository.presentation.ui.FavoritesScreen
import com.example.githubrepository.presentation.ui.Screen
import com.example.githubrepository.presentation.ui.SearchScreen
import com.example.githubrepository.presentation.ui.SplashScreen
import com.example.githubrepository.presentation.ui.theme.GithubrepositoryTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main activity for the GitHub Repository app.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()
        setContent {
            GithubrepositoryTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavHost()
                }
            }
        }
    }
}

@Composable
fun AppNavHost() {
    /**
     * NavController manages app navigation within the app.
     */
    val navController = rememberNavController()

    /**
     * Holds the selected repository object in memory.
     */
    val repoObject = remember { mutableStateOf<Repository?>(null) }

    NavHost(navController = navController, startDestination = Screen.Splash.route) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onTimeout = {
                    navController.navigate(Screen.Search.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Search.route) {
            SearchScreen(
                onRepositoryClick = { repo ->
                    repoObject.value = repo
                    navController.navigate(Screen.Detail.createRoute(repo.id))
                },
                onNavigateToFavorites = { navController.navigate(Screen.Favorites.route) }
            )
        }

        composable(route = Screen.Detail.route) {
            repoObject.value?.let { repo ->
                DetailScreen(
                    repo = repo,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable(Screen.Favorites.route) {
            FavoritesScreen(
                onRepositoryClick = { repo ->
                    repoObject.value = repo
                    navController.navigate(Screen.Detail.createRoute(repo.id))
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}