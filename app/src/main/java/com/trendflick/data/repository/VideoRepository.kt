package com.trendflick.data.repository

import com.trendflick.data.model.Video
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import android.util.LruCache
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.common.util.UnstableApi
import com.trendflick.data.SampleDataProvider

@UnstableApi
@Singleton
class VideoRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val videoCache: Cache
) {
    private val videoMemoryCache = LruCache<String, ByteArray>(10 * 1024 * 1024) // 10MB memory cache

    private val cacheDataSourceFactory by lazy {
        CacheDataSource.Factory()
            .setCache(videoCache)
            .setUpstreamDataSourceFactory(DefaultDataSource.Factory(context))
    }

    fun getAllVideos(): Flow<List<Video>> = SampleDataProvider.videos

    suspend fun insertVideo(video: Video) = withContext(Dispatchers.IO) {
        SampleDataProvider.addVideo(video)
        SampleDataProvider.updateVideoRelations()
    }

    suspend fun likeVideo(videoId: Int) = withContext(Dispatchers.IO) {
        val currentList = SampleDataProvider.videos.value.toMutableList()
        val videoIndex = currentList.indexOfFirst { it.id == videoId }
        if (videoIndex != -1) {
            val video = currentList[videoIndex]
            currentList[videoIndex] = video.copy(likes = video.likes + 1)
            SampleDataProvider.updateVideos(currentList)
        }
    }

    suspend fun unlikeVideo(videoId: Int) = withContext(Dispatchers.IO) {
        val currentList = SampleDataProvider.videos.value.toMutableList()
        val videoIndex = currentList.indexOfFirst { it.id == videoId }
        if (videoIndex != -1) {
            val video = currentList[videoIndex]
            currentList[videoIndex] = video.copy(likes = video.likes - 1)
            SampleDataProvider.updateVideos(currentList)
        }
    }

    suspend fun preloadVideo(videoUrl: String) = withContext(Dispatchers.IO) {
        if (videoMemoryCache.get(videoUrl) != null) return@withContext
        
        try {
            val dataSource = cacheDataSourceFactory.createDataSource()
            val mediaItem = MediaItem.fromUri(videoUrl)
            val dataSpec = DataSpec(mediaItem.localConfiguration?.uri ?: return@withContext)
            
            dataSource.open(dataSpec)
            
            val buffer = ByteArray(1024 * 1024) // 1MB buffer
            var bytesRead = 0
            var totalBytesRead = 0
            
            while (totalBytesRead < 5 * 1024 * 1024) { // Read up to 5MB
                bytesRead = dataSource.read(buffer, 0, buffer.size)
                if (bytesRead == -1) break
                totalBytesRead += bytesRead
            }
            
            dataSource.close()
            
            videoMemoryCache.put(videoUrl, buffer.copyOf(totalBytesRead))
        } catch (e: IOException) {
            // Handle error silently
        }
    }
} 