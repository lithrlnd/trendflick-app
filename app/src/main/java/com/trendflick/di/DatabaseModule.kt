package com.trendflick.di

import android.content.Context
import androidx.room.Room
import com.squareup.moshi.Moshi
import com.trendflick.data.db.Converters
import com.trendflick.data.local.TrendFlickDatabase
import com.trendflick.data.local.UserDao
import com.trendflick.data.local.VideoDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Add new columns to the videos table
            database.execSQL("""
                ALTER TABLE videos 
                ADD COLUMN isImage INTEGER NOT NULL DEFAULT 0
            """)
            database.execSQL("""
                ALTER TABLE videos 
                ADD COLUMN imageUrl TEXT NOT NULL DEFAULT ''
            """)
            database.execSQL("""
                ALTER TABLE videos 
                ADD COLUMN aspectRatio REAL NOT NULL DEFAULT 1.0
            """)
        }
    }

    @Provides
    @Singleton
    fun provideConverters(): Converters {
        return Converters()
    }

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): TrendFlickDatabase {
        return Room.databaseBuilder(
            context,
            TrendFlickDatabase::class.java,
            "trendflick.db"
        )
        .addMigrations(MIGRATION_7_8, TrendFlickDatabase.MIGRATION_8_9)
        .fallbackToDestructiveMigration() // Add this for development only
        .build()
    }

    @Provides
    @Singleton
    fun provideUserDao(database: TrendFlickDatabase): UserDao {
        return database.userDao()
    }

    @Provides
    @Singleton
    fun provideVideoDao(database: TrendFlickDatabase): VideoDao {
        return database.videoDao()
    }
} 