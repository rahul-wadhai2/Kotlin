package com.example.githubrepository.data.local

import android.content.Context
import androidx.core.content.edit
import com.example.githubrepository.domain.model.Repository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The FavoritesLocalDataSource class is responsible for direct read and write operations for
 * repository favorite data, primarily interacting with the SharedPreferences key-value store
 */
@Singleton
class FavoritesLocalDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * SharedPreferences instance for storing favorite repositories.
     */
    private val prefs = context.getSharedPreferences("favorites_prefs", Context.MODE_PRIVATE)

    /**
     * Key for the SharedPreferences map of favorite repositories.
     */
    private val FAV_KEY = "favorite_repos_map"

    /**
     * Gson instance for serializing/deserializing the map.
     */
    private val gson = Gson()

    /**
     * TypeToken for the map of favorite repositories. Used for deserialization.
     */
    private val type = object : TypeToken<MutableMap<Long, Repository>>() {}.type

    /**
     * MutableStateFlow of the map of favorite repositories.
     */
    private val favoritesFlow = MutableStateFlow(getFavoritesMap())

    /**
     * Observes the map of favorite repositories.
     */
    fun observeFavorites(): StateFlow<MutableMap<Long, Repository>> = favoritesFlow

    /**
     * Retrieves the map of favorite repositories (ID -> Repository object) from SharedPreferences.
     */
    private fun getFavoritesMap(): MutableMap<Long, Repository> {
        val json = prefs.getString(FAV_KEY, null)
        return if (json.isNullOrBlank()) {
            mutableMapOf()
        } else {
            // Deserialize the JSON string back into the map
            gson.fromJson(json, type) ?: mutableMapOf()
        }
    }

    /**
     * Saves the current favorite map to SharedPreferences as a JSON string.
     */
    private fun saveFavoritesMap(map: Map<Long, Repository>) {
        // Serialize the map to a JSON string
        val json = gson.toJson(map, type)
        prefs.edit { putString(FAV_KEY, json) }
    }

    /**
     * Retrieves only the IDs of the favorited repositories (used by ViewModels to track state).
     */
    fun getFavoriteIds(): Set<String> {
        return favoritesFlow.value.keys.map { it.toString() }.toSet()
    }

    /**
     * Retrieves the full list of favorited repository objects.
     */
    fun getFavoriteRepositories(): List<Repository> {
        return favoritesFlow.value.values.toList()
    }

    /**
     * Toggles the favorite status for a repository, saving/removing the full object.
     * Accepts the full Repository object to save its details in JSON.
     * @param repo The Repository object to add or remove.
     * @return true if it is now favorited, false otherwise.
     */
    fun toggleFavorite(repo: Repository): Boolean {
        val currentMap = getFavoritesMap()
        val repoId = repo.id

        val isFavorite = if (currentMap.containsKey(repoId)) {
            currentMap.remove(repoId)
            false
        } else {
            currentMap[repoId] = repo
            true
        }

        // Save to SharedPreferences
        saveFavoritesMap(currentMap)

        // Notify ViewModels immediately
        favoritesFlow.value = currentMap

        return isFavorite
    }
}