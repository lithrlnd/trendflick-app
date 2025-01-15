package com.trendflick.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.trendflick.data.model.User
import com.trendflick.data.model.Video

@Database(
    entities = [User::class, Video::class],
    version = 1,
    exportSchema = false
)
abstract class TrendFlickDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun videoDao(): VideoDao

    companion object {
        @Volatile
        private var INSTANCE: TrendFlickDatabase? = null

        fun getDatabase(context: Context): TrendFlickDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TrendFlickDatabase::class.java,
                    "trendflick_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
} 