package com.jejecomms.businesscardapp.ui.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.jejecomms.businesscardapp.repository.ContactsRepository

/**
 * ContactsViewModelFactory is a factory class that is responsible for creating.
 */
class ContactsViewModelFactory (contactsRepository: ContactsRepository?) :
    ViewModelProvider.Factory {
    /**
     * ContactsRepository instance.
     */
    private val mContactsRepository: ContactsRepository? = contactsRepository

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ContactsViewModel::class.java)) {
            return mContactsRepository?.let { ContactsViewModel(it) } as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}