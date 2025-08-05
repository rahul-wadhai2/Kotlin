package com.jejecomms.realtimechatfeature.ui.chatroomscreen

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jejecomms.realtimechatfeature.data.local.ChatMessageEntity
import com.jejecomms.realtimechatfeature.data.local.ChatRoomMemberEntity
import com.jejecomms.realtimechatfeature.data.model.MessageStatus
import com.jejecomms.realtimechatfeature.data.model.MessageType
import com.jejecomms.realtimechatfeature.data.repository.ChatRoomRepository
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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * ViewModel for the chat room screen. It interacts with the [ChatRoomRepository] to send messages
 * and manages the UI state related to messages.
 */
class ChatRoomViewModel(
    private val chatRoomRepository: ChatRoomRepository,
    application: Application,
    private val roomId: String
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
     * State flow for the roomId check when entering in room.
     */
    private val _isRoomExists =  MutableStateFlow<Boolean?>(null)

    /**
     * Exposes the boolean is roomId exists as a StateFlow.
     */
    val isRoomExists: StateFlow<Boolean?> = _isRoomExists.asStateFlow()

    /**
     * State flow for image upload progress
     */
    private val _imageUploadProgress = MutableStateFlow(0)

    /**
     * Exposes the image upload progress as a StateFlow
     */
    val imageUploadProgress: StateFlow<Int> = _imageUploadProgress.asStateFlow()

    init {
        checkIfUserHasJoined()
        updateLastReadTimestamp()
    }

    /**
     * Checks if the user has joined the room and joins them if not.
     */
    private fun checkIfUserHasJoined() {
        viewModelScope.launch {
            currentSenderId?.let {
                val senderName = SharedPreferencesUtil.getString(SENDER_NAME_PREF) ?: SENDER_NAME
                if (!chatRoomRepository.hasJoinTheGroup(roomId, it)) {
                    joinMemberToRoom(it, senderName, roomId)
                }
            }
        }
    }

    /**
     * Encapsulates the long-running coroutines for data synchronization.
     */
    fun startDataCollectionAndSync(roomId: String) {
        // Cancel any previous jobs and start the Firestore listener
        firestoreMessagesListenerJob?.cancel()
        firestoreMessagesListenerJob = viewModelScope.launch {
            try {
                chatRoomRepository.startFirestoreMessageListener(roomId).collect { remoteMessages ->
                    chatRoomRepository.insertMessages(remoteMessages)
                }
            } catch (_: Exception) { }
        }

        // Cancel any previous jobs and start the local database collector
        localDataMessagesCollectorJob?.cancel()
        localDataMessagesCollectorJob = viewModelScope.launch {
            try {
                _uiState.value = ChatScreenState.Loading
                combine(
                    chatRoomRepository.getLocalMessages(roomId),
                    chatRoomRepository.getGroupMembers(roomId)
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
                            roomId = roomId,
                            messageType = MessageType.TEXT
                        )
                    }
                    (chatMessages + joinMessages).sortedBy { it.timestamp }
                }.collect { combinedMessages ->
                    _uiState.value = ChatScreenState.Content(combinedMessages)
                }
            } catch (_: Exception) {
            }
        }

        _isRoomExists.value = null
        checkIfRoomExists(roomId)
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
                roomId = roomId,
                messageType = MessageType.TEXT
            )

            viewModelScope.launch {
                chatRoomRepository.sendMessage(roomId, newMessage)
            }
        }
    }

    /**
     * Handles sending image messages.
     *
     * @param imageUri The URI of the image to send.
     */
    fun sendImageMessage(currentSenderId: String,roomId: String, imageUri: Uri
                         ,context: Context) {
        viewModelScope.launch {
            val messageId = UuidGenerator.generateUniqueId()
            //Save the image to a local cache and get its path
            val localImagePath = chatRoomRepository.saveImageToCache(imageUri, messageId
                ,context)
            // Create a temporary message entity for the image
            val imageMessage = ChatMessageEntity(
                id = messageId,
                roomId = roomId,
                senderId = currentSenderId,
                senderName = senderName ?: SENDER_NAME,
                text = "",
                imageUrl = localImagePath,// Handle only offline will logic change for online.
                timestamp = System.currentTimeMillis(),
                status = MessageStatus.SENDING,
                messageType = MessageType.IMAGE
            )

            // Reset progress
            _imageUploadProgress.value = 0

            // Start the upload process
            chatRoomRepository.sendImageMessage(
                roomId = roomId,
                message = imageMessage,
                imageUri = imageUri,
                onProgress = { progress ->
                    // Update progress state for the UI
                    _imageUploadProgress.value = progress
                }
            )
        }
    }

    /**
     * Retries sending a failed message.
     * This function now handles both text and image messages.
     *
     * @param message The failed message to retry.
     * @param roomId The ID of the chat room.
     */
    fun retrySendMessage(message: ChatMessageEntity, roomId: String) {
        viewModelScope.launch {
            chatRoomRepository.retrySingleMessage(roomId, message)
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
            chatRoomRepository.joinRoom(roomId, joinData)
        }
    }

    /**
     * Updates the last read timestamp for the room.
     */
    fun updateLastReadTimestamp() {
        viewModelScope.launch {
            chatRoomRepository.updateLastReadTimestamp(roomId, System.currentTimeMillis())
        }
    }

    /***
     * Checks if the room exists.
     */
    fun checkIfRoomExists(roomId: String) {
        viewModelScope.launch {
            _isRoomExists.value = chatRoomRepository.checkIfRoomIdExists(roomId)
        }
    }
}