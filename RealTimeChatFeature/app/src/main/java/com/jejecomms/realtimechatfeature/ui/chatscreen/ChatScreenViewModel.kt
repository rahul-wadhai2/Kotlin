package com.jejecomms.realtimechatfeature.ui.chatscreen

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jejecomms.realtimechatfeature.data.local.ChatMessageEntity
import com.jejecomms.realtimechatfeature.data.local.GroupMembersEntity
import com.jejecomms.realtimechatfeature.data.repository.ChatRepository
import com.jejecomms.realtimechatfeature.utils.Constants.GENERAL_CHAT_ROOM_ID
import com.jejecomms.realtimechatfeature.utils.Constants.KEY_SENDER_ID
import com.jejecomms.realtimechatfeature.utils.Constants.SENDER_NAME
import com.jejecomms.realtimechatfeature.utils.Constants.USER_JOINED_THE_CHAT_ROOM
import com.jejecomms.realtimechatfeature.utils.Constants.YOU_HAVE_JOINED_THE_CHAT_ROOM
import com.jejecomms.realtimechatfeature.utils.SharedPreferencesUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * ViewModel for the chat screen. It interacts with the ChatRepository to send messages
 * and manages the UI state related to messages.
 */
class ChatScreenViewModel(
    private val chatRepository: ChatRepository,
    application: Application,
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
        // Start the Firestore listener to keep the local database in sync.
        GENERAL_CHAT_ROOM_ID.let { chatRepository.startFirestoreMessageListener(it) }

        // This combines the flow of chat messages and group members into a single list.
        val currentSenderId = SharedPreferencesUtil.getString(KEY_SENDER_ID)

        viewModelScope.launch {
            combine(
                chatRepository.getLocalMessages(),
                chatRepository.getGroupMembers(GENERAL_CHAT_ROOM_ID)
            ) { chatMessages, groupMembers ->
                // Map GroupMembersEntity to ChatMessageEntity for a unified list
                val joinMessages = groupMembers.map { member ->
                    val joinMessageText = if (member.senderId == currentSenderId) {
                        YOU_HAVE_JOINED_THE_CHAT_ROOM
                    } else {
                        "${member.senderName} $USER_JOINED_THE_CHAT_ROOM"
                    }
                    ChatMessageEntity(
                        id = member.id,
                        senderId = member.senderId,
                        senderName = member.senderName,
                        text = joinMessageText,
                        timestamp = member.timestamp,
                        isSystemMessage = true
                    )
                }
                // Combine and sort all messages by timestamp
                (chatMessages + joinMessages).sortedBy { it.timestamp }
            }.collect { combinedMessages ->
                _uiState.value = ChatScreenState.Content(combinedMessages)
            }
        }

        // It is crucial to check Firestore to ensure this message is only sent once per user.
        viewModelScope.launch {
            currentSenderId?.let {
                if (!chatRepository.hasJoinTheGroup(GENERAL_CHAT_ROOM_ID, it)) {
                    addMemberToGroup(it, SENDER_NAME)
                }
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
        if (text.isNotBlank()) {
            val newMessage = ChatMessageEntity(
                senderId = senderId,
                senderName = senderName,
                text = text,
                timestamp = System.currentTimeMillis()
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
     * @param message The ChatMessage object to retry.
     */
    fun retrySendMessage(message: ChatMessageEntity) {
        viewModelScope.launch {
            chatRepository.retrySingleMessage(GENERAL_CHAT_ROOM_ID, message)
        }
    }

    /**
     * Add a new member to the group.
     * This method should only be called once per user session.
     *
     * @param userName The name of the user who joined.
     * @param senderId The unique ID of the user who joined.
     */
    private fun addMemberToGroup(senderId: String, userName: String) {
        val joinMessage = GroupMembersEntity(
            senderId = senderId,
            senderName = userName,
            text = USER_JOINED_THE_CHAT_ROOM,
            timestamp = System.currentTimeMillis(),
            isGroupMember = true,
        )

        viewModelScope.launch {
            chatRepository.joinedGroup(GENERAL_CHAT_ROOM_ID, joinMessage)
        }
    }
}