package com.jejecomms.realtimechatfeature.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data access object (DAO) for the ChatMessageEntity.
 */
@Dao
interface MessageDao {
    /**
     * Retrieves all messages from the database in ascending order by timestamp.
     */
    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    fun getMessages(): Flow<List<ChatMessageEntity>>

    /**
     * Inserts a new message into the database.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    /**
     * Inserts a list of messages into the database.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<ChatMessageEntity>)

    /**
     * Updates the status of a message in the database.
     */
    @Update
    suspend fun updateMessage(message: ChatMessageEntity)

    /**
     * Retrieves messages by their status from the database.
     */
    @Query("SELECT * FROM messages WHERE status = :status ORDER BY timestamp ASC")
    suspend fun getMessagesByStatus(status: String): List<ChatMessageEntity>

    /**
     * Inserts a new message into the database of group members.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGroupMember(message: GroupMembersEntity)

    // The function you requested
    @Update
    suspend fun updateGroupMember(groupMember: GroupMembersEntity)
}