package com.jejecomms.businesscardapp.repository

import android.content.Context
import com.google.gson.Gson
import com.jejecomms.businesscardapp.R
import com.jejecomms.businesscardapp.model.ContactsListWrapper
import com.jejecomms.businesscardapp.model.ContactsModel
import java.util.Locale

/**
 * ContactsRepository is a class that provides methods to read contacts from a JSON file.
 */
class ContactsRepository {

    /**
     * Reads contacts from a JSON file in the assets folder and parses them
     * into a list of ContactsModel objects.
     *
     * @param context The application context to access assets.
     * @return A List of ContactsModel, or null if an error occurs during reading or parsing.
     */
    fun getContacts(context: Context): List<ContactsModel>? {
        return try {
            val inputStream = context.resources.openRawResource(R.raw.contacts)
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val gson = Gson()
            val wrapper = gson.fromJson(jsonString, ContactsListWrapper::class.java)
            val contacts = wrapper?.contacts // Extract the list from the wrapper
            contacts?.sortedBy { it.name.uppercase(Locale.getDefault()) }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

}