package com.jejecomms.realtimechatfeature.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jejecomms.realtimechatfeature.data.local.entity.UsersEntity
import com.jejecomms.realtimechatfeature.utils.Constants.USERS
import kotlinx.coroutines.flow.Flow

/**
 * Data access object (DAO) for the users
 */
@Dao
interface UsersDao {
    /**
     * Inserts a list of users into the database.
     */
    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertAllUsers(users: List<UsersEntity>)

    /**
     * Retrieves all users from the database in ascending order by timestamp.
     */
    @Query("SELECT * FROM ${USERS} WHERE uid != :senderId ORDER BY loginTime ASC")
    fun getAllUsers(senderId: String): Flow<List<UsersEntity>>

    /**
     * Retrieves a specific user name from the database.
     */
    @Query("SELECT * FROM ${USERS} WHERE uid = :senderId")
    fun getUserName(senderId: String): UsersEntity?
}