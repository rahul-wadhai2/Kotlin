package com.example.githubrepository.presentation.ui

/**
 * Represents all navigation destinations in the app.
 *
 * Each screen is defined as an object extending [Screen] with a unique
 * navigation route. Screens that require arguments (such as [Detail])
 * include helper functions (e.g., [Detail.createRoute]) to generate
 * a valid navigation path.
 *
 * @property route The unique navigation route used by the NavHost.
 */
sealed class Screen(val route: String) {

    /** The app’s splash screen, shown during initial loading. */
    object Splash : Screen("splash")

    /** The search screen where users can search GitHub repositories. */
    object Search : Screen("search")

    /**
     * Detail screen for displaying information about a specific repository.
     *
     * This screen uses a dynamic navigation argument: `repoId`.
     */
    object Detail : Screen("detail/{repoId}") {

        /**
         * Creates a valid navigation route for the Detail screen.
         *
         * @param repoId The ID of the repository to show.
         * @return A complete route string including the repository ID.
         */
        fun createRoute(repoId: Long) = "detail/$repoId"
    }

    /** Screen that displays the list of favorited repositories. */
    object Favorites : Screen("favorites")
}
