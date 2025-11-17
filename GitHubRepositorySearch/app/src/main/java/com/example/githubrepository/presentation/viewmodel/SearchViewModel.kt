package com.example.githubrepository.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.githubrepository.data.di.NetworkMonitor
import com.example.githubrepository.domain.model.Repository
import com.example.githubrepository.domain.repository.GitHubRepository
import com.example.githubrepository.presentation.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel responsible for handling repository search operations and
 * managing UI-related state for the Search screen.
 *
 * @param repository Data source for fetching GitHub repositories and
 *                   managing favorite-related operations.
 * @param networkMonitor Used to observe internet connectivity and
 *                       update UI behavior based on network status.
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: GitHubRepository,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    /**
     * Holds the current state of the repository search operation.
     * Exposes Loading, Success, or Error along with the result list.
     */
    private val _searchState =
        MutableStateFlow<Resource<List<Repository>>>(Resource.Success(emptyList()))
    val searchState: StateFlow<Resource<List<Repository>>> = _searchState

    /**
     * Stores the set of favorite repository IDs.
     * Used to update the UI instantly when toggling favorites.
     */
    private val _favorites = MutableStateFlow<Set<Long>>(emptySet())
    val favorites: StateFlow<Set<Long>> = _favorites

    /**
     * Tracks the current search query typed by the user.
     */
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    /**
     * Indicates whether a search has been performed at least once.
     * Used for showing results or "no search yet" message.
     */
    private val _searchTriggered = MutableStateFlow(false)
    val searchTriggered = _searchTriggered.asStateFlow()

    init {
        loadFavorites()
        searchRepositories(_searchQuery.value)
    }

    /**
     * Updates the current search query entered by the user.
     *
     * This method modifies the `_searchQuery` state, which is observed by the UI.
     * It does **not** trigger a search by itself — the actual search is performed
     * when `searchRepositories()` is called.
     *
     * @param query The latest text input from the user.
     */
    fun updateQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Performs a repository search based on the provided query.
     *
     * If no query is explicitly passed, it uses the current value of `_searchQuery`.
     * This method triggers the search operation, updates the loading state,
     * and stores the resulting repositories or error message in `searchState`.
     *
     * @param query The search keyword entered by the user. Defaults to the
     *              current search query maintained in the ViewModel.
     */
    fun searchRepositories(query: String = _searchQuery.value) {
        if (query.isBlank()) {
            _searchState.value = Resource.Success(emptyList())
            return
        }

        _searchTriggered.value = true

        _searchState.value = Resource.Loading()
        viewModelScope.launch {
            // Check for network connectivity before attempting the API call
            val isOnline = networkMonitor.isOnline().first()

            if (!isOnline) {
                _searchState.value = Resource.Error("No internet connection. Please try again.")
                return@launch
            }

            try {
                val results = repository.searchRepositories(query)
                _searchState.value = Resource.Success(results)
            } catch (e: Exception) {
                // In a real app, check for specific network errors
                _searchState.value = Resource.Error("Network Error: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Toggles the favorite status.
     * The function now accepts the full Repository object,
     * which is necessary for the underlying data layer to serialize the details to JSON.
     *
     * @param repo The full Repository object to save or remove.
     */
    fun toggleFavorite(repo: Repository) {
        // Perform local persistence update and UI state update
        val isFavorite = repository.toggleFavorite(repo)

        // Update the StateFlow for UI consumption using the repo.id
        _favorites.value = if (isFavorite) {
            _favorites.value + repo.id
        } else {
            _favorites.value - repo.id
        }
    }

    /**
     * Loads the set of favorite repository IDs from the repository.
     */
    fun loadFavorites() {
        viewModelScope.launch {
            repository.observeFavorites().collect { map ->
                _favorites.value = map.keys
            }
        }
    }
}