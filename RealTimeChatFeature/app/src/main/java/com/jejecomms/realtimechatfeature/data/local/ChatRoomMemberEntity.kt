package com.jejecomms.realtimechatfeature.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jejecomms.realtimechatfeature.utils.Constants.CHAT_ROOM_MEMBERS
import com.jejecomms.realtimechatfeature.utils.DateUtils
import com.jejecomms.realtimechatfeature.utils.UuidGenerator

/**
 *  Chat message data class.
 *
 *  @param id Unique ID for the message, defaults to a new UUID.
 *  @param senderId ID of the user who sent the message.
 *  @param senderName Display name of the sender.
 *  @param text Content of the message.
 *  @param timestamp Timestamp when the message was created.
 *  @param isGroupMember Flag indicating if the message is a system message.
 */
@Entity(tableName = CHAT_ROOM_MEMBERS)
data class ChatRoomMemberEntity(
    @PrimaryKey
    val id: String = UuidGenerator.generateUniqueId(),
    val senderId: String = "",
    val senderName: String = "",
    val timestamp: Long = DateUtils.getTimestamp(),
    val isGroupMember: Boolean = false,
    val roomId: String ="",
)