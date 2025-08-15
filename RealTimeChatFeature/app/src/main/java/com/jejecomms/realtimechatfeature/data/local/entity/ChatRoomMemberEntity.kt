package com.jejecomms.realtimechatfeature.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jejecomms.realtimechatfeature.utils.Constants
import com.jejecomms.realtimechatfeature.utils.UuidGenerator

/**
 *  Chat message data class.
 *
 *  @param id Unique ID for the message, defaults to a new UUID.
 *  @param userId ID of the user who sent the message.
 *  @param userName Display name of the sender.
 *  @param roomId ID of the chat room the message belongs to.
 *  @param role Role of the sender in the chat room.
 *  @param isMuted Flag indicating if the message is muted.
 *  @param joinedAt Timestamp when the sender joined the chat room.
 */
@Entity(tableName = Constants.CHAT_ROOM_MEMBERS)
data class ChatRoomMemberEntity(
    @PrimaryKey
    val id: String = UuidGenerator.generateUniqueId(),
    val userId: String = "",
    val userName: String = "",
    val roomId: String = "",
    val role: String = "",
    val isMuted: Boolean = false,
    val joinedAt: Long = 0L,
)