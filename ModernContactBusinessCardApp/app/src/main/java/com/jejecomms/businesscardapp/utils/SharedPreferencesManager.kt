package com.jejecomms.businesscardapp.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object SharedPreferencesManager {

    /**
     * The name of the SharedPreferences file.
     */
    private const val PREF_NAME = "JeJecoms"

    const val KEY_USER_FAVORITE = "user_favorite"

    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Saves a List of any type to SharedPreferences as a JSON string.
     * @param key The key to store the list under.
     * @param list The list to save.
     */
    fun <T> saveList(context: Context, key: String, list: List<T>) {
        getSharedPreferences(context).edit() {
            val gson = Gson()
            val json = gson.toJson(list) // Convert the list to a JSON string
            putString(key, json)
            apply()
        }
    }

    /**
     * Retrieves a List of a specific type from SharedPreferences from its JSON string representation.
     * @param key The key to retrieve the list from.
     * @return The retrieved List, or an empty list if not found.
     */
    internal inline fun <reified T> getList(context: Context, key: String): List<T> {
        val sharedPrefs = getSharedPreferences(context)
        val gson = Gson()
        val json = sharedPrefs.getString(key, null)
        return if (json != null) {
            // Use TypeToken to correctly deserialize generic lists
            val type = object : TypeToken<List<T>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } else {
            emptyList()
        }
    }
}