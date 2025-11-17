package com.example.githubrepository.data.remote

import com.example.githubrepository.domain.model.Repository
import com.google.gson.annotations.SerializedName

/**
 * Represents the top-level response returned by the GitHub search API.
 *
 * The GitHub API wraps search results inside an "items" array. This DTO
 * extracts that list and maps each item to [RepositoryDto].
 *
 * @property repositories The list of repository DTOs returned by the API.
 */
data class SearchResponse(
    @SerializedName("items")
    val repositories: List<RepositoryDto>
)

/**
 * Data Transfer Object (DTO) representing a GitHub repository as returned
 * by the REST API. This class is responsible only for parsing API JSON,
 * and should not contain business logic.
 *
 * @property id Unique identifier for the repository.
 * @property name The repository name.
 * @property description Optional description provided by the repository owner.
 * @property starCount Total star count of the repository.
 * @property forkCount Number of times the repository has been forked.
 * @property language Primary programming language used in the repository.
 * @property owner Nested DTO containing repository owner details.
 */
data class RepositoryDto(
    val id: Long,
    val name: String,
    val description: String?,
    @SerializedName("stargazers_count")
    val starCount: Int,
    @SerializedName("forks_count")
    val forkCount: Int,
    val language: String?,
    val owner: OwnerDto
) {
    /**
     * Converts this DTO into the domain-layer [Repository] model.
     * Provides fallback values for optional API fields.
     *
     * @return A fully populated [Repository] object.
     */
    fun toDomain() = Repository(
        id = id,
        name = name,
        description = description ?: "No description available.",
        starCount = starCount,
        forkCount = forkCount,
        language = language ?: "Unknown",
        ownerName = owner.login,
        ownerAvatarUrl = owner.avatarUrl
    )
}

/**
 * Data Transfer Object representing the owner of a GitHub repository.
 *
 * @property login GitHub username of the repository owner.
 * @property avatarUrl URL of the owner's profile avatar image.
 */
data class OwnerDto(
    val login: String,
    @SerializedName("avatar_url")
    val avatarUrl: String
)