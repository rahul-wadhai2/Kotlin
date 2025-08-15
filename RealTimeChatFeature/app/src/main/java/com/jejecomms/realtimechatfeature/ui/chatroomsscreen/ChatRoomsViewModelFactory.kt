package com.jejecomms.realtimechatfeature.ui.chatroomsscreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.jejecomms.realtimechatfeature.ChatApplication
import com.jejecomms.realtimechatfeature.data.repository.ChatRoomsRepository
import com.jejecomms.realtimechatfeature.data.repository.LoginRepository

/**
 * Factory for creating a [ChatRoomsViewModel] with a [ChatRoomsRepository] dependency.
 */
class ChatRoomsViewModelFactory(private val chatRoomsRepository: ChatRoomsRepository,
                                private val loginRepository: LoginRepository,
                                private val application: ChatApplication) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatRoomsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatRoomsViewModel(chatRoomsRepository, loginRepository, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}