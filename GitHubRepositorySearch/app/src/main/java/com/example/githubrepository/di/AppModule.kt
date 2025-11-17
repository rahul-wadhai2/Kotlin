package com.example.githubrepository.di

import com.example.githubrepository.data.remote.GitHubApi
import com.example.githubrepository.data.repository.GitHubRepositoryImpl
import com.example.githubrepository.domain.repository.GitHubRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

/**
 * Dagger-Hilt module responsible for providing app-wide dependencies.
 *
 * Objects provided here are scoped as singletons and are shared across
 * the entire application lifecycle. This module wires up:
 *
 * - Retrofit instance for network communication
 * - GitHub API interface used for making HTTP calls
 * - Repository implementation used by ViewModels and use cases
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Provides a singleton Retrofit-based implementation of [GitHubApi].
     *
     * This API service is used to perform network requests to GitHub's REST API.
     * Gson is used as the JSON converter.
     *
     * @return Configured [GitHubApi] instance for making API calls.
     */
    @Provides
    @Singleton
    fun provideGitHubApi(): GitHubApi {
        return Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GitHubApi::class.java)
    }

    /**
     * Provides a singleton instance of [GitHubRepository].
     *
     * Hilt automatically constructs [GitHubRepositoryImpl] with all its required
     * dependencies. This method binds the implementation to the domain-level
     * interface so that the rest of the app depends only on the abstraction.
     *
     * @param repositoryImpl The automatically injected implementation.
     * @return The [GitHubRepository] abstraction.
     */
    @Provides
    @Singleton
    fun provideGitHubRepository(
        repositoryImpl: GitHubRepositoryImpl
    ): GitHubRepository = repositoryImpl
}
