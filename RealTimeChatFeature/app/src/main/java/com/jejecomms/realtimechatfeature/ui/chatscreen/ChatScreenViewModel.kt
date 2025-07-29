package com.jejecomms.realtimechatfeature.ui.chatscreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jejecomms.realtimechatfeature.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 *  ViewModel for the chat screen.
 */
class ChatScreenViewModel(
    private val chatRepository: ChatRepository = ChatRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChatScreenState>(ChatScreenState.Loading)
    val uiState: StateFlow<ChatScreenState> = _uiState

    // Assuming a fixed current user for demonstration
    val currentUserId = "user1"

    init {
        loadMessages()
    }

    private fun loadMessages() {
        viewModelScope.launch {
            chatRepository.messages
                .map { messages ->
                    ChatScreenState.Content(messages)
                }
                .catch { e ->
                    _uiState.value = ChatScreenState.Error("Failed to load messages: ${e.message}")
                }
                .collect { state ->
                    _uiState.value = state
                }
        }
    }

    fun sendMessage(text: String) {
        if (text.isNotBlank()) {
            viewModelScope.launch {
                chatRepository.sendMessage(text, currentUserId)
            }
        }
    }
}