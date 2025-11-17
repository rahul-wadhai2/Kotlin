package com.example.githubrepository.domain.repository

import com.example.githubrepository.domain.model.Repository
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository abstraction for managing GitHub data and local favorite storage.
 *
 * This interface defines how the data layer interacts with:
 * - Remote GitHub API (for searching repositories)
 * - Local persistent storage (for saving / removing favorites)
 * - Reactive streams (Flow) for UI updates
 */
interface GitHubRepository {

    /**
     * Searches repositories on GitHub using the given query string.
     *
     * @param query The search text entered by the user.
     * @return A list of repositories matching the query.
     */
    suspend fun searchRepositories(query: String): List<Repository>

    /**
     * Returns a set of IDs representing all repositories
     * currently marked as favorites in local storage.
     *
     * @return A set of repository IDs.
     */
    fun getFavoriteIds(): Set<Long>

    /**
     * Loads the complete list of repositories saved as favorites.
     * Useful for displaying the Favorites screen.
     *
     * @return A list of full Repository objects.
     */
    suspend fun getFavoriteRepositories(): List<Repository>

    /**
     * Toggles the favorite state of a repository.
     *
     * If the repository is already favorite → it removes it.
     * If not favorite → it adds it to favorites.
     *
     * @param repo The repository to toggle.
     * @return `true` if added to favorites, `false` if removed.
     */
    fun toggleFavorite(repo: Repository): Boolean

    /**
     * Observes continuous updates of the favorites map.
     * This allows the UI to automatically refresh when
     * favorites change from anywhere in the app.
     *
     * @return A StateFlow containing a map of favorite repositories keyed by ID.
     */
    fun observeFavorites(): StateFlow<MutableMap<Long, Repository>>
}