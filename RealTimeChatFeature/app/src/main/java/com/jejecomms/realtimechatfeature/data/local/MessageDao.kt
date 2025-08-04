package com.jejecomms.realtimechatfeature.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.jejecomms.realtimechatfeature.utils.Constants.CHAT_ROOM
import com.jejecomms.realtimechatfeature.utils.Constants.MESSAGES
import kotlinx.coroutines.flow.Flow

/**
 * Data access object (DAO) for the ChatMessageEntity.
 */
@Dao
interface MessageDao {
    /**
     * Retrieves all messages from the database in ascending order by timestamp.
     */
    @Query("SELECT * FROM $MESSAGES WHERE roomId = :roomId ORDER BY timestamp ASC")
    fun getMessages(roomId: String): Flow<List<ChatMessageEntity>>

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
    @Query("SELECT * FROM $MESSAGES WHERE status = :status ORDER BY timestamp ASC")
    suspend fun getMessagesByStatus(status: String): List<ChatMessageEntity>

    /**
     * Inserts a new message into the database of group members.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGroupMember(message: ChatRoomMemberEntity)

    /**
     * Updates a group member in the database.
     */
    @Update
    suspend fun updateGroupMember(groupMember: ChatRoomMemberEntity)

    /**
     * Inserts a new chat room into the database.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatRoom(chatRoom: ChatRoomEntity)

    /**
     *  Inserts a list of chat rooms into the database.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatRooms(chatRooms: List<ChatRoomEntity>)

    /**
     * This is the new method to get all chat rooms from the database.
     */
    @Query("SELECT * FROM $CHAT_ROOM WHERE isArchived = 0 AND isDeletedLocally = 0 ORDER BY lastTimestamp DESC")
    fun getAllChatRooms(): Flow<List<ChatRoomEntity>>

    /**
     * Query to perform a soft delete on a chat room.
     */
    @Query("UPDATE $CHAT_ROOM SET isDeletedLocally = 1 WHERE roomId = :roomId")
    suspend fun markChatRoomAsLocallyDeleted(roomId: String)

    /**
     * Query to find rooms that are locally deleted but not yet synced with Firestore.
     */
    @Query("SELECT * FROM $CHAT_ROOM WHERE isDeletedLocally = 1")
    fun getLocallyDeletedChatRooms(): Flow<List<ChatRoomEntity>>

    /**
     * Method to permanently delete a room from the local database.
     */
    @Query("DELETE FROM $CHAT_ROOM WHERE roomId = :roomId")
    suspend fun deleteChatRoom(roomId: String)

    /**
     * Get a flow of all chat rooms from the local Room database, including a count
     * of unread messages for each room.
     */
    @Query("""
        SELECT 
            T1.*,
            SUM(CASE WHEN T2.senderId != :currentUserId AND T2.timestamp > T1.lastReadTimestamp THEN 1 ELSE 0 END) AS unreadCount
        FROM $CHAT_ROOM AS T1
        LEFT JOIN $MESSAGES AS T2 ON T1.roomId = T2.roomId
        WHERE T1.isDeletedLocally = 0
        GROUP BY T1.roomId
        ORDER BY T1.lastTimestamp DESC
    """)
    fun getAllChatRoomsWithUnreadCount(currentUserId: String): Flow<List<ChatRoomEntity>>

    /**
     * Query to count unread messages based on lastReadTimestamp.
     */
    @Query("SELECT COUNT(*) FROM $MESSAGES WHERE roomId = :roomId AND timestamp > :lastReadTimestamp")
    fun getUnreadMessageCount(roomId: String, lastReadTimestamp: Long): Flow<Int>

    /**
     * Updates the last read timestamp for a specific chat room.
     * This is crucial for resetting the unread message count.
     */
    @Query("UPDATE $CHAT_ROOM SET lastReadTimestamp = :timestamp WHERE roomId = :roomId")
    suspend fun updateLastReadTimestamp(roomId: String, timestamp: Long)

    /**
     * Get a single chat room by its ID.
     */
    @Query("SELECT * FROM $CHAT_ROOM WHERE roomId = :roomId LIMIT 1")
    suspend fun getChatRoomById(roomId: String): ChatRoomEntity?

    /**
     * Updates the last read timestamp for ALL chat rooms to the current time.
     * This is a one-time operation to handle the initial app launch.
     */
    @Query("UPDATE $CHAT_ROOM SET lastReadTimestamp = :timestamp")
    suspend fun updateAllLastReadTimestamps(timestamp: Long)

    /**
     * Gets the minimum lastReadTimestamp of all chat rooms.
     */
    @Query("SELECT MIN(lastReadTimestamp) FROM $CHAT_ROOM")
    suspend fun getMinLastReadTimestamp(): Long?
}