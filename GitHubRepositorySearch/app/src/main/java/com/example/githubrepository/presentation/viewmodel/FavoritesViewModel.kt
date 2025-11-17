package com.example.githubrepository.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.githubrepository.domain.model.Repository
import com.example.githubrepository.domain.repository.GitHubRepository
import com.example.githubrepository.presentation.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel responsible for managing and exposing the list of favorite repositories.
 *
 * This ViewModel interacts with the `GitHubRepository` to load, observe,
 * and update the user's favorite repositories. It exposes a reactive data stream
 * that the UI can collect to automatically update when favorites change.
 *
 * @param repository The data source used to access and modify favorite repositories.
 */
@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val repository: GitHubRepository
) : ViewModel() {

    /**
     * Holds the current state of the favorite repositories list.
     *
     * The UI collects this flow to show progress indicators, results,
     * or error messages accordingly.
     */
    private val _favoritesState =
        MutableStateFlow<Resource<List<Repository>>>(Resource.Loading())
    val favoritesState: StateFlow<Resource<List<Repository>>> = _favoritesState

    /**
     * Stores the current set of favorite repository IDs.
     *
     * This flow is primarily used by the UI to determine which
     * repositories should display a filled favorite icon. It updates
     * in real time whenever a favorite is added or removed.
     */
    private val _favoriteIds = MutableStateFlow<Set<Long>>(emptySet())
    val favoriteIds: StateFlow<Set<Long>> = _favoriteIds

    init {
        loadFavoriteIds()
        loadFavoriteRepositories()
    }

    /**
     * Load the favorites repo Ids.
     */
    private fun loadFavoriteIds() {
        viewModelScope.launch {
            repository.observeFavorites().collect { map ->

                // IDs for UI toggle state
                _favoriteIds.value = map.keys

                // Full favorite objects for list screen
                _favoritesState.value = Resource.Success(map.values.toList())
            }
        }
    }

    /**
     * Load the favorite repositories from the data source.
     */
    fun loadFavoriteRepositories() {
        _favoritesState.value = Resource.Loading()
        viewModelScope.launch {
            try {
                // This now reads the full repository objects from JSON storage
                val favRepos = repository.getFavoriteRepositories()
                _favoritesState.value = Resource.Success(favRepos)
            } catch (e: Exception) {
                _favoritesState.value = Resource.Error("Failed to load favorites: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Accepts the full Repository object to pass to the data layer.
     * It then updates the UI by optimistically removing the item if unfavorited.
     *
     * @param repo The full Repository object to remove or add.
     */
    fun toggleFavorite(repo: Repository) {
        //Call the updated repository method with the full object
        val isFavorite = repository.toggleFavorite(repo)

        //Update the ID list state
        _favoriteIds.value = if (isFavorite) {
            _favoriteIds.value + repo.id
        } else {
            _favoriteIds.value - repo.id
        }

        //Update the displayed list (Favorites screen behavior)
        if (!isFavorite) {
            // Optimistically remove the item from the displayed list for a smooth transition
            val currentList = _favoritesState.value.data?.toMutableList()
            currentList?.removeAll { it.id == repo.id }
            _favoritesState.value = Resource.Success(currentList ?: emptyList())
        }
    }
}