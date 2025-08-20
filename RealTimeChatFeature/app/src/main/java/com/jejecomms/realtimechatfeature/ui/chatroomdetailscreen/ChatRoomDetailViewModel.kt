package com.jejecomms.realtimechatfeature.ui.chatroomdetailscreen

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jejecomms.realtimechatfeature.ChatApplication
import com.jejecomms.realtimechatfeature.R
import com.jejecomms.realtimechatfeature.data.local.entity.ChatRoomMemberEntity
import com.jejecomms.realtimechatfeature.data.local.entity.UsersEntity
import com.jejecomms.realtimechatfeature.data.repository.ChatRoomDetailRepository
import com.jejecomms.realtimechatfeature.data.repository.ChatRoomsRepository
import com.jejecomms.realtimechatfeature.data.repository.LoginRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 *  ViewModel for the ChatRoomDetailScreen.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChatRoomDetailViewModel(
    private val loginRepository: LoginRepository,
    private val chatRoomsRepository: ChatRoomsRepository,
    private val chatRoomDetailRepository: ChatRoomDetailRepository,
    private val application: ChatApplication,
) : AndroidViewModel(application) {

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

    /**
     * StateFlow to hold the list of all users.
     */
    private val _allUsers = MutableStateFlow<List<UsersEntity>>(emptyList())

    /**
     * Public StateFlow to expose the list of all users.
     */
    val allUsers: StateFlow<List<UsersEntity>> = _allUsers.asStateFlow()

    /**
     * StateFlow to control the loading state.
     */
    private val _isLoading = MutableStateFlow(false)

    /**
     * Public StateFlow to expose the loading state.
     */
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * SharedFlow to send one-time success messages.
     */
    private val _successMessage = MutableStateFlow<String?>(null)

    /**
     * Public SharedFlow to expose one-time success messages.
     */
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    /**
     * SharedFlow to handle navigation back to the ChatRoomsScreen.
     */
    private val _navigateToChatRooms = MutableStateFlow(false)

    /**
     * Public SharedFlow to expose navigation back to the ChatRoomsScreen.
     */
    val navigateToChatRooms: StateFlow<Boolean> = _navigateToChatRooms.asStateFlow()

    /**
     * StateFlow to hold the current roomId.
     */
    private val _currentRoomId = MutableStateFlow<String?>(null)

    /**
     * Public StateFlow to expose the list of members,
     * directly collecting from the local database.
     */
    val members: StateFlow<List<ChatRoomMemberEntity>> = _currentRoomId
        .filterNotNull() // Ensure a roomId is present before proceeding
        .flatMapLatest { roomId ->
            // This flow will automatically emit new lists whenever the local database changes.
            // The UI will now be updated for offline additions.
            chatRoomDetailRepository.getMembers(roomId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Loads the chat room and starts data collection.
     * This is the single entry point for setting the roomId.
     */
    fun loadChatRoom(newRoomId: String) {
        // Only load if the room ID has actually changed to avoid redundant work
        if (_currentRoomId.value != newRoomId) {
            _currentRoomId.value = newRoomId
            startMemberSync()
        }
    }

    /**
     * Refreshes the list of members by re-syncing from Firestore.
     */
    private fun startMemberSync() {
        _currentRoomId.value.let { id ->
            chatRoomDetailRepository.startMemberSync(id.toString())
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
            _currentRoomId.value?.let { roomId ->
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
                    _currentRoomId.value?.let { id ->
                        chatRoomsRepository.deleteChatRoom(id)
                        _navigateToChatRooms.emit(true)
                    }
                } else {
                    // The user is not the last member.
                    // Proceed with the standard leave room logic.
                    chatRoomDetailRepository.leaveRoom(currentUser, members.value)
                }
            } else {
                // The current user is the last member in the group.
                // Delete the group locally and then sync to Firebase.
                _currentRoomId.value?.let { id ->
                    chatRoomsRepository.deleteChatRoom(id)
                    _navigateToChatRooms.emit(true)
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

    /**
     * Load all users from the repository.
     */
    fun loadAllUsers(senderId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            loginRepository.getAllUsers(senderId).collect { users ->
                _allUsers.value = users
                _isLoading.value = false
            }
        }
    }

    /**
     * Add selected members to the room.
     */
    fun addMembersToRoom(newMembers: List<UsersEntity>) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _currentRoomId.value?.let { id ->
                    val isMemberAdded = chatRoomDetailRepository.addMembers(id, newMembers)
                    if (isMemberAdded) {
                        _successMessage.value = application
                            .getString(R.string.members_added_successfully)
                    } else {
                        _successMessage.value = application
                            .getString(R.string.error_adding_members)
                    }
                }
            } catch (e: Exception) {
                _successMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Clears the success message after it has been displayed.
     */
    fun clearSuccessMessage() {
        viewModelScope.launch {
            _successMessage.value = null
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Clean up the listener when the ViewModel is destroyed
        chatRoomDetailRepository.cleanup()
    }
}