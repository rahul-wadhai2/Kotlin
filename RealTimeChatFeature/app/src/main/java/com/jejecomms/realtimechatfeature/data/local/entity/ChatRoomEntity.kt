package com.jejecomms.realtimechatfeature.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jejecomms.realtimechatfeature.utils.Constants.CHAT_ROOMS
import com.jejecomms.realtimechatfeature.utils.UuidGenerator

/**
 * Data class representing a chat room.
 *
 * @param id Unique identifier for the chat room.
 * @param roomId Unique identifier for the chat room.
 * @param lastMessage The last message sent in the chat room.
 * @param lastTimestamp Timestamp of the last message in the chat room.
 * @param unreadCount Number of unread messages in the chat room.
 * @param isMuted Flag indicating if the chat room is muted.
 * @param isArchived Flag indicating if the chat room is archived.
 * @param createdBy The name of the chat room.
 * @param lastReadTimestamp Timestamp of the last read message in the chat room.
 * @param isDeletedLocally Flag indicating if the chat room has been deleted locally.
 * @param type Type of the chat room.
 * @param createdAt Timestamp of when the chat room was created.
 * @param title Title of the chat room.
 */
@Entity(tableName = CHAT_ROOMS)
data class ChatRoomEntity(
    @PrimaryKey
    val id: String = UuidGenerator.generateUniqueId(),
    val roomId: String? = null,
    val lastMessage: String? = "",
    val lastTimestamp: Long = 0L,
    val unreadCount: Int = 0,
    val isMuted: Boolean = false,
    val isArchived: Boolean = false,
    val createdBy: String? = null,
    val lastReadTimestamp: Long = 0L,
    val isDeletedLocally: Boolean = false,
    val type: String? = null,
    val createdAt: Long = 0L,
    val title: String? = null,
    val isPendingChatRoom: Boolean = false
)