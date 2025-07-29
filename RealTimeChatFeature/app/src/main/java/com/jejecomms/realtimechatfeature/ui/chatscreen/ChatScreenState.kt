package com.jejecomms.realtimechatfeature.ui.chatscreen

import com.jejecomms.realtimechatfeature.data.model.ChatMessage

/**
 *  Sealed class for representing different states of the chat screen.
 */
sealed class ChatScreenState {
    object Loading : ChatScreenState()
    data class Content(val messages: List<ChatMessage>) : ChatScreenState()
    data class Error(val message: String) : ChatScreenState()
}