package com.jejecomms.realtimechatfeature.ui.chatscreen

import com.jejecomms.realtimechatfeature.data.local.ChatMessageEntity


/**
 * Sealed class representing the different UI states of the chat screen.
 * This is for overall screen status.
 */
sealed class ChatScreenState {
    /**
     * Initial state or when data is being loaded.
     */
    object Loading : ChatScreenState()

    /**
     * State when the chat screen is ready and displaying messages.
     */
    data class Content(val messages: List<ChatMessageEntity>) : ChatScreenState()

    /**
     * State when an error occurs.
     *
     * @param message The error message to display.
     */
    data class Error(val message: String) : ChatScreenState()
}
