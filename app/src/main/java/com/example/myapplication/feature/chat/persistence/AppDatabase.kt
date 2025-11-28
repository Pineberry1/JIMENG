package com.example.myapplication.feature.chat.persistence

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.myapplication.feature.chat.model.ChatMessage
import com.example.myapplication.feature.chat.model.ConversationEntity
import com.example.myapplication.feature.settings.model.ModelConfigDao
import com.example.myapplication.feature.settings.model.ModelConfigEntity
import androidx.room.TypeConverters
import com.example.myapplication.feature.chat.model.uploadImageIndex

@Database(entities = [
    ConversationEntity::class,
    ChatMessage::class,
    ModelConfigEntity::class,
    uploadImageIndex::class // <-- 1. 添加新实体
                     ], version = 2)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun conversationDao(): ConversationDao
    abstract fun modelConfigDao(): ModelConfigDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
