package com.jejecomms.realtimechatfeature.ui.chatroomscreen

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.jejecomms.realtimechatfeature.data.repository.ChatRoomRepository

/**
 *  Factory for creating a [ChatRoomViewModel] with a [ChatRoomRepository] dependency.
 */
class ChatRoomViewModelFactory(
    private val repository: ChatRoomRepository,
    private val application: Application,
    private val roomId: String
) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatRoomViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatRoomViewModel(repository, application, roomId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}