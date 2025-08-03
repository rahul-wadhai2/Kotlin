package com.jejecomms.realtimechatfeature.ui.chatroomlist

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.jejecomms.realtimechatfeature.ChatApplication
import com.jejecomms.realtimechatfeature.R
import com.jejecomms.realtimechatfeature.data.local.ChatRoomEntity
import com.jejecomms.realtimechatfeature.data.repository.ChatRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the chat room list screen.
 */
class ChatRoomListViewModel(private val chatRepository: ChatRepository,
application: ChatApplication) : AndroidViewModel(application) {
    /***
     * State flow for the list of chat rooms.
     */
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

    /**
     * Job for the local data rooms collector.
     */
    private var localDataRoomsCollectorJob: Job? = null

    init {
        initializeChatRooms()
    }

    /**
     * Initializes the chat rooms by starting to listen for rooms.
     * This should be called whenever the user navigates to a chat rooms screen.
     */
    fun initializeChatRooms() {
        // Cancel any previous jobs to prevent conflicts
        firestoreRoomsListenerJob?.cancel()
        localDataRoomsCollectorJob?.cancel()

        // Launch a separate coroutine to listen to Firestore changes and update the local DB.
        // This is a long-running job that should be active as long as the screen is.
        firestoreRoomsListenerJob = viewModelScope.launch {
            try {
                chatRepository.startFirestoreChatRoomsListener().collect { remoteRooms ->
                    chatRepository.insertRooms(remoteRooms)
                }
            } catch (_: Exception) { }
        }

        localDataRoomsCollectorJob = viewModelScope.launch {
            chatRepository.getAllChatRooms().collect { chatRoomList ->
                _chatRooms.value = chatRoomList
            }
        }
    }

    /**
     * Delete a chat room.
     */
    fun onDeleteChatRoom(roomId: String) {
        viewModelScope.launch {
            chatRepository.deleteChatRoom(roomId)
        }
    }

    /**
     * Fetch chat rooms with their unread message count.
     */
//    private fun fetchChatRoomsUnreadMessageCount() {
//        viewModelScope.launch {
//            chatRepository.getAllChatRoomsWithUnreadCount().collect {
//                _chatRooms.value = it
//            }
//        }
//    }

    /**
     * Collect chat rooms from the local database.
     */
    private fun getAllChatRooms() {
        viewModelScope.launch {
            chatRepository.getAllChatRooms().collect { chatRoomList ->
                _chatRooms.value = chatRoomList
            }
        }
    }

    /**
     * Creates a new chat room and adds the creating user as a member.
     * Updates the UI state with an error message if the creation fails.
     */
    fun createChatRoom(groupName: String, userName: String, currentUserId: String) {
        viewModelScope.launch {
            val isSuccess = chatRepository.createChatRoom(groupName, userName, currentUserId)
            if (!isSuccess) {
                _createGroupError.value = application
                    .getString(R.string.create_group_error_already_exit)
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
}