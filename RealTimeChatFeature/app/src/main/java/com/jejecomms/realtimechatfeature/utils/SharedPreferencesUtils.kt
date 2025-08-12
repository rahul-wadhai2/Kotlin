package com.jejecomms.realtimechatfeature.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * A utility object for managing SharedPreferences in the application.
 * Provides methods to save and retrieve various data types.
 */
object SharedPreferencesUtils {

    /**
     * The name of the SharedPreferences file.
     */
    private const val PREFS_NAME = "chat_app_prefs"

    /**
     * The SharedPreferences instance used for data storage.
     */
    private lateinit var preferences: SharedPreferences

    /**
     * Initializes the SharedPreferences utility.
     *
     * @param context The application context.
     */
    fun init(context: Context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Saves a String value to SharedPreferences.
     *
     * @param key The key to associate with the value.
     * @param value The String value to save.
     */
    fun putString(key: String, value: String) {
        preferences.edit() { putString(key, value) }
    }

    /**
     * Retrieves a String value from SharedPreferences.
     *
     * @param key The key of the value to retrieve.
     * @param defaultValue The default value to return if the key is not found.
     * @return The retrieved String value, or the defaultValue if not found.
     */
    fun getString(key: String, defaultValue: String? = null): String? {
        return preferences.getString(key, defaultValue)
    }
}