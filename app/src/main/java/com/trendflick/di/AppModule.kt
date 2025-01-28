package com.trendflick.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import androidx.media3.datasource.cache.Cache
import androidx.media3.common.util.UnstableApi
import com.trendflick.TrendFlickApplication

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @UnstableApi
    @Provides
    @Singleton
    fun provideVideoCache(@ApplicationContext context: Context): Cache {
        return TrendFlickApplication.getVideoCache(context as TrendFlickApplication)
    }

    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context {
        return context
    }
} 