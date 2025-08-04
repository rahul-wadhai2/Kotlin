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
     * Job for the Firestore listener for rooms.
     */
    private var firestoreRoomsListenerJob: Job? = null

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
                .collect { chatRoomList ->
                    _chatRooms.value = chatRoomList
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
            val isSuccess = chatRoomsRepository.createChatRoom(groupName, userName, currentUserId)
            if (!isSuccess) {
                _createGroupError.value =
                    application.getString(R.string.create_group_error_already_exit)
            } else {
                _createGroupError.value = null
            }
        }
    }

    /**
     * Clears the create group error message.
     */
    fun clearCreateGroupError() {
        _createGroupError.value = null
    }

    override fun onCleared() {
        super.onCleared()
        firestoreRoomsListenerJob?.cancel()
    }
}