package com.jejecomms.realtimechatfeature.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Room database for the chat feature.
 */
@Database(entities = [ChatMessageEntity::class,ChatRoomMemberEntity::class,
    ChatRoomEntity::class, ReadReceiptEntity::class]
    ,version = 1, exportSchema = false)
@TypeConverters(ListConverter::class)
abstract class ChatDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao

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