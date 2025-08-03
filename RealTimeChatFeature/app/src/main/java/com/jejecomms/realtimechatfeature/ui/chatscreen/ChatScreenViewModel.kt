package com.jejecomms.realtimechatfeature.ui.chatscreen

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jejecomms.realtimechatfeature.data.local.ChatMessageEntity
import com.jejecomms.realtimechatfeature.data.local.ChatRoomMemberEntity
import com.jejecomms.realtimechatfeature.data.repository.ChatRepository
import com.jejecomms.realtimechatfeature.utils.Constants.KEY_SENDER_ID
import com.jejecomms.realtimechatfeature.utils.Constants.SENDER_NAME
import com.jejecomms.realtimechatfeature.utils.Constants.SENDER_NAME_PREF
import com.jejecomms.realtimechatfeature.utils.Constants.USER_JOINED_THE_CHAT_ROOM
import com.jejecomms.realtimechatfeature.utils.Constants.YOU_HAVE_JOINED_THE_CHAT_ROOM
import com.jejecomms.realtimechatfeature.utils.SharedPreferencesUtil
import com.jejecomms.realtimechatfeature.utils.UuidGenerator
import kotlinx.coroutines.Job
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
     * Retrieves the sender ID from SharedPreferences.
     */
    val currentSenderId = SharedPreferencesUtil.getString(KEY_SENDER_ID)

    /**
     * Retrieves the sender name from SharedPreferences.
     */
    val senderName = SharedPreferencesUtil.getString(SENDER_NAME_PREF)

    /**
     * Job for the Firestore listener for messages.
     */
    private var firestoreMessagesListenerJob: Job? = null

    /**
     * Job for the local data messages collector.
     */
    private var localDataMessagesCollectorJob: Job? = null

    /**
     * Initializes the chat room by starting to listen for messages and members.
     * This should be called whenever the user navigates to a new chat screen.
     *
     * @param roomId The ID of the chat room to initialize.
     */
    fun initializeChatRoom(roomId: String) {
        // Cancel any previous jobs to prevent conflicts
        firestoreMessagesListenerJob?.cancel()
        localDataMessagesCollectorJob?.cancel()

        // Launch a separate coroutine to listen to Firestore changes and update the local DB.
        // This is a long-running job that should be active as long as the screen is.
        firestoreMessagesListenerJob = viewModelScope.launch {
            try {
                chatRepository.startFirestoreMessageListener(roomId).collect { remoteMessages ->
                    chatRepository.insertMessages(remoteMessages)
                }
            } catch (_: Exception) { }
        }

        // Launch a separate coroutine to combine local data and update the UI state.
        // This will trigger whenever the local database changes (which is updated by the job above).
        localDataMessagesCollectorJob = viewModelScope.launch {
            try {
                _uiState.value = ChatScreenState.Loading
                combine(
                    chatRepository.getLocalMessages(roomId),
                    chatRepository.getGroupMembers(roomId)
                ) { chatMessages, groupMembers ->
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
                            isSystemMessage = true,
                            roomId = roomId
                        )
                    }
                    (chatMessages + joinMessages).sortedBy { it.timestamp }
                }.collect { combinedMessages ->
                    _uiState.value = ChatScreenState.Content(combinedMessages)
                }
            } catch (_: Exception) {
            }
        }

        // Check if the user has joined the room. This can run independently.
        viewModelScope.launch {
            currentSenderId?.let {
                val senderName = SharedPreferencesUtil.getString(SENDER_NAME_PREF) ?: SENDER_NAME
                if (!chatRepository.hasJoinTheGroup(roomId, it)) {
                    joinMemberToRoom(it, senderName, roomId)
                }
            }
        }

        // Launch a new coroutine to update the last read timestamp
//        viewModelScope.launch {
//            chatRepository.updateLastReadTimestamp(roomId, System.currentTimeMillis())
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
    fun sendMessage(text: String, senderId: String, roomId: String) {
        if (text.isNotBlank()) {
            val newMessage = ChatMessageEntity(
                senderId = senderId,
                senderName = senderName?:SENDER_NAME,
                text = text,
                timestamp = System.currentTimeMillis(),
                roomId = roomId
            )

            viewModelScope.launch {
                chatRepository.sendMessage(roomId, newMessage)
            }
        }
    }

    /**
     * Retries sending a failed message.
     * This function updates the message status back to SENDING and attempts to resend it.
     *
     * @param message The ChatMessage object to retry.
     */
    fun retrySendMessage(message: ChatMessageEntity, roomId: String) {
        viewModelScope.launch {
            chatRepository.retrySingleMessage(roomId, message)
        }
    }

    /**
     * Join a new member to the room.
     * This method should only be called once per user session.
     *
     * @param userName The name of the user who joined.
     * @param senderId The unique ID of the user who joined.
     */
    private fun joinMemberToRoom(senderId: String, userName: String, roomId: String) {
        val memberId = UuidGenerator.generateUniqueId()
        val joinData = ChatRoomMemberEntity(
            id = memberId,
            senderId = senderId,
            senderName = userName,
            timestamp = System.currentTimeMillis(),
            isGroupMember = false,
            roomId = ""
        )

        viewModelScope.launch {
            chatRepository.joinRoom(roomId, joinData)
        }
    }
}