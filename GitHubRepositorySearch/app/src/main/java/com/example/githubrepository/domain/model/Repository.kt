package com.example.githubrepository.domain.model

import java.io.Serializable

/**
 * Domain model representing a GitHub repository.
 *
 * This class contains all essential information required to display
 * repository details in the UI as well as persist them locally, such as:
 *
 * @property id Unique identifier of the repository.
 * @property name Repository name.
 * @property description Short description of the project.
 * @property starCount Total number of stars the repository has received.
 * @property forkCount Number of forks created from this repository.
 * @property language Primary programming language used in the project.
 * @property ownerName GitHub username of the repository owner.
 * @property ownerAvatarUrl URL of the owner's profile avatar image.
 */
data class Repository(
    val id: Long,
    val name: String,
    val description: String,
    val starCount: Int,
    val forkCount: Int,
    val language: String,
    val ownerName: String,
    val ownerAvatarUrl: String
) : Serializable
