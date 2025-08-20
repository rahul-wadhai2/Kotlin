package com.jejecomms.realtimechatfeature.ui.chatroomdetailscreen

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModel
import com.jejecomms.realtimechatfeature.ChatApplication
import com.jejecomms.realtimechatfeature.data.repository.ChatRoomDetailRepository
import com.jejecomms.realtimechatfeature.data.repository.ChatRoomsRepository
import com.jejecomms.realtimechatfeature.data.repository.LoginRepository

/**
 *  Factory for creating a [ChatRoomDetailViewModel] with a [ChatRoomDetailRepository] dependency.
 */
class ChatRoomDetailViewModelFactory(
    private val loginRepository: LoginRepository,
    private val chatRoomsRepository: ChatRoomsRepository,
    private val chatRoomDetailRepository: ChatRoomDetailRepository,
    private val application: ChatApplication
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatRoomDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatRoomDetailViewModel(loginRepository, chatRoomsRepository,
                chatRoomDetailRepository, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}