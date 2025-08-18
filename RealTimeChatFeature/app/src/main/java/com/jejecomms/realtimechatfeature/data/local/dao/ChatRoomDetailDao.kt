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
    @Query("SELECT * FROM ${CHAT_ROOM_MEMBERS} WHERE roomId = :roomId AND " +
            "isPendingRemoval = 0 ORDER BY userName ASC")
    fun getMembers(roomId: String): Flow<List<ChatRoomMemberEntity>>

    /**
     * Inserts a member into the database.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(chatRoomMemberEntity: ChatRoomMemberEntity)

    /**
     * Deletes a member from the database by their userId.
     */
    @Query("DELETE FROM ${CHAT_ROOM_MEMBERS} WHERE userId = :userId")
    suspend fun deleteMember(userId: String)

    /**
     * Retrieves all members marked for pending removal.
     */
    @Query("SELECT * FROM ${CHAT_ROOM_MEMBERS} WHERE isPendingRemoval = 1")
    fun getPendingRemovals(): Flow<List<ChatRoomMemberEntity>>

    /**
     * Retrieves a single member from the database by their userId.
     * Returns null if no member is found.
     */
    @Query("SELECT * FROM ${CHAT_ROOM_MEMBERS} WHERE userId = :userId")
    suspend fun getMemberByUserId(userId: String): ChatRoomMemberEntity?

    /**
     * Updates a single member's role by their userId.
     */
    @Query("UPDATE ${CHAT_ROOM_MEMBERS} SET role = :newRole WHERE userId = :userId")
    suspend fun updateMemberRole(userId: String, newRole: String)

    /**
     * Updates a member's role and their pending transfer role.
     */
    @Query("UPDATE ${CHAT_ROOM_MEMBERS} SET role = :newRole, transferRole = :newTransferRole " +
            "WHERE userId = :userId")
    suspend fun updateMemberRoleAndTransferRole(userId: String, newRole: String,
                                                newTransferRole: String)

    /**
     * Retrieves all members with a pending role transfer (transferRole is not empty).
     */
    @Query("SELECT * FROM ${CHAT_ROOM_MEMBERS} WHERE transferRole != ''")
    fun getMembersWithPendingRoleChanges(): Flow<List<ChatRoomMemberEntity>>
}