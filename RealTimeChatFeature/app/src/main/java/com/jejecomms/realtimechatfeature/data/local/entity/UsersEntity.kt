package com.jejecomms.realtimechatfeature.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jejecomms.realtimechatfeature.utils.Constants

/**
 * Represents a user in the chat.
 *
 * @param uid The unique identifier for the user Auth provider.
 * @param username The username of the user.
 * @param email The email address of the user.
 * @param password The password of the user.
 * @param loginTime The timestamp when the user logged in.
 */
@Entity(tableName = Constants.USERS)
data class UsersEntity(
    @PrimaryKey
    val uid: String = "",
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val loginTime: Long = 0L
)