package com.example.githubrepository.data.repository

import com.example.githubrepository.data.local.FavoritesLocalDataSource
import com.example.githubrepository.data.remote.GitHubApi
import com.example.githubrepository.data.remote.RepositoryDto
import com.example.githubrepository.domain.model.Repository
import com.example.githubrepository.domain.repository.GitHubRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Implementation of the GitHubRepository interface.
 */
class GitHubRepositoryImpl @Inject constructor(
    private val api: GitHubApi,
    private val favoritesSource: FavoritesLocalDataSource
) : GitHubRepository {

    override suspend fun searchRepositories(query: String): List<Repository> {
        return api.searchRepositories(query).repositories.map(RepositoryDto::toDomain)
    }

    override fun getFavoriteIds(): Set<Long> {
        return favoritesSource.getFavoriteIds().mapNotNull { it.toLongOrNull() }.toSet()
    }

    override suspend fun getFavoriteRepositories(): List<Repository> {
        return favoritesSource.getFavoriteRepositories()
    }

    override fun toggleFavorite(repo: Repository): Boolean {
        return favoritesSource.toggleFavorite(repo)
    }

    override fun observeFavorites(): StateFlow<MutableMap<Long, Repository>> {
        return favoritesSource.observeFavorites()
    }
}