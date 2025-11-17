package com.example.githubrepository.presentation.utils

/**
 * A generic wrapper class used to represent the state of a data operation.
 *
 * This helps the UI layer react to different states such as loading, success,
 * or failure when working with asynchronous operations like API calls or database queries.
 *
 * @param T The type of data being wrapped.
 * @property data The actual data returned from the operation, if available.
 * @property message An optional message, usually used to describe errors.
 */
sealed class Resource<T>(val data: T? = null, val message: String? = null) {

    /**
     * Represents a successful data fetch.
     *
     * @param data The result of the operation. Non-null for success.
     */
    class Success<T>(data: T) : Resource<T>(data)

    /**
     * Represents a loading state.
     *
     * @param data Optional cached/placeholder data to show while loading.
     */
    class Loading<T>(data: T? = null) : Resource<T>(data)

    /**
     * Represents an error state.
     *
     * @param message A message describing the error.
     * @param data Optional fallback data, if available.
     */
    class Error<T>(message: String, data: T? = null) : Resource<T>(data, message)
}
