package com.jejecomms.realtimechatfeature.ui.chatroomdetailscreen

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jejecomms.realtimechatfeature.ChatApplication
import com.jejecomms.realtimechatfeature.R
import com.jejecomms.realtimechatfeature.data.local.entity.ChatRoomMemberEntity
import com.jejecomms.realtimechatfeature.data.repository.ChatRoomDetailRepository
import com.jejecomms.realtimechatfeature.data.repository.ChatRoomsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 *  ViewModel for the ChatRoomDetailScreen.
 */
class ChatRoomDetailViewModel(
    private val chatRoomsRepository: ChatRoomsRepository,
    private val chatRoomDetailRepository: ChatRoomDetailRepository,
    private val roomId: String?,
    private val application: ChatApplication,
) : AndroidViewModel(application) {

    /**
     * StateFlow to hold the list of members.
     */
    private val _members = MutableStateFlow<List<ChatRoomMemberEntity>>(emptyList())

    /**
     * Public StateFlow to expose the list of members.
     */
    val members: StateFlow<List<ChatRoomMemberEntity>> = _members.asStateFlow()

    /**
     * StateFlow to hold the message for ownership transfer.
     */
    private val _transferOwnershipMessage = MutableStateFlow<String?>(null)

    /**
     * Public StateFlow to expose the ownership transfer message.
     */
    val transferOwnershipMessage: StateFlow<String?> = _transferOwnershipMessage.asStateFlow()

    /**
     * StateFlow to control the visibility of the leave dialog.
     */
    private val _showLeaveDialog = MutableStateFlow(false)

    /**
     * Public StateFlow to expose the visibility of the leave dialog.
     */
    val showLeaveDialog: StateFlow<Boolean> = _showLeaveDialog.asStateFlow()

    init {
        startMemberSync()
        // This will update the UI reactively whenever the local database changes.
        viewModelScope.launch {
            roomId?.let { id ->
                chatRoomDetailRepository.getMembers(id).collect { memberList ->
                    _members.value = memberList
                }
            }
        }
    }

    /**
     * Refreshes the list of members by re-syncing from Firestore.
     */
    fun startMemberSync() {
        roomId?.let { id ->
            chatRoomDetailRepository.startMemberSync(id)
        }
    }

    /**
     * Removes a member from the chat room.
     * The repository handles the local and remote database operations.
     *
     * @param member The ChatRoomMemberEntity to remove.
     */
    fun removeMember(member: ChatRoomMemberEntity) {
        viewModelScope.launch {
            // The ViewModel's responsibility is to trigger the action on the repository.
            // The repository's implementation handles the rest (local update, Firebase sync).
            chatRoomDetailRepository.removeMember(member)
        }
    }

    /**
     * Initiates the process of making a new member an admin.
     *
     * @param newOwner The member who will become the new admin.
     */
    fun transferOwnership(newOwner: ChatRoomMemberEntity) {
        viewModelScope.launch {
            roomId?.let { roomId ->
                chatRoomDetailRepository.transferOwnership(roomId, newOwner)
                _transferOwnershipMessage.value = application
                    .getString(R.string.ownership_transfer_message, newOwner.userName)
            } ?: run {
                _transferOwnershipMessage.value = application
                    .getString(R.string.error_transferring_ownership)
            }
        }
    }

    /**
     * Opens the dialog to confirm leaving the chat room.
     */
    fun onConfirmLeave(currentUserId: String) {
        viewModelScope.launch {
            _showLeaveDialog.value = false
            val currentUser = members.value.find { it.userId == currentUserId }
            if (currentUser != null) {
                val otherMembers = members.value.filter { it.userId != currentUserId }
                if (otherMembers.isEmpty()) {
                    // The current user is the last member in the group.
                    // Delete the group locally and then sync to Firebase.
                    roomId?.let { id ->
                        chatRoomsRepository.deleteChatRoom(roomId)
                    }
                } else {
                    // The user is not the last member.
                    // Proceed with the standard leave room logic.
                    chatRoomDetailRepository.leaveRoom(currentUser, members.value)
                }
            } else {
                // The current user is the last member in the group.
                // Delete the group locally and then sync to Firebase.
                roomId?.let { id ->
                    chatRoomsRepository.deleteChatRoom(roomId)
                }
            }
        }
    }

    /**
     * Closes the dialog to confirm leaving the chat room.
     */
    fun onDismissLeaveDialog() {
        _showLeaveDialog.value = false
    }

    /**
     * Opens the dialog to confirm leaving the chat room.
     */
    fun onLeaveRoomClicked() {
        _showLeaveDialog.value = true
    }

    /**
     * Clears the ownership transfer message after it has been displayed.
     */
    fun clearTransferOwnershipMessage() {
        _transferOwnershipMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        // Clean up the listener when the ViewModel is destroyed
        chatRoomDetailRepository.cleanup()
    }
}