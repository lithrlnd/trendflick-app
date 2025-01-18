package com.trendflick.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.trendflick.data.model.User
import com.trendflick.data.model.Video
import com.trendflick.data.db.Converters

@Database(
    entities = [User::class, Video::class],
    version = 5,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class TrendFlickDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun videoDao(): VideoDao
} 