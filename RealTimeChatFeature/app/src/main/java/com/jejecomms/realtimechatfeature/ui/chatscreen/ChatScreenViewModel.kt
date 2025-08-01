package com.jejecomms.realtimechatfeature.ui.chatscreen

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jejecomms.realtimechatfeature.data.local.ChatMessageEntity
import com.jejecomms.realtimechatfeature.data.model.MessageStatus
import com.jejecomms.realtimechatfeature.data.repository.ChatRepository
import com.jejecomms.realtimechatfeature.utils.Constants.GENERAL_CHAT_ROOM_ID
import com.jejecomms.realtimechatfeature.utils.Constants.KEY_SENDER_ID
import com.jejecomms.realtimechatfeature.utils.Constants.SENDER_NAME
import com.jejecomms.realtimechatfeature.utils.SharedPreferencesUtil
import com.jejecomms.realtimechatfeature.utils.UuidGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 *  ViewModel for the chat screen. It interacts with the ChatRepository to send messages
 * and manages the UI state related to messages.
 */
class ChatScreenViewModel(
    private val chatRepository: ChatRepository,
    private val application: Application,
) : AndroidViewModel(application) {

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
        // Start collecting messages from the local Room database
        viewModelScope.launch {
            chatRepository.getLocalMessages().collect { messages ->
                _uiState.value = ChatScreenState.Content(messages)
            }
        }

        // Start the Firestore listener to keep the local database in sync
        GENERAL_CHAT_ROOM_ID.let { chatRepository.startFirestoreMessageListener(it) }

//        // It is crucial to check Firestore to ensure this message is only sent once per user.
//        val currentSenderId = SharedPreferencesUtil.getString(KEY_SENDER_ID)
//        viewModelScope.launch {
//            currentSenderId?.let {
//                println("True: "+chatRepository.hasJoinMessageBeenSent(GENERAL_CHAT_ROOM_ID, it))
//                if (!chatRepository.hasJoinMessageBeenSent(GENERAL_CHAT_ROOM_ID, it)) {
//                    currentSenderId.let { sendJoinMessage(it, SENDER_NAME) }
//                }
//            }
//        }
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
        if (text.isNotBlank()) {
            // Generate unique IDs for the message
            val messageId = UuidGenerator.generateUniqueId()
            val clientGeneratedId = UuidGenerator.generateUniqueId()

            // Create a new ChatMessage with SENDING status
            val newMessage = ChatMessageEntity(
                id = messageId,
                clientGeneratedId = clientGeneratedId,
                senderId = senderId,
                senderName = senderName,
                text = text,
                timestamp = System.currentTimeMillis(),
                isSystemMessage = false,
                status = MessageStatus.SENDING
            )

            viewModelScope.launch {
                chatRepository.sendMessage(GENERAL_CHAT_ROOM_ID, newMessage)
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
    fun retrySendMessage(message: ChatMessageEntity) {
        viewModelScope.launch {
            chatRepository.retrySingleMessage(GENERAL_CHAT_ROOM_ID, message)
        }
    }

    /**
     * Sends a system message indicating a user has joined the chat.
     * This method should only be called once per user session.
     *
     * @param userName The name of the user who joined.
     * @param senderId The unique ID of the user who joined.
     */
    private fun sendJoinMessage(senderId: String, userName: String) {
        val joinMessage = ChatMessageEntity(
            id = UuidGenerator.generateUniqueId(),
            clientGeneratedId = UuidGenerator.generateUniqueId(),
            senderId = senderId, // Use the user's actual ID
            senderName = "system",
            text = "$userName has joined the chat",
            timestamp = System.currentTimeMillis(),
            isSystemMessage = true,
            status = MessageStatus.SENT
        )

        viewModelScope.launch {
            chatRepository.sendMessage(GENERAL_CHAT_ROOM_ID, joinMessage)
        }
    }
}