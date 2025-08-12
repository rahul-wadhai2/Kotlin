package com.jejecomms.realtimechatfeature.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jejecomms.realtimechatfeature.utils.Constants.FAILED_READ_RECEIPTS
import com.jejecomms.realtimechatfeature.utils.DateUtils

/**
 * Represents a message in the chat.
 */
@Entity(tableName = FAILED_READ_RECEIPTS)
data class ReadReceiptEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val messageId: String,
    val userId: String,
    val timestamp: Long = DateUtils.getTimestamp(),
    val roomId: String
)