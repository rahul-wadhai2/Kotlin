package com.jejecomms.realtimechatfeature.ui.chatroomdetailscreen

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModel
import com.jejecomms.realtimechatfeature.data.repository.ChatRoomDetailRepository

/**
 *  Factory for creating a [ChatRoomDetailViewModel] with a [ChatRoomDetailRepository] dependency.
 */
class ChatRoomDetailViewModelFactory(
    private val chatRoomDetailRepository: ChatRoomDetailRepository,
    private val roomId: String?
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatRoomDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatRoomDetailViewModel(chatRoomDetailRepository, roomId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}