package com.trendflick.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.trendflick.data.local.UserDao
import com.trendflick.data.local.VideoDao
import com.trendflick.data.model.User
import com.trendflick.data.model.Video

@Database(
    entities = [
        User::class,
        Video::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun videoDao(): VideoDao
} 