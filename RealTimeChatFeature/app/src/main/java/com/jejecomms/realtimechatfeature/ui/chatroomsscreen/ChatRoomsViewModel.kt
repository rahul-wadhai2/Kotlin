package com.jejecomms.realtimechatfeature.ui.chatroomsscreen

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jejecomms.realtimechatfeature.ChatApplication
import com.jejecomms.realtimechatfeature.R
import com.jejecomms.realtimechatfeature.data.local.ChatRoomEntity
import com.jejecomms.realtimechatfeature.data.repository.ChatRoomsRepository
import com.jejecomms.realtimechatfeature.utils.Constants.KEY_SENDER_ID
import com.jejecomms.realtimechatfeature.utils.SharedPreferencesUtil
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * ViewModel for the chat rooms screen.
 */
class ChatRoomsViewModel(
    private val chatRoomsRepository: ChatRoomsRepository,
    private val application: ChatApplication,
) : AndroidViewModel(application) {

    private val _chatRooms = MutableStateFlow<List<ChatRoomEntity>>(emptyList())

    /**
     * Exposes the list of chat rooms as a StateFlow.
     */
    val chatRooms: StateFlow<List<ChatRoomEntity>> = _chatRooms.asStateFlow()

    /**
     * State flow for the error message when creating a group.
     */
    private val _createGroupError = MutableStateFlow<String?>(null)

    /**
     * Exposes the error message as a StateFlow.
     */
    val createGroupError: StateFlow<String?> = _createGroupError.asStateFlow()

    /**
     * State flow to track the success of group creation.
     */
    private val _groupCreationSuccessful = MutableStateFlow(false)

    /**
     * Exposes the success state as a StateFlow.
     */
    val groupCreationSuccessful: StateFlow<Boolean> = _groupCreationSuccessful.asStateFlow()

    /**
     * Job for the Firestore listener for rooms.
     */
    private var firestoreRoomsListenerJob: Job? = null

    // StateFlow to hold the ChatRoomEntity from a deep link
    private val _deepLinkChatRoom = MutableStateFlow<ChatRoomEntity?>(null)
    val deepLinkChatRoom: StateFlow<ChatRoomEntity?> = _deepLinkChatRoom

    init {
        // Start the Firestore listener and collect the unread count flow
        startRoomDataSync()
    }

    /**
     * Initializes the chat rooms and starts the data collection
     * from Firestore and the local database.
     */
    private fun startRoomDataSync() {
        // Cancel any previous jobs
        firestoreRoomsListenerJob?.cancel()

        // Launch a coroutine to listen to Firestore changes and update the local DB.
        firestoreRoomsListenerJob = viewModelScope.launch {
            try {
                chatRoomsRepository.startFirestoreChatRoomsListener().collect { remoteRooms ->
                    chatRoomsRepository.insertRooms(remoteRooms)
                }
            } catch (_: Exception) {
            }
        }

        // Launch a coroutine to collect the CORRECT flow with unread count
        viewModelScope.launch {
            val currentSenderId = SharedPreferencesUtil.getString(KEY_SENDER_ID)
            chatRoomsRepository.getAllChatRoomsWithUnreadCount(currentSenderId.toString())
                .collect { chatRooms ->
                    // For each chat room, fetch the last message
                    val updatedChatRooms = chatRooms.map { chatRoom ->
                        val lastMessage = chatRoomsRepository
                            .getLastMessageForRoom(chatRoom.roomId.toString())
                        chatRoom.copy(lastMessage = lastMessage)
                    }
                    _chatRooms.value = updatedChatRooms
                }
        }
    }

    /**
     * Delete a chat room.
     */
    fun onDeleteChatRoom(roomId: String) {
        viewModelScope.launch {
            chatRoomsRepository.deleteChatRoom(roomId)
        }
    }

    /**
     * Creates a new chat room and adds the creating user as a member.
     * Updates the UI state with an error message if the creation fails.
     */
    fun createChatRoom(groupName: String, userName: String, currentUserId: String) {
        viewModelScope.launch {
            try {
                val isSuccess = chatRoomsRepository.createChatRoom(groupName, userName, currentUserId)
                if (isSuccess) {
                    _groupCreationSuccessful.value = true
                    _createGroupError.value = null
                } else {
                    _createGroupError.value =
                        application.getString(R.string.create_group_error_already_exit)
                    _groupCreationSuccessful.value = false
                }
            } catch (e: Exception) {
                _createGroupError.value = e.message ?: application.getString(R.string.unknown_error)
                _groupCreationSuccessful.value = false
            }
        }
    }

    /**
     * Clears the create group status after a successful message.
     */
    fun resetGroupCreationStatus() {
        _groupCreationSuccessful.value = false
        _createGroupError.value = null
    }

    // Function to handle the initial deep link
    fun handleInitialDeepLink(roomId: String?) {
        if (roomId != null) {
            viewModelScope.launch {
                // Wait for the chatRooms flow to emit its first non-empty list
                chatRooms.first { it.isNotEmpty() }

                // Now that the data is loaded, find the room and set the state
                val room = chatRooms.value.find { it.roomId.toString() == roomId }
                if (room != null) {
                    _deepLinkChatRoom.value = room
                }
            }
        }
    }

    // Function to clear the deep link state after navigation
    fun clearDeepLinkChatRoom() {
        _deepLinkChatRoom.value = null
    }

    override fun onCleared() {
        super.onCleared()
        firestoreRoomsListenerJob?.cancel()
    }
}