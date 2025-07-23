package com.jejecomms.businesscardapp.ui.contacts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.jejecomms.businesscardapp.BaseViewModel

class ContactsViewModel : BaseViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is Contact Fragment"
    }
    val text: LiveData<String> = _text
}