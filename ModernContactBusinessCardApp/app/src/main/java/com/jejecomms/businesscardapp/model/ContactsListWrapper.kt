package com.jejecomms.businesscardapp.model

import com.google.gson.annotations.SerializedName

/**
 * ContactsListWrapper is a data class that represents a wrapper for a list of contacts.
 */
data class ContactsListWrapper(
    @SerializedName("contacts")
    val contacts: List<ContactsModel>
)