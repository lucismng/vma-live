package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.ChannelDao
import com.example.data.local.dao.ProgramDao
import com.example.data.local.dao.RecordingDao
import com.example.data.local.entity.ChannelEntity
import com.example.data.local.entity.ProgramEntity
import com.example.data.local.entity.RecordingEntity

@Database(
    entities = [ChannelEntity::class, ProgramEntity::class, RecordingEntity::class],
    version = 4,
    exportSchema = false
)
abstract class VmaDatabase : RoomDatabase() {
    abstract fun channelDao(): ChannelDao
    abstract fun programDao(): ProgramDao
    abstract fun recordingDao(): RecordingDao

    companion object {
        @Volatile
        private var INSTANCE: VmaDatabase? = null

        fun getInstance(context: Context): VmaDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    VmaDatabase::class.java,
                    "vma_tv.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
