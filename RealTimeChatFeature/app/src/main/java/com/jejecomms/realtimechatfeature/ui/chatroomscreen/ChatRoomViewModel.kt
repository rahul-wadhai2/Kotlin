package com.jejecomms.realtimechatfeature.ui.chatroomscreen

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jejecomms.realtimechatfeature.R
import com.jejecomms.realtimechatfeature.data.local.ChatMessageEntity
import com.jejecomms.realtimechatfeature.data.local.ChatRoomMemberEntity
import com.jejecomms.realtimechatfeature.data.model.MessageStatus
import com.jejecomms.realtimechatfeature.data.model.MessageType
import com.jejecomms.realtimechatfeature.data.repository.ChatRoomRepository
import com.jejecomms.realtimechatfeature.utils.Constants.CACHE_FOLDER_EXPORT_CHAT
import com.jejecomms.realtimechatfeature.utils.Constants.CACHE_FOLDER_MAIN
import com.jejecomms.realtimechatfeature.utils.Constants.KEY_SENDER_ID
import com.jejecomms.realtimechatfeature.utils.Constants.SENDER_NAME
import com.jejecomms.realtimechatfeature.utils.Constants.SENDER_NAME_PREF
import com.jejecomms.realtimechatfeature.utils.Constants.USER_JOINED_THE_CHAT_ROOM
import com.jejecomms.realtimechatfeature.utils.Constants.YOU_HAVE_JOINED_THE_CHAT_ROOM
import com.jejecomms.realtimechatfeature.utils.DateUtils
import com.jejecomms.realtimechatfeature.utils.DateUtils.getTimestamp
import com.jejecomms.realtimechatfeature.utils.SharedPreferencesUtil
import com.jejecomms.realtimechatfeature.utils.UuidGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * ViewModel for the chat room screen. It interacts with the [ChatRoomRepository] to send messages
 * and manages the UI state related to messages.
 */
class ChatRoomViewModel(
    private val chatRoomRepository: ChatRoomRepository,
    application: Application,
    private val roomId: String,
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
    private val _isRoomExists = MutableStateFlow<Boolean?>(null)

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
     * Update a message's as a read status.
     */
    fun markMessageAsRead(message: ChatMessageEntity, userId: String) {
        viewModelScope.launch {
            chatRoomRepository.markMessageAsRead(message.roomId, message.id, userId)
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
                    remoteMessages.forEach { remoteMessage ->
                        // Check if the message is from another user and is in the 'SENT' state.
                        if (remoteMessage.senderId != currentSenderId
                            && remoteMessage.status == MessageStatus.SENT
                        ) {
                            // Mark the message as delivered.
                            // This should only update the status, not replace the entire message.
                            chatRoomRepository
                                .markMessageAsDelivered(remoteMessage.roomId, remoteMessage.id)
                        }

                        // A new function is needed here to either insert a new message
                        // or update an existing one without overwriting a more advanced status.
                        // This function should be implemented repository and Dao.
                        chatRoomRepository.upsertMessage(remoteMessage)
                    }
                }
            } catch (_: Exception) {
            }
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
                senderName = senderName ?: SENDER_NAME,
                text = text,
                timestamp = DateUtils.getTimestamp(),
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
    fun sendImageMessage(
        currentSenderId: String, roomId: String, imageUri: Uri,
        context: Context,
        messageType: MessageType,
    ) {
        viewModelScope.launch {
            val messageId = UuidGenerator.generateUniqueId()
            //Save the image to a local cache and get its path
            val localImagePath = chatRoomRepository.saveFileToCache(
                imageUri, messageId, context, messageType
            )
            // Create a temporary message entity for the image
            val imageMessage = ChatMessageEntity(
                id = messageId,
                roomId = roomId,
                senderId = currentSenderId,
                senderName = senderName ?: SENDER_NAME,
                text = "",
                url = localImagePath,// Handle only offline will logic change for online.
                timestamp = DateUtils.getTimestamp(),
                status = MessageStatus.SENDING,
                messageType = messageType
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
            timestamp = DateUtils.getTimestamp(),
            isGroupMember = false,
            roomId = roomId
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

    /**
     * Exports the chat history to a JSON file and shares it.
     */
    fun exportChat(roomId: String, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val messages = chatRoomRepository.getLocalMessages(roomId).first()
            val jsonString = convertMessagesToJson(messages)

            // Create the directory for exported chats
            val exportDir = File(context.cacheDir, "$CACHE_FOLDER_MAIN/$CACHE_FOLDER_EXPORT_CHAT")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }

            // Write the JSON to a file
            val fileName = "chat_${roomId}_"+getTimestamp()+".json"
            val file = File(exportDir, fileName)
            file.writeText(jsonString)

            // Share the file
            shareFile(context, file)

            // Show a success message on the main thread
            withContext(Dispatchers.Main) {
                Toast.makeText(context, context.getString(R.string.chat_exported_successfully)
                    ,Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Converts a list of messages to a JSON string.
     */
    private fun convertMessagesToJson(messages: List<ChatMessageEntity>): String {
        val gson = com.google.gson.GsonBuilder().setPrettyPrinting().create()
        return gson.toJson(messages)
    }

    /**
     * Shares a file using an Intent.
     */
    private fun shareFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooserIntent = Intent.createChooser(shareIntent, "Export Chat")
        context.startActivity(chooserIntent)
    }
}