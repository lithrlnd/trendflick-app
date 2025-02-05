package com.trendflick.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.trendflick.data.model.User
import com.trendflick.data.model.Video
import com.trendflick.data.db.Converters

@Database(
    entities = [User::class, Video::class],
    version = 9,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class TrendFlickDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun videoDao(): VideoDao

    companion object {
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Drop the old table if it exists
                database.execSQL("DROP TABLE IF EXISTS videos")

                // Create the new videos table with all fields
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS videos (
                        uri TEXT PRIMARY KEY NOT NULL,
                        did TEXT NOT NULL,
                        handle TEXT NOT NULL,
                        videoUrl TEXT NOT NULL,
                        description TEXT NOT NULL,
                        createdAt TEXT NOT NULL,
                        indexedAt TEXT NOT NULL,
                        sortAt TEXT NOT NULL,
                        title TEXT NOT NULL DEFAULT '',
                        thumbnailUrl TEXT NOT NULL DEFAULT '',
                        likes INTEGER NOT NULL DEFAULT 0,
                        comments INTEGER NOT NULL DEFAULT 0,
                        shares INTEGER NOT NULL DEFAULT 0,
                        username TEXT NOT NULL DEFAULT '',
                        userId TEXT NOT NULL DEFAULT '',
                        isImage INTEGER NOT NULL DEFAULT 0,
                        imageUrl TEXT NOT NULL DEFAULT '',
                        aspectRatio REAL NOT NULL DEFAULT 1.0,
                        authorAvatar TEXT NOT NULL DEFAULT ''
                    )
                """)
            }
        }
    }
} 