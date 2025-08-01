package com.jejecomms.realtimechatfeature.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jejecomms.realtimechatfeature.data.model.MessageStatus
import com.jejecomms.realtimechatfeature.utils.UuidGenerator

/**
 *  Chat message data class.
 *
 *  @param id Unique ID for the message, defaults to a new UUID.
 *  @param clientGeneratedId Client-generated ID for retry safety, defaults to a new UUID.
 *  @param senderId ID of the user who sent the message.
 *  @param senderName Display name of the sender.
 *  @param text Content of the message.
 *  @param timestamp Timestamp when the message was created.
 *  @param status Current status of the message.
 *  @param isSystemMessage Flag indicating if the message is a system message.
 */
@Entity(tableName = "messages")
data class ChatMessageEntity(
    @PrimaryKey
    val id: String = UuidGenerator.generateUniqueId(),
    val clientGeneratedId: String = UuidGenerator.generateUniqueId(),
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val status: MessageStatus = MessageStatus.SENDING,
    val isSystemMessage: Boolean = false
)