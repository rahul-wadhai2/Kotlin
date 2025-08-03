package com.jejecomms.realtimechatfeature.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jejecomms.realtimechatfeature.utils.Constants.CHAT_ROOM
import com.jejecomms.realtimechatfeature.utils.UuidGenerator

/**
 * Data class representing a chat room.
 *
 * @param roomId Unique identifier for the chat room.
 * @param lastMessage The last message sent in the chat room.
 * @param lastTimestamp Timestamp of the last message in the chat room.
 * @param unreadCount Number of unread messages in the chat room.
 * @param isMuted Flag indicating if the chat room is muted.
 * @param isArchived Flag indicating if the chat room is archived.
 */
@Entity(tableName = CHAT_ROOM)
data class ChatRoomEntity(
    @PrimaryKey
    val id: String = UuidGenerator.generateUniqueId(),
    val roomId: String? = null,
    val lastMessage: String? = null,
    val lastTimestamp: Long = 0L,
    val unreadCount: Int = 0,
    val isMuted: Boolean = false,
    val isArchived: Boolean = false,
    val groupName: String? = null,
    val lastReadTimestamp: Long = 0L,
    val userName: String? = null,
    val isDeletedLocally: Boolean = false
)