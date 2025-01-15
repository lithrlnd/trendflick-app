package com.trendflick

import android.app.Application
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import dagger.hilt.android.HiltAndroidApp
import java.io.File

@HiltAndroidApp
class TrendFlickApplication : Application() {

    companion object {
        private var cache: SimpleCache? = null
        private const val CACHE_SIZE: Long = 90 * 1024 * 1024 // 90MB

        fun getVideoCache(application: TrendFlickApplication): SimpleCache {
            if (cache == null) {
                val cacheDir = File(application.cacheDir, "media")
                val databaseProvider = StandaloneDatabaseProvider(application)
                cache = SimpleCache(
                    cacheDir,
                    LeastRecentlyUsedCacheEvictor(CACHE_SIZE),
                    databaseProvider
                )
            }
            return cache!!
        }
    }
} 