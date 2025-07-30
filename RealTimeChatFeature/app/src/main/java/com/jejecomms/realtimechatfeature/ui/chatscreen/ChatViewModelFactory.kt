package com.jejecomms.realtimechatfeature.ui.chatscreen

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.jejecomms.realtimechatfeature.data.repository.ChatRepository

/**
 *  Factory class for creating ChatViewModel instances.
 */
class ChatViewModelFactory(
    private val repository: ChatRepository,
    private val application: Application,
    private val isSystemMessage: Boolean
) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatScreenViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatScreenViewModel(repository, application, isSystemMessage) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}