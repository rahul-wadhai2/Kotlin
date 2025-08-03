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
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertChatRoom(chatRoom: ChatRoomEntity)

    /**
     *  Inserts a list of chat rooms into the database.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatRooms(chatRooms: List<ChatRoomEntity>)

    /**
     * This is the new method to get all chat rooms from the database.
     */
    @Query("SELECT * FROM $CHAT_ROOM WHERE isArchived = 0 ORDER BY lastTimestamp DESC")
    fun getAllChatRooms(): Flow<List<ChatRoomEntity>>

//    /**
//     * Query to get all chat rooms with their unread count.
//     */
//    @Query("""
//        SELECT
//            r.*,
//            (SELECT COUNT(m.id) FROM messages m WHERE m.roomId = r.roomId AND m.timestamp > r.lastReadTimestamp) AS unreadCount
//        FROM chat_room r
//        WHERE r.isArchived = 0
//        ORDER BY r.lastTimestamp DESC
//    """)
//    fun getAllChatRoomsWithUnreadCount(): Flow<List<ChatRoomEntity>>
//
//    /**
//     * Query to count unread messages based on lastReadTimestamp.
//     */
//    @Query("SELECT COUNT(*) FROM $MESSAGES WHERE roomId = :roomId AND timestamp > :lastReadTimestamp")
//    fun getUnreadMessageCount(roomId: String, lastReadTimestamp: Long): Flow<Int>
//
//    /**
//     * Query to update the last read timestamp for a specific chat room.
//     */
//    @Query("UPDATE $CHAT_ROOM SET lastReadTimestamp = :timestamp WHERE roomId = :roomId")
//    suspend fun updateLastReadTimestamp(roomId: String, timestamp: Long)
}