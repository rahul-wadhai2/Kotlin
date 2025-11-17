package com.example.githubrepository.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import com.example.githubrepository.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.githubrepository.domain.model.Repository
import com.example.githubrepository.presentation.utils.Resource
import com.example.githubrepository.presentation.viewmodel.FavoritesViewModel

/**
 * FavoritesScreen displays a list of repositories that the user has marked
 * as favorites. It allows navigating to a repository’s detail screen and
 * removing items from the favorites list.
 *
 * @param viewModel The ViewModel responsible for providing and managing
 *                  the list of favorite repositories. (Injected via Hilt)
 * @param onRepositoryClick Callback invoked when a repository item is clicked.
 *                          Typically used to navigate to the DetailScreen.
 * @param onBack Callback invoked when the user presses the Back button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    viewModel: FavoritesViewModel = hiltViewModel(),
    onRepositoryClick: (Repository) -> Unit,
    onBack: () -> Unit
) {
    /**
     * State from the ViewModel.
     */
    val favoritesState by viewModel.favoritesState.collectAsState()

    /**
     * Set of favorite repository IDs.
     */
    val favoriteIds by viewModel.favoriteIds.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Favorites") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier
            .padding(padding)
            .fillMaxSize()) {

            // --- State Handling ---
            when (favoritesState) {
                is Resource.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                is Resource.Error -> {
                    Text(
                        text = favoritesState.message ?: "An unknown error occurred.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        color = Color.Red
                    )
                }

                is Resource.Success -> {
                    val repositories = favoritesState.data ?: emptyList()
                    if (repositories.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(id = R.string.not_added_to_favorites),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        LazyColumn(Modifier.fillMaxSize()) {
                            items(repositories) { repo ->
                                // REUSE THE LIST ITEM FROM SEARCH SCREEN
                                RepositoryListItem(
                                    repo = repo,
                                    // Use the IDs set from the VM to check if it's a favorite
                                    isFavorite = favoriteIds.contains(repo.id),
                                    onRepoClick = onRepositoryClick,
                                    onFavoriteToggle = { viewModel.toggleFavorite(repo) }
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}