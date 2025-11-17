package com.example.githubrepository.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

/**
 *The GitHubApi class serves as the interface layer for making network requests to
 * the GitHub REST API.
 */
interface GitHubApi {
    @GET("search/repositories")
    suspend fun searchRepositories(
        @Query("q") query: String
    ): SearchResponse
}