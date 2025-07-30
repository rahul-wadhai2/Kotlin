package com.jejecomms.realtimechatfeature.ui.chatscreen

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jejecomms.realtimechatfeature.R
import com.jejecomms.realtimechatfeature.data.model.ChatMessage
import com.jejecomms.realtimechatfeature.data.model.MessageStatus
import com.jejecomms.realtimechatfeature.data.repository.ChatRepository
import com.jejecomms.realtimechatfeature.utils.Constants.GENERAL_CHAT_ROOM_ID
import com.jejecomms.realtimechatfeature.utils.NetworkMonitor
import com.jejecomms.realtimechatfeature.utils.UuidGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 *  ViewModel for the chat screen. It interacts with the ChatRepository to send messages
 * and manages the UI state related to messages.
 */
class ChatScreenViewModel(
    private val chatRepository: ChatRepository,
    private val application: Application,
    private val isSystemMessage: Boolean,
) : AndroidViewModel(application) {

    /**
     * MutableStateFlow to hold the list of chat messages displayed in the UI.
     * It's private to ensure updates are controlled by the ViewModel.
     */
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())

    /**
     * Publicly exposed StateFlow for UI observation.
     */
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    /**
     * MutableStateFlow to hold the current UI state.
     */
    private val _uiState = MutableStateFlow<ChatScreenState>(ChatScreenState.Loading)

    /**
     * Publicly exposed StateFlow for UI observation.
     */
    val uiState: StateFlow<ChatScreenState> = _uiState

    /**
     * This runs when the ViewModel is first created.
     */
    init {
        GENERAL_CHAT_ROOM_ID.collectMessages()
    }

    /**
     * Collects real-time chat messages from the repository and updates the UI state.
     * This function should be called once to establish the real-time listener.
     *
     * @param this@collectMessages The ID of the chat room to listen for messages.
     */
    private fun String.collectMessages() {
        viewModelScope.launch {
            chatRepository.getMessages(this@collectMessages)
                .catch { e ->
                    _uiState.value = ChatScreenState
                        .Error(
                            application.getString(
                                R.string.message_load_failed_error, e.message
                            )
                        )
                }.collect { fetchedMessages ->
                    _uiState.value = ChatScreenState.Content(fetchedMessages)
                }
        }
    }

    /**
     * Sends a new chat message.
     * This function handles the entire flow:
     *
     * @param text The content of the message.
     * @param senderId The ID of the sender.
     * @param senderName The display name of the sender.
     */
    fun sendMessage(text: String, senderId: String, senderName: String) {
        //Check if the message text is empty.
        if (text.isBlank()) {
            _uiState.value =
                ChatScreenState.Error(application.getString(R.string.empty_message_error))
            return
        }

        // Generate unique IDs for the message
        val messageId = UuidGenerator.generateUniqueId()
        val clientGeneratedId = UuidGenerator.generateUniqueId()

        // Create a new ChatMessage with SENDING status
        val newMessage = ChatMessage(
            id = messageId,
            clientGeneratedId = clientGeneratedId,
            senderId = senderId,
            senderName = senderName,
            text = text,
            timestamp = System.currentTimeMillis(),
            isSystemMessage = isSystemMessage,
            status = MessageStatus.SENDING
        )

        // Add the message to the local list immediately for optimistic UI update.
        // This makes the message appear instantly to the user, even before it's confirmed by Firebase.
        _messages.update { currentMessages ->
            currentMessages + newMessage
        }

        //Launch a coroutine in the ViewModel's scope to send the message asynchronously.
        viewModelScope.launch {

            if (!NetworkMonitor.isOnline()) {
                _messages.update { currentMessages ->
                    currentMessages.map { msg ->
                        msg.copy(status = MessageStatus.FAILED)
                    }
                }
                return@launch
            }

            val result = chatRepository.sendMessage(GENERAL_CHAT_ROOM_ID, newMessage)

            // Handle the result of the message send operation.
            if (result.isFailure) {
                val exception = result.exceptionOrNull()
                val errorMessage = exception?.message ?: application
                    .getString(R.string.unknown_error_sending_message)
                _messages.update { currentMessages ->
                    currentMessages.map { msg ->
                        if (msg.id == newMessage.id) {
                            msg.copy(status = MessageStatus.FAILED)
                        } else {
                            msg
                        }
                    }
                }
                val fullErrorMessage = application.getString(
                    R.string.failed_to_send_message_error,
                    newMessage.id,
                    errorMessage
                )
                _uiState.value = ChatScreenState.Error(fullErrorMessage)
            } else {
                _messages.update { currentMessages ->
                    currentMessages.map { msg ->
                        if (msg.id == newMessage.id) {
                            msg.copy(status = MessageStatus.SENT)
                        } else {
                            msg
                        }
                    }
                }
            }
        }
    }

    /**
     * Retries sending a failed message.
     * This function updates the message status back to SENDING and attempts to resend it.
     *
     * @param roomId The ID of the chat room.
     * @param message The ChatMessage object to retry.
     */
    fun retrySendMessage(message: ChatMessage) {
        if (message.status != MessageStatus.FAILED) {
            println("Cannot retry message not in FAILED status.")
            return
        }

        // Update the message status to SENDING locally before retrying
        _messages.update { currentMessages ->
            currentMessages.map { msg ->
                if (msg.id == message.id) {
                    msg.copy(status = MessageStatus.SENDING)
                } else {
                    msg
                }
            }
        }

        // Re-launch the sending process for the message
        viewModelScope.launch {
            val result = chatRepository.sendMessage(GENERAL_CHAT_ROOM_ID, message)
            _messages.update { currentMessages ->
                currentMessages.map { msg ->
                    if (msg.id == message.id) {
                        if (result.isSuccess) {
                            msg.copy(status = MessageStatus.SENT)
                        } else {
                            msg.copy(status = MessageStatus.FAILED)
                        }
                    } else {
                        msg
                    }
                }
            }
        }
    }
}