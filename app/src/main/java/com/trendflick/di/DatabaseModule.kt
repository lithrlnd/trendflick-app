package com.trendflick.di

import android.content.Context
import androidx.room.Room
import com.squareup.moshi.Moshi
import com.trendflick.data.db.AppDatabase
import com.trendflick.data.db.Converters
import com.trendflick.data.local.UserDao
import com.trendflick.data.local.VideoDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideConverters(moshi: Moshi): Converters {
        return Converters(moshi)
    }

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        converters: Converters
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "trendflick.db"
        )
        .addTypeConverter(converters)
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    @Singleton
    fun provideUserDao(database: AppDatabase): UserDao {
        return database.userDao()
    }

    @Provides
    @Singleton
    fun provideVideoDao(database: AppDatabase): VideoDao {
        return database.videoDao()
    }
} 