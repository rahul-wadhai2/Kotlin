package com.jejecomms.realtimechatfeature.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jejecomms.realtimechatfeature.utils.Constants
import com.jejecomms.realtimechatfeature.utils.DateUtils

/**
 * Represents a message in the chat.
 *
 * @param id The unique identifier for the message.
 * @param messageId The unique identifier for the message.
 * @param userId The unique identifier for the user who sent the message.
 * @param timestamp The timestamp when the message was sent.
 * @param roomId The unique identifier for the room the message belongs to.
 */
@Entity(tableName = Constants.FAILED_READ_RECEIPTS)
data class ReadReceiptEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val messageId: String,
    val userId: String,
    val timestamp: Long = DateUtils.getTimestamp(),
    val roomId: String
)