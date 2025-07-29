package com.jejecomms.realtimechatfeature.data.repository

import com.jejecomms.realtimechatfeature.data.model.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class ChatRepository {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: Flow<List<ChatMessage>> = _messages.asStateFlow()

    // Simulate initial messages
    init {
        _messages.value = listOf(
            ChatMessage(UUID.randomUUID().toString(), "Hi there!", "user1", System.currentTimeMillis() - 600000),
            ChatMessage(UUID.randomUUID().toString(), "Hello!", "user2", System.currentTimeMillis() - 540000),
            ChatMessage(UUID.randomUUID().toString(), "How are you?", "user1", System.currentTimeMillis() - 480000),
            ChatMessage(UUID.randomUUID().toString(), "I'm good, thanks! And you?", "user2", System.currentTimeMillis() - 420000),
            ChatMessage(UUID.randomUUID().toString(), "User1 has joined the chat.", "system", System.currentTimeMillis() - 300000, isSystemMessage = true),
            ChatMessage(UUID.randomUUID().toString(), "I'm doing great too!", "user1", System.currentTimeMillis() - 240000),
            ChatMessage(UUID.randomUUID().toString(), "What's up?", "user2", System.currentTimeMillis() - 180000),
            ChatMessage(UUID.randomUUID().toString(), "User2 has left the chat.", "system", System.currentTimeMillis() - 120000, isSystemMessage = true),
            ChatMessage(UUID.randomUUID().toString(), "Not much, just chilling.", "user1", System.currentTimeMillis() - 60000),
            ChatMessage(UUID.randomUUID().toString(), "Ok, cool!", "user2", System.currentTimeMillis())
        )
    }

    fun sendMessage(text: String, senderId: String) {
        val newMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            text = text,
            senderId = senderId,
            timestamp = System.currentTimeMillis()
        )
        _messages.value = _messages.value + newMessage
    }
}