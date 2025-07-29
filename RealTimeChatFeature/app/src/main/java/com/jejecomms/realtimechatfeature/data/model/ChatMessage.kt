package com.jejecomms.realtimechatfeature.data.model

/**
 *  Chat message data class.
 */
data class ChatMessage(
    val id: String,
    val text: String,
    val senderId: String,
    val timestamp: Long,
    val isSystemMessage: Boolean = false
)