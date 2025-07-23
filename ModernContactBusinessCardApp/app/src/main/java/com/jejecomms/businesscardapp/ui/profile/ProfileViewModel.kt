package com.jejecomms.businesscardapp.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.jejecomms.businesscardapp.BaseViewModel

class ProfileViewModel : BaseViewModel() {
    private val _text = MutableLiveData<String>().apply {
        value = "This is Profile Fragment"
    }
    val text: LiveData<String> = _text
}