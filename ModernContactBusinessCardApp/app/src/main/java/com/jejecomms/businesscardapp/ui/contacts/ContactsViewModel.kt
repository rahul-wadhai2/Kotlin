package com.jejecomms.businesscardapp.ui.contacts

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.jejecomms.businesscardapp.BaseViewModel
import com.jejecomms.businesscardapp.model.ContactsModel
import com.jejecomms.businesscardapp.repository.ContactsRepository
import com.jejecomms.businesscardapp.utils.SharedPreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ContactsViewModel is a ViewModel class that provides data to the UI.
 */
class ContactsViewModel(private val contactsRepository: ContactsRepository) : BaseViewModel() {

    /**
     * MutableLiveData for the list of contacts.
     */
    private val mContactsListLiveData = MutableLiveData<List<ContactsModel>>()

    /**
     * LiveData for the list of contacts.
     */
    val contactsListLiveData: LiveData<List<ContactsModel>> = mContactsListLiveData

    /**
     * List of contact IDs.
     */
    var listContactId = mutableListOf<String>()

    /**
     * MutableLiveData for loading state.
     */
    private val isLoading = MutableLiveData<Boolean>()

    /**
     * LiveData to expose loading state to the UI.
     */
    val mIsLoading: LiveData<Boolean> get() = isLoading

    /**
     * Loads contacts from the repository and updates the LiveData.
     */
    fun loadContacts(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            isLoading.postValue(true)
            try {
                val contacts = contactsRepository.getContacts(context)

                withContext(Dispatchers.Main) {
                    mContactsListLiveData.value = contacts ?: emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                isLoading.postValue(false)
            } finally {
                isLoading.postValue(false)
            }
        }
    }

    /**
     * On Contact favorite clicked.
     */
    fun onContactFavoriteClicked(context: Context, user: ContactsModel) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentList = mContactsListLiveData.value?.toMutableList() ?: mutableListOf()
            val index = currentList.indexOfFirst { it.id == user.id }
            if (index != -1) {
                val contact = currentList[index]
                val contactId = contact.id
                listContactId = SharedPreferencesManager.getList<String>(context,
                    SharedPreferencesManager.KEY_USER_FAVORITE).toMutableList()
                if (listContactId.isNotEmpty()) {
                    if (listContactId.contains(contactId)) {
                        listContactId.remove(contactId)
                    } else {
                        listContactId.add(contactId)
                    }
                } else {
                    listContactId.add(contactId)
                }

                SharedPreferencesManager.saveList(context,SharedPreferencesManager.KEY_USER_FAVORITE
                    ,listContactId)
                mContactsListLiveData.postValue(ArrayList(currentList))
            }
        }
    }

}