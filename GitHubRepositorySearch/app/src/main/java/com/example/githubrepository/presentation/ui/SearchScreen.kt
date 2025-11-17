package com.example.githubrepository.presentation.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.githubrepository.R
import com.example.githubrepository.domain.model.Repository
import com.example.githubrepository.presentation.utils.Resource
import com.example.githubrepository.presentation.viewmodel.SearchViewModel

/**
 * Displays the main search screen where users can search GitHub repositories.
 *
 * This screen includes:
 * - A search bar for entering queries.
 * - A list of repositories returned from the GitHub API.
 * - A button/icon to navigate to the Favorites screen.
 *
 * State is provided by [SearchViewModel], which is injected using Hilt.
 *
 * @param viewModel The ViewModel managing search logic, UI state, and favorites.
 * @param onRepositoryClick Callback invoked when a repository item is selected.
 * @param onNavigateToFavorites Callback triggered when navigating to the Favorites screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel = hiltViewModel(),
    onRepositoryClick: (Repository) -> Unit,
    onNavigateToFavorites: () -> Unit
) {
    /**
     * Current UI state emitted by the ViewModel, including loading status,
     * search results, and possible error messages.
     */
    val searchState by viewModel.searchState.collectAsState()

    /**
     * The text entered by the user in the search bar. This value is observed
     * from the ViewModel and updates reactively.
     */
    val searchQuery by viewModel.searchQuery.collectAsState()

    /**
     * A map of favorite repositories stored in the app. This updates automatically
     * when the user adds or removes favorites.
     */
    val favorites by viewModel.favorites.collectAsState()

    /**
     * Indicates whether a search has been initiated by the user.
     * This helps control when to show suggestion text, empty states, etc.
     */
    val searchTriggered by viewModel.searchTriggered.collectAsState()

    /**
     * Manages focus for input fields, allowing the UI to programmatically
     * clear or move focus when needed (e.g., hiding the keyboard).
     */
    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.repository_search)) },
                actions = {
                    IconButton(onClick = onNavigateToFavorites) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = "Favorites",
                            tint = Color.Red
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier
            .padding(padding)
            .fillMaxSize()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::updateQuery,
                label = { Text("Search Keyword") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    viewModel.searchRepositories()
                    focusManager.clearFocus()
                }),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    } else {
                        IconButton(onClick = {
                            viewModel.searchRepositories()
                            focusManager.clearFocus()
                        }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    }
                },
                singleLine = true
            )

            // --- State Handling ---
            when (searchState) {
                is Resource.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                is Resource.Error -> {
                    Text(
                        text = searchState.message ?:
                        stringResource(id = R.string.unknown_error_occurred),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        color = Color.Red
                    )
                }

                is Resource.Success -> {
                    val repositories = searchState.data ?: emptyList()
                    if (repositories.isEmpty()) {
                        if (searchTriggered && searchQuery.isNotEmpty()) {
                            Text(
                                stringResource(id = R.string.no_results_found)+" \"$searchQuery\".",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            )
                        } else {
                            Text(
                                stringResource(id = R.string.search_gitHub_repositories),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            )
                        }
                    } else {
                        LazyColumn(Modifier.fillMaxSize()) {
                            items(repositories) { repo ->
                                RepositoryListItem(
                                    repo = repo,
                                    isFavorite = favorites.contains(repo.id),
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

@Composable
fun RepositoryListItem(
    repo: Repository,
    isFavorite: Boolean,
    onRepoClick: (Repository) -> Unit,
    onFavoriteToggle: (Repository) -> Unit
) {
    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onRepoClick(repo) }
            .padding(12.dp)
    ) {
        val (avatar, title, owner, starIcon, starText, favIcon) = createRefs()

        // Avatar
        AsyncImage(
            model = repo.ownerAvatarUrl,
            contentDescription = "${repo.ownerName} Avatar",
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .constrainAs(avatar) {
                    start.linkTo(parent.start)
                    top.linkTo(parent.top)
                }
        )

        // Title
        Text(
            text = repo.name,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            modifier = Modifier.constrainAs(title) {
                start.linkTo(avatar.end, margin = 12.dp)
                top.linkTo(parent.top)
                end.linkTo(favIcon.start, margin = 12.dp)
                width = Dimension.fillToConstraints
            }
        )

        // Owner
        Text(
            text = stringResource(id = R.string.owner)+": ${repo.ownerName}",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            maxLines = 1,
            modifier = Modifier.constrainAs(owner) {
                start.linkTo(title.start)
                top.linkTo(title.bottom, margin = 4.dp)
                end.linkTo(favIcon.start, margin = 12.dp)
                width = Dimension.fillToConstraints
            }
        )

        // Star Icon
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = "Stars",
            tint = Color(0xFFFFC107),
            modifier = Modifier
                .size(18.dp)
                .constrainAs(starIcon) {
                    start.linkTo(title.start)
                    top.linkTo(owner.bottom, margin = 6.dp)
                }
        )

        // Star Count
        Text(
            text = repo.starCount.toString(),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.constrainAs(starText) {
                start.linkTo(starIcon.end, margin = 6.dp)
                top.linkTo(starIcon.top)
            }
        )

        // Favorite Icon
        IconButton(
            onClick = { onFavoriteToggle(repo) },
            modifier = Modifier.constrainAs(favIcon) {
                end.linkTo(parent.end)
                top.linkTo(parent.top)
            }
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = "Toggle Favorite",
                tint = if (isFavorite) Color.Red else Color.Gray
            )
        }
    }
}
