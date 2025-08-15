package com.jejecomms.realtimechatfeature.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jejecomms.realtimechatfeature.data.local.entity.ChatRoomMemberEntity
import com.jejecomms.realtimechatfeature.utils.Constants.CHAT_ROOM_MEMBERS
import kotlinx.coroutines.flow.Flow

/**
 * Data access object (DAO) for the RoomDetails
 */
@Dao
interface ChatRoomDetailDao {

    /**
     * Retrieves members by their roomId from the database.
     */
    @Query("SELECT * FROM ${CHAT_ROOM_MEMBERS} WHERE roomId = :roomId ORDER BY userName ASC")
    fun getMembers(roomId: String): Flow<List<ChatRoomMemberEntity>>

    /**
     * Inserts a member into the database.
     */
    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertMember(chatRoomMemberEntity: ChatRoomMemberEntity)
}