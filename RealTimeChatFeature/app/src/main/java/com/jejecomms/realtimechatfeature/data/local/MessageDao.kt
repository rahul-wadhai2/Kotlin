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
     * Retrieves a single message from the database using its client-generated ID.
     * This is crucial for checking if a message already exists before updating it.
     *
     * @param clientGeneratedId The unique ID generated on the client side.
     * @return The ChatMessageEntity if found, or null otherwise.
     */
    @Query("SELECT * FROM messages WHERE clientGeneratedId = :clientGeneratedId")
    suspend fun getMessageByClientGeneratedId(clientGeneratedId: String): ChatMessageEntity?
}