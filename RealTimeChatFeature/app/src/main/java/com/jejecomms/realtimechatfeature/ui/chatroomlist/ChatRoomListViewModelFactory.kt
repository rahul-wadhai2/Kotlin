package com.jejecomms.realtimechatfeature.ui.chatroomlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.jejecomms.realtimechatfeature.ChatApplication
import com.jejecomms.realtimechatfeature.data.repository.ChatRepository

/**
 * Factory for creating a [ChatRoomListViewModel] with a [ChatRepository] dependency.
 */
class ChatRoomListViewModelFactory(private val chatRepository: ChatRepository
,private val application: ChatApplication) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatRoomListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatRoomListViewModel(chatRepository, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}