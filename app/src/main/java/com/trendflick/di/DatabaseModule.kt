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
    fun provideTrendFlickDatabase(
        @ApplicationContext context: Context,
        converters: Converters
    ): TrendFlickDatabase {
        return Room.databaseBuilder(
            context,
            TrendFlickDatabase::class.java,
            "trendflick.db"
        )
        .addTypeConverter(converters)
        .fallbackToDestructiveMigration()
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