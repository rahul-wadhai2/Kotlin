package com.jejecomms.realtimechatfeature.ui.chatroomdetailscreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jejecomms.realtimechatfeature.data.local.entity.ChatRoomMemberEntity
import com.jejecomms.realtimechatfeature.data.repository.ChatRoomDetailRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 *  ViewModel for the ChatRoomDetailScreen.
 */
class ChatRoomDetailViewModel(
    chatRoomDetailRepository: ChatRoomDetailRepository,
    roomId: String?
) : ViewModel() {

    /**
     * StateFlow to hold the list of members.
     */
    private val _members = MutableStateFlow<List<ChatRoomMemberEntity>>(emptyList())

    /**
     * Public StateFlow to expose the list of members.
     */
    val members: StateFlow<List<ChatRoomMemberEntity>> = _members.asStateFlow()

    init {
        viewModelScope.launch {
            chatRoomDetailRepository.syncMembers(roomId.toString())
            chatRoomDetailRepository.getMembers(roomId.toString()).collect { memberList ->
                _members.value = memberList
            }
        }
    }
}