package com.jejecomms.realtimechatfeature.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.jejecomms.realtimechatfeature.data.local.dao.ChatRoomDetailDao
import com.jejecomms.realtimechatfeature.data.local.dao.MessageDao
import com.jejecomms.realtimechatfeature.data.local.dao.UsersDao
import com.jejecomms.realtimechatfeature.data.local.entity.ChatMessageEntity
import com.jejecomms.realtimechatfeature.data.local.entity.ChatRoomEntity
import com.jejecomms.realtimechatfeature.data.local.entity.ChatRoomMemberEntity
import com.jejecomms.realtimechatfeature.data.local.entity.ListConverter
import com.jejecomms.realtimechatfeature.data.local.entity.ReadReceiptEntity
import com.jejecomms.realtimechatfeature.data.local.entity.UsersEntity

/**
 * Room database for the chat feature.
 */
@Database(entities = [ChatMessageEntity::class, ChatRoomMemberEntity::class,
    ChatRoomEntity::class, ReadReceiptEntity::class, UsersEntity::class]
    ,version = 1, exportSchema = false)
@TypeConverters(ListConverter::class)
abstract class ChatDatabase : RoomDatabase() {

    /**
     * Get the message DAO.
     */
    abstract fun messageDao(): MessageDao

    /**
     * Get the user DAO.
     */
    abstract fun userDao(): UsersDao

    /**
     * Get the chat room detail DAO.
     */
    abstract fun chatRoomDetailDao(): ChatRoomDetailDao

    companion object {
        /**
         * Singleton instance of the ChatDatabase.
         */
        @Volatile
        private var INSTANCE: ChatDatabase? = null

        /**
         * Get the database instance.
         */
        fun getDatabase(context: Context): ChatDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ChatDatabase::class.java,
                    "chat_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}